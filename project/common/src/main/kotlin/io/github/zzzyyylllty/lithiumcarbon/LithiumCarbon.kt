package io.github.zzzyyylllty.lithiumcarbon

import io.github.zzzyyylllty.lithiumcarbon.data.PlayerData
import org.bukkit.command.CommandSender
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.console
import taboolib.common.platform.function.info
import taboolib.common.platform.function.submit
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.database.getHost
import taboolib.module.lang.Language
import java.time.format.DateTimeFormatter


object LithiumCarbon : Plugin() {


    @Config("config.yml")
    lateinit var config: Configuration

    val console by lazy { console() }
    val consoleSender by lazy { console.castSafely<CommandSender>()!! }
    val host by lazy { config.getHost("database") }
    val dataSource by lazy { host.createDataSource() }
    val playerRegions = mutableMapOf<String, Set<String>>()
    val playerDataMap = mutableMapOf<String, PlayerData>()

    val dateTimeFormatter: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") }
    var devMode = true

    /*
    fun compat() {
        if (Bukkit.getPluginManager().getPlugin("Chemdah") != null) {
            connectChemdah()
        }
    }*/

    fun reloadCustomConfig(async: Boolean = true) {
        submit(async) {

            config.reload()
            devMode = config.getBoolean("debug",false)
        }
    }


}
