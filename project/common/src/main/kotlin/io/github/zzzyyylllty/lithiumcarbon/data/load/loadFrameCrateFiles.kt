package io.github.zzzyyylllty.lithiumcarbon.data.load

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.frameCrateConfigs
import io.github.zzzyyylllty.lithiumcarbon.data.FrameCrateConfig
import io.github.zzzyyylllty.lithiumcarbon.logger.infoL
import io.github.zzzyyylllty.lithiumcarbon.logger.severeL
import io.github.zzzyyylllty.lithiumcarbon.logger.warningL
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import org.bukkit.Bukkit
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import java.io.File

fun loadFrameCrateFiles() {
    infoL("FrameCrateLoad")
    val frameCratesDir = File(getDataFolder(), "frame-crates")

    if (!frameCratesDir.exists()) {
        warningL("FrameCrateDirNotFound")
        frameCratesDir.mkdirs()
        releaseResourceFile("frame-crates/example.yml", true)
    }

    val files = frameCratesDir.listFiles() ?: return

    for (file in files) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { loadFrameCrateFile(it) }
        } else {
            loadFrameCrateFile(file)
        }
    }
}

private fun loadFrameCrateFile(file: File) {
    devLog("Loading frame crate file: ${file.name}")

    val regex = "^(?![#!]).*\\.(?i)(yaml|yml|toml|tml|json|conf)$".toRegex()
    if (!file.name.matches(regex)) {
        devLog("${file.name} not match regex, skipping...")
        return
    }

    val map = multiExtensionLoader(file) ?: run {
        devLog("Failed to load file: ${file.name}")
        return
    }

    for ((key, value) in map.entries) {
        val configMap = value as? Map<String, Any?> ?: continue
        loadFrameCrate(key, configMap)
    }
}

private fun loadFrameCrate(id: String, config: Map<String, Any?>) {
    // Parse loot template reference
    val lootTemplate = config["loot-template"]?.toString() ?: run {
        severeL("FrameCrateNoLootTemplate", id)
        return
    }

    // Parse expire
    val expire = config["expire"]?.toString()

    // Parse glow (default: false)
    val glow = config["glow"] as? Boolean ?: false

    // Parse agents
    val agents = ConfigUtil.getAgents(config)

    frameCrateConfigs[id] = FrameCrateConfig(
        id = id,
        lootTemplate = lootTemplate,
        expire = expire,
        glow = glow,
        agents = agents,
    )

    devLog("Loaded frame crate: $id")
}
