package io.github.zzzyyylllty.lithiumcarbon.data.index

import io.github.zzzyyylllty.lithiumcarbon.data.Condition
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.data.load.ConfigUtil
import io.github.zzzyyylllty.lithiumcarbon.util.WorldGuardHelper
import io.github.zzzyyylllty.lithiumcarbon.util.getBlockID
import org.bukkit.block.Block
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap
import kotlin.text.matches
import kotlin.text.toRegex

/**
 * Loot index condition, extensible via LootIndexConditionRegistry
 */
fun interface LootIndexCondition {
    fun check(location: LootLocation, block: Block, player: Player): Boolean
}

/**
 * Condition builder: builds a checker from the raw config value
 * (e.g. { is: "loot_world.+" } or plain "loot_world.+"). Returns null if the value is invalid.
 */
fun interface LootIndexConditionBuilder {
    fun build(value: Any?): LootIndexCondition?
}

object LootIndexConditionRegistry {
    private val registry = ConcurrentHashMap<String, LootIndexConditionBuilder>()

    fun register(type: String, builder: LootIndexConditionBuilder) {
        registry[type] = builder
    }

    /** Convenience registration: parses the value as a regex, then delegates to matcher */
    fun registerRegex(type: String, matcher: (Regex, LootLocation, Block, Player) -> Boolean) {
        register(type) { value ->
            val regex = parseIndexRegex(value) ?: return@register null
            LootIndexCondition { location, block, player -> matcher(regex, location, block, player) }
        }
    }

    fun get(type: String): LootIndexConditionBuilder? = registry[type]

    fun unregister(type: String) {
        registry.remove(type)
    }

    fun registeredTypes(): Set<String> = registry.keys
}

/** Parses regex config in the form { is: "..." } or plain "..." */
fun parseIndexRegex(value: Any?): Regex? {
    val raw = when (value) {
        is Map<*, *> -> value["is"]?.toString()
        is String -> value
        else -> null
    }
    return raw?.let { runCatching { it.toRegex() }.getOrNull() }
}

/** Registers built-in condition types; repeated calls are idempotent */
fun registerBuiltinIndexConditions() {
    LootIndexConditionRegistry.registerRegex("world") { regex, _, block, _ ->
        block.world.name.matches(regex)
    }
    LootIndexConditionRegistry.register("block") { value ->
        val regex = parseIndexRegex(value) ?: return@register null
        LootIndexCondition { _, block, _ ->
            // 原版材质名（向后兼容）
            if (block.type.name.matches(regex)) return@LootIndexCondition true
            // 正则不含冒号说明只匹配原版，跳过自定义方块查询以节省性能
            if (!regex.pattern.contains(":")) return@LootIndexCondition false
            // CE 等自定义方块：支持 craftengine:xxx（id 可能带命名空间，两种形式都匹配）
            val id = getBlockID(block)
            if (id.adapter == "minecraft") return@LootIndexCondition false
            "${id.adapter}:${id.block}".matches(regex) ||
                "${id.adapter}:${id.block.substringAfterLast(':')}".matches(regex)
        }
    }
    LootIndexConditionRegistry.registerRegex("wg_region") { regex, location, _, _ ->
        WorldGuardHelper.checkLocationRegion(location)?.any { it.matches(regex) } == true
    }
    LootIndexConditionRegistry.registerRegex("wg-region") { regex, location, _, _ ->
        WorldGuardHelper.checkLocationRegion(location)?.any { it.matches(regex) } == true
    }
    LootIndexConditionRegistry.register("multiblocks") { value ->
        val map = value as? Map<*, *> ?: return@register null
        val raw = map["detect"] ?: return@register null
        val lines = when (raw) {
            is String -> raw.lines()
            is List<*> -> raw.mapNotNull { it?.toString() }
            else -> return@register null
        }
        // 统一小写，大小写不敏感；支持 3 段（plugin:namespace:id）、2 段（minecraft:id）、1 段（裸 id）
        val entries = lines.mapNotNull { line ->
            line.trim().lowercase().takeIf { it.isNotBlank() }
        }.toSet()
        if (entries.isEmpty()) return@register null
        LootIndexCondition { _, block, _ ->
            getBlockIndexTriple(block).candidates().any { it in entries }
        }
    }
    LootIndexConditionRegistry.register("position") { value ->
        val map = value as? Map<*, *> ?: return@register null
        val worldRegex = parseIndexRegex(map["world"]) ?: return@register null
        val parts = map["in"]?.toString()?.trim()?.split(Regex("\\s+")) ?: return@register null
        if (parts.size != 6) return@register null
        val nums = parts.map { it.toDoubleOrNull() ?: return@register null }
        val (x1, y1, z1) = Triple(nums[0], nums[1], nums[2])
        val (x2, y2, z2) = Triple(nums[3], nums[4], nums[5])
        LootIndexCondition { _, block, _ ->
            block.world.name.matches(worldRegex) &&
                block.x >= minOf(x1, x2) && block.x <= maxOf(x1, x2) &&
                block.y >= minOf(y1, y2) && block.y <= maxOf(y1, y2) &&
                block.z >= minOf(z1, z2) && block.z <= maxOf(z1, z2)
        }
    }
    LootIndexConditionRegistry.register("condition") { value ->
        buildConditionFromRaw(value)
    }
    LootIndexConditionRegistry.register("conditions") { value ->
        buildConditionFromRaw(value)
    }
}

private fun buildConditionFromRaw(value: Any?): LootIndexCondition? {
    val condition = ConfigUtil.getConditions(mapOf("conditions" to value)) ?: return null
    return LootIndexCondition { _, block, player -> condition.validateExtra(block, player) }
}

fun Condition.validateExtra(block: Block, player: Player): Boolean {
    val extraVariable = mapOf<String, Any?>(
        "block" to block,
        "type" to block.type.name,
        "x" to block.x,
        "y" to block.y,
        "z" to block.z,
        "world" to block.world.name,
        "player" to player,
    )
    return validate(extraVariable, player)
}

/** 方块标识：原版为 plugin:id（无命名空间），CE 为 plugin:namespace:id */
private data class BlockIndexTriple(val plugin: String, val namespace: String?, val id: String) {
    /** 用于匹配的候选形式（统一小写）：CE 为 plugin:namespace:id；原版为 minecraft:id 和裸 id */
    fun candidates(): Set<String> {
        val pluginLower = plugin.lowercase()
        val idLower = id.lowercase()
        return if (namespace != null) {
            setOf("$pluginLower:${namespace.lowercase()}:$idLower")
        } else {
            setOf("$pluginLower:$idLower", idLower)
        }
    }
}

/** 原版方块为 minecraft:类型名（无命名空间）；CE 自定义方块为 craftengine:命名空间:id */
private fun getBlockIndexTriple(block: Block): BlockIndexTriple {
    val id = getBlockID(block)
    if (id.adapter == "minecraft") return BlockIndexTriple("minecraft", null, id.block.lowercase())
    val parts = id.block.split(":", limit = 2)
    return BlockIndexTriple(id.adapter, parts[0].lowercase(), parts.getOrElse(1) { parts[0] }.lowercase())
}

/** 替换 open 里的 %block_plugin%/%block_namespace%/%block_id% 占位符（不含 % 时零开销） */
private fun String.replaceIndexVariables(block: Block): String {
    if (!contains("%")) return this
    val triple = getBlockIndexTriple(block)
    return replace("%block_plugin%", triple.plugin)
        .replace("%block_namespace%", triple.namespace ?: "")
        .replace("%block_id%", triple.id)
}

/** Index resolution outcome: a matched rule either opens a template, passes (stop everything), or none (keep matching) */
sealed class LootIndexResult {
    /** A matched rule requested to stop all subsequent judgment; no loot is produced. */
    object Pass : LootIndexResult()
    /** A matched rule resolved to a loot template id. */
    data class Open(val templateId: String) : LootIndexResult()
    /** No match; continue evaluating the remaining rules. */
    object None : LootIndexResult()
}

/**
 * Index rule: when `if` matches, return `open`, or keep matching `then`;
 * when it does not match, match `else`. A rule without `if` always matches and can serve as a default.
 * A matched rule with `pass: true` stops all subsequent judgment (including nested then/else) and yields no loot.
 */
data class LootIndexRule(
    val checks: List<LootIndexCondition>,
    val open: String?,
    val thenRules: List<LootIndexRule>,
    val elseRules: List<LootIndexRule>,
    val pass: Boolean = false,
) {
    fun matches(location: LootLocation, block: Block, player: Player): Boolean {
        return checks.all { it.check(location, block, player) }
    }

    fun resolve(location: LootLocation, block: Block, player: Player): LootIndexResult {
        if (matches(location, block, player)) {
            if (pass) return LootIndexResult.Pass
            open?.let { return LootIndexResult.Open(it.replaceIndexVariables(block)) }
            for (rule in thenRules) {
                when (val result = rule.resolve(location, block, player)) {
                    LootIndexResult.Pass -> return LootIndexResult.Pass
                    is LootIndexResult.Open -> return result
                    LootIndexResult.None -> {}
                }
            }
            return LootIndexResult.None
        }
        for (rule in elseRules) {
            when (val result = rule.resolve(location, block, player)) {
                LootIndexResult.Pass -> return LootIndexResult.Pass
                is LootIndexResult.Open -> return result
                LootIndexResult.None -> {}
            }
        }
        return LootIndexResult.None
    }
}

/** Loot index: routes blocks to loot templates by world/block/WG region conditions */
data class LootIndex(
    val enabled: Boolean = false,
    val rules: List<LootIndexRule> = emptyList(),
) {
    /** Backward-compatible: pass and no-match both yield null */
    fun resolve(location: LootLocation, block: Block, player: Player): String? {
        return when (val result = resolveResult(location, block, player)) {
            is LootIndexResult.Open -> result.templateId
            else -> null
        }
    }

    fun resolveResult(location: LootLocation, block: Block, player: Player): LootIndexResult {
        if (!enabled) return LootIndexResult.None
        for (rule in rules) {
            when (val result = rule.resolve(location, block, player)) {
                is LootIndexResult.Open -> return result
                LootIndexResult.Pass -> return LootIndexResult.Pass
                LootIndexResult.None -> {}
            }
        }
        return LootIndexResult.None
    }
}
