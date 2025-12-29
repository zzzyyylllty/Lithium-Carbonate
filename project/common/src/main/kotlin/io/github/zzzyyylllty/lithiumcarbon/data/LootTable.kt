package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.LinkedList

data class LootTable(
    val pools: List<LootPool>,
    val agent: Agents?,
) {
    fun apply(bypassConditions: Boolean = false, extraVariables: Map<String, Any>,player: Player) {
    }
}

data class LootPool(
    val rolls: String,
    val conditions: Condition?,
    val loots: List<Loots>,
    val agent: Agents?,
) {
    fun roll(bypassConditions: Boolean = false, extraVariables: Map<String, Any>, player: Player) {

        // 不满足条件直接返回
        if (!bypassConditions) if (conditions?.validate(extraVariables, player) == false) {
            devLog("Loot Pool condition not met, return.")
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