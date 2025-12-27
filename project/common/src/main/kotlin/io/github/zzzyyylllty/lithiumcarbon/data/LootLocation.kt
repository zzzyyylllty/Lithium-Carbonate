package io.github.zzzyyylllty.lithiumcarbon.data

import com.sk89q.worldedit.bukkit.BukkitAdapter
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.util.Vector
import kotlin.math.roundToInt

data class LootLocation(
    val world: String,
    val x: Int,
    val y: Int,
    val z: Int
) {
    fun toFormat(): String {
        return "$world $x $y $z"
    }
    fun toBukkitLocation(): Location {
        return Location(Bukkit.getWorld(world), x.toDouble(), y.toDouble(), z.toDouble())
    }
    fun toWGLocation(): com.sk89q.worldedit.util.Location? {
        return BukkitAdapter.adapt(toBukkitLocation())
    }
}

object LocationHelper {
    fun toLocation(world: String,x: Int,y: Int,z: Int): LootLocation {
        return LootLocation(world,x,y,z)
    }

    fun toLootLocation(l: Location): LootLocation {
        return LootLocation(l.world.name, l.x.roundToInt(), l.y.roundToInt(), l.z.roundToInt())
    }

    fun toLootLocation(l: Vector, world: String): LootLocation {
        return LootLocation(world, l.x.roundToInt(), l.y.roundToInt(), l.z.roundToInt())
    }
}