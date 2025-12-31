package io.github.zzzyyylllty.lithiumcarbon.gui

import io.github.zzzyyylllty.lithiumcarbon.data.LootElement
import io.github.zzzyyylllty.lithiumcarbon.data.LootElementStat
import io.github.zzzyyylllty.lithiumcarbon.data.LootInstance
import io.github.zzzyyylllty.lithiumcarbon.data.LootItem
import io.github.zzzyyylllty.lithiumcarbon.logger.warningS
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import io.github.zzzyyylllty.sertraline.Sertraline.console
import kotlin220.collections.toTypedArray
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType.*
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
import taboolib.platform.util.giveItem
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

    player.openMenu<Chest>(console.asLangText("Editor_Title")) {

        rows(6)

        map(*template.layout.toTypedArray())

        set('-', XMaterial.GRAY_STAINED_GLASS_PANE) { name = " " }

        fun update(int: Int) {
            val element = instance.elements[int] ?: return
            val display = element.getDisplayItem(instance.getSearchStat(player, element), player)
            inventory.setItem(int, display)
        }
        fun update(int: Int, element: LootElement) {
            val display = element.getDisplayItem(instance.getSearchStat(player, element), player)
            inventory.setItem(int, display)
        }
        fun updateAll() {
            val instance = instance.refresh() ?: run {
                closeInventory()
                return
            }
            for (element in instance.elements) {
                update(element.key, element.value)
            }
        }

        onBuild(async = true) { player, inventory ->
            devLog("战利品容器开始总刷新，原因为 界面初始化")
            updateAll()
        }

        // 元素点击事件
        onClick { event ->

            // 如果不是战利品
            if (event.slot != ' ') {
                devLog("点击了非战利品的格子")
                return@onClick
            }

            submitAsync {

                val rawSlot = event.rawSlot

                update(rawSlot)

                val bukkitPlayer = event.clicker

                val element = instance.elements[rawSlot]

                if (element != null) {
                    devLog("格子含有物品")

                    if (instance.getSearchStat(player, element) == LootElementStat.NOT_SEARCHED) {
                        devLog("开始搜索物品")
                        val time = element.searchTime
                        if (time > 0) {
                            devLog("搜索时间不为0，开始搜索。")
                            instance.startSearch(player, rawSlot, time)
                        } else {
                            devLog("搜索时间为0，跳过搜索过程。")
                            instance.startSearch(player, rawSlot, time)
                        }
                        return@submitAsync
                    }


                    // 先移除物品
                    instance.elements.remove(rawSlot)

                    // 再构建并给予
                    element.applyToPlayer(player)

                } else {
                    devLog("格子不含有物品")
                }

            }
        }

        onClick { event ->
            closed = true
        }


    }
}

fun Player.refreshLootChestGUI(instance: LootInstance?) {
    val player = this
    submitAsync {
        instance?.let { player.openLootChest(it) } ?: closeInventory()
    }
}