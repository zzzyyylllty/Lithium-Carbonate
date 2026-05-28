package io.github.zzzyyylllty.lithiumcarbon.cardroom

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon
import io.github.zzzyyylllty.lithiumcarbon.data.LocationHelper
import io.github.zzzyyylllty.lithiumcarbon.data.defaultData
import io.github.zzzyyylllty.lithiumcarbon.frame.FrameCrateManager
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import io.github.zzzyyylllty.lithiumcarbon.util.mmUtil
import org.bukkit.Bukkit
import taboolib.module.nms.getItemTag
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Directional
import org.bukkit.block.data.Openable
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.Damageable
import taboolib.common.platform.function.submit
import taboolib.common5.compileJS
import io.github.zzzyyylllty.lithiumcarbon.function.kether.evalKether
import javax.script.SimpleBindings

/**
 * 卡房动作执行器
 */
object CardRoomExecutor {

    /**
     * 执行动作列表
     */
    fun executeActions(
        actions: List<ActionConfig>,
        config: CardRoomConfig,
        instance: CardRoomInstance,
        player: Player?,
        async: Boolean = true
    ) {
        for (action in actions) {
            try {
                executeAction(action, config, instance, player, async)
            } catch (e: Exception) {
                devLog("Error executing action $action: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * 执行单个动作
     */
    private fun executeAction(
        action: ActionConfig,
        config: CardRoomConfig,
        instance: CardRoomInstance,
        player: Player?,
        async: Boolean
    ) {
        when (action) {
            is ActionConfig.RemoveBlockAction -> executeRemoveBlock(action, config, instance, async)
            is ActionConfig.SetBlockAction -> executeSetBlock(action, config, instance, async)
            is ActionConfig.DoorAction -> executeDoorAction(action, config, instance, async)
            is ActionConfig.SpawnChestAction -> executeSpawnChest(action, config, instance, player, async)
            is ActionConfig.ExecuteScriptAction -> executeScript(action, player)
            is ActionConfig.SpawnFrameAction -> executeSpawnFrame(action, config, instance, player, async)
            is ActionConfig.RemoveFrameAction -> executeRemoveFrame(action, instance, async)
        }
    }

    /**
     * 执行移除方块动作
     */
    private fun executeRemoveBlock(
        action: ActionConfig.RemoveBlockAction,
        config: CardRoomConfig,
        instance: CardRoomInstance,
        async: Boolean
    ) {
        val location = action.location.toBukkitLocation()
        val block = location.block

        // 验证方块类型（如果指定了）
        if (action.block != null) {
            val expectedBlock = Material.valueOf(action.block.uppercase())
            if (block.type != expectedBlock) {
                devLog("Block at ${location} is ${block.type}, expected ${expectedBlock}")
                return
            }
        }

        // 保存原始方块状态
        if (config.reset.restore) {
            instance.modifiedBlocks[action.location] = block.blockData.clone()
        }

        // 移除方块（设置为空气）
        val actionBlock = {
            block.type = Material.AIR
            devLog("Removed block at ${location}")
        }

        if (async) {
            submit { actionBlock() }
        } else {
            actionBlock()
        }
    }

    /**
     * 执行设置方块动作
     */
    private fun executeSetBlock(
        action: ActionConfig.SetBlockAction,
        config: CardRoomConfig,
        instance: CardRoomInstance,
        async: Boolean
    ) {
        val location = action.location.toBukkitLocation()
        val block = location.block

        // 保存原始方块状态
        if (config.reset.restore) {
            instance.modifiedBlocks[action.location] = block.blockData.clone()
        }

        // 设置新方块
        val actionBlock = {
            try {
                val material = Material.valueOf(action.block.uppercase())
                block.type = material
                devLog("Set block at ${location} to ${material}")
            } catch (e: IllegalArgumentException) {
                devLog("Invalid material: ${action.block}")
            }
        }

        if (async) {
            submit { actionBlock() }
        } else {
            actionBlock()
        }
    }

    /**
     * 执行门操作动作
     */
    private fun executeDoorAction(
        action: ActionConfig.DoorAction,
        config: CardRoomConfig,
        instance: CardRoomInstance,
        async: Boolean
    ) {
        val location = action.location.toBukkitLocation()
        val block = location.block

        // 保存原始门状态
        if (config.reset.restore && instance.modifiedBlocks[action.location] == null) {
            instance.modifiedBlocks[action.location] = block.blockData.clone()
        }

        val actionBlock = {
            val blockData = block.blockData

            // 检查是否为可打开的门
            if (blockData is Openable) {
                blockData.isOpen = action.open
                block.blockData = blockData
                devLog("${if (action.open) "Opened" else "Closed"} door at ${location}")
            } else {
                devLog("Block at ${location} is not an openable door: ${block.type}")
            }

            // 设置门方向（如果指定了）
            if (action.direction != null && blockData is Directional) {
                try {
                    val face = BlockFace.valueOf(action.direction.uppercase())
                    blockData.facing = face
                    block.blockData = blockData
                    devLog("Set door direction to ${face} at ${location}")
                } catch (e: IllegalArgumentException) {
                    devLog("Invalid direction: ${action.direction}")
                }
            }
        }

        if (async) {
            submit { actionBlock() }
        } else {
            actionBlock()
        }
    }

    /**
     * 执行生成物资箱动作
     */
    private fun executeSpawnChest(
        action: ActionConfig.SpawnChestAction,
        config: CardRoomConfig,
        instance: CardRoomInstance,
        player: Player?,
        async: Boolean
    ) {
        val location = action.location.toBukkitLocation()
        val block = location.block

        // 保存原始方块状态
        if (config.reset.restore) {
            instance.modifiedBlocks[action.location] = block.blockData.clone()
        }

        // 设置方块为指定类型
        val actionBlock = actionBlock@{
            try {
                val material = Material.valueOf(action.block.uppercase())
                block.type = material
                devLog("Set block at ${location} to ${material}")
            } catch (e: IllegalArgumentException) {
                devLog("Invalid material: ${action.block}")
                return@actionBlock
            }

            // 创建战利品实例
            val template = LithiumCarbon.lootTemplates[action.lootTemplate]
            if (template == null) {
                devLog("Loot template not found: ${action.lootTemplate}")
                return@actionBlock
            }

            // 创建LootInstanceKey
            val playerId = if (action.private && player != null) player.uniqueId else null
            val key = LithiumCarbon.LootInstanceKey(action.location, playerId)

            // 创建战利品实例
            val lootInstance = template.createInstance(block, player ?: return@actionBlock, true)

            // 注册到全局map
            LithiumCarbon.lootMap[key] = lootInstance

            // 保存到卡房实例
            instance.spawnedChests[action.location] = key

            devLog("Spawned chest at ${location} with template ${action.lootTemplate}")
        }

        if (async) {
            submit { actionBlock() }
        } else {
            actionBlock()
        }
    }

    /**
     * 执行脚本动作
     */
    private fun executeScript(
        action: ActionConfig.ExecuteScriptAction,
        player: Player?
    ) {
        val extraVariables = mutableMapOf<String, Any?>(
            "player" to player,
            "location" to action.location?.toBukkitLocation()
        )

        // 执行JavaScript
        if (action.js != null) {
            try {
                val script = action.js
                val compiledScript = script.compileJS()
                if (compiledScript != null) {
                    val bindings = SimpleBindings(defaultData  + extraVariables)
                    compiledScript.eval(bindings)
                    devLog("Executed JavaScript: ${script.take(50)}...")
                } else {
                    devLog("Failed to compile JavaScript: ${script.take(50)}...")
                }
            } catch (e: Exception) {
                devLog("Error executing JavaScript: ${e.message}")
                e.printStackTrace()
            }
        }

        // 执行Kether脚本
        if (action.kether != null) {
            try {
                val script = action.kether
                script.evalKether(player, defaultData  +extraVariables)
                devLog("Executed Kether: ${script.take(50)}...")
            } catch (e: Exception) {
                devLog("Error executing Kether: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * 恢复方块到原始状态
     */
    fun restoreBlock(location: io.github.zzzyyylllty.lithiumcarbon.data.LootLocation, originalData: BlockData) {
        val bukkitLocation = location.toBukkitLocation()
        submit {
            val block = bukkitLocation.block
            block.blockData = originalData
            devLog("Restored block at ${bukkitLocation} to original state")
        }
    }

    /**
     * 移除生成的箱子
     */
    fun removeSpawnedChest(location: io.github.zzzyyylllty.lithiumcarbon.data.LootLocation, key: LithiumCarbon.LootInstanceKey) {
        submit {
            // 从lootMap中移除实例
            val lootInstance = LithiumCarbon.lootMap.remove(key)
            lootInstance?.update()

            // 设置方块为空气
            val bukkitLocation = location.toBukkitLocation()
            bukkitLocation.block.type = Material.AIR

            devLog("Removed chest at ${location}")
        }
    }

    /**
     * 执行生成展示框动作
     */
    private fun executeSpawnFrame(
        action: ActionConfig.SpawnFrameAction,
        config: CardRoomConfig,
        instance: CardRoomInstance,
        player: Player?,
        async: Boolean
    ) {
        val location = action.location.toBukkitLocation()
        val actionBlock = {
            // 在生成前移除当前位置的现有展示框（防止服务器意外关闭后遗留的展示框冲突）
            if (action.removeExisting) {
                FrameCrateManager.removeFrameAtLocation(location)
                location.world?.getNearbyEntities(location, 0.5, 0.5, 0.5)
                    ?.filterIsInstance<ItemFrame>()
                    ?.forEach { it.remove() }
            }

            val frameUuid = FrameCrateManager.spawnFrame(
                location = location,
                configId = action.frameCrateConfig,
                player = player,
                facing = action.facing
            )

            if (frameUuid != null) {
                instance.spawnedFrames.add(action.location)
                devLog("Spawned frame crate at ${location} for card room ${config.id}")
            } else {
                devLog("Failed to spawn frame crate at ${location} for card room ${config.id}")
            }
        }

        if (async) {
            submit { actionBlock() }
        } else {
            actionBlock()
        }
    }

    /**
     * 执行移除展示框动作
     */
    private fun executeRemoveFrame(
        action: ActionConfig.RemoveFrameAction,
        instance: CardRoomInstance,
        async: Boolean
    ) {
        val location = action.location.toBukkitLocation()
        val actionBlock = {
            val removed = FrameCrateManager.removeFrameAtLocation(location)
            if (removed) {
                instance.spawnedFrames.remove(action.location)
                devLog("Removed frame crate at ${location}")
            } else {
                devLog("No frame crate found at ${location}")
            }
        }

        if (async) {
            submit { actionBlock() }
        } else {
            actionBlock()
        }
    }

    /**
     * 消耗玩家手持物品
     */
    fun consumePlayerItem(player: Player, config: ConsumeKeyConfig) {
        when (config.mode) {
            ConsumeMode.NONE -> return
            ConsumeMode.ITEM -> consumeItem(player, config.value)
            ConsumeMode.DURABILITY -> consumeDurability(player, config.value)
            ConsumeMode.TAG -> consumeTag(player, config)
        }
    }

    /**
     * 减少物品数量
     */
    private fun consumeItem(player: Player, amount: Int) {
        val itemInHand = player.inventory.itemInMainHand
        if (itemInHand.type == Material.AIR) return

        val newAmount = itemInHand.amount - amount
        if (newAmount <= 0) {
            player.inventory.setItemInMainHand(null)
        } else {
            itemInHand.amount = newAmount
        }
        player.playSound(player.location, "entity.item.pickup", 1.0f, 1.0f)
        devLog("Consumed ${amount}x item from ${player.name}")
    }

    /**
     * 降低物品耐久
     */
    private fun consumeDurability(player: Player, amount: Int) {
        val itemInHand = player.inventory.itemInMainHand
        if (itemInHand.type == Material.AIR) return

        val meta = itemInHand.itemMeta ?: return
        if (meta !is Damageable || meta.isUnbreakable) return

        val newDamage = (meta.damage + kotlin.math.abs(amount))
            .coerceAtMost(itemInHand.type.maxDurability - 1)
        meta.damage = newDamage
        itemInHand.itemMeta = meta

        if (newDamage >= itemInHand.type.maxDurability - 1) {
            player.inventory.setItemInMainHand(null)
            player.playSound(player.location, "entity.item.break", 1.0f, 1.0f)
        }
        player.playSound(player.location, "entity.item.pickup", 1.0f, 0.5f)
        devLog("Reduced durability by ${amount} for ${player.name}'s item")
    }

    /**
     * 修改物品NBT标签值
     */
    private fun consumeTag(player: Player, config: ConsumeKeyConfig) {
        val tagKey = config.tag ?: return
        val itemInHand = player.inventory.itemInMainHand
        if (itemInHand.type == Material.AIR) return

        try {
            val itemTag = itemInHand.getItemTag()
            val current = itemTag.getDeep(tagKey)?.asInt() ?: 0
            itemTag.putDeep(tagKey, current + config.value)
            itemTag.saveTo(itemInHand)
            player.playSound(player.location, "entity.item.pickup", 1.0f, 1.0f)
            devLog("Modified tag ${tagKey} by ${config.value} for ${player.name}'s item")
        } catch (e: Exception) {
            devLog("Error modifying item tag: ${e.message}")
        }
    }
}