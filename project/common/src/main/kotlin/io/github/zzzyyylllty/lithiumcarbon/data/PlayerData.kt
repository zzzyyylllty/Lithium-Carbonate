package io.github.zzzyyylllty.lithiumcarbon.data

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon.config
import taboolib.expansion.Id
import taboolib.expansion.Key
import taboolib.expansion.Length
import taboolib.expansion.UniqueKey
import java.time.Instant
import kotlin.math.round

data class PlayerData (
    @Key val playerName: String,
    @Id @UniqueKey @Length(32) val uuid: String,
) {
}
