package io.github.zzzyyylllty.lithiumcarbon.data.define

import io.github.zzzyyylllty.lithiumcarbon.data.Condition
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.util.MultiBlock
import org.bukkit.block.Block
import org.bukkit.entity.Player

interface LootDefine {
    val type: String
    val weight: Int
        get() = 0
    val blocks: HashSet<MultiBlock>
    val condition: Condition?
    fun isValidLocation(location: LootLocation, block: Block, player: Player): Boolean
    fun validateCondition(location: LootLocation, block: Block, player: Player): Boolean
}

fun LootDefines.getMaxMatchingWeight(location: LootLocation, block: Block, player: Player): Int? {
    var maxWeight: Int? = null
    defines.forEach {
        if (it.value.isValidLocation(location, block, player)) {
            val w = it.value.weight
            if (maxWeight == null || w > maxWeight) maxWeight = w
        }
    }
    return maxWeight
}

data class LootDefines(
    val defines: LinkedHashMap<String, LootDefine> = linkedMapOf()
) {
    fun isValidLocation(location: LootLocation, block: Block, player: Player): Boolean {
        defines.forEach {
            if (it.value.isValidLocation(location, block, player)) return true
        }
        return false
    }
}