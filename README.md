# MaxTools

MaxTools is a tool evolution plugin for modern Paper servers. It tracks mining progress on configured tools, unlocks milestone rewards, applies special abilities, displays progression in item names and lore, provides GUI panels for players and administrators, and can announce important unlocks through Discord webhooks.

## Main Features

- Tool evolution based on mined block progress.
- Configurable tracked tools, milestones, enchantment rewards, and special abilities.
- Persistent tool data stored on the item through Bukkit persistent data.
- SQLite registry for custom MaxTools tools, including administrative purge support.
- Automatic sync for older or outdated evolved tools when players join or use `/met sync`.
- Progress display in the tool name with configurable formats.
- Managed evolution lore with tiers, progress bars, abilities, cooldowns, and enchantment lines.
- GUI hub, milestone tree, milestone details, ability status panel, tool statistics panel, and admin preview menu.
- English and Spanish language files.
- Optional language profile sync for `config.yml` and `menus.yml`.
- Discord webhook notifications for milestone unlocks, ability unlocks, and test messages.
- Optional update checks and bStats metrics.
- Anti-farm block counting rules for player-placed blocks, wrong tools, blacklist, and whitelist.

## Compatibility

- Main plugin class: `org.zkaleejoo.MaxTools`
- Bukkit API version declared by the plugin: `1.21`
- Root command: `/maxtools`
- Command alias: `/met`
- Runtime libraries shaded into the plugin include bStats and SQLite JDBC.

## How Tool Evolution Works

MaxTools only tracks materials listed under `tracked-tools` in `evolution.yml`. By default, pickaxes, axes, and shovels from wood through netherite are tracked.

When a player breaks a valid block with a tracked tool:

1. The plugin checks that the item is a valid tracked tool.
2. The plugin checks that the block should count according to `counting`.
3. The tool is registered as a MaxTools custom tool if it does not already have a custom tool id.
4. The mined block counter increases, up to the highest configured milestone.
5. The display name and managed lore are refreshed.
6. Newly reached milestones are applied.
7. Unlocked block-break abilities may activate.
8. Passive or tick abilities continue to run while the player holds the tool.

Player-placed blocks are tracked and forgotten after a successful break, so they can be excluded from evolution progress and ability abuse paths.

## Default Tracked Tools

The default tracked tools are:

- `WOODEN_PICKAXE`, `STONE_PICKAXE`, `IRON_PICKAXE`, `GOLDEN_PICKAXE`, `DIAMOND_PICKAXE`, `NETHERITE_PICKAXE`
- `WOODEN_AXE`, `STONE_AXE`, `IRON_AXE`, `GOLDEN_AXE`, `DIAMOND_AXE`, `NETHERITE_AXE`
- `WOODEN_SHOVEL`, `STONE_SHOVEL`, `IRON_SHOVEL`, `GOLDEN_SHOVEL`, `DIAMOND_SHOVEL`, `NETHERITE_SHOVEL`

Use exact Bukkit `Material` enum names when editing `tracked-tools`.

## Block Counting Rules

The `counting` section in `evolution.yml` controls which blocks count.

| Setting | Default | Description |
| --- | --- | --- |
| `require-preferred-tool` | `true` | Requires the correct preferred tool for the broken block. |
| `strict-tool-category-match` | `true` | Requires the block to match the tool category, such as pickaxe, axe, or shovel. |
| `whitelist-materials` | `[]` | If not empty, only these block materials can count. |
| `blacklist-materials` | `[]` | Materials that never count. If the key exists but is empty, built-in anti-farm defaults are used. |

## Default Milestones

Milestones are configured in `evolution.yml`. A milestone activates when the tool reaches at least the configured block amount.

| Blocks | Default Reward |
| ---: | --- |
| 100 | `EFFICIENCY` level 3 |
| 300 | Unlocks `self-repair` |
| 750 | `DURABILITY` level 2 |
| 1000 | Unlocks `telepathy` |
| 1200 | `EFFICIENCY` level 4 |
| 2000 | Unlocks `auto-smelt` |
| 2500 | Unlocks `xp-boost` |
| 3000 | `FORTUNE` level 4 |
| 3100 | `DURABILITY` level 4 |
| 3500 | `EFFICIENCY` level 6 |
| 5000 | Unlocks `vein-miner` |
| 6000 | `FORTUNE` level 5 |
| 9000 | Unlocks `drill` |

Milestones support:

- `blocks`: required mined blocks.
- `enchantment`: Bukkit enchantment name or supported alias.
- `level`: enchantment level.
- `unlock-abilities`: list of ability ids from `special-abilities`.

## Special Abilities

Abilities are configured under `special-abilities` in `evolution.yml`.

| Ability id | Type | Trigger | Default behavior |
| --- | --- | --- | --- |
| `self-repair` | `SELF_REPAIR` | `WALK_DISTANCE` | Repairs durability after the player walks enough blocks with the tool in hand. |
| `auto-smelt` | `AUTO_SMELT` | Block break | Converts supported ore/raw drops into smelted results. |
| `telepathy` | `TELEPATHY` | Block break | Sends drops directly into the player's inventory and drops overflow at the player location. |
| `drill` | `DRILL` | Block break | Breaks an area around the original block for diamond and netherite pickaxes or shovels. |
| `hammer` | `HAMMER` | Block break | Registered internally as an area-break ability for high-tier shovels if configured. |
| `vein-miner` | `VEIN_MINER` | Block break | Breaks connected blocks of the same material, usually ores. |
| `xp-boost` | `XP_BOOST` | Block break | Multiplies natural block experience drops. |
| `haste` | `HASTE` | `TICK` | Gives Haste while holding the evolved tool. |
| `momentum` | `MOMENTUM` | Block break and `TICK` | Builds temporary Haste stacks while the player keeps breaking blocks. |
| `luck-surge` | `LUCK_SURGE` | Block break | Temporarily multiplies compatible block drops during a short active window. |
| `saturation-pulse` | `SATURATION_PULSE` | Block break | Restores hunger and may apply Saturation when the player reaches full hunger. |

Common ability options:

| Option | Description |
| --- | --- |
| `type` | Internal ability type. Must match a supported type. |
| `enabled` | Enables or disables the ability globally. |
| `trigger` | Activation trigger, such as `BLOCK_BREAK`, `WALK_DISTANCE`, or `TICK`. |
| `chance` | Proc chance from `0.0` to `1.0`. |
| `amount` | Main power value. Meaning depends on the ability. |
| `distance-blocks` | Walking distance needed for `WALK_DISTANCE` abilities. |
| `cooldown-seconds` | Cooldown between activations. |
| `require-main-hand` | Requires the tool to remain in the main hand for supported abilities. |
| `compatible-with-mending` | If `false`, the ability will not work on tools with Mending. |
| `material-whitelist` | Restricts the ability to specific block materials. |
| `max-multiplier` | Drop multiplier cap for abilities such as `luck-surge`. |
| `max-stacks` | Maximum stacking value for abilities such as `momentum`. |
| `stack-window-ms` | Time window used by stack-based abilities. |
| `per-stack-amplifier` | Effect strength added per stack for stack-based abilities. |

## Progress Display

The `progress-display` section controls the text appended to evolved tool names.

| Setting | Description |
| --- | --- |
| `enabled` | Enables or disables progress text in the display name. |
| `format` | Format used before the final milestone. |
| `completed-format` | Format used when the final milestone is reached. |
| `refresh-base-name-on-rename` | If `true`, later renames can become the new base name. If `false`, the original base name is preserved. |

Supported placeholders:

- `{current}`: current mined block count.
- `{target}`: current milestone target.
- `{unit}`: progress unit from the language file, default `Blocks`.

## Evolution Lore

The `evolution-lore` section in `config.yml` controls generated lore lines.

Supported sections:

- `header`: optional top and bottom separator lines.
- `tier`: rarity/tier line. Can use milestone count or percent mode.
- `progress`: progress bar, percent, current count, and target.
- `abilities`: unlocked and locked ability status lines.
- `enchantments`: current enchantment lines.

Important lore placeholders:

- `{tier}`: resolved tier name.
- `{bar}`: rendered progress bar.
- `{percent}`: progress percent.
- `{current}`: current block count.
- `{target}`: current target.
- `{ability}`: display ability name.
- `{ability_state}`: active, cooldown, or blocked label.
- `{cooldown}`: cooldown text.
- `{enchant}`: display enchantment name.
- `{key}`: raw enchantment key.
- `{level}`: enchantment level.

## GUIs and Menus

Menus are configured in `menus.yml`.

| Menu | Purpose |
| --- | --- |
| Evolution hub | Entry menu for the held tracked tool. |
| Milestone tree | Shows locked, current, and unlocked milestones with paging. |
| Milestone detail | Shows exact milestone requirement and reward. |
| Abilities menu | Shows ability status, cooldowns, and required blocks. |
| Tool stats | Shows progress summary, ability stats, completion bars, and total activations. |
| Language menu | Lets admins switch between English and Spanish. |
| Admin preview | Shows localization, progress bars, ability states, and lore previews. |

Menu settings include titles, inventory sizes, slots, materials, custom model data, lore, sounds, colors, state variants, rarity variants, and progress variants.

## Commands

All commands use `/maxtools` or `/met`.

| Command | Aliases | Permission | Sender | Description |
| --- | --- | --- | --- | --- |
| `/met toolinfo [gui|text|legacy]` | none | `maxtools.toolinfo` | Player | Shows progress for the held tracked tool. GUI mode opens the evolution hub; text mode sends a chat summary. |
| `/met menu` | `/met hub`, `/met gui` | `maxtools.menu` | Player | Opens the evolution GUI hub for the held tracked tool. |
| `/met reload` | none | `maxtools.admin.reload` | Any | Reloads config, menus, language files, evolution data, Discord webhook state, update checks, and bStats state. |
| `/met preview` | `/met adminpreview` | `maxtools.admin.preview` | Player | Opens the admin preview GUI. |
| `/met discordtest` | `/met dctest` | `maxtools.admin.discordtest` | Any | Queues a Discord webhook test message. |
| `/met testtool <material> [ability|all] [level]` | `/met testtools` | `maxtools.admin.testtool` | Player | Creates a registered test tool with optional ability unlocks and block progress. |
| `/met cleartesttool [id]` | `/met cleartest`, `/met untesttool`, `/met cleartestool`, `/met cleartestools` | `maxtools.admin.cleartesttool` | Player | Removes a held test tool or removes a registered test tool id. |
| `/met admintoolsremove confirm` | `/met adminremove`, `/met purgetools`, `/met toolspurge` | `maxtools.admin.admintoolsremove` | Any | Removes MaxTools custom tools from loaded inventories/entities and clears the SQLite custom tool registry. |
| `/met lang` | none | `maxtools.admin.lang` | Player | Opens the language selector GUI. |
| `/met sync` | none | `maxtools.admin.sync` | Player | Synchronizes the held evolved tool with the current `evolution.yml` milestones and abilities. |

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `maxtools.admin` | `op` | Full access to MaxTools administrative commands and child permissions. |
| `maxtools.toolinfo` | `true` | Allows players to use `/met toolinfo`. |
| `maxtools.menu` | `true` | Allows players to open `/met menu`, `/met hub`, or `/met gui`. |
| `maxtools.admin.reload` | `op` | Allows `/met reload`. |
| `maxtools.admin.preview` | `op` | Allows `/met preview` and `/met adminpreview`. |
| `maxtools.admin.discordtest` | `op` | Allows `/met discordtest` and `/met dctest`. |
| `maxtools.admin.testtool` | `op` | Allows `/met testtool` and `/met testtools`. |
| `maxtools.admin.cleartesttool` | `op` | Allows `/met cleartesttool` and its aliases. |
| `maxtools.admin.admintoolsremove` | `op` | Allows `/met admintoolsremove confirm` and its aliases. |
| `maxtools.admin.lang` | `op` | Allows `/met lang`. |
| `maxtools.admin.sync` | `op` | Allows `/met sync`. |

## Discord Webhooks

Discord notifications are configured under `discord` in `config.yml`.

Main options:

- `enabled`: enables Discord webhook usage.
- `webhook-url`: Discord webhook URL.
- `server-name`: display value for `{server}`.
- `username`: optional webhook username override.
- `avatar-url`: optional webhook avatar override.
- `allowed-mentions.parse`: controls whether templates may ping `everyone`, `roles`, or `users`.
- `allowed-mentions.replied-user`: controls replied-user pings.
- `queue.max-pending`: maximum pending webhook tasks in memory.
- `events.milestone-unlocked`: enables milestone notifications.
- `events.ability-unlocked`: enables ability notifications.
- `templates`: legacy text templates used when embeds are disabled.
- `embeds`: rich embed templates for milestone, ability, and test messages.

Discord template and embed placeholders:

- `{player}`: player name.
- `{player_uuid}`: player UUID.
- `{tool}`: localized tool name.
- `{tool_type}`: raw Bukkit material name.
- `{blocks}`: block count.
- `{ability}`: ability id or display value.
- `{reward}`: milestone reward summary.
- `{timestamp}`: generated timestamp text.
- `{server}`: configured server display name.
- `{server_software}`: server software name.
- `{plugin}`: plugin name.

## Language System

The active language is controlled by `general.language` in `config.yml`.

Bundled language files:

- `messages_en.yml`
- `messages_es.yml`

When `general.language-profile-sync` is `true`, changing language from `/met lang` also swaps active `config.yml` and `menus.yml` from language profiles under:

- `plugins/MaxTools/language_profiles/config_en.yml`
- `plugins/MaxTools/language_profiles/menus_en.yml`
- `plugins/MaxTools/language_profiles/config_es.yml`
- `plugins/MaxTools/language_profiles/menus_es.yml`

Before switching, the plugin snapshots the current language profile so custom edits are preserved per language.

## Message Placeholders

Language messages support these placeholders where used:

- `{version}`: version from update messages.
- `%blocks%`: milestone block requirement.
- `%enchant%`: localized enchantment name.
- `%level%`: enchantment level.
- `%ability%`: ability id.
- `%usage%`: current block count.
- `%special%`: enabled or disabled word.
- `%abilities%`: comma-separated unlocked abilities.
- `{id}`: test tool id.
- `{material}`: material name.
- `{usage}`: usage/block count in test tool messages.
- `{abilities}`: ability list in test tool messages.
- `{removed}`: loaded custom items removed by purge.
- `{database}`: SQLite records deleted by purge.
- `{milestones}`: milestone count applied by sync.

## Menu Placeholders

Common menu placeholders:

- `{tool_name}`
- `{current_level}`
- `{next_level}`
- `{usage}`
- `{target}`
- `{unlocked_milestones}`
- `{total_milestones}`
- `{unlocked_abilities}`
- `{total_abilities}`
- `{page}`
- `{pages}`
- `{blocks}`
- `{state}`
- `{reward}`
- `{next_reward}`
- `{remaining_blocks}`
- `{tier}`
- `{abilities}`
- `{enchantment}`
- `{level}`
- `{milestones_unlocked}`
- `{milestones_total}`
- `{milestones_percent}`
- `{milestones_bar}`
- `{abilities_unlocked}`
- `{abilities_total}`
- `{abilities_percent}`
- `{abilities_bar}`
- `{activations_total}`
- `{ability}`
- `{ability_activations}`
- `{ability_percent}`
- `{cooldown}`
- `{required_blocks}`
- `{unlocked}`

Admin preview placeholders:

- `{wooden_pickaxe}`
- `{diamond_axe}`
- `{netherite_shovel}`
- `{efficiency}`
- `{unbreaking}`
- `{fortune}`
- `{sample_1_percent}`
- `{sample_1_bar}`
- `{sample_2_percent}`
- `{sample_2_bar}`
- `{sample_3_percent}`
- `{sample_3_bar}`
- `{unit}`
- `{self_repair}`
- `{auto_smelt}`
- `{telepathy}`
- `{active}`
- `{blocked}`
- `{cooldown_seconds}`
- `{progress_line}`
- `{enchant_line}`

## Internal Item Data

MaxTools stores evolution state on items through persistent data keys. Important state includes:

- `blocks_mined`
- `special_unlocked`
- `unlocked_abilities`
- `managed_lore_lines`
- `base_display_name`
- `last_progress_display_name`
- `last_applied_milestone`
- `last_lore_progress_usage`
- `last_lore_progress_percent`
- `last_lore_progress_target`
- `ability_activations_total`
- `custom_tool_id`
- `test_mode`
- `test_tool_id`
- `ability_activations_<ability>`
- `ability_cooldown_<ability>`
- `ability_distance_<ability>`

These keys let the plugin keep progress, cooldowns, ability statistics, lore ownership, and custom tool registration attached to each evolved tool.

## SQLite Custom Tool Registry

The plugin creates `custom_tools.db` inside the MaxTools data folder.

It stores:

- Tool id.
- Material.
- Owner UUID.
- Owner name.
- Whether the tool is a test tool.
- Created timestamp.
- Last seen timestamp.

This registry helps identify valid MaxTools custom tools and allows `/met admintoolsremove confirm` to revoke all registered tools.

If a custom tool is no longer registered after a purge, the plugin removes it from loaded inventories, ender chests, opened inventories, dropped items, item frames, and nested container items when those locations are scanned.

## Update Checks and bStats

`general.update-check` controls update checks. When enabled, the plugin checks for a newer version periodically and notifies the console. Operators with `maxtools.admin` can also receive update notices on join.

`general.bstats` controls bStats metrics. Reloading the plugin state through `/met reload` also refreshes the bStats state.

## Main Configuration Files

| File | Purpose |
| --- | --- |
| `config.yml` | General language, prefix, tool info mode, update checks, bStats, Discord, evolution lore, and localized names. |
| `evolution.yml` | Tracked tools, block counting rules, progress display, milestones, and ability catalog. |
| `menus.yml` | GUI titles, layout, slots, materials, lore, sounds, colors, variants, and admin preview. |
| `messages_en.yml` / `messages_es.yml` | Command messages, player feedback, tool names, and enchantment names. |
| `language_profiles/*` | Runtime profile copies used by the language switcher. |
| `custom_tools.db` | SQLite registry for MaxTools custom tools. |
| `player-placed-blocks.yml` | Registry used to track player-placed blocks for counting protection. |

## Notes for Server Owners

- Normal players only need `maxtools.toolinfo` and `maxtools.menu`, both enabled by default.
- Administrative commands are protected with `maxtools.admin.*` permissions and default to operators.
- Use `/met sync` after changing milestones if an existing held tool should be brought in line with the current `evolution.yml`.
- Use `/met admintoolsremove confirm` carefully. It is intentionally destructive and removes registered custom tools from loaded server state and the database.
- Keep ability material whitelists restrictive for powerful abilities such as `vein-miner`, `drill`, and `luck-surge`.
- If Discord notifications do not send, check `discord.enabled`, `discord.webhook-url`, and `/met discordtest`.
