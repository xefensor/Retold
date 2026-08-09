# Iron Golem Equipment And Villager Metalworking

> Developer-confirmed design direction from the 2026-08-09 design pass. This is a design document, not an implementation-status claim. Exact recipes, item names, equipment slots, statistics, materials, visuals, AI behavior, and balancing remain future work.

## Core Idea

Villagers are a fundamentally pacifist civilization. That means the existence of Armorer and Weaponsmith professions should not imply that villagers themselves routinely equip weapons and armor.

Instead, a major in-world reason for village metalworking is **Iron Golem equipment and maintenance**.

Villagers can still produce player-sized armor, weapons, and tools for trade, but military metalworking inside the settlement should primarily make sense as support for the village's non-villager defenders.

## Iron Golems As The Village's Armed Defenders

Iron Golems remain the main physical military force of a village.

Villagers themselves do not need to become soldiers in order for advanced defensive metalworking to exist. When a settlement becomes wealthy enough, acquires enough iron, or faces sustained danger, it may invest resources into improving its golems instead.

This fits the wider Retold settlement model:

```text
threat pressure
-> settlement prioritizes defense
-> iron/materials are reserved
-> Armorer/Weaponsmith prepares golem equipment
-> golem becomes better equipped
```

This should be a real resource decision rather than an invisible combat-stat increase.

## Golem Equipment Is Oversized

Iron Golem equipment should visually and materially communicate that it is made for a much larger creature than the player.

A player-crafted golem armor piece should therefore use **blocks of material rather than ordinary ingots** where appropriate.

For iron equipment, the basic rule is:

> Golem-scale armor and weapons are made from Iron Blocks, not Iron Ingots.

This is intentionally intuitive rather than simulation-heavy. The player should look at a huge iron chestplate and immediately understand why it requires blocks of iron.

Conceptual examples only:

- Golem Chestplate: several Iron Blocks
- Golem Leg/Lower-Body Armor: several Iron Blocks
- Golem Head Armor: several Iron Blocks
- Golem weapon or reinforced arm attachment: several Iron Blocks

Exact recipes and whether the equipment uses vanilla-armor-like recipe shapes remain undecided.

## Village Economy Impact

Golem equipment should be expensive enough to represent a meaningful settlement investment.

A poor or iron-starved village may rely on an ordinary unarmored golem.

A larger, resource-rich, or frequently attacked settlement may choose to spend stored iron on reinforced golem equipment.

This creates a natural connection between:

- mining and metal supply
- communal storage
- Armorer/Weaponsmith professions
- settlement threat memory
- village defensive priorities

Do not implement this as an arbitrary village upgrade tier. The equipment exists because the settlement has the materials, professions, infrastructure, and reason to make it.

## Profession Roles

### Armorer

The Armorer's military role is primarily connected to golem protection rather than arming villagers.

Responsibilities can include:

- producing player-sized armor for trade
- producing Iron Golem armor/plating
- maintaining or replacing damaged golem armor
- preparing defensive metal components required by village defense

### Weaponsmith

The Weaponsmith's military role is primarily connected to the settlement's defenders rather than giving weapons to ordinary villagers.

Responsibilities can include:

- producing player-sized weapons for trade
- producing large-scale Iron Golem weapons or weapon attachments if the final golem system uses them
- repairing/replacing golem weapon equipment

### Toolsmith

The Toolsmith remains primarily associated with tools, work equipment, and trade rather than village combat.

Do not invent a combat role for Toolsmith merely to make all three smith professions symmetrical.

## Player Crafting

Golem equipment should not be villager-exclusive.

If villagers can construct a physical piece of golem armor or weaponry, the player should generally be able to learn and reproduce it through normal Retold crafting/progression rules.

The defining distinction is scale:

- player-sized iron equipment uses ordinary material quantities appropriate to the player
- golem-sized iron equipment consumes Iron Blocks or similarly large material quantities

This gives the player an intuitive way to understand the cost before ever interacting with the village economy.

## Equipment Complexity

Keep the first implementation Minecraft-simple.

Do not begin with a deep RPG equipment system containing many armor slots, affixes, durability subsystems, and stat combinations.

The initial direction should be a small number of highly visible equipment choices, for example:

- basic/unarmored Iron Golem
- armored/reinforced Iron Golem
- optional golem weapon equipment if it provides a clear gameplay role

Exact slot count is undecided.

Likewise, do not automatically create golem equivalents of every player armor material. Iron is the obvious first material. Steel or another later Retold material may be considered if it serves progression, but leather/chain/diamond-style duplication should not happen without a strong reason.

## Damage And Repair

Ordinary Iron Golem body damage and golem equipment damage should remain conceptually distinct if equipment has its own durability/state.

Current direction:

- normal golem body damage may continue to use ordinary iron repair interactions where appropriate
- damaged or destroyed armor/equipment should require the material scale appropriate to that equipment
- villagers may maintain equipped golems through the normal settlement resource/logistics system

Exact repair costs and whether equipment uses durability, discrete damage states, or replacement-only behavior remain undecided.

## Threat Response

Golem equipment should connect to settlement danger without creating hidden stat buffs.

Repeated attacks, raids, villager deaths, or other sustained danger can increase the priority of spending real resources on golem defense.

A settlement with little danger may prefer to keep its iron for construction, trade, or other needs. A settlement under sustained threat may decide that reinforcing a golem is worth the cost.

This follows Retold's general rule that difficulty and world response should emerge from visible behavior, resources, and systems rather than arbitrary numerical scaling.

## Design Guardrails

- Villagers remain pacifists; ordinary villagers should not become armed soldiers merely because Weaponsmith/Armorer exist.
- The main internal military purpose of Armorer/Weaponsmith is support for Iron Golems and village defense.
- Player-sized weapons and armor can still be produced as trade goods.
- Golem equipment should use visibly large material quantities; Iron Blocks are the intended basis for iron golem-scale equipment.
- Golem equipment must consume real settlement resources.
- Threat response should affect whether the village chooses to invest in equipment, not magically grant combat bonuses.
- Players should be able to craft golem equipment through ordinary Retold progression rather than it being NPC-exclusive.
- Start with a small, readable equipment system instead of a deep RPG loadout framework.
- Do not add materials, slots, or specialized equipment solely for symmetry or content volume.

## Still Undecided

- exact golem armor pieces/equipment slots
- exact Iron Block recipes and costs
- whether golems use a separate weapon item, reinforced arms, or another simple offensive upgrade
- exact defensive/offensive effects
- whether golem equipment has durability and how it is repaired
- exact visual models/textures
- whether later materials such as steel can be used
- how villagers physically equip or replace golem equipment
- exact settlement thresholds/priorities for deciding to manufacture equipment
