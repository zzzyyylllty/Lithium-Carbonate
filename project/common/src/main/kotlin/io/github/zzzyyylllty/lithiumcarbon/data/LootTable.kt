package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.logger.severeL
import io.github.zzzyyylllty.lithiumcarbon.util.asNumberFormat
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.LinkedList
import javax.script.CompiledScript

data class LootTable(
    val pools: List<LootPool>,
    val agent: Agents?,
) {
    fun apply(bypassConditions: Boolean = false, extraVariables: Map<String, Any>,player: Player): List<LootElement> {
        val elements = mutableListOf<LootElement>()
        pools.forEach { pool ->
            pool.roll(bypassConditions, extraVariables, player)?.let { elements += it }
        }
        return elements
    }
}

data class LootPool(
    val rolls: String,
    val conditions: Condition?,
    val loots: List<Loots>,
    val agent: Agents?,
) {
    fun roll(bypassConditions: Boolean = false, extraVariables: Map<String, Any>, player: Player, availableSlots: List<Int>): List<LootElement>? {

        val elements = mutableListOf<LootElement>()
        val availableSlots = availableSlots.toMutableList()

        // 不满足条件直接返回
        if (!bypassConditions) if (conditions?.validate(extraVariables, player) == false) {
            devLog("Loot Pool condition not met, return.")
            return null
        }

        if (availableSlots.isEmpty()) {
            severeL("ErrorNoAvailableSlots")
            return null
        }

        loots.forEach { loot ->

            if (availableSlots.isEmpty()) {
                devLog("available slots is safety-empty. skipping.")
                return@forEach
            }

            availableSlots.random().let { slot ->
                devLog("Loot Pool available slot $slot, parsing.")
                availableSlots.remove(slot)
                elements += loot.parseLoot(slot, player)
            }

        }

        return elements

    }
}

data class Loots(
    var displayItem: LootItem? = null,
    val exps: String,
    val items: List<LootItem>? = null,
    val kether: List<List<String>>? = null,
    val javaScript: List<CompiledScript>? = null,
    val searchTime: Long = 0,
    val skipSearch: Boolean = false,
    val weight: Double?,
    val dynamicWeight: String?,
) {
    fun getWeight(player: Player): Double {
        return weight ?: dynamicWeight.asNumberFormat(player)
    }

    fun parseLoot(slot: Int, player: Player): LootElement {
        return LootElement(
            slot = slot,
            displayItem = displayItem,
            exps = exps.asNumberFormat(player),
            items = items,
            kether = kether,
            javaScript = javaScript,
            searchTime = searchTime,
            skipSearch = skipSearch
        )
    }
}