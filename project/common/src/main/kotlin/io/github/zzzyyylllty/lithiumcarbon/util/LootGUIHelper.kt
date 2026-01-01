package io.github.zzzyyylllty.lithiumcarbon.util

import io.github.zzzyyylllty.lithiumcarbon.data.LootItem
import taboolib.common.util.resettableLazy

object LootGUIHelper {
    val unsearch = LootItem("mc", "GRAY_STAINED_GLASS_PANE", linkedMapOf("name" to "<red><bold>点击搜索"), linkedMapOf())
    val searching = LootItem("mc","YELLOW_STAINED_GLASS_PANE", linkedMapOf("name" to "<yellow><bold>搜索中"), linkedMapOf())
    val undefinedItem = LootItem("mc","GOLD_INGOT", linkedMapOf("name" to "战利品"), linkedMapOf())
}