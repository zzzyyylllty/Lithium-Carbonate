package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.config
import io.github.zzzyyylllty.lithiumcarbon.event.LootItemGrantEvent
import io.github.zzzyyylllty.lithiumcarbon.function.kether.evalKether
import io.github.zzzyyylllty.lithiumcarbon.function.player.sendComponent
import io.github.zzzyyylllty.lithiumcarbon.util.LootGUIHelper
import io.github.zzzyyylllty.lithiumcarbon.util.asNumberFormat
import io.github.zzzyyylllty.lithiumcarbon.util.mmUtil
import io.github.zzzyyylllty.lithiumcarbon.util.toComponent
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.platform.util.asLangText
import taboolib.platform.util.giveItem
import javax.script.CompiledScript
import javax.script.SimpleBindings
import kotlin.math.roundToInt

data class LootElement(
    var displayItem: LootItem? = null,
    val exps: Double? = 0.0,
    var items: List<LootItem>? = null,
    val kether: List<String>? = null,
    val javaScript: CompiledScript? = null,
    val searchTime: Double = 0.0,
    val skipSearch: Boolean = false,
) {
    // 首次显示时锁定各物品的数量（按 items 下标），之后显示和发放都用同一个值
    var decidedAmounts: MutableList<Int>? = null
    // displayItem 独立于 items 时的数量锁定
    var decidedDisplayAmount: Int? = null

    fun decideAmount(index: Int, player: Player?): Int {
        val list = decidedAmounts ?: mutableListOf<Int>().also { decidedAmounts = it }
        while (list.size <= index) list.add(-1)
        if (list[index] < 0) {
            val item = items?.getOrNull(index)
            list[index] = item?.amount?.asNumberFormat(player)?.roundToInt() ?: 1
        }
        return list[index]
    }

    fun decideDisplayAmount(item: LootItem, player: Player?): Int {
        return decidedDisplayAmount ?: (item.amount?.asNumberFormat(player)?.roundToInt() ?: 1).also { decidedDisplayAmount = it }
    }

    fun getDisplayItem(stat: LootElementStat, player: Player?, options: LootTemplateOptions): ItemStack? {
        return when (stat) {
            LootElementStat.NOT_SEARCHED -> LootGUIHelper.unsearch.build(player, 1)
            LootElementStat.SEARCHING -> LootGUIHelper.searching.build(player, 1)
            LootElementStat.SEARCHED -> {
                val firstItem = items?.firstOrNull()
                val display = (displayItem ?: firstItem ?: LootGUIHelper.undefinedItem)
                val amount = if (displayItem != null) decideDisplayAmount(display, player) else decideAmount(0, player)
                val built = display.build(player, amount)
                // 应用lore修改到独立的物品堆，不修改共享的LootItem.parameters
                if (options.removeLore || !options.addLore.isNullOrEmpty()) {
                    val meta = built.itemMeta
                    if (meta != null) {
                        if (options.removeLore) {
                            meta.lore(listOf())
                        }
                        if (!options.addLore.isNullOrEmpty()) {
                            meta.lore((meta.lore() ?: listOf()) + options.addLore.map { it.toComponent() })
                        }
                        built.setItemMeta(meta)
                    }
                }
                built
            }
            LootElementStat.NOITEM -> null
        }
    }

    fun applyToPlayer(player: Player,template: LootTemplate) {
        items?.forEachIndexed { index, lItem ->
            val event = LootItemGrantEvent(player, template, this, lItem.build(player, decideAmount(index, player)))
            event.call()
            if (!event.isCancelled) {
                player.giveItem(event.item)
                if (config.getBoolean("message.Claim")) player.sendComponent(
                    player.asLangText(
                        "Claim",
                        template.name,
                        mmUtil.serialize(event.item.displayName())
                    )
                )
            }
        }
        val exp = exps?.roundToInt()
        if (exp != null && exp != 0) {
            player.giveExp(exp)
            if (config.getBoolean("message.ClaimExp")) player.sendComponent(player.asLangText("ClaimExp", template.name, exp))
        }
        if (kether != null || javaScript != null) {
            val data = defaultData.toMutableMap()
            data.putAll(
                linkedMapOf(
                    "displayItem" to displayItem,
                    "exps" to exps,
                    "element" to this,
                    "searchTime" to searchTime,
                    "skipSearch" to skipSearch,
                    "player" to player,
                )
            )
            kether?.evalKether(player, data)
            javaScript?.eval(SimpleBindings(data))
        }
    }

}

enum class LootElementStat {
    NOT_SEARCHED,
    SEARCHING,
    SEARCHED,
    NOITEM
}