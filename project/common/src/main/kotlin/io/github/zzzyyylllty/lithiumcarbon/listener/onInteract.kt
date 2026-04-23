package io.github.zzzyyylllty.lithiumcarbon.listener

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.allowedWorlds
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.config
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootCaches
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootDefines
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.LootInstanceKey
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootMap
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootTemplates
import io.github.zzzyyylllty.lithiumcarbon.cardroom.CardRoomManager
import io.github.zzzyyylllty.lithiumcarbon.data.LocationHelper
import io.github.zzzyyylllty.lithiumcarbon.data.LootInstance
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.data.LootTemplate
import io.github.zzzyyylllty.lithiumcarbon.gui.openLootChest
import io.github.zzzyyylllty.lithiumcarbon.util.DependencyHelper
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import kotlin.collections.get

@SubscribeEvent
fun onInteract(e: PlayerInteractEvent) {
    if (e.action != Action.RIGHT_CLICK_BLOCK) return
    val block = e.clickedBlock ?: return
    if (block.type.isAir) return
    val player = e.player ?: return // 防止 NPC 搞鬼

    // 检查事件是否已被取消（例如被卡房系统取消）
    if (e.isCancelled) return

    // 检查是否为卡房触发位置（如果是，让卡房系统处理）
    val cardRoomConfig = CardRoomManager.getCardRoomAtLocation(block.location)
    if (cardRoomConfig != null) {
        // 卡房系统会处理这个位置，我们不处理
        return
    }
    if (config.getBoolean("allowed-all-blocks", false) || config.getList("allowed-blocks")?.contains(block.type.name) ?: false) {

        if (!config.getBoolean("allowed-all-worlds", false)) {
            val world = block.world.name
            var passed = false
            for (regex in allowedWorlds) {
                if (world.matches(regex)) {
                    passed = true
                    break
                }
            }
            if (!passed) return
        }

        val location = LocationHelper.toLootLocation(block.location)
        val define =
//            if (DependencyHelper.wg) {
//                player ?: getDefines(location, block, player)
//            }
//            else
                getDefines(location, block, player)
            ?: run {
            devLog("Define is null, return.")
            return
        }
        e.isCancelled = true
        submitAsync {
            val key = if (define.options.private) LootInstanceKey(location, player.uniqueId) else LootInstanceKey(location, null)
            // 当前战利品
            val current = lootMap[key]

            // 更新后的战利品
            lateinit var instance: LootInstance
            if (current == null) {
                devLog("CURRENT LootInstance is null, regenerating.")
                instance = lootMap.getOrPut(key) {
                    define.createInstance(block, player)
                }
            } else {
                val pendingInstance = current.checkUpdate()
                instance = pendingInstance ?: run {
                    lootMap.getOrPut(key) {
                        define.createInstance(block, player)
                    }
                }
            }
            submit { player.openLootChest(instance) }
        }
    } else {
        return
    }
}

fun getDefines(location: LootLocation, block: Block, player: Player): LootTemplate? {
    return if (lootCaches[location] == null) {
        val define = getDefinesWithoutCache(location, block, player)
        define?.let { lootCaches[location] = it }
        define
    } else {
        lootCaches[location]
    }
}

fun getDefinesWithoutCache(location: LootLocation, block: Block, player: Player): LootTemplate? {
    for (it in lootDefines) {
        if (it.value.isValidLocation(location, block, player)) return lootTemplates[it.key]
    }
    return null
}