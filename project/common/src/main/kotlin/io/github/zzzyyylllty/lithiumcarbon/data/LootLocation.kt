package io.github.zzzyyylllty.lithiumcarbon.data

import com.sk89q.worldedit.bukkit.BukkitAdapter
import io.github.zzzyyylllty.lithiumcarbon.logger.severeL
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.util.Vector
import kotlin.math.roundToInt

data class LootLocation(
    val world: String,
    val vector: LootVector
) {
    val x
        get() = vector.x
    val y
        get() = vector.y
    val z
        get() = vector.z
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
data class LootVector(
    val x: Int,
    val y: Int,
    val z: Int
) {
}

object LocationHelper {
    fun toLocation(world: String,x: Int,y: Int,z: Int): LootLocation {
        return LootLocation(world, LootVector(x,y,z))
    }

    fun toLocationByString(string: String): LootLocation {
        val split = string.split(" ")
        if (split.size <= 3) {
            severeL("ErrorInvalidLocation")
            return LootLocation("world", LootVector(0,0,0))
        }
        return LootLocation(split[0], LootVector(split[1].toInt(),split[2].toInt(),split[3].toInt()))
    }
    fun toVectorByString(string: String): LootVector {
        val split = string.split(" ")
        if (split.size <= 2) {
            severeL("ErrorInvalidVector")
            return LootVector(0,0,0)
        }
        return if (split.size == 3) {
            LootVector(split[0].toInt(),split[1].toInt(),split[2].toInt())
        } else {
            LootVector(split[1].toInt(),split[2].toInt(),split[3].toInt())
        }
    }

    fun toLootLocation(l: Location): LootLocation {
        return LootLocation(l.world.name, LootVector(l.x.roundToInt(), l.y.roundToInt(), l.z.roundToInt()))
    }

    fun toLootVector(l: Location): LootVector {
        return LootVector(l.x.roundToInt(), l.y.roundToInt(), l.z.roundToInt())
    }

    fun toLootLocation(l: Vector, world: String): LootLocation {
        return LootLocation(world,LootVector( l.x.roundToInt(), l.y.roundToInt(), l.z.roundToInt()))
    }
}