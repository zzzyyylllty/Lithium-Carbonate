package io.github.zzzyyylllty.lithiumcarbon.event

import taboolib.platform.type.BukkitProxyEvent

class LithiumCarbonReloadEvent() : BukkitProxyEvent()

/**
 * Modify [defaultData] you can register
 * your custom utils.
 * DO NOT USE clear, re-set or directly modify it, OR OTHER SENSITIVE FUNCTIONS.
 * */
class LithiumCarbonCustomScriptDataLoadEvent(
    var defaultData: LinkedHashMap<String, Any?>
) : BukkitProxyEvent()
