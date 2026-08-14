package io.github.zzzyyylllty.lithiumcarbon.data.load

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootIndex
import io.github.zzzyyylllty.lithiumcarbon.data.index.LootIndex
import io.github.zzzyyylllty.lithiumcarbon.data.index.LootIndexCondition
import io.github.zzzyyylllty.lithiumcarbon.data.index.LootIndexConditionRegistry
import io.github.zzzyyylllty.lithiumcarbon.data.index.LootIndexRule
import io.github.zzzyyylllty.lithiumcarbon.data.index.registerBuiltinIndexConditions
import io.github.zzzyyylllty.lithiumcarbon.logger.infoL
import io.github.zzzyyylllty.lithiumcarbon.logger.warningL
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import java.io.File

fun loadLootIndexes() {
    infoL("LootIndexLoad")
    registerBuiltinIndexConditions()
    if (!File(getDataFolder(), "loot-indexes.yml").exists()) {
        warningL("LootIndexRegen")
        releaseResourceFile("loot-indexes.yml")
    }
    val file = File(getDataFolder(), "loot-indexes.yml")
    val map = multiExtensionLoader(file) ?: run {
        devLog("loot-indexes.yml is empty, skipping.")
        return
    }
    val enabled = map["enabled"] as? Boolean ?: false
    val rules = (map["when"] as? List<*>)?.mapNotNull { parseLootIndexRule(it) } ?: emptyList()
    lootIndex = LootIndex(enabled, rules)
    devLog("loot-indexes loaded: enabled=$enabled, rules=${rules.size}")
}

fun parseLootIndexRule(raw: Any?): LootIndexRule? {
    val map = raw as? Map<*, *> ?: return null
    val checks = mutableListOf<LootIndexCondition>()
    val ifRaw = map["if"]
    if (ifRaw != null && ifRaw !is Map<*, *>) {
        warningL("WarningInvalidIndexCondition", "if")
    }
    (ifRaw as? Map<*, *>)?.forEach { (typeKey, value) ->
        val type = typeKey.toString()
        val builder = LootIndexConditionRegistry.get(type)
        if (builder == null) {
            warningL("WarningUnknownIndexCondition", type)
            return@forEach
        }
        val condition = builder.build(value)
        if (condition == null) {
            warningL("WarningInvalidIndexCondition", type)
            return@forEach
        }
        checks.add(condition)
    }
    val open = map["open"]?.toString()
    val pass = map["pass"] as? Boolean ?: false
    val thenRules = when (val thenRaw = map["then"]) {
        is List<*> -> thenRaw.mapNotNull { parseLootIndexRule(it) }
        else -> listOfNotNull(parseLootIndexRule(thenRaw))
    }
    val elseRules = when (val elseRaw = map["else"]) {
        is List<*> -> elseRaw.mapNotNull { parseLootIndexRule(it) }
        else -> listOfNotNull(parseLootIndexRule(elseRaw))
    }
    return LootIndexRule(checks, open, thenRules, elseRules, pass)
}
