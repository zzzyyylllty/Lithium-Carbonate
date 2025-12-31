package io.github.zzzyyylllty.lithiumcarbon.data.load

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.config
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootTemplates
import io.github.zzzyyylllty.lithiumcarbon.data.LootTemplate
import io.github.zzzyyylllty.lithiumcarbon.data.LootTemplateOptions

import io.github.zzzyyylllty.sertraline.debugMode.devLog
import io.github.zzzyyylllty.sertraline.logger.infoL
import io.github.zzzyyylllty.sertraline.logger.severeL
import io.github.zzzyyylllty.sertraline.logger.warningL
// import org.yaml.snakeyaml.Yaml
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import java.io.File
import kotlin.collections.forEach
import kotlin.collections.set
import kotlin.let
import kotlin.toString


fun loadLootFiles() {
    infoL("LoreFormat_Load")
    if (!File(getDataFolder(), "lore-formats").exists()) {
        warningL("LoreFormat_Load_Regen")
        releaseResourceFile("lore-formats/loreGenerator.yml")
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
            loadLoot(key, value as Map<String, Any?>)
        } else {
            devLog("Map is null, skipping.")
        }
    }
}

fun loadLoot(key: String, arg: Map<String, Any?>) {
    val c = ConfigUtil

    val options = LootTemplateOptions(
        removeLore = c.getDeep(arg, "options.remove-lore") as? Boolean? ?: false,
        addLore = c.getDeep(arg, "options.add-lore") as? List<String>? ?: config.getStringList("default-options.add-lore"),
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

    for (pool in rawPools) {

    }

    val loot = LootTemplate(
        id = key,
        name = c.getDeep(arg, "display.name") as? String? ?: "Unknown Name",
        title = c.getDeep(arg, "display.title") as? String? ?: "Unknown Name",
        rows = c.getDeep(arg, "display.rows") as? Int? ?: layout.size,
        layout = layout,
        availableSlots = availableSlots,
        lootTable = TODO(),
        agents = TODO(),
        options = options
    )
    lootTemplates[key] = loot
}