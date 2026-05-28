package io.github.zzzyyylllty.lithiumcarbon.cardroom

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon
import io.github.zzzyyylllty.lithiumcarbon.data.*
import org.bukkit.block.data.BlockData
import java.util.UUID

/**
 * 卡房状态枚举
 */
enum class CardRoomState {
    IDLE,      // 未激活
    ACTIVE,    // 已激活
    RESETTING  // 重置中
}

/**
 * 区域范围，用于玩家检测
 */
data class AreaRange(
    val from: LootLocation,
    val to: LootLocation
) {
    /**
     * 检查位置是否在范围内
     */
    fun contains(location: LootLocation): Boolean {
        if (from.world != location.world || to.world != location.world) return false
        val minX = minOf(from.x, to.x)
        val maxX = maxOf(from.x, to.x)
        val minY = minOf(from.y, to.y)
        val maxY = maxOf(from.y, to.y)
        val minZ = minOf(from.z, to.z)
        val maxZ = maxOf(from.z, to.z)

        return location.x in minX..maxX &&
               location.y in minY..maxY &&
               location.z in minZ..maxZ
    }
}

/**
 * 钥匙消耗模式
 */
enum class ConsumeMode {
    ITEM,       // 减少物品数量
    DURABILITY, // 降低耐久
    TAG,        // 修改NBT标签值
    NONE        // 无操作
}

/**
 * 钥匙消耗配置
 */
data class ConsumeKeyConfig(
    val mode: ConsumeMode = ConsumeMode.ITEM,
    val tag: String? = null,
    val value: Int = 1
)

/**
 * 物品匹配规则
 */
data class ItemMatcher(
    val tag: Map<String, Any>?,           // NBT标签要求（为空则匹配任意物品）
    val consumeKey: ConsumeKeyConfig      // 匹配成功后如何消耗
)

/**
 * 触发配置
 */
data class TriggerConfig(
    val block: LootLocation,              // 触发方块位置
    val items: List<ItemMatcher>,         // 物品匹配列表（从上到下，首匹配）
    val condition: Condition?             // 额外条件
)

/**
 * 动作配置（密封类）
 */
sealed class ActionConfig(val type: String) {
    abstract val location: LootLocation?

    // 移除方块动作
    data class RemoveBlockAction(
        override val location: LootLocation,
        val block: String? = null  // 可选：验证要移除的方块类型
    ) : ActionConfig("remove-block")

    // 设置方块动作
    data class SetBlockAction(
        override val location: LootLocation,
        val block: String          // 方块类型
    ) : ActionConfig("set-block")

    // 门操作动作
    data class DoorAction(
        override val location: LootLocation,
        val direction: String?,    // 可选：门方向（NORTH, EAST, SOUTH, WEST）
        val open: Boolean          // true=开门，false=关门
    ) : ActionConfig("door")

    // 生成物资箱动作
    data class SpawnChestAction(
        override val location: LootLocation,
        val lootTemplate: String,  // 战利品模板ID
        val private: Boolean = false, // 是否为私有箱子
        val block: String = "CHEST" // 方块类型，默认为箱子
    ) : ActionConfig("spawn-chest")

    // 生成展示框动作
    data class SpawnFrameAction(
        override val location: LootLocation,
        val frameCrateConfig: String,  // 展示框物资箱配置ID
        val facing: String? = null,    // 可选：展示框朝向
        val removeExisting: Boolean = true // 是否在生成前移除当前位置的展示框（防止服务器意外关闭后遗留的展示框冲突）
    ) : ActionConfig("spawn-frame")

    // 移除展示框动作
    data class RemoveFrameAction(
        override val location: LootLocation
    ) : ActionConfig("remove-frame")

    // 执行脚本动作
    data class ExecuteScriptAction(
        override val location: LootLocation? = null,
        val js: String? = null,    // JavaScript脚本
        val kether: String? = null // Kether脚本
    ) : ActionConfig("execute-script")
}

/**
 * 重置配置
 */
data class ResetConfig(
    val range: AreaRange?,          // 检测玩家区域
    val delay: Double,              // 没有玩家后重置延迟（秒）
    val restore: Boolean,           // 是否恢复环境
    val actions: List<ActionConfig> // 重置时执行的动作
)

/**
 * 卡房配置
 */
data class CardRoomConfig(
    val id: String,                 // 唯一标识
    val name: String,               // 显示名称
    val trigger: TriggerConfig,     // 触发配置
    val actions: List<ActionConfig>, // 动作列表
    val reset: ResetConfig,         // 重置配置
    val agents: Agents?             // 事件代理
)

/**
 * 卡房实例状态
 */
data class CardRoomInstance(
    val configId: String,                              // 配置ID
    var state: CardRoomState = CardRoomState.IDLE,     // 当前状态
    var activatedTime: Long = 0,                       // 激活时间
    val spawnedChests: MutableMap<LootLocation, LithiumCarbon.LootInstanceKey> = mutableMapOf(), // 生成的箱子
    val spawnedFrames: MutableList<LootLocation> = mutableListOf(), // 生成的展示框位置
    val modifiedBlocks: MutableMap<LootLocation, BlockData> = mutableMapOf(),       // 修改的方块
    var nextResetTime: Long? = null                    // 下次重置时间
) {
    /**
     * 检查是否需要重置
     */
    fun shouldReset(): Boolean {
        if (state != CardRoomState.ACTIVE) return false
        return nextResetTime?.let { it <= System.currentTimeMillis() } ?: false
    }

    /**
     * 激活卡房
     */
    fun activate() {
        state = CardRoomState.ACTIVE
        activatedTime = System.currentTimeMillis()
    }

    /**
     * 开始重置
     */
    fun startReset() {
        state = CardRoomState.RESETTING
    }

    /**
     * 完成重置
     */
    fun completeReset() {
        state = CardRoomState.IDLE
        activatedTime = 0
        spawnedChests.clear()
        spawnedFrames.clear()
        modifiedBlocks.clear()
        nextResetTime = null
    }
}