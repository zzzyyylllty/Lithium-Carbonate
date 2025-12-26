package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.projectunified.uniitem.all.AllItemProvider
import io.github.projectunified.uniitem.api.ItemKey
import io.github.zzzyyylllty.embiancomponent.EmbianComponent
import io.github.zzzyyylllty.lithiumcarbon.util.VersionHelper
import io.github.zzzyyylllty.lithiumcarbon.util.componentUtil
import io.github.zzzyyylllty.lithiumcarbon.util.minimessage.toComponent
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.function.severe
import taboolib.common.platform.function.warning
import taboolib.library.xseries.XItemStack
import taboolib.module.nms.NMSItemTag.Companion.asNMSCopy

data class LootTemplate (
    val id: String,
    val name: String,
    val lootTable: LootTable,
    val elements: LinkedHashMap<String, LootItem>,
) {
}
