package io.github.zzzyyylllty.lithiumcarbon.util

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.entity.Player
import kotlin.text.indexOf
import kotlin.text.substring
import kotlin.text.toDoubleOrNull
import kotlin.text.toRegex
import kotlin.text.trim

object PapiHelper {

    fun parse(player: Player, text: String): String {
        if (!DependencyHelper.papi) return text
        return PlaceholderAPI.setPlaceholders(player, text)
    }

    /**
     * Evaluates a single papi condition: %placeholder% <operator> value
     * Operators: == != >= <= > < =~(regex). A bare placeholder is evaluated as boolean
     * (non-empty and not false/0 after parsing).
     */
    fun checkCondition(raw: String, player: Player): Boolean {
        val operators = listOf("==", "!=", ">=", "<=", ">", "<", "=~")
        val op = operators.firstOrNull { raw.contains(it) }
        if (op == null) {
            val result = parse(player, raw).trim()
            return result.isNotEmpty() && result.toBooleanTolerance()
        }
        val index = raw.indexOf(op)
        val left = parse(player, raw.substring(0, index)).trim()
        val right = parse(player, raw.substring(index + op.length)).trim()
        return when (op) {
            "==" -> left == right
            "!=" -> left != right
            "=~" -> runCatching { right.toRegex().containsMatchIn(left) }.getOrDefault(false)
            else -> {
                val l = left.toDoubleOrNull()
                val r = right.toDoubleOrNull()
                if (l != null && r != null) {
                    when (op) {
                        ">=" -> l >= r
                        "<=" -> l <= r
                        ">" -> l > r
                        else -> l < r
                    }
                } else {
                    when (op) {
                        ">=" -> left >= right
                        "<=" -> left <= right
                        ">" -> left > right
                        else -> left < right
                    }
                }
            }
        }
    }
}
