package io.github.zzzyyylllty.lithiumcarbon.data.define

import io.github.zzzyyylllty.lithiumcarbon.data.Condition
import io.github.zzzyyylllty.lithiumcarbon.data.LocationHelper
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.data.LootVector
import io.github.zzzyyylllty.lithiumcarbon.util.WorldGuardHelper
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import io.github.zzzyyylllty.lithiumcarbon.util.loc
import org.bukkit.block.Block
import org.bukkit.entity.Player
import kotlin.text.matches

class SpecifyDefine(val locations: LinkedHashMap<String, HashSet<LootVector>>, val worldRegex: Regex?, override val blocks: HashSet<String>, override val condition: Condition?): LootDefine {

    override val type: String = "world"

    override fun isValidLocation(location: LootLocation, block: Block, player: Player): Boolean {

        val blockWorld = block.world.name

//        locations.forEach {
//            if (if (regex) blockWorld.matches(it.world.toRegex()) else blockWorld == it.world) {
//                devLog("Specify define passed.")
//
//                if (blocks.contains(block.type.name)) return validateCondition(location, block, player)
//            }
//        }

        val blockVector = LocationHelper.toLootVector(block.location)

        if (worldRegex != null) {
            locations.forEach { (world, vector) ->
                if (blockWorld.matches(worldRegex)) {
                    if (vector.contains(blockVector)) return true
                }
            }
        } else {
            locations[blockWorld]?.let { vector ->
                if (vector.contains(blockVector)) return true
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
