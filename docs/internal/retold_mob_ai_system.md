# Retold Mob AI System

> Internal documentation. This file is meant for human developers and future AI coding agents. Sections named "Design rule", "Adding A New Mob Behavior", "Validation Checklist", and "Refactor Rules" are also implementation guidance for AI-assisted work.

This document describes the current Retold mob AI design and the technical structure that implements it.
It is meant to be the main reference before adding behavior, debugging behavior, or doing more performance work.

Whole-mod architecture reference: [`retold_mod_system.md`](retold_mod_system.md).
Design risks: [`retold_design_risks.md`](retold_design_risks.md).
Known issues: [`retold_issues.md`](retold_issues.md).
Per-mob performance baseline: [`mob_tps_benchmark.md`](mob_tps_benchmark.md).
Consolidated 2026-08-03 work record: [`mob_ai_work_report_2026-08-03.md`](mob_ai_work_report_2026-08-03.md).

## Goals

Retold mob AI is built around this model:

```text
behavior = species + faction + profile + current state + nearby world situation
```

The goal is not to replace every vanilla behavior. The goal is to own the parts that need consistent Retold logic:

- hunger, feeding, foraging, hunting, fleeing, regrouping, home/range return
- faction relationships and target ownership
- territory warning and reputation
- invalid target cleanup for creative and spectator players
- performance-safe scheduling, caching, and work budgets

Vanilla behavior can still run where it does not break Retold ownership. Bosses and special mobs mostly keep their vanilla logic.

## Design Layers

### Species

Species is the exact entity type path, such as `cow`, `wolf`, `piglin`, or `guardian`.

Technical owner:

- `RetoldMobRules.getEntityTypePath`
- `RetoldAiTickContext.entityPath`

Species is used for small exact differences, such as:

- foxes carrying food home
- chickens using roost behavior
- rabbits hiding at warrens
- dolphins using pod behavior
- piglins belonging to Nether Remnants

### Profile

Profile describes how a mob lives day to day. It controls life behavior, not diplomacy.

Technical owner:

- `RetoldMobProfileType`
- `RetoldMobProfile`
- `RetoldMobProfiles`
- `RetoldMobProfileReloadListener`
- `data/retold/mob_profiles/*.json`
- `RetoldMobRules`

Profiles are server datapack data. Each file owns one entity and can be replaced independently
by a higher-priority datapack using the same resource path. `/reload` validates all definitions
and atomically publishes a new immutable profile snapshot. Invalid or duplicate entity entries
do not replace the last valid snapshot.

Example `data/retold/mob_profiles/wolf.json`:

```json
{
  "entity": "minecraft:wolf",
  "profile": {
    "type": "pack_predator",
    "managed": true,
    "predator": true,
    "pack_social": true,
    "territory_guard": false,
    "hunger_interval_ticks": 460,
    "eat_threshold": 18,
    "hunt_threshold": 36
  }
}
```

Boolean fields default to `false`, `hunger_interval_ticks` defaults to `0`, and both thresholds
default to `101` (disabled). Profile type `none` is reserved for Java fallback behavior.

Examples:

| Profile | Mobs | Main purpose |
| --- | --- | --- |
| `HUNGRY_GRAZER` | cows, sheep, goats, horses, llamas, camels | graze, use herd range, flee/regroup |
| `SMALL_FORAGER` | pigs, chickens, rabbits | forage, roost/warren/rest, flee |
| `PACK_PREDATOR` | wolves | pack hunt, den, return, defend |
| `SOLO_OPPORTUNIST` | foxes, cats, ocelots | solo territory, hunt, return |
| `AQUATIC_PREDATOR` | dolphins | pod behavior, fish hunting |
| `AQUATIC_SCHOOL` | cod, salmon, tropical fish, pufferfish | exact-species school cohesion through aquatic navigation; cross-species panic remains separate |
| `LOOSE_AQUATIC_GROUP` | squid, glow squid | exact-species danger sharing without ordinary school cohesion |
| `HIVE_COLONY` | bees | hive and flower loop with Retold state awareness |
| `NETHER_HUNGRY` | piglins, hoglins | nether hunger behavior where relevant |
| `UNDEAD_HUNGRY` | zombies, husks, drowned, zombified piglins | horde pressure |
| `UNDEAD_TOLERANT` | skeletons, strays, bogged | ranged undead pressure |
| `TERRITORY_GUARD` | iron golems, brutes, blazes, shulkers, wither skeletons | guard post/zone behavior |
| `COMMANDER_SUPPORT` | evokers, witches | support and pressure from behind allies |
| `ILLAGER_RAIDER` | pillagers, vindicators, ravagers, vexes, illusioners | illager roaming and territory behavior |
| `BAT_COLONY` | bats | persisted broad roost identity, upward ceiling-slot search, hunger, five-member directional night hunting, organic danger response |
| `SPECIAL_VANILLA` | creepers, endermen, breeze, creaking | mostly vanilla plus safety rules |
| `APEX_OR_BOSS` | warden, wither, ender dragon | mostly excluded from Retold AI |

### Faction

Faction controls relationships, assist, hatred, tolerance, and territory. It does not decide daily life.

Technical owners:

- `RetoldFactionMembers`
- `RetoldFactionRelations`
- `RetoldFactionAssistEvents`
- `RetoldFactionCombatEvents`
- `RetoldAiTargets`
- `RetoldCombatTargets`
- `RetoldFactionTargetMemory`

Main faction design:

- Static faction classification is data-driven through one additive `retold:factions/*`
  entity-type tag per fixed Retold faction. `minecraft:illager` and `minecraft:undead` are composed
  where their meanings match; Ghasts remain an explicit Retold Undead addition.
- An entity type in multiple full faction tags fails closed as unfactioned and logs an error. Full
  membership takes precedence over `retold:alliances/illager_loose_allies` with a warning.
- Classification is cached by entity type and invalidated after server tag reload. Faction target
  and retaliation goals are added or removed for loaded mobs as their effective membership changes.
- Faction membership is independent from mob profiles: it supplies diplomacy, assist, retaliation,
  and territory identity without assigning daily-life behavior to an unknown third-party mob.
- Nether Remnants: piglins, piglin brutes, blazes
- Illagers: pillagers, vindicators, evokers, illusioners, ravagers, vexes
- Witch: permanent loose-ally identity, active Illager combat alignment only during a raid, and
  cooperation only with members of that same raid; never a full faction or territory member
- Village defenders: iron golems, snow golems, dynamically defending tamed wolves
- Undead: not one cozy social faction; standard `minecraft:undead` types plus Ghasts share diplomacy,
  zombies horde, skeletons tolerate undead, and Wither Skeletons guard fortresses. Tamed undead
  mounts do not inherit the generic hostile identity while tamed.
- Ocean Monument: guardians and elder guardians defend monument purpose

### State

State is temporary memory that changes behavior over time.

Technical owners:

- `RetoldMobState`
- `RetoldMobStates`
- `RetoldMobStateRecoveryEvents`
- `RetoldAiControl`
- `RetoldTerritoryMobState`
- `RetoldTerritoryMobStates`
- `RetoldTerritoryReputation`

Mob state is stored through `RetoldMobStates`: active mobs are cached in a weak runtime map and saved back to each mob's persistent NBT under `RetoldMobState`.

Saved mob state includes:

- hunger
- stress
- confidence
- last ate tick
- last danger tick
- last flee end tick
- last successful hunt tick
- last failed hunt tick
- last hunger tick
- home/range memory

## Confirmed Gameplay Contract

This section records the developer-confirmed target behavior. It is authoritative design, not
an implementation claim. The completion matrix below and
[`design_implementation_status.md`](design_implementation_status.md) say what actually exists.

### Universal Rules

- No mob deliberately targets or attacks a creeper. Cats hiss and retreat from creepers, and
  creepers avoid cats. When a creeper ignites, mobile creatures flee with species-dependent
  awareness and reaction delay; zombies do not run. Village defenders also flee an igniting
  creeper instead of trying to fight it.
- Hunger never overrides alliance, ownership, duty, urgent self-preservation, or creeper safety.
- Direct violence and an unmistakable active threat bypass a territorial warning. Accidental
  allied damage is ignored unless it becomes repeated or clearly deliberate.
- Intelligent creatures use believable sight, hearing, scent, communication, memory, target
  inertia, and occasional mistakes rather than perfect shared knowledge. Some species may rely
  on more senses than others.
- Healthy ordinary predators defend themselves after successful damage from a valid living attacker.
  Wolves, Foxes, Cats, Ocelots, Dolphins, Spiders, and Cave Spiders use five-second recent-attacker
  memory and explicit `RETALIATION` target ownership; the owned response may continue after the
  one-time vanilla damage memory clears. Tamed animals never retaliate against their owner, and
  global player-mode, alliance, and Creeper exclusions remain authoritative.
- The implemented first serious-wound rule applies to wild ordinary predators: after real damage
  from a living attacker leaves one below 25% health, it abandons ordinary hunting or retaliation
  and flees that attacker for ten seconds. Exactly 25% does not trigger the rule. Tamed defenders,
  Undead, bosses, and active territory duty are exempt. Broader intelligent-creature disengagement
  remains unspecified and must not be inferred from this predator slice.
- Tamed and hired defenders prioritize their owner and the owner's allies, except that they do
  not attack creepers. Tamed predators may defend but do not independently hunt owner or allied
  livestock.
- All snowballs deal one point of incoming damage, including player-thrown, mob-owned, and
  ownerless/dispenser-style shots, while retaining vanilla's three-damage Blaze interaction.
  Snowballs damage Creepers if they hit, although mobs still do not deliberately target Creepers.

### Factions And Hostility

- Undead tolerate other undead and attack every living non-undead creature except creepers,
  without prioritizing players. The Wither uses Undead diplomacy and actively searches for
  living targets, choosing serious threats intelligently. Ghasts, zoglins, zombie nautiluses,
  and the hostile untamed undead mounts belong to the Undead relationship family.
- Ordinary vanilla Mob targets and Brain attack memories cannot bypass that Undead tolerance.
  Retained targets clear when a live per-entity boundary changes, such as a tamed Zombie Nautilus
  becoming wild again. An explicit Retold-owned `RETALIATION` target remains allowed.
- Zombies, drowned, husks, and zombie villagers form mixed hordes. Stage 1 keeps Zombie- and
  Skeleton-family awareness and cooperation deliberately local: Zombie-family sharing uses 10
  blocks and notice uses 12, while Skeleton-family sharing uses 10 and notice uses 14. Stage 2
  restores wider same-family convergence at 22/18 blocks for Zombie sharing/notice and 24/22 for
  Skeleton sharing/notice. A stable one-in-three subset of the opposite family may also answer an
  incident within 12 blocks when within six blocks of the source or able to see it. This support
  uses normal faction-assist ownership and never becomes perfect shared knowledge. Zombies should
  remain comparatively simple and do not sprint merely because another creature recognized a
  danger.
- Slimes and magma cubes do not attack one another. Only members at or above their configured
  hunt-hunger threshold attack other living creatures indiscriminately except creepers. Dropping
  below that threshold clears their target and ends combat. They consume defeated prey to relieve
  hunger/heal/grow. At every hunger level, including completely fed, they seek and swallow every
  kind of dropped item, preserving the complete stacks and components so those items drop again
  when the Cube Mob dies. Their Retold movement requests are translated into the facing direction
  and repeated hops used by the vanilla Cube Mob controller, instead of ordinary path navigation.
  This unconditional item appetite does not enable living-prey hunting. Hungry size-one Cube
  Mobs deal contact damage through the same vanilla damage operation as larger members, while
  fed size-one members remain harmless.
  Ordinary vanilla Mob and Brain target writes cannot bypass Cube Mob tolerance. Explicit
  Retold-owned retaliation remains available when one member genuinely attacks another.
  Hunger gained per configured interval is `ceil(size / 2)`: sizes 1-2 gain one point, sizes 3-4
  gain two, and so on through five points at sizes 9-10. Larger members therefore reach hunting
  and feeding pressure sooner without exhausting their survival window as quickly as a linear
  size multiplier would. Every split child inherits half of its parent's
  current hunger, including ordinary death splitting. At 100 hunger, a size-two-or-larger member
  splits into exactly two half-size children at 50 hunger each; a size-one member dies through
  the normal death path. The starvation split preserves swallowed storage exactly once and starts
  the existing merge cooldown on both children so they cannot immediately undo the starvation
  response.
  Food-driven growth advances one size at a time up to size 10, with each step doubling in cost
  from 16 items for size 2 through 4,096 items for size 10 (8,176 items total). Compatible smaller
  members may also merge through the natural 1-to-2-to-4 sizes with a cooldown, transferring all swallowed
  contents to the survivor without bypassing the expensive later food-growth steps.
- Guardians and the Elder Guardian are hostile everywhere and immediately attack every living
  non-guardian intruder except creepers. Ordinary guardians are not random axolotl prey; an
  axolotl joins only through retaliation or genuine assistance. Damaging ordinary guardians
  increases monument pressure. Ordinary vanilla Mob and Brain targets cannot bypass Guardian
  tolerance, while explicit Retold-owned retaliation remains available.
- Village defenders are pacifist-purpose protectors: they attack actual danger to a village,
  protected player, or allied creature, not arbitrary political enemies.
- Witches and Illagers normally ignore one another. Witches assist Illagers during raids but are
  not normal territory members. The permanent loose-ally identity is separate from combat
  alignment: a witch must have an active raid, and assist partners must belong to the same raid.
  Leaving the raid clears Retold-owned assist targets.
- An attacked Enderman retaliates against any valid living attacker, and only in Stage 3 do nearby
  idle Endermen within 32 blocks join that defense. The central Creeper and invalid-player target
  guards still apply. Dragon attacks use the same rule, but Endermen do not proactively attack the
  dragon. Silverfish and endermites are unrelated and never coordinate. Spiders and cave spiders
  may cooperate in hunts.

### Territory And Society

- Nether Remnants are piglins, brutes, and blazes. A physically present Remnant warns every
  intruder in its territory. Before Stage 3 an empty abandoned bastion does not warn by itself.
  Acceptable local reputation plus qualifying gold-or-better armor grants entry; bad reputation
  overrides armor. Diamond, netherite, and Aenderite qualify above gold. A hired brute ignores
  armor and reputation while fulfilling its contract. The exact number of required armor pieces
  remains undecided.
- Nether Remnant trade/barter requires good reputation. Hungry piglins organize hoglin hunts.
  Hired brutes protect their employer and allied players/creatures. If the employer attacks one,
  the response escalates from no response, to warning, stronger warning, contract loss, and
  finally hostility rather than changing instantly.
- Illagers warn every non-allied intruder in their territory and attack immediately when
  attacked. During Stage 3 raids, pillagers, vindicators, and ravagers form the front line while
  evokers support from the rear; illusioners join raids and can rarely appear in mansion groups.
  Illager reputation currently needs only tolerated-to-hostile states. Vex direct strikes deal
  half of their otherwise calculated damage. Raids are Stage 3 content: before Stage 3,
  `RetoldRaidProgression` prevents Bad Omen from converting into Raid Omen and rejects vanilla
  raid creation. This start gate does not cancel a raid already in progress.
- Blazes can fight fire-immune Undead rivals when they meet in the Nether. A faction-scoped target
  exception lets mobs attack enemy Ghasts despite vanilla's blanket Mob-versus-Ghast rejection,
  without admitting allied or unrelated attackers. Wildfire-to-Ghast acquisition and retention use
  the leader's full 64-block follow range; other faction matchups keep the shared 40-block acquire
  and 48-block release boundaries. A narrow invulnerability hook lets Blaze- and
  Wildfire-owned Small Fireballs inflict their direct damage on Wither Skeletons and Ghasts while
  both retain ordinary fire and lava immunity. The Wildfire is a Stage 2+ Nether Remnant roaming
  miniboss that naturally arrives with three to five Blazes. Its dedicated 600-tick encounter
  owner makes bounded loaded-ground probes near one active Nether player independently of biome
  spawn weights and the ordinary monster cap, while rejecting another Wildfire within 128
  horizontal and 64 vertical blocks. Outside combat, persisted numbered escort identities let it
  lead those Blazes along sustained collision-checked airborne destinations in a
  2.25-block-spaced single-file line. The leader chooses a clear climb corridor before crossing
  raised terrain, and each escort follows a bounded three-dimensional path to its moving slot. This
  low-priority `REGROUP` movement uses the `WILDFIRE_FORMATION` owner. Combat releases that patrol
  owner but activates a flagless `ATTACK`/`WILDFIRE_ESCORT_COMBAT` owner: escorts inherit the
  leader's valid target, climb an immediately blocked forward corridor, and use budgeted flight
  paths to a compact staggered screen around it without replacing vanilla Blaze fireballs.
  Higher-priority lava recovery releases the group. A persisted encounter marker limits the leader and its
  escorts to targetable players and live `UNDEAD` faction members through every target-assignment,
  retaliation, and `canAttack` path; ordinary Blazes are not narrowed. A flagless companion goal coexists with the private
  vanilla Blaze attack goal and uses the `ATTACK`/`WILDFIRE_COMBAT` owner to select up to six
  loaded, collision-free orbit destinations every 40 ticks. It therefore keeps moving during its
  unchanged ranged-fire cycle without overriding higher-priority shelter recovery. Healthy idle
  Wildfires keep their heads above lava. A half-health leader instead prefers a bounded cached
  `DEEP_LAVA_SOURCE`: the exposed surface of a lava column at least three blocks deep. It uses
  higher-priority owned collision-aware flight, clears combat, submerges, restores six health per
  second plus every missing shield only while its eyes remain below the surface in that qualifying
  column, and rises only after both are full. The submerged state persists across saves; ordinary
  Fire, Soul Fire, shallow lava, and mere lava contact restore neither shields nor health. It preserves Blaze ranged combat, but carries boss-tier health, armor,
  knockback resistance, stronger direct fireball damage, remains allied with Blazes, uses the shared
  faction-owned targeting path against Undead, and guarantees
  the Nether Reactor Core Fire offering. Zombified Piglins deliberately lose vanilla fire immunity
  so both Blaze and Wildfire fireballs can damage and continue burning their priority Undead target.
  Stage 2's Undead sunlight protection suppresses only vanilla sunlight ignition and does not clear
  combat fire. The Wildfire entity type is explicitly fire-immune. Four reinforced synchronized
  shields gate health damage, close hostiles trigger a larger three-second cached-scan shockwave,
  and wounded Wildfires use bounded lava-source search plus movement ownership to retreat and
  recover shields. A dedicated model uses
  the credited Minecraft Dungeons texture; natural balance and client presentation need verification.
- Villagers are absolute pacifists. They communicate danger, alert golems, remember attackers,
  use local reputation, have hunger and communal food, and refresh trade inventory daily to
  simulate new stock. In loaded villages, hungry Villagers use every accessible chest or barrel
  near their remembered home, meeting point, or job site, regardless of its placement origin. They
  first eat the highest-value vanilla food already in their inventory. A Villager with no personal
  food walks to storage, withdraws up to 12 food points while preferring higher-value items, eats
  one, and carries the remainder for later meals. Observed chest/barrel positions, slot capacity,
  and exact contents form persistent shared village knowledge, so another Villager can directly
  resolve food, emeralds, livestock feed, or any other requested item without repeating the world
  scan. Container changes by players, hoppers, Villagers, or other systems refresh known or
  active-village storage at event time. Knowledge is advisory: the chunk, storage type, contents,
  village boundary, supported access cell, and exact count are revalidated before use, and stale
  entries are refreshed or removed. Machine inventories are ignored. Adult Farmers
  use vanilla crop harvesting, replanting, and bread
  making, then deliver only surplus Bread, Carrots, Potatoes, and Beetroot to the same stores while
  retaining 24 vanilla food points for themselves. Delivery uses low-priority communal search
  ownership, exact supported-side movement, the shared loaded-chunk cache/LOD/work budget, and
  yields to hunger, danger, sleep, and trading. Bounded unloaded reconciliation can consume
  personal or stored food through the same transaction and separately simulate one transaction-safe
  Farmer-owned crop harvest/replant/deposit per unloaded day.
  Generated village loot and future Villager-controlled deposits carry separately persisted
  village-owned quantities. Player additions remain unowned even when matching stacks merge, and
  withdrawals consume that unowned quantity first. A nearby village-context Villager who can see
  a Survival player remove protected contents adds vanilla negative gossip; breaking protected
  storage is treated more severely and can trigger vanilla Iron Golem hostility. Creative and
  Spectator players are ignored, and already-opened ambiguous existing-world storage is not
  retroactively claimed. `/retold village status` reports a bounded summary of the executing
  player's individual vanilla reputations among nearby loaded village-context Villagers, including
  possible golem hostility. Crops actually planted or replanted by Farmers carry persisted
  position ownership through natural growth; player planting clears it. A witnessed mature harvest
  is minor theft, while breaking an immature owned crop or trampling its farmland is stronger
  vandalism. This provenance is updated only when vanilla Farmer work changes the crop and adds no
  Farmer scan or tick cadence. Loaded Shepherds tend Sheep/Goats, Leatherworkers tend Cows/
  Mooshrooms, and Butchers tend Pigs/Chickens/Rabbits. A tender retrieves exactly two suitable
  items from village chest/barrel storage, follows low-priority owned routes to a valid adult pair,
  consumes the items, and relieves both animals' hunger. Successfully tended adults and offspring
  of two village-owned parents carry persisted ownership. Player interaction keeps previously
  unowned animals player-associated, and automatic offspring inherit that protection from an
  unowned player-associated parent, so
  village bounds alone never claim livestock. A sighted village-context witness applies `-50`
  vanilla reputation for a direct Survival player kill; monsters, environmental deaths, Creative,
  and Spectator are excluded. Tending shares the dispatcher, cached entity/storage searches, LOD,
  work budgets, and control priorities, and performs no unloaded simulation.
  All current vanilla breedable animals use persisted hunger-satisfaction breeding rather than
  direct item-triggered love mode. A supported adult must remain at `FULL` hunger without panic,
  damage, or an active target for five loaded or reconciled minutes. It then performs a bounded cached search for
  a compatible equally ready adult within eight blocks; a miss waits one minute before retrying.
  Retold only arms the ready pair, while vanilla owns mating movement, offspring/genetics, tame
  ownership, mixed Horse/Donkey births, Turtle eggs, Frog pregnancy, and other special behavior.
  A successful birth adds 40 hunger to each parent and retains vanilla's five-minute age cooldown.
  Player and Villager feeding only relieve hunger. Accumulated satisfaction plus retry/armed state
  saves with the entity; loaded dispatcher progress is interval-capped, while bounded catch-up
  advances only the continuous portion of its simulated timeline that remains `FULL`.
  In every world stage, adult Villagers also maintain dry weather-extinguished torches within
  eight horizontal and five vertical blocks and within 32 blocks of their village anchor. Most
  professions stop and use a one-second ranged magical cast. Nitwits cannot use
  magic: they select a supported adjacent access cell, acquire `VILLAGER_TORCH_RELIGHT` movement
  ownership, path close, and show a temporary Flint and Steel throughout the one-second interaction.
  The active visual is reasserted each tick if vanilla clears the hand, but the tool never enters
  inventory and consumes no item or durability. Both active methods continuously aim the
  Villager's body, head, and look control at the torch. Idle discovery and routing retain the
  central 20-tick Villager cadence; active magical and physical casts alone receive continuous
  presentation updates. A success immediately selects another eligible nearby indexed torch,
  bounded to eight relights per interruptible maintenance run before the normal success cooldown.
  Both methods
  preserve normal/soul/copper and floor/wall identity. Priority 29 maintenance uses the
  weather-owned loaded-chunk index, shared block-search/path budgets, LOD-scaled cooldowns, and a
  bounded physical-route timeout; every consecutive search is separately work-budgeted and performs
  no broad block scan. Nitwit `PLAY` is eligible
  low-priority time, while hunger, danger, targets, sleep, trading, incompatible activities,
  higher-priority work, ongoing precipitation, and `mobGriefing=false` prevent or interrupt it.
  Wandering Traders remain independent and teach travel-themed recipes that ordinary villagers do
  not know.
- At Stage 2+, vanilla still decides whether a village may create an Iron Golem: the builder must
  want one, five nearby Villagers must agree, and vanilla's local recent-golem detection must pass.
  Retold replaces only that eligible instant spawn with persistent staged construction when the
  builder is a Cleric, Librarian, Armorer, Toolsmith, or Weaponsmith. Other professions, including
  Nitwits, cannot construct golems. This profession gate applies when starting and resuming a
  saved build. Retold does not add a numeric village cap or daily cooldown.
- One builder finds a reachable supported site, places the four magical iron blocks and a regular
  pumpkin at 40-tick intervals, spends one emerald from a nearby Villager or accessible village
  chest/barrel, then holds the emerald for 40 ticks before carving the pumpkin to invoke vanilla
  animation. An emerald trade retains one physical emerald for village construction when inventory
  or communal storage permits it. The result is not marked player-created and does not award nearby
  players the player-summoning “Hired Help” advancement.
- Construction uses the central dispatcher, `VILLAGER_GOLEM_CONSTRUCTION` ownership, shared
  loaded-chunk block-search caches and budgets, and persistent Villager data. It requires
  `mobGriefing` and yields to danger, hunger, sleep, trading, targets, and higher-priority activity.
- Successfully animating a player-built Iron Golem costs five experience levels in Survival.
  Creative players are free, and invalid/obstructed frames, Snow Golems, and Copper Golems are not
  charged.

### Animals And Ecology

- All ordinary animals, including player-kept, named, and tamed animals, have hunger and consume/remove reachable
  food according to species-specific diets. `mobGriefing=false` prevents block/crop consumption,
  weak-barrier damage, creeper terrain damage, and Gale Core block damage, while dropped food can
  still be eaten. At 100 hunger, loaded hunger-aware mobs take one point of starvation damage per
  species-specific hunger interval until feeding lowers hunger or the mob dies. This loaded rule
  also applies to named and tamed mobs; the separately planned unloaded simulation retains its
  confirmed protection against offline death for named/tamed animals.
- Cows and mooshrooms share herds. Other herds are mainly species-specific; equines can group,
  and llamas group with trader llamas. Groups remain around their persisted range while compatible
  local food is usable, then migrate together under hunger when that food is depleted or
  inaccessible. There is no separate domesticated classification or player-defined enclosure flag:
  a player keeps animals where they placed them by maintaining food there. The loaded core gives
  Cows/Mooshrooms one `bovine` social identity, retains the mixed `equine` and `llama` identities,
  recognizes compatible Animal Feeders around land ranges, and applies the same rule to Pig
  foraging ranges.
- Desperate animals and actively hunting wild predators may breach explicitly tagged weak
  barriers, including player-built barriers. The initial tag contains wooden fences and closed
  wooden fence gates, including the Aender variants; open gates, doors, trapdoors, walls, and iron
  barriers are excluded. Predators break for 60 ticks and desperate non-predators for 120 ticks,
  with visible crack stages, normal block drops, and a 200-tick per-mob cooldown after success.
  Tamed predators, leashed/passenger mobs, and water-only/flying navigation do not participate.
  Barrier breaking is bounded to a four-block local cached search and never happens in unloaded
  simulation.
- Bees are peaceful near hives and flowers. The colony reacts collectively only after successful
  health damage to a Bee, a tagged hive is broken, or a full tagged hive is harvested with Shears
  or a Glass Bottle without smoke. The victim owns retaliation while available nearby Bees use
  faction-assist ownership; smoke, Creative/Spectator players, busy Bees, and arbitrary unrelated
  Bee targets are excluded. Hungry Pandas consume the exact bamboo block they reach without
  producing a drop; the meal is credited only after successful removal and obeys `mobGriefing`.
  Armadillos tolerate ordinary walking but hide from sprinting, aggression, and real danger. When
  hungry, they use a cached bounded search to approach exposed grass/dirt-like soil, perform a
  visible grub-dig meal without removing the soil, and wait at least 30 seconds before digging
  again. Turtles flee predators and aggressive or
  sprinting players, not peaceful walkers. Polar bears warn before attacking cub intruders.
- Hungry spiders can hunt full-sized animals only at night, end those food hunts in daylight, and
  still retaliate immediately at any time. Their proactive player aggression remains darkness-based,
  and recently fed Spiders and Cave Spiders can establish shared webbed lairs only in genuinely dark,
  sheltered spaces. Raw skylight must be below 8 at both the builder and placement, so ordinary
  outdoor nighttime darkness does not qualify. A lair grows or repairs one persistent cobweb every 30 seconds up to 50 and
  obeys `mobGriefing`. Members return during daylight, but targets interrupt that movement and night
  releases it. Wolves give hungry pack members priority in initiating and benefiting from hunts.
  Predators disengage when sufficiently fed during a hunt.
- Fish form species-specific schools but may share panic across species. Cod, Salmon, Tropical Fish,
  and Pufferfish use a conservative loaded grazer model: their 520-tick hunger cycle drives bounded
  destructive searches for `retold:aquatic_school_forage_blocks`, whose default contains seagrass,
  tall seagrass, kelp, and kelp plants. Squid and Glow Squid use the same loaded hunger interval and
  seek dropped `retold:squid_foods`; the standalone default contains raw fish only and does not
  enable living-prey hunting. Plant grazing obeys `mobGriefing`, while dropped-food consumption does
  not edit terrain. Bucketed fish and axolotls remain wild.
  The loaded-world school core is implemented for Cod, Salmon, Tropical Fish, and Pufferfish:
  cached exact-species scans establish a shared persisted `AQUATIC_SCHOOL_RANGE`, isolated members acquire low-priority
  `AQUATIC_SCHOOL` ownership in `REGROUP`, and `RetoldBehaviorMovement` requests a real aquatic
  navigation path. Nearby usable plants hold that range; when hungry and depleted, the lowest-ID
  current member alone evaluates a bounded cached better range and shares it with the school. Fish
  still share panic across fish species through the separate flee owner.
  Squid and Glow Squid use exact-species loose danger groups; successful damage performs one
  bounded cached broadcast before the receiver-side panic scan can retry. The shared food owner
  supplies cached scans, block-search budgets, throttled paths, feeding priority, and starvation for
  the six loaded diet profiles. Natural depletion/regrowth balance remains unverified.
- Dolphins defend their pod collectively after successful damage from a valid living threat. The
  victim owns retaliation, while available nearby witnesses use faction-assist ownership without
  requiring hunger; a podmate with another live target is not redirected. Bats use loose roost colonies, hunt arthropods at night,
  dodge arthropod counterattacks without routing, and spread unrelated panic selectively with
  individual delays. Vanilla roost disturbances and unrelated danger hold Bats in owned panic
  flight for ten seconds before daytime settling can resume. Parrots now forage for seeds/crops and give owners a distinct real-danger warning; natural presentation still needs in-game verification.
  Striders are hungry Nether herd animals with remembered lava ranges and domesticated homes. A
  Strider standing on or in lava receives two hunger relief every ten seconds without consuming
  lava and does not leave it for an ordinary autonomous food search; warped fungus remains fallback
  food away from lava and for player interaction. Wild
  hungry Nautiluses join the controlled aquatic hunt owner and pursue living fish, while tamed
  Nautiluses remain excluded from autonomous hunting.
- Wild Skeleton Horses, Zombie Horses, and Camel Husks are hostile but tameable. Persisted owner
  references, rather than vanilla's tame flag, define when these mounts are claimed because Camel
  always reports itself as tamed and Skeleton traps can mark horses tamed before player ownership.
  An ownerless already-tame riderless Skeleton Horse or Camel Husk is claimed on a valid player
  mount; Zombie Horses retain vanilla bucking/taming. Claimed undead mounts defend themselves and
  their owners without independently hunting. Other Undead tolerate them.

### Special And Stage-Gated Creatures

- Stage 2 Undead escalation comes from wider awareness, convergence, imperfect
  Zombie/Skeleton-family assistance, and modest natural-spawn pressure, not stat buffs. For each
  already-present entry whose entity type belongs to `retold:stage_2_undead_spawn_pressure`, the
  potential-spawn list gains a duplicate entry worth about 25% of its original weight. Vanilla
  retains the monster cap, biome/structure eligibility, placement checks, and original pack sizes;
  the hook creates no entry where one did not already exist. Stage 1 retains the weaker short-range
  coordination baseline, and Stage 3 adds no bonus before its existing Undead spawn cancellation.
- Wither Skeletons spawn in fortresses and naturally but rarely in Soul Sand Valleys. The valley
  uses a data-driven weight-one solitary biome entry; their territory profile still requires a
  fortress anchor, so they do not guard the valley. Phantoms are rare nightmare-like demons that
  can appear alone or in groups at night or during storms under open sky, independently of insomnia.
- Endermen retain vanilla gaze aggression in Stage 1 and become peaceful unless attacked in
  Stages 2 and 3. Vanilla block carrying remains. Aender Eye gameplay beyond the prototype is
  undesigned.
- Breezes are Air Temple inhabitants rather than Trial Chamber mobs. Inside that encounter,
  Breezes attack every living non-Breeze/non-Gale-Core intruder except creepers, and the Gale Core
  can command them to assist. The Gale Core pressures every participating player and destroys
  suitable blocks without drops, subject to `mobGriefing`.
- Wildfires are Stage 2+ Nether roaming minibosses rather than boss-room mobs. Their dedicated
  600-tick encounter owner selects one active player and makes up to 16 loaded-ground probes 48 to
  96 blocks away, bypassing biome spawn weights and the ordinary monster cap while retaining
  Peaceful, `doMobSpawning`, placement, player-distance, normal despawn, and 128-horizontal/
  64-vertical Wildfire-exclusion rules. A successful attempt finalizes with three to five Blaze escorts.
  The leader persists those escort UUIDs in numbered slots and, while idle, selects sustained
  open airborne patrol destinations. A bounded collision probe chooses any required vertical
  clearance, then budgeted three-dimensional routes move the leader and each Blaze toward its
  current slot at 2.25-block spacing, producing one line instead of independent random roaming.
  The low-priority `REGROUP`/`WILDFIRE_FORMATION` claim releases for any combat target. A separate
  flagless `ATTACK`/`WILDFIRE_ESCORT_COMBAT` claim then shares the leader's valid target and routes
  every escort into a compact staggered combat screen. Its immediate-corridor collision check
  forces ascent before the budgeted flying route resumes, preventing walls or depressions from
  leaving rear escorts behind. Both group modes yield to higher-priority shelter recovery. Loaded escort goals are restored from the bounded list every
  20 ticks without an entity scan. Installation also writes a persistent encounter marker used by
  the central hostility gate, so the leader and its escorts accept only targetable players and the
  datapack-extensible `UNDEAD` faction. Retaliation cannot bypass this limit, while ordinary Blazes
  retain the broader Nether Remnant defaults.
  The leader's no-flag combat goal runs alongside vanilla's private Blaze attack goal and claims
  `ATTACK` movement through `WILDFIRE_COMBAT` below shelter priority. Every 40 ticks it considers at
  most six loaded, collision-free orbit destinations 7 to 20 blocks from its target, keeping the
  leader mobile without replacing fireball timing or blocking wounded fire retreat. A constant,
  scan-free lava lift keeps a healthy leader's eyes above the surface when idle. At half health,
  `WILDFIRE_RECOVERY` prefers the cached `DEEP_LAVA_SOURCE` block-search mode, which accepts only an
  exposed lava surface with two further lava blocks directly below it. It owns collision-aware
  flying approach, clears its target, and persists a submerged state. It restores six body-health
  points every 20 ticks alongside the existing shield cadence, then releases recovery and applies
  surface lift only when health and all four shields are full. A live deep-site plus eye-level
  submersion check gates both shield and body regeneration; Fire, Soul Fire, shallow lava, and
  surface contact do not qualify.
  Invalid-target cleanup preserves this recovery owner and
  route when the recovery transition deliberately clears the former combat target.
  Their unmanaged `SPECIAL_VANILLA` profile preserves Blaze ranged combat, while
  boss-tier attributes and stronger direct fireball damage make the leader the encounter's main
  threat. Nether Remnant tag
  membership supplies bounded faction-owned targeting against Undead, including Ghasts, and
  alliance with Blazes. Ghasts are the one extended pairing: Wildfires acquire and retain them at
  up to 64 blocks, matching the leader's ranged-combat envelope, while non-Ghast faction targets
  keep the shared 40/48-block bounds.
  They guarantee the Nether Reactor Core Fire offering. Zombified Piglins are no longer fire-immune,
  allowing Blaze and Wildfire fireballs to damage and continue burning them, while Wildfires are
  explicitly fire-immune themselves. Four reinforced persisted shields absorb damage independently,
  a bounded cached hostile scan drives the six-block, three-second knockback shockwave, and wounded
  shield-damaged Wildfires claim shelter movement toward a cached lava target. Lava recovery
  restores the complete boss only while submerged and visibly resurfaces it; ordinary fire and
  surface lava contact restore nothing. The
  dedicated model uses the credited Minecraft Dungeons texture; natural encounter balance and
  client presentation remain to be verified.
- The Elder Guardian is the single monument sentinel and guaranteed Water Element source. It is
  invulnerable while in water. Monument/water progression remains available in Stage 1.
- Sniffers and Endermites are unavailable through normal survival acquisition/spawning but remain
  functional through commands/creative; existing entities and their Retold AI are retained.
  Wardens/Ancient Cities/Deep Dark and Trial Chambers are removed from survival. Shulkers and End
  Cities are outside the active mob design because End Cities do not exist in Retold survival.
- Happy Ghasts, ghastlings, and dried ghast corpses do not exist. Only the ordinary hostile Ghast
  remains. The Creaking/Pale Garden can stay vanilla for now.
- Planned additions retain these identities: Killer Bunny as a rare natural hostile creature;
  Iceologer as an Illager guarding an isolated igloo with a warning; the original one-block,
  stackable Mojang Tuff Golem display-statue concept; and the music-disc monster. Fire, Earth,
  Nether, and Aender dragon details remain undesigned.

### Unloaded Ecosystem Simulation

- The implemented first stage detects at least two missed metabolism pulses from each mob's
  persisted `lastHungerTickAt`, deduplicates the returning mob into a server-lifecycle queue, and
  reconciles at most 16 mobs per tick. At most seven Minecraft days are considered. Uncapped
  calculations retain their incomplete metabolism interval; a capped calculation discards older
  debt so the same time cannot be charged again.
- The implemented food stage advances hunger and the persisted timestamp atomically and permits at
  most one real meal per simulated Minecraft day, only after that day's species-specific metabolism
  reaches its eat threshold. Eligible animals first resolve a compatible Animal Feeder around a
  valid persisted home/range, falling back to their saved entity position when no valid home exists.
  A remaining meal deficit uses one fair-budget local scan to collect enough distinct accessible
  forage blocks before any mutation. Destructive grass, crops, and aquatic plants reuse the normal
  `mobGriefing` and world-protection guard; renewable environmental forage remains non-destructive.
  Villagers consume their highest-value personal food first, then use only an accessible chest or
  barrel inside remembered/live village context through the existing provenance-aware restock
  transaction. Discovery can defer the whole task when shared search work is unavailable. Meals use
  normal relief and persist exact source state, meal time, stress, and confidence without playing a
  loaded animation or synthesizing movement.
- The implemented predation stage runs only after feeder and forage capacity is exhausted. A fresh
  shared-budget snapshot finds currently loaded living candidates within the ordinary 18-block hunt
  radius, then at most eight global reconciliation path probes per tick prove that prey is reachable
  without crossing a closed barrier. It reuses the loaded prey diet/faction rules and hunt threshold.
  Named, tamed, ridden, and passenger prey are excluded, as are tamed hunters. Each simulated day can
  remove at most one actual wild prey; `discard` deliberately creates no loot or XP, and the normal
  feeding plus successful-hunt stress, confidence, and timestamps persist. Budget denial defers the
  complete transaction before any mutation.
- The breeding stage advances the existing continuous five-minute `FULL` clock for eligible adult
  breeders across the same bounded hunger timeline. Hunger above `FULL` resets progress, capped
  history does not preserve uncertain earlier progress, and a returning ready pair uses the normal
  bounded loaded mate scan and vanilla birth path. Local population never blocks breeding.
- The migration stage applies only after at least one full unloaded Minecraft day, no catch-up meal,
  species-specific hunger, and depletion of the group's valid persisted range. Land herds, Pig
  foraging groups, and fish schools reuse their loaded social compatibility and forage policies; a
  compatible feeder or local forage therefore holds a player-placed land range without a separate
  domesticated state. One real group per tick uses a fresh budgeted entity snapshot, and only its
  lowest-ID current member may move the shared range to a better loaded candidate within 32 blocks.
  While that member is queued, a dedicated unloaded-ecology owner holds `REGROUP` just above
  ordinary feeding priority so a budget deferral cannot silently hand the mob to loaded food or
  social movement; higher-priority danger and combat behavior can still cancel the migration.
  Every member must be idle or under safe `REGROUP` ownership, free of targets, leashes, riders, and
  vehicles, and settled on land or in water. The transaction pre-resolves distinct collision-free
  landings and proves every route through a separate ten-probe-per-tick budget and 64-block path
  horizon. Any unavailable budget, unsafe landing, busy member, or unreachable route prevents all
  movement and home replacement; closed barriers cannot cause a partial teleport.
- The starvation stage applies one bounded damage transaction for all remaining critical hunger
  pulses. Ordinary wild mobs can die from that accumulated damage. Named mobs, tamed animals, and
  horse-family mobs instead stop at one health. Cube Mobs use one real split-or-die transaction;
  catch-up never recursively simulates fictional child generations, and swallowed storage is
  transferred only through that real split.
- The Farmer stage queues at most one eligible adult Farmer per tick and one real production cycle
  per unloaded Minecraft day, capped at seven. It considers at most seven nearby persisted
  Farmer-owned vanilla crops, applies current `mobGriefing` and world-protection checks, and proves
  a current route to the crop and an accessible village chest/barrel before mutation. The mature
  crop's real loot is generated, one matching replant item is consumed, Wheat can become Bread,
  the crop is replanted at age zero, the Farmer retains 24 food points, and only the surplus enters
  storage through the normal village-ownership ledger. Missing access, crop, inventory capacity,
  or compatible storage produces nothing.
- The natural-spawn stage deduplicates returning mobs by dimension/chunk and records the largest
  daily debt before post-tick work begins. One returning chunk per tick receives at most one vanilla
  `NaturalSpawner` chunk pass per simulated day, capped at seven, and no chunk is force-loaded.
  Current spawn gamerules, biome/structure lists, server monster mode, placement/light validation,
  player-distance eligibility, and mob caps remain vanilla-owned.
- Catch-up work is queued so chunk loading does not spike. Ordinary loaded behavior resumes after
  reconciliation; natural population composition and pacing still require in-game verification.
- The simulation changes real state: food/crop stores can be consumed, predators can remove real
  prey entities, and breeding readiness persists. A ready returned pair then uses vanilla to create
  real offspring; eligible foodless groups physically relocate together only after every member's
  route and landing have been validated. Catch-up never breaks barriers.
- Extreme hunger can cause weakness, suppress regeneration, deal damage, and eventually kill.
  Named/tamed and horse-family mobs return from a long unload at no less than one health.
- There is deliberately no separate carrying-capacity or population-count gate. Food availability
  and the continuous `FULL` requirement control breeding, including for player-placed groups.
  Catch-up randomness does not need deterministic replay. The elapsed-time cap is seven Minecraft
  days; food, predation, Farmer production, and returning-chunk spawning use daily granularity.

Territory mob state is separate and currently lives in `RetoldTerritoryMobStates` as runtime weak-map state for warning posture/debug values. Per-player suspicion/reputation is owned by server-global `RetoldTerritoryReputationData` SavedData and accessed through `RetoldTerritoryReputation`.

## Core Priority Order

The intended behavior priority is:

1. invalid state cleanup
2. boss/special exclusions
3. owner, home, or territory defense
4. flee serious threat
5. feed if food is already acquired
6. eat easy nearby food
7. hunt/search if hungry
8. regroup or return home/range
9. rest, social, or idle

The code expresses this through:

- profile checks in `RetoldMobRules`
- control ownership and priorities in `RetoldAiControl`
- behavior gates in `RetoldBehaviorCoordinator`
- target ownership in `RetoldBehaviorTargets` and combat helpers
- dispatcher order in `RetoldBehaviorEntityTickDispatcher`

## Control Model

Retold-owned movement and targeting should use `RetoldAiControl`.

Important classes:

- `RetoldAiControl`
- `RetoldAiControlMode`
- `RetoldAiControlOwner`
- `RetoldAiPriorities`
- `RetoldBehaviorCoordinator`
- `RetoldBehaviorMovement`
- `RetoldBehaviorCombat`
- `RetoldBehaviorTargets`

Control exists so different systems do not fight each other. For example, flee should beat idle, territory should beat ordinary hunger, and controlled combat should not be overwritten by vanilla random target selection.

General rules:

- Use control ownership before starting movement or combat behavior.
- Every deliberate Retold travel destination must use the mob's appropriate obstacle-aware path or
  navigation system. Ground and swimming `PathfinderMob`s use vanilla navigation through
  `RetoldBehaviorMovement`; supported free-flying mobs use a bounded three-dimensional path and may
  apply native flight physics only toward the next safe path node. Direct target-vector steering is
  not an acceptable substitute for route finding.
- Clear control when the behavior is no longer valid.
- Do not directly force targets from high-level behavior unless using the Retold target helpers.
- Do not let vanilla target assignment bypass warning or controlled hunting rules.

## Hunger And Life Behavior

Hunger stages:

| Hunger | Stage | Expected behavior |
| --- | --- | --- |
| `0-20` | full | rest, social, idle |
| `21-40` | light hunger | eat easy nearby food |
| `41-65` | hungry | active food search |
| `66-85` | very hungry | hunt or risky food |
| `86-100` | desperate | more aggressive hunger behavior |

Technical owners:

- `RetoldHungerStage`
- `RetoldFoodBehaviorEvents`
- `RetoldStarvationBehavior`
- `RetoldAnimalFeederBehavior`
- `RetoldAnimalFeederSearch`
- `AnimalFeederBlockEntity`
- `RetoldHeldFoodConsumptionEvents`
- `RetoldForageBlockSearch`
- `RetoldBlockTargetSearch`
- `RetoldFeedingAnimations`
- `RetoldRangeForage`
- `RetoldMobGriefing`
- `RetoldWeakBarrierBehavior`
- `RetoldSlimeSplitBehavior`
- `RetoldSlimeStarvationBehavior`
- `RetoldWeakBarriers`
- `RetoldTags.WEAK_MOB_BARRIERS`

On loaded servers, every profile with a positive `hunger_interval_ticks` value receives one point
of starvation damage whenever its normal metabolism interval reaches or remains at 100 hunger.
The damage uses Minecraft's starvation source, has no minimum-health floor, and can therefore kill
ordinary, hostile, tamed, named, and Villager mobs that use Retold hunger. Feeding below 100 stops
the next pulse without a separate recovery timer. `RetoldFoodBehaviorEvents` owns the ordinary
`PathfinderMob` path, `RetoldBatColonyEvents` owns the distinct Bat-colony path, and
`RetoldVillagerCommunalFood` owns the deliberately separate Villager path; all three route through
the same `RetoldStarvationBehavior`. The dispatcher routes Bats to their adapter before checking
the vanilla mob hierarchy because `Bat` enters the `PathfinderMob` branch in 26.2. Slimes and Magma
Cubes retain their established critical-
hunger split-or-die response and never receive the generic pulse.

`RetoldHungerSurvivalGameTests` complements the starvation-owner checks with one isolated loaded
feeding habitat for every positive-hunger profile. Forty-seven species begin at 99 hunger and must stay
alive while lowering it through production behavior; a 48th registry guard prevents profiles from
being added without a case. The cases use a representative source from the species' patched 26.2
spawn environments rather than artificial dropped-item substitutes: appropriate living prey,
aquatic prey and water, flowers/crops/forage, cave and Nether resources, caravan sustenance, or
Villager communal storage. A Creative mock observer keeps each case at normal player-loaded LOD
while remaining invalid prey. This is a one-meal viability matrix, not evidence for long-term
balance, every generated terrain arrangement, unloaded simulation, multiplayer, or existing worlds.

Spawn-habitat fallbacks fill the contexts where an ordinary diet is not naturally placed nearby.
Camels and desert Rabbits browse blocks in `retold:desert_browse_blocks`, with Dead Bush as the
default while cactus remains a hazard. Goats non-destructively scrape blocks in
`retold:goat_scrape_blocks`, defaulting to stone, snow, packed ice, and gravel; Mooshrooms
non-destructively graze `retold:mooshroom_grazing_blocks`, defaulting to mycelium; and Armadillos find abstract grubs in red sand
and terracotta as well as ordinary soil. Piglins can consume red/brown mushrooms or Crimson Fungus.
Cats admit Frogs as wetland prey, Ocelots retain jungle Chicken prey, and Spiders/Cave Spiders admit Bats. A hungry nighttime
Bat that finds neither a dropped Spider Eye nor physical prey catches abstract cave insects, and a
Trader Llama linked to a live Wandering Trader receives abstract caravan fodder. Hungry Slimes and
Magma Cubes independently acquire nearby faction-valid non-Cube prey when no current or shared swarm
target exists. Habitat forage is renewable and non-destructive, remains available with
`mobGriefing=false`, and has a 600-tick per-mob repeat-use cooldown.
Renewable habitat forage uses the existing six-block horizontal scan volume so a staggered first
food tick does not lose a nearby source after ordinary vanilla wandering; destructive forage keeps
the narrower four-block scoring radius.
Ordinary crop, flower, grazer-plant, small-passive-plant, Turtle, Hoglin, Piglin, and Strider forage
eligibility is exposed through Retold-owned block tags. Meat, fish, berry, grazer, small-passive,
flower, Nether-fungus, Bat, and feline-scavenge dropped-food families are exposed through item
tags, nesting exact-match vanilla/Common tags where available. Existing registry-path checks remain
as compatibility fallbacks. Tagged ordinary forage is still destroyed and requires `mobGriefing`;
tagging an item or block changes classification only and does not bypass AI ownership, bounded
searches, feeding timing, or hunger rules.

Living-death integration credits valid ordinary predator and wild Nautilus prey through
`RetoldControlledHuntingEvents` and credits vanilla as well as Retold lethal Frog/Axolotl attacks
through their species owners. Piglins receive meals from Hoglins they kill, hungry undead receive
meals from non-undead living victims, and Slimes/Magma Cubes receive meals from non-Cube living
victims. Creepers and same-family victims are excluded. Meal relief is recorded at death, while the
low-priority feeding pose is acquired only when it does not displace urgent combat ownership.
Nonlethal custom bites retain their partial relief, so a lethal bite is counted exactly once. The
generic random food-search fallback excludes Bee and Sniffer profiles because their flower and
diggable-ground owners already supply bounded specialized searches. Sniffer ground movement targets
the walkable block above the remembered ground and uses a four-block completion radius appropriate
to its body size. `RetoldNaturalFoodAcquisitionGameTests` covers the three kill-meal families,
their undead/Cube/Creeper exclusions, and non-consuming Strider lava sustenance. The exact
Armadillo, Nautilus, and Strider survival cases use soil, live fish, and a contained lava habitat
rather than dropped-item substitutes. Warped fungus remains a valid Strider fallback. Turtle block
forage remains seagrass-specific.

Hunger should not override territory guard purpose, special boss behavior, or urgent flee/combat behavior.
Dropped-item consumption is not terrain modification and remains available when `mobGriefing` is
disabled. Block forage and weak-barrier breaking recheck the shared entity-griefing policy at the
point of destruction rather than reading the gamerule independently. Direct food uses a higher
control priority than barrier breaching and ordinary prey hunting. Every hunger-driven profile
that can eat a nearby dropped stack abandons its ordinary `HUNT` target, clears chase sprint and
strike state, and lets `RetoldFoodBehaviorEvents` take `FEED` ownership. Retaliation and territory
attack targets remain urgent and are not abandoned for food; flee, defense, special combat, and
territory purposes also remain above ordinary feeding.
At `ACTIVE_SEARCH` hunger, managed non-predator `PathfinderMob`s that have found neither a dropped
item nor an edible block acquire `FOOD`/`SEARCH` ownership and travel to a bounded reachable search
point. Successful paths are reused briefly; failed paths are not cached as successful. Predator
profiles retain their directional prey-search layer, while dropped edible items can supersede an
ordinary food hunt as soon as they are detected. Bats use the corresponding bounded 3D search route
at night through their colony adapter rather than the ordinary ground-search path.

The loaded-world food order is dropped food, Animal Feeder, edible forage, then bounded random food
search. `animal_feeder` is a one-slot wooden trough whose exact stack persists. Hungry managed
non-monster land `Animal`s use their existing species diet and hunger-relief values, a cached
LOD-aware eight-horizontal/two-vertical block search, the shared block-search budget, ordinary
`FOOD`/`FEED` ownership, and a reachable supported cell beside the trough. One item is consumed per
feeding. The final feeder approach uses `RetoldBehaviorMovement.throttledMoveToExact`, which asks
vanilla navigation for reach range zero; ordinary Retold destinations retain their normal reach
range of one. Path memories include that reach range so a less precise route cannot satisfy the
feeder request. Aquatic groups/helpers/predators, Villagers, hostile monsters, Slimes, and Magma Cubes do
not use it. Live targets and urgent owners retain priority. Feeder consumption is an inventory
operation rather than terrain modification, so it remains available with `mobGriefing=false`.
The Animal Feeder remains animal-only. Villagers instead use `RetoldVillagerCommunalFood` and
`RetoldVillagerCommunalFoodSearch`: a 16-horizontal/four-vertical loaded-chunk block-entity scan,
bounded to accessible chests and barrels within 32 blocks of a remembered HOME, MEETING_POINT, or
JOB_SITE. A live vanilla village near the Villager is the fallback context. Each scan observes all
eligible storage it encounters into the chunk-indexed, server-global
`RetoldVillageStorageKnowledge` SavedData. Later food, arbitrary exact-item/count, and deposit-space
requests consult that shared index before spending a world-search budget. Generated village loot,
Villager/player transactions, and chest/barrel `setChanged` calls for known or active-village
storage keep it current without an always-on tick subscriber. Lookups never force-load chunks and
must still pass the existing live container/access validation before movement or transfer. A miss
falls back to the original scan, which remains cached, LOD-aware, and charged to the shared
block-search budget. The Villager claims ordinary
`FOOD`/`FEED` movement, and first consumes the highest-value Bread, Carrot, Potato, or Beetroot
already in its inventory. Only an empty personal food supply starts a storage route. At a supported
adjacent cell, the Villager transfers the highest-value available items up to a 12-food-point stock,
consumes one, retains the rest, and uses the shared source-facing feeding pose. Exact accepted counts
are removed from the container, so a full Villager inventory cannot delete or duplicate food. Sleep,
trading, recent attackers, avoidance, and remembered nearby hostiles retain priority. Other
containers are ignored. Entity hunger, personal inventory, and container inventory save normally,
and bounded unloaded reconciliation reuses the same personal-first withdrawal transaction without
loaded movement, sound, or pose state.
Player insertion is a server transaction. A normal right-click offers one compatible item; a
sneak-right-click with compatible food offers as much of the held stack as the remaining capacity
accepts. Storage receives a copy, then a Survival player's actual held stack loses exactly that
accepted count. Creative players retain their held stack, matching ordinary Creative placement.
Sneak-right-clicking with an empty hand or incompatible item retrieves only the stored stack or
drops only the inventory remainder. Focused coverage checks the single-item, bulk, and retrieval
controls, exact Survival conservation, and non-consuming Creative behavior.

Stationary or instantaneous Retold actions with a concrete subject use `RetoldActionFacing` to aim
the mob's body, head, and look control together. Current users are feeding poses, Villager torch
casts, golem work, communal storage and livestock tending, desperate weak-barrier breaking, Polar
Bear warnings, and Spider web placement. Moving pursuit, ranged positioning, migration, fleeing,
and free-flight navigation intentionally retain ordinary look control so presentation does not
fight the navigation-owned body heading.

Every completed Retold feeding action enters the same two-second presentation through
`RetoldFeedingPose`. The pose remembers the actual source position, owns `FOOD`/`FEED` at priority
57, stops navigation, move control, sprinting, velocity, and the separate Cube Mob wanted movement,
then turns body, head, and look control toward that source on every tick. Dropped items, forage
blocks, Animal Feeders, Bee flowers, Panda bamboo, Frog/Axolotl prey, predator kills, Bat food and
prey, Sniffer forage, and Cube Mob swallowing all supply their world position. Held food has no
external world position, so it uses the point directly in front of the mob's face. The pose priority
remains below faction pressure, defense, attacks, territory work, and flee control; if any higher
priority owner takes control, the transient pose is discarded without clearing that urgent owner.
Every meal also rechecks `RetoldBehaviorCoordinator.canCompleteMeal` immediately before changing
hunger or consuming an item, block, or prey. `HUNT` may complete its own bite/kill meal, but
`FLEE`, defense, attack, support, shelter, territory, and unrelated live-target work cannot. The
same rule gates vanilla `EatBlockGoal`: urgent ownership prevents a new graze, ends an active one,
and cancels its delayed block transaction. This transaction-time check is required even when the
owning dispatcher already screened danger, because vanilla goals and event callbacks can remain
active across an ownership change.

## Home, Range, And Social Systems

Home/range is used by animal-like profiles. Guards use territory purpose instead.

Technical owners:

- `RetoldAnimalHomes`
- `RetoldAnimalHomeMemory`
- `RetoldAnimalHomeType`
- `RetoldAnimalHomeData`
- `RetoldAnimalSocialGroups`
- `RetoldAnimalHomeIdle`
- `RetoldAnimalHomeRepairEvents`
- `RetoldSpiderLairEvents`
- `RetoldBatColonyEvents`
- profile-specific home events

Home/range types:

- `WOLF_DEN`
- `DOLPHIN_POD_RANGE`
- `HERD_RANGE`
- `AQUATIC_SCHOOL_RANGE`
- `FORAGING_RANGE`
- `ROOST`
- `WARREN`
- `FOX_DEN`
- `CAT_TERRITORY`
- `OCELOT_TERRITORY`
- `PANDA_BAMBOO_GROVE`
- `SNIFFER_FORAGING_RANGE`
- `ARMADILLO_SCRUB_RANGE`
- `TURTLE_BEACH`
- `AMPHIBIAN_WETLAND`
- `AXOLOTL_WATER_RANGE`
- `SPIDER_LAIR`
- `BAT_ROOST`

Important design rule:

Herd animals have ranges, not dens. Guards have territory purpose, not cozy home life.

## Flee, Regroup, Hunt, And Combat

Flee:

- `RetoldControlledFleeEvents`
- `RetoldCreeperAwareness`
- prey flees from serious threats
- fish and land prey are handled
- successful damage immediately seeds the same ten-second flee memory for shared passive prey;
  the causing entity is preferred as the escape origin, projectile/explosion positions are used when
  available, and source-less environmental damage produces a random panic direction
- a successful hit from a living attacker that leaves a wild ordinary Wolf, Fox, Cat, Ocelot,
  Dolphin, Spider, or Cave Spider below 25% health seeds a separate ten-second wounded-flight
  memory, clears ordinary hunt/retaliation target ownership, and claims `FLEE` as `FLEEING`
- exactly 25% health does not trigger wounded flight; tamed predators, Undead, bosses, profile
  territory guards, active `TERRITORY` ownership, and `TERRITORY_ATTACK` targets are exempt
- wounded flight suppresses ordinary controlled retaliation and Dolphin pod retaliation for the
  victim while active, then releases only its own reasoned flee claim when the memory expires
- panic can spread through nearby herd-like mobs
- successful Squid damage immediately broadcasts panic through one bounded cached scan to nearby
  ordinary Squid only, or nearby Glow Squid only; the normal receiver-side scan remains a fallback
- cats retreat from nearby creepers before ignition; creepers retain vanilla cat/ocelot avoidance
- an active creeper fuse uses cached sight/hearing awareness, species-banded reaction delays, and
  high-priority flee control for mobile pathfinding and flying mobs; zombie-family mobs do not flee
- creeper flight interrupts existing combat and guard movement and retains a short last-known danger
  memory so a creature does not stop at the edge of its scan radius

Regroup:

- `RetoldControlledRegroupEvents`
- grazers and small foragers regroup when isolated or scared
- regroup should stop if predator pressure is present

Hunting and search:

- `RetoldPredatorSearchEvents`
- `RetoldControlledHuntingEvents`
- `RetoldPackHuntingEvents`
- `RetoldPackSenses`
- `RetoldPackCombat`
- `RetoldPredatorStrike`
- `RetoldPredatorAttackGuards`
- `RetoldBatColonyEvents`

Combat target ownership:

- `RetoldAiTargets`
- `RetoldCombatTargets`
- `RetoldFactionTargetMemory`
- `RetoldTargetSource`

`RetoldAiTargets` synchronizes `ATTACK_TARGET` for brain-backed Retold combatants that do not read
the ordinary `Mob` target field, currently Axolotls and Piglins. Axolotl-to-Guardian assignments
are source-sensitive: only `RETALIATION` and `FACTION_ASSIST` may pass the global hostility guard.
Ordinary vanilla brain writes and Retold hunting remain blocked.

Standing tamed predators inspect both five-second vanilla owner interaction memories: the entity
that hurt the owner and the entity the owner hurt. A valid target receives explicit
`OWNER_DEFENSE` ownership before lower-priority controlled combat continues. That source is
protected from hunger cleanup and warning suppression while still using the tamed animal's vanilla
`wantsToAttack` exclusions; sitting animals do not begin or continue owner defense.

Ordinary predators use the same controlled-combat owner for direct self-defense. A successful hit
records a valid living attacker for five seconds, claims `ATTACK` control, and assigns the target
with `RETALIATION` ownership. Existing owned retaliation can continue after the transient damage
memory clears. The central target policy, `canAttack`, tame-owner safety, and wounded-predator flight
gate are checked before assignment, so this replacement cannot bypass alliance, Creeper, player-mode,
or below-25% escape rules.

Important target rule:

Retold-owned combat is allowed. Vanilla or random prey targeting is blocked where it would bypass Retold rules.

## Territory Warning System

Territory defense is the warning/reputation system used by Nether Remnants and Illagers.

Technical owners:

- `RetoldTerritoryEvents`
- `RetoldTerritoryController`
- `RetoldTerritoryConfig`
- `RetoldTerritoryConfigs`
- `RetoldTerritoryContext`
- `RetoldTerritoryDetector`
- `RetoldTerritoryRules`
- `RetoldTerritoryReputation`
- `RetoldTerritoryMobState`
- `RetoldTerritoryMobStates`
- `RetoldTerritoryTargetBlocker`
- `RetoldTerritoryTargetSelector`
- `RetoldTerritoryBrainGuards`
- `RetoldTerritoryWitnesses`
- `RetoldWarningMovement`
- `RetoldWarningEffects`
- `RetoldWarningPose`
- `RetoldTerritoryCombat`

Faction territories:

| Faction | Territory |
| --- | --- |
| Nether Remnants | `minecraft:bastion_remnant`, `minecraft:fortress`, Nether only |
| Illagers | `minecraft:pillager_outpost`, `minecraft:mansion` |

Warning stages:

- `NONE`
- `NOTICED`
- `WARNING`
- `FINAL_WARNING`
- `ATTACK`

Suspicion sources:

- being seen inside territory
- staying too close after warning
- illegal actions like stealing or breaking protected blocks
- attacking or killing territory faction members

Hard rules:

- Vanilla target assignment must not bypass warning.
- Brain memory writes like `ATTACK_TARGET` and `ANGRY_AT` are blocked while warning is active.
- Faction assist must not convert a warning-stage player into an immediate attack target.
- Once suspicion reaches `ATTACK`, territory combat may set the target normally.
- Direct retaliation can still happen if the player attacks a guard.
- Creative and spectator players must never remain valid aggro targets.

Deterministic GameTests cover both territory structure tags and every configured Nether Remnant
and Illager member. The shared lifecycle test covers survival-player observation, premature-target
suppression, final-warning delay, territory-owned attack transition, creative/spectator exclusion,
and immediate retaliation. These tests use a synthetic territory context: generated structure-piece
detection, warning formation navigation, sounds, particles, and multiplayer remain in-game checks.

## Guard Purpose

Territory guards defend zones and posts. They should not become ordinary hunger/home mobs.

Technical owner:

- `RetoldTerritoryGuardEvents`

Examples:

- guardian
- elder guardian
- piglin brute
- blaze
- iron golem
- snow golem
- shulker
- wither skeleton

Guard behavior:

- create or repair guard post
- leash target distance from post
- return to post if pulled too far
- release invalid creative/spectator targets
- preserve special boss behavior where needed

## Tick Pipeline

Most AI entity tick behavior is routed through:

- `RetoldBehaviorEntityTickDispatcher`

The dispatcher exists for performance. Instead of registering many independent entity tick subscribers, it:

1. receives one entity tick event
2. routes explicitly supported special-hierarchy mobs such as Bats, Ghasts, and Phantoms through
   bounded species adapters before the ordinary `PathfinderMob` split
3. checks whether other entities are `PathfinderMob`s
4. gets the cached profile once from `RetoldAiTickContext`
5. routes only relevant behavior handlers for that profile
6. applies dispatcher-level cadence gates before calling handlers

Examples:

- grazers route to flee, recovery/repair, regroup, herd range
- small foragers route to flee, recovery/repair, regroup, small home
- pack predators route to predator search, pack hunting, pack home
- solo opportunists route to predator search, solo home, held food
- territory guards route to guard post logic
- illager raiders route to roaming and territory guard logic
- commander-support mobs route to rear-positioning and ally-target adoption; witches enter that
  behavior only while attached to an active raid
- aquatic-school fish route to exact-species cohesion on a ten-tick dispatcher cadence; their
  movement uses ordinary aquatic navigation and yields to targets and higher-priority ownership
- bats route to their colony adapter, whose cached scans and owned flight hook cover feeding,
  five-member directional hunting parties, selective panic, and upward searches for personal
  daytime ceiling slots without using the ordinary ground-mob behavior route; vanilla retains
  physical roost validation and ordinary unowned flight

Classes with other event types, such as server tick, death, or damage events, remain registered normally.

## Scheduling, LOD, And Performance

Performance is a core part of the AI design.

### Timing

Technical owner:

- `RetoldBehaviorTiming`

`shouldThink(entity, gameTime, interval)` controls whether a behavior runs. It uses:

- entity id offset to spread work across ticks
- LOD-adjusted timing
- per-entity same-tick timing result cache

### LOD

Technical owner:

- `RetoldAiLod`
- `RetoldAiLodLevel`

LOD levels:

- `FULL`
- `NEAR`
- `FAR`
- `BACKGROUND`

LOD affects:

- behavior timing intervals
- cache lifetimes
- path start cadence

Important mobs stay `FULL`, including mobs with:

- active target
- recently hurt attacker
- Retold control
- recent danger memory

The goal of LOD is not to make mobs visibly broken. It should reduce low-priority thinking for mobs far from players while keeping active or dangerous situations responsive.

### Work Budgets

Technical owner:

- `RetoldAiWorkBudget`

Budgets limit expensive work per tick:

- entity scans
- position scans
- sight raycasts
- block searches

When budget is exhausted, systems prefer cached or stale-safe results instead of doing new expensive world queries.

### Caches

Technical owners:

- `RetoldAiTickContext`
- `RetoldAiScanCache`
- `RetoldAiSightCache`
- `RetoldBlockTargetSearch`
- `RetoldForageBlockSearch`

Cache purpose:

- profile lookups use an atomically replaced entity-type index, avoiding per-mob/per-tick context
  allocation while retaining datapack-reload correctness; entity paths use the small type cache
- scan cache reuses nearby entity queries by mob, position, shared bucket, and radius bucket, and
  does not construct search bounds for per-mob cache hits
- sight cache avoids repeated line-of-sight raycasts
- block search cache avoids repeated forage/block scans; fixed-center searches remain reusable while
  the requesting mob travels, and Bat ceiling searches check the founding column first and stop at
  the first valid nearby column
- targeted environmental-forage misses retry after eight ticks, and their shared-budget claim uses
  a weak FIFO queue. When deferred claimants exist, one of the existing eight block-search starts per
  tick is reserved for the queue head; dead, removed, garbage-collected, or stale claimants are
  discarded. A denied forage claimant holds position instead of starting a generic random food
  search that could carry it away from an already-present habitat source before its reserved turn.
  This prevents deterministic starvation without increasing the global work cap
- Animal Feeder searches cache both hits and safe negative results per mob, remain LOD-aware, and
  share the global block-search budget before examining the bounded local volume
- feeding poses add one constant-time weak-map lookup only for loaded mobs and do not scan, search,
  or create paths; a pose removes itself after 40 ticks or as soon as another owner interrupts it

LOD memory is mutable and refreshed in place after the initial per-mob entry, avoiding a new
short-lived cache record every ten ticks. These allocation reductions do not change timing,
distance bands, work budgets, or behavior ownership. Loaded-mob profiling with
`/retoldbehavior perf reset` and `/retoldbehavior perf` remains required for balance-sensitive
cadence or budget changes.

### Loaded-Mob Regression Test

`RetoldAiPerformanceGameTests` owns an isolated synthetic stress scenario. It creates 32 each
of eight always-ticking managed animal species spanning grazer, small-forager, Panda, Frog, and
Armadillo profiles, and retains full Retold LOD for a 200-server-tick observation window. The
test asserts that the population survives, timing/LOD work occurs, at least half of entity-scan
requests are cache hits, and successful entity, position, sight, and block work never exceeds
the corresponding per-tick budget. It logs server-tick wall time, requests, cache hits, budget
skips, and path requests for comparison between revisions; wall time is diagnostic rather than
a pass condition because host and CI hardware differ.

The latest recorded mixed-profile stress result completed with all 256 mobs alive and every budget assertion passing.
Its deliberately compact arena reported 14.475 ms average wall time per server tick, 24,757
entity-scan cache hits from 25,081 requests, 57 entity-scan budget skips, 405 position-scan hits
from 434 requests, and 97 unskipped path requests. This synthetic result is a repeatable
regression baseline, not a substitute for natural mixed-hostile populations, multiple players,
dedicated-server observation, or profiler/JFR evidence.

### Per-Mob TPS Matrix

`RetoldPerMobTpsGameTests` registers one independent 50-subject test for each of the 83 loaded
mob profiles. Every species is measured through idle/rest, dropped-food/forage, hunt/target,
danger/social, and habitat/day-night phases. Profile-appropriate fixtures provide water, caves,
Nether ground, prey, threats, forage, hives, and other required stimuli. Each phase records real
server-tick wall time and the Retold scan, path, sight, and block-search counters, then fails if its
average reaches the 50 ms/tick 20-TPS limit.

The original clean 2026-08-03 baseline passed all 68 tests and 340 phases. Bat hunting was the most
expensive remaining phase at 11.833 ms/tick. The first run identified a Sniffer range search that
nested an 11x5x11 nearby-diggable scan inside every candidate of a 37x9x37 range scan, reaching
216.917 ms/tick. New Sniffer range anchors now require the candidate itself to be diggable, reducing
the clean-run Sniffer peak to 3.340 ms/tick. Absolute wall-clock values remain host-dependent; see
[`mob_tps_benchmark.md`](mob_tps_benchmark.md) for the command, caveats, and complete table.
The post-Bat-isolation rerun again passed all 68 tests and 340 phases in 1.284 minutes; Bat
habitat/day-night was highest at 8.438 ms/tick.
After adding four school-fish and two loose-Squid profiles, the final rerun passed all 74 tests and
370 phases in 1.396 minutes. Skeleton idle/rest was the host-load-dependent overall peak at
7.516 ms/tick; the six added aquatic profiles peaked at 2.905 ms/tick for Cod danger/social.
Adding the Villager, Strider, and Nautilus profiles expanded registration to 77 tests and 385 phases.
The latest complete baseline remains the earlier 75-test/375-phase pass; focused Strider and
Nautilus runs passed all ten new phases below 50 ms/tick, and Cow passed all five representative
breeder phases. Parrot expanded registration to 78 tests/390 phases; its focused five-phase run
passed with a 4.005 ms/tick peak. Skeleton Horse, Zombie Horse, and Camel Husk now expand current
registration to 81 tests/405 phases. Their fifteen focused phases passed below 50 ms/tick, peaking
at 5.628, 4.593, and 5.843 ms/tick respectively. A complete expanded rerun was intentionally not
used for these focused changes.
Zombie Nautilus now expands current registration to 82 tests/410 phases. Its initial focused
five-phase run and the affected Zoglin rerun passed below 50 ms/tick, peaking at 5.676 and 5.147
ms/tick. After adding same-Undead Mob/Brain target enforcement, both exact selectors passed again,
peaking at 4.437 and 5.091 ms/tick respectively; the complete expanded matrix was intentionally
not selected.
Wildfire expands current registration to 83 tests/415 phases. After extending only its Ghast
faction scan and retention boundary to 64 blocks, its exact five-phase 50-mob run passes below
50 ms/tick with a 5.967 ms/tick peak; the complete expanded matrix was intentionally not selected
because this unmanaged profile reuses existing faction and work-budget primitives.
After adding its scan-free lava lift and bounded combat repositioning, the exact selector still
sustains 20 TPS in all five phases with a 4.081 ms/tick peak. The complete matrix remains
unnecessary because both repeated paths are Wildfire-local.
After adding lava-first recovery routing, persisted submersion, full health healing, and resurfacing,
the exact selector still sustains 20 TPS in all five phases with a 5.092 ms/tick peak. The complete
matrix remains unnecessary because the active path is Wildfire-local and retains the shared
block-search budget/cache.
After adding obstacle-aware climb corridors and budgeted formation flight routes, the exact selector
still sustains 20 TPS in all five phases with a 5.026 ms/tick peak. That fixture contains leaders
without natural escorts; the focused wall-crossing formation test exercises the active group route,
so the complete matrix remains unnecessary.
After restricting recovery to cached lava columns at least three blocks deep, the exact selector
still sustains 20 TPS in all five phases with a 4.876 ms/tick peak. The complete matrix remains
unnecessary because the additional constant-depth validation stays inside the existing bounded,
cached Wildfire block search.
The Stage 2 Undead-pressure rerun forces `UNDEAD_HUNGRY` and `UNDEAD_TOLERANT` benchmark fixtures
to Stage 2, restores the previous saved stage during cleanup, and covers all eight affected species.
All 40 exact phases passed below 50 ms/tick; Zombie hunt/targeting was the 6.046 ms/tick overall
peak. The complete 82-profile matrix was not selected because the new cached coordination work is
confined to these two existing profile families.
The specialized Wither threat selector adds no profile but changes repeated `APEX_OR_BOSS` work.
Its exact 50-mob run passes all five phases below 50 ms/tick, measuring 4.757 idle/rest, 2.674
dropped-food/forage, 5.710 hunt/targeting, 3.225 danger/social, and 1.915 habitat/day-night
ms/tick. The complete matrix remained unnecessary because no shared cache, budget, or other
profile path changed.
The focused Bee defense rerun makes its danger phase deal real damage so the production colony
event and staggered continuation are measured; all five phases passed with a 7.103 ms/tick peak.
After natural acquisition was added, focused 50-mob Armadillo, Nautilus, and Strider runs again
passed all 15 affected phases, initially peaking at 5.760, 4.544, and 4.602 ms/tick respectively.
The final Strider lava-sustenance rerun passed all five phases with a 6.771 ms/tick peak. Death-event
meal credit added no repeated tick work, so Piglin, undead, and Cube TPS cases were not selected.

## Debug Commands

Primary debug command root:

```mcfunction
/retoldbehavior
```

Important views:

- `/retoldbehavior get`
- `/retoldbehavior nearby`
- `/retoldbehavior toggle overlay`
- `/retoldbehavior warning`
- `/retoldbehavior perf`
- `/retoldbehavior perf reset`
- `/retoldbehavior home`
- `/retoldbehavior guardpost`
- `/retoldbehavior pack`
- `/retoldbehavior targets`

`/retoldbehavior warning` should show:

- territory context
- config faction
- warning target
- attack target
- warning level
- suspicion and attack threshold
- started attack yes/no
- warning pulses
- warned intruder count
- next warning pulse
- next target recheck
- final warning age
- prepared warning shot fired yes/no

`/retoldbehavior perf` shows the health of the performance system:

- timing checks and passes
- timing cache hits
- LOD distribution
- territory checks and cache hits
- AI scan requests, hits, and budget skips
- position scan requests, hits, and budget skips
- path requests and skips
- sight requests, hits, and budget skips
- block search requests, hits, and budget skips
- block-target positions examined, which exposes the actual size of cache misses rather than counting
  a small and a very large search equally

Interpreting common counters:

- High timing checks means too many handlers are still being asked to run.
- High scan requests means behavior is doing many nearby entity queries.
- Low scan cache hit rate means cache sharing is weak.
- High scan budget skips means AI is asking for more world scans than the per-tick budget allows.
- High path skips means path starts are being throttled heavily.
- High max MSPT with low average MSPT means spikes, usually from scans, pathing, or block searches.

## Current Performance Architecture

Current performance passes include:

- single behavior entity tick dispatcher
- profile-routed dispatch
- dispatcher-level behavior cadence gates
- per-tick profile/path context cache
- LOD-adjusted behavior timing
- same-tick timing result cache
- shared entity scan buckets
- rounded shared radius scan keys
- stale shared scan grace during budget pressure
- path start throttling
- sight raycast cache and budget
- block search cache and budget
- old inactive Retold state cleanup

Expected result:

- stable 20 TPS under moderate mob loads
- lower MSPT average and max spikes
- high AI scan cache hit rate
- low AI scan budget skips
- no obvious visible LOD difference for nearby or active mobs

## Adding A New Mob Behavior

Use this checklist:

1. Decide the profile in `RetoldMobProfileType`.
2. Add `data/retold/mob_profiles/<entity>.json`; do not add a Java registration.
3. Add relationship/faction rules only if diplomacy changes.
4. Add state fields only if existing `RetoldMobState` cannot represent it.
5. Route the behavior in `RetoldBehaviorEntityTickDispatcher`.
6. Use `RetoldBehaviorTiming.shouldThink` inside the behavior.
7. Use `RetoldAiScanCache`, `RetoldAiSightCache`, and block search caches for expensive queries.
8. Use `RetoldAiControl` for movement/control ownership.
9. Use Retold target helpers for combat targets.
10. Add debug output if the behavior can be hard to understand in-game.
11. Compile and test with `/retoldbehavior perf`.

Do not:

- add a new always-on entity tick subscriber for normal AI
- call world scans directly in hot paths unless there is a strong reason
- write vanilla attack targets directly from high-level behavior
- let creative or spectator players remain targets
- make guards use ordinary animal home/hunger behavior
- make boss/special mobs lose their vanilla identity

## Completion Matrix

Use this matrix before calling the mob AI system done.

### Global Rules

| Area | Expected behavior | Done when |
| --- | --- | --- |
| Target ownership | Retold-owned targets go through `RetoldCombatTargets` / `RetoldFactionTargetMemory`. | Direct `setTarget`, `setAggressive`, `ATTACK_TARGET`, and `ANGRY_AT` writes only exist in low-level guard helpers, and debug shows source/current target ownership. |
| Invalid players | Creative and spectator players are never valid retained targets. | `/retoldbehavior get` shows no lasting target or brain target for creative/spectator players. |
| Creeper safety | No mob deliberately targets or directly melees a creeper; mobile non-zombies flee an active fuse and cats avoid creepers before ignition. | Vanilla, Retold-owned, brain-memory, retained-target, and direct-melee paths reject creepers. Cached awareness produces delayed high-priority flight for pathfinding/flying mobs, and cat retreat preserves vanilla creeper avoidance. |
| Ordinary predator self-defense | Healthy ordinary predators retaliate after successful damage from a valid living attacker. | Real-damage coverage includes wild Wolf, tamed Wolf, Fox, Cat, Ocelot, Dolphin, Spider, and Cave Spider; target and `ATTACK` control use `RETALIATION` ownership, ownership continues after transient damage memory clears, and tame-owner safety is preserved. |
| Wounded predator disengagement | Wild ordinary predators flee a living attacker for ten seconds when a successful hit leaves them below 25% health. | Hunt/retaliation targets and ownership clear before reasoned `FLEEING` control begins; the exact boundary and tamed, Undead, boss, and territory exemptions are covered, all seven profile species enter the same bounded continuation, and affected exact TPS selectors remain below 50 ms/tick. |
| Mob griefing | Mob-caused terrain edits obey `mobGriefing`; consuming dropped items does not count as terrain editing. | Retold forage, weak-barrier, and Gale Core paths use `RetoldMobGriefing`, vanilla creeper explosions remain behind NeoForge's entity-griefing hook, and each destructive owner has regression GameTest coverage. |
| Loaded starvation | Every mob with a positive Retold hunger interval can lose health and die at 100 hunger; Cube Mobs retain their separate response. | Ordinary `PathfinderMob`, Bat, and Villager hunger ticks share `RetoldStarvationBehavior`, profiles with hunger disabled are excluded, feeding below 100 stops pulses, and Cube Mob splitting remains covered separately. |
| Unloaded reconciliation | Returning hunger-aware mobs reconcile no more than seven days through bounded real transactions. | The deduplicated queue attempts at most 16 mobs per tick; daily food order is feeder, accessible forage, then compatible reachable wild prey for untamed predators. Fresh scans and bounded route probes prevent barrier bypass. Critical pulses become one damage/Cube transaction, one Farmer per tick can process real owned crops and storage, and one returning chunk per tick delegates a daily attempt to vanilla spawning. Named/tamed mobs retain a one-health floor. |
| Faction assist | Nearby allies can help only when target gating allows it. Witches cooperate with Illagers only inside the same active raid. | Assist does not bypass warning-stage players in territory; Witch/Illager GameTests cover ordinary neutrality, same-raid assistance, different/no-raid rejection, and raid-exit target cleanup. |
| Raid progression | Bad Omen and vanilla raid creation cannot begin a raid before Stage 3; active raids are not cancelled by the start gate. | The authoritative saved stage is checked both before Bad Omen conversion and at `Raids.createOrExtendRaid`; the live creation path has a Stage 2 rejection GameTest and the natural Stage 3 omen flow is verified in-game. |
| Territory warning | Nether Remnants and Illagers warn before attack in configured structures. | Bastion, fortress, outpost, and mansion all show warning progression before attack. |
| Retaliation | Directly attacking a guard can still trigger immediate retaliation. | Player hit on guard bypasses warning only for retaliation, not passive sight. |
| Debug | Debug output explains why a mob is or is not controlled. | `get`, `nearby`, `toggle overlay`, `targets`, `warning`, `home`, `guardpost`, and `pack` give actionable state. |

### Territory Factions

| Faction | Members | Territory | Expected player behavior |
| --- | --- | --- | --- |
| Nether Remnants | piglin, piglin brute, blaze | bastion remnant, fortress in Nether | Notice player, warn, posture, move into warning positions, escalate by suspicion, attack only at `ATTACK` or retaliation. |
| Illagers | pillager, vindicator, evoker, illusioner, ravager, vex | pillager outpost, mansion | Same warning/reputation rules as Nether Remnants. |
| Illager loose ally | witch | none as full member | Remains neutral outside raids; can align and support only Illagers in the same active raid, without gaining territory membership. |

### Managed Behavior Profiles

| Profile | Mobs | Primary behavior to validate |
| --- | --- | --- |
| Hungry grazer | cow, mooshroom, sheep, goat, horse, donkey, mule, llama, trader llama, camel | Hunger, grazing/eating, home range, herd panic/flee. |
| Small forager | pig, chicken, rabbit | Hunger, foraging, home return, predator flee. |
| Pack predator | wolf | Pack creation, pack hunt/search/return, den defense, target ownership. Ordinary food hunts release as soon as the predator is satisfied. Individual scouts must retain hunt drive, and a satisfied leader of an active hunt/search transfers leadership and search ownership to an available hungry member. Retaliation and territory defense are excluded from this cleanup. Integrated coverage verifies solo release, urgent-target protection, leadership transfer, and hunger-gated prey validation; natural feeding transitions, multi-candidate selection, and group movement remain unverified. |
| Solo opportunist | fox, cat, ocelot | Solo home behavior, opportunistic hunting, flee/return. |
| Aquatic predator | dolphin | Pod hunting remains hunger-driven. Successful damage starts collective defense through one cached bounded 28-block scan: the victim uses `RETALIATION`, nearby available witnesses use `FACTION_ASSIST`, and both routes hold `AQUATIC_POD` attack ownership until the threat is invalid or beyond 36 blocks. Recruits must be within six blocks of the victim or have cached sight of the attacker, and another live target is never overwritten. Focused behavior coverage exercises real damage, fed assistance, busy-podmate exclusion, and cleanup; the five-phase 50-Dolphin run peaks at 5.675 ms/tick. Natural aquatic pursuit and multiplayer remain unverified. |
| Aquatic school | cod, salmon, tropical fish, pufferfish | Cached exact-species cohesion, a shared persisted `AQUATIC_SCHOOL_RANGE`, low-priority `REGROUP` ownership, reachable aquatic paths, and 520-tick loaded hunger. Tagged seagrass/kelp anchors the range and supplies higher-priority food ownership under `mobGriefing`; when the hungry school is locally depleted, one deterministic member evaluates and shares a bounded better range. Focused coverage exercises all four profiles, the protected transaction, food anchoring, shared migration, and a real route. Natural school stability, migration pacing, and plant-depletion balance remain unverified. |
| Loose aquatic group | squid, glow squid, nautilus | Successful damage produces exact-species nearby panic through bounded cached propagation for the two Squid species. Squid and Glow Squid use 520-tick loaded hunger and consume tagged dropped raw fish without hunting living prey. Wild hungry Nautiluses use controlled aquatic navigation to hunt living fish; tamed Nautiluses do not hunt autonomously. Natural long-term group and diet pacing remain unverified. |
| Hungry swarm predator | spider, cave spider | Hunger-driven prey selection admits adult passive animals only at night, while proactive player aggression remains darkness-based and retaliation remains available at any time. Daylight ends Retold-owned food hunts. Cached swarm scans allow both Spider types to share an owned prey target at night. After a recent feeding or successful hunt, the persisted `SPIDER_LAIR` home lets up to six members share a dark lair, expand or repair one real cobweb per 600 ticks up to 50, and return during daylight through interruptible low-priority ownership. Construction requires the builder's vanilla darkness, raw skylight below 8 at both the builder and supported dark air block, the shared block-search budget, and `mobGriefing`; open-sky nighttime darkness is insufficient. Integrated coverage verifies the hunting boundaries plus bright/open-sky rejection, sheltered creation, sharing, the 50-web cap, repair, griefing, return, retaliation interruption, and night release; natural climbing, combat, site selection, and long-term pacing remain unverified. |
| Hive colony | bee | Flower foraging retains its bounded specialized search. Successful Bee damage, tagged-hive breaking, and unsmoked full-hive harvest are the only Retold colony-defense triggers. The victim uses `RETALIATION`; one cached bounded 18-block incident scan recruits available Bees with `FACTION_ASSIST`. Both hold `HIVE_COLONY` attack ownership until the threat is invalid or beyond 36 blocks. Smoke, Creative/Spectator targets, friendly Bees, and another live target are excluded. Event assignment starts no synchronous path; staggered species ticks refresh ownership and request movement. Focused behavior coverage exercises real damage, source ownership, recruitment, busy-target preservation, prompt cleanup, smoke, player-mode exclusions, harvest, and breaking. The five-phase 50-Bee run peaks at 7.103 ms/tick. Natural hive release, crowded colonies, multiplayer, and dedicated servers remain unverified. |
| Nether hungry | piglin, hoglin, strider | Hunger behavior plus faction/territory interactions for Piglins. Lava passively sustains Striders without being consumed, Warped Fungus remains fallback food, and Piglins receive meal credit from Hoglins they kill. |
| Undead hungry | zombie, zombie villager, husk, drowned, zombified piglin | Hunger/horde behavior, faction targeting, and undead tolerance. Stage 1 uses 10-block same-family sharing and 12-block notice; Stage 2 expands these to 22 and 18 blocks, permits stable one-in-three cross-family assistance within 12 blocks when the incident is heard or seen, and adds about 25% to already-present tagged natural-spawn weights under vanilla caps. A non-undead, non-Creeper living victim killed by one of these mobs provides a meal. Proactive hungry-target scoring has no player bonus. The five exact Stage 2 50-mob runs peak at 6.046 ms/tick. |
| Undead tolerant | skeleton, stray, bogged | Ranged behavior, faction targeting, no hunger loop, and no player bonus when choosing a firing position. Stage 1 uses 10-block same-family sharing and 14-block notice; Stage 2 expands these to 24 and 22 blocks, uses the same bounded imperfect cross-family assistance contract, and shares the tagged natural-spawn weight bonus. The three exact Stage 2 50-mob runs peak at 4.952 ms/tick. |
| Undead mount | skeleton horse, zombie horse, camel husk | Ownerless mounts retain Undead diplomacy and use a bounded cached 24-block scan, sight/close-awareness checks, source-aware faction targets, six-tick dispatch, throttled paths, and real melee damage. A persisted owner reference removes the generic faction identity. Claimed mounts never scan for prey; successful damage and five-second owner interaction memories alone start source-aware retaliation or owner defense under `UNDEAD_MOUNT` attack ownership. Mounting claims an ownerless already-tame riderless Skeleton Horse or Camel Husk, while Zombie Horse keeps vanilla bucking/taming. Two focused tests cover all three claim/faction boundaries, real damage, defense, cleanup, and non-hunting. Fifteen focused 50-mob phases pass below 50 ms/tick with a 5.843 ms/tick overall peak; natural pursuit, riding, multiplayer, dedicated-server, and save/reload behavior remain unverified. |
| Phantom stalker | phantom | Vanilla retains the hostile-spawn gamerule, 60–120-second custom-spawner cadence, dark-sky gate, spectator exclusion, placement validation, and generated group size. At lowest event priority, Retold preserves explicit decisions from other mods and replaces only a default per-player insomnia decision: skylight dimensions require open sky, the Retold night-or-storm context, vanilla local difficulty, and a one-in-eight rarity result. The existing bounded stalker uses owned faction-combat targets without arbitrary player priority and retains Undead diplomacy. Two focused tests cover zero-insomnia eligibility, policy boundaries, event compatibility, and closer ordinary prey versus an Undead neighbor and player. Natural frequency, storms, movement/combat, multiplayer, and dedicated servers remain unverified. |
| Ghast artillery | ghast | Artillery targeting and faction exclusions without an arbitrary player score bonus. |
| Zoglin rampager | zoglin | Undead faction identity, rampage targeting, and owned attack targets without an arbitrary player score bonus. Raw same-Undead Mob/Brain targets are rejected or cleared unless explicitly owned as retaliation. The latest affected 50-Zoglin run peaks at 5.091 ms/tick. |
| Slime hungry | slime, magma cube | `RetoldSlimeHungerCombat` gates target assignment, retained combat, swarm assistance, and every contact-damage path at the profile hunt threshold; only hungry Cube Mobs use faction targeting and `RetoldCubeMobContactDamage`. The central live-faction policy rejects ordinary same-family Mob/Brain targets while retaining explicit Retold-owned retaliation. A non-Cube, non-Creeper living victim killed by either species relieves hunger. The contact hook also lets hungry size-one members use vanilla `dealDamage`, while fed members remain harmless. Independently, `RetoldMobRules.wantsDroppedFood` gives this profile an unconditional item appetite: `RetoldFoodBehaviorEvents` and `RetoldSwarmScavengerEvents` seek items even at zero hunger. `RetoldCubeMobMovement` translates shared movement requests into Cube Mob controller headings and hop speed while the narrow random-direction mixin prevents vanilla wandering from overwriting an owned direction. `RetoldSlimeSplitBehavior` gives every split child half of its parent's current hunger through NeoForge's standard mob-split event and the explicit starvation path. `RetoldSlimeStarvationBehavior` scales each hunger gain as `ceil(size / 2)`, splits size-two-or-larger Cube Mobs into two half-size children at 100 hunger with 50 hunger each, preserves swallowed storage once, starts their merge cooldown, and kills size-one members through normal death. `RetoldSlimeItemStorage` swallows any complete dropped stack, persists exact stack components, returns contents through death drops, and grows Cube Mobs one size at a time up to size 10 with exponentially doubling costs from 16 through 4,096 items. Cached same-species/same-size idle merging through `RetoldSlimeMergeBehavior` remains restricted to natural sizes 1-to-2-to-4 with a persisted cooldown and transfers swallowed contents. The latest focused Slime and Magma Cube runs peak at 4.488 and 5.665 ms/tick. |
| Small arthropod swarm | silverfish, endermite | Species-local swarm behavior and separate faction targeting; the two species remain neutral and never coordinate with each other. |
| Bat colony | bat | Persisted hunger and `BAT_ROOST` homes are supported for this non-pathfinding `Mob`. A loaded home must remain a dark supported ceiling cell; legacy ground homes and broken supports are repaired upward around the previous anchor. Up to 12 nearby Bats share the resulting broad 16-horizontal/8-vertical colony anchor. Daylight clears food and combat directives, then each awake Bat searches upward within eight horizontal and 32 vertical blocks for a personal currently dark supported ceiling cell. Discovery checks the founding column first and exits at the first valid nearby support instead of scoring the full volume; individual Bats then reserve distinct supported cells around that anchor. In-flight destinations plus settling and occupied cells remain reserved, and an already-stacked sleeper with the higher entity ID drops clear before rerouting. The explicit-center cache and selected five-second route remain reusable while the Bat travels. A bounded vanilla `FlyingPathNavigation` route leads toward that slot; Retold flight ownership then performs a collision-checked final approach with minimum lift, holds the Bat against vanilla wandering, and completes its individual 8-to-40-tick settling delay. Vanilla resting-to-awake disturbances and unrelated danger clear shelter state and hold the Bat in owned panic flight for ten seconds before settling can resume. At night, a hungry Bat prefers a reachable dropped Spider Eye, then joins compatible hungry members in a party capped at five. Party-wide sensing and coordination run at most once per eight ticks, incomplete parties retry recruitment once per 40 ticks, the direction lasts 20 seconds, and unchanged routes and four-tick separation vectors are reused. Frightened, feeding, and sheltering Bats remain outside parties. Feeding, search, hunt, dodge, and panic destinations require reachable paths. Close bites deal one damage; local separation, individual arthropod dodges, and selective delayed unrelated-danger panic remain in effect. Isolated coverage verifies legacy/broken-home repair, tall-cave ceiling acquisition, distinct destinations and occupied slots, repair of a stacked pair, eight awake Bats completing a real-tick daytime return, a ten-second disturbance recovery window, party behavior, and a 64-Bat day/night workload that must remain below 50 ms/tick. Clientless GameTest players are now excluded from login payload synchronization, and broken-roost coverage retries across shared AI budget windows while still requiring a different valid dark supported ceiling, shelter ownership, and a real flying path. The latest focused selection passed 7/7, including the 64-Bat workload at 10.658 ms/tick; the post-ecology complete suite passed 150/150. The developer reported the ordered natural Bat acceptance pass works on 2026-08-03; dedicated-server, multiplayer, profiler, and existing-world verification remain unconfirmed. |
| Protective neutral | polar bear | `RetoldNeutralWildlifeEvents` uses cached cub/threat scans. Passive cub intruders receive a 40-tick standing/sound warning with no attack target; the stationary warning keeps the bear's body, head, and look control aimed at the intruder. The intruder can withdraw; staying escalates to owned defense, while actual attacks bypass the warning. Vanilla proactive cub-proximity targeting is blocked. GameTests cover the state boundaries; natural navigation and warning readability still need in-game verification. |
| Armadillo defensive | armadillo | Defensive/flee behavior plus active hunger. A bounded cached soil search drives low-priority movement to exposed eligible ground; reaching it produces a visible non-destructive grub dig, hunger relief, and a 30-second forage cooldown. |
| Panda bamboo | panda | Its dedicated bounded search approaches bamboo and atomically removes the reached block without drops before applying hunger relief. Consumption obeys the shared entity-griefing policy. Two isolated GameTests cover natural approach/removal and `mobGriefing=false`; the focused Panda survival case passes and all five 50-Panda TPS phases remain below 50 ms/tick with a 9.305 ms/tick peak. Natural tall-grove selection and long-term grove depletion remain unverified. |
| Parrot forager | parrot | Loaded hunger uses the shared dropped-food and bounded crop-forage owner with flying navigation, feeding pose, caches, work budgets, and destructive `mobGriefing` policy. Tamed entity and shoulder Parrots first recognize a recent owner attacker, then use a staggered cached 18-block scan for a visible or close-heard mob actively targeting the living non-Creative/non-Spectator owner. The transient sound-and-particle warning takes no combat or movement ownership. Focused coverage verifies diet/griefing, false-positive rejection, threat memory, pacifist behavior, shoulder persistence, profile loading, crop survival, and five 50-Parrot phases below 50 ms/tick with a 4.005 ms/tick peak. Natural flight, crop choice, warning readability, multiplayer, and dedicated-server behavior remain unverified. |
| Sniffer forager | sniffer | Its specialized diggable-ground owner is protected from generic search ownership; paths target air above the ground and use a large-body completion radius before applying forage relief. |
| Turtle beach | turtle | Beach/home behavior plus active hunger and seagrass forage. |
| Amphibian forager | frog | Foraging and controlled prey targeting; valid prey killed through vanilla tongue behavior or Retold bites credits one meal. |
| Aquatic helper predator | axolotl | Hunger-driven helper targeting uses the Axolotl brain target channel. Ordinary Guardians are excluded from prey; a Guardian attack starts retaliation and cached witnessed assistance without hunger reward. Valid fish, Squid, and Drowned kills from vanilla or Retold feeding attacks credit one meal. Integrated coverage verifies blocked proactive targeting, successful defensive damage, and isolated aquatic feeding; natural aquatic pathing and group pacing still need in-game verification. |
| Aquatic territory guard | guardian, elder guardian | Guard behavior, special elder guardian behavior, and source-aware target ownership. The central live-faction policy rejects ordinary same-family Mob/Brain targets while retaining explicit Retold-owned retaliation. The latest focused Guardian and Elder Guardian runs peak at 4.580 and 5.040 ms/tick. |
| Territory guard | iron golem, snow golem, piglin brute, blaze, shulker, wither skeleton | Guard post return/leash behavior and faction/territory interactions where applicable. |
| Commander support | evoker, witch | Support behavior, Illager coordination, target ownership. |
| Illager raider | pillager, vindicator, ravager, vex, illusioner | Illager faction behavior and territory warning where applicable. |
| Special vanilla | creeper, enderman, breeze, creaking, zombie nautilus | Mostly vanilla behavior with Retold target safety protections. Wild Zombie Nautiluses additionally use Undead diplomacy; taming removes that generic faction identity, and retained enemy targets clear if the entity becomes wild again. The latest focused 50-Zombie-Nautilus run peaks at 4.437 ms/tick. |
| Apex or boss | warden, wither, ender dragon | Warden and Ender Dragon remain exempt from managed AI. The Wither retains vanilla flight, three-head firing, healing, powered state, and block destruction, while a ten-tick specialized selector uses bounded cached 40-block scans and sight checks to choose living non-Undead/non-Creeper threats. Recent attackers and creatures actively targeting the Wither outrank proximity, current-target inertia reduces churn, players receive no categorical bonus, and source-aware faction ownership supplies the primary target. Retold's generic forced-target loop defers to this owner. The primary target hook and constant-time per-tick retained-primary/two-alternative-head guards validate all three against current per-entity Retold faction and target-safety rules; wild Zombie Nautilus friendship therefore remains dynamic and taming removes it correctly. One focused test covers scoring, ownership, all three Undead additions, all three heads, retained cleanup, and the tame boundary; the five-phase 50-Wither run peaks at 5.710 ms/tick. Natural three-head combat, flight, arena destruction, target switching, mixed battles, multiplayer, and dedicated servers remain unverified. |

### In-Game Completion Tests

| Test | Command focus | Pass condition |
| --- | --- | --- |
| Bastion player entry | `/retoldbehavior warning` on piglin and brute | `Near territory: yes`, visible intruder found, warning target set, warning level progresses before attack. |
| Fortress player entry | `/retoldbehavior warning` on blaze | Same as bastion. |
| Outpost player entry | `/retoldbehavior warning` on pillager | Same as bastion. |
| Mansion player entry | `/retoldbehavior warning` on vindicator/evoker/illusioner | Same as bastion. |
| Creative/spectator entry | `/retoldbehavior get` and `warning` | No retained mob target, warning target, attack target, `ATTACK_TARGET`, or `ANGRY_AT`. |
| Illegal action | break/steal in territory, then `warning` | Suspicion increases and nearby witnesses react without instant vanilla attack unless threshold is reached. |
| Direct guard hit | hit guard, then `warning` | Retaliation attack starts immediately and is marked as started attack. |
| Multiple players | `nearby` and `warning` | Suspicion is per player and per faction territory context. |
| Faction assist near warning | `warning` and `nearby` | Assist does not convert warning-stage player into an immediate attack target. |
| Leave and return | `warning` over time | Suspicion decays as designed and warning reacquires when player returns. |

Remaining finish criteria:

1. Compile after every AI pass.
2. Keep `rg` scans clean for unauthorized target writes.
3. Validate the four territory structures in-game.
4. Tune warning timings only after structure behavior is confirmed.
5. Split the current large worktree into reviewable commits before merging.

## Validation Checklist

Territory:

- bastion piglins warn before attack
- fortress blazes warn before attack
- outpost illagers warn before attack
- mansion illagers warn before attack
- faction assist does not bypass warning
- direct guard hit still retaliates
- creative/spectator targets are dropped
- mobs cannot retain, receive, or directly melee a creeper target

Animal life:

- grazers create/use herd range; Cows/Mooshrooms share the bovine identity, equines share their
  confirmed group, and Llamas/Trader Llamas share theirs
- small foragers use home/range behavior
- prey flee and regroup
- four ordinary fish types use exact-species school cohesion and shared food-driven persisted ranges
  through aquatic paths
- Squid and Glow Squid share danger only within their exact species
- predators hunt and return
- wolves keep pack/den behavior
- dolphins keep hunger-driven pod hunting and collectively defend a successfully damaged podmate

Special profiles:

- undead horde pressure works
- skeletons keep ranged spacing
- phantom/ghast/zoglin special pressure works
- guardians and elder guardians keep monument/guard identity
- boss mobs are not broken by Retold AI

Performance:

- `/retoldbehavior perf reset`
- load the same test mob count
- compare timing checks, scan requests, scan budget skips, path skips, average MSPT, and max MSPT

## Refactor Rules

When changing this system:

- keep profile values in datapack JSON and derived design rules in `RetoldMobRules`, factions, and territory config
- keep behavior handlers focused on one behavior family
- keep expensive work behind caches and budgets
- keep dispatcher routing profile-aware
- keep safety systems broad enough to preserve invalid target cleanup
- prefer adding debug state over guessing in-game
- compile after every AI pass

## AI Agent Instructions

See the shared [AI Agent Instructions](README.md#ai-agent-instructions).
