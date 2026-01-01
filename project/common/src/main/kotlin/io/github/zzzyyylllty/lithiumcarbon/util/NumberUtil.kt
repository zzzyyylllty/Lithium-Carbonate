package io.github.zzzyyylllty.lithiumcarbon.util

import io.github.zzzyyylllty.lithiumcarbon.function.kether.parseKether
import org.bukkit.entity.Player
import taboolib.common.util.random
import kotlin.math.roundToInt

fun String?.asNumberFormat(player: Player?): Double {
    val oAmount = this ?: "1"
    val full = if (oAmount.contains("{")) oAmount.parseKether(player) else oAmount
    val split = full.split("~")
    return if (split.size >= 2) random(split.first().toDoubleOrNull() ?: 1.0, split.last().toDoubleOrNull() ?: 1.0) else full.toDoubleOrNull() ?: 1.0
}
fun String?.asNumberFormatNullable(player: Player?): Double? {
    val oAmount = this ?: "1"
    val full = if (oAmount.contains("{")) oAmount.parseKether(player) else oAmount
    val split = full.split("~")
    return if (split.size >= 2) random(split.first().toDoubleOrNull() ?: 1.0, split.last().toDoubleOrNull() ?: 1.0) else full.toDoubleOrNull()
}