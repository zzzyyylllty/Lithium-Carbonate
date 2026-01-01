package io.github.zzzyyylllty.lithiumcarbon.util

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.consoleSender
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.devMode
import io.github.zzzyyylllty.lithiumcarbon.logger.debugS
import org.bukkit.Bukkit

fun devLog(input: String) {
    if (devMode) consoleSender.debugS(input)
}

fun devMode(b: Boolean) {
    devMode = b
}