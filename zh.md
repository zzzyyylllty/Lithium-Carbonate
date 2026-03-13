
我之前写了一份简单的文档，但是太傻逼了，丢给AI帮我润色了一下

完整配置:

```
sample:
  refresh:
    loops:
      - period: 300
        agent:
          onRefresh:
            js:
              org.bukkit.Bukkit.broadcast(mmUtil.deserialize("<#dcaaff>[提示] <gray>所有 <yellow>" + name + "</yellow> 已刷新."))
    expire: 500
  display:
    name: "Sample Loot"
    title: "Sample Loot Menu"
    rows: 3
    layout:
      - '         '
      - '         '
      - '         '
  defines:
    region:
      type: square
      from: 'world 0 0 0'
      to: 'world 100 100 100'
      blocks:
        - CHEST
  options:
    shuffle-loot: true
    search-limit: 1
  pools:
  - rolls: 1~4
    loots:
      - items: diamond
        weight: 4
        search-time: 0.5
      - items: emerald
        weight: 1
        search-time: 1.5
```

### 1. `display` (展示)

这个部分定义了战利品箱在游戏界面中的显示方式。

```yaml
  display:
    name: "Sample Loot"
    title: "Sample Loot Menu"
    rows: 3
    layout:
      - '         '
      - '         '
      - '         '
```

*   **`name`**:    *   **描述**: 这是战利品箱的展示名称，主要用于游戏中的消息提示、日志记录或任何需要引用该战利品箱名称的地方。
    *   **示例**: "Sample Loot"
    *   **作用**: 当玩家与战利品箱进行交互（例如，打开、刷新提示）时，这个名称会显示给玩家，提供清晰的标识。

*   **`title`**:
    *   **描述**: 定义了当玩家打开战利品箱时，其图形用户界面（GUI，通常是一个自定义的菜单或箱子界面）的标题。
    *   **示例**: "Sample Loot Menu"
    *   **作用**: 直接显示在 GUI 窗口的顶部，告知玩家他们正在查看的是哪个战利品箱的内容。

*   **`rows`**:
    *   **描述**: 指定了战利品箱 GUI 的行数。在许多游戏中，GUI 通常是基于网格的，`rows` 决定了高度。
    *   **示例**: `3`
    *   **作用**: 决定了 GUI 的大小。例如，一个 3 行的 GUI 通常能显示 27 个物品槽位（假设每行 9 个槽位），这会影响玩家能一次性看到的物品数量。

*   **`layout`**:
    *   **描述**: 这是一个字符串列表，用于定义战利品箱 GUI 的具体布局。每个字符串代表 GUI 的一行。你可以在这里引用在 `items.yml` 文件中定义的“展示用物品”的 ID，以此在特定的槽位放置装饰性物品或功能性按钮。
    *   **示例**:
        ```yaml
          - '         '
          - '         '
          - '         '
        ```
        这个示例中，每个字符串都是 9 个空格，代表一个空的 3x9 网格布局，所有槽位都是空的，可以由战利品物品填充。
    *   **作用**: 提供了高度的自定义性，允许设计者创建独特的 GUI 界面。例如，可以在固定位置放置“搜索”按钮、分页按钮或背景装饰。

### 2. `defines` (定义)

这个部分用于定义哪些“方块”或“区域”可以被识别为 `sample` 类型的战利品箱。它支持多种定义方式。

```yaml
  defines:
    region:
      type: square
      from: 'world 0 0 0'
      to: 'world 100 100 100'
      blocks:
        - CHEST
    region_wg:
      type: worldguard
      region: regionname
      blocks: 
        - CHEST
    region_world:
      type: world
      world: world_.+
      regex: true
      blocks:
        - CHEST
```

*   **`region` (方形区域定义)**:
    *   **`type: square`**: 指定这是一个基于坐标的方形（立方体）区域。
    *   **`from: 'world 0 0 0'`**: 定义区域的起始点（最低 x, y, z 坐标）。格式通常是 `'世界名 x y z'`。
    *   **`to: 'world 100 100 100'`**: 定义区域的结束点（最高 x, y, z 坐标）。
        *   **注意**: 必须手动确保 `from` 是最低点，`to` 是最高点，尽管配置中提到未来版本可能自动排序，但当前需要手动。
    *   **`blocks:`**:
        *   **描述**: 一个方块类型列表。只有这些指定类型的方块在这个 `square` 区域内才会被视为战利品箱。
        *   **示例**: `- CHEST`, `- craftengine:default:palm_log`
        *   **注意**: 原版方块名称必须全部使用大写。如果需要使用箱子以外的方块（如桶、熔炉等），需要在插件的 `config` 文件中修改配置。

*   **`region_wg` (WorldGuard 区域定义)**:
    *   **`type: worldguard`**: 指定这是一个基于流行的 WorldGuard 插件定义的区域。
    *   **`region: regionname`**: WorldGuard 插件中已经创建的区域的名称。
    *   **`blocks:`**: 同上，指定此 WorldGuard 区域内哪些方块是战利品箱。

*   **`region_world` (特定世界方块定义)**:
    *   **`type: world`**: 指定这是基于特定世界和其方块的定义。
    *   **`world: world_.+`**: 指定世界名称。支持使用正则表达式进行匹配。
    *   **`regex: true`**: 如果 `world` 参数中使用了正则表达式（例如 `.+` 用于匹配任意字符），则需要将此设置为 `true`。
    *   **`blocks:`**: 同上，指定此世界中哪些方块是战利品箱。

### 3. `pools` (物品池)

这个部分定义了战利品箱中可能获得的具体物品，以及它们的掉落概率和搜索时间。一个战利品箱可以有一个或多个物品池。

```yaml
  pools:
  - rolls: 1~4
    loots:
      - items: diamond
        weight: 4
        search-time: 0.5
      - items: emerald
        weight: 1
        search-time: 1.5
```

*   **`- rolls: 1~4`**:
    *   **描述**: 这是当前物品池每次被搜索时会掉落的物品数量范围。
    *   **示例**: `1~4` 表示每次搜索会随机获得 1 到 4 件物品。

*   **`loots:`**:
    *   **描述**: 这是一个列表，包含这个物品池中所有可能的掉落物品及其属性。
    *   **`- items: diamond`**:
        *   **描述**: 物品的 ID。这可以是原版 Minecraft 物品的 ID（如 `diamond`），也可以是其他插件定义的物品 ID。
        *   **外部插件物品**: 对于像 CraftEngine 这样的外部插件物品，格式通常是 `插件ID:命名空间:物品ID`，例如 `craftengine:default:topaz`。
    *   **`weight: 4`**:
        *   **描述**: 该物品在当前物品池中的权重。权重越大，被选中的概率越高。在一个 `loots` 列表内，所有物品的权重之和用于计算每个物品的相对概率。
        *   **示例**: 钻石权重 `4`，绿宝石权重 `1`。总权重是 `4+1=5`。所以钻石的概率是 `4/5 (80%)`，绿宝石的概率是 `1/5 (20%)`。
    *   **`search-time: 0.5`**:
        *   **描述**: 玩家从战利品箱中“搜索”到该物品所需的时间，通常以秒为单位。这可以模拟一个搜索过程，增加游戏性。
        *   **示例**: 钻石 0.5 秒，绿宝石 1.5 秒。

### 4. `options` (选项)

这个部分提供了额外的全局选项，用于调整战利品箱的行为。

```yaml
  options:
    shuffle-loot: true
    search-limit: 1
```

*   **`shuffle-loot: true`**:
    *   **描述**: 如果设置为 `true`，则从物品池中选出的物品在显示给玩家时，其顺序会被随机打乱。
    *   **作用**: 增加每次打开战利品箱的惊喜感和随机性。

*   **`search-limit: 1`**:
    *   **描述**: 限制了玩家可以同时从该战利品箱中搜索（或提取）的物品数量。
    *   **示例**: `1` 表示玩家一次只能进行一项搜索任务。
    *   **作用**: 可以控制玩家获取物品的速度，防止一次性将所有物品全部取出，强制玩家进行选择或等待。

### 5. `refresh` (刷新)

这个部分定义了战利品箱中物品的刷新机制，确保它们不会被永久性地掏空。支持周期性刷新和过期刷新两种模式。

```yaml
  refresh:
    loops:
      - period: 500
        agent:
          onRefresh:
            js:
              org.bukkit.Bukkit.broadcast(mmUtil.deserialize("<#dcaaff>[提示] <gray>所有 <yellow>" + name + "</yellow> 物资箱已刷新."))
    expire: 300
```

*   **`loops` (周期性刷新)**:
    *   **描述**: 这是一个列表，可以定义多个周期性刷新规则。每个规则指定了战利品箱在一段时间后自动填充所有物品。
    *   **`- period: 500`**:
        *   **描述**: 刷新周期，以秒为单位。这里是 `500` 秒。
        *   **作用**: 每隔 `500` 秒，所有属于 `sample` 类型的战利品箱都会被刷新，内部的物品数量（或重新生成物品）会重置。
    *   **`agent.onRefresh.js:`**:
        *   **描述**: 在战利品箱进行刷新操作时执行的 JavaScript 脚本。这提供了一个强大的方法来添加自定义行为。
        *   **示例**:
            ```javascript
              org.bukkit.Bukkit.broadcast(mmUtil.deserialize("<#dcaaff>[提示] <gray>所有 <yellow>" + name + "</yellow> 物资箱已刷新."))
            ```
            这段脚本会向服务器中的所有玩家广播一条自定义消息，提示他们 `sample` 战利品箱已刷新。其中 `name` 会被替换为 `display.name` 中定义的值（如 "Sample Loot"）。

*   **`expire` (过期刷新)**:
    *   **描述**: 当一个战利品箱被玩家打开（或首次交互）后，经过指定的时间，它会自动刷新。
    *   **示例**: `300` 代表 300 秒。
    *   **作用**: 这是一种懒加载的刷新机制。只有当箱子被打开后，这个计时器才会开始。当计时器达到 `300` 秒时，如果箱子处于打开状态，或玩家再次尝试打开时，箱子就会被刷新。这种方式可以节省服务器资源，因为它不会频繁检查未被使用的箱子。

