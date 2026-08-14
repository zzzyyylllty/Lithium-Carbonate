package io.github.zzzyyylllty.lithiumcarbon.command

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootCaches
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootDefines
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.LootInstanceKey
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootMap
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootTemplates
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.cardRoomConfigs
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.cardRoomInstances
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.frameCrateConfigs
import io.github.zzzyyylllty.lithiumcarbon.cardroom.CardRoomManager
import io.github.zzzyyylllty.lithiumcarbon.cardroom.CardRoomState
import io.github.zzzyyylllty.lithiumcarbon.frame.FrameCrateManager
import io.github.zzzyyylllty.lithiumcarbon.data.LocationHelper
import io.github.zzzyyylllty.lithiumcarbon.data.LootElement
import io.github.zzzyyylllty.lithiumcarbon.function.player.sendComponent
import io.github.zzzyyylllty.lithiumcarbon.logger.infoS
import io.github.zzzyyylllty.lithiumcarbon.logger.severeS
import io.github.zzzyyylllty.lithiumcarbon.logger.warningS
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.int
import taboolib.common.platform.command.location
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.command.component.*
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import taboolib.platform.util.asLangText
import taboolib.platform.util.toBukkitLocation

@CommandHeader(
    name = "lithiumcarbon-manage",
    aliases = ["li2co3manage", "lcmanage"],
    permission = "lithiumcarbon.command.manage",
    description = "manage Command for LithiumCarbon.",
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
    val generateItem = subCommand {
        dynamic("template") {
            suggestion<CommandSender>() { sender, context ->
                lootTemplates.keys.toList()
            }
            player("player") {
                int("count") {
                    suggestion<CommandSender>() { sender, context ->
                        listOf("1", "16", "64")
                    }
                    execute<CommandSender> { sender, context, argument ->
                        submitAsync {
                            val templateId = context["template"]
                            val template = lootTemplates[templateId]
                            if (template == null) {
                                sender.severeS(sender.asLangText("TemplateNotFound", templateId))
                                return@submitAsync
                            }
                            // 获取目标玩家
                            val targetPlayer = context.player("player").castSafely<Player>() ?: (sender as? Player)
                            if (targetPlayer == null) {
                                sender.severeS(sender.asLangText("PlayerOnlyCommand"))
                                return@submitAsync
                            }
                            // 获取数量
                            val count = context["count"].toIntOrNull() ?: 1
                            if (count <= 0) {
                                sender.severeS(sender.asLangText("CountMustBePositive"))
                                return@submitAsync
                            }
                            // 生成物品
                            val elements = mutableListOf<LootElement>()
                            while (elements.size < count) {
                                val generated = template.generateElements(targetPlayer, bypassCondition = true)
                                elements.addAll(generated.values.filterNotNull())
                                // 防止无限循环（如果模板生成零个元素）
                                if (generated.values.filterNotNull().isEmpty()) {
                                    break
                                }
                            }
                            // 限制数量
                            val limitedElements = if (elements.size > count) elements.take(count) else elements
                            // 应用给玩家
                            limitedElements.forEach { element ->
                                element.applyToPlayer(targetPlayer, template)
                            }
                            sender.infoS(sender.asLangText("GenerateItemSuccess", templateId, limitedElements.size, targetPlayer.name))
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    val update = subCommand {
        location("location") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val location = LocationHelper.toLootLocation(context.location("location").toBukkitLocation())
                    val instances = lootMap.filter { it.key.location == location }.values
                    if (instances.isNotEmpty()) {
                        instances.forEach { it.update() }
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
                    lootMap.filter { it.key.location == location }.values.forEach { it.update() }
                }
            }
        }
    }
    @CommandBody
    val updateAll = subCommand {
        dynamic("template") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val template = context["template"]
                    lootMap.values
                        .filter { it.templateID == template }
                        .forEach {
                            it.update()
                        }
                }
            }
        }
        execute<CommandSender> { sender, context, argument ->
            submitAsync {
                lootMap.values
                    .forEach {
                        it.update()
                    }
            }
        }
    }

    @CommandBody
    val spawnFrame = subCommand {
        dynamic("configId") {
            suggestion<CommandSender> { sender, context ->
                frameCrateConfigs.keys.toList()
            }
            // === With world and coordinates: /lcmanage spawnFrame <configId> <world> <x> <y> <z> [facing] ===
            dynamic("world") {
                suggestion<CommandSender> { sender, context ->
                    sender.server.worlds.map { it.name }
                }
                int("x") {
                    int("y") {
                        int("z") {
                            execute<CommandSender> { sender, context, argument ->
                                submit {
                                    val configId = context["configId"]
                                    val config = frameCrateConfigs[configId]
                                    if (config == null) {
                                        sender.severeS(sender.asLangText("TemplateNotFound", configId))
                                        return@submit
                                    }
                                    val world = sender.server.getWorld(context["world"])
                                    if (world == null) {
                                        sender.severeS("世界 ${context["world"]} 不存在")
                                        return@submit
                                    }
                                    val x = context["x"].toInt()
                                    val y = context["y"].toInt()
                                    val z = context["z"].toInt()
                                    val loc = org.bukkit.Location(world, x.toDouble(), y.toDouble(), z.toDouble())
                                    val player = sender as? Player
                                    val uuid = FrameCrateManager.spawnFrame(
                                        location = loc,
                                        configId = configId,
                                        player = player
                                    )
                                    if (uuid != null) {
                                        sender.infoS(sender.asLangText("FrameCrateSpawnSuccess", configId))
                                    } else {
                                        sender.severeS(sender.asLangText("FrameCrateSpawnFailed", configId))
                                    }
                                }
                            }
                            // With facing
                            dynamic("facing") {
                                suggestion<CommandSender> { sender, context ->
                                    listOf("NORTH", "SOUTH", "EAST", "WEST", "UP", "DOWN")
                                }
                                execute<CommandSender> { sender, context, argument ->
                                    submit {
                                        val configId = context["configId"]
                                        val config = frameCrateConfigs[configId]
                                        if (config == null) {
                                            sender.severeS(sender.asLangText("TemplateNotFound", configId))
                                            return@submit
                                        }
                                        val world = sender.server.getWorld(context["world"])
                                        if (world == null) {
                                            sender.severeS("世界 ${context["world"]} 不存在")
                                            return@submit
                                        }
                                        val x = context["x"].toInt()
                                        val y = context["y"].toInt()
                                        val z = context["z"].toInt()
                                        val loc = org.bukkit.Location(world, x.toDouble(), y.toDouble(), z.toDouble())
                                        val facing = context["facing"]
                                        val player = sender as? Player
                                        val uuid = FrameCrateManager.spawnFrame(
                                            location = loc,
                                            configId = configId,
                                            player = player,
                                            facing = facing
                                        )
                                        if (uuid != null) {
                                            sender.infoS(sender.asLangText("FrameCrateSpawnSuccess", configId))
                                        } else {
                                            sender.severeS(sender.asLangText("FrameCrateSpawnFailed", configId))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // === Without world — player's current location ===
            execute<Player> { sender, context, argument ->
                submit {
                    val configId = context["configId"]
                    val config = frameCrateConfigs[configId]
                    if (config == null) {
                        sender.severeS(sender.asLangText("TemplateNotFound", configId))
                        return@submit
                    }
                    val uuid = FrameCrateManager.spawnFrame(
                        location = sender.location,
                        configId = configId,
                        player = sender
                    )
                    if (uuid != null) {
                        sender.infoS(sender.asLangText("FrameCrateSpawnSuccess", configId))
                    } else {
                        sender.severeS(sender.asLangText("FrameCrateSpawnFailed", configId))
                    }
                }
            }
        }
    }

    @CommandBody
    val cardroom = subCommand {
        // 列出所有卡房配置
        literal("list") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    if (cardRoomConfigs.isEmpty()) {
                        sender.infoS("<gray>No card room configurations found.")
                        return@submitAsync
                    }

                    sender.sendComponent("<gradient:yellow:aqua>=== Card Room Configurations ===")
                    cardRoomConfigs.forEach { (id, config) ->
                        val instance = cardRoomInstances[id]
                        val state = instance?.state?.name ?: "IDLE"
                        val spawnedChests = instance?.spawnedChests?.size ?: 0
                        val modifiedBlocks = instance?.modifiedBlocks?.size ?: 0

                        sender.sendComponent("<hover:show_text:'<gray>ID: <white>$id\n<gray>Name: <white>${config.name}\n<gray>State: <white>$state\n<gray>Chests: <white>$spawnedChests\n<gray>Blocks: <white>$modifiedBlocks'><click:suggest_command:'/lcmanage cardroom info $id'><#66ccff>• $id</#66ccff> <gray>-</gray> <white>${config.name}</white> <gray>[</gray><#ffcc66>$state</#ffcc66><gray>]</gray></click></hover>")
                    }
                    sender.sendComponent("<gray>Total: ${cardRoomConfigs.size} card rooms")
                }
            }
        }

        // 激活卡房
        literal("activate") {
            dynamic("id") {
                suggestion<CommandSender> { sender, context ->
                    cardRoomConfigs.keys.toList()
                }
                player("player", optional = true) {
                    execute<CommandSender> { sender, context, argument ->
                        submitAsync {
                            val id = context["id"].toString()
                            val targetPlayer = context.player("player")?.castSafely<Player>()

                            val success = CardRoomManager.activateCardRoom(id, targetPlayer)
                            if (success) {
                                sender.infoS("<green>Successfully activated card room: $id")
                            } else {
                                sender.severeS("<red>Failed to activate card room: $id")
                            }
                        }
                    }
                }
            }
        }

        // 重置卡房
        literal("reset") {
            dynamic("id") {
                suggestion<CommandSender> { sender, context ->
                    cardRoomConfigs.keys.toList()
                }
                execute<CommandSender> { sender, context, argument ->
                    submitAsync {
                        val id = context["id"].toString()
                        CardRoomManager.startReset(id)
                        sender.infoS("<yellow>Started reset for card room: $id")
                    }
                }
            }
        }

        // 重置所有卡房
        literal("resetall") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    CardRoomManager.resetAllCardRooms()
                    sender.infoS("<yellow>Started reset for all card rooms")
                }
            }
        }

        // 查看卡房状态
        literal("status") {
            dynamic("id") {
                suggestion<CommandSender> { sender, context ->
                    cardRoomConfigs.keys.toList()
                }
                execute<CommandSender> { sender, context, argument ->
                    submitAsync {
                        val id = context["id"].toString()
                        val config = cardRoomConfigs[id]
                        if (config == null) {
                            sender.severeS("<red>Card room configuration not found: $id")
                            return@submitAsync
                        }

                        val instance = cardRoomInstances[id]

                        sender.sendComponent("<gradient:yellow:aqua>=== Card Room Status: $id ===")
                        sender.sendComponent("<gray>Configuration: <white>Loaded")
                        sender.sendComponent("<gray>Instance: <white>${if (instance != null) "Exists" else "Not created"}")

                        if (instance != null) {
                            sender.sendComponent("<gray>State: <white>${instance.state}")
                            sender.sendComponent("<gray>Spawned Chests: <white>${instance.spawnedChests.size}")
                            sender.sendComponent("<gray>Modified Blocks: <white>${instance.modifiedBlocks.size}")

                            when (instance.state) {
                                CardRoomState.IDLE -> sender.sendComponent("<gray>Status: <green>Ready for activation")
                                CardRoomState.ACTIVE -> {
                                    val activeTime = (System.currentTimeMillis() - instance.activatedTime) / 1000
                                    sender.sendComponent("<gray>Active for: <white>${activeTime}s")
                                    if (instance.nextResetTime != null) {
                                        val remaining = (instance.nextResetTime!! - System.currentTimeMillis()) / 1000
                                        if (remaining > 0) {
                                            sender.sendComponent("<gray>Reset in: <yellow>${remaining}s")
                                        } else {
                                            sender.sendComponent("<gray>Reset: <red>Overdue")
                                        }
                                    }
                                }
                                CardRoomState.RESETTING -> sender.sendComponent("<gray>Status: <yellow>Currently resetting")
                            }
                        }

                        // 检查区域内是否有玩家
                        val hasPlayers = CardRoomManager.hasPlayersInRange(id)
                        sender.sendComponent("<gray>Players in range: <white>${if (hasPlayers) "Yes" else "No"}")
                    }
                }
            }
        }
        // 查看卡房详细信息
        literal("info") {
            dynamic("id") {
                suggestion<CommandSender> { sender, context ->
                    cardRoomConfigs.keys.toList()
                }
                execute<CommandSender> { sender, context, argument ->
                    submitAsync {
                        val id = context["id"].toString()
                        val config = cardRoomConfigs[id]
                        if (config == null) {
                            sender.severeS("<red>Card room configuration not found: $id")
                            return@submitAsync
                        }

                        val instance = cardRoomInstances[id]

                        sender.sendComponent("<gradient:yellow:aqua>=== Card Room Info: $id ===")
                        sender.sendComponent("<gray>Name: <white>${config.name}")
                        sender.sendComponent("<gray>State: <white>${instance?.state?.name ?: "IDLE"}")
                        sender.sendComponent("<gray>Trigger Block: <white>${config.trigger.block}")
                        sender.sendComponent("<gray>Actions: <white>${config.actions.size}")
                        sender.sendComponent("<gray>Reset Delay: <white>${config.reset.delay}s")
                        sender.sendComponent("<gray>Restore Environment: <white>${config.reset.restore}")

                        if (instance != null) {
                            sender.sendComponent("<gray>Spawned Chests: <white>${instance.spawnedChests.size}")
                            sender.sendComponent("<gray>Modified Blocks: <white>${instance.modifiedBlocks.size}")
                            if (instance.state == CardRoomState.ACTIVE && instance.nextResetTime != null) {
                                val remaining = (instance.nextResetTime!! - System.currentTimeMillis()) / 1000
                                if (remaining > 0) {
                                    sender.sendComponent("<gray>Reset in: <white>${remaining}s")
                                }
                            }
                        }

                        // 显示动作列表
                        if (config.actions.isNotEmpty()) {
                            sender.sendComponent("<gray>Actions:")
                            config.actions.forEachIndexed { index, action ->
                                sender.sendComponent("<gray>  ${index + 1}. <white>${action.type} <gray>at</gray> <white>${action.location}")
                            }
                        }
                    }
                }
            }
        }

        // 主命令显示帮助
        execute<CommandSender> { sender, context, argument ->
            submitAsync {
                sender.sendComponent("<gradient:yellow:aqua>=== Card Room Management ===")
                sender.sendComponent("<click:suggest_command:'/lcmanage cardroom list'><hover:show_text:'<gray>List all card room configurations'><#66ccff>/lcmanage cardroom list</#66ccff></hover></click> <gray>- List all card rooms")
                sender.sendComponent("<click:suggest_command:'/lcmanage cardroom info '><hover:show_text:'<gray>View detailed information about a card room'><#66ccff>/lcmanage cardroom info <id></#66ccff></hover></click> <gray>- View card room info")
                sender.sendComponent("<click:suggest_command:'/lcmanage cardroom activate '><hover:show_text:'<gray>Activate a card room'><#66ccff>/lcmanage cardroom activate <id> [player]</#66ccff></hover></click> <gray>- Activate card room")
                sender.sendComponent("<click:suggest_command:'/lcmanage cardroom reset '><hover:show_text:'<gray>Reset a card room'><#66ccff>/lcmanage cardroom reset <id></#66ccff></hover></click> <gray>- Reset card room")
                sender.sendComponent("<click:suggest_command:'/lcmanage cardroom resetall'><hover:show_text:'<gray>Reset all card rooms'><#66ccff>/lcmanage cardroom resetall</#66ccff></hover></click> <gray>- Reset all card rooms")
                sender.sendComponent("<click:suggest_command:'/lcmanage cardroom status '><hover:show_text:'<gray>Check card room status'><#66ccff>/lcmanage cardroom status <id></#66ccff></hover></click> <gray>- Check card room status")
            }
        }
    }

}
