package io.github.zzzyyylllty.lithiumcarbon.util

import org.bukkit.inventory.ItemStack
import taboolib.module.nms.ItemTag
import taboolib.module.nms.ItemTagData
import taboolib.module.nms.ItemTagType
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.getI18nName
import taboolib.module.nms.getItemTag

/**
 * ItemTagUtil扩展，用于卡房系统的NBT标签检测
 */
object ItemTagUtilExtensions {

    /**
     * 检查物品是否包含指定的NBT标签
     *
     * @param item 要检查的物品
     * @param requiredTag 要求的NBT标签映射
     * @return 如果物品包含所有要求的标签则返回true
     */
    fun hasItemTag(item: ItemStack?, requiredTag: Map<String, Any>?): Boolean {
        if (item == null || requiredTag == null || requiredTag.isEmpty()) {
            return true // 如果没有要求标签，总是返回true
        }

        // 获取物品的NBT标签
        val itemTag = item?.getItemTag() as? ItemTagData ?: return false

        // 递归检查标签
        return checkTagMatch(itemTag, requiredTag)
    }

    /**
     * 递归检查标签是否匹配
     */
    private fun checkTagMatch(actualTag: ItemTagData, requiredTag: Map<String, Any>): Boolean {
        // 将实际的NBT标签转换为Map
        val actualMap = try {
            ItemTagUtil.run { actualTag.parseMapNBT() }
        } catch (e: Exception) {
            return false
        }
        return checkMapMatch(actualMap, requiredTag)
    }

    private fun checkMapMatch(actualMap: Map<String, Any?>, requiredMap: Map<String, Any>): Boolean {
        // 检查所有要求的标签
        for ((key, requiredValue) in requiredMap) {
            val actualValue = actualMap[key] ?: return false
            if (!checkValueMatch(actualValue, requiredValue)) return false
        }
        return true
    }

    private fun checkValueMatch(actual: Any?, required: Any): Boolean {
        return when (required) {
            is Map<*, *> -> {
                // 嵌套标签检测
                val nestedActualMap = when (actual) {
                    is ItemTagData -> {
                        try {
                            ItemTagUtil.run { actual.parseMapNBT() }
                        } catch (e: Exception) {
                            return false
                        }
                    }
                    is Map<*, *> -> actual as Map<String, Any?>
                    else -> return false
                }
                checkMapMatch(nestedActualMap, required.mapValues { it.value!! } as Map<String, Any>)
            }
            is List<*> -> {
                // 列表检测（顺序敏感）
                val actualList = actual as? List<*> ?: return false
                if (actualList.size != required.size) return false
                for (i in actualList.indices) {
                    val requiredElem = required[i] ?: return false
                    if (!checkValueMatch(actualList[i], requiredElem)) return false
                }
                true
            }
            else -> areValuesEqual(actual, required)
        }
    }

    /**
     * 比较两个值是否相等
     */
    private fun areValuesEqual(actual: Any?, required: Any?): Boolean {
        return when (required) {
            is String -> actual.toString() == required
            is Number -> {
                when (actual) {
                    is Number -> actual.toDouble() == required.toDouble()
                    is String -> actual.toDoubleOrNull() == required.toDouble()
                    else -> false
                }
            }
            is Boolean -> {
                // 在Minecraft NBT中，布尔值通常存储为字节（1=true, 0=false）
                when (actual) {
                    is Boolean -> actual == required
                    is Byte -> (actual == 1.toByte()) == required
                    is Number -> (actual.toInt() == 1) == required
                    is String -> actual.toBooleanStrictOrNull() == required
                    else -> false
                }
            }
            else -> actual == required
        }
    }

    /**
     * 获取物品的显示名称（包含NBT信息）
     */
    fun getItemDisplayInfo(item: ItemStack): String {
        val builder = StringBuilder()
        builder.append("Item: ${item.type}")
        builder.append(", Amount: ${item.amount}")

        val itemTag = item.getItemTag() as? ItemTagData
        if (itemTag != null) {
            try {
                val tagMap = ItemTagUtil.run { itemTag.parseMapNBT() }
                if (tagMap.isNotEmpty()) {
                    builder.append(", NBT: ${tagMap.keys}")
                }
            } catch (e: Exception) {
                // 忽略解析错误
            }
        }

        return builder.toString()
    }
}