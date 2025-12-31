package io.github.zzzyyylllty.lithiumcarbon.data.define

import io.github.zzzyyylllty.lithiumcarbon.data.Condition
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import org.bukkit.block.Block
import org.bukkit.entity.Player

interface LootDefine {
    val type: String
    val blocks: HashSet<String>
    val condition: Condition?
    fun isValidLocation(location: LootLocation, block: Block, player: Player): Boolean
    fun validateCondition(location: LootLocation, block: Block, player: Player): Boolean
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