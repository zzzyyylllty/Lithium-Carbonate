package io.github.zzzyyylllty.lithiumcarbon.function.player

import io.github.zzzyyylllty.lithiumcarbon.util.minimessage.toComponent
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

fun CommandSender.sendComponent(message: String) {
    sendMessage(message.toComponent())
}