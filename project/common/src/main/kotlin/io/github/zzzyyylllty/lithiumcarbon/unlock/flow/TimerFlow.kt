package io.github.zzzyyylllty.lithiumcarbon.unlock.flow

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootItems
import io.github.zzzyyylllty.lithiumcarbon.data.defaultData
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowCancelEvent
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowCompleteEvent
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowStartEvent
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockContext
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockFlow
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockStateManager
import io.github.zzzyyylllty.lithiumcarbon.util.SoundUtil.playConfiguredSound
import io.github.zzzyyylllty.lithiumcarbon.util.mmJsonUtil
import io.github.zzzyyylllty.lithiumcarbon.util.mmUtil
import net.kyori.adventure.bossbar.BossBar
import taboolib.common5.compileJS
import javax.script.SimpleBindings
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import taboolib.expansion.submitChain
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object TimerFlow : UnlockFlow {
    override val typeId = "timer"

    private data class State(
        var remainingMs: Long = 0,
        var totalMs: Long = 0,
        var closed: Boolean = false,
        var completed: Boolean = false,
        var bossBar: BossBar? = null,
        var guiCancel: (() -> Unit)? = null,
        var bossBarCancel: (() -> Unit)? = null,
        var moveCancel: (() -> Unit)? = null,
        var actionBarCancel: (() -> Unit)? = null,
    )

    private val states = ConcurrentHashMap<UUID, State>()

    override fun start(context: UnlockContext): Boolean {
        val player = context.player
        val config = context.uiConfig.timer ?: return false

        val durationStr = context.lightConfig.overrides["duration"]?.toString() ?: config.duration
        val durationSec = if (durationStr.startsWith("js:")) {
            val script = durationStr.removePrefix("js:").compileJS()
            try {
                script?.eval(SimpleBindings(defaultData + mapOf("player" to player, "context" to context)))?.toString()?.toDoubleOrNull() ?: 5.0
            } catch (_: Exception) { 5.0 }
        } else durationStr.toDoubleOrNull() ?: 5.0
        val totalMs = (durationSec * 1000).toLong()

        val state = State(remainingMs = totalMs, totalMs = totalMs)
        states[player.uniqueId] = state

        UnlockStateManager.setActive(player, context, this)
        context.uiConfig.agents?.runAgent("onStart", linkedMapOf("context" to context, "duration" to durationSec), player)
        UnlockFlowStartEvent(player, context).call()

        when (config.displayType.lowercase()) {
            "boss_bar" -> startBossBar(player, context, config, state)
            "action_bar" -> startActionBar(player, context, config, state)
            else -> startGUI(player, context, config, state)
        }
        return true
    }

    private fun startGUI(player: Player, context: UnlockContext, config: io.github.zzzyyylllty.lithiumcarbon.unlock.TimerConfig, state: State) {
        val progressItem = config.progressItem
        val completeItem = config.completeItem

        player.openMenu<Chest>(mmJsonUtil.serialize(mmUtil.deserialize("<black>" + context.uiConfig.title))) {
            rows(context.uiConfig.rows)
            handLocked(true)
            map(*context.uiConfig.layout.toTypedArray())

            for ((char, item) in context.uiConfig.items) {
                set(char, item.build(player))
            }
            for ((char, item) in lootItems) {
                set(char, item.build(player))
            }
            context.uiConfig.info?.let { info ->
                set(info.slot, info.item.build(player))
            }

            val fillSlots = context.uiConfig.layout
                .flatMapIndexed { row, line ->
                    line.mapIndexedNotNull { col, ch ->
                        if (ch == ' ') row * 9 + col else null
                    }
                }

            onBuild(async = true) { _, inventory ->
                val guiTask = submitAsync(period = 10) {
                    if (state.closed || state.completed) { cancel(); return@submitAsync }
                    state.remainingMs -= 500L
                    if (state.remainingMs <= 0) {
                        state.remainingMs = 0
                        state.completed = true
                        cancel()
                        submitChain {
                            sync {
                                config.soundComplete?.let { playConfiguredSound(player, it) }
                                fillSlots.forEach { s -> inventory.setItem(s, completeItem.build(player)) }
                                context.uiConfig.info?.let { info -> inventory.setItem(info.slot, info.item.build(player)) }
                            }
                        }
                        submit(delay = 20) {
                            if (!state.closed) player.closeInventory()
                            handleSuccess(context, state)
                        }
                        return@submitAsync
                    }
                    submitChain {
                        sync {
                            val elapsedRatio = 1.0 - (state.remainingMs.toDouble() / state.totalMs)
                            val filledCount = (fillSlots.size * elapsedRatio).toInt().coerceIn(0, fillSlots.size)
                            val remainingSec = (state.remainingMs / 1000).toInt() + 1

                            fillSlots.forEachIndexed { idx, s ->
                                if (idx < fillSlots.size - filledCount) {
                                    val pItem = progressItem.build(player)
                                    val meta = pItem.itemMeta
                                    if (meta != null) {
                                        val currentName = meta.displayName()?.let { mmUtil.serialize(it) }
                                        val updatedName = currentName?.replace("%remaining%", "${remainingSec}s")
                                        if (updatedName != null) {
                                            meta.displayName(mmUtil.deserialize(updatedName))
                                        }
                                        pItem.itemMeta = meta
                                    }
                                    inventory.setItem(s, pItem)
                                } else {
                                    inventory.setItem(s, completeItem.build(player))
                                }
                            }
                            context.uiConfig.info?.let { info -> inventory.setItem(info.slot, info.item.build(player)) }
                            context.uiConfig.agents?.runAgent("onProgress", linkedMapOf("context" to context, "remainingMs" to state.remainingMs, "totalMs" to state.totalMs), player)
                        }
                    }
                }
                state.guiCancel = { guiTask.cancel() }
            }

            onClose {
                state.closed = true
                state.bossBar?.let { Bukkit.getServer().hideBossBar(it) }
                if (!state.completed && states.containsKey(player.uniqueId)) {
                    states.remove(player.uniqueId)
                    UnlockStateManager.removeActive(player)
                    context.uiConfig.agents?.runAgent("onCancel", linkedMapOf("context" to context, "reason" to "closed"), player)
                    UnlockFlowCancelEvent(player, context, "closed").call()
                    context.onFail("closed")
                }
            }
        }
    }

    private fun startBossBar(player: Player, context: UnlockContext, config: io.github.zzzyyylllty.lithiumcarbon.unlock.TimerConfig, state: State) {
        val barColor = try {
            BossBar.Color.valueOf(config.bossBarColor?.uppercase() ?: "BLUE")
        } catch (_: IllegalArgumentException) {
            BossBar.Color.BLUE
        }
        val bar = BossBar.bossBar(
            MiniMessage.miniMessage().deserialize(context.uiConfig.title),
            1.0f,
            barColor,
            BossBar.Overlay.PROGRESS
        )
        player.showBossBar(bar)
        state.bossBar = bar

        val bossBarTask = submitAsync(period = 10) {
            if (state.closed || state.completed) { cancel(); return@submitAsync }
            state.remainingMs -= 500L
            if (state.remainingMs <= 0) {
                state.remainingMs = 0
                state.completed = true
                cancel()
                submitChain {
                    sync {
                        config.soundComplete?.let { playConfiguredSound(player, it) }
                        Bukkit.getServer().hideBossBar(bar)
                    }
                }
                handleSuccess(context, state)
                return@submitAsync
            }
            submitChain {
                sync {
                    val ratio = state.remainingMs.toFloat() / state.totalMs
                    bar.progress(ratio.coerceIn(0f, 1f))
                    val remainingSec = (state.remainingMs / 1000).toInt() + 1
                    bar.name(MiniMessage.miniMessage().deserialize("${context.uiConfig.title} §7- §f${remainingSec}s"))
                    context.uiConfig.agents?.runAgent("onProgress", linkedMapOf("context" to context, "remainingMs" to state.remainingMs, "totalMs" to state.totalMs), player)
                }
            }
        }
        state.bossBarCancel = { bossBarTask.cancel() }

        if (config.cancelOnMove) {
            val startLoc = player.location.clone()
            val moveTask = submitAsync(period = 20) {
                if (state.closed || state.completed) { cancel(); return@submitAsync }
                if (player.location.distanceSquared(startLoc) > 1.0) {
                    state.closed = true
                    state.bossBarCancel?.invoke()
                    cancel()
                    submitChain {
                        sync {
                            Bukkit.getServer().hideBossBar(bar)
                            player.sendMessage("§c移动取消了开锁。")
                            states.remove(player.uniqueId)
                            UnlockStateManager.removeActive(player)
                            context.uiConfig.agents?.runAgent("onCancel", linkedMapOf("context" to context, "reason" to "moved"), player)
                            context.onFail("moved")
                        }
                    }
                }
            }
            state.moveCancel = { moveTask.cancel() }
        }
    }

    private fun startActionBar(player: Player, context: UnlockContext, config: io.github.zzzyyylllty.lithiumcarbon.unlock.TimerConfig, state: State) {
        val actionBarTask = submitAsync(period = 10) {
            if (state.closed || state.completed) { cancel(); return@submitAsync }
            state.remainingMs -= 500L
            if (state.remainingMs <= 0) {
                state.remainingMs = 0
                state.completed = true
                cancel()
                config.soundComplete?.let { playConfiguredSound(player, it) }
                handleSuccess(context, state)
                return@submitAsync
            }
            submitChain {
                sync {
                    val remainingSec = (state.remainingMs / 1000).toInt() + 1
                    player.sendActionBar(MiniMessage.miniMessage().deserialize("<gray>开锁中... <yellow>${remainingSec}s"))
                    context.uiConfig.agents?.runAgent("onProgress", linkedMapOf("context" to context, "remainingMs" to state.remainingMs, "totalMs" to state.totalMs), player)
                }
            }
        }
        state.actionBarCancel = { actionBarTask.cancel() }
    }

    override fun cancel(player: Player) {
        val state = states.remove(player.uniqueId) ?: return
        state.closed = true
        state.bossBar?.let { Bukkit.getServer().hideBossBar(it) }
        state.guiCancel?.invoke()
        state.bossBarCancel?.invoke()
        state.moveCancel?.invoke()
        state.actionBarCancel?.invoke()
        player.closeInventory()
    }

    override fun isActive(player: Player): Boolean = states.containsKey(player.uniqueId)

    override fun cleanup() {
        states.values.forEach {
            it.closed = true
            it.bossBar?.let { bar -> Bukkit.getServer().hideBossBar(bar) }
            it.guiCancel?.invoke()
            it.bossBarCancel?.invoke()
            it.moveCancel?.invoke()
            it.actionBarCancel?.invoke()
        }
        states.keys.forEach { Bukkit.getPlayer(it)?.closeInventory() }
        states.clear()
    }

    private fun handleSuccess(context: UnlockContext, state: State) {
        val player = context.player
        states.remove(player.uniqueId)
        UnlockStateManager.removeActive(player)
        context.uiConfig.agents?.runAgent("onSuccess", linkedMapOf("context" to context), player)
        UnlockFlowCompleteEvent(player, context).call()

        if (context.lightConfig.onCompleteAction == "open") {
            submit { context.onSuccess(context.lootInstance) }
        }
        if (context.lightConfig.shared) notifySharedSuccess(context, player)
    }
}
