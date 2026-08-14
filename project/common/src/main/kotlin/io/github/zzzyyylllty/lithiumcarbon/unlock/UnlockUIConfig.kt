package io.github.zzzyyylllty.lithiumcarbon.unlock

import io.github.zzzyyylllty.lithiumcarbon.data.Agents
import io.github.zzzyyylllty.lithiumcarbon.data.LootItem

data class UnlockUIConfig(
    val id: String,
    val type: String,
    val title: String,
    val rows: Int,
    val layout: List<String>,
    val items: Map<Char, LootItem>,
    val bruteForce: BruteForceConfig? = null,
    val password: PasswordConfig? = null,
    val decipher: DecipherConfig? = null,
    val speed: SpeedConfig? = null,
    val timer: TimerConfig? = null,
    val agents: Agents? = null,
    val info: TutorialInfo? = null,
)

/**
 * 教程/说明物品：在解锁GUI中展示如何操作的提示
 */
data class TutorialInfo(
    val slot: Int,
    val item: LootItem,
)

data class BruteForceConfig(
    val clicks: String,
    val progressItem: LootItem,
    val completeItem: LootItem,
    val soundProgress: String? = null,
    val soundComplete: String? = null,
)

data class PasswordConfig(
    val password: String,
    val maxAttempts: Int = 3,
    val inputItem: LootItem,
    val attemptItem: LootItem,
    val maskInput: Boolean = false,
    val soundCorrect: String? = null,
    val soundWrong: String? = null,
)

data class DecipherConfig(
    val rounds: Int = 3,
    val items: List<LootItem>,
    val targetSlot: Int,
    val targetSlots: List<Int>? = null,
    val targetItemIndex: Int = 0,
    val rotationSpeed: Int = 10,
    val decipherButtonSlot: Int,
    val decipherButtonItem: LootItem,
    val successItem: LootItem,
    val failItem: LootItem,
    val soundRotate: String? = null,
    val soundSuccess: String? = null,
    val soundFail: String? = null,
)

data class SpeedConfig(
    val totalTargets: Int = 10,
    val spawnInterval: Int = 30,
    val targetItem: LootItem,
    val distractorItem: LootItem? = null,
    val penaltyOnDistractor: Boolean = true,
    val penaltyAction: String = "reset",
    val timePenaltySeconds: Int = 5,
    val timeLimit: Int? = null,
    val soundTarget: String? = null,
    val soundDistractor: String? = null,
    val soundComplete: String? = null,
)

data class TimerConfig(
    val duration: String,
    val displayType: String = "gui",
    val bossBarColor: String? = null,
    val progressItem: LootItem,
    val completeItem: LootItem,
    val soundTick: String? = null,
    val soundComplete: String? = null,
    val cancelOnMove: Boolean = false,
)
