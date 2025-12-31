package io.github.zzzyyylllty.lithiumcarbon.util

import io.github.zzzyyylllty.lithiumcarbon.data.Loots
import io.github.zzzyyylllty.lithiumcarbon.logger.severeL
import org.bukkit.entity.Player

object WeightHelper {

    /**
     * 重复获取 repeat 次带权重的物品对象列表
     * @param weights: 物品列表，带权重
     * @param total: 权重总和
     * @param repeat: 重复获取次数
     * @return 选中对象列表
     */
    fun parse(weights: List<Loots>, total: Double, repeat: Int, player: Player): List<Loots?> {
        if (repeat < 1) {
            severeL("ErrorWeightRepeat")
        }
        if (total < 0) {
            severeL("ErrorWeightTotal")
        }

        // 构造前缀权重
        val prefixSum = DoubleArray(weights.size)
        var sum = 0.0
        for (i in weights.indices) {
            val weight = weights[i].getWeight(player)
            if (weight < 0) {
                severeL("ErrorWeightNegative")
                return emptyList()
            }
            sum += weight
            // 如果 i 是最后一个元素，则用 total
            prefixSum[i] = if (i == weights.size - 1) total else sum
        }

        val result = ArrayList<Loots?>(repeat)
        for (i in 0 until repeat) {
            val r = Math.random() * total  // 使用传入的 total
            val index = binarySearch(prefixSum, r)
            val obj = weights[index]
            result.add(obj)
        }

        return result
    }
    /**
     * 二分查找prefixSum中第一个 > target的索引
     */
    private fun binarySearch(prefixSum: DoubleArray, target: Double): Int {
        var left = 0
        var right = prefixSum.size - 1
        while (left < right) {
            val mid = left + (right - left) / 2
            if (prefixSum[mid] > target) right = mid
            else left = mid + 1
        }
        return left
    }
}