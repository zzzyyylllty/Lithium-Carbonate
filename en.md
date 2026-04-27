# LithiumCarbon Documentation

> LithiumCarbon (Li₂CO₃) — Loot Chest Management System  
> Version 1.2.x | Liminal Skyline Series Plugin

---

## Table of Contents

1. [Overview](#1-overview)
2. [Installation](#2-installation)
3. [Configuration](#3-configuration)
4. [Loot Chest System](#4-loot-chest-system)
5. [Card Room System](#5-card-room-system)
6. [Frame Crate System](#6-frame-crate-system)
7. [Item Configuration](#7-item-configuration)
8. [Scripting & Agents](#8-scripting--agents)
9. [Commands](#9-commands)
10. [API](#10-api)
11. [Events](#11-events)

---

## 1. Overview

LithiumCarbon is a comprehensive loot chest management system with three core modules:

- **Loot Chest System**: Define loot templates with custom GUI layouts, weighted item pools, search mechanics, and auto-refresh.
- **Card Room System**: Trigger-based room mechanics — place trigger blocks, execute multi-step actions (spawn chests, open doors, run scripts), and auto-reset.
- **Frame Crate System**: Spawn item frame crates that display loot items visually; players claim by right-clicking.

---

## 2. Installation

### Requirements
- **Java 21+**
- **Paper 1.20.x+** (or forks)
- **TabooLib 6.2+** (bundled)

### Optional Dependencies
- **WorldGuard**: For region-based loot definitions (`type: worldguard`)
- **Sertraline**: For external item integration
- **CraftEngine / ItemsAdder**: For custom item support
- **Chemdah**: For quest objectives integration

### Setup
1. Place the plugin jar in `plugins/` folder.
2. Restart the server.
3. Configure files in `plugins/LithiumCarbon/`.

### Folder Structure
```
plugins/LithiumCarbon/
├── config.yml              # Main configuration
├── items.yml               # GUI display items definition
├── lang/
│   ├── en_US.yml           # English messages
│   └── zh_CN.yml           # Chinese messages
├── loots/                  # Loot template files (*.yml, *.yaml, *.toml, *.json, *.conf)
├── card-rooms/             # Card room configuration files
└── frame-crates/           # Frame crate configuration files
```

---

## 3. Configuration

### config.yml

```yaml
# LithiumCarbon Configuration
debug: false

hook:
  worldguard: true  # Enable WorldGuard integration

file-load:
  loots: "^(?![#!]).*\\.(?i)(yaml|yml|toml|tml|json|conf)$"  # Loot file loading pattern

lang: en_US  # Default language

default-options:
  add-lore: null       # Default lore to append to all loot items
  remove-lore: false   # Remove original lore from loot items
  shuffle-loot: false  # Shuffle loot positions in GUI
  private: false       # Whether chests are private by default

logger:
  ignore-errors:
    ErrorNoAvailableSlots: false
    ErrorNoPools: false
    ErrorItemGenerationFailed: false
    ErrorItemGenerationFailedNull: false
  ignore-warnings:
    WarningNotSupportDataComponent: false

# Allowed loot blocks (block types that can be loot chests)
allowed-blocks:
  - CHEST
  - GRAY_SHULKER_BOX

allowed-all-blocks: false  # Bypass allowed-blocks check

allowed-worlds:
  - "world.+"  # Worlds where loot can generate (supports regex)

allowed-all-worlds: true  # Bypass allowed-worlds check

message:           # Toggle in-game messages
  SearchLimit: true
  SearchStart: true
  Searching: false
  Searched: false
  Claim: false
  ClaimExp: true

sounds:            # Custom sound effects (format: "sound volume pitch category")
  open: "minecraft:block.chest.locked 1 0.6 block"
  search: "minecraft:item.armor.equip_chain 1 1 block"
  searching: "minecraft:block.stone_pressure_plate.click_on 1 0.5 block"
  search-end: "minecraft:block.dispenser.dispense 1 1 block"
  search-limit: "minecraft:entity.villager.no 1 1 block"
  claim: "minecraft:block.stone_pressure_plate.click_on 1 1 block"
```

---

## 4. Loot Chest System

The core system for creating loot chests with custom GUI, weighted items, search mechanics, and refresh cycles.

### Loot Template Structure

```yaml
# plugins/LithiumCarbon/loots/<name>.yml
template_id:
  refresh:
    loops:
      - period: 300        # Refresh every 300 seconds
        agent:
          onRefresh:
            js: |
              org.bukkit.Bukkit.broadcast(
                mmUtil.deserialize("<yellow>" + name + " Refreshed.")
              )
    expire: 300            # Auto-refresh 300s after being opened

  display:
    name: "Template Name"  # Display name (used in messages)
    title: "GUI Title"      # GUI window title
    rows: 3                 # GUI rows (1-6)
    layout:                 # Layout pattern (9 chars per row)
      - '         '
      - '         '
      - '         '
    # available-slots: [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26]

  defines:              # Define which blocks/regions are this loot type
    region_a:
      type: square      # Square region by coordinates
      from: 'world 0 0 0'
      to: 'world 100 100 100'
      blocks:
        - CHEST
        - BARREL
    region_wg:
      type: worldguard  # WorldGuard region
      region: region_name
      blocks:
        - CHEST
    region_world:
      type: world       # World-wide (regex supported)
      world: world_.+
      regex: true
      blocks:
        - CHEST

  options:
    remove-lore: true     # Strip original lore from items
    add-lore:             # Append extra lore lines
      - "<gray>Event Loot"
    shuffle-loot: true    # Randomize item positions in GUI
    search-limit: 1       # Max simultaneous searches per player
    private: false        # Private chest (only the placer can open)

  pools:
    - rolls: 1~3          # Number of items to draw (supports expressions)
      conditions:         # Optional: conditions for this pool
        js: "player.hasPermission('vip.loot')"
      loots:
        - items:
            - item: diamond
              amount: 1~3
          weight: 10
          search-time: 0.5
          exps: 50        # Experience awarded on claim
          skipSearch: false  # Skip search animation
        - items:
            - item: custom_sword
              amount: 1
          weight: 5
          search-time: 1.2
          displayItem:     # Custom display item in GUI
            item: GOLD_INGOT
            parameters:
              name: "<gold>Mysterious Sword"
```

### Key Concepts

#### `defines` — Block/Region Matching
| Type | Description |
|------|-------------|
| `square` | Cuboid region by coordinate ranges |
| `worldguard` | Region defined by WorldGuard plugin |
| `world` | All matching blocks in a world (supports regex) |

#### `pools` — Item Pools
- **`rolls`**: Number of items to draw per chest open. Supports number expressions and ranges (e.g. `1~3`, `{player_level}`, `Math.min(5, level)`).
- **`weight`**: Relative probability within a pool. Higher = more likely.
- **`search-time`**: Time in seconds required to search/find this item.
- **`exps`**: Experience points granted when item is claimed.
- **`skipSearch`**: Set to `true` to make the item instantly claimable.
- **`conditions`**: Script conditions that must be met for this pool to roll.

#### `refresh` — Auto-Refresh
- **`loops`**: Periodic refresh on a timer. Multiple loops supported.
- **`expire`**: Refresh triggered N seconds after the chest is first opened.
- **Agent events**: `onRefresh`, `onCancel` for custom refresh behavior.

---

## 5. Card Room System

Trigger-based interactive rooms. Place a trigger block, configure item matching, and define multi-step actions that execute when a player activates the room. Rooms auto-reset.

### Card Room Configuration

```yaml
# plugins/LithiumCarbon/card-rooms/<name>.yml
room_id:
  name: "Room Display Name"

  # Trigger configuration
  trigger:
    block: "world 100 64 100"     # Trigger block location (format: "world x y z")

    item:                          # Item matching rules (first match wins)
      - tag:                       #   Match item with specific NBT tags
          custom-key: a
        consume-key:
          mode: durability         #   Consume mode: durability / item / tag
          value: -1
      - tag:
          custom-key: b
        consume-key: true          #   Consume 1 item (default mode)
      - consume-key: false         #   No key required (catch-all)

    # Optional trigger conditions
    condition:
      js: |
        player.level >= 10 &&
        player.gameMode == "SURVIVAL"
      mode: ALL                    # ALL = all conditions must pass, ANY = any one passes

  # Actions to execute (in order)
  actions:
    # Remove a block
    - type: "remove-block"
      location: "world 100 65 100"
      block: "STONE"              # Optional: verify block type before removing

    # Open a door
    - type: "open-door"
      location: "world 101 64 100"
      direction: "NORTH"

    # Close a door
    - type: "close-door"
      location: "world 101 64 100"

    # Set a block
    - type: "set-block"
      location: "world 100 65 100"
      block: "STONE"

    # Spawn a loot chest
    - type: "spawn-chest"
      location: "world 102 64 100"
      loot-template: "dungeon_rewards"
      private: false               # Private chest (only trigger player can open)
      block: "CHEST"               # Block type (CHEST, TRAPPED_CHEST, BARREL, etc.)

    # Spawn an item frame crate
    - type: "spawn-frame"
      location: "world 103 64 100"
      frame-crate: "example_frame_crate"
      facing: "SOUTH"

    # Remove an item frame crate
    - type: "remove-frame"
      location: "world 103 64 100"

    # Execute a script
    - type: "execute-script"
      js: |
        player.sendMessage("Room activated!")
        player.playSound(player.location, "block.iron_door.open", 1.0, 1.0)

  # Reset configuration
  reset:
    range:                          # Player detection area (optional)
      from: "world 90 60 90"
      to: "world 110 70 110"
    delay: 300                      # Reset delay after no players (seconds)
    restore: true                   # Restore environment on reset
    actions:                        # Actions to execute on reset
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
          Bukkit.broadcastMessage("Room has been reset!")

  # Event agents
  agents:
    onOpen:
      js: |
        msg(player, "<gold>You opened the dungeon entrance!")
    onReset:
      js: |
        devLog("Room " + name + " has been reset")
    onResetComplete:
      js: |
        Bukkit.broadcast(mmUtil.deserialize("<yellow>Room reset complete."))
    onActivate:
      js: |
        msg(player, "<green>Room activated!")
    onWrongKey:
      js: |
        msg(player, "<red>You don't have the right key!")
    onConditionFail:
      js: |
        msg(player, "<red>You don't meet the requirements!")
    onAlreadyActive:
      js: |
        msg(player, "<yellow>This room is already active!")
    onOpeningBlocked:
      js: |
        msg(player, "<red>Room opening was blocked!")
```

### Action Types Reference

| Type | Description |
|------|-------------|
| `remove-block` | Remove a block at location (optional block verification) |
| `set-block` | Place a block at location |
| `open-door` | Open a door |
| `close-door` | Close a door |
| `spawn-chest` | Spawn a loot chest with a template |
| `spawn-frame` | Spawn an item frame crate |
| `remove-frame` | Remove an item frame |
| `execute-script` | Run JavaScript or Kether script |

### Item Consumption Modes

| Mode | Description |
|------|-------------|
| `item` | Decrease item amount by `value` (default) |
| `durability` | Damage the item by `value` (use `-1` for one use) |
| `tag` | Modify an NBT tag value |

---

## 6. Frame Crate System

Spawn item frames that display a loot item visually. Players right-click to claim.

### Frame Crate Configuration

```yaml
# plugins/LithiumCarbon/frame-crates/<name>.yml
crate_id:
  loot-template: "template_id"   # Loot template to draw the item from
  expire: 120                     # Expiry time in seconds (0 = never expires)
  glow: true                      # Use glowing item frame (default: false)

  # Event agents
  agents:
    onSpawn:
      js: |
        Bukkit.broadcastMessage("A frame crate has spawned!")
    onClaim:
      js: |
        player.sendMessage("You claimed the reward!")
```

### Spawning Frame Crates

Use the card room's `spawn-frame` action or the `/lcmanage spawnFrame` command.

---

## 7. Item Configuration

Items are defined in `items.yml` and can be referenced in loot templates.

```yaml
# plugins/LithiumCarbon/items.yml
unsearch:
  item: GRAY_STAINED_GLASS_PANE
  parameters:
    name: "<!i><red><bold>Click to Search..."

searching:
  item: GRAY_STAINED_GLASS_PANE
  parameters:
    name: "<!i><red><bold>Searching..."

undefinedItem:
  item: GOLD_INGOT
  parameters:
    name: "<!i><gold><bold>Loot"

A:                                    # Custom display item (referenced in layout as 'A')
  item: TINTED_GLASS
  parameters:
    name: "<!i><gold><bold>Fixed Item"
```

### Item Sources

Items can come from:

1. **Vanilla Minecraft**: Use standard material names (`DIAMOND`, `CHEST`, etc.)
2. **External Plugins**: Format `plugin_id:namespace:item_id`
   - CraftEngine: `craftengine:default:ruby`
   - ItemsAdder: `itemsadder:my_item`
   - Sertraline: `sertraline:namespace:id`

### Item Parameters

| Parameter | Description |
|-----------|-------------|
| `name` | Item display name (MiniMessage format) |
| `display-name` | Same as name |
| `custom-name` | Minecraft custom name |
| `item-name` | Minecraft item name (1.20.5+) |
| `item-model` / `model` | Custom model data / item model |
| `lore` | Item lore lines (list of MiniMessage strings) |

### Data Components (1.20.5+)

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

## 8. Scripting & Agents

### JavaScript

Available in loot templates, card rooms, frame crates, and everywhere `agents` is supported.

**Pre-configured variables**:

| Variable | Description |
|----------|-------------|
| `player` | Bukkit Player object |
| `mmUtil` | MiniMessage utility |
| `mmJsonUtil` | MiniMessage JSON utility |
| `Bukkit` | Bukkit server API |
| `LithiumCarbonAPI` | Plugin API |
| `Math` | Java Math class |
| `System` | Java System class |
| `Gson` | Gson JSON parser |
| `ItemStackUtil` | Item stack utility |
| `EventUtil` | Event utility |
| `ThreadUtil` | Thread/async utility |
| `PlayerUtil` | Player utility |

Plus template-specific variables like `name`, `id`, `template`, `element`, etc.

### Kether

Kether scripts are also supported alongside JS:

```yaml
agents:
  onOpen:
    ke: |
      minitell "<green>Room opened!"
      command papi "say hello world %player_name%!" as console
```

### Async Scripts

Use `async_js` or `async_ke` for scripts that should run asynchronously.

---

## 9. Commands

### Main Command: `/lithiumcarbon` (aliases: `/li2co3`, `/lc`)

| Subcommand | Permission | Description |
|------------|-----------|-------------|
| `about` | `lithiumcarbon.command.main` | Show plugin info |
| `reload` | `lithiumcarbon.command.main` | Reload all configurations |

### Manage Command: `/lithiumcarbon-manage` (aliases: `/li2co3manage`, `/lcmanage`)

| Subcommand | Permission | Description |
|------------|-----------|-------------|
| `generateItem <template> <player> <count>` | `lithiumcarbon.command.manage` | Generate loot items from template |
| `update <location>` | `lithiumcarbon.command.manage` | Force update loot instance at location |
| `updateWithoutCheck <location>` | `lithiumcarbon.command.manage` | Force update without validation |
| `updateAll [template]` | `lithiumcarbon.command.manage` | Update all instances (optionally filtered by template) |
| `spawnFrame <configId> [world x y z] [facing]` | `lithiumcarbon.command.manage` | Spawn a frame crate |
| `cardroom list` | `lithiumcarbon.command.manage` | List all card rooms |
| `cardroom info <id>` | `lithiumcarbon.command.manage` | View card room details |
| `cardroom activate <id> [player]` | `lithiumcarbon.command.manage` | Activate a card room |
| `cardroom reset <id>` | `lithiumcarbon.command.manage` | Reset a card room |
| `cardroom resetall` | `lithiumcarbon.command.manage` | Reset all card rooms |
| `cardroom status <id>` | `lithiumcarbon.command.manage` | Check card room status |

---

## 10. API

### Dependency

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.zzzyyylllty:lithiumcarbon:VERSION")
}
```

### Available API Methods

Access via `LithiumCarbonAPI`:

```kotlin
// Get loot maps
getLootMap(): Map<LootInstanceKey, LootInstance>
getLootTemplates(): Map<String, LootTemplate>
getLootDefines(): Map<String, LootDefines>
getLootCaches(): Map<LootLocation, LootTemplate>
getLootItems(): Map<Char, LootItem>
getLootItemsDef(): Map<String, LootItem>

// Force update a loot instance at a Bukkit location
updateInstance(bukkitLocation: Location)
```

### Adding Custom Script Variables

```kotlin
// Listen to LithiumCarbonCustomScriptDataLoadEvent
// Add custom bindings to the defaultData map
```

---

## 11. Events

All events extend `BukkitProxyEvent` from TabooLib.

### Loot Events

| Event | Cancellable | Description |
|-------|-------------|-------------|
| `LootInstanceCreateEvent` | No | A loot instance is created |
| `ItemSearchStartEvent` | Yes | Player begins searching an item |
| `ItemSearchCompletePreEvent` | Yes | Search complete, before item granted |
| `ItemSearchCompletePostEvent` | No | Search complete, after item granted |
| `LootElementApplyEvent` | Yes | A loot element is applied to a player |
| `LootItemGrantEvent` | Yes | An item is about to be given to player |

### Card Room Events

| Event | Cancellable | Description |
|-------|-------------|-------------|
| `CardRoomPreOpenEvent` | Yes | Before card room activation, before key consumption |
| `CardRoomOpenEvent` | No | Card room fully activated, all actions executed |
| `CardRoomPreResetEvent` | Yes | Before card room reset starts |
| `CardRoomResetEvent` | No | Card room reset complete |

### Frame Crate Events

| Event | Cancellable | Description |
|-------|-------------|-------------|
| `FrameCratePreClaimEvent` | Yes | Before frame crate item is claimed |
| `FrameCrateClaimEvent` | No | Frame crate claimed, item granted |

### Other Events

| Event | Description |
|-------|-------------|
| `LithiumCarbonReloadEvent` | Fired after plugin configuration reload |
| `LithiumCarbonCustomScriptDataLoadEvent` | Fired when JS script data bindings are initialized |

---

## Building from Source

```bash
# Normal build
./gradlew clean build
# Artifact in plugin/build/libs/

# API build (for developers)
./gradlew clean taboolibBuildApi -PDeleteCode
```

---

*LithiumCarbon — Liminal Skyline Series*  
*Designed by AkaCandyKAngel*
