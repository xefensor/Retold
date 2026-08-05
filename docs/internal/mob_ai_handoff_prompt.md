# Mob AI Continuation Prompt

Copy the text below into a new AI coding conversation.

---

Continue the Minecraft Retold mob, faction, ecology, pathfinding, and performance work in:

`/home/xela/Projekty/Minecraft Retold/retold-26.2`

Start by reading `AGENTS.md` completely, then read:

- `docs/internal/mob_ai_work_report_2026-08-03.md`
- `docs/internal/retold_mob_ai_system.md`
- `docs/internal/design_implementation_status.md`
- `docs/internal/retold_issues.md`
- `docs/internal/retold_design_risks.md`
- `docs/internal/retold_roadmap.md`
- `docs/internal/mob_tps_benchmark.md`
- `docs/internal/testing_strategy.md`

Preserve the existing dirty worktree. It contains the accumulated implementation and unrelated
developer work. Do not reset, discard, overwrite, or broadly reformat existing changes. Inspect the
current diff before editing. Do not include the untracked editor backup files `enderman.png~` and
`enderman_eyes.png~` in a commit.

Current position:

- The design clarification pass was completed through questions numbered to 184 and is recorded in
  the confirmed gameplay contract.
- The central mob AI architecture, control ownership, source-aware targets, factions, territory,
  hunger, homes, searches, pathfinding adapters, caches, LOD, work budgets, and debugging exist.
- There are 77 data-driven mob profiles. Strider and Nautilus are the newest profiles, added so
  every current vanilla breedable animal has positive hunger progression.
- Implemented work includes Creeper safety/awareness, passive damage fleeing, dropped-food priority,
  active food search, predator disengagement, Wolf leadership transfer, weak barriers, complete
  current Slime/Magma Cube behavior, Spider hunting/lairs, Bat colonies, defensive Axolotls, Polar
  Bear warnings, Stage 3 Enderman assistance, Witch raid support, Stage 3 raid gating, territory
  coverage, Snowball/Vex damage, Sniffer/Endermite survival removal, Villager stock refresh, shared
  bovine/equine/llama range identities, exact-species fish schooling, exact-species Squid panic,
  the loaded-world Animal Feeder, the loaded Villager communal consumer/Farmer-supplier loop, and
  global loaded-world hunger-satisfaction breeding for all current vanilla breedable animals,
  loaded starvation damage and a passing 40-species natural-feeding survival matrix for every
  active hunger profile, plus
  Stage 2+ staged Villager Iron Golem construction restricted to Clerics, Librarians, Armorers,
  Toolsmiths, and Weaponsmiths, plus all-stage ranged magical Villager torch maintenance with a
  close-range fake-Flint-and-Steel path for Nitwits.
  Every completed Retold feed now stops the mob for two seconds and
  turns it toward the remembered food position, with urgent ownership able to interrupt.
- `animal_feeder` is a one-slot wooden trough crafted from five planks. Right-click with compatible
  food inserts one item; sneak-right-click with compatible food inserts as much of the held stack
  as fits; sneak-right-click with an empty hand or incompatible item retrieves it. Hungry managed non-monster land animals use
  their existing diets and hunger relief, cached/budgeted local discovery, ordinary `FOOD`/`FEED`
  ownership, and an exact adjacent supported cell. A developer Sheep test found that ordinary
  navigation could accept the preceding node; the feeder final approach now requests reach range
  zero, path caching includes that precision, and the regression drives a real Sheep to consumption
  without teleporting. Aquatic mobs, Villagers, hostile monsters,
  Slimes, and Magma Cubes ignore it. It works with `mobGriefing=false` and remains separate from
  village storage.
- Hungry loaded Villagers search every accessible chest and barrel within 16 blocks and within 32
  blocks of their HOME, MEETING_POINT, JOB_SITE, or live village context, regardless of whether the
  container was generated or placed. They prefer
  higher vanilla Villager food points and path to a supported side. They consume their highest-value
  carried food first. Only an empty personal supply triggers storage; there they take up to 12 food
  points, eat one item, retain the remainder, and use the shared source-facing pose. Machine
  inventories are ignored; danger, sleep, and trading retain priority. Hunger and exact entity/
  container inventories save normally. There is deliberately no unloaded-time simulation or
  catch-up in this slice.
- Adult Farmers retain vanilla crop harvesting, replanting, and wheat-to-Bread production. When
  their inventory exceeds a 24-food-point personal reserve, they path to the same village chests or
  barrels and deposit surplus Bread, Carrots, Potatoes, or Beetroot. Seeds and unrelated items stay
  with the Farmer, exact accepted counts are conserved, machines and non-Farmers are rejected, and
  hunger/danger/sleep/trading retain priority. This also remains loaded-world only; normal Villager
  and container saving persists the result.
- Village storage now carries persisted provenance without changing ItemStacks. Unopened generated
  `chests/village/*` loot and future Farmer/Villager-controlled deposits are village-owned. Player
  additions remain unowned even when they merge into a matching stack, and are removed first.
  Nearby village-context Villagers who see a Survival player take protected contents add vanilla
  negative gossip; breaking protected storage is severe enough for vanilla Iron Golem hostility.
  Creative/Spectator players are ignored, and ambiguous already-opened existing-world stores are
  not retroactively protected. Operators can run `/retold village status` to see the executing
  player's standing across loaded village-context Villagers within 32 blocks, including average,
  worst/best, negative count, and possible golem hostility.
- Crops actually planted or replanted by vanilla Farmers now carry persistent position ownership
  through growth. Player planting clears it. A witnessed mature harvest is minor theft; breaking an
  immature owned crop or trampling its farmland is stronger `-50` vandalism. Creative/Spectator
  players are ignored, existing ambiguous crops are not backfilled, and the result appears in
  `/retold village status`.
- Shepherds tend Sheep and Goats, Leatherworkers tend Cows and Mooshrooms, and Butchers tend Pigs,
  Chickens, and Rabbits. A tender retrieves exactly two matching food items from village storage,
  paths to a valid pair, relieves both animals' hunger, and marks the adults village-owned.
  Offspring of two owned parents inherit village ownership; automatic offspring of an unowned
  player-associated parent inherit player protection. A witnessed direct Survival-player kill of owned livestock applies `-50`
  reputation, while Creative/Spectator and non-player deaths are ignored. The focused four-test
  selector passes; natural, multiplayer, dedicated-server, and existing-world behavior remains
  unverified.
- All 26 current vanilla breedable entity types use `retold:automatic_breeders`. Feeding by a
  player, Villager, dropped item, forage, or feeder only relieves hunger. A living adult must remain
  in the `FULL` hunger stage for 6,000 loaded ticks without panic, damage, or a live target; two
  compatible equally ready adults within eight blocks then enter vanilla mating automatically.
  A failed mate search retries after one minute. Vanilla retains movement, genetics, tame ownership,
  Horse/Donkey compatibility, Turtle eggs, Frog pregnancy, and special births. Successful parents
  gain 40 hunger and retain vanilla's age cooldown. Satisfaction/retry/armed state saves with each
  entity, but unloaded time does not advance it.
- Every mob profile with a positive hunger interval takes one point of starvation damage whenever
  its species-specific interval reaches or remains at 100 hunger. The rule can kill ordinary,
  hostile, named/tamed, Villager, and Bat mobs; feeding below 100 stops later pulses. Slimes and
  Magma Cubes keep their separate critical split-or-die response. This is loaded behavior only.
- At Stage 2+, vanilla's exact wanting, five-agreeing-Villager, and local recent-golem checks still
  decide whether a golem may be created. Only a Cleric, Librarian, Armorer, Toolsmith, or
  Weaponsmith may convert an eligible instant spawn into a persisted visible build; Nitwits and
  all other professions cannot construct. The builder paths to a supported site, places four
  magical iron blocks and a normal pumpkin in timed steps, spends one emerald from a Villager
  inventory or accessible village chest/barrel, then invokes vanilla animation. Emerald trades
  retain one physical emerald when inventory or communal storage permits it. Retold adds no
  numeric cap or daily cooldown.
  Construction requires `mobGriefing` and yields to danger, hunger, sleep, trading, targets, and
  higher-priority activity. A successful player-built Iron Golem costs five levels in Survival;
  Creative, invalid/obstructed frames, Snow Golems, and Copper Golems are free. Villager-built
  golems do not award nearby players the player-summoning “Hired Help” advancement.
- In every stage, an adult Villager with remembered or live village context can restore one dry
  weather-extinguished torch within eight horizontal/five vertical blocks and within 32 blocks of
  its village anchor. Most professions use a one-second ranged magical cast. Nitwits instead route
  to a supported adjacent cell and hold a temporary Flint and Steel throughout the full close-use
  interaction; active use reasserts the visual if vanilla clears it, but it never enters inventory
  or consumes durability. Both preserve normal, soul, copper, floor, wall, and wall-
  facing state, require `mobGriefing`, and yield to hunger, danger, targets, sleep, trading,
  incompatible activity, and higher ownership.
  Discovery uses the weather-owned loaded-chunk extinguished index, the shared block-search budget,
  and LOD-scaled cooldowns; there is no unloaded maintenance.
- `./gradlew build` passes.
- Hungry Pandas now consume the exact bamboo block they reach without a drop and receive hunger
  relief only after successful removal. The transaction obeys `mobGriefing`; the isolated Panda
  selector passes 2/2, its hunger-survival case passes 1/1, and all five Panda TPS phases pass below
  50 ms/tick with a 9.305 ms/tick peak.
- Every positive-hunger profile now has an intended loaded food-acquisition route. Armadillos use
  bounded cached searches to dig exposed soil for grubs without changing the block, wild hungry
  Nautiluses hunt living fish, and lava passively sustains Striders without being consumed; Warped
  Fungus remains their fallback food. Piglins, hungry undead,
  Slimes, and Magma Cubes receive family-safe meal credit from appropriate living victims they
  kill; Creepers never count. `retold:natural_food_*` passes 5/5, the exact Armadillo/Nautilus/
  Strider survival cases pass, and their latest affected TPS peaks are 5.760/4.544/6.771 ms/tick.
- The latest complete TPS baseline passes all 75 then-registered per-mob tests and all 375 phases below
  50 ms/tick. Registration is now 77 tests/385 phases. Cow, Strider, and Nautilus focused runs pass
  all 15 phases below 50 ms/tick, and the final 256-managed-animal budget test passes at 17.082 ms/tick;
  the complete expanded matrix was intentionally not rerun. After Farmer supply, only the affected `retold:mob_tps_villager` test was rerun per the
  selection policy; the latest affected-only rerun after the sustained Nitwit tool-visual fix
  passed all five phases and peaked at 7.102 ms/tick while
  recording personal meals, batch restocking, and bounded supplier searches and paths.
- Villager consumer transactions pass 4/4, the exact consumer route passes 1/1, and Farmer supply
  passes 2/2 through three isolated selectors. Together they cover exact one-item consumption,
  personal-first selection, 12-point restocking, entity/container serialization, chest/barrel
  support, machine/non-Farmer rejection, village bounds, danger priority, reserve/conservation, and
  real consumer and supplier paths. Do not group
  the separate route fixtures through `retold:*communal_food*`; synthetic fixture contention can
  time out the otherwise exact focused route.
- The focused `retold:village_container_ownership_*` selector passes 4/4. Related Farmer supply
  passes 2/2, communal consumption passes 4/4, and the exact retained-trade emerald regression
  passes 1/1. Together they cover generated village loot, ownership SavedData, mixed matching
  player/village quantities, actual menu withdrawal, protected breaking, vanilla gossip, Creative
  exclusion, each current Villager storage transaction, and the status command's village-context
  filtering, aggregation, and golem-hostility threshold. No TPS or complete-suite run was
  selected because this is one-off loot/transfer/menu/break work and its focused runtime boundaries
  are covered.
- The focused `retold:village_crop_reputation_*` selector passes 4/4. It covers the real vanilla
  Farmer planting path, crop SavedData, growth retention, safe player planting, mature harvest,
  immature breaking, farmland trampling, witnesses, Creative exclusion, and exact gossip strength.
  No TPS or complete-suite run was selected because this adds only constant-time work to actual
  vanilla Farmer crop changes and player block events.
- The focused `retold:village_animal_reputation_*` selector passes 4/4. It covers every confirmed
  tender role, exact two-item conservation, hunger relief without immediate love mode, entity
  save/load, player handling, both ownership/protection inheritance paths, witnessed direct-player `-50` gossip, and
  monster/environment/Creative exclusions. The focused 50-Villager TPS rerun passes all five
  phases with a 6.864 ms/tick peak. No complete-suite or complete-matrix escalation applied.
- The focused `retold:animal_breeding_*` selector passes 4/4. It covers all tagged current vanilla
  breeders and positive hunger profiles, actual player feeding, continuous full satisfaction,
  hungry-mate rejection, real Cow offspring, parent hunger cost, retry/save-load state,
  Horse/Donkey compatibility, and interruption.
- The focused `retold:starvation_*` selector passes 2/2 across the ordinary, Bat, and Villager
  hunger owners, and the exact Cube Mob critical-hunger regression passes 1/1. Cow, Bat, and
  Villager TPS checks pass all 15 phases below 50 ms/tick, peaking at 6.852, 11.856, and
  10.129 ms/tick. The complete GameTest suite and TPS matrix were not rerun because the focused
  checks cover every hunger owner without changing scans, paths, caches, or persistence.
- The focused `retold:golem_*` selection passes 5/5. It covers the exact Cleric/Librarian/Armorer/
  Toolsmith/Weaponsmith builder whitelist with Nitwit/NONE/Farmer rejection, vanilla five-Villager
  eligibility, no instant spawn, staging, emerald conservation, final ownership, Survival/Creative
  XP costs, Snow Golem non-regression, invalid/obstructed no-charge placement, and the narrow
  Villager-animation advancement-suppression scope.
- The focused `retold:villager_relight*` selection passes 3/3, and the directly related existing
  extinguished-torch drop regression passes 1/1. Coverage includes all three stages, village and
  eight-block bounds, hunger/danger priority, exact wall-facing restoration, and sustained Nitwit
  close-use fake Flint and Steel visibility without inventory mutation. Naturally verify the longer Nitwit route because
  vanilla paths at random multi-million-block GameTest coordinates were nondeterministic.
- The latest complete GameTest suite passed 160/160 in 3.504 minutes before the two Farmer-supply,
  five golem, and three Villager-relighting tests were added. It was intentionally not rerun after these local features because no
  complete-suite escalation condition applied.
- The focused `retold:*feed*` selection passes 6/6 and `retold:*food*` passes 4/4. The shared pose
  covers dropped food, forage, feeder use, held food, flowers, bamboo, prey feeding, Bats, Sniffers,
  and Cube Mob swallowing; its focused regression includes a non-pathfinding Bat and proves urgent
  replacement ownership interrupts the active pose without being cleared.
- The focused `retold:*bat*` selection passes 8/8, including the 64-Bat workload at 8.131 ms/tick in
  the latest run.
- The focused `retold:herd_school_*` selection passes 3/3.
- The focused Axolotl defense test passes 1/1 in its dedicated environment.
- Player insertion copies into storage; Survival loses exactly the accepted count while Creative
  keeps its held stack. The developer confirmed the final one-item/bulk/retrieval controls and the
  wider Animal Feeder acceptance pass in-game.
- The former Bat isolation blockers are resolved. Player synchronization skips clientless GameTest
  players, with dedicated regression coverage. Broken-roost coverage retries across five shared AI
  budget windows while still requiring a different valid dark supported ceiling, shelter ownership,
  and a real flying path.
- The developer reported that the previously ordered natural Bat, passive-flee, and broader
  acceptance pass works. Treat that as an in-game developer report only; multiplayer,
  dedicated-server, profiler, and existing-world verification remain separate and unconfirmed.
- The developer also reported that the ordered herd/school natural acceptance pass works, including
  mixed land groups, exact-species fish cohesion, mixed-fish panic, exact-species Squid panic,
  bucketed fish, crowding, vertical obstacles, and route recovery. Multiplayer, dedicated-server,
  long-session, and existing-world verification remain unconfirmed.
- The developer subsequently reported that the ordered natural movement audit works for aquatic
  mobs, non-Bat flying mobs, large Cube Mobs, crowded groups, vertical terrain, barriers, and
  ownership release. This remains an in-game report, not multiplayer, dedicated-server, profiler,
  long-session, existing-world, or exhaustive automated per-species verification.
- On 2026-08-04, the developer confirmed automatic hunger-satisfaction breeding in-game using a
  temporary 10-second readiness gate. The production constant was then restored to the confirmed
  five loaded minutes; the complete five-minute wait and every-species coverage remain unverified.
- On 2026-08-04, the developer re-tested the reported enclosed-Sheep setup and confirmed exact
  feeder arrival and consumption, the visible two-second source-facing pose for feeder/dropped/
  forage food, immediate damage interruption, and several hungry Sheep using one trough.
- On 2026-08-04, the developer reported that village-container provenance, witnessed reputation,
  and `/retold village status` work in-game. This does not establish multiplayer, dedicated-server,
  long-session, or existing-world behavior.
- On 2026-08-04, the developer also reported that Farmer-crop provenance/reputation works in-game.
  Profession livestock tending, village-animal reputation, and global hunger-satisfaction breeding
  are implemented but have not yet received their natural acceptance passes.
- The new herd/school batch deliberately does not implement exact fish diets, seagrass/kelp
  consumption, Squid hunger, or a player-defined domesticated enclosure mechanism.

Immediate task: naturally verify loaded starvation with representative livestock, a Bat, a
Villager, a hostile hunger profile, and a named/tamed animal. Confirm one damage point per
species-specific metabolism interval at 100 hunger, eventual death and normal drops, and that
feeding below 100 stops later damage. Separately confirm Slimes/Magma Cubes still split or die under
their existing critical rule. Do not report unloaded starvation because catch-up remains absent.

Then naturally verify profession livestock tending in a normal loaded village. Give a
Shepherd two Sheep/Goats and Wheat, a Leatherworker two Cows/Mooshrooms and Wheat, and a Butcher
the corresponding Pig/Chicken/Rabbit pairs with Carrots or Wheat Seeds in accessible village
chests/barrels. Confirm the Villager walks storage-to-pen, removes exactly two matching items,
faces/reaches the pair, relieves both animals' hunger, and eventually produces offspring through
global satisfaction breeding. Save/reload and confirm tended adults plus their offspring remain
protected. Confirm player-handled animals and their protected lineage
remain unowned, a witnessed direct Survival kill causes the strong `-50` reputation change, and
monster/environmental/Creative deaths do not. Record natural, multiplayer, dedicated-server, and
existing-world results separately.

After that, naturally verify Villager torch maintenance in a normal village in every stage.
Let rain extinguish exposed normal, soul, copper, and wall torches, then confirm ordinary
professions wait until precipitation no longer reaches them, stop, face each source, and use the
short ranged magical cast. Confirm a Nitwit instead follows a real supported route, stops close,
faces the source, and holds a fake Flint and Steel for the full one-second interaction without
gaining or damaging one. Both should
relight only torches within the eight-block/village bounds. Confirm hunger, danger, sleep, trading,
and `mobGriefing=false` interrupt or prevent the action, then unload/reload the chunks and repeat.
Record this only as an in-game developer result; multiplayer, dedicated-server, long-session, and
existing-world checks remain separate.

After that, naturally verify Stage 2+ golem construction in a normal village. With five agreeing
Villagers, a Cleric/Librarian/Armorer/Toolsmith/Weaponsmith builder, and one emerald held by a
Villager or stored in an accessible village chest/barrel, confirm the visible timed build,
interruption cleanup, save/reload continuity, and
eventual non-player-created golem. Confirm `mobGriefing=false` blocks construction, vanilla's local
recent-golem detection governs later builds, Survival players need and spend exactly five levels,
Creative is free, Snow/Copper behavior is unchanged, and nearby players do not receive “Hired
Help” for the Villager-built golem. Record this as an in-game developer result
only; multiplayer, dedicated-server, long-session, and existing-world checks remain separate.

Test-selection rule: follow `docs/internal/testing_strategy.md`. Run the narrowest selector for the
changed subsystem, and run `./gradlew build` once before a code handoff. Do not automatically run
the complete GameTest suite or complete TPS matrix. Each broad run needs its own shared-system,
isolation, baseline, milestone, or explicit-developer reason. For the current natural acceptance
task, no automated rerun is needed unless code changes.

Focused examples and explicitly gated broad commands:

```bash
./gradlew build
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:animal_feeder_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:herd_school_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:*bat*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:villager_communal_food_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:villager_paths_to_communal_food_storage"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:farmer_communal_supply_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:village_container_ownership_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:village_crop_reputation_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:village_animal_reputation_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:animal_breeding_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:hunger_survival_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:natural_food_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:golem_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:villager_relight*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:extinguished_torches_drop_matching_lit_items"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:mob_tps_villager"
# Complete TPS matrix: only when the performance escalation rules apply.
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:mob_tps_*"
# Complete GameTest suite: only when the integration escalation rules apply.
./gradlew runGameTestServer
```

Continue in this order:

1. Naturally verify the new acquisition routes with a hungry Armadillo on exposed eligible soil,
   a wild hungry Nautilus with live fish, a hungry Strider on lava and another away from lava near
   Warped Fungus, and representative Piglin/undead/Cube kills. Confirm soil and lava remain intact
   and family/Creeper deaths do not feed.
2. Complete the natural hunger-satisfaction breeding acceptance pass and record the result honestly.
3. Complete the natural profession-livestock tending/reputation acceptance pass and record the
   result honestly.
4. Complete any remaining natural all-stage Villager torch-maintenance checks.
5. Complete any remaining natural Villager golem-construction and personal-food cadence checks.
6. Keep the formerly failing Cube hop, aquatic route, and separated Bat-route assertions under
   observation in relevant focused runs without weakening their requirements. Use a complete run
   only when the test-selection escalation rules apply.
7. Then choose a new implementation batch from Piglin/brute hiring, remaining
   special creatures, stage rules, or the still-unspecified parts of
   herd/school ecology.
8. Leave unloaded ecosystem simulation until loaded behavior is stable. It must be bounded, queued,
   persistent, population-aware, and must never break barriers while unloaded.

Important design boundaries:

- Do not infer that the complete design contract is implemented merely because a profile exists.
- No mob deliberately targets or melees Creepers, although projectiles such as Snowballs can damage
  them when they hit.
- Hunger never overrides retaliation, alliances, ownership, territory duty, or urgent danger.
- Every deliberate Retold destination must use suitable movement: vanilla ground/aquatic navigation,
  bounded three-dimensional Bat navigation, or the Cube Mob facing-and-hop controller.
- Keep server gameplay authoritative and route new ordinary AI through the central dispatcher,
  ownership helpers, caches, LOD, and work budgets.
- Do not implement undecided rules without asking, including the Nether Remnant armor-piece count,
  unloaded catch-up cap/granularity, or the size-10 Slime boss. Golem costs are decided at one
  Villager emerald and five player levels.
- Report automated, focused in-game, multiplayer, dedicated-server, and existing-world verification
  separately and honestly.

Lead with the current diagnosis, make focused changes, and keep the documentation synchronized with
what is actually implemented and verified.

---
