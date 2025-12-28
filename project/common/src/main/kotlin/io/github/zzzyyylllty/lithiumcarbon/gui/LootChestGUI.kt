package io.github.zzzyyylllty.lithiumcarbon.gui

import io.github.zzzyyylllty.lithiumcarbon.data.LootElement
import io.github.zzzyyylllty.lithiumcarbon.data.LootInstance
import io.github.zzzyyylllty.lithiumcarbon.logger.warningS
import io.github.zzzyyylllty.sertraline.Sertraline.console
import io.github.zzzyyylllty.sertraline.Sertraline.itemMap
import io.github.zzzyyylllty.sertraline.config.asListEnhanded
import io.github.zzzyyylllty.sertraline.data.ModernSItem
import io.github.zzzyyylllty.sertraline.item.sertralineItemBuilder
import io.github.zzzyyylllty.sertraline.util.gui.GuiItem
import io.github.zzzyyylllty.sertraline.util.gui.build
import io.github.zzzyyylllty.sertraline.util.minimessage.toComponent
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType.*
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

class LootChestGUI {
    fun mainItemExplorer(player: Player, instance: LootInstance) {

        player.openMenu<Chest>(console.asLangText("Editor_Title")) {

            val template = instance.template ?: {
                warningS("An error occurred: Opening LootTable is not exist. Is you deleted LootTable Configuration in runtime?")
                warningS("Location: ${instance.loc.toFormat()}")
                return
            }

            instance.elements.forEach {
                set(it.key, it.value.displayItem)
            }

            rows(6)

            map(template.layout)

            set('-', XMaterial.GRAY_STAINED_GLASS_PANE) { name = " " }


            // 元素点击事件
            onClick { event ->
                val bukkitPlayer = event.clicker
                when (event.clickEvent().click) {
                    LEFT -> bukkitPlayer.giveItem(sertralineItemBuilder(element.key, bukkitPlayer, amount = 1))
                    SHIFT_LEFT -> {
                        val itemStack = sertralineItemBuilder(element.key, bukkitPlayer)!!
                        itemStack.amount = (itemStack.maxStackSize)
                        bukkitPlayer.giveItem(itemStack)
                    }

                    RIGHT -> {}
                    SHIFT_RIGHT -> {}
                    MIDDLE -> {}
                    else -> {}
                }

            }

        }
    }
}