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
import org.bukkit.inventory.Inventory
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import taboolib.expansion.submitChain
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object DecipherFlow : UnlockFlow {
    override val typeId = "decipher"

    private data class State(
        var roundsDone: Int = 0,
        var roundsRequired: Int = 3,
        var currentItemIndex: Int = 0,
        var closed: Boolean = false,
        var completed: Boolean = false,
    )

    private val states = ConcurrentHashMap<UUID, State>()

    override fun start(context: UnlockContext): Boolean {
        val player = context.player
        val config = context.uiConfig.decipher ?: return false

        val rounds = (context.lightConfig.overrides["rounds"]?.toString()?.toIntOrNull())
            ?: config.rounds

        val state = State(roundsRequired = rounds)
        states[player.uniqueId] = state

        UnlockStateManager.setActive(player, context, this)
        context.uiConfig.agents?.runAgent("onStart", linkedMapOf("context" to context, "rounds" to rounds), player)
        UnlockFlowStartEvent(player, context).call()

        openDecipherGUI(player, context, config, state)
        return true
    }

    private fun openDecipherGUI(
        player: Player,
        context: UnlockContext,
        config: io.github.zzzyyylllty.lithiumcarbon.unlock.DecipherConfig,
        state: State,
    ) {
        val pool = config.items
        if (pool.isEmpty()) return

        val targetSlots = config.targetSlots ?: listOf(config.targetSlot)
        val useMultiSlots = config.targetSlots != null
        val buttonSlot = config.decipherButtonSlot
        val targetIndex = config.targetItemIndex.coerceIn(0, pool.size - 1)

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
            if (!useMultiSlots) {
                set(buttonSlot, config.decipherButtonItem.build(player))
            }

            onBuild(async = true) { _, inventory ->
                fun updateAllTargetSlots() {
                    val item = pool[state.currentItemIndex % pool.size]
                    targetSlots.forEach { slot ->
                        inventory.setItem(slot, item.build(player))
                    }
                }
                updateAllTargetSlots()

                val rotationTask = submitAsync(period = config.rotationSpeed.toLong()) {
                    if (state.closed || state.completed) { cancel(); return@submitAsync }
                    state.currentItemIndex = (state.currentItemIndex + 1) % pool.size
                    config.soundRotate?.let { playConfiguredSound(player, it) }
                    submitChain { sync { updateAllTargetSlots() } }
                }

                onClick(lock = true) { event ->
                    if (state.closed || state.completed) return@onClick
                    val slot = event.rawSlot
                    val isTargetClick = slot in targetSlots
                    val isButtonClick = !useMultiSlots && slot == buttonSlot
                    if (!isTargetClick && !isButtonClick) return@onClick

                    val currentIdx = state.currentItemIndex % pool.size
                    if (currentIdx == targetIndex) {
                        config.soundSuccess?.let { playConfiguredSound(player, it) }
                        state.roundsDone++
                        context.uiConfig.agents?.runAgent("onProgress", linkedMapOf("context" to context, "progress" to state.roundsDone, "total" to state.roundsRequired), player)
                        UnlockFlowProgressEvent(player, context, state.roundsDone, state.roundsRequired).call()

                        if (state.roundsDone >= state.roundsRequired) {
                            state.completed = true
                            rotationTask.cancel()
                            submitChain {
                                sync {
                                    targetSlots.forEach { inventory.setItem(it, config.successItem.build(player)) }
                                }
                            }
                            submit(delay = 20) {
                                if (!state.closed) player.closeInventory()
                                handleSuccess(context, state)
                            }
                        } else {
                            submitChain {
                                sync {
                                    targetSlots.forEach { inventory.setItem(it, config.successItem.build(player)) }
                                }
                            }
                            submit(delay = 10) {
                                if (!state.closed && !state.completed) {
                                    submitChain { sync { updateAllTargetSlots() } }
                                }
                            }
                        }
                    } else {
                        config.soundFail?.let { playConfiguredSound(player, it) }
                        context.uiConfig.agents?.runAgent("onFail", linkedMapOf("context" to context, "reason" to "wrong_timing"), player)
                        UnlockFlowFailEvent(player, context, "wrong_timing").call()

                        state.completed = false
                        rotationTask.cancel()
                        submitChain {
                            sync {
                                targetSlots.forEach { inventory.setItem(it, config.failItem.build(player)) }
                            }
                        }
                        submit(delay = 20) {
                            handleFail(context, state)
                            if (!state.closed) player.closeInventory()
                        }
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
        player.closeInventory()
    }

    override fun isActive(player: Player): Boolean = states.containsKey(player.uniqueId)

    override fun cleanup() {
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

    private fun handleFail(context: UnlockContext, state: State) {
        val player = context.player
        states.remove(player.uniqueId)
        UnlockStateManager.removeActive(player)
        context.onFail("wrong_timing")
    }
}
