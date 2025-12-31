package io.github.zzzyyylllty.lithiumcarbon.util

import com.google.common.base.Strings
import org.bukkit.Color
import taboolib.library.xseries.XItemStack
import java.util.*
import kotlin.collections.forEach
import kotlin.collections.set
import kotlin.text.lowercase
import kotlin.text.replace
import kotlin.text.split
import kotlin.text.startsWith
import kotlin.text.substring
import kotlin.text.toBooleanStrictOrNull
import kotlin.text.toInt
import kotlin.toString

fun Any.toBooleanTolerance(): Boolean {
    return when (this) {
        is Boolean -> this
        is Int -> this > 0
        is String -> this.lowercase() == "true" || this == "1"
        is Double -> this > 0.0
        is Float -> this > 0.0
        is Byte -> (this == 1.toByte())
        is Short -> this > 0
        is Long -> this > 0
        else -> this.toString().toBooleanStrictOrNull() ?: false
    }
}