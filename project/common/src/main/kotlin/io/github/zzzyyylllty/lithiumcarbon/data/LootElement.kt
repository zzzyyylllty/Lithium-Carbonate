package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.util.LootGUIHelper
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.platform.util.giveItem
import javax.script.CompiledScript

data class LootElement(
//    var searchEnd: Long? = null,
//    var searcherUUID: String, // 当玩家关闭容器时，检测玩家UUID的战利品并重置搜索进度
    val slot: Int,
    var displayItem: LootItem? = null,
    val exps: Double = 0.0,
    val items: List<LootItem>? = null,
    val kether: List<List<String>>? = null,
    val javaScript: List<CompiledScript>? = null,
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
    }

}
//    fun isSearchEnd(): Boolean {
//        return (searchEnd ?: 0) <= System.currentTimeMillis()
//    }
//    fun getStat(): LootElementStat {
//        return if (searchEnd == null) {
//            LootElementStat.NOT_SEARCHED
//        } else if (!isSearchEnd()) {
//            LootElementStat.SEARCHING
//        } else {
//            LootElementStat.SEARCHED
//        }
//    }


enum class LootElementStat {
    NOT_SEARCHED,
    SEARCHING,
    SEARCHED
}