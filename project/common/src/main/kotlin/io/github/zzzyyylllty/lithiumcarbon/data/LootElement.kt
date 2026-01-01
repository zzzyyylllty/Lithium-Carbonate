package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.function.kether.evalKether
import io.github.zzzyyylllty.lithiumcarbon.function.player.sendComponent
import io.github.zzzyyylllty.lithiumcarbon.util.LootGUIHelper
import io.github.zzzyyylllty.lithiumcarbon.util.mmUtil
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.platform.util.asLangText
import taboolib.platform.util.giveItem
import javax.script.CompiledScript
import javax.script.SimpleBindings
import kotlin.math.roundToInt

data class LootElement(
    var displayItem: LootItem? = null,
    val exps: Double = 0.0,
    val items: List<LootItem>? = null,
    val kether: List<String>? = null,
    val javaScript: CompiledScript? = null,
    val searchTime: Double = 0.0,
    val skipSearch: Boolean = false,
) {
    fun getDisplayItem(stat: LootElementStat, player: Player?, options: LootTemplateOptions): ItemStack? {
        return when (stat) {
            LootElementStat.NOT_SEARCHED -> LootGUIHelper.unsearch.build(player, 1)
            LootElementStat.SEARCHING -> LootGUIHelper.searching.build(player, 1)
            LootElementStat.SEARCHED -> {
                val item = (displayItem ?: items?.firstOrNull() ?: LootGUIHelper.undefinedItem)
                val lore = item.parameters?.let { (it["lore"] as List<String>?)?.toMutableList() }
                if (options.removeLore) {
                    lore?.clear()
                }
                if (options.addLore != null && options.addLore.isNotEmpty()) {
                    lore?.addAll(options.addLore)
                }
                item.parameters?.let { it["lore"] = lore }
                item.build(player, 1)
            }
            LootElementStat.NOITEM -> null
        }
    }

    fun applyToPlayer(player: Player,template: LootTemplate) {
        items?.forEach { lItem ->
            val item = lItem.build(player)
            player.giveItem(item)
            player.sendComponent(player.asLangText("Claim", template.name, mmUtil.serialize(item.displayName())))
        }
        val exp = exps.roundToInt()
        if (exp != 0) {
            player.giveExp(exp)
            player.sendComponent(player.asLangText("ClaimExp", template.name, exp))
        }
        if (kether != null || javaScript != null) {
            val data = defaultData.toMutableMap()
            data.putAll(
                linkedMapOf(
                    "displayItem" to displayItem,
                    "exps" to exps,
                    "element" to this,
                    "searchTime" to searchTime,
                    "skipSearch" to skipSearch,
                    "player" to player,
                )
            )
            kether?.evalKether(player, data)
            javaScript?.eval(SimpleBindings(data))
        }
    }

}

enum class LootElementStat {
    NOT_SEARCHED,
    SEARCHING,
    SEARCHED,
    NOITEM
}