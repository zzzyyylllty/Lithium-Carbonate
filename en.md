I write a simple doc before, but it's too fucking idiot, so I drop it to AI

Example config


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

### 1. `display`

This section defines how the loot chest appears in the game's user interface.

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

*   **`name`**:
    *   **Description**: This is the display name of the loot chest, primarily used for in-game messages, logging, or any context where the loot chest's name needs to be referenced.
    *   **Example**: "Sample Loot"
    *   **Function**: When players interact with the loot chest (e.g., opening, refresh notifications), this name will be shown to them, providing clear identification.

*   **`title`**:
    *   **Description**: Defines the title of the Graphical User Interface (GUI), typically a custom menu or chest interface, that appears when a player opens the loot chest.
    *   **Example**: "Sample Loot Menu"
    *   **Function**: Directly displayed at the top of the GUI window, informing the player which loot chest's contents they are viewing.

*   **`rows`**:
    *   **Description**: Specifies the number of rows in the loot chest's GUI. In many games, GUIs are grid-based, and `rows` determines the height.
    *   **Example**: `3`
    *   **Function**: Determines the size of the GUI. For instance, a 3-row GUI typically displays 27 item slots (assuming 9 slots per row), influencing how many items a player can see at once.

*   **`layout`**:
    *   **Description**: A list of strings used to define the specific layout of the loot chest's GUI. Each string represents a row of the GUI. You can reference IDs of "display items" defined in an `items.yml` file here to place decorative items or functional buttons in specific slots.
    *   **Example**:
        ```yaml
          - '         '
          - '         '
          - '         '
        ```
        In this example, each string consists of 9 spaces, representing an empty 3x9 grid layout where all slots are empty and can be filled by loot items.
    *   **Function**: Provides a high degree of customization, allowing designers to create unique GUI interfaces. For example, one could place "search" buttons, pagination buttons, or background decorations in fixed positions.

### 2. `defines` (Definitions)

This section is used to define which "blocks" or "regions" can be recognized as `sample` type loot chests. It supports multiple ways of definition.

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

*   **`region` (Square Region Definition)**:
    *   **`type: square`**: Specifies that this is a coordinate-based square (cuboid) region.
    *   **`from: 'world 0 0 0'`**: Defines the starting point of the region (lowest x, y, z coordinates). The format is typically `'world_name x y z'`.
    *   **`to: 'world 100 100 100'`**: Defines the ending point of the region (highest x, y, z coordinates).
        *   **Note**: You must manually ensure that `from` is the lowest point and `to` is the highest point. Although the configuration mentions future versions might auto-sort, it currently requires manual specification.
    *   **`blocks:`**:
        *   **Description**: A list of block types. Only blocks of these specified types within this `square` region will be considered loot chests.
        *   **Example**: `- CHEST`
        *   **Note**: Block names must be all uppercase. If you want to use blocks other than chests (e.g., barrels, furnaces), you usually need to add additional configuration in the plugin's `config` file.

*   **`region_wg` (WorldGuard Region Definition)**:
    *   **`type: worldguard`**: Specifies that this is a region defined using the popular WorldGuard plugin.
    *   **`region: regionname`**: The name of a pre-existing region defined in the WorldGuard plugin.
    *   **`blocks:`**: Same as above, specifying which blocks within this WorldGuard region are loot chests.

*   **`region_world` (Specific World Block Definition)**:
    *   **`type: world`**: Specifies that this definition is based on a specific world and its blocks.
    *   **`world: world_.+`**: Specifies the world name. Supports the use of regular expressions for matching.
    *   **`regex: true`**: If regular expressions (`.+` for matching any character, for example) are used in the `world` parameter, this needs to be set to `true`.
    *   **`blocks:`**: Same as above, specifying which blocks in this world are loot chests.

### 3. `pools` (Item Pools)

This section defines the specific items that can be obtained from the loot chest, along with their drop probabilities and search times. A loot chest can have one or more item pools.

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
    *   **Description**: This is the range of items that will be dropped each time this item pool is searched.
    *   **Example**: `1~4` means that 1 to 4 items will be randomly obtained per search.

*   **`loots:`**:
    *   **Description**: This is a list containing all possible drop items from this pool and their attributes.
    *   **`- items: diamond`**:
        *   **Description**: The ID of the item. This can be a vanilla Minecraft item ID (like `diamond`) or an item ID defined by another plugin.
        *   **External Plugin Items**: For items from external plugins like CraftEngine, the format is usually `plugin_ID:namespace:item_ID`, e.g., `craftengine:default:topaz`.
    *   **`weight: 4`**:
        *   **Description**: The weight of the item within the current item pool. A higher weight means a higher probability of being selected. Within a `loots` list, the sum of all item weights is used to calculate each item's relative probability.
        *   **Example**: Diamond has a weight of `4`, Emerald has `1`. The total weight is `4+1=5`. So, the probability of getting a Diamond is `4/5 (80%)`, and Emerald is `1/5 (20%)`.
    *   **`search-time: 0.5`**:
        *   **Description**: The time, typically in seconds, required for a player to "search" for and obtain this item from the loot chest. This can simulate a search process, adding to gameplay.
        *   **Example**: Diamond takes 0.5 seconds, Emerald takes 1.5 seconds.

### 4. `options`

This section provides additional global options to adjust the behavior of the loot chest.

```yaml
  options:
    shuffle-loot: true
    search-limit: 1
```

*   **`shuffle-loot: true`**:
    *   **Description**: If set to `true`, the order of items selected from the item pool will be randomized when displayed to the player.
    *   **Function**: Increases the sense of surprise and randomness each time the loot chest is opened.

*   **`search-limit: 1`**:
    *   **Description**: Limits the number of items a player can simultaneously search for (or extract) from this loot chest.
    *   **Example**: `1` means a player can only perform one search task at a time.
    *   **Function**: Can control the rate at which players acquire items, preventing them from taking all items at once and forcing them to make choices or wait.

### 5. `refresh`

This section defines the refresh mechanism for items in the loot chest, ensuring they are not permanently depleted. It supports two modes: periodic refresh and expiration refresh.

```yaml
  refresh:
    loops:
      - period: 500
        agent:
          onRefresh:
            js:
              org.bukkit.Bukkit.broadcast(mmUtil.deserialize("<#dcaaff>[Tip] <gray>All <yellow>" + name + "</yellow> supply chests have been refreshed."))
    expire: 300
```

*   **`loops` (Periodic Refresh)**:
    *   **Description**: This is a list that can define multiple periodic refresh rules. Each rule specifies that loot chests will automatically restock their items after a certain period.
    *   **`- period: 500`**:
        *   **Description**: The refresh cycle, in seconds. Here it is `500` seconds.
        *   **Function**: Every `500` seconds, all loot chests of type `sample` will be refreshed, and their internal item counts (or item generation) will be reset.
    *   **`agent.onRefresh.js:`**:
        *   **Description**: A JavaScript script executed when the loot chest performs a refresh operation. This provides a powerful way to add custom behaviors.
        *   **Example**:
            ```javascript
              org.bukkit.Bukkit.broadcast(mmUtil.deserialize("<#dcaaff>[Tip] <gray>All <yellow>" + name + "</yellow> supply chests have been refreshed."))
            ```
            This script broadcasts a custom message to all players on the server, notifying them that the `sample` loot chests have been refreshed. The `name` placeholder will be replaced with the value defined in `display.name` (e.g., "Sample Loot").

*   **`expire` (Expiration Refresh)**:
    *   **Description**: When a loot chest is opened (or first interacted with) by a player, it will automatically refresh after a specified amount of time.
    *   **Example**: `300` represents 300 seconds.
    *   **Function**: This is a lazy-loading refresh mechanism. The timer only starts when the chest is opened. When the timer reaches `300` seconds, if the chest is currently open or a player attempts to open it again, the chest will be refreshed. This method saves server resources as it doesn't frequently check unused chests.