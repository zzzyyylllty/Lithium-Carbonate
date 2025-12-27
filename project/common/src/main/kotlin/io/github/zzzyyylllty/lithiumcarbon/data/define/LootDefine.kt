package io.github.zzzyyylllty.lithiumcarbon.data.define

import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation

interface LootDefine {
    val type: String
    fun isValidLocation(location: LootLocation): Boolean
}