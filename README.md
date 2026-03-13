# LithiumCarbon

本插件已经基本完成，但是我懒得写文档。干脆丢给AI了：

This plugin is complete but I am lazy to write full doc.

Here are two simple docs

[English](en.md)

[Chinese](zh.md)

超天战利品箱管理系统服务 (Liminal Skyline v4.0 服务)

ChoTen Loot Chest management system service (Liminal Skyline v4.0 Service)

## Used server

BKRMC (search on bilibili)

United Liminal Skyline (No video available for new version of loot table)

## Donate!

Click here to donate via [Afdian](https://afdian.com/a/liminalskyline).

## As dependency

```Gradle kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.zzzyyylllty:lithiumcarbon:VERSION")
}
```

## Build Runtime Version

Required Java 21.

Runtime version for normal use.

Build artifact is in `plugin/build/libs` folder.

```
./gradlew clean build
```

## Build Api Version

The api version includes the TabooLib core, intended for developers' use but not runnable.

```
./gradlew clean taboolibBuildApi -PDeleteCode
```

> The parameter `-PDeleteCode` indicates the removal of all logic code to reduce size.
