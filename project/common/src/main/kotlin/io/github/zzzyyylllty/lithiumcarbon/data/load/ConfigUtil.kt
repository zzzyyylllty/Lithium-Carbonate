package io.github.zzzyyylllty.lithiumcarbon.data.load

import io.github.zzzyyylllty.lithiumcarbon.data.Agent
import io.github.zzzyyylllty.lithiumcarbon.data.Agents
import io.github.zzzyyylllty.sertraline.Sertraline.console
import io.github.zzzyyylllty.sertraline.Sertraline.mappings
import io.github.zzzyyylllty.sertraline.event.FeatureLoadEvent
import io.github.zzzyyylllty.sertraline.logger.warningS
import io.github.zzzyyylllty.sertraline.util.minimessage.legacyToMiniMessage
import io.github.zzzyyylllty.sertraline.util.serialize.isListOfType
import taboolib.common5.compileJS
import taboolib.module.lang.asLangText
import kotlin.collections.contains
import kotlin.collections.forEach
import kotlin.collections.getOrElse
import kotlin.collections.set
import kotlin.collections.toMutableMap
import kotlin.let
import kotlin.text.isEmpty
import kotlin.text.matches
import kotlin.text.split
import kotlin.text.toInt
import kotlin.text.toLong
import kotlin.text.toRegex
import kotlin.to

object ConfigUtil {
    fun getString(input: Any?): String? {
        return input?.toString()
    }
    fun getInt(input: Any?): Int? {
        return input?.toString()?.toInt()
    }

    fun getLong(input: Any?): Long? {
        return input?.toString()?.toLong()
    }
    fun getDeep(input: Any?, location: String): Any? {
        if (input == null || location.isEmpty()) return null

        val keys = location.split(".")
        var current: Any? = input

        for (key in keys) {
            if (current !is Map<*, *>) return input
            current = current[key]
        }
        return current
    }
    fun getAgents(input: Any?): Agents? {

        if (input == null) return null

        val agentsRaw = if (input !is Map<*, *>) return null else input["agents"] ?: input["agent"]

        val agents = LinkedHashMap<String, Agent>()

        for (agentPart in agentsRaw as LinkedHashMap<String, LinkedHashMap<String, Any?>>) {
            val agentName = agentPart.key
            val agentsPartRaw = agentPart.value

            // 输入容错，出于性能考虑，优先使用第一个给定的值
            agents[agentName] = Agent(
                trigger = agentName,
                js = (agentsPartRaw["js"] ?: agentsPartRaw["JS"] ?: agentsPartRaw["javascript"] ?: agentsPartRaw["JAVASCRIPT"]).asListedStringEnhanced()?.compileJS(),
                asyncJs = (agentsPartRaw["async_js"] ?: agentsPartRaw["ASYNC_JS"] ?: agentsPartRaw["asyncjs"] ?: agentsPartRaw["ASYNCJS"] ?: agentsPartRaw["js_async"] ?: agentsPartRaw["JS_ASYNC"] ?: agentsPartRaw["jsasync"] ?: agentsPartRaw["JSASYNC"]).asListedStringEnhanced()?.compileJS(),
                kether = (agentsPartRaw["ke"] ?: agentsPartRaw["KE"] ?: agentsPartRaw["KETHER"]?: agentsPartRaw["kether"]).asListEnhanced(),
                asyncKe = (agentsPartRaw["ke"] ?: agentsPartRaw["KE"] ?: agentsPartRaw["KETHER"]?: agentsPartRaw["kether"]).asListEnhanced(),
                )
        }

        return Agents(agents)
    }
    fun existDeep(input: Any?, location: String): Boolean {
        if (input == null || location.isEmpty()) return false

        val keys = location.split(".")
        var current: Any? = input

        for (key in keys) {
            if (current !is Map<*, *>) return (current != null)
            current = current[key]
        }
        return (current != null)
    }

    fun getFeature(sItemId: String?, input: Map<*, *>?, feature: String): Any? {
        val geted = getFeatureDefault(input, feature)
        val event = FeatureLoadEvent(sItemId,geted, feature, null)
        event.call()
        return event.result ?: geted // 如果未触发特殊处理
    }


    fun getFeatures(
        sItemId: String?,
        input: Map<*, *>?,
        features: List<String>,
        final: Map<String, Any?>? = null
    ): Map<String, Any?> {
        val baseMap = final?.toMutableMap() ?: linkedMapOf()
        features.forEach { feature ->
            val unparsed = getFeature(sItemId, input, feature)
            unparsed?.let { baseMap[feature] = transformValue(it) }
        }
        return baseMap
    }

    private fun transformValue(value: Any): Any = when {
        value is String -> value.legacyToMiniMessage()
        value is List<*> && isListOfType<String>(value) -> value.legacyToMiniMessage()
        else -> value
    }


}
fun getFeatureDefault(input: Map<*, *>?, feature: String): Any? {

    if (input == null) {
        return null
    }

    val mapping = mappings[feature]
    if (mapping == null) {
        warningS(console.asLangText("Warning_No_Mapping", feature))
        return null
    }

    for (entry in mapping) {
        val (namespace, id) = entry.split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        val section = input[namespace] as? Map<*, *>
        if (section?.contains(id) == true) { // 如果找到了对应的mapping
            return section[id]
        }
    }
    return null
}

fun checkRegexMatch(input: String, regex: String): Boolean {
    return input.matches(regex.toRegex())
}



fun Any?.asListEnhanced() : List<String>? {
    if (this == null) return null
    val thisList = if (this is List<*>) this else listOf(this)
    val list = mutableListOf<String>()
    for (string in thisList) {
        if (string == null) continue
        list.addAll(string.toString().split("\n","<br>", ignoreCase = true))
    }
    if (!list.isEmpty() && list.last() == "") list.removeLast()
    return list
}

fun Any?.asListedStringEnhanced() : String? {
    return this.asListEnhanced()?.joinToString("\n")
}
