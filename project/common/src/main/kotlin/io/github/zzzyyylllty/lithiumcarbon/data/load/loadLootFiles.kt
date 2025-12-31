package io.github.zzzyyylllty.lithiumcarbon.data.load

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.config
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootTemplates
import io.github.zzzyyylllty.lithiumcarbon.data.LootItem
import io.github.zzzyyylllty.lithiumcarbon.data.LootPool
import io.github.zzzyyylllty.lithiumcarbon.data.LootTable
import io.github.zzzyyylllty.lithiumcarbon.data.LootTemplate
import io.github.zzzyyylllty.lithiumcarbon.data.LootTemplateOptions
import io.github.zzzyyylllty.lithiumcarbon.data.Loots
import io.github.zzzyyylllty.lithiumcarbon.logger.infoL
import io.github.zzzyyylllty.lithiumcarbon.logger.severeL
import io.github.zzzyyylllty.lithiumcarbon.logger.warningL
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import io.github.zzzyyylllty.lithiumcarbon.util.toBooleanTolerance
import org.bukkit.entity.Item

// import org.yaml.snakeyaml.Yaml
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import taboolib.common5.compileJS
import java.io.File
import kotlin.collections.forEach
import kotlin.collections.set


fun loadLootFiles() {
    infoL("LootLoad")
    if (!File(getDataFolder(), "loots").exists()) {
        warningL("LootRegen")
        releaseResourceFile("loots/test.yml")
    }
    val files = File(getDataFolder(), "lore-formats").listFiles()
    for (file in files) {
        // If directory load file in it...
        if (file.isDirectory) file.listFiles()?.forEach {
            loadLootFile(it)
        }
        else loadLootFile(file)
    }
}
fun loadLootFile(file: File) {
    devLog("Loading file ${file.name}")

    if (file.isDirectory) file.listFiles()?.forEach {
        loadLootFile(it)
    } else {
        if (!checkRegexMatch(file.name, (config["file-load.lore-format"] ?: ".*").toString())) {
            devLog("${file.name} not match regex, skipping...")
            return
        }
        val map = multiExtensionLoader(file)
        if (map != null) for (it in map.entries) {
            val key = it.key
            val value = map[key]
            (value as Map<String, Any?>?)?.let { arg -> loadLoot(key, arg) }
        } else {
            devLog("Map is null, skipping.")
        }
    }
}

fun loadLoot(key: String, arg: Map<String, Any?>) {
    val c = ConfigUtil

    val options = LootTemplateOptions(
        removeLore = c.getDeep(arg, "options.remove-lore") as? Boolean? ?: config.getBoolean("default-options.remove-lore", false),
        addLore = c.getDeep(arg, "options.add-lore") as? List<String>? ?: config.getStringList("default-options.add-lore"),
        shuffleLoot = c.getDeep(arg, "options.shuffle-loot") as? Boolean? ?: config.getBoolean("default-options.shuffle-loot", false),
    )

    val layoutP = c.getDeep(arg, "display.layout").asListEnhanced() ?: config.getStringList("default-layout")
    val layout = layoutP.ifEmpty { listOf("         ", "         ", "         ") }

    val availableSlots = (c.getDeep(arg, "display.layout") as List<Int>?)?.toMutableList() ?: mutableListOf()

    var currentLine = 0
    if (availableSlots.isEmpty()) {
        for (line in layout) {
            currentLine++
            var currentChar = 0
            for (char in line) {
                currentChar++
                val location = ((currentLine - 1) * 9 + currentChar) - 1
                availableSlots.add(location)
                // devLog("($currentLine,$currentChar) = '$char' OF $line")
            }
        }
    }
    devLog("availableSlots: $availableSlots")

    val rawPools = c.getDeep(arg, "pools") as? List<LinkedHashMap<String, Any?>?>? ?: c.getDeep(arg, "pool") as? List<LinkedHashMap<String, Any?>?>? ?: run {
        severeL("ErrorNoPools", key)
        return
    }

    val loadedPools = mutableListOf<LootPool>()

    for (pool in rawPools) {
        if (pool == null) continue
        val rolls = (pool["rolls"] ?: pool["roll"]) as? Int? ?: 1
        val loots = (pool["loots"] ?: pool["loot"]) as? List<LinkedHashMap<String, Any?>?>
        val loadedLoots = mutableListOf<Loots>()
        devLog("rolls: $rolls")
        devLog("loots: $loots")
        loots?.forEach { it ->

            val loadedItems = mutableListOf<LootItem>()

            val displayItem = c.getItem(pool["display"])

            val items = pool["items"] as? List<Any?>?

            if (items != null) for (item in items) {
                if (item != null) c.getItem(item)?.let { element -> loadedItems.add(element) }
            }

            if (it != null) {
                loadedLoots.add(
                    Loots(
                        displayItem = displayItem,
                        exps = (it["exp"] ?: it["exps"]).toString(),
                        items = loadedItems,
                        kether = it["javascript"].asListEnhanced(),
                        javaScript = it["javascript"].asListedStringEnhanced()?.compileJS(),
                        searchTime = it["search-time"].toString(),
                        skipSearch = it["skip-search"]?.toBooleanTolerance() ?: false,
                        weight = it["weight"].toString()
                    )
                )
            } else {
                devLog("Loot is null, skipping...")
            }
        }
    }

    val lootTable = LootTable(
        pools = loadedPools,
        agents = c.getAgents(arg)
    )

    val loot = LootTemplate(
        id = key,
        name = c.getDeep(arg, "display.name") as? String? ?: "Unknown Name",
        title = c.getDeep(arg, "display.title") as? String? ?: "Unknown Name",
        rows = c.getDeep(arg, "display.rows") as? Int? ?: layout.size,
        layout = layout,
        availableSlots = availableSlots.toSet(),
        lootTable = lootTable,
        agents = c.getAgents(arg),
        options = options
    )
    lootTemplates[key] = loot
}