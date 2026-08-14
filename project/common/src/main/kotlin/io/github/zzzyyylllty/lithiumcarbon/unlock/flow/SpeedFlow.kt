package io.github.zzzyyylllty.lithiumcarbon.unlock.flow

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootItems
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowCancelEvent
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowCompleteEvent
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowFailEvent
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowProgressEvent
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowStartEvent
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockContext
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockFlow
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockStateManager
import io.github.zzzyyylllty.lithiumcarbon.util.SoundUtil.playConfiguredSound
import io.github.zzzyyylllty.lithiumcarbon.util.mmJsonUtil
import io.github.zzzyyylllty.lithiumcarbon.util.mmUtil
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import taboolib.expansion.submitChain
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

object SpeedFlow : UnlockFlow {
    override val typeId = "speed"

    private data class State(
        var targetsCleared: Int = 0,
        val totalTargets: Int = 10,
        var elapsedSeconds: Int = 0,
        val spawnedTargets: MutableSet<Int> = ConcurrentHashMap.newKeySet(),
        val spawnedDistractors: MutableSet<Int> = ConcurrentHashMap.newKeySet(),
        var closed: Boolean = false,
        var completed: Boolean = false,
        var spawnCancel: (() -> Unit)? = null,
        var timerCancel: (() -> Unit)? = null,
    )

    private val states = ConcurrentHashMap<UUID, State>()

    override fun start(context: UnlockContext): Boolean {
        val player = context.player
        val config = context.uiConfig.speed ?: return false

        val totalTargets = (context.lightConfig.overrides["totalTargets"]?.toString()?.toIntOrNull()
            ?: context.lightConfig.overrides["total-targets"]?.toString()?.toIntOrNull())
            ?: config.totalTargets

        val state = State(totalTargets = totalTargets)
        states[player.uniqueId] = state

        UnlockStateManager.setActive(player, context, this)
        context.uiConfig.agents?.runAgent("onStart", linkedMapOf("context" to context, "totalTargets" to totalTargets), player)
        UnlockFlowStartEvent(player, context).call()

        openSpeedGUI(player, context, config, state)
        return true
    }

    private fun openSpeedGUI(
        player: Player,
        context: UnlockContext,
        config: io.github.zzzyyylllty.lithiumcarbon.unlock.SpeedConfig,
        state: State,
    ) {
        val targetItem = config.targetItem
        val distractorItem = config.distractorItem
        val timeLimit = config.timeLimit
        val spawnInterval = config.spawnInterval.toLong()

        val playSlots = context.uiConfig.layout
            .flatMapIndexed { row, line ->
                line.mapIndexedNotNull { col, ch ->
                    if (ch == ' ') row * 9 + col else null
                }
            }
        if (playSlots.isEmpty()) return

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

            onBuild(async = true) { _, inventory ->

            fun spawnTarget() {
                val available = playSlots.filter { it !in state.spawnedTargets && it !in state.spawnedDistractors && inventory.getItem(it) == null }
                if (available.isEmpty()) return
                val slot = available[ThreadLocalRandom.current().nextInt(available.size)]
                state.spawnedTargets.add(slot)
                inventory.setItem(slot, targetItem.build(player))
            }

            fun spawnDistractor() {
                if (distractorItem == null) return
                val available = playSlots.filter { it !in state.spawnedTargets && it !in state.spawnedDistractors && inventory.getItem(it) == null }
                if (available.isEmpty()) return
                val slot = available[ThreadLocalRandom.current().nextInt(available.size)]
                state.spawnedDistractors.add(slot)
                inventory.setItem(slot, distractorItem.build(player))
            }

            state.spawnCancel?.let { it() }
            val spawnTask = submitAsync(period = spawnInterval) {
                if (state.closed || state.completed) { cancel(); return@submitAsync }
                submitChain { sync { spawnTarget() } }
                if (distractorItem != null && ThreadLocalRandom.current().nextDouble() < 0.3) {
                    submitChain { sync { spawnDistractor() } }
                }
            }
            state.spawnCancel = { spawnTask.cancel() }

            if (timeLimit != null && timeLimit > 0) {
                val timerTask = submitAsync(period = 20) {
                    if (state.closed || state.completed) { cancel(); return@submitAsync }
                    state.elapsedSeconds++
                    if (state.elapsedSeconds >= timeLimit) {
                        cancel()
                        state.spawnCancel?.invoke()
                        state.closed = true
                        state.completed = false
                        submitChain {
                            sync {
                                player.sendMessage("§c时间到！速度练习失败。")
                                player.closeInventory()
                            }
                        }
                        handleFail(context, state, "timeout")
                    }
                }
                state.timerCancel = { timerTask.cancel() }
            }

            onClick { event ->
                event.clickEvent().isCancelled = true
                if (state.closed || state.completed) return@onClick
                val slot = event.rawSlot
                val item = inventory.getItem(slot) ?: return@onClick

                // Distractor hit
                if (slot in state.spawnedDistractors) {
                    state.spawnedDistractors.remove(slot)
                    config.soundDistractor?.let { playConfiguredSound(player, it) }
                    inventory.setItem(slot, null)

                    when (config.penaltyAction.lowercase()) {
                        "reset" -> {
                            state.spawnedTargets.forEach { inventory.setItem(it, null) }
                            state.spawnedDistractors.forEach { inventory.setItem(it, null) }
                            state.spawnedTargets.clear()
                            state.spawnedDistractors.clear()
                            state.targetsCleared = 0
                            player.sendMessage("§c点击了干扰项！已重置进度。")
                        }
                        "fail" -> {
                            state.closed = true
                            state.completed = false
                            player.sendMessage("§c点击了干扰项！解锁失败。")
                            player.closeInventory()
                            handleFail(context, state, "distractor")
                        }
                        "time_penalty" -> {
                            state.elapsedSeconds += config.timePenaltySeconds
                            player.sendMessage("§c点击了干扰项！时间惩罚 +${config.timePenaltySeconds}秒")
                        }
                    }
                    return@onClick
                }

                // Target hit
                if (slot !in state.spawnedTargets) return@onClick
                state.spawnedTargets.remove(slot)
                state.targetsCleared++
                config.soundTarget?.let { playConfiguredSound(player, it) }
                inventory.setItem(slot, null)

                context.uiConfig.agents?.runAgent("onProgress", linkedMapOf("context" to context, "progress" to state.targetsCleared, "total" to state.totalTargets), player)
                UnlockFlowProgressEvent(player, context, state.targetsCleared, state.totalTargets).call()

                if (state.targetsCleared >= state.totalTargets) {
                    state.completed = true
                    state.spawnCancel?.invoke()
                    state.timerCancel?.invoke()
                    config.soundComplete?.let { playConfiguredSound(player, it) }
                    player.closeInventory()
                    handleSuccess(context, state)
                }
            }
            }

            onClose {
                state.closed = true
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

    override fun cancel(player: Player) {
        val state = states.remove(player.uniqueId) ?: return
        state.closed = true
        state.spawnCancel?.invoke()
        state.timerCancel?.invoke()
        player.closeInventory()
    }

    override fun isActive(player: Player): Boolean = states.containsKey(player.uniqueId)

    override fun cleanup() {
        states.values.forEach {
            it.closed = true
            it.spawnCancel?.invoke()
            it.timerCancel?.invoke()
        }
        states.keys.forEach { org.bukkit.Bukkit.getPlayer(it)?.closeInventory() }
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

    private fun handleFail(context: UnlockContext, state: State, reason: String) {
        val player = context.player
        states.remove(player.uniqueId)
        UnlockStateManager.removeActive(player)
        context.uiConfig.agents?.runAgent("onFail", linkedMapOf("context" to context, "reason" to reason), player)
        UnlockFlowFailEvent(player, context, reason).call()
        context.onFail(reason)
    }
}
