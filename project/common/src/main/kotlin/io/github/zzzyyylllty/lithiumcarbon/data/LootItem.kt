package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.embiancomponent.EmbianComponent
import io.github.zzzyyylllty.lithiumcarbon.function.kether.evalKether
import io.github.zzzyyylllty.lithiumcarbon.function.kether.evalKetherValue
import io.github.zzzyyylllty.lithiumcarbon.function.kether.parseKether
import io.github.zzzyyylllty.lithiumcarbon.logger.severeL
import io.github.zzzyyylllty.lithiumcarbon.logger.warningL
import io.github.zzzyyylllty.lithiumcarbon.util.ExternalItemHelper
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

private val specialItemNamespace = listOf("minecraft", "mc", "vanilla")
val componentHelper by lazy { if (VersionHelper().isOrAbove12005()) EmbianComponent.SafetyComponentSetter else null }

data class LootItem(
    val source: String,
    val item: String,
    val parameters: LinkedHashMap<String, Any?>? = null,
    val components: LinkedHashMap<String, Any?>? = null,
    val amount: String? = "1",
) {
    fun build(player: Player?, overrideAmount: Int? = null): ItemStack {

        val amount = overrideAmount ?: amount.asNumberFormat(player).roundToInt()

//        val split = (if (namespaceID.contains("{")) namespaceID.parseKether(player, defaultData).split(":") else listOf("mc", "grass_block")).toMutableList()
//        val source = if (split.size >= 2) split.first().lowercase() else "mc"
//        split.removeFirst()
//        val item = split.joinToString(":")
        var itemStack: ItemStack?

        try {
            val providedItem = if (specialItemNamespace.contains(source)) {

                when (source) {
                    "mc", "minecraft", "vanilla" -> {
                        val parameters = (parameters ?: mapOf<String, Any?>()).toMutableMap()
                        parameters["material"] = item
                        XItemStack.deserialize(parameters)
                    }

                    else -> null
                }

            } else {
                if (player != null) {
                    ExternalItemHelper.itemBridge?.build(source, player, item)?.get()
                } else {
                    ExternalItemHelper.itemBridge?.build(source, item)?.get()
                }
            }
            if (providedItem == null) {
                severeL("ErrorItemGenerationFailedNull", source, item)
                return ItemStack(Material.GRASS_BLOCK)
            }
            itemStack = providedItem

        } catch (e: Exception) {
            severeL("ErrorItemGenerationFailed", source, item)
            e.printStackTrace()
            return ItemStack(Material.GRASS_BLOCK)
        }

        if (parameters?.isNotEmpty() ?: false) {

            parameters["name"] ?.let { itemStack.itemMeta.itemName(it.toString().toComponent()) }
            parameters["display-name"] ?.let { itemStack.itemMeta.displayName(it.toString().toComponent()) }
            parameters["custom-name"] ?.let { itemStack.itemMeta.customName(it.toString().toComponent()) }
            parameters["item-name"] ?.let { itemStack.itemMeta.itemName(it.toString().toComponent()) }
            (parameters["lore"] as List<String>?)?.let { itemStack.lore((it).toComponent()) }

        }
        if (components != null && components.isNotEmpty()) {
            if (VersionHelper().isOrAbove12005()) {
                var nmsStack = asNMSCopy(itemStack)
                components.forEach {
                    val value = it.value
                    if (value != null) componentUtil.setComponentNMS(nmsStack, it.key, value)?.let { nmsStack = it }
                    else componentUtil.removeComponentNMS(nmsStack, it.key).let { nmsStack = it }
                }
            } else {
                warningL("WarningNotSupportDataComponent")
            }
        }
        itemStack.amount = amount
        return itemStack
    }
}