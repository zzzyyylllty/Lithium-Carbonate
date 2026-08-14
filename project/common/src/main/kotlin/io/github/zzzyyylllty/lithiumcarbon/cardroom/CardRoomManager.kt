package io.github.zzzyyylllty.lithiumcarbon.cardroom

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.cardRoomConfigs
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.cardRoomInstances
import io.github.zzzyyylllty.lithiumcarbon.event.CardRoomPreResetEvent
import io.github.zzzyyylllty.lithiumcarbon.event.CardRoomResetEvent
import io.github.zzzyyylllty.lithiumcarbon.frame.FrameCrateManager
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 卡房状态管理器
 */
object CardRoomManager {

    // 正在重置的卡房集合
    private val resettingRooms = ConcurrentHashMap.newKeySet<String>()

    // 正在激活的卡房集合（防止并发重复激活）
    private val pendingActivations = mutableSetOf<String>()

    /**
     * 尝试获取卡房激活锁
     * @return true 表示成功获取锁，可以继续激活
     */
    fun tryAcquireActivation(configId: String): Boolean {
        return synchronized(pendingActivations) {
            if (pendingActivations.contains(configId) || getInstanceState(configId) == CardRoomState.ACTIVE) {
                false
            } else {
                pendingActivations.add(configId)
                true
            }
        }
    }

    /**
     * 释放卡房激活锁
     */
    fun releaseActivation(configId: String) {
        synchronized(pendingActivations) {
            pendingActivations.remove(configId)
        }
    }

    /**
     * 获取卡房实例状态
     */
    private fun getInstanceState(configId: String): CardRoomState? {
        return cardRoomInstances[configId]?.state
    }

    /**
     * 初始化管理器
     */
    fun init() {
        // 提升代次以取消上一个检查任务
        val gen = LithiumCarbon.cardRoomCheckGeneration.incrementAndGet()
        // 启动定期检查任务（每5秒检查一次）
        submitAsync(period = 100L) { // 100 ticks = 5 seconds
            if (gen != LithiumCarbon.cardRoomCheckGeneration.get()) { cancel(); return@submitAsync }
            checkActiveRooms()
        }
    }

    /**
     * 获取或创建卡房实例
     */
    fun getOrCreateInstance(configId: String): CardRoomInstance {
        return cardRoomInstances.getOrPut(configId) {
            CardRoomInstance(configId = configId)
        }
    }

    /**
     * 激活卡房
     */
    fun activateCardRoom(configId: String, player: Player? = null): Boolean {
        val config = cardRoomConfigs[configId] ?: run {
            devLog("Card room config not found: $configId")
            return false
        }

        val instance = getOrCreateInstance(configId)

        // 如果已经在激活状态，不重复激活
        if (instance.state == CardRoomState.ACTIVE) {
            devLog("Card room $configId is already active")
            return false
        }

        // 激活实例
        instance.activate()

        // 设置重置时间
        val resetDelay = config.reset.delay
        if (resetDelay > 0) {
            instance.nextResetTime = System.currentTimeMillis() + (resetDelay * 1000).toLong()
        }

        devLog("Card room $configId activated by ${player?.name ?: "unknown"}")

        // 触发onOpen事件代理
        config.agents?.runAgent("onOpen", mapOf(
            "config" to config,
            "player" to player,
            "instance" to instance
        ), player)

        return true
    }

    /**
     * 获取指定位置的卡房配置
     */
    fun getCardRoomAtLocation(location: Location): CardRoomConfig? {
        val lootLocation = io.github.zzzyyylllty.lithiumcarbon.data.LocationHelper.toLootLocation(location)

        for ((id, config) in cardRoomConfigs) {
            if (config.trigger.block == lootLocation) {
                return config
            }
        }

        return null
    }

    /**
     * 检查指定卡房区域内是否有玩家
     */
    fun hasPlayersInRange(configId: String): Boolean {
        val config = cardRoomConfigs[configId] ?: return false
        val range = config.reset.range ?: return false

        for (player in Bukkit.getOnlinePlayers()) {
            val playerLoc = io.github.zzzyyylllty.lithiumcarbon.data.LocationHelper.toLootLocation(player.location)
            if (range.contains(playerLoc)) {
                return true
            }
        }

        return false
    }

    /**
     * 检查所有激活的卡房是否需要重置
     */
    private fun checkActiveRooms() {
        val now = System.currentTimeMillis()

        for ((configId, instance) in cardRoomInstances) {
            if (instance.state != CardRoomState.ACTIVE) continue

            // 检查重置时间是否到达
            if (instance.shouldReset()) {
                startReset(configId)
            } else {
                // 如果配置了检测区域，检查区域内是否有玩家
                val config = cardRoomConfigs[configId] ?: continue
                if (config.reset.range != null) {
                    if (!hasPlayersInRange(configId)) {
                        // 区域内没有玩家，检查是否应该启动重置计时器
                        if (instance.nextResetTime == null) {
                            val resetDelay = config.reset.delay
                            if (resetDelay > 0) {
                                instance.nextResetTime = now + (resetDelay * 1000).toLong()
                                devLog("Reset timer started for card room $configId")
                            }
                        }
                    } else {
                        // 区域内还有玩家，清除重置计时器
                        instance.nextResetTime = null
                    }
                }
                // range 为 null 时不干预计时器（由 activation 时设定的 nextResetTime 决定）
            }
        }
    }

    /**
     * 开始重置卡房
     */
    fun startReset(configId: String) {
        if (resettingRooms.contains(configId)) {
            devLog("Card room $configId is already resetting")
            return
        }

        val config = cardRoomConfigs[configId] ?: run {
            devLog("Card room config not found for reset: $configId")
            return
        }

        val instance = cardRoomInstances[configId] ?: run {
            devLog("Card room instance not found for reset: $configId")
            return
        }

        // 触发重置前事件（可取消）
        val preEvent = CardRoomPreResetEvent(config, instance)
        preEvent.call()
        if (preEvent.isCancelled) {
            devLog("Card room $configId pre-reset cancelled by event")
            return
        }

        resettingRooms.add(configId)
        instance.startReset()

        devLog("Starting reset for card room $configId")

        // 触发onReset事件代理
        config.agents?.runAgent("onReset", mapOf(
            "config" to config,
            "instance" to instance
        ), null)

        // 异步执行重置
        submitAsync {
            try {
                resetCardRoom(configId)
            } catch (e: Exception) {
                devLog("Error resetting card room $configId: ${e.message}")
                e.printStackTrace()
            } finally {
                resettingRooms.remove(configId)
            }
        }
    }

    /**
     * 重置卡房
     */
    private fun resetCardRoom(configId: String) {
        val config = cardRoomConfigs[configId] ?: return
        val instance = cardRoomInstances[configId] ?: return

        devLog("Resetting card room $configId")

        // 执行重置动作
        val executor = CardRoomExecutor
        executor.executeActions(config.reset.actions, config, instance, null, async = true)

        // 清除生成的箱子
        instance.spawnedChests.forEach { (location, key) ->
            val lootInstance = LithiumCarbon.lootMap[key]
            lootInstance?.update()
            // 如果未启用还原，则将箱子方块设为空气
            if (!config.reset.restore) {
                location.toBukkitLocation().block.type = org.bukkit.Material.AIR
            }
        }

        // 清除生成的展示框
        instance.spawnedFrames.forEach { location ->
            FrameCrateManager.removeFrame(location)
        }

        // 还原所有修改过的方块（当 restore 为 true 时）
        if (config.reset.restore) {
            instance.modifiedBlocks.forEach { (location, blockData) ->
                try {
                    val bukkitLocation = location.toBukkitLocation()
                    bukkitLocation.block.blockData = blockData
                    devLog("Restored block at $location to original state")
                } catch (e: Exception) {
                    devLog("Failed to restore block at $location: ${e.message}")
                }
            }
        }

        // 完成重置
        instance.completeReset()

        // 触发重置完成事件
        CardRoomResetEvent(config, instance).call()

        // 触发onResetComplete事件代理
        config.agents?.runAgent("onResetComplete", mapOf(
            "config" to config,
            "instance" to instance
        ), null)

        devLog("Card room $configId reset completed")
    }

    /**
     * 同步重置卡房（用于服务器关闭等场景）
     */
    private fun resetCardRoomSync(configId: String) {
        val config = cardRoomConfigs[configId] ?: return
        val instance = cardRoomInstances[configId] ?: return

        devLog("Synchronously resetting card room $configId")

        // 执行重置动作
        val executor = CardRoomExecutor
        executor.executeActions(config.reset.actions, config, instance, null, async = false)

        // 清除生成的箱子
        instance.spawnedChests.forEach { (location, key) ->
            val lootInstance = LithiumCarbon.lootMap[key]
            lootInstance?.update()
            // 如果未启用还原，则将箱子方块设为空气
            if (!config.reset.restore) {
                location.toBukkitLocation().block.type = org.bukkit.Material.AIR
            }
        }

        // 清除生成的展示框
        instance.spawnedFrames.forEach { location ->
            FrameCrateManager.removeFrame(location)
        }

        // 还原所有修改过的方块（同步重置时始终还原，用于插件卸载场景）
        instance.modifiedBlocks.forEach { (location, blockData) ->
            try {
                val bukkitLocation = location.toBukkitLocation()
                bukkitLocation.block.blockData = blockData
                devLog("Restored block at $location to original state")
            } catch (e: Exception) {
                devLog("Failed to restore block at $location: ${e.message}")
            }
        }

        // 完成重置
        instance.completeReset()

        // 触发重置完成事件
        CardRoomResetEvent(config, instance).call()

        devLog("Card room $configId reset completed")
    }

    /**
     * 添加生成的箱子到实例
     */
    fun addSpawnedChest(configId: String, location: io.github.zzzyyylllty.lithiumcarbon.data.LootLocation, key: LithiumCarbon.LootInstanceKey) {
        val instance = cardRoomInstances[configId] ?: return
        instance.spawnedChests[location] = key
    }

    /**
     * 添加修改的方块到实例
     */
    fun addModifiedBlock(configId: String, location: io.github.zzzyyylllty.lithiumcarbon.data.LootLocation, blockData: org.bukkit.block.data.BlockData) {
        val instance = cardRoomInstances[configId] ?: return
        instance.modifiedBlocks[location] = blockData
    }

    /**
     * 获取修改的方块数据
     */
    fun getModifiedBlock(configId: String, location: io.github.zzzyyylllty.lithiumcarbon.data.LootLocation): org.bukkit.block.data.BlockData? {
        val instance = cardRoomInstances[configId] ?: return null
        return instance.modifiedBlocks[location]
    }

    /**
     * 强制重置所有卡房（用于插件重载等）
     */
    fun resetAllCardRooms() {
        devLog("Resetting all card rooms")

        for (configId in cardRoomInstances.keys.toList()) {
            startReset(configId)
        }
    }

    /**
     * 同步强制重置所有卡房（用于服务器关闭等场景）
     */
    fun resetAllCardRoomsSync() {
        devLog("Synchronously resetting all card rooms")

        for (configId in cardRoomInstances.keys.toList()) {
            resetCardRoomSync(configId)
        }
    }
}