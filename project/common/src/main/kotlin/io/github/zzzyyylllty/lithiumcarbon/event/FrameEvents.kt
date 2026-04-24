package io.github.zzzyyylllty.lithiumcarbon.event

import io.github.zzzyyylllty.lithiumcarbon.data.FrameCrateConfig
import io.github.zzzyyylllty.lithiumcarbon.frame.FrameCrateData
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

/**
 * 展示框物资箱领取前事件（可取消）
 * 在给予物品和移除展示框之前触发
 */
class FrameCratePreClaimEvent(
    val frameCrateConfig: FrameCrateConfig,
    val frameCrateData: FrameCrateData,
    val player: Player,
    val entity: Entity,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()

/**
 * 展示框物资箱领取完成事件
 * 在物品已给予、展示框已移除后触发
 */
class FrameCrateClaimEvent(
    val frameCrateConfig: FrameCrateConfig,
    val frameCrateData: FrameCrateData,
    val player: Player,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()
