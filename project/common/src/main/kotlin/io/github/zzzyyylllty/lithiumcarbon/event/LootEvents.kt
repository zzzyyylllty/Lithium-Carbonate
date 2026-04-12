package io.github.zzzyyylllty.lithiumcarbon.event

import io.github.zzzyyylllty.lithiumcarbon.data.LootElement
import io.github.zzzyyylllty.lithiumcarbon.data.LootInstance
import io.github.zzzyyylllty.lithiumcarbon.data.LootTemplate
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import taboolib.platform.type.BukkitProxyEvent

class ItemSearchStartEvent(
    val player: Player,
    val initialInstance: LootInstance,
    val lootElement: LootElement,
    val rawSlot: Int,
    val inventory: Inventory,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()


class ItemSearchCompletePreEvent(
    val player: Player,
    val initialInstance: LootInstance,
    val lootElement: LootElement,
    val rawSlot: Int,
    val inventory: Inventory,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()



class ItemSearchCompletePostEvent(
    val player: Player,
    val initialInstance: LootInstance,
    val lootElement: LootElement,
    val rawSlot: Int,
    val inventory: Inventory,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()


class LootElementApplyEvent(
    val player: Player,
    val initialInstance: LootInstance,
    val lootElement: LootElement,
    val rawSlot: Int,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()



class LootItemGrantEvent(
    val player: Player,
    val template: LootTemplate,
    val lootElement: LootElement,
    var item: ItemStack,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()
