package io.github.zzzyyylllty.lithiumcarbon.data.define

import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.util.WorldGuardHelper

class WGDefine(val regions: List<String>, val regex: Boolean): LootDefine {

    override val type: String = "worldguard"

    override fun isValidLocation(location: LootLocation): Boolean {

        if (regions.isNotEmpty()) {

            val required = WorldGuardHelper.checkLocationRegion(location)

            if (regex) required?.forEach {
                for (r in regions) {
                    if (it.matches(r.toRegex())) return true
                }
            } else required?.forEach {
                if (regions.contains(it)) return true
            }

            return false

        } else {
            return false
        }

    }
}
