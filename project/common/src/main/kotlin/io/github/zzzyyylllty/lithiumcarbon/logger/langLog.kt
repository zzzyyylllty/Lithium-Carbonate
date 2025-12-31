package io.github.zzzyyylllty.lithiumcarbon.logger

import io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon
import io.github.zzzyyylllty.lithiumcarbon.util.minimessage.legacyToMiniMessage
import io.github.zzzyyylllty.lithiumcarbon.util.minimessage.mmUtil
import net.kyori.adventure.audience.Audience
import org.bukkit.command.CommandSender
import taboolib.common.platform.function.submitAsync
import taboolib.module.lang.asLangText

val prefix = "[<gradient:#ccaaff:#9900ff:#ff0099>LithiumCarbon</gradient>]"



fun infoL(node: String,vararg args: Any) {
    LithiumCarbon.consoleSender.infoS(LithiumCarbon.console.asLangText(node,args))
}
fun severeL(node: String,vararg args: Any) {
    LithiumCarbon.consoleSender.severeS(LithiumCarbon.console.asLangText(node,args))
}
fun warningL(node: String,vararg args: Any) {
    LithiumCarbon.consoleSender.warningS(LithiumCarbon.console.asLangText(node,args))
}

fun CommandSender?.fineS(message: String, bothSendConsole: Boolean = false) {
    val sender = this
    submitAsync {
        (sender ?: LithiumCarbon.consoleSender).sendStringAsComponent("<gray>${prefix} [<#66ffcc>FINE</#66ffcc>]</gray> <reset>$message")
        if (sender != null && bothSendConsole) LithiumCarbon.consoleSender.sendStringAsComponent("<gray>$prefix [<#66ffcc>FINES</#66ffcc>]</gray> <reset>$message")
    }
}

fun CommandSender?.debugS(message: String, bothSendConsole: Boolean = false) {
    val sender = this
    submitAsync {
        (sender ?: LithiumCarbon.consoleSender).sendStringAsComponent("<gray>$prefix [<#ddaa77>DEBUG</#ddaa77>]</gray> <#aaaaaa>$message")
        if (sender != null && bothSendConsole) LithiumCarbon.consoleSender.sendStringAsComponent("<gray>$prefix [<#ddaa77>DEBUG</#ddaa77>]</gray> <#aaaaaa>$message")
    }
}

fun CommandSender?.infoS(message: String, bothSendConsole: Boolean = false) {
    val sender = this
    submitAsync {
        (sender ?: LithiumCarbon.consoleSender).sendStringAsComponent("<gray>$prefix [<#66ccff>INFO</#66ccff>]</gray> <reset>$message")
        if (sender != null && bothSendConsole) LithiumCarbon.consoleSender.sendStringAsComponent("<gray>$prefix [<#66ccff>INFOS</#66ccff>]</gray> <reset>$message")
    }
}

fun CommandSender?.warningS(message: String, bothSendConsole: Boolean = false) {
    val sender = this
    submitAsync {
        (sender ?: LithiumCarbon.consoleSender).sendStringAsComponent("<gray>$prefix [<#ffee66>WARN</#ffee66>]</gray> <#eeeeaa>$message")
        if (sender != null && bothSendConsole) LithiumCarbon.consoleSender.sendStringAsComponent("<gray>$prefix [<#ffee66>WARNI</#ffee66>]</gray> <#eeeeaa>$message")
    }
}

fun CommandSender?.severeS(message: String, bothSendConsole: Boolean = false) {
    val sender = this
    submitAsync {
        (sender ?: LithiumCarbon.consoleSender).sendStringAsComponent("<gray>$prefix [<#ff6600>ERROR</#ff6600>]</gray> <#ffccbb>$message")
        if (sender != null && bothSendConsole) LithiumCarbon.consoleSender.sendStringAsComponent("<gray>$prefix [<#ff6600>ERROR</#ff6600>]</gray> <#ffccbb>$message")
    }
}

fun fineS(message: String) {
    submitAsync {
        LithiumCarbon.consoleSender.sendStringAsComponent("<gray>$prefix [<#66ffcc>FINE</#66ffcc>]</gray> <reset>$message")
    }
}

fun debugS(message: String) {
    submitAsync {
        LithiumCarbon.consoleSender.sendStringAsComponent("<gray>$prefix [<#ddaa77>DEBUG</#ddaa77>]</gray> <#aaaaaa>$message")
    }
}

fun infoS(message: String) {
    submitAsync {
        LithiumCarbon.consoleSender.sendStringAsComponent("<gray>$prefix [<#66ccff>INFO</#66ccff>]</gray> <reset>$message")
    }
}

fun warningS(message: String) {
    submitAsync {
        LithiumCarbon.consoleSender.sendStringAsComponent("<gray>$prefix [<#ffee66>WARN</#ffee66>]</gray> <#eeeeaa>$message")
    }
}

fun severeS(message: String) {
    submitAsync {
        LithiumCarbon.consoleSender.sendStringAsComponent("<gray>$prefix [<#ff6600>ERROR</#ff6600>]</gray> <#ffccbb>$message")
    }
}

fun CommandSender.sendStringAsComponent(message: String) {
    val sender = this
    (sender as Audience).sendMessage(mmUtil.deserialize(message.legacyToMiniMessage()))
}
