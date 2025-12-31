package io.github.zzzyyylllty.lithiumcarbon.data


data class LootTemplate (
    val id: String,
    val name: String,
    val title: String,
    val rows: Int,
    val layout: List<String>,
    val availableSlots: List<Int>,
    val lootTable: LootTable,
    val agents: Agents,
    val options: LootTemplateOptions,
) {
}

data class LootTemplateOptions(
    val removeLore: Boolean,
    val addLore: List<String>?,
)