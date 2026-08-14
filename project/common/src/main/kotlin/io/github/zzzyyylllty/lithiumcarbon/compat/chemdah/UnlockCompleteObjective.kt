package io.github.zzzyyylllty.lithiumcarbon.compat.chemdah

import ink.ptms.chemdah.core.quest.QuestLoader.register
import ink.ptms.chemdah.core.quest.objective.ObjectiveCountableI
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowCompleteEvent
import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

object UnlockCompleteObjective : ObjectiveCountableI<UnlockFlowCompleteEvent>() {

    override val name = "lithium unlock complete"
    override val event = UnlockFlowCompleteEvent::class.java

    init {
        handler { it.player }

        addSimpleCondition("id") { data, e ->
            data.asList().any { it.equals(e.context.uiConfig.id, true) }
        }

        addSimpleCondition("type") { data, e ->
            data.asList().any { it.equals(e.context.uiConfig.type, true) }
        }

        addSimpleCondition("template") { data, e ->
            data.asList().any { it.equals(e.context.lootTemplate.id, true) }
        }
    }
}

@Awake(LifeCycle.ENABLE)
fun registerUnlockCompleteObjective() {
    if (Bukkit.getPluginManager().isPluginEnabled("Chemdah")) UnlockCompleteObjective.register()
}
