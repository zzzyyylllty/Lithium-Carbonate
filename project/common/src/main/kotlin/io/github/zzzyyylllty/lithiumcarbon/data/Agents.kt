package io.github.zzzyyylllty.lithiumcarbon.data

import com.google.gson.Gson
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon
import io.github.zzzyyylllty.lithiumcarbon.api.LithiumCarbonAPI
import io.github.zzzyyylllty.sertraline.api.SertralineAPI
import io.github.zzzyyylllty.sertraline.event.SertralineCustomScriptDataLoadEvent
import io.github.zzzyyylllty.sertraline.function.javascript.EventUtil
import io.github.zzzyyylllty.sertraline.function.javascript.ItemStackUtil
import io.github.zzzyyylllty.sertraline.function.javascript.PlayerUtil
import io.github.zzzyyylllty.sertraline.function.javascript.ThreadUtil
import io.github.zzzyyylllty.sertraline.function.kether.evalKether
import io.github.zzzyyylllty.sertraline.util.data.DataUtil
import io.github.zzzyyylllty.sertraline.util.jsonUtils
import io.github.zzzyyylllty.sertraline.util.minimessage.mmJsonUtil
import io.github.zzzyyylllty.sertraline.util.minimessage.mmLegacyAmpersandUtil
import io.github.zzzyyylllty.sertraline.util.minimessage.mmLegacySectionUtil
import io.github.zzzyyylllty.sertraline.util.minimessage.mmUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import javax.script.CompiledScript
import javax.script.SimpleBindings

var defaultData = LinkedHashMap<String, Any?>()

@Awake(LifeCycle.ENABLE)
fun registerExternalData() {
    defaultData.putAll(
        linkedMapOf(
            "mmUtil" to mmUtil,
            "mmJsonUtil" to mmJsonUtil,
            "mmLegacySectionUtil" to mmLegacySectionUtil,
            "mmLegacyAmpersandUtil" to mmLegacyAmpersandUtil,
            "jsonUtils" to jsonUtils,
            "ItemStackUtil" to ItemStackUtil,
            "EventUtil" to EventUtil,
            "ThreadUtil" to ThreadUtil,
            "PlayerUtil" to PlayerUtil,
            "SertralineAPI" to LithiumCarbonAPI::class.java,
            "DataUtil" to DataUtil,
            "Math" to Math::class.java,
            "System" to System::class.java,
            "Bukkit" to Bukkit::class.java,
            "Gson" to Gson::class.java
        ))
    val event = SertralineCustomScriptDataLoadEvent(defaultData)
    event.call()
    defaultData = event.defaultData
}

data class Agents(
    val agents: LinkedHashMap<String, Agent>
) {
    fun runAgent(agent: String, extraVariables: Map<String, Any>, player: Player) {
        agents[agent]?.runAgent(extraVariables, player)
    }
}

data class Agent(
    val trigger: String,
    val js: CompiledScript? = null,
    val kether: List<String>? = null,
){
    fun runAgent(extraVariables: Map<String, Any>, player: Player) {
        val data = defaultData + extraVariables + mapOf("player" to player, "trigger" to trigger)
        js?.eval(SimpleBindings(data))
        kether?.evalKether(player, data)
    }
}