# LithiumCarbon (碳酸锂缓释片) 文档

> LithiumCarbon (Li₂CO₃) — 战利品箱管理系统  
> 版本 1.2.x | Liminal Skyline 系列插件

---

## 目录

1. [概述](#1-概述)
2. [安装](#2-安装)
3. [配置文件](#3-配置文件)
4. [战利品箱系统](#4-战利品箱系统)
5. [卡房系统](#5-卡房系统)
6. [展示框物资箱系统](#6-展示框物资箱系统)
7. [物品配置](#7-物品配置)
8. [脚本与事件代理](#8-脚本与事件代理)
9. [命令](#9-命令)
10. [API](#10-api)
11. [事件](#11-事件)

---

## 1. 概述

LithiumCarbon (碳酸锂缓释片) 是一个综合性战利品管理插件，包含三个核心模块：

- **战利品箱系统**：定义战利品模板，支持自定义 GUI 布局、加权物品池、搜索机制和自动刷新。
- **卡房系统**：基于触发的房间机制——放置触发方块，配置物品匹配，执行多步动作（生成箱子、开关门、执行脚本），自动重置。
- **展示框物资箱系统**：生成展示框展示战利品，玩家右键点击领取。

---

## 2. 安装

### 环境要求
- **Java 21+**
- **Paper 1.20.x+**（或兼容服务端）
- **TabooLib 6.2+**（已内置）

### 可选依赖
- **WorldGuard**：用于基于区域的战利品定义（`type: worldguard`）
- **Sertraline**：用于外部物品集成
- **CraftEngine / ItemsAdder**：用于自定义物品支持
- **Chemdah**：用于任务目标集成

### 安装步骤
1. 将插件 jar 放入 `plugins/` 文件夹。
2. 重启服务器。
3. 配置 `plugins/LithiumCarbon/` 下的文件。

### 文件夹结构
```
plugins/LithiumCarbon/
├── config.yml              # 主配置文件
├── items.yml               # GUI 展示物品定义
├── lang/
│   ├── en_US.yml           # 英文消息
│   └── zh_CN.yml           # 中文消息
├── loots/                  # 战利品模板文件 (*.yml, *.yaml, *.toml, *.json, *.conf)
├── card-rooms/             # 卡房配置文件
└── frame-crates/           # 展示框物资箱配置文件
```

---

## 3. 配置文件

### config.yml

```yaml
# lithiumcarbon Configuration / lithiumcarbon 配置
debug: false

hook:
  worldguard: true  # 启用 WorldGuard 集成

file-load:
  loots: "^(?![#!]).*\\.(?i)(yaml|yml|toml|tml|json|conf)$"  # 战利品文件加载模式

lang: en_US  # 默认语言

default-options:
  add-lore: null       # 默认追加到所有战利品物品的 lore
  remove-lore: false   # 移除物品原始 lore
  shuffle-loot: false  # 随机排列 GUI 中的物品位置
  private: false       # 箱子是否默认为私有

# Ignore warnings and errors / 忽略警告和错误
logger:
  ignore-errors:
    ErrorNoAvailableSlots: false
    ErrorNoPools: false
    ErrorItemGenerationFailed: false
    ErrorItemGenerationFailedNull: false
  ignore-warnings:
    WarningNotSupportDataComponent: false

# Allowed loot blocks / 允许的战利品方块
allowed-blocks:
  - CHEST
  - GRAY_SHULKER_BOX

allowed-all-blocks: false  # Bypass allowed-blocks check / 绕过允许方块检查

# Allowed loot worlds / 允许的战利品世界
allowed-worlds:
  - "world.+"

allowed-all-worlds: true  # Bypass allowed-worlds check / 绕过允许世界检查

message:           # 开关游戏内消息
  SearchLimit: true
  SearchStart: true
  Searching: false
  Searched: false
  Claim: false
  ClaimExp: true

sounds:            # 自定义音效（格式："sound 音量 音调 类别"）
  open: "minecraft:block.chest.locked 1 0.6 block"
  search: "minecraft:item.armor.equip_chain 1 1 block"
  searching: "minecraft:block.stone_pressure_plate.click_on 1 0.5 block"
  search-end: "minecraft:block.dispenser.dispense 1 1 block"
  search-limit: "minecraft:entity.villager.no 1 1 block"
  claim: "minecraft:block.stone_pressure_plate.click_on 1 1 block"
```

---

## 4. 战利品箱系统

用于创建带有自定义 GUI、加权物品、搜索机制和刷新周期的战利品箱。

### 战利品模板结构

```yaml
# plugins/LithiumCarbon/loots/<name>.yml
template_id:
  refresh:
    loops:
      - period: 300        # 每 300 秒刷新一次
        agent:
          onRefresh:
            js: |
              org.bukkit.Bukkit.broadcast(
                mmUtil.deserialize("<yellow>" + name + " 已刷新.")
              )
    expire: 300            # 开启后 300 秒自动刷新

  display:
    name: "模板名称"       # 显示名称（用于消息提示）
    title: "GUI标题"       # GUI 窗口标题
    rows: 3                # GUI 行数（1-6）
    layout:                # 布局模板（每行 9 字符）
      - '         '
      - '         '
      - '         '
    # available-slots: [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26]

  defines:              # 定义哪些方块/区域属于此战利品类型
    region_a:
      type: square      # 方形区域（坐标范围）
      from: 'world 0 0 0'
      to: 'world 100 100 100'
      blocks:
        - CHEST
        - BARREL
    region_wg:
      type: worldguard  # WorldGuard 区域
      region: region_name
      blocks:
        - CHEST
    region_world:
      type: world       # 世界范围（支持正则）
      world: world_.+
      regex: true
      blocks:
        - CHEST

  options:
    remove-lore: true     # 移除物品原始 lore
    add-lore:             # 追加 lore 行
      - "<gray>活动战利品"
    shuffle-loot: true    # 随机排列物品在 GUI 中的位置
    search-limit: 1       # 每位玩家同时搜索数量上限
    private: false        # 私有箱子（仅放置者可打开）

  pools:
    - rolls: 1~3          # 抽取物品数量（支持表达式）
      conditions:         # 可选：此物品池的条件
        js: "player.hasPermission('vip.loot')"
      loots:
        - items:
            - item: diamond
              amount: 1~3
          weight: 10
          search-time: 0.5
          exps: 50        # 领取时获得的经验值
          skipSearch: false  # 跳过搜索动画
        - items:
            - item: custom_sword
              amount: 1
          weight: 5
          search-time: 1.2
          displayItem:     # 在 GUI 中自定义展示物品
            item: GOLD_INGOT
            parameters:
              name: "<gold>神秘之剑"
```

### 关键概念

#### `defines` — 方块/区域匹配
| 类型 | 说明 |
|------|------|
| `square` | 通过坐标范围定义的立方体区域 |
| `worldguard` | WorldGuard 插件定义的区域 |
| `world` | 世界中所有匹配的方块（支持正则表达式） |

#### `pools` — 物品池
- **`rolls`**：每次打开箱子时抽取的物品数量。支持数字表达式和范围（如 `1~3`、`{player_level}`、`Math.min(5, level)`）。
- **`weight`**：在同一物品池中的相对权重。值越大，概率越高。
- **`search-time`**：搜索/找到该物品所需的时间（秒）。
- **`exps`**：领取物品时给予的经验值。
- **`skipSearch`**：设置为 `true` 使物品可立即领取，无需搜索。
- **`conditions`**：脚本条件，满足时此物品池才会参与抽取。

#### `refresh` — 自动刷新
- **`loops`**：定时周期性刷新。支持多个循环。
- **`expire`**：首次开启后 N 秒自动刷新。
- **Agent 事件**：`onRefresh`（刷新时）、`onCancel`（取消时）。

---

## 5. 卡房系统

基于触发的交互式房间。放置触发方块，配置物品匹配规则，定义多步动作。房间会自动重置。

### 卡房配置

```yaml
# plugins/LithiumCarbon/card-rooms/<name>.yml
room_id:
  name: "房间显示名称"

  # 触发配置
  trigger:
    block: "world 100 64 100"     # 触发方块位置（格式："世界名 x y z"）

    item:                          # 物品匹配规则（首匹配决定消耗方式）
      - tag:                       #   匹配具有特定 NBT 标签的物品
          custom-key: a
        consume-key:
          mode: durability         #   消耗模式：durability（耐久）/ item（物品）/ tag（标签）
          value: -1
      - tag:
          custom-key: b
        consume-key: true          #   消耗 1 个物品（默认模式）
      - consume-key: false         #   不需要钥匙（兜底匹配）

    # 可选触发条件
    condition:
      js: |
        player.level >= 10 &&
        player.gameMode == "SURVIVAL"
      mode: ALL                    # ALL = 所有条件必须满足，ANY = 任一条件满足即可

  # 动作列表（按顺序执行）
  actions:
    # 移除方块
    - type: "remove-block"
      location: "world 100 65 100"
      block: "STONE"              # 可选：移除前验证方块类型

    # 开门
    - type: "open-door"
      location: "world 101 64 100"
      direction: "NORTH"

    # 关门
    - type: "close-door"
      location: "world 101 64 100"

    # 设置方块
    - type: "set-block"
      location: "world 100 65 100"
      block: "STONE"

    # 生成物资箱
    - type: "spawn-chest"
      location: "world 102 64 100"
      loot-template: "dungeon_rewards"
      private: false               # 私有箱子（仅触发玩家可打开）
      block: "CHEST"               # 方块类型（CHEST, TRAPPED_CHEST, BARREL 等）

    # 生成展示框物资箱
    - type: "spawn-frame"
      location: "world 103 64 100"
      frame-crate: "example_frame_crate"
      facing: "SOUTH"

    # 移除展示框
    - type: "remove-frame"
      location: "world 103 64 100"

    # 执行脚本
    - type: "execute-script"
      js: |
        player.sendMessage("房间已激活！")
        player.playSound(player.location, "block.iron_door.open", 1.0, 1.0)

  # 重置配置
  reset:
    range:                          # 玩家检测区域（可选）
      from: "world 90 60 90"
      to: "world 110 70 110"
    delay: 300                      # 没有玩家后重置延迟（秒）
    restore: true                   # 是否恢复环境
    actions:                        # 重置时执行的动作
      - type: "remove-block"
        location: "world 102 64 100"
        block: "CHEST"
      - type: "set-block"
        location: "world 100 65 100"
        block: "STONE"
      - type: "close-door"
        location: "world 101 64 100"
      - type: "execute-script"
        js: |
          Bukkit.broadcastMessage("卡房已重置！")

  # 事件代理
  agents:
    onOpen:
      js: |
        msg(player, "<gold>你开启了地下城入口！")
    onReset:
      js: |
        devLog("卡房 " + name + " 已重置")
    onResetComplete:
      js: |
        Bukkit.broadcast(mmUtil.deserialize("<yellow>卡房重置完成。"))
    onActivate:
      js: |
        msg(player, "<green>卡房已激活！")
    onWrongKey:
      js: |
        msg(player, "<red>你没有正确的钥匙！")
    onConditionFail:
      js: |
        msg(player, "<red>你不满足开启条件！")
    onAlreadyActive:
      js: |
        msg(player, "<yellow>该卡房已经开启了！")
    onOpeningBlocked:
      js: |
        msg(player, "<red>卡房开启被阻止！")
```

### 动作类型参考

| 类型 | 说明 |
|------|------|
| `remove-block` | 移除方块（可选验证方块类型） |
| `set-block` | 放置方块 |
| `open-door` | 开门 |
| `close-door` | 关门 |
| `spawn-chest` | 生成战利品箱（需指定模板） |
| `spawn-frame` | 生成展示框物资箱 |
| `remove-frame` | 移除展示框 |
| `execute-script` | 执行 JavaScript 或 Kether 脚本 |

### 物品消耗模式

| 模式 | 说明 |
|------|------|
| `item` | 减少物品数量（默认） |
| `durability` | 消耗物品耐久（`-1` 表示一次使用） |
| `tag` | 修改 NBT 标签值 |

---

## 6. 展示框物资箱系统

生成展示框展示战利品物品外观，玩家右键点击领取。

### 展示框物资箱配置

```yaml
# plugins/LithiumCarbon/frame-crates/<name>.yml
crate_id:
  loot-template: "template_id"   # 引用的战利品模板 ID
  expire: 120                     # 过期时间（秒，0 为永不过期）
  glow: true                      # 使用发光展示框（默认：false）

  # 事件代理
  agents:
    onSpawn:
      js: |
        Bukkit.broadcastMessage("展示框物资箱已生成！")
    onClaim:
      js: |
        player.sendMessage("你领取了奖励！")
```

### 生成展示框物资箱

可通过卡房的 `spawn-frame` 动作或 `/lcmanage spawnFrame` 命令生成。

---

## 7. 物品配置

物品在 `items.yml` 中定义，可在战利品模板中引用。

```yaml
# plugins/LithiumCarbon/items.yml
unsearch:
  item: GRAY_STAINED_GLASS_PANE
  parameters:
    name: "<!i><red><bold>点击搜索..."

searching:
  item: GRAY_STAINED_GLASS_PANE
  parameters:
    name: "<!i><red><bold>搜索中..."

undefinedItem:
  item: GOLD_INGOT
  parameters:
    name: "<!i><gold><bold>战利品"

A:                                    # 自定义展示物品（在 layout 中引用为 'A'）
  item: TINTED_GLASS
  parameters:
    name: "<!i><gold><bold>固定物品"
```

### 物品来源

物品可以来自：

1. **原版 Minecraft**：使用标准材料名（`DIAMOND`、`CHEST` 等）
2. **外部插件**：格式 `插件ID:命名空间:物品ID`
   - CraftEngine：`craftengine:default:ruby`
   - ItemsAdder：`itemsadder:my_item`
   - Sertraline：`sertraline:namespace:id`

### 物品参数

| 参数 | 说明 |
|------|------|
| `name` | 物品显示名称（MiniMessage 格式） |
| `display-name` | 同 name |
| `custom-name` | Minecraft 自定义名称 |
| `item-name` | Minecraft 物品名称（1.20.5+） |
| `item-model` / `model` | 自定义模型数据 / 物品模型 |
| `lore` | 物品 lore 行（MiniMessage 字符串列表） |

### 数据组件（1.20.5+）

```yaml
item_id:
  item: DIAMOND_SWORD
  components:
    minecraft:unbreakable: true
    minecraft:attribute_modifiers: []
    minecraft:enchantments:
      levels:
        minecraft:sharpness: 5
```

---

## 8. 脚本与事件代理

### JavaScript

可在战利品模板、卡房、展示框等所有支持 `agents` 的地方使用。

**预配置变量**：

| 变量 | 说明 |
|------|------|
| `player` | Bukkit Player 对象 |
| `mmUtil` | MiniMessage 工具类 |
| `mmJsonUtil` | MiniMessage JSON 工具类 |
| `Bukkit` | Bukkit 服务器 API |
| `LithiumCarbonAPI` | 插件 API |
| `Math` | Java Math 类 |
| `System` | Java System 类 |
| `Gson` | Gson JSON 解析器 |
| `ItemStackUtil` | 物品堆工具类 |
| `EventUtil` | 事件工具类 |
| `ThreadUtil` | 线程/异步工具类 |
| `PlayerUtil` | 玩家工具类 |

此外还有模板特定变量如 `name`、`id`、`template`、`element` 等。

### Kether

Kether 脚本与 JS 同样支持：

```yaml
agents:
  onOpen:
    ke: |
      minitell "<green>房间已开启！"
      sound "block.iron_door.open" 1 1
```

### 异步脚本

使用 `async_js` 或 `async_ke` 使脚本异步执行。

---

## 9. 命令

### 主命令：`/lithiumcarbon`（别名：`/li2co3`、`/lc`）

| 子命令 | 权限 | 说明 |
|--------|------|------|
| `about` | `lithiumcarbon.command.main` | 显示插件信息 |
| `reload` | `lithiumcarbon.command.main` | 重载所有配置 |

### 管理命令：`/lithiumcarbon-manage`（别名：`/li2co3manage`、`/lcmanage`）

| 子命令 | 权限 | 说明 |
|--------|------|------|
| `generateItem <模板> <玩家> <数量>` | `lithiumcarbon.command.manage` | 从模板生成战利品物品 |
| `update <位置>` | `lithiumcarbon.command.manage` | 强制更新指定位置的战利品实例 |
| `updateWithoutCheck <位置>` | `lithiumcarbon.command.manage` | 强制更新（不验证） |
| `updateAll [模板]` | `lithiumcarbon.command.manage` | 更新所有实例（可选按模板过滤） |
| `spawnFrame <配置ID> [世界 x y z] [朝向]` | `lithiumcarbon.command.manage` | 生成展示框物资箱 |
| `cardroom list` | `lithiumcarbon.command.manage` | 列出所有卡房 |
| `cardroom info <id>` | `lithiumcarbon.command.manage` | 查看卡房详细信息 |
| `cardroom activate <id> [玩家]` | `lithiumcarbon.command.manage` | 激活卡房 |
| `cardroom reset <id>` | `lithiumcarbon.command.manage` | 重置卡房 |
| `cardroom resetall` | `lithiumcarbon.command.manage` | 重置所有卡房 |
| `cardroom status <id>` | `lithiumcarbon.command.manage` | 查看卡房状态 |

---

## 10. API

### 依赖引入

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.zzzyyylllty:lithiumcarbon:VERSION")
}
```

### 可用 API 方法

通过 `LithiumCarbonAPI` 访问：

```kotlin
// 获取战利品映射
getLootMap(): Map<LootInstanceKey, LootInstance>
getLootTemplates(): Map<String, LootTemplate>
getLootDefines(): Map<String, LootDefines>
getLootCaches(): Map<LootLocation, LootTemplate>
getLootItems(): Map<Char, LootItem>
getLootItemsDef(): Map<String, LootItem>

// 强制更新指定 Bukkit 位置的战利品实例
updateInstance(bukkitLocation: Location)
```

### 添加自定义脚本变量

```kotlin
// 监听 LithiumCarbonCustomScriptDataLoadEvent
// 向 defaultData 映射中添加自定义绑定
```

---

## 11. 事件

所有事件继承自 TabooLib 的 `BukkitProxyEvent`。

### 战利品事件

| 事件 | 可取消 | 说明 |
|------|--------|------|
| `LootInstanceCreateEvent` | 否 | 战利品实例被创建时 |
| `ItemSearchStartEvent` | 是 | 玩家开始搜索物品时 |
| `ItemSearchCompletePreEvent` | 是 | 搜索完成，物品给予玩家前 |
| `ItemSearchCompletePostEvent` | 否 | 搜索完成，物品给予玩家后 |
| `LootElementApplyEvent` | 是 | 战利品元素即将应用到玩家时 |
| `LootItemGrantEvent` | 是 | 物品即将给予玩家时 |

### 卡房事件

| 事件 | 可取消 | 说明 |
|------|--------|------|
| `CardRoomPreOpenEvent` | 是 | 卡房激活前，消耗钥匙和执行动作前 |
| `CardRoomOpenEvent` | 否 | 卡房已激活，所有动作执行完毕 |
| `CardRoomPreResetEvent` | 是 | 卡房重置开始前 |
| `CardRoomResetEvent` | 否 | 卡房重置完成 |

### 展示框事件

| 事件 | 可取消 | 说明 |
|------|--------|------|
| `FrameCratePreClaimEvent` | 是 | 领取展示框物品前 |
| `FrameCrateClaimEvent` | 否 | 展示框已领取，物品已给予 |

### 其他事件

| 事件 | 说明 |
|------|------|
| `LithiumCarbonReloadEvent` | 插件配置重载后触发 |
| `LithiumCarbonCustomScriptDataLoadEvent` | JS 脚本数据绑定初始化时触发 |

---

## 从源码构建

```bash
# 正常构建
./gradlew clean build
# 产物在 plugin/build/libs/

# API 构建（供开发者使用）
./gradlew clean taboolibBuildApi -PDeleteCode
```

---

*LithiumCarbon — Liminal Skyline 系列插件*  
*作者：AkaCandyKAngel*
