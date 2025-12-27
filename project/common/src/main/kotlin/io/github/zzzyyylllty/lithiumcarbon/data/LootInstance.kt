package io.github.zzzyyylllty.lithiumcarbon.data

import org.bukkit.entity.Player

data class LootInstance(
    val elements: LinkedHashMap<Int, LootElement>,
    val searches: LinkedHashMap<String, SearchStat>,
) {
    fun getSlotItem(int: Int): LootElement? {
        return elements[int]
    }
    fun resetPlayerSearch(player: Player): LootElement? {
        return searches[player.uniqueId.toString()].reset()
    }
}