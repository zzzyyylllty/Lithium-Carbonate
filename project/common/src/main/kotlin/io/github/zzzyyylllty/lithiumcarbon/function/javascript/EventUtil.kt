package io.github.zzzyyylllty.lithiumcarbon.function.javascript

import io.github.zzzyyylllty.sertraline.logger.severeS
import org.bukkit.event.Cancellable
import org.bukkit.event.Event

object EventUtil {
    fun cancel(event: Cancellable,cancel: Boolean = true) {
        try {
            event.isCancelled = cancel
        } catch (e: Exception) {
            severeS("An error occurred while trying to cancel event $event")
            e.printStackTrace()
        }
    }
    fun call(event: Event) {
        try {
            event.callEvent()
        } catch (e: Exception) {
            severeS("An error occurred while trying to calling event $event")
            e.printStackTrace()
        }
    }
}