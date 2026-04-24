package io.github.zzzyyylllty.lithiumcarbon.event

import io.github.zzzyyylllty.lithiumcarbon.cardroom.CardRoomConfig
import io.github.zzzyyylllty.lithiumcarbon.cardroom.CardRoomInstance
import org.bukkit.block.Block
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

/**
 * 卡房开启前事件（可取消）
 * 在消耗钥匙和执行动作之前触发
 */
class CardRoomPreOpenEvent(
    val config: CardRoomConfig,
    val player: Player,
    val block: Block,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()

/**
 * 卡房开启完成事件
 * 在所有动作执行完毕后触发
 */
class CardRoomOpenEvent(
    val config: CardRoomConfig,
    val player: Player,
    val instance: CardRoomInstance,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

/**
 * 卡房重置前事件（可取消）
 * 在重置动作执行之前触发
 */
class CardRoomPreResetEvent(
    val config: CardRoomConfig,
    val instance: CardRoomInstance,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()

/**
 * 卡房重置完成事件
 * 在重置动作全部执行完毕后触发
 */
class CardRoomResetEvent(
    val config: CardRoomConfig,
    val instance: CardRoomInstance,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()
