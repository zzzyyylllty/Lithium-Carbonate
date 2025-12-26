package io.github.zzzyyylllty.lithiumcarbon.data

import javax.script.CompiledScript


data class LootReturn(
    val exps: Double,
    val items: List<LootItem>,
    val kether: List<List<String>>,
    val javaScript: List<CompiledScript>
)
