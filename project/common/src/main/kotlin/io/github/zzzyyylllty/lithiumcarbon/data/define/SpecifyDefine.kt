package io.github.zzzyyylllty.lithiumcarbon.data.define

import io.github.zzzyyylllty.lithiumcarbon.data.Condition
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.util.WorldGuardHelper
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import org.bukkit.block.Block
import org.bukkit.entity.Player
import kotlin.text.matches

class SpecifyDefine(val locations: List<LootLocation>, val regex: Boolean,override val blocks: List<String>, override val condition: Condition?): LootDefine {

    override val type: String = "world"

    override fun isValidLocation(location: LootLocation, block: Block, player: Player): Boolean {

        val blockWorld = block.world.name

        locations.forEach {
            if (if (regex) blockWorld.matches(it.world.toRegex()) else blockWorld == it.world) {
                devLog("World define passed.")

                if (blocks.contains(block.type.name)) return validateCondition(location, block, player)
            }
        }

        return false
    }

    override fun validateCondition(location: LootLocation, block: Block, player: Player): Boolean {

        condition?.let {
            val extraVariable = mapOf<String, Any?>(
                "block" to block,
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
