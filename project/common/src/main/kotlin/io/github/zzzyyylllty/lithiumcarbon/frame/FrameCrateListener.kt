package io.github.zzzyyylllty.lithiumcarbon.frame

import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import org.bukkit.entity.GlowItemFrame
import org.bukkit.entity.ItemFrame
import org.bukkit.event.player.PlayerInteractEntityEvent
import taboolib.common.platform.event.SubscribeEvent

@SubscribeEvent
fun onFrameCrateClaim(e: PlayerInteractEntityEvent) {
    val entity = e.rightClicked
    if (entity !is ItemFrame) return
    if (!FrameCrateManager.activeFrameCrates.containsKey(entity.uniqueId)) return

    val player = e.player
    e.isCancelled = true

    val success = FrameCrateManager.claimFrame(entity.uniqueId, player, entity)
    if (success) {
        devLog("Player ${player.name} claimed frame crate from ${entity.location}")
    }
}
