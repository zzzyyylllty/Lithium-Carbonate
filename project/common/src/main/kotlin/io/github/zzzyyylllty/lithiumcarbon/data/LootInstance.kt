package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootMap
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootTemplates
import io.github.zzzyyylllty.lithiumcarbon.gui.openedLootLocation
import io.github.zzzyyylllty.lithiumcarbon.util.serialize.toUUID
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import kotlin.math.roundToLong

data class LootInstance(
    var templateID: String,
    var loc: LootLocation,
    var elements: LinkedHashMap<Int, LootElement?>,
    var searches: LinkedHashMap<String, SearchStat>,
    var nextRefresh: Long?,
) {
    val template get() = lootTemplates[templateID]

    fun getSlotItem(int: Int): LootElement? {
        return elements[int]
    }

    fun getSearchStatRaw(player: Player): SearchStat? {
        return searches[player.uniqueId.toString()]
    }

    fun getSearchStat(player: Player, element: LootElement, slot: Int, instance: LootInstance): LootElementStat {
        return if (instance.elements[slot] == null) LootElementStat.NOITEM
        else if (element.skipSearch) LootElementStat.SEARCHED
        else if (getSearchStatRaw(player)?.isSearchEnded(slot) ?: return LootElementStat.NOT_SEARCHED)
            LootElementStat.SEARCHED else LootElementStat.SEARCHING
    }

    fun startSearch(player: Player, location: Int, time: Double, skip: Boolean = false) {
        val searches = searches.getOrPut(player.uniqueId.toString()) {
            SearchStat(linkedMapOf())
        }.searches
        searches[location] = SingleSearchStat(location, System.currentTimeMillis() + (time * 1000).roundToLong(), skip)
    }

    fun resetPlayerSearch(player: Player) {
        searches[player.uniqueId.toString()]?.reset()
    }

    /**
     * @return null - 需要更新
     * @return LootInstance - 不需要更新
     */
    fun checkUpdate(): LootInstance? {
        nextRefresh?.let {
            if (it <= System.currentTimeMillis()) {
                update()
            }
        } ?: return this
        return lootMap[loc]
    }

    fun update() {
        openedLootLocation
            .filter{ it.value == loc }
            .forEach {
                Bukkit.getPlayer(it.key.toUUID())?.closeInventory()
            }
        lootMap.remove(loc)
    }

}