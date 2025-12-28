package io.github.zzzyyylllty.lithiumcarbon.data

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import javax.script.CompiledScript

data class LootElement(
//    var searchEnd: Long? = null,
//    var searcherUUID: String, // 当玩家关闭容器时，检测玩家UUID的战利品并重置搜索进度
    var displayItem: ItemStack? = null,
    val exps: Double = 0.0,
    val items: List<LootItem>? = null,
    val kether: List<List<String>>? = null,
    val javaScript: List<CompiledScript>? = null,
) {
    fun getDisplayItem(isSearched: Boolean) {
        if (isSearched) // TODO 使用定义的搜索中物品
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
}

//enum class LootElementStat {
//    NOT_SEARCHED,
//    SEARCHING,
//    SEARCHED
//}