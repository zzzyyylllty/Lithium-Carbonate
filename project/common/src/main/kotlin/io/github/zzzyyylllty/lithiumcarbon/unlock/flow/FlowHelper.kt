package io.github.zzzyyylllty.lithiumcarbon.unlock.flow

import io.github.zzzyyylllty.lithiumcarbon.gui.openLootChest
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockContext
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockStateManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit

fun notifySharedSuccess(context: UnlockContext, successPlayer: Player) {
    val loc = context.lootInstance.loc
    val otherPlayers = UnlockStateManager.getPlayersAtLocation(loc).filter { it != successPlayer.uniqueId }
    for (uid in otherPlayers) {
        val other = Bukkit.getPlayer(uid) ?: continue
        other.sendMessage("§e${successPlayer.name} §7已解锁该战利品箱。")
        val otherActive = UnlockStateManager.getActive(other)
        if (otherActive != null) {
            otherActive.flow.cancel(other)
            UnlockStateManager.removeActive(other)
            if (context.lightConfig.sharedCompleteAction == "open") {
                submit { other.openLootChest(context.lootInstance, context.event) }
            }
        }
    }
}
