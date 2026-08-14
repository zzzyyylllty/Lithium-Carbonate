package io.github.zzzyyylllty.lithiumcarbon.unlock.flow

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootItems
import io.github.zzzyyylllty.lithiumcarbon.data.defaultData
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowCancelEvent
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowCompleteEvent
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowProgressEvent
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowStartEvent
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockContext
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockFlow
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockStateManager
import io.github.zzzyyylllty.lithiumcarbon.util.SoundUtil.playConfiguredSound
import io.github.zzzyyylllty.lithiumcarbon.util.mmJsonUtil
import io.github.zzzyyylllty.lithiumcarbon.util.mmUtil
import org.bukkit.entity.Player
import taboolib.common5.compileJS
import javax.script.SimpleBindings
import org.bukkit.inventory.Inventory
import taboolib.common.platform.function.submit
import taboolib.expansion.submitChain
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object BruteForceFlow : UnlockFlow {
    override val typeId = "brute_force"

    private data class State(
        var clicksDone: Int = 0,
        var clicksRequired: Int = 5,
        var closed: Boolean = false,
        var completed: Boolean = false,
    )

    private val states = ConcurrentHashMap<UUID, State>()

    override fun start(context: UnlockContext): Boolean {
        val player = context.player
        val config = context.uiConfig.bruteForce ?: return false

        val clicksRaw = context.lightConfig.overrides["clicks"]?.toString() ?: config.clicks
        val clicksRequired = if (clicksRaw.startsWith("js:")) {
            val script = clicksRaw.removePrefix("js:").compileJS()
            try {
                script?.eval(SimpleBindings(defaultData + mapOf("player" to player, "context" to context)))?.toString()?.toIntOrNull() ?: 5
            } catch (_: Exception) { 5 }
        } else clicksRaw.toIntOrNull() ?: 5

        val state = State(clicksRequired = clicksRequired)
        states[player.uniqueId] = state

        UnlockStateManager.setActive(player, context, this)
        context.uiConfig.agents?.runAgent("onStart", linkedMapOf("context" to context, "total" to clicksRequired), player)
        UnlockFlowStartEvent(player, context).call()

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

            val bruteSlots = context.uiConfig.layout
                .flatMapIndexed { row, line ->
                    line.mapIndexedNotNull { col, ch -> if (ch == ' ') row * 9 + col else null }
                }.toSet()

            onClick(lock = true) { event ->
                if (state.completed || state.closed) return@onClick
                val slot = event.rawSlot
                if (slot !in bruteSlots) return@onClick

                submitChain {
                    sync {
                        val inv = event.inventory
                        state.clicksDone++
                        val total = state.clicksRequired

                        config.soundProgress?.let { playConfiguredSound(player, it) }
                        context.uiConfig.agents?.runAgent("onProgress", linkedMapOf("context" to context, "progress" to state.clicksDone, "total" to total), player)
                        UnlockFlowProgressEvent(player, context, state.clicksDone, total).call()

                        bruteSlots.forEach { s ->
                            if (state.clicksDone >= state.clicksRequired) {
                                inv.setItem(s, completeItem.build(player))
                            } else {
                                val item = progressItem.build(player)
                                val meta = item.itemMeta
                                if (meta != null) {
                                    val currentName = meta.displayName()?.let { mmUtil.serialize(it) }
                                    val updated = currentName
                                        ?.replace("%current%", state.clicksDone.toString())
                                        ?.replace("%total%", total.toString())
                                    if (updated != null) {
                                        meta.displayName(mmUtil.deserialize(updated))
                                    }
                                    item.itemMeta = meta
                                }
                                inv.setItem(s, item)
                            }
                        }

                        if (state.clicksDone >= state.clicksRequired) {
                            state.completed = true
                            config.soundComplete?.let { playConfiguredSound(player, it) }
                            context.uiConfig.agents?.runAgent("onSuccess", linkedMapOf("context" to context), player)

                            submit(delay = 20) {
                                if (!state.closed) player.closeInventory()
                                handleSuccess(context, state)
                            }
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
        return true
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
        UnlockFlowCompleteEvent(player, context).call()

        if (context.lightConfig.onCompleteAction == "open") {
            submit { context.onSuccess(context.lootInstance) }
        }
        if (context.lightConfig.shared) notifySharedSuccess(context, player)
    }
}
