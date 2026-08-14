package io.github.zzzyyylllty.lithiumcarbon.gui

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.config
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootItems
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootMap
import io.github.zzzyyylllty.lithiumcarbon.data.LootElement
import io.github.zzzyyylllty.lithiumcarbon.data.LootElementStat
import io.github.zzzyyylllty.lithiumcarbon.data.LootInstance
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.event.ItemSearchCompletePostEvent
import io.github.zzzyyylllty.lithiumcarbon.event.ItemSearchCompletePreEvent
import io.github.zzzyyylllty.lithiumcarbon.event.ItemSearchStartEvent
import io.github.zzzyyylllty.lithiumcarbon.event.LootElementApplyEvent
import io.github.zzzyyylllty.lithiumcarbon.function.player.sendComponent
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockStateManager
import io.github.zzzyyylllty.lithiumcarbon.util.SoundUtil.playConfiguredSound
import io.github.zzzyyylllty.lithiumcarbon.util.asNumberFormatNullable
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import io.github.zzzyyylllty.lithiumcarbon.util.mmJsonUtil
import io.github.zzzyyylllty.lithiumcarbon.util.mmUtil
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.Inventory
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submitAsync
import taboolib.expansion.submitChain
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest
import taboolib.platform.util.asLangText
import taboolib.platform.util.submit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

val openedLootLocation = ConcurrentHashMap<UUID, LootLocation>()

@SubscribeEvent
fun onPlayerLeaveUnloadLocation(e: PlayerQuitEvent) {
    openedLootLocation.remove(e.player.uniqueId)
    val active = UnlockStateManager.getActive(e.player)
    if (active != null) {
        active.flow.cancel(e.player)
        UnlockStateManager.removeActive(e.player)
    }
}


// 这段我懒得修异步问题了，直接用AI
fun Player.openLootChest(initialInstance: LootInstance, event: PlayerInteractEvent?) {

    val player = this

    val template = initialInstance.template ?: return

    // 打开的战利品位置对应的方块，供 agents 使用（event 可能为 null 或过时）
    val block = initialInstance.loc.toBukkitLocation().block

    val closed = java.util.concurrent.atomic.AtomicBoolean(false)
    var cancelUpdateTask: (() -> Unit)? = null
    val searchLimit: Int? = template.options.searchLimit.asNumberFormatNullable(player)?.roundToInt()

    player.openMenu<Chest>(mmJsonUtil.serialize(mmUtil.deserialize("<black>" + template.title))) {

        rows(template.rows)
        handLocked(true)
        map(*template.layout.toTypedArray())

        for (i in lootItems) {
            set(i.key, i.value.build(player))
        }

        fun updateSingleSlotOnMainThread(
            slot: Int,
            element: LootElement?,
            inventory: Inventory,
            currentLootInstance: LootInstance,
            elementsMap: MutableMap<Int, LootElement?>
        ) {
            // slot 不在范围内
            if (slot >= rows*9) return

            val stat = element?.let { currentLootInstance.getSearchStat(player, it, slot, currentLootInstance) }
            val display = stat?.let { element.getDisplayItem(it, player, template.options) }

            if (stat == LootElementStat.SEARCHED && currentLootInstance.getSearchingSlots(player)?.contains(slot) == true) {
                playConfiguredSound(player, "search-end") // Bukkit API
                if (config.getBoolean("message.Searched")) player.sendComponent(player.asLangText("Searched", template.name, display?.displayName()?.let { mmUtil.serialize(it) } ?: "")) // Bukkit API
                currentLootInstance.removeSearchingSlots(player, slot) // 操作 LootInstance
            }
            inventory.setItem(slot, display) // Bukkit API
        }

        // 刷新所有搜索中的槽位，包括已完成的（让显示从"搜索中"切换到物品）
        fun updateSearchingSlotsOnMainThread(inventory: Inventory, currentLootInstance: LootInstance) {
            val search = currentLootInstance.searches[player.uniqueId.toString()]?.searches?.keys ?: return
            val elements = currentLootInstance.elements
            for (slot in search) {
                val element = elements[slot]
                updateSingleSlotOnMainThread(slot, element, inventory, currentLootInstance, elements)
            }
        }

        fun updateAllSlotsOnMainThread(inventory: Inventory, currentLootInstance: LootInstance) {
            val elementsBeingProcessed = currentLootInstance.elements.toMutableMap()

            val keysToUpdate = elementsBeingProcessed.keys.toList()

            for (key in keysToUpdate) {
                val element = elementsBeingProcessed[key]
                updateSingleSlotOnMainThread(key, element, inventory, currentLootInstance, elementsBeingProcessed)
            }
            // 处理完成后，再更新回 LootInstance 的 elements 属性
            currentLootInstance.elements = elementsBeingProcessed // 操作 LootInstance
        }


        onBuild(async = true) { _, inventory ->
            // 使用 submitChain 来调度主线程操作
            submitChain {
                sync { // 切换到主线程
                    openedLootLocation[player.uniqueId] = initialInstance.loc
                    devLog("refreshing")
                    playConfiguredSound(player, "open")

                    updateAllSlotsOnMainThread(inventory, initialInstance) // 在主线程调用辅助函数
                    template.agents?.runAgent("onOpen", linkedMapOf("inventory" to inventory, "event" to event, "block" to block), player) // 假设 agent 也可能需要主线程
                }
            }

            // 周期任务：只刷新正在搜索的格子（替代原来的全量刷新）
            val task = submitAsync(period = 5) {
                if (closed.get()) {
                    cancel()
                } else {
                    submitChain {
                        sync {
                            updateSearchingSlotsOnMainThread(inventory, initialInstance)
                            template.agents?.runAgent("onUpdate", linkedMapOf("inventory" to inventory, "event" to event, "block" to block), player)
                        }
                    }
                }
            }
            cancelUpdateTask = { task.cancel() }
        }



        onClick { event ->
            event.clickEvent().isCancelled = true
            submitChain { // 为每次点击事件创建一个新的调度链
                sync { // 切换到主线程

                    if (event.slot != ' ') {
                        devLog("clicked non-loot slot")
                        return@sync // 从 sync 块返回
                    }

                    val rawSlot = event.rawSlot
                    if (rawSlot == -999) return@sync

                    val inventory = event.inventory

                    // 所有对 `initialInstance` 的访问都必须在主线程
                    val element = initialInstance.elements.getOrDefault(rawSlot, null)

                    if (element != null) {
                        devLog("slot $rawSlot have item")
                        val stat = initialInstance.getSearchStat(player, element, rawSlot, initialInstance)

                        when (stat) {
                            LootElementStat.NOT_SEARCHED -> {
                                val event = ItemSearchStartEvent(player, initialInstance, element, rawSlot, inventory)
                                devLog("Starting to search $rawSlot item")
                                event.call()
                                if (event.isCancelled) return@sync
                                searchLimit?.let {
                                    if (it <= (initialInstance.getSearchingSlots(player)?.size ?: 0)) { // 访问 LootInstance
                                        playConfiguredSound(player, "search-limit") // Bukkit API
                                        template.agents?.runAgent(
                                            "onSearchLimit",
                                            linkedMapOf(
                                                "limit" to it,
                                                "event" to event,
                                                "element" to element,
                                                "displayItem" to element.displayItem,
                                                "inventory" to inventory,
                                                "block" to block
                                            ),
                                            player
                                        )
                                        if (config.getBoolean("message.SearchLimit")) player.sendComponent(player.asLangText("SearchLimit", template.name, it)) // Bukkit API
                                        return@sync
                                    }
                                }
                                val time = element.searchTime
                                if (!element.skipSearch) {
                                    if (time > 0) {
                                        devLog("Start search.")
                                        initialInstance.startSearch(player, rawSlot, time) // 操作 LootInstance
                                        if (config.getBoolean("message.SearchStart"))  player.sendComponent(player.asLangText("SearchStart", template.name)) // Bukkit API
                                        playConfiguredSound(player, "search") // Bukkit API
                                    } else {
                                        devLog("Search time is 0, skip search.")
                                        playConfiguredSound(player, "search") // Bukkit API
                                        if (config.getBoolean("message.SearchStart"))  player.sendComponent(player.asLangText("SearchStart", template.name)) // Bukkit API
                                        initialInstance.startSearch(player, rawSlot, time, true) // 操作 LootInstance
                                    }
                                } else {
                                    initialInstance.startSearch(player, rawSlot, time, true) // 操作 LootInstance
                                }
                                // 获取可变副本，修改，然后赋值回 LootInstance
                                val eventComplete = ItemSearchCompletePreEvent(player, initialInstance, element, rawSlot, inventory)
                                eventComplete.call()
                                if (eventComplete.isCancelled) return@sync
                                val newElements = initialInstance.elements.toMutableMap()
                                updateSingleSlotOnMainThread(rawSlot, element, inventory, initialInstance, newElements)
                                initialInstance.elements = newElements
                                ItemSearchCompletePostEvent(player, initialInstance, element, rawSlot, inventory).call()
                                return@sync
                            }
                            LootElementStat.SEARCHING -> {
                                if (config.getBoolean("message.Searching"))  player.sendComponent(player.asLangText("Searching", template.name)) // Bukkit API
                                playConfiguredSound(player, "searching") // Bukkit API
                                return@sync
                            }
                            LootElementStat.SEARCHED -> {
                                // 移除物品并给予玩家

                                val event = LootElementApplyEvent(player, initialInstance, element, rawSlot)
                                event.call()
                                if (event.isCancelled) return@sync
                                val elementsCopy = initialInstance.elements.toMutableMap()
                                elementsCopy[rawSlot] = null
                                initialInstance.elements = elementsCopy // 操作 LootInstance

                                element.applyToPlayer(player, template)
                                template.agents?.runAgent(
                                    "onClaim",
                                    linkedMapOf(
                                        "event" to event,
                                        "element" to element,
                                        "displayItem" to element.displayItem,
                                        "inventory" to inventory,
                                        "block" to block
                                    ),
                                    player
                                )
                                playConfiguredSound(player, "claim") // Bukkit API

                                val newElements = initialInstance.elements.toMutableMap()
                                updateSingleSlotOnMainThread(rawSlot, element, inventory, initialInstance, newElements)
                                initialInstance.elements = newElements // 操作 LootInstance
                                if (initialInstance.isFullyLooted()) {
                                    template.agents?.runAgent(
                                        "onEmpty",
                                        linkedMapOf(
                                            "event" to event,
                                            "inventory" to inventory,
                                            "instance" to initialInstance,
                                            "player" to player,
                                            "block" to block,
                                        ),
                                        player
                                    )
                                }
                                return@sync
                            }
                            LootElementStat.NOITEM -> {
                                return@sync
                            }
                        }

                    } else {
                        devLog("slot $rawSlot is empty")
                        val newElements = initialInstance.elements.toMutableMap()
                        updateSingleSlotOnMainThread(rawSlot, null, inventory, initialInstance, newElements)
                        initialInstance.elements = newElements // 操作 LootInstance
                    }
                }
            }
        }

        // onClose 回调：假设也在异步线程执行。
        onClose { closeEvent ->
            submitChain { // 为关闭事件创建一个新的调度链
                sync { // 切换到主线程
                    closed.set(true)
                    cancelUpdateTask?.invoke()
                    initialInstance.resetPlayerSearch(closeEvent.player as Player) // 操作 LootInstance
                    openedLootLocation.remove(closeEvent.player.uniqueId) // 假设 openedLootLocation 是线程安全的
                    template.agents?.runAgent("onClose", linkedMapOf("closeEvent" to closeEvent, "inventory" to inventory, "event" to event, "block" to block), player) // 假设 agent 也可能需要主线程
                }
            }
        }
    }
}

//fun Player.openLootChest(instance: LootInstance) {
//
//    val player = this
//
//    val template = instance.template ?: return
//
//    var closed = false
//    val searchLimit: Int? = template.options.searchLimit.asNumberFormatNullable(player)?.roundToInt()
//
//    player.openMenu<Chest>(template.title) {
//
//        rows(template.rows)
//
//        handLocked(true)
//
//        map(*template.layout.toTypedArray())
//
//        for (i in lootItems) {
//            set(i.key, i.value.build(player))
//        }
//
//        fun update(int: Int, element: LootElement?, inventory: Inventory, instance: LootInstance, elements: MutableMap<Int, LootElement?>) {
//            val stat = element?.let { instance.getSearchStat(player, it, int, instance) }
//            val display = stat?.let { element.getDisplayItem(it, player, template.options) }
//
//            if (display == null && element != null) {
//                elements.remove(int)
//            }
//
//            if (stat == LootElementStat.SEARCHED && instance.getSearchingSlots(player)?.contains(int) == true) {
//                playConfiguredSound(player, "search-end")
//
//                player.sendComponent(player.asLangText("Searched", template.name, display?.displayName()?.let { mmUtil.serialize(it) } ?: ""))
//                instance.removeSearchingSlots(player, int)
//            }
////            devLog("Updating $int")
//            inventory.setItem(int, display)
//        }
//        fun updateAll(inventory: Inventory) {
//            val elementsBeingProcessed = instance.elements.toMutableMap()
//
//            val keysToUpdate = elementsBeingProcessed.keys.toList() // 获取一个稳定的键列表
//
//            for (key in keysToUpdate) {
//                val element = elementsBeingProcessed[key]
//                update(key, element, inventory, instance, elementsBeingProcessed)
//            }
//
//            instance.elements = elementsBeingProcessed
//        }
//        onBuild(async = true) { player, inventory ->
//            openedLootLocation[player.uniqueId.toString()] = instance.loc
//            devLog("refreshing")
//            playConfiguredSound(player, "open")
//            updateAll(inventory)
//            template.agents?.runAgent("onOpen", linkedMapOf("inventory" to inventory), player)
//            submitAsync(period = 5) {
//                if (closed || !player.isOnline) {
//                    cancel()
//                } else {
//                    updateAll(inventory)
//                    template.agents?.runAgent("onUpdate", linkedMapOf("inventory" to inventory), player)
//                }
//            }
//        }
//
//
//        // 元素点击事件
//        onClick { event ->
//
//            event.clickEvent().isCancelled = true
//            // 如果不是战利品
//            if (event.slot != ' ') {
//                devLog("clicked non-loot slot")
//                return@onClick
//            }
//
//            val rawSlot = event.rawSlot
//
//            if (rawSlot == -999) return@onClick
//
//            val inventory = event.inventory
//
//            val element = instance.elements.getOrDefault(rawSlot, null)
//
//            if (element != null) {
//                devLog("slot $rawSlot have item")
//
//                val stat = instance.getSearchStat(player, element, rawSlot, instance)
//
//                when (stat) {
//                    LootElementStat.NOT_SEARCHED -> {
//                        devLog("Starting to search $rawSlot item")
//                        searchLimit?.let {
//                            if (it <= (instance.getSearchingSlots(player)?.size ?: 0)) {
//                                playConfiguredSound(player, "search-limit")
//                                template.agents?.runAgent(
//                                    "onSearchLimit",
//                                    linkedMapOf(
//                                        "limit" to it,
//                                        "event" to event,
//                                        "element" to element,
//                                        "displayItem" to element.displayItem,
//                                        "inventory" to inventory
//                                    ),
//                                    player
//                                )
//                                player.sendComponent(player.asLangText("SearchLimit", template.name, it))
//                                return@onClick
//                            }
//                        }
//                        val time = element.searchTime
//                        if (!element.skipSearch) {
//                            if (time > 0) {
//                                devLog("Start search.")
//                                instance.startSearch(player, rawSlot, time)
//                                player.sendComponent(player.asLangText("SearchStart", template.name))
//                                playConfiguredSound(player, "search")
//                            } else {
//                                devLog("Search time is 0, skip search.")
//                                playConfiguredSound(player, "search")
//                                player.sendComponent(player.asLangText("SearchStart", template.name))
//                                instance.startSearch(player, rawSlot, time, true)
//                            }
//                        } else {
//                            instance.startSearch(player, rawSlot, time, true)
//                        }
//                        val newElements = instance.elements
//                        update(rawSlot, element, inventory, instance, newElements)
//                        instance.elements = newElements
//                        return@onClick
//                    }
//                    LootElementStat.SEARCHING -> {
//                        player.sendComponent(player.asLangText("Searching", template.name))
//                        playConfiguredSound(player, "searching")
//                        return@onClick
//                    }
//                    LootElementStat.SEARCHED -> {
//
//                        // 先移除物品
//                        instance.elements[rawSlot] = null
//
//                        // 再构建并给予
//                        element.applyToPlayer(player, template)
//                        template.agents?.runAgent(
//                            "onClaim",
//                            linkedMapOf(
//                                "event" to event,
//                                "element" to element,
//                                "displayItem" to element.displayItem,
//                                "inventory" to inventory
//                            ),
//                            player
//                        )
//                        playConfiguredSound(player, "claim")
//
//                        val newElements = instance.elements
//                        update(rawSlot, element, inventory, instance, newElements)
//                        instance.elements = newElements
//                        return@onClick
//                    }
//                    LootElementStat.NOITEM -> {
//                        return@onClick
//                    }
//                }
//
//            } else {
//                devLog("slot $rawSlot is empty")
//                val newElements = instance.elements
//                update(rawSlot, null, inventory, instance, newElements)
//                instance.elements = newElements
//            }
//
//        }
//
//        onClose { event ->
//            closed = true
//            instance.resetPlayerSearch(event.player as Player)
//            openedLootLocation.remove(event.player.uniqueId.toString())
//            template.agents?.runAgent("onClose", linkedMapOf("event" to event, "inventory" to inventory), player)
//        }
//
//    }
//}
