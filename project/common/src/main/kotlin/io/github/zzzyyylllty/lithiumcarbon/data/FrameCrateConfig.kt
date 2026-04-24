package io.github.zzzyyylllty.lithiumcarbon.data

/**
 * Frame crate (展示框物资箱) configuration
 * A loot box that displays an item in an item frame for one-click claiming.
 * References an existing LootTemplate and generates exactly 1 item from it.
 */
data class FrameCrateConfig(
    val id: String,
    val lootTemplate: String,
    val expire: String? = null,
    val glow: Boolean = false,
    val agents: Agents? = null,
)
