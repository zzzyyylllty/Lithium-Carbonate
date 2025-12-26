package io.github.zzzyyylllty.lithiumcarbon.command

import com.google.common.collect.Lists
import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.github.zzzyyylllty.lithiumcarbon.util.minimessage.toComponent
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.configuration.PluginMeta
import me.clip.placeholderapi.libs.kyori.adventure.text.Component.toComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.plugin.java.JavaPlugin
import org.jspecify.annotations.NullMarked
import taboolib.common.platform.event.SubscribeEvent
import java.util.*
import java.util.function.Predicate


val blockedCommands = mutableListOf<String>(
    "pl",
    "plugins",
    "version",
    "ver",
    "icanhasbukkit",
    "about",
    "help"
)

private data class FakePluginInstance(
    val name: String,
    val version: String,
    val description: String? = null,
    val authors: List<String> = emptyList(),
    val website: String? = null,
    val enabled: Boolean = true,
) {
    fun generateFakeLore(): String {
        val version = "Version: <green>$version</green><br>"
        val description = description?.let { "Description: <green>$it</green><br>" }
        val website = website?.let { "Website: <green>$it</green><br>" }
        val author = when (authors.size) {
            0 -> null // 空列表，返回空字符串
            1 -> "Author: <green>${authors.first()}</green><br>" // 只有一个作者
            2 -> "Authors: <green>${authors.first()}</green> and <green>${authors.last()}</green><br>" // 两个作者，用 " and " 连接
            else -> { // 三个或更多作者
                // 获取除最后一个作者之外的所有作者，并用 ", " 连接
                val allButLast = authors.dropLast(1).joinToString(", ") { "<green>$it</green>" }
                // 获取最后一个作者
                val lastAuthor = "<green>${authors.last()}</green>"
                // 组合成最终字符串
                "Authors: $allButLast and $lastAuthor<br>"
            }
        }
        val color = if (enabled) "green" else "red"
        return "<$color><hover:show_text:'${(version + (description ?: "") + (website ?: "") + (author ?: "")).removeSuffix("<br>")}'><click:run_command:'/version $name'>$name</click></hover></$color>, "

    }
    fun generateFakeVersionLore(): List<Component> {
        val list = mutableListOf("<green>$name</green> version <green>$version</green>")
        if (description != null) list.add(description)
        if (website != null) list.add("Website: <green>$website</green>")
        if (authors.isNotEmpty()) when (authors.size) {
            0 -> null // 空列表，返回空字符串
            1 -> "Author: <green>${authors.first()}</green>" // 只有一个作者
            2 -> "Authors: <green>${authors.first()}</green> and <green>${authors.last()}</green>" // 两个作者，用 " and " 连接
            else -> { // 三个或更多作者
                // 获取除最后一个作者之外的所有作者，并用 ", " 连接
                val allButLast = authors.dropLast(1).joinToString(", ") { "<green>$it</green>" }
                // 获取最后一个作者
                val lastAuthor = "<green>${authors.last()}</green>"
                // 组合成最终字符串
                "Authors: $allButLast and $lastAuthor"
            }
        }?.let { list.add(it) }
        return list.toComponent()
    }
}

private val leavesPlugins = listOf(
    FakePluginInstance("HeartsOfIronIV","0.1.17.2", "A famous World War II simulation game!", listOf("Paradox Interactive CO."))
)
private val paperPlugins = listOf(
    FakePluginInstance("CraftEngine","0.0.65.19", null, listOf("XiaoMoMi"))
)
private val bukkitPlugins = listOf(
    FakePluginInstance("BetterHud","1.14.0", "A multiplatform server-side implementation of HUD in Minecraft.", listOf("toxicity"), "https://www.spigotmc.org/resources/115559"),
    FakePluginInstance("LithiumCarbon","2000.5.5","ChoTen backrooms plugin.",listOf("AkaCandyKAngel")),
    FakePluginInstance("Sertraline","3.7.1","An advanced item plugin. ChoTen item management plugin.",listOf("AkaCandyKAngel", "jhqwqmc")),
    FakePluginInstance("PlaceHolderAPI","2.11.6","An awesome placeholder provider!",listOf("HelpChat"))
)

private val fakePluginsMessage: List<Component> by lazy {

    val INFO_COLOR = TextColor.color(52, 159, 218)
    val SERVER_PLUGIN_INFO = Component.text("ℹ What is a server plugin?", INFO_COLOR)
        .append("""
            <white>Server plugins can add new behavior to your server!
            You can find new plugins on Paper's plugin repository, Hangar.
            
            https://hangar.papermc.io/
            """.trimIndent().toComponent())
    val INFO_ICON_START = Component.text("ℹ ", INFO_COLOR)
    val INFO_ICON_SERVER_PLUGIN = INFO_ICON_START.hoverEvent(SERVER_PLUGIN_INFO).clickEvent(ClickEvent.openUrl("https://docs.papermc.io/paper/adding-plugins"))
    val list = mutableListOf<Component>()
    list.add(Component.text().append(INFO_ICON_SERVER_PLUGIN).append(Component.text("Server Plugins (${paperPlugins.size + bukkitPlugins.size + leavesPlugins.size}):")).build())
    list.add("<#37D1AB>Leaves Plugins (${leavesPlugins.size}):</#37D1AB>".toComponent())
    list.add(generatePluginsList(leavesPlugins))
    list.add("<#0288D1>Paper Plugins (${paperPlugins.size}):</#0288D1>".toComponent())
    list.add(generatePluginsList(paperPlugins))
    list.add("<#ED8106>Bukkit Plugins (${bukkitPlugins.size}):</#ED8106>".toComponent())
    list.add(generatePluginsList(bukkitPlugins))
    list
}

private val fakePluginsMap by lazy {
    val list = paperPlugins + bukkitPlugins + leavesPlugins
    val map = mutableMapOf<String, FakePluginInstance>()
    list.forEach {
        map[it.name] = it
    }
    map
}

private fun generatePluginsList(plugins: List<FakePluginInstance>): Component {
    var pluginsText = ""
    plugins.forEach {
        pluginsText += it.generateFakeLore()
    }
    return "<dark_gray> - </dark_gray>$pluginsText".removeSuffix(", ").toComponent()
}

@SubscribeEvent
fun fakeCommandListener(e: PlayerCommandPreprocessEvent) {
    val command = e.message.split(" ").first().split(":").last().removePrefix("/")
    if (blockedCommands.contains(command.removeSuffix("!"))) {
        val isBypass = e.player.hasPermission("lithiumcarbon.bypassfakecommandhelp")
        if (isBypass && command.endsWith("!")) {
        }
        if (isBypass) e.player.sendMessage("正在为您展示假信息。如要获取真信息，请在指令头末尾添加!，例如/pl!, /ver! Sertraline")
        if (command.endsWith("!") && isBypass) {
            val split = e.message.split(" ").toMutableList()
            split[0] = split.first().removeSuffix("!")
            e.message = split.joinToString(" ")
            return
        }
        val message: List<Component>? = when (command.removeSuffix("!")) {
            "pl", "plugins" -> {
                e.isCancelled = true
                fakePluginsMessage
            }
            "version", "ver", "icanhasbukkit", "about" -> {
                if (command.startsWith("ver") && e.message.contains(" ")) {
                    e.isCancelled = true
                    fakePluginsMap[e.message.split(" ")[1]]?.generateFakeVersionLore()
                } else {
                    e.isCancelled = true
                    listOf(
                        "<white>This server is running <yellow>ChoTenLeaf</yellow> version 1.21.4-514-ver/1.21.4@20ff718 (2025-07-18T01:20:36Z) (Implementing API Version 11.45.14-R1.919810-SNAPSHOT) (Git: rain505 on ver/11.45.14)".toComponent(),
                        "<red>* Error obtaining version infomation".toComponent(),
                        "<gray><i>>Previous version: Minestom-114514 (MC: 11.45.14)".toComponent()
                    )
                }
            }
            "help" -> null
            else -> null
        }
        message?.let { it.forEach { it -> e.player.sendMessage(it) } }
    }
}