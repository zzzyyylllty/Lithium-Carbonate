package io.github.zzzyyylllty.lithiumcarbon.data.define

import io.github.zzzyyylllty.lithiumcarbon.data.Condition
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.util.MultiBlock
import io.github.zzzyyylllty.lithiumcarbon.util.WorldGuardHelper
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import io.github.zzzyyylllty.lithiumcarbon.util.validate
import org.bukkit.block.Block
import org.bukkit.entity.Player

class WGDefine(val regions: List<String>, val regionsRegex: List<Regex>?, override val weight: Int, override val blocks: HashSet<MultiBlock>, override val condition: Condition?): LootDefine {

    override val type: String = "worldguard"

    override fun isValidLocation(location: LootLocation, block: Block, player: Player): Boolean {

        if (regions.isNotEmpty()) {

            val required = WorldGuardHelper.checkLocationRegion(location)

            if (regionsRegex != null) required?.forEach {
                for (r in regionsRegex) {
                    if (it.matches(r)) {
                        devLog("WG define passed: $it")
                        if (blocks.validate(block)) return validateCondition(location, block, player)
                    }
                }
            } else required?.forEach {
                if (regions.contains(it)) {
                    devLog("WG define passed: $it")
                    if (blocks.validate(block)) return validateCondition(location, block, player)
                }
            }

            return false

        } else {
            return false
        }

    }

    override fun validateCondition(location: LootLocation, block: Block, player: Player): Boolean {

        condition?.let {
            val extraVariable = mapOf<String, Any?>(
                "block" to block,
                "type" to block.type.name,
                "x" to block.x,
                "y" to block.y,
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
