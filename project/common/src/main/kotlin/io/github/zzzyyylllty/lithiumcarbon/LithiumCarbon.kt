package io.github.zzzyyylllty.lithiumcarbon

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.reloadCustomConfig
import io.github.zzzyyylllty.lithiumcarbon.data.LootInstance
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.data.LootTemplate
import io.github.zzzyyylllty.lithiumcarbon.data.define.LootDefines
import io.github.zzzyyylllty.lithiumcarbon.data.load.loadLootFiles
import io.github.zzzyyylllty.lithiumcarbon.event.LithiumCarbonReloadEvent
import org.bukkit.command.CommandSender
import taboolib.common.LifeCycle
import taboolib.common.env.RuntimeDependency
import taboolib.common.platform.Awake
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

//@RuntimeDependency(
//    value = "!com.google.code.gson:gson:2.10.1",
//    relocate = ["!com.google.gson", "!io.github.zzzyyylllty.lithiumcarbon.library.google.gson"]
//)
object LithiumCarbon : Plugin() {


    @Config("config.yml")
    lateinit var config: Configuration

    val console by lazy { console() }
    val consoleSender by lazy { console.castSafely<CommandSender>()!! }
//    val host by lazy { config.getHost("database") }
//    val dataSource by lazy { host.createDataSource() }
//    val playerDataMap = mutableMapOf<String, PlayerData>()
    val lootMap = mutableMapOf<LootLocation, LootInstance>()
    val lootTemplates = mutableMapOf<String, LootTemplate>()
    val lootDefines = mutableMapOf<String, LootDefines>()
    val lootCaches = mutableMapOf<LootLocation, LootTemplate>()
    val allowedWorlds = mutableListOf<Regex>()

    var devMode = true


    @SubscribeEvent
    fun lang(event: PlayerSelectLocaleEvent) {
        event.locale = config.getString("lang", "en_US")!!
    }

    @SubscribeEvent
    fun lang(event: SystemSelectLocaleEvent) {
        event.locale = config.getString("lang", "en_US")!!
    }

    fun reloadCustomConfig(async: Boolean = true) {
        submit(async) {

            config.reload()
            devMode = config.getBoolean("debug",false)
            lootCaches.clear()
            lootDefines.clear()
            lootTemplates.clear()
            lootMap.clear()
            allowedWorlds.clear()
            loadLootFiles()
            for (world in config.getList("allowed-worlds") ?: listOf("*")) {
                allowedWorlds.add(world.toString().toRegex())
            }
            LithiumCarbonReloadEvent().call()
        }
    }


}

@Awake(LifeCycle.ENABLE)
fun onEnable() {
    reloadCustomConfig(false)
}