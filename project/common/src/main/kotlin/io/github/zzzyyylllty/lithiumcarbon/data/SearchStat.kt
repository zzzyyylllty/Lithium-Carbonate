package io.github.zzzyyylllty.lithiumcarbon.data

import kotlin.compareTo

data class SearchStat(
    val searches: LinkedHashMap<Int, SingleSearchStat>
) {
    fun isSearchEnded(id: Int): Boolean? {
        return searches[id]?.isSearchEnded()
    }
    fun isSearchExist(id: Int): Boolean {
        return searches[id] != null
    }
    fun getSearch(id: Int): SingleSearchStat? {
        return searches[id]
    }
    fun addSearch(id: Int, ms: Long) {
        searches[id] = SingleSearchStat(id, System.currentTimeMillis() + ms, false)
    }
    fun reset() {}
}
data class SingleSearchStat(
    val slot: Int,
    val endTime: Long,
    val isEnded: Boolean
) {
    fun isSearchEnded(): Boolean {
        return if (isEnded) true else endTime <= System.currentTimeMillis()
    }
}