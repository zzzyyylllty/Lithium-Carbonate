package io.github.zzzyyylllty.lithiumcarbon.cardroom

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.cardRoomConfigs
import io.github.zzzyyylllty.lithiumcarbon.data.*
import io.github.zzzyyylllty.lithiumcarbon.data.load.ConfigUtil
import io.github.zzzyyylllty.lithiumcarbon.data.load.multiExtensionLoader
import io.github.zzzyyylllty.lithiumcarbon.logger.infoL
import io.github.zzzyyylllty.lithiumcarbon.logger.severeL
import io.github.zzzyyylllty.lithiumcarbon.logger.warningL
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import java.io.File

/**
 * 卡房配置文件加载器
 */
object CardRoomLoader {

    /**
     * 加载所有卡房配置文件
     */
    fun loadCardRoomFiles() {
        infoL("CardRoomLoad")
        val cardRoomsDir = File(getDataFolder(), "card-rooms")

        // 如果目录不存在，创建并释放示例文件
        if (!cardRoomsDir.exists()) {
            warningL("CardRoomDirNotFound")
            cardRoomsDir.mkdirs()
            releaseResourceFile("card-rooms/example.yml", true)
        }

        val files = cardRoomsDir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                file.listFiles()?.forEach { loadCardRoomFile(it) }
            } else {
                loadCardRoomFile(file)
            }
        }
    }

    /**
     * 加载单个卡房配置文件
     */
    private fun loadCardRoomFile(file: File) {
        devLog("Loading card room file: ${file.name}")

        // 检查文件名是否符合配置的正则
        val config = LithiumCarbon.config
        val regex = (config["file-load.card-rooms"] ?: config["file-load.loots"] ?: ".*").toString()
        if (!checkRegexMatch(file.name, regex)) {
            devLog("${file.name} not match regex, skipping...")
            return
        }

        val map = multiExtensionLoader(file) ?: run {
            devLog("Failed to load file: ${file.name}")
            return
        }

        for ((key, value) in map.entries) {
            val configMap = value as? Map<String, Any?> ?: continue
            loadCardRoom(key, configMap)
        }
    }

    /**
     * 加载单个卡房配置
     */
    private fun loadCardRoom(id: String, config: Map<String, Any?>) {
        val c = ConfigUtil

        try {
            // 解析名称
            val name = c.getDeep(config, "name") as? String ?: id

            // 解析触发配置
            val triggerRaw = c.getDeep(config, "trigger") as? Map<String, Any?> ?: run {
                severeL("CardRoomNoTrigger", id)
                return
            }

            val trigger = parseTriggerConfig(triggerRaw)

            // 解析动作列表
            val actionsRaw = config["actions"] as? List<Map<String, Any?>> ?: run {
                severeL("CardRoomNoActions", id)
                return
            }

            val actions = parseActions(actionsRaw)

            // 解析重置配置
            val resetRaw = c.getDeep(config, "reset") as? Map<String, Any?> ?: run {
                severeL("CardRoomNoReset", id)
                return
            }

            val reset = parseResetConfig(resetRaw)

            // 解析事件代理
            val agents = c.getAgents(config)

            // 创建卡房配置
            val cardRoomConfig = CardRoomConfig(
                id = id,
                name = name,
                trigger = trigger,
                actions = actions,
                reset = reset,
                agents = agents
            )

            devLog("Loaded card room: $id = $cardRoomConfig")
            cardRoomConfigs[id] = cardRoomConfig

        } catch (e: Exception) {
            severeL("CardRoomLoadError", id, e.message ?: "Unknown error")
            e.printStackTrace()
        }
    }

    /**
     * 解析钥匙消耗配置
     */
    private fun parseConsumeKey(input: Any?): ConsumeKeyConfig {
        return when (input) {
            is Boolean -> {
                if (input) ConsumeKeyConfig(ConsumeMode.ITEM, value = 1)
                else ConsumeKeyConfig(ConsumeMode.NONE)
            }
            is Map<*, *> -> {
                val mode = when ((input["mode"] as? String)?.lowercase()) {
                    "durability" -> ConsumeMode.DURABILITY
                    "tag" -> ConsumeMode.TAG
                    "item" -> ConsumeMode.ITEM
                    "none" -> ConsumeMode.NONE
                    else -> ConsumeMode.ITEM
                }
                val tag = input["tag"] as? String
                val value = (input["value"] as? Number)?.toInt() ?: 1
                ConsumeKeyConfig(mode, tag, value)
            }
            else -> ConsumeKeyConfig(ConsumeMode.ITEM, value = 1)
        }
    }

    /**
     * 解析物品匹配列表
     */
    private fun parseItems(config: Map<String, Any?>): List<ItemMatcher> {
        // 新格式：item 列表
        val itemList = config["item"] as? List<Map<String, Any?>>
        if (itemList != null) {
            return itemList.mapNotNull { entry ->
                val tag = (entry["tag"] as? Map<String, Any?>)?.mapNotNull { (k, v) -> v?.let { k to it } }?.toMap()
                val consumeKey = parseConsumeKey(entry["consume-key"] ?: entry["consume"] ?: true)
                ItemMatcher(tag, consumeKey)
            }
        }

        // 旧格式兼容：单个 item-tag + consume-key
        val itemTag: Map<String, Any>? = (config["item-tag"] as? Map<String, Any?>)?.mapNotNull { (k, v) -> v?.let { k to it } }?.toMap()
        val consumeKey = parseConsumeKey(config["consume-key"])
        return listOf(ItemMatcher(itemTag, consumeKey))
    }

    /**
     * 解析触发配置
     */
    private fun parseTriggerConfig(config: Map<String, Any?>): TriggerConfig {
        val c = ConfigUtil

        // 解析触发方块位置
        val blockStr = config["block"]?.toString() ?: run {
            severeL("CardRoomNoTriggerBlock")
            throw IllegalArgumentException("Missing trigger block")
        }
        val blockLocation = LocationHelper.toLocationByString(blockStr)

        // 解析物品匹配规则
        val items = parseItems(config)

        // 解析额外条件
        val condition = c.getConditions(config)

        return TriggerConfig(
            block = blockLocation,
            items = items,
            condition = condition
        )
    }

    /**
     * 解析动作列表
     */
    private fun parseActions(actionsRaw: List<Map<String, Any?>>): List<ActionConfig> {
        val actions = mutableListOf<ActionConfig>()

        for (actionRaw in actionsRaw) {
            val type = actionRaw["type"]?.toString() ?: continue

            when (type.lowercase()) {
                "remove-block" -> parseRemoveBlockAction(actionRaw)?.let { actions.add(it) }
                "set-block" -> parseSetBlockAction(actionRaw)?.let { actions.add(it) }
                "door", "open-door", "close-door" -> parseDoorAction(actionRaw)?.let { actions.add(it) }
                "spawn-chest" -> parseSpawnChestAction(actionRaw)?.let { actions.add(it) }
                "spawn-frame" -> parseSpawnFrameAction(actionRaw)?.let { actions.add(it) }
                "remove-frame" -> parseRemoveFrameAction(actionRaw)?.let { actions.add(it) }
                "execute-script" -> parseExecuteScriptAction(actionRaw)?.let { actions.add(it) }
                else -> warningL("CardRoomUnknownActionType", type)
            }
        }

        return actions
    }

    /**
     * 解析移除方块动作
     */
    private fun parseRemoveBlockAction(config: Map<String, Any?>): ActionConfig.RemoveBlockAction? {
        val locationStr = config["location"]?.toString() ?: run {
            severeL("CardRoomActionNoLocation", "remove-block")
            return null
        }
        val location = LocationHelper.toLocationByString(locationStr)
        val block = config["block"]?.toString()

        return ActionConfig.RemoveBlockAction(location, block)
    }

    /**
     * 解析设置方块动作
     */
    private fun parseSetBlockAction(config: Map<String, Any?>): ActionConfig.SetBlockAction? {
        val locationStr = config["location"]?.toString() ?: run {
            severeL("CardRoomActionNoLocation", "set-block")
            return null
        }
        val location = LocationHelper.toLocationByString(locationStr)
        val block = config["block"]?.toString() ?: run {
            severeL("CardRoomActionNoBlock", "set-block")
            return null
        }

        return ActionConfig.SetBlockAction(location, block)
    }

    /**
     * 解析门操作动作
     */
    private fun parseDoorAction(config: Map<String, Any?>): ActionConfig.DoorAction? {
        val locationStr = config["location"]?.toString() ?: run {
            severeL("CardRoomActionNoLocation", "door")
            return null
        }
        val location = LocationHelper.toLocationByString(locationStr)
        val direction = config["direction"]?.toString()

        // 根据类型决定开门还是关门
        val type = config["type"]?.toString()?.lowercase()
        val open = when (type) {
            "open-door" -> true
            "close-door" -> false
            "door" -> config["open"] as? Boolean ?: true
            else -> true
        }

        return ActionConfig.DoorAction(location, direction, open)
    }

    /**
     * 解析生成物资箱动作
     */
    private fun parseSpawnChestAction(config: Map<String, Any?>): ActionConfig.SpawnChestAction? {
        val locationStr = config["location"]?.toString() ?: run {
            severeL("CardRoomActionNoLocation", "spawn-chest")
            return null
        }
        val location = LocationHelper.toLocationByString(locationStr)
        val lootTemplate = config["loot-template"]?.toString() ?: run {
            severeL("CardRoomActionNoLootTemplate", "spawn-chest")
            return null
        }
        val private = config["private"] as? Boolean ?: false
        val block = config["block"]?.toString() ?: "CHEST"

        return ActionConfig.SpawnChestAction(location, lootTemplate, private, block)
    }

    /**
     * 解析生成展示框动作
     */
    private fun parseSpawnFrameAction(config: Map<String, Any?>): ActionConfig.SpawnFrameAction? {
        val locationStr = config["location"]?.toString() ?: run {
            severeL("CardRoomActionNoLocation", "spawn-frame")
            return null
        }
        val location = LocationHelper.toLocationByString(locationStr)
        val frameCrateConfig = config["frame-crate"]?.toString() ?: run {
            severeL("CardRoomActionNoFrameCrate", "spawn-frame")
            return null
        }
        val facing = config["facing"]?.toString()
        val removeExisting = config["remove-existing"] as? Boolean ?: true

        return ActionConfig.SpawnFrameAction(location, frameCrateConfig, facing, removeExisting)
    }

    /**
     * 解析移除展示框动作
     */
    private fun parseRemoveFrameAction(config: Map<String, Any?>): ActionConfig.RemoveFrameAction? {
        val locationStr = config["location"]?.toString() ?: run {
            severeL("CardRoomActionNoLocation", "remove-frame")
            return null
        }
        val location = LocationHelper.toLocationByString(locationStr)

        return ActionConfig.RemoveFrameAction(location)
    }

    /**
     * 解析执行脚本动作
     */
    private fun parseExecuteScriptAction(config: Map<String, Any?>): ActionConfig.ExecuteScriptAction? {
        val locationStr = config["location"]?.toString()
        val location = locationStr?.let { LocationHelper.toLocationByString(it) }
        val js = config["js"]?.toString()
        val kether = config["kether"]?.toString()

        if (js == null && kether == null) {
            warningL("CardRoomActionNoScript", "execute-script")
            return null
        }

        return ActionConfig.ExecuteScriptAction(location, js, kether)
    }

    /**
     * 解析重置配置
     */
    private fun parseResetConfig(config: Map<String, Any?>): ResetConfig {
        // 解析检测区域
        val range = parseAreaRange(config)

        // 解析重置延迟
        val delay = config["delay"]?.toString()?.toDoubleOrNull() ?: 300.0

        // 解析是否恢复环境
        val restore = config["restore"] as? Boolean ?: true

        // 解析重置动作
        val actionsRaw = config["actions"] as? List<Map<String, Any?>> ?: emptyList()
        val actions = parseActions(actionsRaw)

        return ResetConfig(
            range = range,
            delay = delay,
            restore = restore,
            actions = actions
        )
    }

    /**
     * 解析区域范围
     */
    private fun parseAreaRange(config: Map<String, Any?>): AreaRange? {
        val fromStr = config["from"]?.toString()
        val toStr = config["to"]?.toString()

        if (fromStr == null || toStr == null) return null

        val from = LocationHelper.toLocationByString(fromStr)
        val to = LocationHelper.toLocationByString(toStr)

        return AreaRange(from, to)
    }

    /**
     * 检查文件名是否匹配正则
     */
    private fun checkRegexMatch(input: String, regex: String): Boolean {
        return input.matches(regex.toRegex())
    }
}