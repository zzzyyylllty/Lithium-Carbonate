package io.github.zzzyyylllty.lithiumcarbon.command

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootCaches
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootDefines
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootMap
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootTemplates
import io.github.zzzyyylllty.lithiumcarbon.data.LocationHelper
import io.github.zzzyyylllty.lithiumcarbon.function.player.sendComponent
import io.github.zzzyyylllty.lithiumcarbon.logger.infoS
import io.github.zzzyyylllty.lithiumcarbon.logger.severeS
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.location
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.submitAsync
import taboolib.platform.util.asLangText
import taboolib.platform.util.toBukkitLocation

@CommandHeader(
    name = "lithiumcarbon-debug",
    aliases = ["li2co3debug", "lcdebug"],
    permission = "lithiumcarbon.command.debug",
    description = "Debug Command for LithiumCarbon.",
    permissionMessage = "",
    permissionDefault = PermissionDefault.OP,
    newParser = false,
)
object LithiumCarbonManageCommand {

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
    val update = subCommand {
        location("location") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val location = LocationHelper.toLootLocation(context.location("location").toBukkitLocation())
                    val instance = lootMap[location]
                    if (instance != null) {
                        instance.update()
                    } else {
                        sender.severeS(sender.asLangText("LootInstanceNotFound"))
                    }
                }
            }
        }
    }
    @CommandBody
    val updateWithoutCheck = subCommand {
        location("location") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val location = LocationHelper.toLootLocation(context.location("location").toBukkitLocation())
                    val instance = lootMap[location]
                    instance?.update()
                }
            }
        }
    }
    @CommandBody
    val updateAll = subCommand {
        dynamic("template") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val template = context["location"]
                    lootMap.values
                        .filter { it.templateID == template }
                        .forEach {
                            it.update()
                        }
                }
            }
        }
    }

}
