
import io.izzel.taboolib.gradle.*
import io.izzel.taboolib.gradle.BukkitNMSUtil
import org.gradle.internal.impldep.org.apache.http.client.methods.RequestBuilder.options
import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
//import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    java
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.taboolib)
    kotlin("plugin.serialization") version "2.0.0"
    id("maven-publish")
}

allprojects {

    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "io.izzel.taboolib")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")
    taboolib {
        env {
            // 调试模式
            debug = false
            // 是否在开发模式下强制下载依赖
            forceDownloadInDev = false
            // 中央仓库地址
            repoCentral = "https://maven.aliyun.com/repository/central"
            // TabooLib 仓库地址
//            repoTabooLib = "https://repo.xiao-jie.top/repository/maven-releases"
            repoTabooLib = "https://repo.tabooproject.org/repository/releases"
//            repoTabooLib = project.repositories.mavenLocal().url.toString()
            // 依赖下载目录
            fileLibs = "libraries"
            // 资源下载目录
            fileAssets = "assets"
            // 是否启用隔离加载器（即完全隔离模式）
            enableIsolatedClassloader = false
            install(Basic, Bukkit, BukkitHook, BukkitNMSUtil, Database, Kether, CommandHelper, BukkitNMSItemTag, JavaScript, BukkitUI, BukkitUtil, Jexl, Metrics, PtcObject)
            install(BukkitNMS)
            install(BukkitNMSDataSerializer)
            // install("bukkit-nms-tag-component")
        }
        version {
            taboolib = rootProject.libs.versions.taboolib.get()
            coroutines = "1.8.1"
            // 跳过 Kotlin 加载
            skipKotlin = false
            // 跳过 Kotlin 重定向
            skipKotlinRelocate = false
            // 跳过 TabooLib 重定向
            skipTabooLibRelocate = false

        }
    }

    repositories {
        mavenLocal()
        maven("https://repo.gtemc.net/releases/")
        maven("https://repo.auxilor.io/repository/maven-public/")
        maven("https://nexus.phoenixdevt.fr/repository/maven-public/")
        maven("https://repo.aeoliancloud.com/releases/")
        maven("https://repo.xiao-jie.top/repository/maven-releases")
        maven("https://jitpack.io")
        maven {
            name = "CodeMC"
            url = uri("https://repo.codemc.io/repository/maven-public/")
        }
        maven("https://central.sonatype.com/repository/maven-snapshots")
        maven("https://mvnrepository.com/artifact/")
        maven {
            // 枫溪的仓库
            url = uri("https://nexus.maplex.top/repository/maven-public/")
            isAllowInsecureProtocol = true
        }
        maven("https://maven.enginehub.org/repo")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/jcenter")
        mavenCentral()

        maven("https://dl.bintray.com/kotlin/kotlinx/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.papermc.io/repository/maven-snapshots/")
        maven {
            name = "mythicmobs"
            url = uri("https://mvn.lumine.io/repository/maven-public/")
        }
        maven {
            url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        }
        maven("https://repo.codemc.org/repository/maven-public")
        maven("https://repo.rosewooddev.io/repository/public/")
        maven("https://repo.opencollab.dev/main/")
        maven("https://r.irepo.space/maven/")
        maven("https://repo.hibiscusmc.com/releases/")
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/") {
            name = "sonatype-oss-snapshots"
        }

        maven("https://libraries.minecraft.net")
        maven("https://repo.momirealms.net/releases/")
        maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
        maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
        maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
        maven("https://repo.extendedclip.com/releases/")
    }

    dependencies {
        taboo("io.github.zzzyyylllty:EmbianComponent:1.0.5")
        implementation("com.github.zzzyyylllty:Sertraline-Hydrochloride:3.8.2")
        implementation(rootProject.libs.bundles.asm)

        taboo("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        implementation("com.sk89q.worldguard:worldguard-bukkit:7.0.10-SNAPSHOT")
        implementation("com.sk89q.worldguard:worldguard-core:7.0.10-SNAPSHOT")
        // 服务器 API
        implementation(rootProject.libs.paperapi)

        compileOnly("ink.ptms.chemdah:api:1.1.17")
        compileOnly(rootProject.libs.placeholderapi)

        // 本地依赖 (这行需要保留)
        compileOnly(fileTree("libs"))

        // 工具库
        compileOnly(rootProject.libs.gson)

        compileOnly("net.momirealms:craft-engine-core:0.0.66")
        compileOnly("net.momirealms:craft-engine-bukkit:0.0.66")
        // 核心功能库 (运行时需要)
        implementation(rootProject.libs.bundles.adventure)
        taboo(rootProject.libs.arim)
        taboo(rootProject.libs.bundles.jackson)
        taboo("cn.gtemc:itembridge:1.0.18")
        kotlin("stdlib")
    }


    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    tasks.withType<JavaCompile> {
        options.release.set(21)
        options.encoding = "UTF-8"
    }

// 定义源码包任务
    val sourcesJar by tasks.registering(Jar::class) {
        from(sourceSets.main.get().allJava)
        archiveClassifier.set("sources")
    }

// 定义文档包任务（可选）
    val javadocJar by tasks.registering(Jar::class) {
        from(tasks.javadoc)
        archiveClassifier.set("javadoc")
    }

    publishing {
        publications {
            create<MavenPublication>("lithiumcarbon") {
                // 主构件（自动从 java 组件生成）
                from(components["java"])

                val rootVersion = rootProject.version
                val rootGroup = "io.github.zzzyyylllty.lithiumcarbon"
                // 添加源码包（必须指定分类器）
                artifact(sourcesJar.get()) {
                    classifier = "sources"
                }

                // 添加文档包（可选）
                artifact(javadocJar.get()) {
                    classifier = "javadoc"
                }

                // 其他构件配置...
            }
        }
    }
}
//
//
//project(":project:common-files") {
//    tasks.withType<ProcessResources>().configureEach {
//        filesMatching("**/*.json") {
//            expand(
//                "nashornVersion" to rootProject.libs.versions.nashorn.get(),
//                "graaljsVersion" to rootProject.libs.versions.graalvm.get(),
//                "jexlVersion" to rootProject.libs.versions.jexl.get(),
//                "gsonVersion" to rootProject.libs.versions.gson.get(),
//                "kotlincryptoVersion" to rootProject.libs.versions.kotlinCrypto.get(),
//                "caffeineVersion" to rootProject.libs.versions.caffeine.get(),
//                "fluxonVersion" to rootProject.libs.versions.fluxon.get(),
//                "datafixerupperVersion" to rootProject.libs.versions.datafixerupper.get(),
//                "uniItemVersion" to rootProject.libs.versions.uniItem.get(),
//                "adventureVersion" to rootProject.libs.versions.adventure.get(),
//                "jacksonVersion" to rootProject.libs.versions.jackson.get(),
//                "arimVersion" to rootProject.libs.versions.arim.get(),
//            )
//        }
//    }
//}