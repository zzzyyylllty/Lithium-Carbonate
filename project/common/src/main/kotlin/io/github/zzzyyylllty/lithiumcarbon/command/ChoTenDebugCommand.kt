package io.github.zzzyyylllty.lithiumcarbon.command

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.levelsList
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.playerDataMap
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.sdClassList
import io.github.zzzyyylllty.lithiumcarbon.data.IDData
import io.github.zzzyyylllty.lithiumcarbon.data.PlayerDataManager
import io.github.zzzyyylllty.lithiumcarbon.data.emptyData
import io.github.zzzyyylllty.lithiumcarbon.data.generateID
import io.github.zzzyyylllty.lithiumcarbon.function.player.sendComponent
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.submitAsync

@CommandHeader(
    name = "lithiumcarbon-debug",
    aliases = ["cttdebug"],
    permission = "lithiumcarbon.command.debug",
    description = "Debug Command for LithiumCarbon.",
    permissionMessage = "",
    permissionDefault = PermissionDefault.OP,
    newParser = false,
)
object ChoTenDebugCommand {

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
    val getLevelMap = subCommand {
        execute<CommandSender> { sender, context, argument ->
            sender.sendComponent(levelsList.entries.toString())
        }
    }

    @CommandBody
    val getSDClassMap = subCommand {
        execute<CommandSender> { sender, context, argument ->
            sender.sendComponent(sdClassList.entries.toString())
        }
    }
    @CommandBody
    val getIdExists = subCommand {
        dynamic("id"){
            execute<CommandSender> { sender, context, argument ->
                sender.sendComponent("${PlayerDataManager.isIDExist(context["id"].toLong())}")
            }
        }
    }
    @CommandBody
    val getDataById = subCommand {
        dynamic("id"){
            execute<CommandSender> { sender, context, argument ->
                sender.sendComponent("${PlayerDataManager.getDataByID(context["id"].toLong())}")
            }
        }
    }
    @CommandBody
    val getDataByPlayer = subCommand {
        player("user") {
            execute<CommandSender> { sender, context, argument ->
                val user = context.player("user")
                // 转化为Bukkit的Player
                val bukkitPlayer = user.castSafely<Player>()
                sender.sendComponent("${PlayerDataManager.getDataByUUID(bukkitPlayer?.uniqueId.toString())}")
            }
        }
    }
    @CommandBody
    val changePlayerId = subCommand {
        player("user") {
            dynamic("id"){
                execute<CommandSender> { sender, context, argument ->
                    val user = context.player("user")
                    // 转化为Bukkit的Player
                    val bukkitPlayer = user.castSafely<Player>()
                    if (PlayerDataManager.isIDExist(context["id"].toLong()))
                        sender.sendComponent("userId must be unique")
                    else {
                        val data = bukkitPlayer?.let { PlayerDataManager.getData(it) }
                        bukkitPlayer?.let {
                            val id = context["id"].toLong()
                            if (data == null) sender.sendComponent("<red>未找到玩家数据")
                            data?.let { playerData -> PlayerDataManager.modifyUserID(id, playerData) }
                        }
                    }
                }
                dynamic("type"){
                    execute<CommandSender> { sender, context, argument ->
                        val user = context.player("user")
                        // 转化为Bukkit的Player
                        val bukkitPlayer = user.castSafely<Player>()
                        if (PlayerDataManager.isIDExist(context["id"].toLong()))
                            sender.sendComponent("userId must be unique")
                        else {
                            val data = bukkitPlayer?.let { PlayerDataManager.getData(it) }
                            bukkitPlayer?.let {
                                val id = context["id"].toLong()
                                val type = context["type"]
                                if (data == null) sender.sendComponent("<red>未找到玩家数据")
                                data?.let { playerData -> PlayerDataManager.modifyUserIDData(IDData(id, type), playerData) }
                            }
                        }
                    }
                }
            }
        }
    }
    @CommandBody
    val randomGenerateID = subCommand {
        execute<CommandSender> { sender, context, argument ->
            submitAsync {
                repeat(10) {
                    sender.sendComponent(generateID().toString())
                }
            }
        }
    }

}
