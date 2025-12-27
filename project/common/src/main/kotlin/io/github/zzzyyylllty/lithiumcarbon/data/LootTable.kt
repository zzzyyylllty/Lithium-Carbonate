package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.LinkedList

data class LootTable(
    val pools: List<LootPool>,
    val agent: Agents?,
) {
    fun apply(bypassConditions: Boolean = false, extraVariables: Map<String, Any>,player: Player): List<LootReturn> {
        val returns = mutableListOf<LootReturn>()
        pools.forEach { pool ->
            pool.roll(bypassConditions, extraVariables, player)?.let { returns.add(it) }
        }
        return returns
    }
}

data class LootPool(
    val rolls: String,
    val conditions: Condition?,
    val loots: List<Loots>,
) {
    fun roll(bypassConditions: Boolean = false, extraVariables: Map<String, Any>, player: Player): LootReturn? {

        // 不满足条件直接返回
        if (!bypassConditions) if (conditions?.validate(extraVariables, player) == false) {
            devLog("Loot Pool condition is not valid. return null.")
            return null
        }

        loots.forEach {
            it.parseLoot()
        }

    }
}

data class Loots(
    val type: LootType,
    val parameters: LinkedHashMap<String, Any>
) {
    fun parseLoot(): LootType {}
}

enum class LootType {
    EXP,
    ITEM,
    KETHER,
    JAVASCRIPT
}