package io.github.zzzyyylllty.lithiumcarbon.event

import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockContext
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockFlowRegistry
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

class UnlockFlowRegisterEvent(
    val registry: UnlockFlowRegistry,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

class UnlockFlowPreStartEvent(
    val player: Player,
    val context: UnlockContext,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()

class UnlockFlowStartEvent(
    val player: Player,
    val context: UnlockContext,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

class UnlockFlowProgressEvent(
    val player: Player,
    val context: UnlockContext,
    val progress: Int,
    val total: Int,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

class UnlockFlowCompleteEvent(
    val player: Player,
    val context: UnlockContext,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

class UnlockFlowFailEvent(
    val player: Player,
    val context: UnlockContext,
    val reason: String,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

class UnlockFlowCancelEvent(
    val player: Player,
    val context: UnlockContext,
    val reason: String,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()
