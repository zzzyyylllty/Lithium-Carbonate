package io.github.zzzyyylllty.lithiumcarbon.util

import com.sk89q.worldguard.WorldGuard
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.config
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.logger.severeL
import io.github.zzzyyylllty.sertraline.Sertraline
import io.github.zzzyyylllty.sertraline.api.SertralineAPI
import io.github.zzzyyylllty.sertraline.api.SertralineAPIImpl
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object SertralineHelper {
    val isHooked: Boolean by lazy {
        DependencyHelper.sertraline && config.getBoolean("hook.sertraline", true)
    }
    fun buildItem(sItem: String, player: Player?, amount: Int): ItemStack? {
        if (!isHooked) return null
        try {
            val item = Sertraline.api().buildItem(sItem, player, amount = amount)
            return item
        } catch (e: ClassNotFoundException) {
            severeL("WorldGuardNotFoundException")
            return null
        } catch (e: NoClassDefFoundError) {
            severeL("WorldGuardNotFoundException")
            return null
        }
    }
}