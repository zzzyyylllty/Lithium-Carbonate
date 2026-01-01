package io.github.zzzyyylllty.lithiumcarbon.data

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
    fun reset() {
        searches.clear()
    }
}
data class SingleSearchStat(
    val slot: Int,
    val endTime: Long,
    val isSkip: Boolean
) {
    fun isSearchEnded(): Boolean {
        return if (isSkip) true else endTime <= System.currentTimeMillis()
    }
}