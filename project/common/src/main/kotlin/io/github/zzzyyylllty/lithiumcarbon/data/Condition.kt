package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.function.kether.evalKether
import io.github.zzzyyylllty.lithiumcarbon.util.PapiHelper
import io.github.zzzyyylllty.lithiumcarbon.util.toBooleanTolerance
import org.bukkit.entity.Player
import javax.script.CompiledScript
import javax.script.SimpleBindings

data class Condition(
    val js: CompiledScript? = null,
    val kether: List<String>? = null,
    val papi: List<String>? = null,
    val mode: ConditionMode = ConditionMode.ALL
){
    fun validate(extraVariables: Map<String, Any?>, player: Player): Boolean {

        val data = defaultData + extraVariables + mapOf("player" to player, "mode" to mode.name)
        val jsEnd = js?.eval(SimpleBindings(data))?.toBooleanTolerance()
        val keEnd = kether?.evalKether(player, data)?.toBooleanTolerance()
        val papiEnd = papi?.all { PapiHelper.checkCondition(it, player) }

        val ends = listOfNotNull(jsEnd, keEnd, papiEnd)
        return if (mode == ConditionMode.ALL) {
            ends.isEmpty() || ends.all { it }
        } else {
            ends.isEmpty() || ends.any { it }
        }

    }
}

enum class ConditionMode{
    ALL,
    ANY
}