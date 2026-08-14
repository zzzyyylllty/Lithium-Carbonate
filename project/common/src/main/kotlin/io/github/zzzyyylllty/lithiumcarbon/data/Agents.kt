package io.github.zzzyyylllty.lithiumcarbon.data

import com.google.gson.Gson
import io.github.zzzyyylllty.lithiumcarbon.api.LithiumCarbonAPI
import io.github.zzzyyylllty.lithiumcarbon.event.LithiumCarbonCustomScriptDataLoadEvent
import io.github.zzzyyylllty.lithiumcarbon.function.javascript.EventUtil
import io.github.zzzyyylllty.lithiumcarbon.function.javascript.ItemStackUtil
import io.github.zzzyyylllty.lithiumcarbon.function.javascript.PlayerUtil
import io.github.zzzyyylllty.lithiumcarbon.function.javascript.ThreadUtil
import io.github.zzzyyylllty.lithiumcarbon.function.kether.evalKether
import io.github.zzzyyylllty.lithiumcarbon.util.CraftEngineHelper
import io.github.zzzyyylllty.lithiumcarbon.util.DependencyHelper
//import io.github.zzzyyylllty.lithiumcarbon.util.jsonUtils
import io.github.zzzyyylllty.lithiumcarbon.util.mmJsonUtil
import io.github.zzzyyylllty.lithiumcarbon.util.mmLegacyAmpersandUtil
import io.github.zzzyyylllty.lithiumcarbon.util.mmLegacySectionUtil
import io.github.zzzyyylllty.lithiumcarbon.util.mmUtil
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.bukkit.util.BlockStateUtils
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.plugin.CraftEngine
import net.momirealms.craftengine.core.world.BlockPos
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import java.util.UUID
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
//            "jsonUtils" to jsonUtils,
            "ItemStackUtil" to ItemStackUtil,
            "EventUtil" to EventUtil,
            "ThreadUtil" to ThreadUtil,
            "PlayerUtil" to PlayerUtil,
            "LithiumCarbonAPI" to LithiumCarbonAPI::class.java,
            "Math" to Math::class.java,
            "System" to System::class.java,
            "Bukkit" to Bukkit::class.java,
            "Gson" to Gson::class.java
        ))
    if (DependencyHelper.ce) {
        defaultData.putAll(
            linkedMapOf(
                "CEInstance" to CraftEngine.instance(),
                "CraftEngineBlocks" to object {
                    fun getCustomBlockState(block: org.bukkit.block.Block) =
                        CraftEngineBlocks.getCustomBlockState(block)
                    fun withProperty(state: ImmutableBlockState, propertyName: String, value: String): ImmutableBlockState? {
                        val wrapper = state.customBlockState().withProperty(propertyName, value)
                        // Convert BlockStateWrapper back to ImmutableBlockState
                        return BlockStateUtils.getOptionalCustomBlockState(wrapper.minecraftState()).orElse(null)
                    }
                },
                "CraftEngineItems" to object {
                    fun byId(id: String) = CraftEngineItems.byId(id)
                    fun byItemStack(itemStack: org.bukkit.inventory.ItemStack) =
                        CraftEngineItems.byItemStack(itemStack)
                    fun isCustomItem(itemStack: org.bukkit.inventory.ItemStack) =
                        CraftEngineItems.isCustomItem(itemStack)
                },
                "CraftEngineFurniture" to CraftEngineFurniture::class.java,
                "BlockPos" to object {
                    fun create(x: Int, y: Int, z: Int) =
                        net.momirealms.craftengine.core.world.BlockPos(x, y, z)
                },
                "CraftEngineHelper" to CraftEngineHelper
            ))
//
//        CraftEngine.instance().worldManager().getWorld(UUID.randomUUID()).setBlockStateAtIfLoaded(BlockP)
    }
    val event = LithiumCarbonCustomScriptDataLoadEvent(defaultData)
    event.call()
    defaultData = event.defaultData
}

data class Agents(
    val agents: LinkedHashMap<String, Agent>
) {
    fun runAgent(agent: String, extraVariables: Map<String, Any?>, player: Player?) {
        agents[agent]?.runAgent(extraVariables, player)
    }
    fun hasAction(agent: String): Boolean = agents.containsKey(agent)
}

data class Agent(
    val trigger: String,
    val js: CompiledScript? = null,
    val asyncJs: CompiledScript? = null,
    val asyncKe: List<String>? = null,
    val kether: List<String>? = null,
){
    fun runAgent(extraVariables: Map<String, Any?>, player: Player?) {
        val data = defaultData + extraVariables + mapOf("player" to player, "trigger" to trigger)
        js?.let {
            submit {
                it.eval(SimpleBindings(data))
            }
        }
        kether?.evalKether(player, data)
        asyncJs?.let {
            submitAsync {
                it.eval(SimpleBindings(data))
            }
        }
        asyncKe?.let {
            submitAsync {
                it.evalKether(player, data)
            }
        }
    }
}