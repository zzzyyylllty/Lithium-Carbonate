package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.logger.severeL
import io.github.zzzyyylllty.lithiumcarbon.util.asNumberFormat
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import org.bukkit.entity.Player
import javax.script.CompiledScript
import kotlin.math.roundToLong

data class LootTable(
    val pools: List<LootPool>,
    val agents: Agents?,
) {
    fun apply(bypassConditions: Boolean = false, extraVariables: Map<String, Any?>, player: Player, availableSlots: Set<Int>, shuffleLoot: Boolean = false): LinkedHashMap<Int, LootElement> {
        val elements = mutableListOf<LootElement>()
        val availableSlots = availableSlots.toMutableSet()
        val availableSlotsCount = availableSlots.size

        if (availableSlotsCount == 0) {
            severeL("ErrorNoAvailableSlots")
            return linkedMapOf()
        }
        pools.forEach { pool ->
            if (availableSlotsCount >= elements.size) {
                pool.roll(bypassConditions, extraVariables, player)?.let { elements += it }
            } else {
                devLog("No space left to roll. skipping.")
                return@forEach
            }
        }
        val sloted = LinkedHashMap<Int, LootElement>()
        if (shuffleLoot) {
            elements.forEach { loot ->
                if (availableSlots.isEmpty()) return@forEach
                availableSlots.random().let {
                    sloted[it] = loot
                    availableSlots.remove(it)
                }
            }
        } else {
            var slot = 0
            for (element in elements) {
                if (!availableSlots.contains(slot)) break
                sloted[slot] = element
                availableSlots.remove(slot)
                slot++
            }
        }
        return sloted
    }
}

data class LootPool(
    val rolls: String,
    val conditions: Condition?,
    val loots: List<Loots>,
    val agent: Agents?,
) {
    fun roll(bypassConditions: Boolean = false, extraVariables: Map<String, Any?>, player: Player): List<LootElement>? {

        val elements = mutableListOf<LootElement>()

        // 不满足条件直接返回
        if (!bypassConditions) if (conditions?.validate(extraVariables, player) == false) {
            devLog("Loot Pool condition not met, return.")
            return null
        }


        loots.forEach { loot ->
            devLog("parsing $loot.")
            elements += loot.parseLoot(player)
        }

        return elements

    }
}

data class Loots(
    var displayItem: LootItem? = null,
    val exps: String,
    val items: List<LootItem>? = null,
    val kether: List<String>? = null,
    val javaScript: CompiledScript? = null,
    val searchTime: String = "0",
    val skipSearch: Boolean = false,
    val weight: String?,
) {
    fun getWeight(player: Player): Double {
        return weight?.asNumberFormat(player) ?: 1.0
    }

    fun parseLoot(player: Player): LootElement {
        return LootElement(
            displayItem = displayItem,
            exps = exps.asNumberFormat(player),
            items = items,
            kether = kether,
            javaScript = javaScript,
            searchTime = searchTime.asNumberFormat(player).roundToLong(),
            skipSearch = skipSearch
        )
    }
}