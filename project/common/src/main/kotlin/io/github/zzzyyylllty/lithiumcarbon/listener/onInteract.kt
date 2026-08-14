package io.github.zzzyyylllty.lithiumcarbon.listener

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.allowedWorlds
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.config
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootCaches
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootDefines
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootIndex
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.LootInstanceKey
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootMap
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.lootTemplates
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.weightSystem
import io.github.zzzyyylllty.lithiumcarbon.cardroom.CardRoomManager
import io.github.zzzyyylllty.lithiumcarbon.data.LocationHelper
import io.github.zzzyyylllty.lithiumcarbon.data.LootInstance
import io.github.zzzyyylllty.lithiumcarbon.data.LootLocation
import io.github.zzzyyylllty.lithiumcarbon.data.LootTemplate
import io.github.zzzyyylllty.lithiumcarbon.data.index.LootIndexResult
import io.github.zzzyyylllty.lithiumcarbon.data.define.getMaxMatchingWeight
import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.unlockUIConfigs
import io.github.zzzyyylllty.lithiumcarbon.event.UnlockFlowPreStartEvent
import io.github.zzzyyylllty.lithiumcarbon.gui.openLootChest
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockContext
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockFlowRegistry
import io.github.zzzyyylllty.lithiumcarbon.unlock.UnlockStateManager
import io.github.zzzyyylllty.lithiumcarbon.logger.warningL
import io.github.zzzyyylllty.lithiumcarbon.util.devLog
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.platform.util.isMainhand

@SubscribeEvent
fun onInteract(e: PlayerInteractEvent) {
    if (e.action != Action.RIGHT_CLICK_BLOCK) return
    if (!e.isMainhand()) return
    val block = e.clickedBlock ?: return

//    var currentState = CraftEngineBlocks.getCustomBlockState(block);
//    if (currentState != null) {
//        var stateWrapper = currentState.customBlockState();
//        var newStateWrapper = stateWrapper.withProperty("open", "true");
//        var newState = newStateWrapper;
//        var worldManager = CraftEngine.instance().worldManager();
//        var ceWorld = worldManager.getWorld(block.getWorld().uid);
//        var pos = BlockPos(block.getX(), block.getY(), block.getZ());
//        CraftEngineBlocks.
//        var success = ceWorld.setBlockStateAtIfLoaded(pos, newState.);
//    }


    if (block.type.isAir) return
    val player = e.player ?: return // 防止 NPC 搞鬼

    // 检查事件是否已被取消（例如被卡房系统取消）
    if (e.isCancelled) return

    // 检查是否为卡房触发位置（如果是，让卡房系统处理）
    val cardRoomConfig = CardRoomManager.getCardRoomAtLocation(block.location)
    if (cardRoomConfig != null) {
        // 卡房系统会处理这个位置，我们不处理
        return
    }
    if (config.getBoolean("allowed-all-blocks", false) || config.getList("allowed-blocks")?.contains(block.type.name) ?: false) {

        if (!config.getBoolean("allowed-all-worlds", false)) {
            val world = block.world.name
            var passed = false
            for (regex in allowedWorlds) {
                if (world.matches(regex)) {
                    passed = true
                    break
                }
            }
            if (!passed) return
        }

        val location = LocationHelper.toLootLocation(block.location)
        val define =
//            if (DependencyHelper.wg) {
//                player ?: getDefines(location, block, player)
//            }
//            else
                getDefines(location, block, player)
            ?: run {
            devLog("Define is null, return.")
            return
        }
        e.isCancelled = true
        submit {
            val key = if (define.options.private) LootInstanceKey(location, player.uniqueId) else LootInstanceKey(location, null)
            // 当前战利品
            val current = lootMap[key]

            // 更新后的战利品
            lateinit var instance: LootInstance
            if (current == null) {
                devLog("CURRENT LootInstance is null, regenerating.")
                instance = lootMap.getOrPut(key) {
                    define.createInstance(block, player)
                }
            } else {
                val pendingInstance = current.checkUpdate()
                instance = pendingInstance ?: run {
                    lootMap.getOrPut(key) {
                        define.createInstance(block, player)
                    }
                }
            }

            val unlockConfig = define.unlock
            if (unlockConfig != null && unlockConfig.enabled) {
                val uiId = unlockConfig.template ?: run { player.openLootChest(instance, e); return@submit }
                val uiConfig = unlockUIConfigs[uiId] ?: run { player.openLootChest(instance, e); return@submit }
                val flowType = unlockConfig.type ?: uiConfig.type
                val flow = UnlockFlowRegistry.get(flowType) ?: run { player.openLootChest(instance, e); return@submit }

                if (UnlockStateManager.getActive(player) != null) return@submit

                val context = UnlockContext(
                    player = player,
                    block = block,
                    event = e,
                    lootTemplate = define,
                    lootInstance = instance,
                    uiConfig = uiConfig,
                    lightConfig = unlockConfig,
                    onSuccess = { inst -> player.openLootChest(inst, e) },
                    onFail = { reason -> devLog("Unlock failed for ${player.name}: $reason") },
                )

                val preEvent = UnlockFlowPreStartEvent(player, context)
                preEvent.call()
                if (preEvent.isCancelled) return@submit

                flow.start(context)
            } else {
                player.openLootChest(instance, e)
            }
        }
    } else {
        return
    }
}

/**
 * 权重系统：找出所有匹配的模板，按 define 中的最高权重分组，返回权重最高的模板列表。
 * 权重相等时全部返回（由调用方决定如何混合）。
 */
fun resolveTemplatesByWeight(location: LootLocation, block: Block, player: Player): List<LootTemplate> {
    val matches = mutableListOf<Pair<Int, LootTemplate>>()
    for ((id, defines) in lootDefines) {
        val weight = defines.getMaxMatchingWeight(location, block, player) ?: continue
        matches.add(weight to (lootTemplates[id] ?: continue))
    }
    if (matches.isEmpty()) return emptyList()
    val maxWeight = matches.maxOf { it.first }
    return matches.filter { it.first == maxWeight }.map { it.second }
}

fun getDefines(location: LootLocation, block: Block, player: Player): LootTemplate? {
    // Loot index routing takes priority: use the indexed template directly, fall back to defines matching
    val index = lootIndex
    if (index != null && index.enabled) {
        when (val result = index.resolveResult(location, block, player)) {
            is LootIndexResult.Open -> {
                val template = lootTemplates[result.templateId]
                if (template != null) {
                    devLog("Loot index matched template: ${result.templateId}")
                    return template
                }
                warningL("LootIndexTemplateNotFound", result.templateId)
            }
            // pass 命中：直接结束，不产生任何战利品
            LootIndexResult.Pass -> return null
            LootIndexResult.None -> {}
        }
    }
    if (weightSystem) {
        // 权重系统启用：从缓存读取，或按权重解析并缓存
        val cached = lootCaches[location]
        if (cached != null) return cached

        val templates = resolveTemplatesByWeight(location, block, player)
        if (templates.isEmpty()) return null

        // 权重相等时随机选取一个，实现"混合刷新"
        val winner = if (templates.size == 1) templates[0] else templates.random()
        lootCaches[location] = winner
        return winner
    }

    return if (lootCaches[location] == null) {
        val define = getDefinesWithoutCache(location, block, player)
        define?.let { lootCaches[location] = it }
        define
    } else {
        lootCaches[location]
    }
}

fun getDefinesWithoutCache(location: LootLocation, block: Block, player: Player): LootTemplate? {
    for (it in lootDefines) {
        if (it.value.isValidLocation(location, block, player)) return lootTemplates[it.key]
    }
    return null
}