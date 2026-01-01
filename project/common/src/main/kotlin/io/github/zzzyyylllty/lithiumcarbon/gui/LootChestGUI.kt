package io.github.zzzyyylllty.lithiumcarbon.gui

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.console
import io.github.zzzyyylllty.lithiumcarbon.data.LootElement
import io.github.zzzyyylllty.lithiumcarbon.data.LootElementStat
import io.github.zzzyyylllty.lithiumcarbon.data.LootInstance
import io.github.zzzyyylllty.lithiumcarbon.data.LootItem
import io.github.zzzyyylllty.lithiumcarbon.data.SearchStat
import io.github.zzzyyylllty.lithiumcarbon.logger.warningS
import io.github.zzzyyylllty.lithiumcarbon.util.SoundUtil.playConfiguredSound
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType.*
import org.bukkit.inventory.Inventory
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import taboolib.common.platform.function.warning
import taboolib.library.xseries.XMaterial
import taboolib.module.lang.asLangText
import taboolib.module.nms.getItemTag
import taboolib.module.nms.setItemTag
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest
import taboolib.module.ui.type.PageableChest
import taboolib.platform.util.ItemBuilder
import taboolib.platform.util.Slots
import taboolib.platform.util.giveItem
import taboolib.platform.util.inventoryCenterSlots
import kotlin.collections.toList
import kotlin.collections.toMutableList

fun Player.openLootChest(instance: LootInstance) {

    val player = this

    val template = instance.template ?: run {
        warningS("An error occurred: Opening LootTable is not exist. Is you deleted LootTable Configuration in runtime?")
        warningS("Location: ${instance.loc.toFormat()}")
        return
    }

    var closed = false

    val searchingSlots = mutableSetOf<Int>()

    player.openMenu<Chest>(template.title) {

        rows(template.rows)

        handLocked(true)

        map(*template.layout.toTypedArray())

        set('-', XMaterial.GRAY_STAINED_GLASS_PANE) { name = " " }

        fun update(int: Int, inventory: Inventory, instance: LootInstance) {
            val element = instance.getSlotItem(int) ?: return
            val stat = instance.getSearchStat(player, element, int, instance)
            val display = element.getDisplayItem(stat, player)
            if (display == null) {
                instance.elements.remove(int)
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
                instance.elements.remove(int)
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
            for (element in instance.elements) {
                update(element.key, element.value, inventory, instance)
            }
        }

        onBuild(async = true) { player, inventory ->
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

                if (stat == LootElementStat.NOT_SEARCHED) {
                    devLog("Starting to search $rawSlot item")
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
                } else if (stat == LootElementStat.SEARCHING) {
                    playConfiguredSound(player, "searching")
                    return@onClick
                } else if (stat == LootElementStat.SEARCHED) {

                    // 先移除物品
                    instance.elements[rawSlot] = null

                    // 再构建并给予
                    element.applyToPlayer(player)
                    template.agents?.runAgent("onClaim", linkedMapOf("event" to event, "element" to element, "displayItem" to element.displayItem, "inventory" to inventory), player)
                    playConfiguredSound(player, "claim")

                    update(rawSlot, inventory, instance)
                    return@onClick
                } else if (stat == LootElementStat.NOITEM) {
                    return@onClick
                }

            } else {
                devLog("slot $rawSlot is empty")
                update(rawSlot, inventory, instance)
            }

        }

        onClose { event ->
            closed = true
            instance.resetPlayerSearch(event.player as Player)
            template.agents?.runAgent("onClose", linkedMapOf("event" to event, "inventory" to inventory), player)
        }

    }
}
