package io.github.zzzyyylllty.lithiumcarbon.unlock

data class UnlockLightConfig(
    val enabled: Boolean = true,
    val template: String? = null,
    val type: String? = null,
    val shared: Boolean = false,
    val onCompleteAction: String = "open",
    val sharedCompleteAction: String = "close",
    val overrides: Map<String, Any?> = emptyMap(),
)
