package io.github.zzzyyylllty.lithiumcarbon.unlock

import java.util.concurrent.ConcurrentHashMap

object UnlockFlowRegistry {
    private val registry = ConcurrentHashMap<String, UnlockFlow>()

    fun register(flow: UnlockFlow) {
        registry[flow.typeId] = flow
    }

    fun get(typeId: String): UnlockFlow? = registry[typeId]

    fun unregister(typeId: String) {
        registry.remove(typeId)
    }

    fun cleanupAll() {
        registry.values.forEach { it.cleanup() }
    }
}
