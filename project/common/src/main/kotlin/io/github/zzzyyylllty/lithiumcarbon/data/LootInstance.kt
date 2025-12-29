package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootMap
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

    fun getSearchStatRaw(player: Player, location: Int): SearchStat? {
        return searches[player.uniqueId.toString()]
    }

    fun getSearchStat(player: Player, element: LootElement): LootElementStat {
        return if (element.skipSearch) LootElementStat.SEARCHED else if (getSearchStatRaw(player, element.slot)?.isSearchEnded(element.slot) ?: return LootElementStat.NOT_SEARCHED) LootElementStat.SEARCHING else LootElementStat.NOT_SEARCHED
    }

    fun startSearch(player: Player, location: Int, ms: Long) {
        searches.getOrPut(player.uniqueId.toString()) {
            SearchStat(linkedMapOf())
        }.searches
    }

    fun resetPlayerSearch(player: Player) {
        searches[player.uniqueId.toString()]?.reset()
    }

    fun refresh(): LootInstance? {
        return lootMap[loc]
    }

}