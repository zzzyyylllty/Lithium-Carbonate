package io.github.zzzyyylllty.lithiumcarbon.unlock

import io.github.zzzyyylllty.lithiumcarbon.data.LootInstance
import io.github.zzzyyylllty.lithiumcarbon.data.LootTemplate
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent

data class UnlockContext(
    val player: Player,
    val block: Block,
    val event: PlayerInteractEvent?,
    val lootTemplate: LootTemplate,
    val lootInstance: LootInstance,
    val uiConfig: UnlockUIConfig,
    val lightConfig: UnlockLightConfig,
    val onSuccess: (LootInstance) -> Unit,
    val onFail: (String) -> Unit,
)
