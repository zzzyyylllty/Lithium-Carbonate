package io.github.zzzyyylllty.lithiumcarbon.unlock.load

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.unlockUIConfigs
import io.github.zzzyyylllty.lithiumcarbon.data.LootItem
import io.github.zzzyyylllty.lithiumcarbon.data.load.ConfigUtil
import io.github.zzzyyylllty.lithiumcarbon.data.load.multiExtensionLoader
import io.github.zzzyyylllty.lithiumcarbon.logger.infoL
import io.github.zzzyyylllty.lithiumcarbon.logger.warningL
import io.github.zzzyyylllty.lithiumcarbon.unlock.BruteForceConfig
import io.github.zzzyyylllty.lithiumcarbon.unlock.DecipherConfig
import io.github.zzzyyylllty.lithiumcarbon.unlock.PasswordConfig
import io.github.zzzyyylllty.lithiumcarbon.unlock.SpeedConfig
import io.github.zzzyyylllty.lithiumcarbon.unlock.TimerConfig
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockUIConfig
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import java.io.File

fun loadUnlockUIFiles() {
    infoL("UnlockUILoad")
    val dir = File(getDataFolder(), "unlock-uis")

    if (!dir.exists()) {
        warningL("UnlockUINotFound")
        dir.mkdirs()
        releaseResourceFile("unlock-uis/example.yml", true)
    }

    val files = dir.listFiles() ?: return

    for (file in files) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { loadUnlockUIFile(it) }
        } else {
            loadUnlockUIFile(file)
        }
    }
}

private fun loadUnlockUIFile(file: File) {
    devLog("Loading unlock UI file: ${file.name}")

    val regex = "^(?![#!]).*\\.(?i)(yaml|yml|toml|tml|json|conf)$".toRegex()
    if (!file.name.matches(regex)) {
        devLog("${file.name} not match regex, skipping...")
        return
    }

    val map = multiExtensionLoader(file) ?: run {
        devLog("Failed to load unlock UI file: ${file.name}")
        return
    }

    for ((key, value) in map.entries) {
        val config = value as? Map<String, Any?> ?: continue
        loadUnlockUI(key, config)
    }
}

@Suppress("UNCHECKED_CAST")
private fun loadUnlockUI(id: String, config: Map<String, Any?>) {
    val c = ConfigUtil

    val type = c.getDeep(config, "type")?.toString() ?: run {
        devLog("Unlock UI $id has no type, skipping")
        return
    }

    val display = c.getDeep(config, "display") as? Map<String, Any?> ?: return
    val title = c.getDeep(display, "title")?.toString() ?: "Unlock"
    val rows = (c.getDeep(display, "rows") as? Number)?.toInt() ?: 3
    val layout = (c.getDeep(display, "layout") as? List<*>)?.map { it.toString() } ?: listOf("         ", "         ", "         ")

    val items = mutableMapOf<Char, LootItem>()
    val rawItems = c.getDeep(display, "items") as? Map<String, Any?>?
    rawItems?.forEach { (char, itemConfig) ->
        if (char.length == 1) {
            c.getItem(itemConfig, null)?.let { items[char[0]] = it }
        }
    }

    val bruteForce = (c.getDeep(config, "brute_force") as? Map<String, Any?>)?.let { bf ->
        BruteForceConfig(
            clicks = c.getDeep(bf, "clicks")?.toString() ?: "5",
            progressItem = c.getItem(c.getDeep(bf, "progress-item"), null) ?: LootItem("mc", "gray_stained_glass_pane"),
            completeItem = c.getItem(c.getDeep(bf, "complete-item"), null) ?: LootItem("mc", "lime_stained_glass_pane"),
            soundProgress = c.getDeep(bf, "sound-progress")?.toString(),
            soundComplete = c.getDeep(bf, "sound-complete")?.toString(),
        )
    }

    val password = (c.getDeep(config, "password") as? Map<String, Any?>)?.let { pw ->
        PasswordConfig(
            password = c.getDeep(pw, "password")?.toString() ?: "1234",
            maxAttempts = (c.getDeep(pw, "max-attempts") as? Number)?.toInt() ?: 3,
            inputItem = c.getItem(c.getDeep(pw, "input-item"), null) ?: LootItem("mc", "name_tag"),
            attemptItem = c.getItem(c.getDeep(pw, "attempt-item"), null) ?: LootItem("mc", "paper"),
            maskInput = c.getDeep(pw, "mask-input") as? Boolean ?: false,
            soundCorrect = c.getDeep(pw, "sound-correct")?.toString(),
            soundWrong = c.getDeep(pw, "sound-wrong")?.toString(),
        )
    }

    val decipher = (c.getDeep(config, "decipher") as? Map<String, Any?>)?.let { dc ->
        val poolItems = (c.getDeep(dc, "items") as? List<*>)?.mapNotNull { c.getItem(it, null) } ?: emptyList()
        DecipherConfig(
            rounds = (c.getDeep(dc, "rounds") as? Number)?.toInt() ?: 3,
            items = poolItems,
            targetSlot = (c.getDeep(dc, "target-slot") as? Number)?.toInt() ?: 13,
            targetSlots = (c.getDeep(dc, "target-slots") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() },
            targetItemIndex = (c.getDeep(dc, "target-item-index") as? Number)?.toInt() ?: 0,
            rotationSpeed = (c.getDeep(dc, "rotation-speed-ticks") as? Number)?.toInt() ?: 10,
            decipherButtonSlot = (c.getDeep(dc, "decipher-button-slot") as? Number)?.toInt() ?: 15,
            decipherButtonItem = c.getItem(c.getDeep(dc, "decipher-button-item"), null) ?: LootItem("mc", "lever"),
            successItem = c.getItem(c.getDeep(dc, "success-item"), null) ?: LootItem("mc", "lime_concrete"),
            failItem = c.getItem(c.getDeep(dc, "fail-item"), null) ?: LootItem("mc", "red_concrete"),
            soundRotate = c.getDeep(dc, "sound-rotate")?.toString(),
            soundSuccess = c.getDeep(dc, "sound-success")?.toString(),
            soundFail = c.getDeep(dc, "sound-fail")?.toString(),
        )
    }

    val speed = (c.getDeep(config, "speed") as? Map<String, Any?>)?.let { sp ->
        SpeedConfig(
            totalTargets = (c.getDeep(sp, "total-targets") as? Number)?.toInt() ?: 10,
            spawnInterval = (c.getDeep(sp, "spawn-interval-ticks") as? Number)?.toInt() ?: 30,
            targetItem = c.getItem(c.getDeep(sp, "target-item"), null) ?: LootItem("mc", "red_wool"),
            distractorItem = c.getItem(c.getDeep(sp, "distractor-item"), null),
            penaltyOnDistractor = c.getDeep(sp, "penalty-on-distractor") as? Boolean ?: true,
            penaltyAction = c.getDeep(sp, "penalty-action")?.toString() ?: "reset",
            timePenaltySeconds = (c.getDeep(sp, "time-penalty-seconds") as? Number)?.toInt() ?: 5,
            timeLimit = (c.getDeep(sp, "time-limit-seconds") as? Number)?.toInt(),
            soundTarget = c.getDeep(sp, "sound-target")?.toString(),
            soundDistractor = c.getDeep(sp, "sound-distractor")?.toString(),
            soundComplete = c.getDeep(sp, "sound-complete")?.toString(),
        )
    }

    val timer = (c.getDeep(config, "timer") as? Map<String, Any?>)?.let { tm ->
        TimerConfig(
            duration = c.getDeep(tm, "duration")?.toString() ?: "5",
            displayType = c.getDeep(tm, "display-type")?.toString() ?: "gui",
            bossBarColor = c.getDeep(tm, "boss-bar-color")?.toString(),
            progressItem = c.getItem(c.getDeep(tm, "progress-item"), null) ?: LootItem("mc", "clock"),
            completeItem = c.getItem(c.getDeep(tm, "complete-item"), null) ?: LootItem("mc", "lime_stained_glass_pane"),
            soundTick = c.getDeep(tm, "sound-tick")?.toString(),
            soundComplete = c.getDeep(tm, "sound-complete")?.toString(),
            cancelOnMove = c.getDeep(tm, "cancel-on-move") as? Boolean ?: false,
        )
    }

    val agents = ConfigUtil.getAgents(config)

    val infoRaw = c.getDeep(display, "info") as? Map<String, Any?>?
    val info = infoRaw?.let { infoCfg ->
        val slot = (c.getDeep(infoCfg, "slot") as? Number)?.toInt() ?: return@let null
        val item = c.getItem(c.getDeep(infoCfg, "item"), null) ?: return@let null
        io.github.zzzyyylllty.lithiumcarbon.unlock.TutorialInfo(slot, item)
    }

    unlockUIConfigs[id] = UnlockUIConfig(
        id = id,
        type = type,
        title = title,
        rows = rows,
        layout = layout,
        items = items,
        bruteForce = bruteForce,
        password = password,
        decipher = decipher,
        speed = speed,
        timer = timer,
        agents = agents,
        info = info,
    )

    devLog("Loaded unlock UI: $id (type=$type)")
}
