package io.github.zzzyyylllty.lithiumcarbon.util.serialize

import java.util.UUID

fun String.toUUID(): UUID {
    return UUID.fromString(this)
}
fun String.generateUUID(): UUID {
    return UUID.nameUUIDFromBytes(this.toByteArray())
}

fun List<String>.generateUUID(): UUID {
    return UUID.nameUUIDFromBytes(this.joinToString("\n").toByteArray())
}
