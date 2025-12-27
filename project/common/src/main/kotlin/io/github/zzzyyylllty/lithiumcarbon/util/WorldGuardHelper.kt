package io.github.zzzyyylllty.lithiumcarbon.util

import com.sk89q.worldguard.WorldGuard
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.config
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.logger.severeL

object WorldGuardHelper {
    val isHooked: Boolean by lazy {
        DependencyHelper.wg && config.getBoolean("hook.worldguard", true)
    }
    fun checkLocationRegion(location: LootLocation): List<String>? {
        if (!isHooked) return null
        try {
            val regionContainer = WorldGuard.getInstance().platform.regionContainer
            val query = regionContainer.createQuery()
            val currentRegions = query.getApplicableRegions(location.toWGLocation()).regions.map { it.id }
            return currentRegions
        } catch (e: ClassNotFoundException) {
            severeL("WorldGuardNotFoundException")
            return null
        } catch (e: NoClassDefFoundError) {
            severeL("WorldGuardNotFoundException")
            return null
        }
    }
}