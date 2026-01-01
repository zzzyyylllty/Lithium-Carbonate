package io.github.zzzyyylllty.lithiumcarbon.listener

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.allowedWorlds
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.config
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootCaches
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootDefines
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootMap
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootTemplates
import io.github.zzzyyylllty.lithiumcarbon.data.LocationHelper
import io.github.zzzyyylllty.lithiumcarbon.data.LootInstance
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.data.LootTemplate
import io.github.zzzyyylllty.lithiumcarbon.gui.openLootChest
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submitAsync
import kotlin.collections.get

@SubscribeEvent
fun onInteract(e: PlayerInteractEvent) {
    if (e.action != Action.RIGHT_CLICK_BLOCK) return
    val block = e.clickedBlock ?: return
    if (block.type.isAir) return
    val player = e.player ?: return // 防止 NPC 搞鬼
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
        val define = getDefines(location, block, player) ?: run {
            devLog("Define is null, return.")
            return
        }
        e.isCancelled = true
        submitAsync {

            // 当前战利品
            val current = lootMap[location]

            // 更新后的战利品
            lateinit var instance: LootInstance
            if (current == null) {
                devLog("CURRENT LootInstance is null, regenerating.")
                instance = lootMap.getOrPut(location) {
                    define.createInstance(block, player)
                }
            } else {
                val pendingInstance = current.checkUpdate()
                instance = pendingInstance ?: run {
                    lootMap.getOrPut(location) {
                        define.createInstance(block, player)
                    }
                }
            }
            player.openLootChest(instance)
        }
    } else {
        return
    }
}

fun getDefines(location: LootLocation, block: Block, player: Player): LootTemplate? {
    return getDefinesWithoutCache(location, block, player)?.let {
        lootCaches.getOrPut(location) {
            it
        }
    }
}

fun getDefinesWithoutCache(location: LootLocation, block: Block, player: Player): LootTemplate? {
    for (it in lootDefines) {
        if (it.value.isValidLocation(location, block, player)) return lootTemplates[it.key]
    }
    return null
}