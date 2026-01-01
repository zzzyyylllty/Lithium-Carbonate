package io.github.zzzyyylllty.lithiumcarbon.util

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootItems
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootItemsDef
import io.github.zzzyyylllty.lithiumcarbon.data.LootItem
import io.github.zzzyyylllty.lithiumcarbon.data.LootTemplate
import taboolib.common.util.resettableLazy

object LootGUIHelper {
    val unsearch
        get() = lootItemsDef["unsearch"] ?: LootItem("mc", "GRAY_STAINED_GLASS_PANE", linkedMapOf("name" to "<red><bold>点击搜索"), linkedMapOf())
    val searching
        get() = lootItemsDef["searching"] ?: LootItem("mc","YELLOW_STAINED_GLASS_PANE", linkedMapOf("name" to "<yellow><bold>搜索中"), linkedMapOf())
    val undefinedItem
        get() = lootItemsDef["undefinedItem"] ?: LootItem("mc","GLASS_PANE", linkedMapOf("name" to "未定物品"), linkedMapOf())
    fun getByChar(char: Char): LootItem? {
        return lootItems[char]
    }
}