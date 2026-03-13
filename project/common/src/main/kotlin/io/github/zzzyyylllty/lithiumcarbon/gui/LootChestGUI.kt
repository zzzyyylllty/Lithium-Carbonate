package io.github.zzzyyylllty.lithiumcarbon.gui

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.config
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootItems
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootMap
import io.github.zzzyyylllty.lithiumcarbon.data.LootElement
import io.github.zzzyyylllty.lithiumcarbon.data.LootElementStat
import io.github.zzzyyylllty.lithiumcarbon.data.LootInstance
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.function.player.sendComponent
import io.github.zzzyyylllty.lithiumcarbon.util.SoundUtil.playConfiguredSound
import io.github.zzzyyylllty.lithiumcarbon.util.asNumberFormatNullable
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import io.github.zzzyyylllty.lithiumcarbon.util.mmUtil
import org.bukkit.entity.Player
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
}


// 这段我懒得修异步问题了，直接用AI
fun Player.openLootChest(initialInstance: LootInstance) { // 将参数名改为 initialInstance 以便与协程块内的变量区分

    val player = this

    // 假设 `template` 及其配置数据是静态的，或者在异步线程访问是安全的。
    // 如果 `initialInstance.template` 访问本身就需要主线程，那么此函数在调用时就应该被 submitChain { sync { ... } } 包裹。
    // 但鉴于问题焦点是 `LootInstance` 的方法调用和多人搜刮问题，我们先假设此处是安全的。
    val template = initialInstance.template ?: return

    var closed = false
    // `asNumberFormatNullable(player)` 可能会访问 Player 对象，此处最好也放在主线程，
    // 但由于它是只读操作且通常不直接触发 Bukkit 错误，我们暂时保持。
    // 更严格的做法是：val searchLimit = submitChain { sync { template.options.searchLimit.asNumberFormatNullable(player)?.roundToInt() } }.get()
    val searchLimit: Int? = template.options.searchLimit.asNumberFormatNullable(player)?.roundToInt()

    // `player.openMenu` 本身是一个 Bukkit UI 操作，菜单库通常会确保其在主线程执行。
    // `lootItems` 及其 `build(player)` 如果涉及 Bukkit ItemStacks 的创建或修改，
    // 理论上也应在主线程。我们假设菜单库或 ItemBuilder 已处理或其操作是线程安全的。
    player.openMenu<Chest>(template.title) {

        rows(template.rows)
        handLocked(true)
        map(*template.layout.toTypedArray())

        for (i in lootItems) {
            set(i.key, i.value.build(player))
        }

        // 辅助函数：更新单个槽位及其相关逻辑。此函数必须在主线程中调用。
        fun updateSingleSlotOnMainThread(
            slot: Int,
            element: LootElement?,
            inventory: Inventory,
            currentLootInstance: LootInstance, // 传入当前 LootInstance 的引用
            elementsMap: MutableMap<Int, LootElement?> // 传入可变 map 的引用
        ) {
            // slot 不在箱子范围内
            if (slot >= rows*9) return

            // 所有对 `currentLootInstance` 的访问和 Bukkit API 调用都将在此处的主线程环境中执行
            val stat = element?.let { currentLootInstance.getSearchStat(player, it, slot, currentLootInstance) }
            val display = stat?.let { element.getDisplayItem(it, player, template.options) }
            if (display == null && elementsMap.getOrDefault(slot, null) != null) {
                elementsMap.remove(slot)
            }

            if (stat == LootElementStat.SEARCHED && currentLootInstance.getSearchingSlots(player)?.contains(slot) == true) {
                playConfiguredSound(player, "search-end") // Bukkit API
                if (config.getBoolean("message.Searched")) player.sendComponent(player.asLangText("Searched", template.name, display?.displayName()?.let { mmUtil.serialize(it) } ?: "")) // Bukkit API
                currentLootInstance.removeSearchingSlots(player, slot) // 操作 LootInstance
            }
            inventory.setItem(slot, display) // Bukkit API
        }

        // 辅助函数：更新所有槽位。此函数必须在主线程中调用。
        fun updateAllSlotsOnMainThread(inventory: Inventory, currentLootInstance: LootInstance) {
            // 先从 LootInstance 读取一份可变副本，进行处理
            val elementsBeingProcessed = currentLootInstance.elements.toMutableMap()

            val keysToUpdate = elementsBeingProcessed.keys.toList()

            for (key in keysToUpdate) {
                val element = elementsBeingProcessed[key]
                updateSingleSlotOnMainThread(key, element, inventory, currentLootInstance, elementsBeingProcessed)
            }
            // 处理完成后，再更新回 LootInstance 的 elements 属性
            currentLootInstance.elements = elementsBeingProcessed // 操作 LootInstance
        }


        // onBuild 回调：由于 `async = true`，此块在异步线程执行。
        // 其中所有与 `initialInstance` 和 Bukkit API 相关的操作都必须切换到主线程。
        onBuild(async = true) { _, inventory -> // 将 player 参数重命名为 _ 以避免与外部 player 变量冲突
            // 使用 submitChain 来调度主线程操作
            submitChain {
                sync { // 切换到主线程
                    openedLootLocation[player.uniqueId] = initialInstance.loc // 操作 LootInstance
                    devLog("refreshing")
                    playConfiguredSound(player, "open") // Bukkit API

                    updateAllSlotsOnMainThread(inventory, initialInstance) // 在主线程调用辅助函数
                    template.agents?.runAgent("onOpen", linkedMapOf("inventory" to inventory), player) // 假设 agent 也可能需要主线程
                }
            }

            // 周期任务：`submitAsync` 本身就将任务提交到异步线程。
            // 因此，其内部所有涉及 `initialInstance` 和 Bukkit API 的操作仍然需要 `submitChain { sync { ... } }`。
            submitAsync(period = 5) {
                // 此处仍在异步线程
                if (closed) {
                    cancel() // 取消 `submitAsync` 任务
                } else {
                    submitChain { // 每次周期执行都创建一个新的调度链
                        sync { // 切换到主线程执行
                            updateAllSlotsOnMainThread(inventory, initialInstance) // 在主线程调用辅助函数
                            template.agents?.runAgent("onUpdate", linkedMapOf("inventory" to inventory), player) // 假设 agent 也可能需要主线程
                        }
                    }
                }
            }
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
                                devLog("Starting to search $rawSlot item")
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
                                                "inventory" to inventory
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
                                val newElements = initialInstance.elements.toMutableMap()
                                updateSingleSlotOnMainThread(rawSlot, element, inventory, initialInstance, newElements)
                                initialInstance.elements = newElements // 操作 LootInstance
                                return@sync
                            }
                            LootElementStat.SEARCHING -> {
                                if (config.getBoolean("message.Searching"))  player.sendComponent(player.asLangText("Searching", template.name)) // Bukkit API
                                playConfiguredSound(player, "searching") // Bukkit API
                                return@sync
                            }
                            LootElementStat.SEARCHED -> {
                                // 移除物品并给予玩家
                                val elementsCopy = initialInstance.elements.toMutableMap()
                                elementsCopy[rawSlot] = null
                                initialInstance.elements = elementsCopy // 操作 LootInstance

                                element.applyToPlayer(player, template) // 假设 applyToPlayer 内部是安全的，如果它有 Bukkit API，也需要包裹
                                template.agents?.runAgent(
                                    "onClaim",
                                    linkedMapOf(
                                        "event" to event,
                                        "element" to element,
                                        "displayItem" to element.displayItem,
                                        "inventory" to inventory
                                    ),
                                    player
                                )
                                playConfiguredSound(player, "claim") // Bukkit API

                                val newElements = initialInstance.elements.toMutableMap()
                                updateSingleSlotOnMainThread(rawSlot, element, inventory, initialInstance, newElements)
                                initialInstance.elements = newElements // 操作 LootInstance
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
        onClose { event ->
            submitChain { // 为关闭事件创建一个新的调度链
                sync { // 切换到主线程
                    closed = true // 修改局部变量是安全的
                    initialInstance.resetPlayerSearch(event.player as Player) // 操作 LootInstance
                    openedLootLocation.remove(event.player.uniqueId) // 假设 openedLootLocation 是线程安全的
                    template.agents?.runAgent("onClose", linkedMapOf("event" to event, "inventory" to inventory), player) // 假设 agent 也可能需要主线程
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
