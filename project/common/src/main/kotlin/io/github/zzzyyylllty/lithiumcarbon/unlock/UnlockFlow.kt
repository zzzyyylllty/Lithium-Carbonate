package io.github.zzzyyylllty.lithiumcarbon.unlock

import org.bukkit.entity.Player

interface UnlockFlow {
    val typeId: String

    fun start(context: UnlockContext): Boolean

    fun cancel(player: Player)

    fun isActive(player: Player): Boolean

    fun cleanup()
}
