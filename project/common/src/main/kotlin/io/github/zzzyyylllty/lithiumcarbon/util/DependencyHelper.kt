package io.github.zzzyyylllty.lithiumcarbon.util

import org.bukkit.Bukkit


object DependencyHelper {

    val wg by lazy {
        isPluginInstalled("WorldGuard")
    }

    val papi by lazy {
        isPluginInstalled("PlaceholderAPI")
    }

    val sertraline by lazy {
        isPluginInstalled("Sertraline")
    }




    fun isPluginInstalled(name: String): Boolean {
        return (Bukkit.getPluginManager().getPlugin(name) != null)
    }

}


