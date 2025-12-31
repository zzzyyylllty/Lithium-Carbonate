package io.github.zzzyyylllty.lithiumcarbon.command

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootCaches
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootDefines
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootMap
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootTemplates
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

//    @CommandBody
//    val getDataMap = subCommand {
//        execute<CommandSender> { sender, context, argument ->
//            sender.sendComponent(playerDataMap.entries.toString())
//        }
//    }

    @CommandBody
    val getAllLoots = subCommand {
        execute<CommandSender> { sender, context, argument ->
            sender.sendComponent(lootMap.entries.toString())
        }
    }

    @CommandBody
    val getDefines = subCommand {
        execute<CommandSender> { sender, context, argument ->
            sender.sendComponent(lootDefines.entries.toString())
        }
    }

    @CommandBody
    val getCaches = subCommand {
        execute<CommandSender> { sender, context, argument ->
            sender.sendComponent(lootCaches.entries.toString())
        }
    }

    @CommandBody
    val getTemplates = subCommand {
        execute<CommandSender> { sender, context, argument ->
            sender.sendComponent(lootTemplates.entries.toString())
        }
    }


}
