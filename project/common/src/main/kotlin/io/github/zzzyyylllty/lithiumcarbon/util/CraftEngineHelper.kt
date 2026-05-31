package io.github.zzzyyylllty.lithiumcarbon.util

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.UpdateFlags
import net.momirealms.craftengine.core.plugin.CraftEngine
import net.momirealms.craftengine.core.util.Key
import net.momirealms.craftengine.core.world.BlockPos
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.collections.iterator
import net.momirealms.craftengine.bukkit.util.BlockStateUtils
import net.momirealms.craftengine.core.block.property.Property
import java.util.concurrent.ConcurrentHashMap

object CraftEngineHelper {

    // Cache for Property objects to avoid repeated lookups
    private val propertyCache = ConcurrentHashMap<String, Property<*>>()

    /**
     * Gets the current block state at position and modifies a property
     */
    fun modifyBlockProperty(
        world: World,
        x: Int,
        y: Int,
        z: Int,
        propertyName: String,
        value: String
    ): Boolean {
        val currentState = getBlockState(world, x, y, z)
        if (currentState == null || currentState.isEmpty()) {
            return false
        }

        val property = getProperty(currentState, propertyName) ?: return false
        val propertyValue = property.valueByName(value) ?: return false

        // Use static with() method to handle type conversion
        val newState = ImmutableBlockState.with(currentState, property, propertyValue)

        val location = org.bukkit.Location(world, x.toDouble(), y.toDouble(), z.toDouble())
        return CraftEngineBlocks.place(location, newState, 3, false)
    }

    /**
     * Gets the current block state at position
     */
    fun getBlockState(world: World, x: Int, y: Int, z: Int): ImmutableBlockState? {
        val ceWorld = CraftEngine.instance().worldManager().getWorld(world.uid)
        val pos = BlockPos(x, y, z)
        return ceWorld.getBlockStateAtIfLoaded(pos)
    }

    /**
     * Gets a property with caching
     */
    private fun getProperty(state: ImmutableBlockState, propertyName: String): Property<*>? {
        val cacheKey = state.owner().value().id().toString() + ":" + propertyName

        return propertyCache.computeIfAbsent(cacheKey) {
            state.owner().value().getProperty(propertyName)!!
        }
    }

    /**
     * Clears the property cache (call this when CraftEngine reloads)
     */
    fun clearPropertyCache() {
        propertyCache.clear()
    }

    // ========== BlockPos Creation ==========

    fun createBlockPos(x: Int, y: Int, z: Int): BlockPos {
        return BlockPos(x, y, z)
    }

    // ========== Block State Operations ==========

    fun getCustomBlockState(block: Block): ImmutableBlockState? {
        return CraftEngineBlocks.getCustomBlockState(block)
    }

    fun isCustomBlock(block: Block): Boolean {
        return CraftEngineBlocks.isCustomBlock(block)
    }

    fun setBlockState(
        world: World,
        x: Int,
        y: Int,
        z: Int,
        blockId: String
    ): Boolean {
        val key = net.momirealms.craftengine.core.util.Key.of(blockId)
        val location = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
        return CraftEngineBlocks.place(location, key, true)
    }

    fun setBlockStateWithProperty(
        world: World,
        x: Int,
        y: Int,
        z: Int,
        blockId: String,
        propertyName: String,
        propertyValue: String
    ): Boolean {
        val key = net.momirealms.craftengine.core.util.Key.of(blockId)
        val block = CraftEngineBlocks.byId(key) ?: return false
        val blockState = block.defaultState()

        val property = block.getProperty(propertyName) ?: return false
        val value = property.valueByName(propertyValue) ?: return false
        val newState = ImmutableBlockState.with(blockState, property, value)

        val location = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
        return CraftEngineBlocks.place(location, newState, UpdateFlags.UPDATE_ALL, true)
    }

    @Suppress("UNCHECKED_CAST")
    fun setBlockProperty(state: ImmutableBlockState, propertyName: String, value: String): ImmutableBlockState? {
        val property = state.owner().value().getProperty(propertyName) ?: return null
        val propertyValue = property.valueByName(value) ?: return null
        return ImmutableBlockState.with(state, property, propertyValue)
    }

    fun placeBlock(location: Location, blockId: String, playSound: Boolean): Boolean {
        val key = Key.of(blockId)
        return CraftEngineBlocks.place(location, key, playSound)
    }

    fun removeBlock(block: Block): Boolean {
        return CraftEngineBlocks.remove(block)
    }

    // ========== Item Operations ==========

    fun getCustomItem(itemId: String): Any? {
        return CraftEngineItems.byId(itemId)
    }

    fun isCustomItem(itemStack: ItemStack): Boolean {
        return CraftEngineItems.isCustomItem(itemStack)
    }

    fun getCustomItemId(itemStack: ItemStack): String? {
        val key = CraftEngineItems.getCustomItemId(itemStack) ?: return null
        return key.toString()
    }

    fun getItemByStack(itemStack: ItemStack): Any? {
        return CraftEngineItems.byItemStack(itemStack)
    }

    // ========== Furniture Operations ==========

    fun placeFurniture(location: Location, furnitureId: String): Any? {
        val key = Key.of(furnitureId)
        return CraftEngineFurniture.place(location, key)
    }

    fun placeFurnitureWithVariant(location: Location, furnitureId: String, variant: String): Any? {
        val key = Key.of(furnitureId)
        return CraftEngineFurniture.place(location, key, variant)
    }

    fun placeFurnitureWithSound(location: Location, furnitureId: String, variant: String, playSound: Boolean): Any? {
        val key = Key.of(furnitureId)
        return CraftEngineFurniture.place(location, key, variant, playSound)
    }

    fun rayTraceFurniture(player: Player): Any? {
        return CraftEngineFurniture.rayTrace(player)
    }

    fun rayTraceFurnitureAtDistance(player: Player, distance: Double): Any? {
        return CraftEngineFurniture.rayTrace(player, distance)
    }

    // ========== World Manager Access ==========

    fun getCEWorld(world: World): Any {
        return CraftEngine.instance().worldManager().getWorld(world.uid)
    }

    // ========== Update Flags ==========

    fun getUpdateFlagAll(): Int {
        return UpdateFlags.UPDATE_ALL
    }

    fun getUpdateFlagNone(): Int {
        return UpdateFlags.UPDATE_NONE
    }

    fun getUpdateFlagNeighbors(): Int {
        return UpdateFlags.UPDATE_NEIGHBORS
    }

    // ========== Utility Methods ==========

    fun getKey(string: String): Key {
        return Key.of(string)
    }

    fun adaptPlayer(player: Player): Any? {
        return BukkitAdaptor.adapt(player)
    }

}