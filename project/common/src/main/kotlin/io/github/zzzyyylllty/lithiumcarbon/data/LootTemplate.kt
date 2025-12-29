package io.github.zzzyyylllty.lithiumcarbon.data


data class LootTemplate (
    val id: String,
    val name: String,
    val title: String,
    val rows: Int,
    val layout: List<String>?,
    val availableSlots: List<Int>,
    val lootTable: LootTable,
    val elements: LinkedHashMap<String, LootItem>,
    val staticItem: LinkedHashMap<String, LootItem>
) {
    fun getLayout(): List<String> {
        return layout ?: listOf("         ","         ","         ")
    }
}
