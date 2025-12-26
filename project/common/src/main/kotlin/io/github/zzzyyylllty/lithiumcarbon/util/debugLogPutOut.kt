package io.github.zzzyyylllty.lithiumcarbon.util

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.consoleSender
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.devMode
import io.github.zzzyyylllty.lithiumcarbon.util.minimessage.toComponent

fun devLog(input: String) {
    if (devMode) consoleSender.sendMessage(input.toComponent())
}

fun devMode(b: Boolean) {
    devMode = b
}