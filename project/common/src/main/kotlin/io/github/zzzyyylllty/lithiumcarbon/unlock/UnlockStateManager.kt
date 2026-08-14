package io.github.zzzyyylllty.lithiumcarbon.unlock

import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object UnlockStateManager {
    private val activeFlows = ConcurrentHashMap<UUID, ActiveUnlock>()
    private val locationPlayers = ConcurrentHashMap<LootLocation, MutableSet<UUID>>()

    data class ActiveUnlock(
        val flow: UnlockFlow,
        val context: UnlockContext,
        val startedAt: Long = System.currentTimeMillis(),
        var cancelled: Boolean = false,
    )

    fun getActive(player: Player): ActiveUnlock? = activeFlows[player.uniqueId]

    fun setActive(player: Player, context: UnlockContext, flow: UnlockFlow) {
        activeFlows[player.uniqueId] = ActiveUnlock(flow, context)
        if (context.lightConfig.shared) {
            locationPlayers.getOrPut(context.lootInstance.loc) { ConcurrentHashMap.newKeySet() }.add(player.uniqueId)
        }
    }

    fun removeActive(player: Player) {
        val active = activeFlows.remove(player.uniqueId) ?: return
        if (active.context.lightConfig.shared) {
            val players = locationPlayers[active.context.lootInstance.loc]
            players?.remove(player.uniqueId)
            if (players.isNullOrEmpty()) {
                locationPlayers.remove(active.context.lootInstance.loc)
            }
        }
    }

    fun getPlayersAtLocation(loc: LootLocation): Set<UUID> {
        return locationPlayers[loc]?.toSet() ?: emptySet()
    }

    fun cleanupAll() {
        activeFlows.values.forEach {
            it.cancelled = true
            it.flow.cancel(it.context.player)
        }
        activeFlows.clear()
        locationPlayers.clear()
    }
}
