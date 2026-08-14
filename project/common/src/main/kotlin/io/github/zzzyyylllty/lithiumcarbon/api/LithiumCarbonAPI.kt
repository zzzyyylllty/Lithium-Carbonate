package io.github.zzzyyylllty.lithiumcarbon.api

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootCaches
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootDefines
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootItems
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootItemsDef
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.LootInstanceKey
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootMap
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootTemplates
import io.github.zzzyyylllty.lithiumcarbon.data.LocationHelper
import io.github.zzzyyylllty.lithiumcarbon.data.LootInstance
import io.github.zzzyyylllty.lithiumcarbon.data.LootItem
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.data.LootTemplate
import io.github.zzzyyylllty.lithiumcarbon.data.define.LootDefines
import io.github.zzzyyylllty.lithiumcarbon.data.index.LootIndexConditionRegistry
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockFlowRegistry
import io.github.zzzyyylllty.sertraline.api.SertralineAPI
import org.bukkit.Location
import taboolib.common.platform.command.location
import taboolib.platform.util.toBukkitLocation
import java.util.Collections


public object LithiumCarbonAPIImpl: LithiumCarbonAPI {
}


interface LithiumCarbonAPI {
    fun getLootMap(): Map<LootInstanceKey, LootInstance> {
        return Collections.unmodifiableMap(lootMap)
    }
    fun getLootTemplates(): Map<String, LootTemplate> {
        return Collections.unmodifiableMap(lootTemplates)
    }
    fun getLootDefines(): Map<String, LootDefines> {
        return Collections.unmodifiableMap(lootDefines)
    }
    fun getLootCaches(): Map<LootLocation, LootTemplate> {
        return Collections.unmodifiableMap(lootCaches)
    }
    fun getLootItems(): Map<Char, LootItem> {
        return Collections.unmodifiableMap(lootItems)
    }
    fun getLootItemsDef(): Map<String, LootItem> {
        return Collections.unmodifiableMap(lootItemsDef)
    }
    fun updateInstance(bukkitLocation: Location) {
        val location = LocationHelper.toLootLocation(bukkitLocation)
        val key = LootInstanceKey(location, null) // 更新共享实例
        val instance = lootMap[key]
        instance?.update()
    }
    fun getUnlockFlowRegistry(): UnlockFlowRegistry {
        return UnlockFlowRegistry
    }
    fun getLootIndexConditionRegistry(): LootIndexConditionRegistry {
        return LootIndexConditionRegistry
    }
}