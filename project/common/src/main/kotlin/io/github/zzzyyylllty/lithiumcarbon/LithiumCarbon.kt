package io.github.zzzyyylllty.lithiumcarbon

import io.github.zzzyyylllty.lithiumcarbon.data.LootInstance
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.data.PlayerData
import io.github.zzzyyylllty.lithiumcarbon.event.LithiumCarbonReloadEvent
import io.github.zzzyyylllty.sertraline.Sertraline
import org.bukkit.command.CommandSender
import taboolib.common.platform.Plugin
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.console
import taboolib.common.platform.function.info
import taboolib.common.platform.function.submit
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.database.getHost
import taboolib.module.lang.Language
import taboolib.module.lang.event.PlayerSelectLocaleEvent
import taboolib.module.lang.event.SystemSelectLocaleEvent
import java.time.format.DateTimeFormatter


object LithiumCarbon : Plugin() {


    @Config("config.yml")
    lateinit var config: Configuration

    val console by lazy { console() }
    val consoleSender by lazy { console.castSafely<CommandSender>()!! }
    val host by lazy { config.getHost("database") }
    val dataSource by lazy { host.createDataSource() }
    val playerDataMap = mutableMapOf<String, PlayerData>()
    val lootMap = mutableMapOf<LootLocation, LootInstance>()

    var devMode = true

    /*
    fun compat() {
        if (Bukkit.getPluginManager().getPlugin("Chemdah") != null) {
            connectChemdah()
        }
    }*/


    @SubscribeEvent
    fun lang(event: PlayerSelectLocaleEvent) {
        event.locale = Sertraline.config.getString("lang", "en_US")!!
    }

    @SubscribeEvent
    fun lang(event: SystemSelectLocaleEvent) {
        event.locale = Sertraline.config.getString("lang", "en_US")!!
    }

    fun reloadCustomConfig(async: Boolean = true) {
        submit(async) {

            config.reload()
            devMode = config.getBoolean("debug",false)
            LithiumCarbonReloadEvent().call()
        }
    }


}
