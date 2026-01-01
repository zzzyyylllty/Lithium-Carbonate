package io.github.zzzyyylllty.lithiumcarbon.function.player

import io.github.zzzyyylllty.lithiumcarbon.util.toComponent
import org.bukkit.command.CommandSender

fun CommandSender.sendComponent(message: String) {
    sendMessage(message.toComponent())
}