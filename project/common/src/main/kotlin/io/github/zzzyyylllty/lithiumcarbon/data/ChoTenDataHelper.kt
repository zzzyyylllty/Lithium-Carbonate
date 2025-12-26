package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.config
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.playerDataMap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor.RED
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submitAsync
import taboolib.expansion.persistentContainer
import java.util.concurrent.ThreadLocalRandom


object PlayerDataManager {

    val container by lazy {
        persistentContainer {
            new<PlayerData>()
        }
    }

    fun isUUIDExist(id: Long): Boolean {
        return container.get<PlayerData>().has {
            "uuid" eq id
        }
    }

    fun getDataByUUID(uuid: String): PlayerData? {
        return container.get<PlayerData>().findOne<PlayerData>(uuid)
    }


    fun getData(player: Player): PlayerData? {
        val uuid = player.uniqueId.toString()
        return container.get<PlayerData>().findOne<PlayerData>(uuid)
    }

    fun initData(player: Player): PlayerData {
        val data = getDataByUUID(player.uniqueId.toString()) ?: emptyData(player)
        playerDataMap[player.uniqueId.toString()] = data
        return data
    }
    fun releaseData(player: Player) {
        playerDataMap.remove(player.uniqueId.toString())
    }

    @Awake(LifeCycle.DISABLE)
    fun close() {
        container.close()
    }

    @SubscribeEvent
    fun onJoin(e: PlayerJoinEvent) {
        val player = e.player
        submitAsync {
            initData(player)
        }
    }
    @SubscribeEvent
    fun onQuit(e: PlayerQuitEvent) {
        val player = e.player
        submitAsync {
            releaseData(player)
        }
    }
}

class ChoTenDataHelper(player: Player) {
    val player = player
    val data = playerDataMap[player.uniqueId.toString()] ?: PlayerDataManager.initData(player)
    fun addSanity(amount: Double) {
        data.sanity += amount
    }
    fun removeSanity(amount: Double) {
        data.sanity -= amount
    }
    fun setSanity(amount: Double) {
        data.sanity -= amount
    }
    fun fullSanity() {
        data.sanity = data.maxSanity
    }
    fun resetSanity() {
        data.maxSanity = config.getDouble("default.sanity", 40.0)
    }
    fun addMaxSanity(amount: Double) {
        data.maxSanity += amount
    }
    fun removeMaxSanity(amount: Double) {
        data.maxSanity -= amount
    }
    fun setMaxSanity(amount: Double) {
        data.maxSanity -= amount
    }
    fun resetMaxSanity() {
        data.maxSanity = config.getDouble("default.max-sanity", 100.0)
    }
    fun addDarkness(amount: Double) {
        data.darkness += amount
    }
    fun removeDarkness(amount: Double) {
        data.darkness -= amount
    }
    fun setDarkness(amount: Double) {
        data.darkness -= amount
    }
    fun fullDarkness() {
        data.darkness = data.maxDarkness
    }
    fun resetDarkness() {
        data.maxDarkness = config.getDouble("default.darkness", 15.0)
    }
    fun addMaxDarkness(amount: Double) {
        data.maxDarkness += amount
    }
    fun removeMaxDarkness(amount: Double) {
        data.maxDarkness -= amount
    }
    fun setMaxDarkness(amount: Double) {
        data.maxDarkness -= amount
    }
    fun resetMaxDarkness() {
        data.maxDarkness = config.getDouble("default.max-darkness", 100.0)
    }
    fun exitNoclip() {
        data.setCooldownSec(data.noclipPoint?.cooldown ?: 10.0)
        data.noclipPoint = null
        data.noclipping = false
    }
}