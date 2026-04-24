package io.github.zzzyyylllty.lithiumcarbon.compat.chemdah

import ink.ptms.chemdah.core.PlayerProfile
import ink.ptms.chemdah.core.quest.QuestLoader.register
import ink.ptms.chemdah.core.quest.Task
import ink.ptms.chemdah.core.quest.objective.ObjectiveCountableI
import ink.ptms.chemdah.core.quest.selector.InferItem.Companion.toInferItem
import io.github.zzzyyylllty.lithiumcarbon.event.CardRoomOpenEvent
import io.github.zzzyyylllty.lithiumcarbon.event.LootInstanceCreateEvent
import io.github.zzzyyylllty.lithiumcarbon.event.LootItemGrantEvent
import io.github.zzzyyylllty.lithiumcarbon.function.player.sendComponent
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import io.github.zzzyyylllty.sertraline.function.kether.evalKetherBoolean
import io.github.zzzyyylllty.sertraline.function.kether.evalKetherValue
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.platform.util.hasItem
import taboolib.platform.util.isNotAir
import taboolib.platform.util.takeItem
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.component1
import kotlin.collections.component2




object LootCreateObjective: ObjectiveCountableI<LootInstanceCreateEvent>() {

    override val name = "lithium loot create"
    override val event = LootInstanceCreateEvent::class.java

    init {
        handler { it.player }

        addSimpleCondition("id") { data, e ->
            data.toString() == e.instance.templateID
        }

        addSimpleCondition("location") { data, e ->
            data.toPosition().inside(e.instance.loc.toBukkitLocation())
        }

    }

}

@Awake(LifeCycle.ENABLE)
fun registerLootCreateObjective(){
    if (Bukkit.getPluginManager().isPluginEnabled("Chemdah")) LootCreateObjective.register()
}

