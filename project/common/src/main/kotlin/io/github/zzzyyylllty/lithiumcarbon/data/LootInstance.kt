package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.LootInstanceKey
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootMap
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootTemplates
import io.github.zzzyyylllty.lithiumcarbon.gui.openedLootLocation
import io.github.zzzyyylllty.lithiumcarbon.util.serialize.toUUID
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import java.util.UUID
import kotlin.math.roundToLong

data class LootInstance(
    var templateID: String,
    var loc: LootLocation,
    var elements: MutableMap<Int, LootElement?>,
    var searches: MutableMap<String, SearchStat>,
    var nextRefresh: Long?,
    val isPrivate: Boolean = false,
    val playerId: UUID? = null,
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

    fun getSearchingSlots(player: Player): Set<Int>? {
        return searches[player.uniqueId.toString()]?.searches?.filter { !it.value.isSearchEnded() }?.keys
    }
    fun removeSearchingSlots(player: Player, slot: Int) {
        searches[player.uniqueId.toString()]?.searches?.remove(slot)
    }
    fun resetPlayerSearch(player: Player) {
        searches[player.uniqueId.toString()]?.reset()
    }

    /**
     * @return null - 需要更新
     * @return LootInstance - 不需要更新
     */
    fun checkUpdate(): LootInstance? {
        val key = LootInstanceKey(loc, playerId)
        nextRefresh?.let {
            if (it <= System.currentTimeMillis()) {
                update()
            }
        } ?: return this
        return lootMap[key]
    }

    fun update() {
        val key = LootInstanceKey(loc, playerId)
        val players = if (playerId != null) {
            // 私有箱子，只关闭该玩家的库存
            openedLootLocation.filter { it.key == playerId && it.value == loc }
        } else {
            // 共享箱子，关闭所有在该位置的玩家库存
            openedLootLocation.filter { it.value == loc }
        }
        submit {
            players.forEach {
                Bukkit.getPlayer(it.key)?.closeInventory()
            }
            lootMap.remove(key)
        }
    }

}