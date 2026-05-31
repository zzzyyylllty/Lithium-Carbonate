package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootMap
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.reloadTimes
import io.github.zzzyyylllty.lithiumcarbon.event.LootInstanceCreateEvent
import io.github.zzzyyylllty.lithiumcarbon.util.asNumberFormat
import io.github.zzzyyylllty.lithiumcarbon.util.asNumberFormatNullable
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import kotlin.math.roundToLong


data class LootTemplate (
    val id: String,
    val name: String,
    val title: String,
    val rows: Int,
    val layout: List<String>,
    val availableSlots: Set<Int>,
    val lootTable: LootTable,
    val agents: Agents?,
    val options: LootTemplateOptions,
    val update: LootUpdate,
) {
    fun createInstance(block: Block, player: Player, bypassCondition: Boolean = false): LootInstance {
        val i = LootInstance(
            templateID = id,
            loc = LocationHelper.toLootLocation(block.location),
            elements = generateElements(player, bypassCondition),
            searches = linkedMapOf(),
            nextRefresh = update.expire?.asNumberFormatNullable(player)?.let { System.currentTimeMillis() + (it * 1000).roundToLong() },
            isPrivate = options.private,
            playerId = if (options.private) player.uniqueId else null,
        )
        val event = LootInstanceCreateEvent(player, i)
        event.call()
        return event.instance
    }
    fun generateElements(player: Player, bypassCondition: Boolean = false): LinkedHashMap<Int, LootElement?> {
        return lootTable.apply(bypassCondition, getExtraVariables(player), player, availableSlots, shuffleLoot = options.shuffleLoot)
    }
    fun getExtraVariables(player: Player): Map<String, Any?> {
        return linkedMapOf<String, Any?>(
            "template" to this,
            "id" to id,
            "name" to name,
            "title" to title,
            "player" to player,
        )
    }
}

data class LootTemplateOptions(
    val removeLore: Boolean,
    val addLore: List<String>?,
    val shuffleLoot: Boolean,
    val searchLimit: String?,
    val private: Boolean = false,
)

data class LootUpdate(
    val loops: List<LootUpdateLoop>?,
    val expire: String?,
) {
    fun runUpdate(template : LootTemplate) {
        val currentLoop = reloadTimes
        loops?.forEach { loop ->
            val id = template.id
            devLog("Starting loop $id")
            submitAsync(period = (loop.period*20).roundToLong()) {
                devLog("Acting loop $id")
                if (currentLoop != reloadTimes) {
                    loop.agents?.runAgent("onCancel", linkedMapOf("loop" to loop, "timestart" to currentLoop, "template" to template, "name" to template.name), null)
                    cancel()
                }
                loop.agents?.runAgent("onRefresh", linkedMapOf("loop" to loop, "timestart" to currentLoop, "name" to template.name), null)
                lootMap.forEach {
                    val loc = it.value.loc.toBukkitLocation()
                    val block = loc.world.getBlockAt(loc)
                    loop.agents?.runAgent("onRefreshEach", linkedMapOf("loop" to loop, "timestart" to currentLoop, "name" to template.name, "loc" to loc, "block" to block), null)
                    if (it.value.templateID == id) {
                        it.value.update()
                    }
                }
            }
        }
    }
}
data class LootUpdateLoop(
    val period: Double,
    val agents: Agents?
)