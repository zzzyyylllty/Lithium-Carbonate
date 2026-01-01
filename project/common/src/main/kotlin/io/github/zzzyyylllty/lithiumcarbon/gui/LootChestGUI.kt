package io.github.zzzyyylllty.lithiumcarbon.gui

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootItems
import io.github.zzzyyylllty.lithiumcarbon.data.LootElement
import io.github.zzzyyylllty.lithiumcarbon.data.LootElementStat
import io.github.zzzyyylllty.lithiumcarbon.data.LootInstance
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.util.SoundUtil.playConfiguredSound
import io.github.zzzyyylllty.lithiumcarbon.util.asNumberFormatNullable
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.Inventory
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest
import kotlin.math.roundToInt

val openedLootLocation = LinkedHashMap<String, LootLocation>()

@SubscribeEvent
fun onPlayerLeaveUnloadLocation(e: PlayerQuitEvent) {
    openedLootLocation.remove(e.player.uniqueId.toString())
}

fun Player.openLootChest(instance: LootInstance) {

    val player = this

    val template = instance.template ?: return

    var closed = false
    val searchLimit: Int? = template.options.searchLimit.asNumberFormatNullable(player)?.roundToInt()

    val searchingSlots = mutableSetOf<Int>()

    player.openMenu<Chest>(template.title) {

        rows(template.rows)

        handLocked(true)

        map(*template.layout.toTypedArray())

        for (i in lootItems) {
            set(i.key, i.value.build(player))
        }

        fun update(int: Int, inventory: Inventory, instance: LootInstance) {
            val element = instance.getSlotItem(int) ?: return
            val stat = instance.getSearchStat(player, element, int, instance)
            val display = element.getDisplayItem(stat, player)

            if (display == null) {
                val newElement = instance.elements
                newElement.remove(int)
                instance.elements = newElement
            }

            if (stat == LootElementStat.SEARCHED && searchingSlots.contains(int)) {
                playConfiguredSound(player, "search-end")
                searchingSlots.remove(int)
            }
//            devLog("Updating $int")
            inventory.setItem(int, display)
        }
        fun update(int: Int, element: LootElement?, inventory: Inventory, instance: LootInstance) {
            val stat = element?.let { instance.getSearchStat(player, it, int, instance) }
            val display = stat?.let { element.getDisplayItem(it, player) }

            if (display == null) {
                val newElement = instance.elements
                newElement.remove(int)
                instance.elements = newElement
            }

            if (stat == LootElementStat.SEARCHED && searchingSlots.contains(int)) {
                playConfiguredSound(player, "search-end")
                searchingSlots.remove(int)
            }
//            devLog("Updating $int")
            inventory.setItem(int, display)
        }
        fun updateAll(inventory: Inventory) {
//            devLog("Updating ALL")

            val iterator = instance.elements.iterator()
            while (iterator.hasNext()) {
                val element = iterator.next()
                update(element.key, element.value, inventory, instance)
            }
//            for (element in instance.elements) {
//                update(element.key, element.value, inventory, instance)
//            }
        }

        onBuild(async = true) { player, inventory ->
            openedLootLocation[player.uniqueId.toString()] = instance.loc
            devLog("refreshing")
            playConfiguredSound(player, "open")
            updateAll(inventory)
            template.agents?.runAgent("onOpen", linkedMapOf("inventory" to inventory), player)
            submitAsync(period = 5) {
                if (closed) {
                    cancel()
                } else {
                    updateAll(inventory)
                    template.agents?.runAgent("onUpdate", linkedMapOf("inventory" to inventory), player)
                }
            }
        }


        // 元素点击事件
        onClick { event ->

            event.clickEvent().isCancelled = true
            // 如果不是战利品
            if (event.slot != ' ') {
                devLog("clicked non-loot slot")
                return@onClick
            }

            val rawSlot = event.rawSlot
            val inventory = event.inventory

            val element = instance.elements[rawSlot]

            if (element != null) {
                devLog("slot $rawSlot have item")

                val stat = instance.getSearchStat(player, element, rawSlot, instance)

                when (stat) {
                    LootElementStat.NOT_SEARCHED -> {
                        devLog("Starting to search $rawSlot item")
                        searchLimit?.let {
                            if (it <= (instance.getSearchStatRaw(player)?.searches?.size ?: 0)) {
                                playConfiguredSound(player, "search-limit")
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
                            }
                        }
                        val time = element.searchTime
                        if (!element.skipSearch) {
                            if (time > 0) {
                                devLog("Start search.")
                                instance.startSearch(player, rawSlot, time)
                                playConfiguredSound(player, "search")
                                searchingSlots.add(rawSlot)
                            } else {
                                devLog("Search time is 0, skip search.")
                                playConfiguredSound(player, "search")
                                instance.startSearch(player, rawSlot, time, true)
                                searchingSlots.add(rawSlot)
                            }
                        } else {

                            instance.startSearch(player, rawSlot, time, true)
                        }
                        update(rawSlot, element, inventory, instance)
                        return@onClick
                    }
                    LootElementStat.SEARCHING -> {
                        playConfiguredSound(player, "searching")
                        return@onClick
                    }
                    LootElementStat.SEARCHED -> {

                        // 先移除物品
                        instance.elements[rawSlot] = null

                        // 再构建并给予
                        element.applyToPlayer(player)
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
                        playConfiguredSound(player, "claim")

                        update(rawSlot, inventory, instance)
                        return@onClick
                    }
                    LootElementStat.NOITEM -> {
                        return@onClick
                    }
                }

            } else {
                devLog("slot $rawSlot is empty")
                update(rawSlot, inventory, instance)
            }

        }

        onClose { event ->
            closed = true
            instance.resetPlayerSearch(event.player as Player)
            openedLootLocation.remove(event.player.uniqueId.toString())
            template.agents?.runAgent("onClose", linkedMapOf("event" to event, "inventory" to inventory), player)
        }

    }
}
