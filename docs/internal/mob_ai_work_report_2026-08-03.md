# Mob, Faction, Ecology, And AI Work Report — 2026-08-03

> This is the consolidated record of the mob-design clarification and implementation work completed
> during the 2026-08-02 to 2026-08-03 development pass. It is a snapshot, not a replacement for the
> authoritative design, status, architecture, risk, issue, or benchmark documents linked below.

## Authority And Related Documents

- Confirmed design contract: [`retold_mob_ai_system.md`](retold_mob_ai_system.md#confirmed-gameplay-contract)
- Current implementation status: [`design_implementation_status.md`](design_implementation_status.md#mobs-and-ai)
- Whole-mod ownership: [`retold_mod_system.md`](retold_mod_system.md#mob-ai-factions-and-territory)
- Remaining direction: [`retold_roadmap.md`](retold_roadmap.md)
- Open design and verification risks: [`retold_design_risks.md`](retold_design_risks.md)
- Confirmed defects and failed tests: [`retold_issues.md`](retold_issues.md)
- Complete per-mob performance table: [`mob_tps_benchmark.md`](mob_tps_benchmark.md)
- Player-facing and technical change ledger: [`../../CHANGELOG.md`](../../CHANGELOG.md#next---unreleased)

If this dated report conflicts with a newer developer clarification or the status documents, use the
newer source. Implemented, partially implemented, planned, and verified are deliberately separate
claims.

## Scope Of The Pass

The developer and AI assistant reviewed the mob and faction design through an extended clarification
sequence numbered through 184. That pass established rules for universal target safety, faction
relationships, territory and reputation, village society, animal hunger and ecology, stage-dependent
creatures, pathfinding, performance, and eventual unloaded simulation. The discussion then moved into
incremental implementation and testing.

This pass produced four kinds of result:

1. Confirmed design rules that are now written down but are not necessarily implemented.
2. Gameplay behavior implemented in the existing Retold ownership pipeline.
3. Regression and performance tests for the implemented behavior.
4. Explicit follow-up items where natural-world, multiplayer, or long-duration verification is still
   missing.

## Confirmed Design Direction

The detailed wording lives in the confirmed gameplay contract. This table is the consolidated map.

| Area | Confirmed direction |
| --- | --- |
| Universal safety | Mobs do not deliberately target or melee Creepers. Snowballs still damage Creepers when they hit. Mobile non-zombies flee an ignited Creeper with species-dependent awareness and reaction delay. |
| Priority | Hunger does not override allegiance, ownership, duty, retaliation, territory defense, or urgent danger. Suitable dropped food is preferred over ordinary prey hunting. |
| Senses | Intelligent behavior uses imperfect sight, hearing, scent, communication, memory, and target inertia. Different species may emphasize different senses. |
| Undead | Undead tolerate Undead and attack living non-Undead creatures without prioritizing players. Zombies remain comparatively simple. The Wither shares Undead diplomacy. |
| Cube Mobs | Slimes and Magma Cubes are peaceful while fed, attack living targets only when hungry, always want dropped items, preserve swallowed stacks, grow expensively to size 10, split under starvation, and may naturally merge only through sizes 1, 2, and 4. |
| Guardians | Guardians remain immediately hostile everywhere, but Axolotls fight them only defensively or through witnessed assistance. |
| Village defenders | Defenders protect the village and its allies from real danger rather than attacking arbitrary political enemies. |
| Witches and Illagers | They normally ignore one another. Witches support Illagers only while participating in the same active raid and are not territory members. |
| Endermen | The attacked Enderman always retaliates; only Stage 3 recruits nearby idle Endermen. They do not proactively attack the Dragon. |
| Small arthropods | Silverfish and Endermites are separate neutral factions and coordinate only with their own species. Spiders and Cave Spiders may coordinate. |
| Territory | Nether Remnants and Illagers warn intruders before attacking unless attacked first. Nether Remnant entry depends on reputation plus gold-or-better armor; bad reputation overrides armor and hired brutes bypass both checks. The required armor-piece count remains undecided. |
| Raids | Raids begin only in Stage 3. Existing raids are not cancelled by a later administrative stage change. |
| Villagers | Villagers are pacifists, communicate danger, use reputation and food systems, and refresh stock daily. At Stage 2+, vanilla's five-Villager agreement and local recent-golem detection still decide eligibility, but only a Cleric, Librarian, Armorer, Toolsmith, or Weaponsmith may turn an eligible instant spawn into visible construction costing one emerald. Nitwits and other professions cannot construct. A successful player-built Iron Golem costs five experience levels; Creative placement is free. |
| Ordinary animals | Animals have hunger and species diets, can eat dropped food, and may consume appropriate blocks when `mobGriefing` permits it. Herds, homes, ranges, migration, and player enclosures remain species-aware. |
| Weak barriers | Desperate animals and actively hunting wild predators may break tagged weak barriers. The behavior is real-time only, path-backed, bounded, drop-preserving, and disabled by `mobGriefing`. |
| Spiders | Hungry Spiders hunt adult animals only at night, can cooperate across Spider types, and can establish shared sheltered dark lairs with up to 50 cobwebs. |
| Bats | Bats sleep around broad shared ceiling-roost areas by day and hunt arthropods in directional parties of up to five at night. Their hard destinations use obstacle-aware three-dimensional paths. |
| Other wildlife | Polar Bears warn before proactive cub defense. Dolphins defend pods. Fish school primarily by species. Bucketed fish and Axolotls remain wild. Wild Undead mounts are hostile but tameable. |
| Stage rules | Stage 2 Undead pressure comes from awareness, convergence, and spawning rather than stat buffs. Stage 3 cleanses selected Undead. Phantoms are rare nightmare-like night/storm creatures independent of insomnia. |
| Survival removals | Sniffers and Endermites remain functional in code but are unavailable through ordinary survival acquisition. Wardens, Ancient Cities, Deep Dark, Trial Chambers, Shulkers, and End Cities are outside intended normal Retold survival. |
| Special creatures | Happy Ghasts and related forms do not exist. The ordinary hostile Ghast remains. Breezes belong to the Air Temple. Creaking can remain vanilla for now. Killer Bunny, Iceologer, Tuff Golem, music-disc monster, and Wildfire remain future work. |
| Unloaded ecology | Future bounded catch-up should simulate hunger, food, predation, breeding, capacity, migration, and spawning without breaking barriers or causing chunk-load spikes. Exact cap and granularity remain undecided. |

## Implemented Work

### Global Targeting, Damage, And Safety

| Change | Implementation | Validation status |
| --- | --- | --- |
| Creeper target prohibition | `RetoldMobTargetPolicy`, `RetoldInvalidTargetEvents`, faction target guards, brain cleanup, retained-target cleanup, and the melee guard reject deliberate Creeper targets. | Deterministic GameTest covers target assignment and direct melee. Needs broad natural combat observation. |
| Invalid player cleanup | The old player-only cleanup was replaced by a general invalid-target owner that also handles global mob rules. Creative and spectator exclusions remain protected. | Covered by target and territory tests. |
| Creeper awareness | `RetoldCreeperAwareness` uses cached awareness, delayed reactions, remembered fuse danger, and high-priority movement ownership. Cats retreat before ignition; zombies do not flee. | Tests cover cats, animals, defenders, zombies, and flying mobs. |
| Damage-driven fleeing | Every successful health-damaging hit immediately seeds the shared ten-second flee memory for supported passive land and aquatic prey. Entity attackers, projectile/explosion positions, and source-less environmental damage produce suitable escape origins. Predators, defenders, and specialized danger owners remain independent. | `RetoldDamageFleeGameTests` covers Zombie, player, environmental, aquatic, and Wolf non-regression cases; natural repeated pursuit and terrain choices remain unverified. |
| Snowball damage | `RetoldSnowballEvents` makes otherwise harmless snowballs deal one health point for player, mob-owned, and ownerless/dispenser-style sources, including Creeper hits. Vanilla Blaze damage remains three. | Integrated boundary GameTest passes. |
| Vex damage | `RetoldVexEvents` halves the final incoming amount of a direct Vex strike. | Exact-health-loss GameTest passes. |
| Shared mob-griefing policy | `RetoldMobGriefing` delegates terrain edits to NeoForge's entity-griefing hook and resolves projectile owners. Forage, weak barriers, Gale Core damage, and vanilla Creeper explosions are covered. Dropped-item consumption remains allowed. | Regression tests cover all named paths. |

### Factions, Raids, Territory, And Availability

| Change | Implementation | Validation status |
| --- | --- | --- |
| General living-target relations | Faction membership and relationships now admit ordinary living creatures for indiscriminate factions while retaining Creeper, alliance, player-mode, and ownership guards. The Wither uses Undead diplomacy. | Faction regression tests pass; natural mixed battles need observation. |
| Silverfish/Endermite split | Added separate faction identities and exact-species swarm recruitment. | Neutrality and same-species assistance GameTest passes. |
| Witch/Illager raid alignment | Permanent loose-alliance identity is separate from active combat alignment and same-raid cooperation. Raid-exit cleanup removes Retold-owned assist targets. | Same/no/different-raid GameTest passes; natural raid support remains unverified. |
| Stage 3 raid gate | `RetoldRaidProgression`, `BadOmenMobEffectMixin`, and `RaidsMixin` block new raids before Stage 3 without cancelling existing raids. | Live Stage 2 creation path is covered; natural Bad Omen and Stage 3 raid flow need in-game verification. |
| Territory contract | Tests cover bastion, fortress, outpost, and mansion tags; all configured warning members; player modes; target suppression; escalation; target ownership; and immediate retaliation. | Synthetic coverage passes. Generated-structure behavior and presentation remain unverified. |
| Sniffer survival removal | Warm-ocean-ruin archaeology keeps ordinary rewards but removes the Sniffer Egg. Commands, Creative, existing Sniffers, and AI remain available. | Loot-table sampling and command-created Sniffer GameTest pass. |
| Endermite survival removal | Normal survival spawning is filtered, and the direct Ender Pearl creation path has its own narrow mixin guard. Existing, loaded, command, and Creative Endermites remain available. | GameTest passes; developer verified the behavior in-game on 2026-08-02. |

### Hunger, Food, Movement, And Barriers

| Change | Implementation | Validation status |
| --- | --- | --- |
| Dropped-food priority | `RetoldFoodBehaviorEvents` can interrupt an ordinary food hunt, clear prey/chase state, and claim higher-priority food movement without erasing retaliation or territory attacks. | GameTests cover Wolf, Slime, target ownership, and retaliation boundaries. |
| Active food search | Hungry managed ground mobs use bounded reachable search destinations instead of waiting motionless for food. Failed navigation is not cached as success. | Ground-forager and Bat search tests pass. The developer reports that the ordered natural movement audit works across aquatic and non-Bat flying mobs as part of the broader pathfinding pass. |
| Animal Feeder | A one-slot five-plank wooden trough persists an exact compatible stack. Right-click inserts one compatible item, sneak-right-click with compatible food inserts as much as fits, and sneak-right-click with an empty hand or incompatible item retrieves the stack. Hungry managed non-monster land animals search it through a cached, LOD-aware, budgeted local scan, claim ordinary `FOOD`/`FEED` movement to a supported adjacent cell, and consume one item using the existing diet and hunger relief. Aquatic mobs, Villagers, hostile monsters, Slimes, and Magma Cubes are excluded; live combat remains higher priority and feeding works with `mobGriefing=false`. The exact final approach uses reach range zero. Insertion copies only into storage and removes exactly the accepted count from a Survival player's actual held stack; Creative keeps its source stack. | Five isolated feeder-environment GameTests cover the controls, Survival conservation, Creative non-consumption, and retrieval. The updated direct interaction test passes 1/1; grouped reruns passed its assertions but hit the separately tracked Sheep-route timeout, which passed 1/1 alone. The developer confirmed exact Sheep routing and contention, then reported duplication; the Survival conservation and revised controls need natural re-verification. Model/collision, break drops, save/reload, broader diets/exclusions, and server compatibility remain unverified. |
| Shared feeding pose | Every completed Retold feed remembers the source position, stops all ordinary and Cube Mob movement for 40 ticks, and turns body/head/look control toward dropped food, forage, a feeder, held food, flowers, bamboo, prey, or specialized forage. Its `FOOD`/`FEED` priority remains below urgent danger, defense, combat, faction pressure, and territory work. | Focused feeding 6/6, food 4/4, and Bat 1/1 checks pass. On 2026-08-04, the developer confirmed the visible Sheep pose for trough, dropped-wheat, and forage feeding plus immediate damage interruption. Other representative mob families remain naturally unverified. |
| Predator disengagement | Satisfied predators release ordinary hunts. A satisfied Wolf leader transfers an active hunt/search to a hungry member where possible. | Two focused Wolf tests pass; natural mid-chase feeding and large packs need observation. |
| Weak-barrier breaking | Desperate ground/amphibious animals take 120 ticks; actively hunting wild predators take 60. The four-block search is cached and budgeted. Cracks, normal drops, a 200-tick cooldown, tags, and `mobGriefing` are enforced. | GameTests pass and the developer verified basic behavior in-game on 2026-08-02. Crowded pens remain a stress-test risk. |
| Cube Mob movement | `RetoldCubeMobMovement` translates Retold destinations into the vanilla Cube Mob facing-and-hop controller; a narrow mixin prevents random direction goals from overwriting owned movement. | A distant-item locomotion regression test passes; the developer reports that the natural large-Cube movement audit works. |
| Suitable pathfinding | Ground mobs use vanilla navigation; Bats use bounded `FlyingPathNavigation`; Cube Mobs use their specialized hop controller. Retold movement owners retain priority and release semantics. | Representative tests pass. On 2026-08-03, the developer reported that the ordered natural audit works for aquatic mobs, non-Bat flying mobs, large Cube Mobs, crowded groups, vertical terrain, barriers, and ownership release. Multiplayer, dedicated-server, profiler, long-session, existing-world, and exhaustive automated per-species coverage remain separate. |

### Slimes And Magma Cubes

Implemented through `RetoldSlimeHungerCombat`, `RetoldCubeMobContactDamage`,
`RetoldSlimeItemStorage`, `RetoldSlimeMergeBehavior`, `RetoldSlimeSplitBehavior`, and
`RetoldSlimeStarvationBehavior`:

- combat target assignment, target retention, assistance, and contact damage require hunt hunger
- fed Cube Mobs abandon ordinary combat but still seek all dropped items
- hungry size-one Cube Mobs can damage valid contacted targets
- swallowed items retain complete stacks and components and are restored without duplication on death
- food grows Cube Mobs one size at a time through size 10
- growth costs double from 16 items for size 2 to 4,096 for size 10, or 8,176 total
- natural same-species merging remains size 1 to 2 to 4 and has a 600-tick cooldown
- merge survivors retain swallowed contents and proportional health/hunger state
- hunger gain is `ceil(size / 2)` for sizes 1 through 10
- every ordinary death-split child inherits half of the parent's current hunger
- critical hunger splits size-two-or-larger Cube Mobs into two half-size children at 50 hunger
- size-one critical hunger ends in a normal death because no further split is possible
- starvation splitting preserves swallowed storage once and applies the merge cooldown

Five integrated Cube Mob GameTests cover merging, cooldown, both species, hunger-gated damage,
size-one contact, arbitrary swallowed items and components, exponential growth, size 10, death drops,
all hunger rates, death inheritance, starvation splitting, storage, and terminal death. Natural pursuit,
large-size combat balance, long-lived storage, and multiplayer visuals remain unverified. The maximum-size
Slime boss is only a recorded future concept.

### Spiders

- Spider and Cave Spider food hunts require hunger and nighttime.
- Adult passive livestock are valid food prey; players are not food prey.
- Daylight releases Retold-owned food hunts, while retaliation remains immediate.
- Cached swarm recruitment can share a valid hunt across both Spider types.
- Recent feeding or a successful hunt can establish a persisted `SPIDER_LAIR`.
- Lairs require darkness, raw skylight below 8 at builder and placement, supported air, and
  `mobGriefing`; ordinary open-sky night is insufficient.
- Up to six members share a lair, overlapping homes are separated, and construction/repair places
  one persistent cobweb every 600 ticks up to 50.
- Daylight return is low priority and yields to a live target; night releases it.

Ecology and lair tests cover the combat clock, adult Cow prey, cooperation, site requirements,
construction, sharing, cap, repair, griefing, return, retaliation, and release. Natural climbing, web
navigation, site choice, and long-duration pacing remain unverified.

### Bats

The Bat implementation was revised repeatedly in response to in-game screenshots and behavior:

1. Replaced exact persistent hanging points with broad colony identity.
2. Restored vanilla-style supported hanging requirements instead of allowing air/wall destinations.
3. Moved home repair upward so large caves find ceilings rather than retaining ground anchors.
4. Added bounded three-dimensional pathfinding for food, search, hunting, return, dodge, and panic.
5. Added night-only hungry parties capped at five with a shared direction lasting about 20 seconds,
   loose lanes, shared arthropod detection, and delayed direction changes.
6. Made arthropod counterattacks produce individual dodges rather than colony-wide retreat.
7. Added selective, distance-weighted, delayed panic for unrelated danger.
8. Added daylight return ownership, a collision-safe ceiling approach, independent 8-to-40 tick
   settling delays, and a ten-second post-disturbance panic window before resettling.
9. Reserved in-flight, settling, and occupied ceiling cells and made duplicate sleepers reroute.
10. Reduced hot-path work with upward column ordering, early exit, stable search results, route reuse,
    separation caching, shared party decisions, and rate-limited recruitment.

The focused `retold:*bat*` run on 2026-08-03 now passes all seven matching tests. This includes the
50-Bat matrix case and the 64-Bat, 200-tick day/night workload, which measured 10.658 ms/tick against
the 50 ms/tick limit. Natural caves, the developer's cave, multiplayer, and dedicated play need
observation.

The two Bat test-isolation blockers are resolved without weakening their gameplay requirements.
An immediate pre-fix complete rerun passed 140/140, confirming that the reported 137/139 failure
was intermittent, but code inspection still confirmed both unsafe test boundaries.
Player synchronization now skips mock `ServerPlayer` instances without an active client connection,
and a dedicated regression invokes the login handler with a clientless GameTest player. The
broken-roost coverage now retries across five shared block-search/path-start budget windows, but it
still requires a different dark supported ceiling, `SHELTER` ownership, and a real flying path before
movement. The post-ecology complete suite passes 150/150.

### Axolotls, Polar Bears, Endermen, And Villagers

| Subject | Implemented behavior | Validation status |
| --- | --- | --- |
| Axolotl | Ordinary Guardian prey is rejected in vanilla, brain, and Retold target paths. A Guardian hit creates retaliation and can recruit nearby witnessing Axolotls. Defensive bites do not feed them. | Integrated combat test passes; natural aquatic movement remains unverified. |
| Polar Bear | Cub defense begins with a 40-tick warning and no retained target. Withdrawal cancels it; attacking the adult or cub bypasses the delay. | Warning, withdrawal, escalation, and immediate-defense tests pass. |
| Enderman | A victim retaliates in every stage. Only Stage 3 recruits idle Endermen within 32 blocks. Existing targets remain, and the Dragon is not proactively targeted. | Stage-boundary GameTest passes; natural attacker variants need observation. |
| Villager | `RetoldVillagerTradeRefresh` refreshes professional-adult stock once per Overworld day. `VILLAGER_COMMUNAL`, `RetoldVillagerCommunalFood`, and its cached search let hungry loaded Villagers use every accessible chest/barrel within their HOME, MEETING_POINT, JOB_SITE, or live village context, regardless of whether it was generated or placed. They consume their highest-value personal food first; only an empty supply starts a route, after which they withdraw up to 12 food points, eat one item, and retain the rest. Adult Farmers retain vanilla harvesting/replanting/Bread production and use `RetoldVillagerCommunalSupply` to deposit surplus food while keeping 24 food points. Machines are excluded; hunger, danger, sleep, and trading win. Hunger and inventory changes save normally, but unloaded elapsed time is not simulated. | Trade-refresh and seven communal-food GameTests pass through three isolated selectors. Personal-first eating, exact batch conservation/serialization, chest/barrel support, machine rejection, village bounds, consumer/supplier danger priority, Farmer reserves, and real routes in both directions are covered. The latest focused 50-Villager TPS rerun, after livestock tending was added, peaks at 6.864 ms/tick. The developer verified the multi-Farmer/multi-consumer communal loop in-game before personal stock was added; the new carry/restock cadence still needs natural verification. Multiplayer, dedicated server, and existing-world POIs remain unverified. |
| Village storage ownership | `RetoldVillageContainerOwnershipData` persists exact quantities from unopened generated village loot and future Villager-controlled Farmer, food, trade, and golem-currency transactions. Player deposits remain unowned even when matching stacks merge, and leave first. Witnessed Survival theft uses vanilla negative gossip; breaking protected storage is severe enough for vanilla golem hostility. Creative/Spectator are excluded and ambiguous already-opened existing-world stores are not retroactively claimed. `/retold village status` reports the executing player's bounded nearby village standing and hostility risk. | The focused ownership selector passes 4/4 and covers generated loot, persistence, mixed quantities, actual menu withdrawal, breaking, gossip, Creative exclusion, village-context filtering, aggregation, and the golem-hostility threshold. The developer reported that the provenance, reputation, and status command work in-game on 2026-08-04. Save/reload, double chests, hoppers, multiplayer, dedicated server, and existing-world behavior remain unverified. |
| Village crop ownership | `RetoldVillageCropOwnershipData` persists the positions of crops actually planted/replanted by vanilla Farmers. Growth retains provenance; Farmer removal and player planting clear it. Witnessed mature harvest is minor theft, while immature breaking and farmland trampling apply a stronger `-50` vandalism penalty through the shared gossip owner. Creative/Spectator are excluded and ambiguous existing crops are not backfilled. | The focused crop selector passes 4/4, including the real vanilla `HarvestFarmland` planting path, SavedData round-trip, growth retention, safe player crops, offense strengths, trampling, witnesses, and Creative exclusion. The developer reported the natural crop/reputation behavior works on 2026-08-04; save/reload, unusual farms, multiplayer, dedicated server, and existing worlds remain unverified. |
| Village livestock ownership and tending | Loaded Shepherds tend Sheep/Goats, Leatherworkers tend Cows/Mooshrooms, and Butchers tend Pigs/Chickens/Rabbits. They use bounded shared scans and low-priority ownership, retrieve exactly two suitable items from village chest/barrel storage, approach a valid adult pair, consume the items, and relieve both animals' hunger. Successfully fed adults and offspring of two owned parents persist as village livestock. Player interaction protects previously unowned animals, and automatic offspring inherit player protection from an unowned player-associated parent. Witnessed direct Survival player killing applies `-50`; monsters, environment, Creative, and Spectator are excluded. | `retold:village_animal_reputation_*` passes 4/4 for roles, exact conservation, hunger relief without love mode, entity save/load, player protections, both inheritance paths, gossip strength, and death-source exclusions. The focused 50-Villager TPS rerun passes all five phases with a 6.864 ms/tick peak. Natural storage-to-pen movement, breeding cadence, transported animals, witness/trade/golem consequences, multiplayer, dedicated server, and existing worlds remain unverified. |
| Hunger-satisfaction breeding | All 26 current vanilla breedable entity types are selected through `retold:automatic_breeders`. Feeding by players, Villagers, dropped food, forage, or feeders relieves hunger but does not directly trigger love. Adults must remain at `FULL` hunger for 6,000 loaded ticks without panic, damage, or an active target. Compatible equally ready partners within eight blocks then use vanilla mating/birth behavior; a failed search retries after one minute. Birth adds 40 hunger to both parents and retains vanilla's age cooldown. Accumulated loaded satisfaction/retry/armed state persists per entity, with no unloaded-time advancement. | `retold:animal_breeding_*` passes 4/4 for complete tag/profile coverage, actual one-item player feeding, hungry-mate rejection, continuous readiness, unloaded-gap persistence, real Cow offspring, parent cost, retry state, Horse/Donkey compatibility, and interruption. `retold:mob_tps_cow`, `strider`, and `nautilus` pass all 15 phases below 50 ms/tick; after the final persistence correction, Cow peaks at 5.849 ms/tick and the 256-animal budget test passes at 17.082 ms/tick. The developer confirmed automatic breeding in-game on 2026-08-04 with a temporary 10-second gate, after which the five-minute production value was restored. Natural every-species, the complete five-minute wait, dense populations, multiplayer, dedicated-server, and existing-world checks remain unverified. |

### Herd And School Ecology

The first approved post-validation implementation batch added the confirmed loaded-world grouping
core without inventing fish diets or enclosure mechanics:

- Cows and Mooshrooms now use one `bovine` social identity when sharing persisted herd ranges.
  Horses/Donkeys/Mules and Llamas/Trader Llamas retain their mixed confirmed groups; other ordinary
  land herds remain species-specific.
- Cod, Salmon, Tropical Fish, and Pufferfish now have data-driven `AQUATIC_SCHOOL` profiles. An
  isolated member uses cached exact-species scans, low-priority `AQUATIC_SCHOOL`/`REGROUP`
  ownership, and ordinary aquatic navigation toward the nearby school center.
- Squid and Glow Squid now have `LOOSE_AQUATIC_GROUP` profiles. Successful damage performs one
  bounded cached panic broadcast to the exact same Squid species, while the existing receiver-side
  flee scan remains available as fallback. The two Squid species do not join each other's group.
- `RetoldHerdSchoolGameTests` covers confirmed land compatibility and a shared persisted bovine
  range, exact-species fish exclusion plus a reachable aquatic path, and Squid/Glow Squid panic
  isolation. The focused `retold:herd_school_*` batch passes 3/3.

The profiles deliberately leave hunger disabled. Exact fish diet assignments, seagrass/kelp
consumption, Squid hunger, wild migration completion, bucket-release behavior, and the
player-defined domesticated enclosure/range mechanism remain unimplemented or naturally unverified.

## Architecture And Performance Work

Behavior stays in the central ownership architecture rather than separate per-feature tick subscribers:

- `RetoldBehaviorEntityTickDispatcher` routes profile and supported special-mob behavior.
- `RetoldAiControl` owns purpose and prevents incompatible goals from fighting.
- `RetoldCombatTargets`, target memory, and source-aware guards own Retold targets.
- `RetoldMobProfiles` uses an atomically replaced direct entity-type index after reload.
- `RetoldAiTickContext` avoids per-mob profile-context allocation in the hot path.
- `RetoldAiLod` refreshes mutable entries rather than allocating replacements.
- Entity, sight, and block-search caches plus work budgets bound expensive queries.
- Entity-scan hits avoid constructing unused search bounds.
- Explicit-center block searches remain reusable while a mob travels.
- `/retoldbehavior perf` reports actual block-target positions examined.

### Performance Tests And Findings

| Test | Workload | Recorded result |
| --- | --- | --- |
| Mixed-profile stress | 256 always-ticking animals across eight profiles for 200 ticks. | Earlier baseline: 14.475 ms/tick; 24,757/25,081 entity-scan cache hits; 57 scan-budget skips; all budget assertions passed. |
| Bat colony stress | 64 Bats across 100 daylight and 100 nighttime ticks. | Post-fix focused run: 10.658 ms/tick; the complete suite measured 9.742 ms/tick. Both pass 50 ms/tick. |
| Per-mob TPS matrix | 74 species tests, 50 subjects each, five 80-tick phases after warmup. Shared passive-flee profiles receive one point of real damage in the danger phase, and the six added aquatic profiles exercise school or loose-danger behavior. | Final ecology rerun: all 74 tests and 370 phases passed in 1.396 minutes. Skeleton idle/rest was highest at 7.516 ms/tick; the six new profiles peaked at 2.905 ms/tick for Cod danger/social. Earlier 68-profile baselines remain recorded separately. |
| Animal Feeder performance rerun | The same 74-species matrix exercises cached negative feeder lookup for eligible land animals; positive routing is covered by the focused feeder fixture. | All 74 tests and 370 phases passed in 1.232 minutes. Bat habitat/day-night was highest at 6.815 ms/tick; every phase remained below 50 ms/tick. |
| Shared feeding pose rerun | Applicable dropped-food/forage phases exercise the constant-time 40-tick pose; focused tests cover exact orientation and zero movement. | All 74 tests and 370 phases passed in 1.746 minutes. Bat danger/social was highest at 9.692 ms/tick; every phase remained below 50 ms/tick. |
| Villager communal-food rerun | The matrix adds 50 Villagers with meeting-point memories and four Bread barrels so their positive loaded-store search, cache, ownership, and paths execute. | All 75 tests and 375 phases passed in 1.390 minutes. Every phase remained below 50 ms/tick; Villager danger/social was its local peak at 3.319 ms/tick. |
| Villager golem-construction rerun | The focused 50-Villager fixture exercises the repeated dispatcher and bounded construction eligibility/site work introduced by staged golems. | All five Villager phases passed below 50 ms/tick; the peak was 8.542 ms/tick. The complete TPS matrix was not repeated because only the Villager hot path changed. |

The matrix found a Sniffer performance bug. A 37x9x37 range scan validated every candidate with
another 11x5x11 nearby-diggable scan, producing active-phase averages as high as 216.917 ms/tick.
New range anchors now require the candidate itself to be diggable, while existing stored memories
retain separate nearby validation. The clean rerun reduced the Sniffer peak to 3.340 ms/tick.

Absolute timing depends on host, JVM warmup, and concurrent tests. Relative rankings and work-counter
changes are the more useful regression signal.

## Automated Test Inventory

| Area | Main owner and coverage |
| --- | --- |
| Global rules | `RetoldGameTests`: profiles, Creepers, factions, Enderman defense, danger flight, griefing, food priority, Cube movement, and raids. `RetoldDamageFleeGameTests` covers successful damage from entity, player, environmental, and aquatic cases. |
| Territory | `RetoldTerritoryGameTests`: tags, membership, player modes, warnings, escalation, ownership, and retaliation. |
| Food/starvation/barriers/Wolves | `RetoldStarvationGameTests`, the 40-species plus coverage-guard `RetoldHungerSurvivalGameTests`, `RetoldAnimalFeederGameTests`, `RetoldFoodSearchGameTests`, `RetoldWeakBarrierGameTests`, and `RetoldWolfPackHungerGameTests`. |
| Slimes/Magma Cubes | `RetoldSlimeMergeGameTests`: merging, combat, storage, growth, starvation, and splitting. |
| Spiders | `RetoldSpiderEcologyGameTests` and `RetoldSpiderLairGameTests`. |
| Bats | `RetoldBatColonyGameTests`: roosts, ceilings, unique slots, detours, parties, panic, settling, and TPS. |
| Herds/schools | `RetoldHerdSchoolGameTests`: shared land groups/ranges, exact-species fish paths, and exact-species Squid panic. |
| Species/factions | Axolotl, Polar Bear, and Witch support test owners plus Enderman coverage in `RetoldGameTests`. |
| Availability/damage/trades | Sniffer/Endermite availability, Snowball, Vex, and Villager refresh test owners. |
| Villager golems | `RetoldVillagerGolemConstructionGameTests`: exact Cleric/Librarian/Armorer/Toolsmith/Weaponsmith builder whitelist with Nitwit/NONE/Farmer rejection, vanilla five-Villager eligibility, no instant spawn, staged construction, emerald trade/storage conservation, final non-player ownership, construction-scoped Hired Help suppression with ordinary animation restored afterward, five-level Survival charge, Creative exemption, Snow Golem non-regression, and invalid/obstructed no-charge placement. |
| Village storage/reputation | `RetoldVillageContainerOwnershipGameTests`: generated village loot, SavedData persistence, Farmer output, matching player deposits, menu-click removal, protected-container breaking, witness gossip, and Creative exclusion. |
| Village crops/reputation | `RetoldVillageCropReputationGameTests`: real vanilla Farmer planting hook, position SavedData, growth retention, player planting, mature harvest, immature destruction, trampling, witnesses, and Creative exclusion. |
| Performance | Mixed 256-mob stress, 75-species TPS matrix, Bat TPS, and sight-cache regression coverage. |

## Test Selection Policy

On 2026-08-04, the developer directed that validation must be selected by affected risk instead of
automatically running every long suite. The maintained policy is
[`testing_strategy.md`](testing_strategy.md): start with the narrowest relevant JUnit or `retold:`
GameTest selector and stop when it covers the changed contract. The complete GameTest suite and the
complete per-mob TPS matrix each require a separate shared-system, isolation, baseline,
release/milestone, or explicit-developer reason. Documentation-only changes do not run Gradle, and
`./gradlew build` is run once before a code handoff rather than after every edit.

## Validation Record

| Check | Result |
| --- | --- |
| `./gradlew build` | Passed on 2026-08-04 after the Panda bamboo-consumption change, including compilation, JUnit tests, PMD, and assembly. |
| Clientless-player synchronization regression | 1/1 passed on 2026-08-03. |
| Focused herd/school command | 3/3 passed on 2026-08-03. |
| Focused feeding commands | After the one-item/bulk and Creative non-consumption changes, the direct interaction test passed 1/1 on 2026-08-04. The latest `retold:animal_feeder_*` rerun passed 3/4; the Sheep-route case also timed out 1/1 in isolation. A controlled diagnostic repeated the same timeout with the new breeding dispatch disabled, so this failure is not caused by hunger-satisfaction breeding. All control, Survival-conservation, Creative-non-consumption, persistence, eligibility/diet, combat-priority, source-facing, and urgent-interruption assertions passed. Earlier focused runs and the developer's natural enclosed-Sheep check passed the exact supported adjacent route. |
| Focused Villager communal-food commands | Consumer transactions passed 4/4, the exact consumer route passed 1/1, and Farmer supply passed 2/2 on 2026-08-04. Coverage includes personal-first selection, 12-point batch restocking, exact conservation/serialization, the 24-point Farmer reserve, seeds/unrelated inventory, machine and non-Farmer rejection, danger priority, and physical arrival in both directions. The three selectors remain separated because their synthetic route fixtures can contend when grouped. |
| Focused Villager golem command | `retold:golem_*` passed 5/5 on 2026-08-04. Coverage includes the exact five-profession builder whitelist and Nitwit/NONE/Farmer rejection, vanilla five-Villager agreement, no instant spawn, timed placement, emerald retention/conservation, final golem identity, narrowly scoped Hired Help suppression with ordinary animation restored, five-level Survival cost, Creative exemption, Snow Golem non-regression, and invalid/obstructed no-charge placement. |
| Focused village-container ownership commands | `retold:village_container_ownership_*` passed 4/4 on 2026-08-04 after adding `/retold village status`. The related Farmer supply selector passed 2/2, communal consumer selector passed 4/4, and exact retained-trade emerald regression passed 1/1. Coverage includes generated loot unpacking, persisted ownership, mixed player/village quantities, menu transactions, protected breaking, vanilla gossip, Creative exclusion, ownership updates through each current Villager storage path, status-query village filtering/aggregation, and the golem-hostility threshold. No TPS or complete-suite run was selected because the change adds one-off loot/transfer/menu/break/command work rather than repeated tick work, and the focused runtime boundaries are covered. |
| Focused village-crop reputation command | `retold:village_crop_reputation_*` passed 4/4 on 2026-08-04. Coverage includes the real vanilla Farmer planting behavior through `HarvestFarmlandMixin`, persisted ownership, natural growth, player placement clearing, mature harvest theft, immature crop vandalism, farmland trampling, witness gossip, and Creative exclusion. No TPS or complete-suite run was selected because the hook adds constant-time state comparison only when vanilla Farmer work runs and changes no scan, navigation, or cadence. |
| Focused village-animal reputation command | `retold:village_animal_reputation_*` passed 4/4 on 2026-08-04. Coverage includes all profession/species roles, exact two-item storage conservation, hunger relief without immediate love mode, persisted ownership across entity save/load, player handling, village-owned and player-associated offspring inheritance, direct Survival `-50` gossip, and monster/environment/Creative exclusions. |
| Focused animal-breeding command | `retold:animal_breeding_*` passed 4/4 on 2026-08-04. Coverage includes every tagged current vanilla breeder and positive hunger profile, real final-item player feeding, no direct love mode, five-minute continuous satisfaction, hungry-mate rejection, actual Cow birth, 40-hunger parent cost, retry/save-load state, an unloaded-time gap, Horse/Donkey compatibility, and damage/danger interruption. |
| Focused starvation commands | `retold:starvation_*` passed 2/2 on 2026-08-04, asserting that every loaded positive-hunger profile reaches the ordinary `PathfinderMob`, separate Bat, or Villager communal-food owner and covering first critical damage through all three, terminal death, and a disabled-hunger exclusion. The exact Cube Mob critical split/death regression passed 1/1. Cow, Bat, and Villager TPS passed all 15 phases below 50 ms/tick, with peaks of 6.852, 11.856, and 10.129 ms/tick. The complete GameTest suite and TPS matrix were intentionally not run because the focused checks cover all shared-hunger entry paths without changing dispatch, scans, paths, caches, or persistence. |
| Hunger-survival matrix | `retold:hunger_survival_*` passed 41/41 in two final different-order runs on 2026-08-05 after auditing all 40 positive-hunger profiles against patched 26.2 spawn biomes and replacing artificial dropped-item ecology with representative production sources. Coverage now includes safe desert scrub (cactus remains a hazard), badlands grub substrate, alpine forage, Mushroom-Field mycelium, cave prey/insects, Nether mushrooms/lava/prey, caravan fodder, aquatic habitats, ordinary forage/flowers/crops, living prey, and Villager storage. The grouped matrix exposed and drove a bounded fairness fix for deterministic block-search starvation; the global cap remains eight starts per tick, and a deferred claimant holds position rather than wandering away from its nearby source. Bat, Camel, Rabbit, Slime, and Magma Cube TPS passed all 25 phases below 50 ms/tick, peaking at 8.276, 10.061, 4.292, 5.243, and 5.305 ms/tick respectively. The complete GameTest suite and full TPS matrix were intentionally not run because the dedicated matrix covers every active hunger profile and the five focused TPS tests cover the changed/shared hot paths. |
| Focused Panda bamboo commands | `retold:panda_bamboo_*` passed 2/2 on 2026-08-04 after separating its natural and gamerule fixtures into isolated environments. Coverage requires the Panda to approach a bamboo block, remove it without drops, and lower hunger only after successful removal; `mobGriefing=false` must preserve both the block and hunger. The existing `retold:hunger_survival_panda` case passed 1/1. `retold:mob_tps_panda` passed all five 50-Panda phases below 50 ms/tick, peaking at 9.305 ms/tick. The complete GameTest suite and full TPS matrix were intentionally not run for this species-local transaction. |
| Focused natural-food acquisition commands | `retold:natural_food_*` passed 5/5 on 2026-08-04 for Piglin-on-Hoglin, five hungry-undead, both Cube Mob kill meals, undead-family/Cube-family/Creeper exclusions, and non-consuming Strider lava sustenance. The exact `hunger_survival_armadillo`, `hunger_survival_nautilus`, and `hunger_survival_strider` cases pass using grub soil, living Cod, and lava rather than dropped substitutes. Their latest 50-mob TPS checks pass all 15 affected phases below 50 ms/tick, peaking at 5.760, 4.544, and 6.771 ms/tick. The complete GameTest suite, complete survival matrix, and complete TPS matrix were intentionally not run: focused acquisition and exact survival checks cover the changed contracts, and only the Strider species path changed in the latest increment. |
| Focused Villager torch commands | `retold:villager_relight*` passed 3/3 and `retold:extinguished_torches_drop_matching_lit_items` passed 1/1 on 2026-08-04 after the magic/Nitwit split. After the developer reported that the fake Flint and Steel only appeared for one tick, the 3/3 selector passed again with a strengthened regression that clears the displayed stack mid-action, requires it to recover, and requires at least ten visible ticks. Coverage includes Stage 1/2/3 behavior, village context, the eight-block radius, hunger/danger priority, exact wall-facing restoration, Nitwit close-range fake-tool use, zero inventory/tool consumption, and the existing six extinguished-variant item contract. The longer Nitwit route and final visual presentation remain natural in-game checks. |
| Per-mob TPS command | The last complete matrix passed 75/75 in 1.390 minutes and 375/375 phases below 50 ms/tick before Farmer supply. Registration is now 77/385 after adding Strider and Nautilus. Per the maintained selection policy, the breeding change ran Cow, Strider, and Nautilus only; all 15 phases passed below 50 ms/tick, peaking at 6.419 ms/tick. The complete expanded matrix was not rerun. |
| Loaded mixed-mob budget command | After the final accumulated-loaded-time persistence correction, `retold:loaded_mob_ai_work_remains_bounded` passed with all 256 managed animals alive, all shared budget/cache assertions satisfied, and 17.082 ms average server-tick wall time. |
| Focused Bat command | 8/8 matching tests passed on 2026-08-03 after stale fixture paths were cleared; the 64-Bat workload measured 8.131 ms/tick in that run. |
| Focused Axolotl command | 1/1 passed in its dedicated test environment after replacing an exact-tick damage check with a wait bounded by the existing timeout. |
| Complete GameTest command | The latest complete run passed 160/160 in 3.504 minutes on 2026-08-04 before the two Farmer-supply, five golem, and three Villager-relighting tests were added. It was not rerun after these local features because no complete-suite escalation condition applied; the narrow owning selectors passed instead. |
| Developer in-game checks | The developer reported on 2026-08-03 that the ordered natural acceptance passes work, including Bat/passive-flee, herd/school ecology, and the subsequent movement audit. On 2026-08-04, the developer also confirmed the exact enclosed-Sheep feeder route, two-second source-facing pose for trough/dropped/forage food, immediate damage interruption, and several hungry Sheep sharing one trough. These are in-game developer reports, not dedicated-server, multiplayer, profiler, long-session, existing-world, or exhaustive automated per-species verification. |

Previously used commands are retained below for reproducibility, not as a list to run after every
change. Select only the commands justified by [`testing_strategy.md`](testing_strategy.md).

```bash
./gradlew build
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:villager_communal_food_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:villager_paths_to_communal_food_storage"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:farmer_communal_supply_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:golem_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:villager_relight*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:village_animal_reputation_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:starvation_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:hunger_survival_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:natural_food_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:cube_mob_size_scales_hunger_and_starvation_splits"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:extinguished_torches_drop_matching_lit_items"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:mob_tps_villager"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:*bat*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:herd_school_*"
# Complete TPS matrix: only when performance escalation is justified.
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:mob_tps_*"
# Complete GameTest suite: only when integration escalation is justified.
./gradlew runGameTestServer
```

## Known Limitations And Next Work

- Exercise natural mixed-hostile populations, multiplayer, dedicated servers, long sessions, realistic
  terrain, and crowded pens while monitoring `/retoldbehavior perf`.
- Verify generated territory structures, natural Stage 3 raids, and Witch support.
- Verify natural Cube Mob pursuit, large-size balance, storage volume, and visuals.
- Keep the size-10 Slime boss undecided until its trigger, identity, rewards, storage behavior, and
  Magma Cube participation are designed.
- Complete the remaining Animal Feeder acceptance work: interaction details, break drops,
  persistence, broader diets/exclusions, visual compatibility, and server scenarios. Exact Sheep
  arrival, source-facing feeding, damage interruption, and same-species contention are confirmed.
- Naturally verify Stage 2+ Villager golem construction: confirm Clerics, Librarians, Armorers,
  Toolsmiths, and Weaponsmiths can build while Nitwits and other professions cannot; also check
  vanilla eligibility, visible placement, emerald sourcing, interruption/cleanup, save/reload,
  `mobGriefing=false`, repeated builds under vanilla's recent-golem detection, and the player
  Survival/Creative cost boundary.
- Naturally recheck Villager torch maintenance after the sustained-tool fix: non-Nitwits should relight all
  three torch families and a wall torch through a visible short ranged cast in every stage. A
  Nitwit should instead follow a real route to a supported close-use cell, face the torch, and hold
  Flint and Steel throughout the full one-second interaction without possessing or damaging one
  before relighting it. Continuing exposed rain,
  hunger, danger, sleep, trading, and `mobGriefing=false` should block both methods. Check several
  Villagers and torches near a chunk border without treating this as multiplayer or dedicated-
  server proof.
- Implement hiring, remaining creature families, and unloaded ecology only as separate approved
  work.

## Current Position And Recommended Execution Order

The mob system is now a broad, performance-tested foundation rather than a finished implementation
of the complete design contract. The first herd/school core, loaded Animal Feeder, shared feeding
pose, loaded Villager communal consumer/Farmer-supplier chest/barrel loop, Stage 2+ staged Iron
Golem construction, profession livestock tending/property reputation, and all-stage magical/
Nitwit-physical Villager torch maintenance, global loaded-world hunger-satisfaction breeding, and
loaded starvation, one-meal automated spawn-habitat survival coverage for all 40 active hunger
profiles, and natural acquisition owners for desert/badlands/alpine/Mushroom-Field/cave/Nether,
aquatic, caravan, village, forage, and predator contexts are implemented. The golem feature keeps vanilla's five-agreeing-Villager and
local recent-golem checks rather than adding a numeric cap or daily cooldown. The latest complete
complete baseline remains 75 isolated per-mob tests and 375 measured phases passing; registration
is now 77/385, with Cow, Strider, and Nautilus focused phases passing below 50 ms/tick. Four focused
breeding, four village-animal, five golem, three relighting, five natural-acquisition, and 41
hunger-survival tests pass. The latest focused Bat, Camel, Rabbit, Slime, and Magma Cube performance
checks keep all 25 measured phases below 50 ms/tick. The
developer confirms the natural Animal Feeder controls, model, persistence, routes, feeding pose,
interruption, and representative eligibility behavior. Villager communal food and golem
construction remain naturally unverified and deliberately perform no unloaded catch-up.

Work should continue in this order:

1. Naturally verify loaded starvation with representative livestock, a Bat, a Villager, a hostile
   hunger profile, and a named/tamed animal. Confirm the species-specific interval, one-point
   damage, eventual death, normal drops, and immediate cessation after feeding below 100. Keep the
   Cube Mob split/death behavior separate and do not claim unloaded simulation.
2. Naturally verify global hunger-satisfaction breeding with representative ordinary, aquatic,
   Nether, egg-laying, pregnant, mixed-equine, and tamed animals. Confirm five continuous loaded
   minutes at full satisfaction, interruption resets, automatic vanilla birth, the parent hunger
   cost/cooldown, population cadence, and save/reload without claiming unloaded-time simulation.
3. Naturally verify profession livestock tending in a normal loaded village. Put two eligible
   adults and suitable food storage within village context, confirm the assigned profession walks
   storage-to-pen, removes exactly two items, relieves both animals' hunger, and eventually produces
   persistent inherited ownership through global satisfaction breeding. Confirm player-handled
   animals and their protected lineage stay safe and witnessed
   Survival killing changes trade reputation while monsters, environment, and Creative do not.
4. Naturally verify the new all-stage torch maintenance in a normal village after rain stops.
   Confirm the one-second source-facing magic for ordinary professions and the Nitwit's real
   close-range route plus fake Flint and Steel animation. Cover normal/soul/copper and wall
   variants, the eight-block boundary, continuing-rain refusal, priority interruptions,
   `mobGriefing=false`, and chunk reload.
5. Naturally verify Stage 2+ golem construction in a normal village: five agreeing Villagers, an
   eligible Cleric/Librarian/Armorer/Toolsmith/Weaponsmith builder, and an emerald held by a
   Villager or stored in a village chest/barrel should produce a visible,
   interruptible, save-safe build. Confirm vanilla's recent-golem detection permits later builds,
   `mobGriefing=false` prevents construction, Survival requires and spends exactly five levels,
   Creative is free, Snow/Copper behavior is unchanged, and a nearby player does not receive
   “Hired Help” from the Villager-built golem.
6. Naturally verify the new personal-stock cadence: an empty Villager should take three Bread or
   twelve vegetables, eat one, retain the rest across later meals and save/reload, and avoid another
   storage trip until personally empty. The developer already reports that the preceding
   multi-Farmer/multi-consumer communal loop works.
7. Keep the formerly failing Cube Mob hop and fish/Bat contention assertions under observation in
   relevant focused runs without weakening their behavior requirements. Revisit complete runs only
   when the test-selection escalation rules apply.
8. Then choose the next small approved loaded-world batch from hiring, remaining special
   creatures, unfinished stage rules, or the still-
   unspecified portions of herd/school ecology.
9. Implement unloaded ecosystem catch-up last, after loaded behavior is stable. It must remain
   bounded, queued, persistent, and prohibited from breaking barriers offline.

The worktree remains intentionally dirty with the accumulated implementation. Before committing,
review every modified and untracked file, retain unrelated developer work, and exclude accidental
editor backup files such as `enderman.png~` and `enderman_eyes.png~`.

The copy-paste continuation brief is maintained in
[`mob_ai_handoff_prompt.md`](mob_ai_handoff_prompt.md).

## Primary Implementation Map

| Area | Main owners |
| --- | --- |
| Dispatcher and movement | `RetoldBehaviorEntityTickDispatcher`, `RetoldBehaviorMovement`, `RetoldCubeMobMovement` |
| Control and targets | `RetoldAiControl`, `RetoldAiControlOwner`, `RetoldCombatTargets`, `RetoldMobTargetPolicy`, `RetoldInvalidTargetEvents` |
| Food and barriers | `RetoldFoodBehaviorEvents`, `RetoldStarvationBehavior`, `RetoldFeedingPose`, `RetoldAnimalFeederBehavior`, `RetoldAnimalFeederSearch`, `AnimalFeederBlockEntity`, `RetoldWeakBarrierBehavior`, `RetoldWeakBarriers`, `RetoldMobGriefing` |
| Profiles, state, and breeding | `RetoldMobProfiles`, `RetoldMobRules`, `RetoldMobStates`, `RetoldAnimalBreeding`, `AnimalBreedingMixin`, `data/retold/mob_profiles`, `retold:automatic_breeders` |
| Factions and territory | `RetoldFactionMembers`, `RetoldFactionRelations`, `RetoldFactionAssistEvents`, territory controllers and tests |
| Species | `behavior/species`, especially `RetoldAquaticSchoolEvents`, the herd/school tests, and the Bat, Slime, Spider, Axolotl, and Polar Bear owners |
| Stages and availability | `RetoldRaidProgression`, raid mixins, `RetoldMobAvailability`, `ThrownEnderpearlMixin` |
| Villagers | `RetoldVillagerTradeRefresh`, `RetoldVillagerCommunalFood`, `RetoldVillagerCommunalSupply`, `RetoldVillagerAnimalTending`, `RetoldVillageAnimalOwnership`, property-reputation owners, `RetoldVillagerGolemConstruction`, `RetoldVillagerTorchRelighting`, `RetoldGolemAnimation`, and the three golem/advancement mixins |
| Performance | `RetoldAiLod`, `RetoldAiScanCache`, `RetoldAiTickContext`, `RetoldAiWorkBudget`, `RetoldBehaviorPerf`, `RetoldBlockTargetSearch` |
| Data | 77 mob profiles including fish/Squid ecology, Villager communal food, Strider, and Nautilus; automatic-breeder and weak-barrier tags; warm-ocean-ruin loot-table override |
