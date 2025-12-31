package io.github.zzzyyylllty.lithiumcarbon.data.define

import io.github.zzzyyylllty.lithiumcarbon.data.Condition
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.util.WorldGuardHelper
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import org.bukkit.block.Block
import org.bukkit.entity.Player

class SquareDefine(val from: LootLocation, val to: LootLocation, override val blocks: List<String>, override val condition: Condition?): LootDefine {

    override val type: String = "square"

    override fun isValidLocation(location: LootLocation, block: Block, player: Player): Boolean {

        if (!block.world.name.contains(from.world.toRegex())) {
            devLog("World not met,return false.")
            return false
        }

        if (block.x in from.x..to.x && block.y in from.y..to.y && block.z in from.z..to.z) {

            devLog("Square define passed.")

            if (blocks.contains(block.type.name)) {
                return validateCondition(location, block, player)
            }
            return false

        } else {
            return false
        }

    }

    override fun validateCondition(location: LootLocation, block: Block, player: Player): Boolean {

        condition?.let {
            val extraVariable = mapOf<String, Any?>(
                "block" to block.type,
                "type" to block.type.name,
                "x" to block.x,
                "u" to block.y,
                "z" to block.z,
                "world" to block.world.name,
                "player" to player,
            )
            if (!it.validate(extraVariable, player)) {
                devLog("Condition not met,return false.")
                return false
            }
        }
        return true
    }
}
