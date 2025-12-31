package io.github.zzzyyylllty.lithiumcarbon.util

import io.github.zzzyyylllty.lithiumcarbon.logger.severeL

data class WeightObject(
    val weight: Double,
    val obj: Any?
)

object WeightHelper {

    /**
     * 重复获取 repeat 次带权重的物品对象列表
     * @param weights: 物品列表，带权重
     * @param total: 权重总和
     * @param repeat: 重复获取次数
     * @return 选中对象列表
     */
    fun parse(weights: List<WeightObject>, total: Double, repeat: Int): List<Any?> {
        if (repeat < 1) {
            severeL("ErrorWeightRepeat")
        }
        if (total < 0) {
            severeL("ErrorWeightTotal")
        }
        if (weights.all { it.weight >= 0 }) {
            severeL("ErrorWeightNegative")
        }

        // 构造前缀权重
        val prefixSum = DoubleArray(weights.size)
        var sum = 0.0
        for (i in weights.indices) {
            sum += weights[i].weight
            // 如果 i 是最后一个元素，则用 total
            prefixSum[i] = if (i == weights.size - 1) total else sum
        }

        val result = ArrayList<Any?>(repeat)
        for (i in 0 until repeat) {
            val r = Math.random() * total  // 使用传入的 total
            val index = binarySearch(prefixSum, r)
            val obj = weights[index].obj
            result.add(obj)
        }

        return result
    }

    /**
     * 单次获取带权重的物品
     * @param weights: 物品列表，带权重
     * @param total: 权重总和
     * @return 选中对象
     */
    fun parse(weights: List<WeightObject>, total: Double): Any? {
        return parse(weights, total, 1).firstOrNull()
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