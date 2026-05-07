package io.github.zzzyyylllty.lithiumcarbon.frame

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.frameCrateConfigs
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootTemplates
import io.github.zzzyyylllty.lithiumcarbon.data.FrameCrateConfig
import io.github.zzzyyylllty.lithiumcarbon.data.LootElement
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.event.FrameCrateClaimEvent
import io.github.zzzyyylllty.lithiumcarbon.event.FrameCratePreClaimEvent
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.GlowItemFrame
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.warning
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

/**
 * Runtime data for an active frame crate.
 */
data class FrameCrateData(
    val frameCrateConfig: FrameCrateConfig,
    val configId: String,
    val element: LootElement,
    val templateId: String,
    val itemFrame: ItemFrame,
    val spawnedTime: Long,
    val expireTime: Long?,
    var claimed: Boolean = false,
)

/**
 * Manager for frame crate (展示框物资箱) lifecycle.
 */
object FrameCrateManager {

    /** All active frame crates indexed by item frame UUID. */
    val activeFrameCrates = ConcurrentHashMap<UUID, FrameCrateData>()

    /**
     * Spawn an item frame with a generated loot item at the given location.
     *
     * @param location the block location to place the frame at
     * @param configId the frame crate config id
     * @param player the player who triggered the spawn (for loot generation context)
     * @param facing the block face to attach the frame to, null = default
     * @return the item frame entity UUID if successful, null otherwise
     */
    fun spawnFrame(
        location: Location,
        configId: String,
        player: Player?,
        facing: String? = null,
    ): UUID? {
        val config = frameCrateConfigs[configId] ?: run {
            devLog("Frame crate config not found: $configId")
            return null
        }

        val world = location.world ?: return null

        // Get a valid player context for loot generation
        val contextPlayer = player ?: Bukkit.getOnlinePlayers().firstOrNull()
        if (contextPlayer == null) {
            devLog("No player context available for loot generation")
            return null
        }

        // Get the loot template and generate exactly 1 item
        val template = lootTemplates[config.lootTemplate] ?: run {
            devLog("Loot template not found: ${config.lootTemplate} for frame crate $configId")
            return null
        }

        val elements = template.generateElements(contextPlayer, true)
        val element = elements.values.firstNotNullOfOrNull { it } ?: run {
            devLog("Failed to generate any items from template ${config.lootTemplate}")
            return null
        }

        // Build the display item from the generated element
        val lootItem = element.items?.firstOrNull() ?: run {
            devLog("Generated element has no items")
            return null
        }
        val displayStack = lootItem.build(contextPlayer)
        displayStack.amount = 1 // item frames show amount=1

        // Spawn the item frame (normal or glow)
        var frame: ItemFrame? = null
        submit {
            frame = if (config.glow) {
                world.spawn(location, GlowItemFrame::class.java)
            } else {
                world.spawn(location, ItemFrame::class.java)
            }

            // Set facing direction
            if (facing != null) {
                try {
                    val blockFace = org.bukkit.block.BlockFace.valueOf(facing.uppercase())
                    frame.setFacingDirection(blockFace)
                } catch (e: IllegalArgumentException) {
                    devLog("Invalid facing direction: $facing, using default")
                }
            }

            // Set item in frame
            frame.setItem(displayStack)
            frame.setFixed(true)
            frame.setVisible(true)
        }
        // Calculate expire time
        val expireTime = config.expire?.let {
            (it.toDoubleOrNull() ?: 0.0).let { exp ->
                if (exp > 0) System.currentTimeMillis() + (exp * 1000).roundToLong() else null
            }
        }
        if (frame == null) {
            warning("item frame creation failed.")
            return null
        }

        val data = FrameCrateData(
            frameCrateConfig = config,
            configId = configId,
            element = element,
            templateId = config.lootTemplate,
            itemFrame = frame,
            spawnedTime = System.currentTimeMillis(),
            expireTime = expireTime,
        )

        activeFrameCrates[frame.uniqueId] = data

        devLog("Spawned frame crate $configId at $location (frame UUID: ${frame.uniqueId})")
        return frame.uniqueId
    }

    /**
     * Remove an item frame crate at the given location.
     */
    fun removeFrame(location: LootLocation): Boolean {
        val bukkitLoc = location.toBukkitLocation()
        return removeFrameAtLocation(bukkitLoc)
    }

    /**
     * Remove an item frame crate by location.
     */
    fun removeFrameAtLocation(location: Location): Boolean {
        val frame = activeFrameCrates.entries.find { (_, data) ->
            val frameLoc = data.itemFrame.location
            frameLoc.blockX == location.blockX &&
            frameLoc.blockY == location.blockY &&
            frameLoc.blockZ == location.blockZ &&
            frameLoc.world?.name == location.world?.name
        } ?: return false

        submit { frame.value.itemFrame.remove() }
        activeFrameCrates.remove(frame.key)
        devLog("Removed frame crate at $location")
        return true
    }

    /**
     * Remove a frame crate by its entity UUID.
     */
    fun removeFrameByUUID(uuid: UUID): Boolean {
        val data = activeFrameCrates[uuid] ?: return false
        data.itemFrame.remove()
        activeFrameCrates.remove(uuid)
        devLog("Removed frame crate (UUID: $uuid)")
        return true
    }

    /**
     * Handle a player claiming an item from a frame crate.
     */
    fun claimFrame(frameUuid: UUID, player: Player, entity: Entity): Boolean {
        val data = activeFrameCrates[frameUuid] ?: return false

        if (data.claimed) {
            devLog("Frame crate ${data.configId} already claimed")
            return false
        }

        // Check expire
        if (data.expireTime != null && System.currentTimeMillis() > data.expireTime) {
            devLog("Frame crate ${data.configId} has expired")
            data.itemFrame.remove()
            activeFrameCrates.remove(frameUuid)
            return false
        }

        // 触发领取前事件（可取消）
        val preEvent = FrameCratePreClaimEvent(data.frameCrateConfig, data, player, entity)
        preEvent.call()
        if (preEvent.isCancelled) {
            devLog("Frame crate ${data.configId} claim cancelled by event")
            return false
        }

        // Mark as claimed
        data.claimed = true

        // Apply the generated element to player (gives item, exp, runs scripts)
        val template = lootTemplates[data.templateId]
        if (template != null) {
            data.element.applyToPlayer(player, template)
        }

        // Run agents
        data.frameCrateConfig.agents?.runAgent("onClaim", mapOf(
            "player" to player,
            "frameCrateConfig" to data.frameCrateConfig,
            "frameCrateData" to data,
            "frame" to entity,
        ), player)

        // Remove the frame entity
        data.itemFrame.remove()
        activeFrameCrates.remove(frameUuid)

        // 触发领取完成事件
        FrameCrateClaimEvent(data.frameCrateConfig, data, player).call()

        devLog("Player ${player.name} claimed frame crate ${data.configId}")
        return true
    }

    /**
     * Check if a location has an active frame crate.
     */
    fun hasFrameAt(location: Location): Boolean {
        return activeFrameCrates.values.any { data ->
            val fl = data.itemFrame.location
            fl.blockX == location.blockX &&
            fl.blockY == location.blockY &&
            fl.blockZ == location.blockZ &&
            fl.world?.name == location.world?.name
        }
    }

    /**
     * Get frame crate data by frame UUID.
     */
    fun getFrameData(uuid: UUID): FrameCrateData? = activeFrameCrates[uuid]

    /**
     * Clean up expired frame crates.
     */
    fun cleanupExpired() {
        val now = System.currentTimeMillis()
        activeFrameCrates.entries.removeIf { (_, data) ->
            if (data.expireTime != null && now > data.expireTime) {
                data.itemFrame.remove()
                devLog("Removed expired frame crate ${data.configId}")
                true
            } else {
                false
            }
        }
    }

    /**
     * Remove all active frame crates (for plugin disable/reload).
     */
    fun removeAll() {
        activeFrameCrates.values.forEach { data ->
            data.itemFrame.remove()
        }
        activeFrameCrates.clear()
        devLog("Removed all frame crates")
    }
}
