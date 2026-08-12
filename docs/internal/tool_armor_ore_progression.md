# Tool, Armor, Ore, And Station Progression

> Developer-confirmed design direction from the 2026-08-10 progression pass, updated by the 2026-08-11 Steel-production choice. This document supersedes the older first-draft progression table where the two conflict. Final mining speeds, durability, combat stats, ore frequencies, Aenderite abilities, and Gold's role remain future balancing/design work.

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

Gold remains outside the required progression as an optional post-Iron sidegrade. It keeps its
vanilla identity: exceptionally fast but fragile tools, high enchantability, and armor utility for
Piglin interaction. Gold does not gate Steel or Diamond.

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

Keep vanilla ore generation by default except where natural playtesting identifies a concrete
problem. Copper is the first confirmed exception: its placement frequency is reduced while its
vein size, height distribution, and biome identities remain vanilla. The progression otherwise
changes what geology the player can practically mine and therefore rewards caves, exposed ore,
and terrain reading without requiring a separate Retold ore-generation ladder.

## Stations

Retold keeps the station vocabulary simple and recognizable.

### Crafting Table

- normal 3x3 crafting station
- crafted from planks as in vanilla
- cannot be the first required recipe because the player initially cannot obtain wood by hand

### Brick Furnace

The Brick Furnace replaces the gameplay role of the vanilla **Smoker**.

It is the early cooking station and also performs the one progression-critical metal process needed before a normal Furnace is available:

- cooks food
- can make charcoal where the relevant recipe belongs
- **smelts Copper ore into Copper**
- does not replace the normal Furnace for later ordinary ore smelting

The player must first fire Clay Balls into Bricks on a Campfire. A Campfire is crafted without a
fuel item from three Sticks and three Logs:

```text
  Stick
Stick  Stick
Log Log Log
```

Newly placed Campfires begin unlit. A bare Flint lights an unlit, non-waterlogged Campfire and is
consumed in Survival; Flint and Steel also works through its ordinary durability-based behavior.
Campfire cooking turns one Clay Ball into one Brick in 600 ticks. Eight fired Bricks in a ring then
craft the Brick Furnace:

```text
Brick  Brick  Brick
Brick    -    Brick
Brick  Brick  Brick
```

The Brick Furnace reuses the vanilla Smoker block and recipe identity. Its English block and
container name is replaced with **Brick Furnace**. The current implementation
keeps the vanilla Smoker model as a provisional visual and adds smoking recipes for burnable logs,
Raw Copper, Copper Ore, and Deepslate Copper Ore. Food retains the Smoker's normal recipe support.

### Furnace

The normal Furnace remains the normal Furnace.

- retains the familiar Cobblestone recipe unless later implementation constraints require a change
- handles Iron and ordinary furnace smelting
- becomes obtainable after the player has a Copper Pickaxe, because Copper can mine Stone slowly enough to obtain the Cobblestone needed for the Furnace

### Blast Furnace

The Blast Furnace remains the advanced metalworking station.

Its key Retold progression role is Steel production:

```text
Iron Ingot
-> Blast Furnace using ordinary fuel
-> Steel Ingot
```

The developer chose normal blasting on 2026-08-11. Iron Ingots are the recipe input and Steel
Ingots are the output at the normal 100-tick blasting duration. Charcoal works as ordinary fuel,
but the recipe does not require it specifically or consume it at a fixed 1:1 ratio; all fuels that
normally power a Blast Furnace remain valid. This intentionally accepts vanilla's fuel efficiency
instead of adding custom fuel-slot logic.

The Blast Furnace keeps its vanilla crafting recipe: five Iron Ingots, one Furnace, and three
Smooth Stone. Requiring both the Furnace and Furnace-made Smooth Stone preserves the station
progression without adding another custom recipe.

### Enchanting Table

Enchanting is a parallel magical progression system rather than the next ordinary furnace tier.

The currently implemented deterministic SGA enchanting system governs its behavior. Enchanting becomes useful around Iron and increasingly important later, with Diamond intentionally designed around enchantment.

### Smithing / Netherite Upgrade

Netherite should remain an upgrade to Diamond equipment rather than a separately crafted normal tool tier.

For the current progression, Netherite keeps vanilla's Smithing Table and Netherite Upgrade Smithing
Template workflow. An older Retold note proposed folding Smithing Table functionality into the
Anvil, but that is not part of the current implementation direction and may only be reconsidered
in a later station-design pass.

## Opening Progression

### 1. Spawn With No Wood Punching

Wood logs cannot be obtained by hand.

The player begins by gathering materials that do not require tools:

- break leaves to obtain Sticks
- break Gravel to obtain Flint

All blocks in the standard `minecraft:leaves` tag receive a supplemental 20% chance to drop 1–2
Sticks. Fortune adds five percentage points per level (25%/30%/35% for Fortune I/II/III). This is
independent of the leaf's ordinary loot-table Stick roll, so the vanilla drop remains possible too.
Shears and Silk Touch do not receive the supplemental drop and continue harvesting the leaf block
normally. The values are provisional until the natural opening loop is playtested.

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

The first implemented balance uses 48 durability, mining speed 2.0, attack damage 1.0, and attack
speed -2.8. Its data-driven mining list combines normal axe and shovel blocks with ordinary Copper
Ore, Sandstone variants, Tuff, and Calcite. It cannot harvest normal Stone, Deepslate Copper Ore,
or blocks that require Iron or Diamond. These values and the exact soft-block list are provisional
until the natural opening loop is playtested. This current list is the retained working baseline;
do not add or remove blocks without concrete survival-play evidence.

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

Following developer cave-play feedback on 2026-08-11, both ordinary and Dripstone Copper placed
features make six attempts per chunk instead of vanilla's sixteen. Ordinary veins retain size 10
and Dripstone Cave veins retain size 20, along with the vanilla -16-to-112 triangular height
distribution. Early Copper therefore still comes primarily from naturally exposed deposits in
caves, cliffs, terrain cuts, and other places the Flint Multi-tool can reach, but it should no
longer cover cave walls continuously.

Copper ore is processed in the **Brick Furnace**.

```text
accessible Copper ore
-> Brick Furnace
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

The first implemented balance applies 25% of the Copper Pickaxe's otherwise calculated mining
speed specifically to normal Stone. Other Copper mining behavior remains vanilla until the later
tier-wide balance pass.

### Copper-to-Iron loop

The intended early loop is:

```text
Flint Multi-tool
-> exposed Copper
-> Brick Furnace
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

Implemented production:

```text
Iron Ingot
-> Blast Furnace
-> Steel Ingot
```

Steel therefore represents infrastructure and processing rather than simply finding a rarer rock.

The recipe uses normal blasting at 100 ticks. Charcoal is an ordinary valid fuel rather than a
second recipe ingredient, so other vanilla Blast Furnace fuels also work and each fuel processes
its normal number of items.

### Steel equipment

Steel receives a full normal tool and armor set.

It should be the strongest conventional Overworld workhorse material before Diamond becomes a magical progression step.

The provisional tool material uses 750 durability, mining speed 7.0, attack bonus 2.5, and
enchantability 12. The implemented set contains Pickaxe, Axe, Shovel, Hoe, Sword, and Spear. The
Steel Spear follows an interpolated Iron-to-Diamond attack curve and temporarily references the
vanilla Iron Spear visuals.

The provisional armor material uses durability multiplier 25, enchantability 12, toughness 1,
and defenses of 3 Helmet, 7 Chestplate, 6 Leggings, and 3 Boots. Until a Steel art direction is
approved, inventory and equipped models deliberately reference vanilla Iron visuals without
copying or modifying Minecraft textures.

These current Steel tool and armor values are the retained working baseline. Do not change them
pre-emptively; tune them only from concrete natural-play results comparing Iron, Steel, and
enchanted Diamond.

### Steel mining identity

Steel is the tier that unlocks **Deepslate** harvesting and opens the deepest Overworld geology.

Copper and Iron Pickaxes break Deepslate-family blocks at 25% of their otherwise calculated speed
but receive no drops. Wooden, Stone, Gold, Copper, Iron, and Flint tools all treat the data-driven
Steel-required list as incorrect for drops. That list contains the Deepslate tier—natural
Deepslate, its construction variants, and Deepslate ores—plus normal Diamond Ore. Steel mines and
harvests Deepslate at full speed and is the first tier that can harvest either Diamond Ore variant.
It still cannot harvest Obsidian, preserving Diamond's next access step. Diamond and Netherite
retain Steel-unlocked harvesting.

This hard-gates all mined Diamond access behind Steel.

Keep vanilla Diamond generation initially; let geological access provide the progression gate.

## Diamond Tier

Diamond is not merely "Steel but with larger numbers."

Diamond equipment is a high-end magical material and is intentionally dependent on enchanting to reach its intended usefulness.

### Acquisition

```text
Steel
-> unlock Deepslate harvesting
-> reach Diamond-rich deep layers
-> mine Diamond
```

### Equipment rule

Diamond tools and armor can be crafted normally, but **unenchanted Diamond equipment has very low durability**.

The developer chose dynamic durability on 2026-08-11:

- unenchanted Diamond Sword, Shovel, Pickaxe, Axe, Hoe, and Spear have 64 maximum durability
- unenchanted Diamond player armor uses durability multiplier 6: Helmet 66, Chestplate 96,
  Leggings 90, and Boots 78
- unenchanted Diamond Horse and Nautilus Armor use the BODY/Chestplate value of 96 durability;
  enchanting either restores its full 528 durability
- while any enchantment is present, including a curse as the only enchantment, the item immediately
  uses its full vanilla Diamond durability
- removing every enchantment, including through a Grindstone, immediately restores the fragile
  maximum
- if the preserved damage value already exceeds that fragile maximum, the effective maximum is
  temporarily `damage + 1`, leaving one final use instead of creating an already-broken stack
- re-enchanting the item restores the full maximum again without changing its preserved damage

This rule is data-driven through separate Retold Diamond tool and armor tags. The armor tag covers
player armor plus Diamond Horse and Nautilus Armor, which Retold makes damageable because vanilla
animal armor does not otherwise have durability.

The intended relationship is:

- Steel = excellent reliable conventional equipment
- unenchanted Diamond = powerful material used incorrectly / impractical
- enchanted Diamond = proper high-end magical equipment with full vanilla durability

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
-> Brick Furnace
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

Iron Ingot + ordinary Blast Furnace fuel
-> Blast Furnace
-> Steel Ingot
-> Steel tools/armor
-> Deepslate harvesting unlocks

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

Wolf Armor and all Horse and Nautilus Armor materials support the same enchantment compatibility
set as a player Chestplate. Each animal armor uses its matching material's enchantability value;
Wolf Armor uses the Armadillo Scute material value. Diamond Horse and Nautilus Armor follow
Diamond's fragile-until-enchanted durability rule at 96/528 durability and receive BODY-slot wear
from non-armor-bypassing hits. Protection-family enchantments affect equipped animal
armor: the normal living-entity pipeline handles Horse and Nautilus health damage, while Retold
applies the same vanilla enchantment calculation before Wolf Armor converts a protected hit into
durability loss. Fire Protection reduces matching damage and burning duration; like a player
Chestplate, it does not make the animal visually fireproof. Non-Diamond Horse and Nautilus armor
retains vanilla's indestructible behavior.

Netherite upgrades Diamond gear.

Aenderite armor behavior remains TBD and should be designed around Aender-specific utility rather than only defense inflation.

## Overworld Ore Generation

Default direction is to retain vanilla Overworld ore generation until playtesting identifies a
specific problem. The confirmed Copper-density complaint is the first such adjustment.

Ordinary and large Copper placed features use six attempts per chunk instead of sixteen. Their
configured vein sizes and height range remain unchanged. Iron and Diamond generation remain
vanilla; do not redesign them pre-emptively merely to enforce the tool ladder.

The intended experience is that early players rely more heavily on:

- caves
- exposed ore
- cliffs and terrain cuts
- naturally accessible geology

Continue testing fresh seeds, cave exposure, and chunk borders. Tune the provisional six-attempt
Copper rate again only from concrete natural-world results.

## Confirmed Guardrails

- Wood logs cannot be harvested by hand at the start.
- The Flint Multi-tool is the primitive combined axe/shovel/pick-like starter tool.
- The Flint Multi-tool recipe is exactly two Flint across the top and one Stick below the right-hand Flint in the 2x2 inventory grid.
- Vanilla Wooden and Stone axe, hoe, pickaxe, shovel, spear, and sword recipes are removed so they cannot bypass Copper progression.
- Spears follow Flint, Copper, Iron, Steel, and Diamond; Gold remains an optional fast, fragile side grade.
- Bonus chests provide one Flint Multi-tool, safe Village smith chests stop at Copper equipment and at most two Iron Ingots, and no player-specific progression state controls shared loot.
- Smith equipment trades unlock Copper at Apprentice for 8–12 Emeralds, Iron at Expert for 24–32, and unenchanted plus rarer enchanted Diamond at Master for 48–64. The Wandering Trader may sell its rare enchanted Iron Pickaxe for roughly 48 Emeralds after its enchantment surcharge.
- Campfires use three Sticks and three Logs without Coal or Flint, begin unlit, and can be lit by consuming bare Flint or by using Flint and Steel durability.
- Campfire cooking fires Clay Balls into Bricks.
- Brick Furnace fills the Smoker role, smelts Copper, and is crafted from eight Bricks in a ring.
- Furnace remains the normal Furnace and smelts Iron/ordinary furnace recipes.
- Copper Pickaxe can mine Stone and obtain Cobblestone, but does so slowly.
- Iron makes Stone mining practical.
- Steel is produced by blasting Iron Ingots; Charcoal is an ordinary valid fuel rather than a required second ingredient.
- Pre-Steel tools cannot harvest Deepslate-family blocks, Deepslate ores, or normal Diamond Ore; Steel unlocks both Diamond Ore variants.
- Ancient Debris remains in the Diamond harvest tier.
- Copper and Steel receive full tool and armor sets.
- Standard tool families follow the same tier ladder from Copper onward.
- Diamond equipment has very low durability until enchanted.
- Curses count as enchantments for Diamond durability, including curse-only equipment.
- Diamond durability is dynamic: removing every enchantment makes tagged Diamond tools, player armor, Horse Armor, and Nautilus Armor fragile again.
- Netherite sits between Diamond and Aenderite and upgrades Diamond equipment.
- Netherite currently keeps the vanilla Smithing Table and upgrade-template workflow.
- Aenderite is the final exotic tier but must have an identity beyond bigger stats.
- Keep vanilla Iron and Diamond generation; Copper is the confirmed exception at six vein attempts per chunk with vanilla vein sizes.
- Gold is an optional post-Iron sidegrade and does not gate Steel or Diamond.
- Mending is excluded from new random loot and Librarian trades, but remains registered and functional on existing or command/Creative-created items.

## Still Undecided

- natural-play validation and any evidence-based tuning of the retained provisional Flint, Copper, Steel, and unenchanted Diamond tool values
- natural-play validation and any evidence-based tuning of the retained provisional Steel and unenchanted Diamond armor/combat values
- natural-play validation and any evidence-based tuning of the retained Flint and Steel-tier harvest lists
- exact Aenderite crafting/upgrading method and special abilities
- whether later nonstandard tools/weapons follow different material rules after the future combat/tool audit
