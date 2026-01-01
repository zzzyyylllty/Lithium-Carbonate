package io.github.zzzyyylllty.lithiumcarbon.api

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootCaches
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootDefines
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootItems
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootItemsDef
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootMap
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootTemplates
import io.github.zzzyyylllty.lithiumcarbon.data.LocationHelper
import io.github.zzzyyylllty.lithiumcarbon.data.LootInstance
import io.github.zzzyyylllty.lithiumcarbon.data.LootItem
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.data.LootTemplate
import io.github.zzzyyylllty.lithiumcarbon.data.define.LootDefines
import io.github.zzzyyylllty.sertraline.api.SertralineAPI
import org.bukkit.Location
import taboolib.common.platform.command.location
import taboolib.platform.util.toBukkitLocation


public class LithiumCarbonAPIImpl: LithiumCarbonAPI {
    public val INSTANCE = LithiumCarbonAPIImpl()
}


interface LithiumCarbonAPI {
    fun getLootMap(): MutableMap<LootLocation, LootInstance> {
        return lootMap
    }
    fun getLootTemplates(): MutableMap<String, LootTemplate> {
        return lootTemplates
    }
    fun getLootDefines(): MutableMap<String, LootDefines> {
        return lootDefines
    }
    fun getLootCaches(): MutableMap<LootLocation, LootTemplate> {
        return lootCaches
    }
    fun getLootItems(): MutableMap<Char, LootItem> {
        return lootItems
    }
    fun getLootItemsDef(): MutableMap<String, LootItem> {
        return lootItemsDef
    }
    fun updateInstance(bukkitLocation: Location) {
        val location = LocationHelper.toLootLocation(bukkitLocation)
        val instance = lootMap[location]
        instance?.update()
    }
}