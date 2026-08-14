package io.github.zzzyyylllty.lithiumcarbon.cardroom

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon
import io.github.zzzyyylllty.lithiumcarbon.data.LocationHelper
import io.github.zzzyyylllty.lithiumcarbon.event.CardRoomOpenEvent
import io.github.zzzyyylllty.lithiumcarbon.event.CardRoomPreOpenEvent
import io.github.zzzyyylllty.lithiumcarbon.util.ItemTagUtilExtensions
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import taboolib.common.platform.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submitAsync
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object CardRoomListener : Listener {

    private val playerCooldowns = ConcurrentHashMap<UUID, Long>()
    private const val COOLDOWN_MS = 500L

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onInteract(e: PlayerInteractEvent) {
        if (e.action != Action.RIGHT_CLICK_BLOCK) return
        if (e.hand == EquipmentSlot.OFF_HAND) return

        val block = e.clickedBlock ?: return
        if (block.type.isAir) return

        val player = e.player ?: return

        val now = System.currentTimeMillis()
        val lastActivation = playerCooldowns[player.uniqueId]
        if (lastActivation != null && now - lastActivation < COOLDOWN_MS) return
        playerCooldowns[player.uniqueId] = now

        val config = CardRoomManager.getCardRoomAtLocation(block.location) ?: return

        // 从上到下匹配物品，首匹配决定消耗方式
        val matchedConsume = findMatchingItem(config, player)
        if (matchedConsume == null) {
            config.agents?.runAgent("onWrongKey", mapOf("player" to player, "config" to config, "block" to block), player)
            return
        }

        // 检查条件
        if (!checkTriggerConditions(config, player, block)) return

        e.isCancelled = true

        submitAsync {
            try {
                activateCardRoom(config, player, block, matchedConsume)
            } catch (ex: Exception) {
                devLog("Error activating card room: ${ex.message}")
                ex.printStackTrace()
            }
        }
    }

    /**
     * 从上到下匹配物品，返回匹配到的消耗配置
     */
    private fun findMatchingItem(config: CardRoomConfig, player: Player): ConsumeKeyConfig? {
        val itemInHand = player.inventory.itemInMainHand
        for (matcher in config.trigger.items) {
            if (checkItemTag(itemInHand, matcher.tag)) {
                devLog("Matched item tag: ${matcher.tag} for ${player.name}")
                return matcher.consumeKey
            }
        }
        devLog("No matching item found for ${player.name}")
        return null
    }

    private fun checkTriggerConditions(
        config: CardRoomConfig,
        player: Player,
        block: Block
    ): Boolean {
        val blockLocation = LocationHelper.toLootLocation(block.location)
        if (config.trigger.block != blockLocation) {
            devLog("Block location mismatch: ${blockLocation} != ${config.trigger.block}")
            return false
        }

        val condition = config.trigger.condition
        if (condition != null) {
            val itemInHand = player.inventory.itemInMainHand
            val extraVariables = mapOf<String, Any?>(
                "player" to player,
                "block" to block,
                "item" to itemInHand,
                "location" to block.location
            )

            if (!condition.validate(extraVariables, player)) {
                devLog("Condition check failed for ${player.name}")
                config.agents?.runAgent("onConditionFail", mapOf("player" to player, "config" to config, "block" to block, "condition" to condition), player)
                return false
            }
        }

        return true
    }

    private fun checkItemTag(item: ItemStack?, requiredTag: Map<String, Any>?): Boolean {
        if (requiredTag == null || requiredTag.isEmpty()) return true
        if (item == null || item.type == Material.AIR) return false
        return ItemTagUtilExtensions.hasItemTag(item, requiredTag)
    }

    private fun activateCardRoom(config: CardRoomConfig, player: Player, block: Block, consumeConfig: ConsumeKeyConfig) {
        devLog("Activating card room ${config.id} for ${player.name}")

        if (!CardRoomManager.tryAcquireActivation(config.id)) {
            config.agents?.runAgent("onAlreadyActive", mapOf("player" to player, "config" to config, "block" to block), player)
            return
        }
        try {
            val preEvent = CardRoomPreOpenEvent(config, player, block)
            preEvent.call()
            if (preEvent.isCancelled) {
                devLog("Card room ${config.id} pre-open cancelled by event")
                config.agents?.runAgent("onOpeningBlocked", mapOf("player" to player, "config" to config, "block" to block), player)
                return
            }

            val instance = CardRoomManager.getOrCreateInstance(config.id)

            // 使用匹配到的消耗配置
            CardRoomExecutor.consumePlayerItem(player, consumeConfig)

            CardRoomExecutor.executeActions(config.actions, config, instance, player)

            CardRoomManager.activateCardRoom(config.id, player)

            CardRoomOpenEvent(config, player, instance).call()

            config.agents?.runAgent("onActivate", mapOf("player" to player, "config" to config, "instance" to instance), player)

        } finally {
            CardRoomManager.releaseActivation(config.id)
        }
    }
}
