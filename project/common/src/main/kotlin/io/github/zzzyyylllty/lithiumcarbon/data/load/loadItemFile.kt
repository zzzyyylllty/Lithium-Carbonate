package io.github.zzzyyylllty.lithiumcarbon.data.load

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.config
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootDefines
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootItems
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootItemsDef
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

// import org.yaml.snakeyaml.Yaml
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import taboolib.common5.compileJS
import java.io.File
import kotlin.collections.forEach
import kotlin.collections.set

fun loadItemFiles() {
    infoL("ItemLoad")
    if (!File(getDataFolder(), "items.yml").exists()) {
        warningL("ItemRegen")
        releaseResourceFile("items.yml")
    }
    val file = File(getDataFolder(), "items.yml")
    loadItemFile(file)
}
fun loadItemFile(file: File) {
        val map = multiExtensionLoader(file)
        if (map != null) for (it in map.entries) {
            val key = it.key
            val value = map[key] ?: continue
            (value as Map<String, Any?>?)?.let { arg -> loadItem(key, arg) }
        } else {
            devLog("Map is null, skipping.")
        }
}

fun loadItem(key: String, arg: Map<String, Any?>) {
    val c = ConfigUtil

    val item = c.getItem(arg)

    item?.let {
        if (key.length >= 2) lootItemsDef[key] = it
        else lootItems[key[0]] = it
    }
}