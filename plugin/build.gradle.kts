taboolib {
    description {
        name("LithiumCarbon")
        desc("ChoTen backrooms plugin.")
        contributors {
            // 作者名称
            name("AkaCandyKAngel")
        }
        dependencies {
            name("MythicMobs").optional(true)
            name("Chemdah").optional(true)
            name("WorldGuard").optional(true)
            // name("ProtocolLib").optional(true)
            // 可选依赖.
            // name("XXX").optional(true)
        }
    }


    // Relocate 必须与META-INF/dependencies里面的relocate一致。
    // 同时，还需要修改io.github.zzzyyylllty.lithiumcarbon.util.lithiumcarbonLocalDependencyHelper.replaceTestTexts
    relocate("top.maplex.arim","io.github.zzzyyylllty.lithiumcarbon.library.arim")
    relocate("ink.ptms.um","io.github.zzzyyylllty.lithiumcarbon.library.um")
     relocate("com.google", "io.github.zzzyyylllty.lithiumcarbon.library.google")
    relocate("com.alibaba", "io.github.zzzyyylllty.lithiumcarbon.library.alibaba")
    relocate("kotlinx.serialization", "kotlinx.serialization170")
    // relocate("de.tr7zw.changeme.nbtapi","io.github.zzzyyylllty.lithiumcarbon.library.nbtapi")
    relocate("io.github.projectunified.uniitem","io.github.zzzyyylllty.lithiumcarbon.library.uniitem")
    relocate("com.fasterxml.jackson","io.github.zzzyyylllty.lithiumcarbon.library.jackson")
    relocate("com.mojang.datafixers","io.github.zzzyyylllty.lithiumcarbon.library.datafixers")
    relocate("io.netty.handler.codec.http", "io.github.zzzyyylllty.lithiumcarbon.library.http")
    relocate("io.netty.handler.codec.rtsp", "io.github.zzzyyylllty.lithiumcarbon.library.rtsp")
    relocate("io.netty.handler.codec.spdy", "io.github.zzzyyylllty.lithiumcarbon.library.spdy")
    relocate("io.netty.handler.codec.http2", "io.github.zzzyyylllty.lithiumcarbon.library.http2")
    relocate("org.tabooproject.fluxon","io.github.zzzyyylllty.lithiumcarbon.library.fluxon")
    relocate("com.github.benmanes.caffeine","io.github.zzzyyylllty.lithiumcarbon.library.caffeine")
    relocate("org.kotlincrypto","io.github.zzzyyylllty.lithiumcarbon.library.kotlincrypto")
//    relocate("com.oracle.truffle","io.github.zzzyyylllty.lithiumcarbon.library.truffle")
//    relocate("org.graalvm.polyglot","io.github.zzzyyylllty.lithiumcarbon.library.polyglot")
}

tasks {
    jar {
        archiveFileName.set("${rootProject.name}-${archiveFileName.get().substringAfter('-')}")
        rootProject.subprojects.forEach { from(it.sourceSets["main"].output) }
    }
}
