# Tool, Armor, Ore, And Station Progression

> Developer-confirmed design direction from the 2026-08-10 progression pass. This document supersedes the older first-draft progression table where the two conflict. Exact mining speeds, durability, combat stats, ore frequencies, Steel ratios, Aenderite abilities, and Gold's role remain future balancing/design work.

## Core Progression

The intended main equipment progression is:

```text
Flint -> Copper -> Iron -> Steel -> Diamond -> Netherite -> Aenderite
```

Armor follows the same broad material ladder once metal armor begins:

```text
Leather -> Copper -> Iron -> Steel -> Diamond -> Netherite -> Aenderite
```

Copper and Steel both receive full normal tool and armor sets. Standard tool families from Copper onward should follow the same familiar material ladder for simplicity, including pickaxe, axe, shovel, hoe, and sword unless a later feature-specific redesign says otherwise.

Gold is deliberately outside this progression for now and will receive a separate design pass because of its Nether/Pigman/lore role.

## Design Goal

Tool tiers should change what parts of the world the player can practically access, not merely increase mining speed and durability.

The main geological progression is:

```text
primitive surface resources
-> softer stone-like materials
-> normal stone
-> deepslate
-> obsidian / Nether progression
-> Aender resources
```

Keep vanilla ore generation by default. The progression changes what geology the player can practically mine and therefore rewards caves, exposed ore, and terrain reading without requiring a separate Retold ore-generation ladder.

## Stations

Retold keeps the station vocabulary simple and recognizable.

### Crafting Table

- normal 3x3 crafting station
- crafted from planks as in vanilla
- cannot be the first required recipe because the player initially cannot obtain wood by hand

### Clay Furnace

The Clay Furnace replaces the gameplay role of the vanilla **Smoker**.

It is the early cooking station and also performs the one progression-critical metal process needed before a normal Furnace is available:

- cooks food
- can make charcoal where the relevant recipe belongs
- **smelts Copper ore into Copper**
- does not replace the normal Furnace for later ordinary ore smelting

The exact Clay Furnace crafting recipe is still to be finalized, but it must be obtainable before normal Stone/Cobblestone access.

### Furnace

The normal Furnace remains the normal Furnace.

- retains the familiar Cobblestone recipe unless later implementation constraints require a change
- handles Iron and ordinary furnace smelting
- becomes obtainable after the player has a Copper Pickaxe, because Copper can mine Stone slowly enough to obtain the Cobblestone needed for the Furnace

### Blast Furnace

The Blast Furnace remains the advanced metalworking station.

Its key Retold progression role is Steel production:

```text
Iron + Charcoal -> Steel
```

Iron and Charcoal are both consumed as part of Steel production; exact item ratios and final recipe representation remain balancing work.

Blast Furnace construction should broadly preserve the old Retold direction of requiring advanced masonry/metal materials such as bricks and iron, while staying visually/readably close to normal Minecraft crafting. Exact recipe remains to be finalized.

### Enchanting Table

Enchanting is a parallel magical progression system rather than the next ordinary furnace tier.

The currently implemented deterministic SGA enchanting system governs its behavior. Enchanting becomes useful around Iron and increasingly important later, with Diamond intentionally designed around enchantment.

### Smithing / Netherite Upgrade

Netherite should remain an upgrade to Diamond equipment rather than a separately crafted normal tool tier.

The exact station ownership between Smithing Table and Anvil can be revisited later because an older Retold note proposed folding Smithing Table functionality into the Anvil. Do not treat that old note as confirmed yet.

## Opening Progression

### 1. Spawn With No Wood Punching

Wood logs cannot be obtained by hand.

The player begins by gathering materials that do not require tools:

- break leaves to obtain Sticks
- break Gravel to obtain Flint

The first essential tool is the **Flint Multi-tool**.

### Flint Multi-tool recipe

It must fit in the player's built-in 2x2 inventory crafting grid because the Crafting Table is not obtainable yet.

Confirmed recipe:

```text
Flint  Flint
  -    Stick
```

This is the foundational recipe that breaks the circular dependency between needing a tool for wood and needing wood for a Crafting Table.

### Flint Multi-tool role

The Flint Multi-tool acts as a primitive combined:

- axe
- shovel
- pick-like tool

It should be noticeably weak/slow/low-durability so replacing it with Copper feels valuable.

Its key purposes are:

- harvest the first logs/wood
- gather dirt/gravel and primitive resources
- mine accessible/exposed Copper needed to leave the primitive tier

It is not a full conventional tool set. Flint remains a deliberately primitive stage.

Once the player can obtain logs:

```text
Flint Multi-tool
-> logs
-> planks
-> Crafting Table
```

## Copper Tier

Copper is the first proper metal and the first conventional equipment tier.

### Obtaining Copper

Retold should initially keep vanilla Copper ore generation. Early Copper therefore comes primarily from naturally exposed/accessible deposits in caves, cliffs, terrain cuts, and other places the Flint Multi-tool can reach.

Copper ore is processed in the **Clay Furnace**.

```text
accessible Copper ore
-> Clay Furnace
-> Copper ingots
```

### Copper tools and armor

Copper receives a full normal tool and armor set.

From Copper onward, standard tools should use familiar vanilla-like crafting shapes so the player learns one crafting language and reuses it for later materials.

### Copper mining capability

Copper is deliberately poor at serious Stone mining, but it **can mine normal Stone and receive Cobblestone**.

The important behavior is:

- Stone takes a noticeably long time to mine with Copper
- it still drops Cobblestone correctly
- this lets the player mine enough Stone to craft the normal Furnace
- Copper is therefore technically capable of Stone mining, while Iron makes Stone practical for ordinary tunneling and building

Copper also handles the softer early stone-like materials from the original draft, including things such as Tuff and Sandstone. The old placeholder phrase "very hardly stone" refers to this broad soft/weak stone-like category and should not be interpreted as "very hard stone."

The exact tag/block list should be decided during implementation.

### Copper-to-Iron loop

The intended early loop is:

```text
Flint Multi-tool
-> exposed Copper
-> Clay Furnace
-> Copper ingots
-> Copper Pickaxe
-> slowly mine Stone for Cobblestone
-> craft Furnace
-> smelt Iron
```

Iron may be found exposed in vanilla caves; Retold does not need a special Iron generation system merely to support this progression.

## Iron Tier

Iron is the dependable general-purpose equipment tier.

Iron ore is processed in the normal Furnace.

```text
Iron ore
-> Furnace
-> Iron ingots
```

Iron receives a full normal tool and armor set.

### Iron mining identity

The important progression change is not that Copper is absolutely forbidden from touching Stone. Instead:

- Copper can mine Stone, but painfully slowly
- **Iron is the tier where normal Stone mining becomes practical**

This makes Iron feel like a genuine expansion of player freedom: ordinary tunneling, quarrying, large-scale Cobblestone acquisition, and normal underground construction become comfortable.

Enchanting also begins to become meaningfully useful around this point, though it is not mandatory for Iron gear.

## Steel Tier

Steel is an engineered material rather than a naturally mined ore.

### Production

Confirmed conceptual recipe:

```text
Iron + Charcoal
-> Blast Furnace
-> Steel
```

Steel therefore represents infrastructure and processing rather than simply finding a rarer rock.

Exact material ratios, processing duration, and fuel behavior are future implementation/balance work.

### Steel equipment

Steel receives a full normal tool and armor set.

It should be the strongest conventional Overworld workhorse material before Diamond becomes a magical progression step.

### Steel mining identity

Steel is the tier that makes **Deepslate** practically mineable and opens the deepest Overworld geology.

This naturally gates practical Diamond access behind Steel because modern vanilla Diamond generation strongly favors deep/deepslate regions.

Keep vanilla Diamond generation initially; let geological access provide the progression gate.

## Diamond Tier

Diamond is not merely "Steel but with larger numbers."

Diamond equipment is a high-end magical material and is intentionally dependent on enchanting to reach its intended usefulness.

### Acquisition

```text
Steel
-> practical Deepslate mining
-> reach Diamond-rich deep layers
-> mine Diamond
```

### Equipment rule

Diamond tools and armor can be crafted normally, but **unenchanted Diamond equipment has very low durability**.

The intended relationship is:

- Steel = excellent reliable conventional equipment
- unenchanted Diamond = powerful material used incorrectly / impractical
- enchanted Diamond = proper high-end magical equipment

The implemented SGA enchanting system provides the knowledge/energy layer for this progression.

### Mining identity

Diamond provides access to Obsidian and therefore supports major magical/Nether infrastructure.

The exact harvest-tag boundary for Ancient Debris and other high-end blocks should be decided during implementation rather than assumed here.

## Netherite Tier

Netherite is inserted between Diamond and Aenderite, preserving its vanilla position as the premier Nether-derived upgrade tier.

Main rule:

```text
Diamond equipment
-> Nether exploration / Netherite materials
-> upgrade existing Diamond equipment
-> Netherite equipment
```

Netherite should preserve the investment already made in Diamond gear and enchantments rather than forcing the player to craft a new item from scratch.

Its exact balance relative to Steel and enchanted Diamond will be tuned later.

## Aenderite Tier

Aenderite is the final exotic material tier after Netherite.

Current code already contains the Aenderite ore/raw/ingot material foundation, but its equipment use is intentionally not yet defined.

The important design guardrail is:

> Aenderite must not become only "Netherite with larger stats."

Its final role should express Aender's identity and late-game sandbox rewards, potentially involving travel, building, reach, flight, stabilization, or world manipulation.

Whether Aenderite upgrades Netherite directly, uses a separate crafting method, or creates specialized equipment remains undecided.

## Full Current Player Progression

```text
Break leaves -> Sticks
Break Gravel -> Flint

2 Flint + 1 Stick in 2x2 grid
-> Flint Multi-tool

Flint Multi-tool
-> harvest logs
-> planks
-> Crafting Table

Find accessible/exposed Copper
-> Clay Furnace
-> Copper ingots
-> Copper tools/armor

Copper Pickaxe
-> Stone is slow but harvestable
-> collect Cobblestone
-> Furnace

Find Iron
-> Furnace
-> Iron ingots
-> Iron tools/armor
-> normal Stone mining becomes practical

Iron + Charcoal
-> Blast Furnace
-> Steel
-> Steel tools/armor
-> Deepslate becomes practical

Deep mining
-> Diamond
-> Diamond tools/armor
-> enchanting is required for practical durability/high-end use
-> Obsidian access

Nether progression
-> upgrade Diamond gear to Netherite

Stage 3 / Aender progression
-> Aenderite
-> final late-game equipment/reward design TBD
```

## Crafting Language

### Flint

Flint is exceptional because its Multi-tool must be craftable before the Crafting Table and combines several primitive functions into one item.

### Copper Through Diamond

From Copper onward, normal tools should use the familiar Minecraft crafting patterns with the tier material substituted consistently.

Examples:

```text
Pickaxe:  XXX    Axe: XX     Shovel: X     Hoe: XX     Sword: X
           S          XS             S          S             X
           S           S             S          S             S
```

Where `X` is Copper, Iron, Steel, or Diamond and `S` is the appropriate handle material.

Do not invent separate arbitrary recipe layouts for each material tier unless a later system specifically requires it.

Netherite remains an upgrade path rather than using these ordinary full-material recipes.

## Armor

Confirmed progression:

```text
Leather -> Copper -> Iron -> Steel -> Diamond -> Netherite -> Aenderite
```

Copper and Steel receive full armor sets.

Diamond armor follows the same low-unenchanted-durability rule as Diamond tools.

Netherite upgrades Diamond gear.

Aenderite armor behavior remains TBD and should be designed around Aender-specific utility rather than only defense inflation.

## Vanilla World Generation

Default direction is to retain vanilla Overworld ore generation initially.

Do not redesign Copper/Iron/Diamond generation pre-emptively merely to enforce the tool ladder.

The intended experience is that early players rely more heavily on:

- caves
- exposed ore
- cliffs and terrain cuts
- naturally accessible geology

Playtest the progression first. Only adjust ore exposure/frequency if normal seeds repeatedly create frustrating or effectively blocked progression.

## Confirmed Guardrails

- Wood logs cannot be harvested by hand at the start.
- The Flint Multi-tool is the primitive combined axe/shovel/pick-like starter tool.
- The Flint Multi-tool recipe is exactly two Flint across the top and one Stick below the right-hand Flint in the 2x2 inventory grid.
- Clay Furnace fills the Smoker role and smelts Copper.
- Furnace remains the normal Furnace and smelts Iron/ordinary furnace recipes.
- Copper Pickaxe can mine Stone and obtain Cobblestone, but does so slowly.
- Iron makes Stone mining practical.
- Steel is produced from Iron + Charcoal in the Blast Furnace.
- Steel makes Deepslate practical and thereby opens deep Diamond progression.
- Copper and Steel receive full tool and armor sets.
- Standard tool families follow the same tier ladder from Copper onward.
- Diamond equipment has very low durability until enchanted.
- Netherite sits between Diamond and Aenderite and upgrades Diamond equipment.
- Aenderite is the final exotic tier but must have an identity beyond bigger stats.
- Keep vanilla ore generation initially and adjust only if playtesting demonstrates a concrete progression problem.
- Gold is intentionally deferred to a separate design pass.

## Still Undecided

- exact Clay Furnace crafting recipe
- exact Blast Furnace crafting recipe if Retold changes vanilla's recipe
- exact Iron + Charcoal -> Steel ratios and processing details
- exact tool mining speeds and durability per tier
- exact armor/combat stats
- exact block/tag harvest lists for Flint, Copper, Iron, Steel, Diamond, Netherite, and Aenderite
- exact handling of Ancient Debris harvest level
- exact role and progression position of Gold
- whether Smithing Table remains or Netherite upgrade functionality moves into the Anvil
- exact Aenderite crafting/upgrading method and special abilities
- whether any nonstandard tools/weapons follow different material rules after the future combat/tool audit
