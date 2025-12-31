package io.github.zzzyyylllty.lithiumcarbon.data.load

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.config
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootDefines
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootTemplates
import io.github.zzzyyylllty.lithiumcarbon.data.LocationHelper
import io.github.zzzyyylllty.lithiumcarbon.data.LootItem
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.data.LootPool
import io.github.zzzyyylllty.lithiumcarbon.data.LootTable
import io.github.zzzyyylllty.lithiumcarbon.data.LootTemplate
import io.github.zzzyyylllty.lithiumcarbon.data.LootTemplateOptions
import io.github.zzzyyylllty.lithiumcarbon.data.LootVector
import io.github.zzzyyylllty.lithiumcarbon.data.Loots
import io.github.zzzyyylllty.lithiumcarbon.data.define.LootDefine
import io.github.zzzyyylllty.lithiumcarbon.data.define.LootDefines
import io.github.zzzyyylllty.lithiumcarbon.data.define.SpecifyDefine
import io.github.zzzyyylllty.lithiumcarbon.data.define.SquareDefine
import io.github.zzzyyylllty.lithiumcarbon.data.define.WGDefine
import io.github.zzzyyylllty.lithiumcarbon.data.define.WorldDefine
import io.github.zzzyyylllty.lithiumcarbon.data.load.ConfigUtil.getConditions
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
    val files = File(getDataFolder(), "loots").listFiles()
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
        if (!checkRegexMatch(file.name, (config["file-load.loots"] ?: ".*").toString())) {
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
        val rolls = (pool["rolls"] ?: pool["roll"] ?: "1").toString()
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

        loadedPools.add(
            LootPool(
                rolls = rolls,
                conditions = c.getConditions(pool),
                loots = loadedLoots,
                agent = c.getAgents(pool)
            )
        )
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

    val defines = LootDefines(parseDefines(arg))

    lootTemplates[key] = loot
    lootDefines[key] = defines
}

@Suppress("UNCHECKED_CAST")
fun parseDefines(arg: Map<String, Any?>): LinkedHashMap<String, LootDefine> {
    val definesRaw = (arg["defines"] ?: arg["define"])
    val definesMap = linkedMapOf<String, Any?>()
    when (definesRaw) {
        is List<*> -> {
            var int = 0
            definesRaw.forEach { def ->
                int++
                definesMap[int.toString()] = (def as LinkedHashMap<String, Any?>)
            }
        }
        is LinkedHashMap<*, *> -> {
            definesRaw.forEach { def ->
                definesMap[def.key.toString()] = def.value
            }
        }
        else -> return linkedMapOf()
    }

    val result = linkedMapOf<String, LootDefine>()

    // 辅助扩展方法，将Any?转成List<String>
    fun Any?.asListEnhanced(): List<String> {
        return when (this) {
            is List<*> -> this.filterIsInstance<String>()
            is String -> listOf(this)
            else -> emptyList()
        }
    }

    // 辅助把字符串列表转成Regex列表
    fun List<String>.toRegexList(): List<Regex> = map { Regex(it) }

    // 假设你有 parseLootLocation 和 parseLootVector 的方法
    fun parseLootLocation(str: String): LootLocation {
        return LocationHelper.toLocationByString(str)
    }

    fun parseLootVector(str: String): LootVector {
        return LocationHelper.toVectorByString(str)
    }

    for ((id, define) in definesMap) {
        val define = define as LinkedHashMap<String, Any?>? ?: continue
        val type = define["type"]?.toString()?.lowercase() ?: continue
        val condition = getConditions(define)

        val blocks = (define["blocks"]?.asListEnhanced() ?: emptyList()).toHashSet()

        // 读取regex标志
        val regexFlag = (define["regex"] as? Boolean) ?: false

        when (type) {
            "specify" -> {
                val locsAny = define["locations"]
                val locationsMap = LinkedHashMap<String, HashSet<LootVector>>()
                if (locsAny is LinkedHashMap<*, *>) {
                    for ((k, v) in locsAny) {
                        val keyStr = k.toString()
                        val vecList = when (v) {
                            is List<*> -> v.filterIsInstance<String>().map { parseLootVector(it) }
                            is String -> listOf(parseLootVector(v))
                            else -> emptyList()
                        }
                        locationsMap[keyStr] = vecList.toHashSet()
                    }
                }

                val world = define["world"].toString().toRegex()

                result[id] = SpecifyDefine(
                    locations = locationsMap,
                    worldRegex = world,
                    blocks = blocks,
                    condition = condition
                )
            }

            "square" -> {
                val fromStr = define["from"]?.toString() ?: continue
                val toStr = define["to"]?.toString() ?: continue

                val fromLoc = parseLootLocation(fromStr)
                val toLoc = parseLootLocation(toStr)

                result[id] = SquareDefine(
                    from = fromLoc,
                    to = toLoc,
                    blocks = blocks,
                    condition = condition
                )
            }

            "worldguard" -> {
                // regions & regionsRegex
                val regionsList = (define["regions"] ?: define["region"])?.asListEnhanced() ?: emptyList()
                val regionsRegex = if (regexFlag) {
                    (define["regions"]?.asListEnhanced() ?: define["region"]?.asListEnhanced() ?: emptyList()).map { Regex(it) }
                } else null

                result[id] = WGDefine(
                    regions = regionsList,
                    regionsRegex = regionsRegex,
                    blocks = blocks,
                    condition = condition
                )
            }

            "world" -> {
                // worlds & regexWorlds
                val worldsList = ((define["worlds"] ?: define["world"])?.asListEnhanced() ?: emptyList()).toHashSet()
                val regexWorlds = if (regexFlag) {
                    ((define["worlds"] ?: define["world"])?.asListEnhanced() ?: emptyList()).map { Regex(it) }.toHashSet()
                } else null

                result[id] = WorldDefine(
                    worlds = worldsList,
                    regexWorlds = regexWorlds,
                    blocks = blocks,
                    condition = condition
                )
            }

            else -> {
                warningL("WarningUnknownDefineType", type)
            }
        }
    }

    return result
}