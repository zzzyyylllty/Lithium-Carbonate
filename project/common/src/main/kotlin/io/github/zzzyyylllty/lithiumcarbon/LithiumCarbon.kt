package io.github.zzzyyylllty.lithiumcarbon

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.reloadCustomConfig
import io.github.zzzyyylllty.lithiumcarbon.data.LootInstance
import io.github.zzzyyylllty.lithiumcarbon.data.LootItem
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.data.LootTemplate
import io.github.zzzyyylllty.lithiumcarbon.data.define.LootDefines
import io.github.zzzyyylllty.lithiumcarbon.data.load.loadItemFiles
import io.github.zzzyyylllty.lithiumcarbon.data.load.loadLootFiles
import io.github.zzzyyylllty.lithiumcarbon.event.LithiumCarbonReloadEvent
import io.github.zzzyyylllty.lithiumcarbon.gui.openedLootLocation
import io.github.zzzyyylllty.lithiumcarbon.util.serialize.toUUID
import org.bukkit.Bukkit
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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import io.github.zzzyyylllty.lithiumcarbon.cardroom.CardRoomConfig
import io.github.zzzyyylllty.lithiumcarbon.cardroom.CardRoomInstance
import io.github.zzzyyylllty.lithiumcarbon.cardroom.CardRoomLoader
import io.github.zzzyyylllty.lithiumcarbon.cardroom.CardRoomManager
import io.github.zzzyyylllty.lithiumcarbon.data.FrameCrateConfig
import io.github.zzzyyylllty.lithiumcarbon.data.load.loadFrameCrateFiles
import io.github.zzzyyylllty.lithiumcarbon.frame.FrameCrateManager

//@RuntimeDependency(
//    value = "!com.google.code.gson:gson:2.10.1",
//    relocate = ["!com.google.gson", "!io.github.zzzyyylllty.lithiumcarbon.library.google.gson"]
//)
object LithiumCarbon : Plugin() {


    @Config("config.yml")
    lateinit var config: Configuration

    val console by lazy { console() }
    val consoleSender by lazy { console as? CommandSender ?: Bukkit.getConsoleSender() }
//    val host by lazy { config.getHost("database") }
//    val dataSource by lazy { host.createDataSource() }
//    val playerDataMap = mutableMapOf<String, PlayerData>()
    data class LootInstanceKey(val location: LootLocation, val playerId: UUID?)
    val lootMap = ConcurrentHashMap<LootInstanceKey, LootInstance>()
    val lootTemplates = mutableMapOf<String, LootTemplate>()
    val lootDefines = mutableMapOf<String, LootDefines>()
    val lootCaches = ConcurrentHashMap<LootLocation, LootTemplate>()
    val lootItems = mutableMapOf<Char, LootItem>()
    val lootItemsDef = mutableMapOf<String, LootItem>()
    val allowedWorlds = mutableListOf<Regex>()
    var reloadTimes: Int = 0

    // 卡房系统集合
    val cardRoomConfigs = mutableMapOf<String, CardRoomConfig>()
    val cardRoomInstances = ConcurrentHashMap<String, CardRoomInstance>()

    // 展示框物资箱系统
    val frameCrateConfigs = mutableMapOf<String, FrameCrateConfig>()

    var devMode = true


    @SubscribeEvent
    fun lang(event: PlayerSelectLocaleEvent) {
        event.locale = config.getString("lang") ?: "en_US"
    }

    @SubscribeEvent
    fun lang(event: SystemSelectLocaleEvent) {
        event.locale = config.getString("lang") ?: "en_US"
    }

    fun reloadCustomConfig(async: Boolean = true) {
        submit(async) {

            reloadTimes++
            config.reload()
            devMode = config.getBoolean("debug",false)
            lootCaches.clear()
            lootDefines.clear()
            lootTemplates.clear()
            lootMap.clear()
            allowedWorlds.clear()
            lootItems.clear()
            lootItemsDef.clear()
            // 重载时同步重置所有卡房
            CardRoomManager.resetAllCardRoomsSync()
            cardRoomConfigs.clear()
            cardRoomInstances.clear()
            // 重载时清除所有展示框物资箱
            FrameCrateManager.removeAll()
            frameCrateConfigs.clear()
            openedLootLocation.forEach {
                Bukkit.getPlayer(it.key)?.closeInventory()
            }
            openedLootLocation.clear()
            loadItemFiles()
            loadLootFiles()
            // 加载展示框物资箱配置
            loadFrameCrateFiles()
            // 加载卡房配置
            CardRoomLoader.loadCardRoomFiles()
            for (world in config.getList("allowed-worlds") ?: listOf(".+")) {
                allowedWorlds.add(world.toString().toRegex())
            }
            for (loot in lootTemplates.values) {
                loot.update.runUpdate(loot)
            }
            // 初始化卡房管理器
            CardRoomManager.init()
            LithiumCarbonReloadEvent().call()
        }
    }


}

@Awake(LifeCycle.ENABLE)
fun onEnable() {
    reloadCustomConfig(false)
}

@Awake(LifeCycle.DISABLE)
fun onDisable() {
    // 服务器关闭时同步重置所有卡房
    CardRoomManager.resetAllCardRoomsSync()
    // 清理所有展示框物资箱
    FrameCrateManager.removeAll()
}