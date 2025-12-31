package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon
import org.bukkit.block.Block
import org.bukkit.entity.Player


data class LootTemplate (
    val id: String,
    val name: String,
    val title: String,
    val rows: Int,
    val layout: List<String>,
    val availableSlots: Set<Int>,
    val lootTable: LootTable,
    val agents: Agents?,
    val options: LootTemplateOptions,
) {
    fun createInstance(block: Block, player: Player, bypassCondition: Boolean = false): LootInstance {
        return LootInstance(
            templateID = id,
            loc = LocationHelper.toLootLocation(block.location),
            elements = generateElements(player, bypassCondition),
            searches = linkedMapOf()
        )
    }
    fun generateElements(player: Player, bypassCondition: Boolean = false): LinkedHashMap<Int, LootElement?> {
        return lootTable.apply(bypassCondition, getExtraVariables(player), player, availableSlots, shuffleLoot = options.shuffleLoot)
    }
    fun getExtraVariables(player: Player): Map<String, Any?> {
        return linkedMapOf<String, Any?>(
            "template" to this,
            "id" to id,
            "name" to name,
            "title" to title,
            "player" to player,
        )
    }
}

data class LootTemplateOptions(
    val removeLore: Boolean,
    val addLore: List<String>?,
    val shuffleLoot: Boolean,
)