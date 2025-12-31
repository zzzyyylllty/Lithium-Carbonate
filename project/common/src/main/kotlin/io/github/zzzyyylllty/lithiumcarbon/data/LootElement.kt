package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.function.kether.evalKether
import io.github.zzzyyylllty.lithiumcarbon.util.LootGUIHelper
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.platform.util.giveItem
import javax.script.CompiledScript
import javax.script.SimpleBindings
import kotlin.math.roundToInt

data class LootElement(
//    var searchEnd: Long? = null,
//    var searcherUUID: String, // 当玩家关闭容器时，检测玩家UUID的战利品并重置搜索进度
    var displayItem: LootItem? = null,
    val exps: Double = 0.0,
    val items: List<LootItem>? = null,
    val kether: List<String>? = null,
    val javaScript: CompiledScript? = null,
    val searchTime: Long = 0,
    val skipSearch: Boolean = false,
) {
    fun getDisplayItem(stat: LootElementStat, player: Player?): ItemStack {
        return when (stat) {
            LootElementStat.NOT_SEARCHED -> LootGUIHelper.unsearch.build(player, 1)
            LootElementStat.SEARCHING -> LootGUIHelper.searching.build(player, 1)
            LootElementStat.SEARCHED -> (displayItem ?: items?.firstOrNull() ?: LootGUIHelper.undefinedItem).build(player, 1)
        }
    }

    fun applyToPlayer(player: Player) {
        items?.forEach { item ->
            player.giveItem(item.build(player))
        }
        player.giveExp(exps.roundToInt())
        if (kether != null || javaScript != null) {
            val data = defaultData.toMutableMap()
            data.putAll(
                linkedMapOf(
                    "displayItem" to displayItem,
                    "exps" to exps,
                    "element" to this,
                    "searchTime" to searchTime,
                    "skipSearch" to skipSearch
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
    SEARCHED
}