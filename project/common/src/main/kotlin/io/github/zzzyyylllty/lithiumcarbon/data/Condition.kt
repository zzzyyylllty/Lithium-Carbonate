package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.sertraline.function.kether.evalKether
import io.github.zzzyyylllty.sertraline.util.toBooleanTolerance
import org.bukkit.entity.Player
import javax.script.CompiledScript
import javax.script.SimpleBindings

data class Condition(
    val js: CompiledScript? = null,
    val kether: List<String>? = null,
    val mode: ConditionMode = ConditionMode.ALL
){
    fun validate(extraVariables: Map<String, Any>, player: Player): Boolean {
        val data = defaultData + extraVariables + mapOf("player" to player, "mode" to mode.name)
        val jsEnd = js?.eval(SimpleBindings(data))?.toBooleanTolerance()
        val keEnd = kether?.evalKether(player, data)?.toBooleanTolerance()
        return if (mode == ConditionMode.ALL) {
            if (jsEnd == null && keEnd == null) true
            else if (jsEnd == null || keEnd == null) jsEnd ?: true && keEnd ?: true
            else jsEnd && keEnd
        } else {
            if (jsEnd == null && keEnd == null) true
            else if (jsEnd == null || keEnd == null) jsEnd ?: false || keEnd ?: false
            else jsEnd || keEnd
        }
    }
}

enum class ConditionMode{
    ALL,
    ANY
}