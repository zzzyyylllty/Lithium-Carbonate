package io.github.zzzyyylllty.lithiumcarbon.unlock.flow

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootItems
import io.github.zzzyyylllty.lithiumcarbon.data.defaultData
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowCancelEvent
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowCompleteEvent
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowFailEvent
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowProgressEvent
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowStartEvent
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockContext
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockFlow
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockStateManager
import io.github.zzzyyylllty.lithiumcarbon.util.SoundUtil.playConfiguredSound
import io.github.zzzyyylllty.lithiumcarbon.util.mmUtil
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import taboolib.common5.compileJS
import taboolib.expansion.submitChain
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Anvil
import taboolib.module.ui.type.Chest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.script.SimpleBindings

object PasswordFlow : UnlockFlow {
    override val typeId = "password"

    private data class State(
        var attemptsLeft: Int = 3,
        var closed: Boolean = false,
        var completed: Boolean = false,
        var retrying: Boolean = false,
    )

    private val states = ConcurrentHashMap<UUID, State>()

    override fun start(context: UnlockContext): Boolean {
        val player = context.player
        val config = context.uiConfig.password ?: return false

        val passwordRaw = context.lightConfig.overrides["password"]?.toString() ?: config.password
        val maxAttempts = (context.lightConfig.overrides["maxAttempts"]?.toString()?.toIntOrNull()
            ?: context.lightConfig.overrides["max-attempts"]?.toString()?.toIntOrNull())
            ?: config.maxAttempts

        val expectedPassword = if (passwordRaw.startsWith("js:")) {
            val script = passwordRaw.removePrefix("js:").compileJS()
            val data = defaultData + mapOf("player" to player, "context" to context)
            try {
                script?.eval(SimpleBindings(data))?.toString() ?: passwordRaw
            } catch (e: Exception) {
                passwordRaw
            }
        } else passwordRaw

        val state = State(attemptsLeft = maxAttempts)
        states[player.uniqueId] = state

        UnlockStateManager.setActive(player, context, this)
        context.uiConfig.agents?.runAgent("onStart", linkedMapOf("context" to context, "maxAttempts" to maxAttempts), player)
        UnlockFlowStartEvent(player, context).call()

        openPasswordAnvil(player, context, config, state, expectedPassword, maxAttempts)
        return true
    }

    private fun openPasswordAnvil(
        player: Player,
        context: UnlockContext,
        config: io.github.zzzyyylllty.lithiumcarbon.unlock.PasswordConfig,
        state: State,
        expectedPassword: String,
        maxAttempts: Int,
    ) {
        player.openMenu<Anvil>(context.uiConfig.title) {
            set(0, config.inputItem.build(player))

            onRename { _, text, inv ->
                if (state.closed || state.completed) return@onRename

                if (text == expectedPassword) {
                    state.completed = true
                    config.soundCorrect?.let { playConfiguredSound(player, it) }
                    context.uiConfig.agents?.runAgent("onSuccess", linkedMapOf("context" to context), player)
                    player.sendMessage("§a密码正确！")

                    submit(delay = 20) {
                        if (!state.closed) player.closeInventory()
                        handleSuccess(context, state)
                    }
                } else {
                    state.attemptsLeft--
                    config.soundWrong?.let { playConfiguredSound(player, it) }

                    if (state.attemptsLeft <= 0) {
                        state.completed = false
                        config.soundWrong?.let { playConfiguredSound(player, it) }
                        context.uiConfig.agents?.runAgent("onFail", linkedMapOf("context" to context, "reason" to "wrong_password"), player)
                        UnlockFlowFailEvent(player, context, "wrong_password").call()
                        player.sendMessage("§c密码错误！机会已用完。")

                        submit(delay = 10) {
                            handleFail(context, state)
                            if (!state.closed) player.closeInventory()
                        }
                    } else {
                        context.uiConfig.agents?.runAgent("onProgress", linkedMapOf("context" to context, "progress" to maxAttempts - state.attemptsLeft, "total" to maxAttempts, "remainingAttempts" to state.attemptsLeft), player)
                        UnlockFlowProgressEvent(player, context, maxAttempts - state.attemptsLeft, maxAttempts).call()
                        player.sendMessage("§c密码错误！剩余机会: §e${state.attemptsLeft}")
                        // Re-open anvil for retry
                        state.retrying = true
                        player.closeInventory()
                        submit(delay = 2) {
                            if (!state.closed && !state.completed && player.isOnline)
                                openPasswordAnvil(player, context, config, state, expectedPassword, maxAttempts)
                        }
                    }
                }
            }

            onClose {
                if (state.retrying) {
                    state.retrying = false
                    return@onClose
                }
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
        context.onFail("wrong_password")
    }
}
