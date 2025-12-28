package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootTemplates
import org.bukkit.entity.Player

data class LootInstance(
    val templateID: String,
    val loc: LootLocation,
    val elements: LinkedHashMap<Int, LootElement>,
    val searches: LinkedHashMap<String, SearchStat>,
) {
    val template get() = lootTemplates[templateID]
    fun getSlotItem(int: Int): LootElement? {
        return elements[int]
    }
    fun resetPlayerSearch(player: Player) {
        searches[player.uniqueId.toString()]?.reset()
    }
}