package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.projectunified.uniitem.all.AllItemProvider
import io.github.projectunified.uniitem.api.ItemKey
import io.github.zzzyyylllty.embiancomponent.EmbianComponent
import io.github.zzzyyylllty.lithiumcarbon.function.kether.evalKether
import io.github.zzzyyylllty.lithiumcarbon.function.kether.evalKetherValue
import io.github.zzzyyylllty.lithiumcarbon.function.kether.parseKether
import io.github.zzzyyylllty.lithiumcarbon.util.SertralineHelper
import io.github.zzzyyylllty.lithiumcarbon.util.VersionHelper
import io.github.zzzyyylllty.lithiumcarbon.util.asNumberFormat
import io.github.zzzyyylllty.lithiumcarbon.util.componentUtil
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import io.github.zzzyyylllty.lithiumcarbon.util.minimessage.toComponent
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.function.severe
import taboolib.common.util.random
import taboolib.library.xseries.XItemStack
import taboolib.module.nms.NMSItemTag.Companion.asNMSCopy
import kotlin.math.roundToInt

private val specialItemNamespace = listOf("minecraft", "mc")
val provider: AllItemProvider by lazy { AllItemProvider() }
val componentHelper by lazy { if (VersionHelper().isOrAbove12005()) EmbianComponent.SafetyComponentSetter else null }

data class LootItem(
    val source: String?,
    val item: String,
    val parameters: LinkedHashMap<String, Any?>,
    val components: LinkedHashMap<String, Any?>? = null,
    val amount: String? = "1",
) {
    fun build(player: Player?, overrideAmount: Int? = null): ItemStack {

        val amount = overrideAmount ?: amount.asNumberFormat(player).roundToInt()

        val source = source?.lowercase() ?: "mc"
        var itemStack: ItemStack = ItemStack(Material.GRASS_BLOCK)

        try {
            itemStack = if (specialItemNamespace.contains(source)) {

                when (source) {
                    "mc", "minecraft", "vanilla" -> {
                        XItemStack.deserialize(parameters)
                    }

                    "sertraline", "sql" -> {
                        SertralineHelper.buildItem(item, player, amount)
                    }

                    else -> throw IllegalArgumentException("Unsupported Special LootItem format: $source-$item")
                }

            } else {
                player?.let { provider.item(ItemKey(source, item), it) } ?: provider.item(ItemKey(source, item))
            } ?: ItemStack(Material.GRASS_BLOCK)

        } catch (e: Exception) {
            severe("An error occurred while parsing Loot Item: $source:$item. See error below.")
            e.printStackTrace()
        }

        if (parameters.isNotEmpty()) {

            parameters["name"] ?.let { itemStack.itemMeta.displayName(it.toString().toComponent()) }
            parameters["display_name"] ?.let { itemStack.itemMeta.displayName(it.toString().toComponent()) }
            parameters["custom_name"] ?.let { itemStack.itemMeta.customName(it.toString().toComponent()) }
            parameters["item_name"] ?.let { itemStack.itemMeta.itemName(it.toString().toComponent()) }
            parameters["lore"] ?.let { itemStack.lore((it as List<String>).toComponent()) }

        }
        if (components != null && components.isNotEmpty()) {
//            if (componentHelper != null) {
            var nmsStack = asNMSCopy(itemStack)
            components.forEach {
                val value = it.value
                if (value != null) componentUtil.setComponentNMS(nmsStack, it.key, value)?.let { nmsStack = it }
                else componentUtil.removeComponentNMS(nmsStack, it.key).let { nmsStack = it }
            }
//            } else {
//                warning("Component helper is null. is version below 1.20.5? if below, remove component section in your loot config to hide this warning.If not below, please open an issue.")
//            }
        }
        itemStack.amount = amount
        return itemStack
    }
}