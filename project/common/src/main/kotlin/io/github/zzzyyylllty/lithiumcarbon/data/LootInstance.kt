package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootMap
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootTemplates
import org.bukkit.entity.Player

data class LootInstance(
    var templateID: String,
    var loc: LootLocation,
    var elements: LinkedHashMap<Int, LootElement?>,
    var searches: LinkedHashMap<String, SearchStat>,
) {
    val template get() = lootTemplates[templateID]

    fun getSlotItem(int: Int): LootElement? {
        return elements[int]
    }

    fun getSearchStatRaw(player: Player, location: Int): SearchStat? {
        return searches[player.uniqueId.toString()]
    }

    fun getSearchStat(player: Player, element: LootElement, slot: Int, instance: LootInstance): LootElementStat {
        return if (instance.elements[slot] == null) LootElementStat.NOITEM
        else if (element.skipSearch) LootElementStat.SEARCHED
        else if (getSearchStatRaw(player, slot)?.isSearchEnded(slot) ?: return LootElementStat.NOT_SEARCHED)
            LootElementStat.SEARCHED else LootElementStat.SEARCHING
    }

    fun startSearch(player: Player, location: Int, ms: Long, skip: Boolean = false) {
        val searches = searches.getOrPut(player.uniqueId.toString()) {
            SearchStat(linkedMapOf())
        }.searches
        searches[location] = SingleSearchStat(location, System.currentTimeMillis() + ms, skip)
    }

    fun resetPlayerSearch(player: Player) {
        searches[player.uniqueId.toString()]?.reset()
    }

}