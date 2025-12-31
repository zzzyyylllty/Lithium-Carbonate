package io.github.zzzyyylllty.lithiumcarbon.data.load

import io.github.zzzyyylllty.lithiumcarbon.data.Agent
import io.github.zzzyyylllty.lithiumcarbon.data.Agents
import io.github.zzzyyylllty.lithiumcarbon.data.Condition
import io.github.zzzyyylllty.lithiumcarbon.data.LootItem
import io.github.zzzyyylllty.lithiumcarbon.data.defaultData
import io.github.zzzyyylllty.lithiumcarbon.function.kether.parseKether
import org.bukkit.entity.Player
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
    fun getItem(input: Any?): LootItem? {
        val input = input as? LinkedHashMap<String, Any?>? ?: (input as? String?)?.let { linkedMapOf("item" to it) } ?: return null

        val namespaceID = input["item"]?.toString() ?: "grass_block"

        val split = namespaceID.split(":").toMutableList()
        val source = if (split.size >= 2) split.first().lowercase() else "mc"
        split.removeFirst()
        val item = split.joinToString(":")

        val parameters = (input["parameters"] ?: input["parameter"]) as LinkedHashMap<String, Any?>?
        val components = (input["components"] ?: input["component"]) as LinkedHashMap<String, Any?>?

        return LootItem(source, item, parameters, components)

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
    fun getConditions(input: Any?): Condition? {

        if (input == null) return null

        val conditionsRaw = if (input !is Map<*, *>) return null else input["conditions"] ?: input["condition"]

        return if (conditionsRaw is String || conditionsRaw is List<*>) {
            Condition(
                kether = conditionsRaw.asListEnhanced()
            )
        } else {
            val map = conditionsRaw as? Map<*, *>? ?: return null
            Condition(
                kether = (map["ke"] ?: map["KE"] ?: map["KETHER"]?: map["kether"]).asListEnhanced(),
                js = (map["js"] ?: map["JS"] ?: map["javascript"] ?: map["JAVASCRIPT"]).asListedStringEnhanced()?.compileJS()
            )
        }

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
