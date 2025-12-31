package io.github.zzzyyylllty.lithiumcarbon.command

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootMap
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.playerDataMap
import io.github.zzzyyylllty.lithiumcarbon.function.player.sendComponent
import org.bukkit.command.CommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand

@CommandHeader(
    name = "lithiumcarbon-debug",
    aliases = ["li2co3debug", "lcdebug"],
    permission = "lithiumcarbon.command.debug",
    description = "Debug Command for LithiumCarbon.",
    permissionMessage = "",
    permissionDefault = PermissionDefault.OP,
    newParser = false,
)
object LithiumCarbonDebugCommand {

    @CommandBody
    val main = mainCommand {
        createModernHelper()
    }

    @CommandBody
    val getDataMap = subCommand {
        execute<CommandSender> { sender, context, argument ->
            sender.sendComponent(playerDataMap.entries.toString())
        }
    }

    @CommandBody
    val getAllLoots = subCommand {
        execute<CommandSender> { sender, context, argument ->
            sender.sendComponent(lootMap.entries.toString())
        }
    }


}
