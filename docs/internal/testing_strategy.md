# Retold Test Selection Strategy

This document defines how to choose validation for a change. The goal is to catch the regressions a
change can plausibly cause without repeatedly running unrelated, long suites.

## Core Rule

Before running tests, identify:

1. the files and owning subsystem changed
2. the behavior or contract that could regress
3. the one exact automated test ID that exercises that behavior
4. whether another specific behavior contract changed and needs its own exact test

Run one exact GameTest ID at a time. Do not use species, subsystem, or wildcard selectors merely for
convenience: a Sheep feeder-path change runs the exact Sheep feeder-path test, not every feeder test
or every Sheep test. If that exact test passes, stop unless another specific changed contract needs
its own exact test. Do not run the complete GameTest suite or complete per-mob TPS matrix.

`GameTestServer` executes its test ticks unthrottled, equivalent to tick sprint for this workflow.
The JVM, registry, server startup, and world shutdown time remains and is not reduced by Minecraft's
`/tick sprint` command. Always use the dedicated GameTest server rather than waiting for real-time
ticks in a client world.

Record the exact commands and results.

## Validation By Change

| Change | Required validation | Do not run by default |
| --- | --- | --- |
| Documentation only | `git diff --check`; inspect changed links, headings, and claims against the current code | Gradle, GameTests, TPS tests |
| Java compile/style or isolated refactor with no behavior change | `./gradlew build` once before handoff | GameTests and TPS tests |
| Pure logic or state transition | Focused JUnit test with `./gradlew test --tests '<class-or-pattern>'`; `./gradlew build` once before handoff | GameTests unless Minecraft integration is involved |
| Local gameplay behavior | One exact matching GameTest ID; `./gradlew build` once before handoff | Species/subsystem wildcards and complete GameTest suite |
| Species-specific mob behavior | One exact behavior GameTest; add that species' exact TPS test only when tick cadence, scans, navigation, allocations, or loaded-group cost changed | Species wildcards, complete TPS matrix, complete GameTest suite |
| Shared mob behavior used by several known species | One exact behavior test per genuinely changed species contract; run each affected species' exact TPS ID separately when hot-path cost changed | Family wildcards and complete suites |
| Resources, recipes, loot, tags, or profiles | Focused loader/registration test and the affected behavior test when one exists; `./gradlew build` once before handoff | Unrelated gameplay suites |
| Worldgen, dimension, networking, persistence, or visuals | Focused automated coverage plus only the relevant manual environments described in `AGENTS.md` | Unrelated mob and TPS suites |

For visual changes, agents run applicable build and technical startup checks but leave subjective
in-game inspection to the developer. Do not drive the game or desktop UI to produce screenshots or
claim visual approval. Record the exact technical checks performed and hand off the remaining
visual scenarios to the developer.

`./gradlew build` runs compilation, JUnit tests, PMD, and assembly; it does not run NeoForge
GameTests. It is the normal final code-quality check, not something to repeat after every edit.

## Focused GameTests

For Earth Guardian pursuit with real maze geometry and simultaneous terrain combat, use
`retold:earth_guardian_pursues_into_generated_maze`. It embeds the production-generated lower
maze in host stone, moves a mock Survival source along a walkable generated route, dispatches
real `STEP` events, and runs forty-tick encounter repair with griefing and environmental attacks
enabled. It requires the guardian to move beyond the central arena toward the player after
observing a real lava eruption on the trail. No direct vibration injection, protected attack pad,
guardian teleport assistance, or fire immunity is used. Failure diagnostics include the blocking state.
On 2026-09-08 this failed twice before the fluid fix at 2,400 ticks on an unchanged lava source,
then passed after whole-state fluid removal; the final strengthened run completed at guardian
tick 239, approximately 19.1 blocks from center, with `lavaSeen=true`.
The exact `retold:earth_guardian_relocates_fluids_without_stalling` selector also passed, covering
source water/lava, flowing lava, waterlogged slabs, empty-source duplicate prevention,
exact-state spending, protection denial, and disabled griefing.
Both selectors were run individually with
`bash ./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:<id>"`.
This is real Minecraft server gameplay with a mock event source, not a connected-client,
subjective visual, natural multi-seed, upper-floor, existing-world, or multiplayer playtest.
No TPS rerun is required for this local mutation fix: cadence, scans, navigation requests, and
budgets are unchanged, and the existing griefing-disabled benchmark would not exercise it.
The pass also completed `bash ./gradlew build` (compilation, JUnit, PMD, assembly) and
`git diff --check` successfully.

Use the exact `retold:` test ID for the changed contract. For example:

```bash
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:villager_paths_to_communal_food_storage"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:villagers_share_persistent_knowledge_of_village_storage"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:farmer_communal_supply_paths_to_storage"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:golem_construction_stages_and_conserves_village_emerald"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:villager_relights_extinguished_torches_in_every_stage"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:extinguished_torches_drop_matching_lit_items"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:anvil_teaches_only_successfully_transferred_book_enchantments"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:enchantment_catalog_payload_round_trips"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:enchantment_catalog_covers_registry"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:enchantment_tooltips_hide_unknown_names"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:deterministic_enchanting_casts_are_atomic"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:enchanting_menu_casts_update_synchronized_slots"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:enchanting_cast_payload_round_trips"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:known_enchanting_options_follow_inserted_item"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:villager_teaching_transactions_are_atomic"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:villager_teaching_preview_payload_round_trips"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:animal_feeder_uses_paths_without_mob_griefing"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:starvation_kills_and_ignores_non_hunger_mobs"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:hunger_survival_panda"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:panda_bamboo_naturally_eats_and_breaks_block"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:panda_replaces_unreachable_bamboo_target"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:bats_find_high_ceiling_and_search_in_five_member_parties"
```

Generic push, pull-request, and release workflows run `./gradlew build` but do not choose GameTests
automatically because the correct exact test depends on the changed behavior. The developer or
implementer records the exact GameTest command and result for the change before handoff.

For Dragon Egg ritual changes, select the exact contract affected:
`retold:world_data_tracks_ritual_progress` covers distinct offering bits and the current hatch
threshold; `retold:dragon_egg_accepts_final_and_legacy_offerings` covers all six item
identities, Survival consumption, duplicate rejection, upgraded-world aliases, and actual hatching;
`retold:lodestone_is_exclusive_to_earth_guardian` covers real boss-death loot, recipe removal, and
both vanilla chest bypasses;
`retold:gale_core_drops_heavy_core` and `retold:elder_guardian_drops_heart_of_the_sea` cover the two
implemented guardian rewards; and `retold:buried_treasure_excludes_heart_of_the_sea` covers the
removed alternative acquisition route. Run only the exact changed contracts, then verify a real
Stage 2 egg interaction in-game when the hatch threshold, crack presentation, or Stage 3 transition
flow changes.

For Earth side-room changes, run `EarthLabyrinthRoomsTest` and the exact
`retold:earth_rooms_keep_loot_and_fire_across_chunk_border` integration test. Generation placement
changes also need `retold:earth_labyrinth_piece_places_connected_maze_geometry`; saved content
version changes need `retold:earth_labyrinth_pieces_round_trip_without_upgrading_old_pyramids`.
These cover deterministic side branches, container/no-refill rules, real redstone firing across a
chunk boundary, generated room placement, and old-start version retention. They do not establish
natural spawn rates, full multi-seed chunked worldgen, balance, or visual approval. Generation-only
content and death-time loot do not require a per-mob TPS rerun.

The 2026-09-07 room/reward pass ran each following ID individually with
`bash ./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:<id>"`;
all six exact tests passed:

- `lodestone_is_exclusive_to_earth_guardian`
- `earth_rooms_keep_loot_and_fire_across_chunk_border`
- `dragon_egg_accepts_final_and_legacy_offerings`
- `world_data_tracks_ritual_progress`
- `earth_labyrinth_pieces_round_trip_without_upgrading_old_pyramids`
- `earth_labyrinth_piece_places_connected_maze_geometry`

`bash ./gradlew test --tests "cz.xefensor.retold.worldgen.earth.EarthLabyrinthRoomsTest"` also passed
512 deterministic room-selection seeds. Natural multi-seed/chunk-border worlds, natural mob rates,
dedicated multiplayer, upgraded-world end-to-end completion, lava-exposed reward retrieval, and
subjective visuals were not verified by these automated fixtures.
The final `bash ./gradlew build` passed compilation, the full JUnit task, PMD, and assembly;
`git diff --check` passed. No client/desktop visual test or unrelated TPS matrix was run.

For Earth Guardian excavation/ramp changes, run each changed contract separately:
`retold:earth_guardian_batches_excavation_with_protection` verifies a full twelve-block slice,
the ten-tick pulse guard, Bedrock excavation, mid-pulse denial, and griefing denial.
`retold:earth_guardian_builds_conserved_trailing_walls` verifies the production wall trigger,
3×3 geometry, exact state consumption, the forty-tick cooldown, griefing, protected cells,
and occupied-cell rejection. Batch/backfill/wall changes also require the exact ascent and descent
selectors because walls must preserve the active stair workspace and support placement must
recover if movement drift occupies the next step. Run the moving-footstep tunnel selector for
pursuit integration and the single guardian TPS selector for cadence/search-cost changes.
The 2026-09-08 batch/wall policy selectors passed individually. The ascent selector exposed a
real occupied-support stall, then passed after actual-feet replanning. One initial descent run
timed out with a healthy entity already removed; two subsequent exact reruns passed without a
removal-behavior change. Temporary tracing in the second rerun showed only normal successful-test
cleanup and was removed afterward. The earlier removal cause remains unestablished; do not claim
an in-world death/despawn fix or fully established long-session traversal reliability from this pass.
The final moving-footstep tunnel selector also passed, as did the exact 50-guardian TPS selector
(7.281 ms/tick peak, 20 TPS in all five phases). Each selector was run separately with
`bash ./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:<id>"`.
No complete GameTest suite or unrelated TPS species was selected. The griefing-disabled TPS
fixture does not establish the cost of many simultaneous mutation bursts or wall physics.
The batch/wall pass also passed `bash ./gradlew build` (compilation, JUnit, PMD, assembly)
and `git diff --check`. No connected-client, multiplayer, natural-world, or subjective visual
verification was performed.

`retold:earth_guardian_follows_real_steps_into_tunnel` covers a moving mock Survival player using
actual `level.gameEvent` footstep dispatch, dynamic listener registration, vibration travel, and
full-sized pursuit/ceiling excavation through a three-block-high tunnel. It does not call the
guardian's vibration callback directly. The destination is protected from block mutation while
attack windups remain enabled. The exact selector failed before the moving-clue fix at 2,000 ticks
and passed afterward on 2026-09-07. This fixture is not a connected-client or natural maze test.
The exact `retold:mob_tps_earth_guardian` rerun also passed, peaking at 9.649 ms/tick with 50
subjects and 20 TPS in all five phases; no unrelated selectors or full matrix were run.
The moving-clue pass also passed `bash ./gradlew build` (compilation, JUnit, PMD, assembly)
and `git diff --check`.

For fixed-clue body clearance, elevation, and mutation policy, use:
`retold:earth_guardian_carves_low_tunnel`, `retold:earth_guardian_carves_ascending_ramp`,
`retold:earth_guardian_carves_descending_ramp`, and
`retold:earth_guardian_ramp_conserves_and_respects_protection`. The traversal fixtures use real
server entity ticks and a continuously refreshed synthetic vibration; ascent/descent cover the
full fourteen-block floor separation but protect an off-route attack pad to isolate locomotion.
The policy test checks independent break/place denial, a mid-construction griefing change, exact
support-state reuse, the 128-state cap/reload, and saved ramp heading. It does not prove a natural
mid-climb reload, real-player vibration dispatch, idle cross-floor patrol, or combined combat.
Run `retold:mob_tps_earth_guardian` for repeated search/navigation changes; that baseline still
disables griefing, so it does not profile fully occupied reserves, active excavation/backfill,
or long-session fluid spread. No unrelated species matrix is warranted.

The 2026-09-07 traversal pass runs those four exact IDs individually with the same
`bash ./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:<id>"`
command. The final ramp fixtures reserve enough entity-ticking chunk rings for their full footprint;
merely loaded chunks had left a healthy guardian unticked near the far landing in an earlier fixture.
Protected attack pads are intentional isolation, not evidence of guardian fire/lava immunity.
All four exact traversal/policy IDs passed, as did the exact 50-guardian TPS selector, with a
7.302 ms/tick peak and 20 TPS in all five phases. Natural combined combat, traversal pacing,
dedicated multiplayer, and subjective visual approval remain unverified.
The traversal pass also completed `bash ./gradlew build` (JUnit, PMD, compilation, assembly) and
`git diff --check` successfully.

For Earth Guardian source-repair changes, use the exact
`retold:earth_guardian_foundation_spawns_and_persists_lifecycle` test for initial placement,
stationary anchoring, uninterrupted awakening, repeated roaming repair, entity-state preservation,
reload rebinding, and defeated suppression. Use
`retold:earth_guardian_physically_investigates_vibrations` for real pursuit crossing a 40-tick
repair without moving the entity or replacing its path/owner. Both were run individually with
`bash ./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:<id>"`
on 2026-09-07 and passed after the center-teleport fix; the extended foundation test first failed
against the unconditional chamber placement. This fix changes neither scan cadence nor search
scope, so no TPS rerun is warranted. Natural dungeon pursuit, actual chunk unload/reload, and
connected multiplayer remain unverified; reload coverage directly restores entity state.
The source-repair pass also passed `bash ./gradlew build` (compilation, JUnit, PMD, assembly)
and `git diff --check`.

For Earth Guardian melee-plus-environmental combat, use the exact
`retold:earth_guardian_melee_respects_contact_and_cooldown` selector for heard-source, player-mode,
reach, fresh-wall, real-damage, cooldown/save-load, and dormant-state guards. Use
`retold:earth_guardian_pursues_during_hazards_and_melees` for real-tick approach during the 30-tick
warning, released ceiling debris, and automatic melee damage in the same encounter. The fixture
waits for physical navigation readiness after chunk preparation; test time is not entity time.
Its mock player has a synthetic packet listener with vanilla client-loading invulnerability cleared,
not a connected client. Changed pursuit speed/arrival and warning movement also warrant the exact
moving-footstep tunnel regression and the single `retold:mob_tps_earth_guardian` selector. These
do not establish natural combined balance, multiplayer, subjective strike presentation, or lava safety.
On 2026-09-08 the two combat selectors and the moving-footstep tunnel selector passed individually
using `bash ./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:<id>"`.
The exact 50-guardian TPS run also passed all five phases at 20 TPS, peaking at 13.915 ms/tick.
It disables griefing and does not measure fifty simultaneous close-player melee strikes.
The combat pass also passed `bash ./gradlew build` (compilation, JUnit, PMD, assembly) and
`git diff --check`. No game client, connected multiplayer, or subjective visual checks were run.

For the Fire path, `retold:wildfire_uses_independent_stage_two_spawner` covers the dedicated
Stage 2 Nether owner, ordinary-monster-cap independence, and normal placement boundary;
`retold:wildfire_targets_undead_and_drops_fire_artifact` covers boss-tier attributes,
Nether Remnant/Blaze alignment, owned Undead targeting, and the guaranteed core;
`retold:zombified_piglins_take_blaze_and_wildfire_fire_damage` covers the narrow fire-immunity
exception, both fireball owners, continued Stage 2 burning, and preserved attacker immunity;
`retold:wither_skeletons_take_remnant_fireball_damage` covers direct damage from both Remnant
attackers while preserving ordinary Wither Skeleton fire immunity;
`retold:wildfire_targets_and_damages_visible_ghast` covers the faction-scoped override of vanilla's
Mob-versus-Ghast rejection, acquisition and 40-tick retention at 56 blocks, the unchanged ordinary
40-block faction boundary, direct Small Fireball damage, and preserved ordinary Ghast fire
immunity; and the exact
`retold:natural_wildfire_spawns_with_blaze_escort`,
`retold:wildfire_encounter_targets_only_undead_and_players`,
`retold:wildfire_patrols_with_single_file_blaze_escort`,
`retold:wildfire_blaze_escorts_follow_into_combat`,
`retold:wildfire_rises_out_of_lava_when_idle`,
`retold:wildfire_repositions_during_ranged_combat`,
`retold:wildfire_submerges_heals_fully_and_resurfaces`,
`retold:wildfire_shields_gate_damage_and_shockwave_hostiles`, and
`retold:wildfire_requires_deep_lava_submersion_for_recovery`, and
`retold:wounded_wildfire_retreats_toward_lava` selectors cover the local spawn exclusion,
three-to-five escort count, persistent encounter-only target gating across ordinary acquisition,
retaliation, Undead, targetable-player, and ordinary-Blaze boundaries, obstacle-clearing ordered
formation movement for both leader and escort, patrol-to-combat handoff, whole-group combat
following across blocked terrain, shared-target reacquisition, idle lava buoyancy,
owned ranged-combat repositioning, lava-first recovery routing under an active target, combat
rejection, full submersion, complete health and shield restoration, and resurfacing,
independent shield gate/body transition, hostile-only shockwave, rejection of ordinary fire and
shallow or surface lava for both recovery paths, three-block lava-column selection, submerged
regeneration cadence, and owned shelter movement. Run
`retold:mob_tps_wildfire` whenever faction-target range/cadence, shield scanning, shockwave
range/cadence, lava-source search, or retreat, formation, or combat-movement cadence changes.
Verify the custom model, disappearing shields, texture mapping, shockwave feedback, and natural
single-file spacing/turning, healthy lava-surface height, wounded dive/recovery/resurfacing, and
combat orbiting in a normal client; GameTests do not establish visual correctness or Nether terrain
fit.

For faction classification changes, run
`retold:faction_tags_preserve_defaults_and_standard_undead` for the registry contract and
`retold:faction_tags_drive_targeting_and_retaliation` for live relationships, target ownership,
retaliation, and loaded-mob goal removal. Add the exact existing faction behavior affected, such as
the Silverfish/Endermite, Witch raid-alliance, or territory-member test. When changing tag extension
or conflict behavior, also run those two faction tests once with a temporary external datapack that
adds a member and exercises the relevant ambiguity rule. Add one factioned and one unfactioned mob
TPS selector when repeated classification or loaded-goal maintenance changes.

For recipe-discovery compatibility changes, run
`retold:recipe_visibility_uses_shared_knowledge_authority` and
`retold:unknown_recipe_types_fail_open_for_viewers`, then the exact existing crafting, cooking,
smithing, stonecutting, or Villager-teaching path changed. A concrete viewer adapter also requires
an in-client login/learning/reconnect pass with that viewer present; a server-only GameTest cannot
prove its dynamic UI refresh.

For generic world-protection changes, run
`retold:world_protection_rules_preserve_defaults_and_block_mutations` plus one focused existing test
for every routed owner changed. A concrete claim adapter requires dedicated-server and multiplayer
checks for allow/deny, claim boundaries, owner/projectile attribution, portals, delayed structures,
and Aender retry behavior. Do not run the complete worldgen or mob suite unless the shared layer or
mutation routing changed broadly enough to justify it.

The Villager consumer transaction, consumer route, and Farmer supply tests intentionally use
separate isolated GameTest environments. Run only the exact route or transaction test changed.
For shared village-storage knowledge, run
`retold:villagers_share_persistent_knowledge_of_village_storage`; it proves one Villager's scan is
shared under an exhausted world-search budget, persists exact food/emerald/arbitrary-item contents
and counts through SavedData, and updates after a physical withdrawal. Add the one exact food,
Farmer-deposit, livestock-feed, or golem-emerald route whose consumer contract changed. Run
`retold:mob_tps_villager` when the repeated lookup, candidate validation, or storage-discovery path
changes; event-time container refresh alone does not justify a TPS selector.

For village-container provenance or witnessed theft, select the one exact
`retold:village_container_ownership_...` test for the changed transaction. The available tests cover
generated village loot, persisted exact-stack ownership, Villager/Farmer additions, mixed player
and village quantities, menu withdrawal, container breaking, vanilla gossip, Creative exclusion,
and the bounded village-status summary. Add the directly affected communal food, Farmer supply, or
exact golem-currency test only when its transaction hook changes. These
operations run on loot unpack, inventory transfer, menu click, or block break rather than a tick
path, so they do not justify a TPS selector unless repeated AI discovery or transfer cadence also
changes.

For village crop ownership and reputation, select the exact relevant
`retold:village_crop_reputation_...` test. The tests drive the
real vanilla Farmer planting behavior through `HarvestFarmlandMixin` and covers persisted Farmer
provenance, player-placement clearing, mature harvest theft, immature crop vandalism, farmland
trampling, witnesses, and Creative exclusion. This adds event-time work plus one constant-time
SavedData update when vanilla Farmer work actually changes a crop; it does not justify a Villager
TPS rerun unless the Farmer behavior cadence or search path changes.

For profession livestock tending and village-animal reputation, select the exact relevant
`retold:village_animal_reputation_...` test. The tests cover the
Shepherd/Leatherworker/Butcher species split, exact two-item storage conservation, hunger relief
without immediate love mode, persisted entity ownership, player-handled protection, both
village-owned and player-associated offspring inheritance, witnessed `-50` gossip, and monster,
environmental, and Creative exclusions. Add `retold:mob_tps_villager` when the repeated tending
dispatcher, cached animal scan, storage search, control ownership, or navigation changes. Do not
run unrelated species or the complete suites for entity-persistent provenance or death-event-only
changes.

For the global hunger-satisfaction breeding contract, select the exact relevant
`retold:animal_breeding_...` test. The tests cover
currently tagged vanilla breeder types and their positive hunger profiles, real player feeding
without direct love mode, the five-minute full-satisfaction gate, hungry-mate rejection, actual
vanilla Cow offspring, 40-hunger parent cost, one-minute retry state, entity save/load,
Horse/Donkey compatibility, and readiness interruption. Add the exact relevant village-animal
reputation test only when Villager feeding or livestock provenance changes. Because the dispatcher
adds one constant-time 20-tick check to breedable animals and performs a cached scan only after five
satisfied minutes, use the 256-mob bounded-work test plus one representative ordinary breeder TPS
case; add affected new profile cases separately.

For loaded starvation, select the exact relevant `retold:starvation_...` test. The tests cover the
ordinary `PathfinderMob`, distinct Bat-colony, and Villager communal-food hunger owners,
first critical damage, terminal death, exclusion of profiles whose hunger interval is zero, and a
registry-wide assertion that every loaded positive-hunger profile reaches one of those owners. Add the exact
`retold:cube_mob_size_scales_hunger_and_starvation_splits` regression when the shared critical-
hunger dispatch changes, because Cube Mobs must split or die through their specialized rule rather
than receive generic damage. The rule adds only a constant-time check on an existing metabolism
tick, so use `retold:mob_tps_cow`, `retold:mob_tps_bat`, and `retold:mob_tps_villager` as the three
representative tick paths when that shared cadence changes, running each exact TPS ID separately.

For unloaded metabolism reconciliation, run the JUnit
`RetoldUnloadedEcosystemCatchUpTest` and exact
`retold:unloaded_hunger_catch_up_is_capped_and_budgeted` GameTest. They cover the seven-day cap,
sub-interval remainder, invalid timestamps, a real persisted entity timestamp, protected-mob
one-health clamping, queue deduplication, and the 16-mob-per-tick budget with deferred overflow.
Select exact contracts below or a coherent subgroup such as `retold:unloaded_*migration*`; do not
use `retold:unloaded_*` as a routine aggregate because the real-mob fixtures intentionally persist
through their isolated arenas and can contend for shared work budgets in randomized test order. Add
`retold:unloaded_feeder_catch_up_consumes_one_daily_meal` when catch-up food cadence, feeder
discovery, diet compatibility, conservation, or persisted meal state changes. It covers two real
daily Wheat removals for a Cow, exact interleaved metabolism/relief, an incompatible Chicken, and
source-priority health outcomes. Add `retold:animal_feeder_uses_paths_without_mob_griefing` whenever shared feeder
search or inventory code changes so the ordinary loaded route remains intact. Add
`retold:unloaded_natural_forage_catch_up_consumes_real_daily_blocks` and
`retold:unloaded_aquatic_forage_catch_up_consumes_real_daily_plants` when catch-up forage discovery,
daily selection, access, mutation, or relief changes. Add
`retold:unloaded_natural_forage_catch_up_respects_mob_griefing` when the destructive transaction or
world-protection routing changes. These cases cover distinct real land/aquatic source removal,
daily metabolism interleaving, persisted meal time, and denial without free relief. Add
`retold:unloaded_villager_food_catch_up_conserves_communal_provenance` when personal-first order,
communal discovery/restocking, or ownership reconciliation changes; pair it with the exact loaded
Villager consumer transaction affected. For unloaded predation, select the exact changed contract:
`retold:unloaded_predation_consumes_one_wild_prey_per_day_without_drops` covers the daily cap, real
prey conservation, exact metabolism relief, no loot/XP, and persisted feeding/hunt outcomes;
`retold:unloaded_predation_protects_named_tamed_and_tamed_hunter_animals` covers named and tamed
prey plus the tamed-hunter exclusion; and
`retold:unloaded_predation_respects_diets_and_closed_barriers` covers loaded-diet parity and the
reachability guard. `retold:unloaded_predation_uses_feeder_before_wild_prey` covers source priority
when predator feeder handling or the transition into predation changes. Add the exact loaded
hunt/kill regression when shared prey rules or hunt outcome state changes. Add
`retold:unloaded_breeding_uses_food_satisfaction_without_population_cap` when catch-up breeding
progress, hunger/fullness interleaving, or the no-population-cap rule changes. It covers a crowded
real Cow group, hunger-reset readiness, actual vanilla offspring, and the normal parent hunger cost.
Pair it with the focused `RetoldUnloadedBreedingProgressTest` timeline unit test. For unloaded
migration, select the exact changed contract: `retold:unloaded_land_migration_requires_one_day_and_no_food`
covers the elapsed-time gate and the feeder/meal anchor;
`retold:unloaded_land_migration_relocates_reachable_real_herd` covers a real mixed bovine group,
shared persisted range replacement, bounded distance, and physical landings;
`retold:unloaded_land_migration_respects_closed_barriers` covers the all-or-nothing reachability
guard; `retold:unloaded_pig_migration_relocates_reachable_foraging_group` covers the distinct Pig
foraging-range policy; and `retold:unloaded_aquatic_migration_relocates_reachable_real_school`
covers shared fish-school range replacement and water landings. Pair migration-policy or range
scoring changes with the exact loaded land/aquatic range case below. Add
`retold:unloaded_starvation_damages_wild_and_protects_named_tamed_animals` when offline critical-
hunger pulse accounting, accumulated damage, or the protected one-health floor changes. Add
`retold:unloaded_cube_starvation_splits_once` with every change to the offline Cube transaction or
shared Cube split/storage path; pair it with
`retold:cube_mob_size_scales_hunger_and_starvation_splits` when shared loaded behavior changes. Add
`retold:unloaded_farmer_production_uses_owned_crops_and_storage_provenance` when owned-crop indexing,
harvest/replant loot, Farmer reserve, storage access, or ownership deposits change; pair it with the
exact loaded crop and communal-supplier regressions affected by the shared path. Add
`retold:unloaded_natural_spawning_deduplicates_chunks_and_respects_rules` when returning-chunk
deduplication, daily debt, gamerule handling, or vanilla spawn delegation changes. The spawning
GameTest proves queue/rule behavior, not natural biome composition or successful placement; verify
those in-game. Add
`retold:starvation_damages_every_hunger_tick_owner` whenever queue entry changes so the ordinary
single-pulse Cow, Bat, and Villager paths remain immediate. This bounded reconciliation is not a
reason to run the per-species TPS matrix: current environment scans and real transactions are
episodic, globally task-bounded, and charged to the existing block-search/entity-scan budgets plus
separate reconciliation and migration path budgets. Migration processes at most one group per tick
and bounds route proofs to 64 blocks; Farmer and natural-spawn queues process at most one owner per
tick and never force-load chunks. Add focused performance coverage only if cadence, group size,
per-task scan/path bounds, or a shared task/work budget changes.

For the viability of natural loaded feeding, choose one exact `retold:hunger_survival_<mob>` ID. The
test family registers one isolated habitat case for every managed profile whose hunger interval is
positive, plus a registry guard that fails when a new hunger profile has no case. Each mob starts near critical hunger and
must remain alive while production behavior lowers hunger from a profile-appropriate opportunity:
live prey for hunters such as Wolves, water prey for Axolotls and Dolphins, plants or ground for
foragers, dropped food where appropriate, and communal storage for Villagers. A Creative mock
observer models player-loaded full LOD without becoming prey. The matrix deliberately uses nearby
patches for consumption checks; use the existing species, food-search, and feeder tests for longer
route requirements. Run one exact `retold:hunger_survival_<mob>` selector while diagnosing a
species. Do not rerun the 48-test group; run another exact habitat case only when its own feeding
contract changed. Add only affected exact species TPS IDs when profile cadence, scans, or paths can
cost more.

For Panda bamboo consumption, run the exact natural-consumption, unreachable-target replacement,
or griefing-denial test. Its cases use separate environments where necessary because the
entity-griefing gamerule and nearby bamboo fixtures are world-shared; combining them in one
concurrent environment can create false contention. The natural case requires production AI to
lower hunger and remove the exact nearby bamboo block, the route-recovery case requires the enclosed
nearest stalk to be replaced in the bounded search while both it and the reachable alternative
remain unchanged, and the denial case requires
`mobGriefing=false` to preserve both block and hunger. Add `retold:hunger_survival_panda` when the
meal transaction changes and `retold:mob_tps_panda` when its scan, movement, or tick path changes.
Do not expand these species-local contracts to either complete suite.

For missing natural food-acquisition routes, run the exact kill-meal or exclusion test, then run the
exact `retold:hunger_survival_<mob>` case for each changed
environmental hunter or forager. Add only species whose repeated scan, movement, or targeting path
changed to TPS validation. A meal credited from an existing death event does not by itself justify
a TPS run, while adding cached block discovery or controlled prey hunting does. Do not rerun the
complete hunger-survival matrix.

For Dolphin collective defense, run the exact
`retold:dolphins_collectively_defend_attacked_podmates` behavior test. It exercises the successful
damage-event entry point, direct retaliation versus faction-assist ownership, fed recruitment,
preservation of another urgent target, and cleanup after the threat disappears. Add the exact
`retold:mob_tps_dolphin` case when recruitment radius, scan caching, controlled continuation, or
path cadence changes; this species-local path does not by itself justify the complete matrix.

For loaded school-fish and Squid diets, run
`retold:aquatic_school_fish_graze_tagged_plants` for all four fish profiles, both default plant
families, actual removal, and the `mobGriefing` denial transaction. Run
`retold:aquatic_squid_consume_only_dropped_raw_fish` for both Squid profiles, raw-only standalone
defaults, stack remainder preservation, and the living-prey-hunting exclusion. When hunger,
repeated food scans, block search, sight, or path work changes, add only the exact affected
`retold:mob_tps_cod`, `salmon`, `tropical_fish`, `pufferfish`, `squid`, or `glow_squid` selectors;
do not select the complete matrix.

For loaded food-driven range migration, run the exact
`retold:herd_school_land_ranges_follow_local_food` case for Animal Feeder anchoring, depleted land
forage, shared range replacement, and reasoned migration ownership. Run the exact
`retold:herd_school_aquatic_ranges_follow_local_food` case for plant anchoring, shared persisted
school ranges, depletion, and a real aquatic path. Add the existing
`retold:herd_school_fish_use_species_paths` and
`retold:aquatic_school_fish_graze_tagged_plants` regressions when school routing or edible-plant
scoring changes. Repeated range scoring affects Cow, Pig, Cod, Salmon, Tropical Fish, and
Pufferfish; run those exact TPS selectors separately rather than selecting the complete matrix.

For Bee colony defense, choose the exact
`retold:bees_collectively_defend_harmed_colony_members` or
`retold:bees_defend_hives_but_not_smoked_harvests` behavior test. Together they exercise real
health damage, retaliation versus faction-assist ownership, busy-target preservation, cleanup,
smoke, Creative exclusion, unsmoked harvest, and hive breaking. Add the exact
`retold:mob_tps_bee` case when incident recruitment, scan caching, controlled continuation, or path
cadence changes; do not expand this species-local path to the complete matrix.

For Undead mounts, run the exact
`retold:wild_undead_mounts_are_hostile_until_claimed` or
`retold:claimed_undead_mounts_defend_themselves_and_owners` behavior test. Together they cover all
three profiles, persisted-owner faction boundaries, trap-tamed and always-tamed vanilla edge cases,
Zombie Horse taming preservation, real melee damage, retaliation/owner-defense ownership, stale
target cleanup, and claimed non-hunting. Add only the affected exact
`retold:mob_tps_skeleton_horse`, `retold:mob_tps_zombie_horse`, or
`retold:mob_tps_camel_husk` selector when dispatcher cadence, scan/sight radius, movement, damage,
or claim/defense event work changes; do not select the complete per-mob matrix for this family alone.

For Phantom pressure, run the exact
`retold:phantom_spawn_pressure_is_insomnia_independent` test when the spawn event, rarity,
time/weather, cover, difficulty, or compatibility boundary changes. Run
`retold:phantoms_do_not_prioritize_players_over_nearer_prey` when stalk-target scoring or Undead
diplomacy changes. Run `retold:phantom_stalker_completes_swoop_attack` when owned attack flight,
contact damage, target release, or recovery timing changes. Event-time and score-order changes do
not by themselves justify `retold:mob_tps_phantom`; add that exact TPS selector only when repeated
dispatch cadence, scan or sight caching, ownership continuation, movement, or path work changes.

For generic social defense, run the exact
`retold:successful_attacks_recruit_only_social_allies` test when the successful-damage trigger,
`retold:shared_defense_species` membership, cooperative-faction list, responder availability,
hearing/sight boundary, cap, or assist ownership changes. Run
`retold:visible_attack_intent_triggers_pre_hit_response` when pre-hit victim perception, the
24-block/sight boundary, prey flight, ordinary-predator defense, faction countertargeting,
exact-species witness warning, counter-rally suppression, or promotion to retaliation changes.
Pair it with `retold:successful_attacks_recruit_only_social_allies`,
`retold:ordinary_predators_defend_themselves_after_damage`,
`retold:passive_mobs_flee_every_successful_damage_source`,
`retold:faction_tags_drive_targeting_and_retaliation`,
`retold:village_defenders_ignore_neutral_piglins_and_endermen_until_attacked`, and
`retold:territory_warning_gates_attack_until_escalation` when the shared pre-hit target hook or
source policy changes. Because that hook executes for every target-holding `PathfinderMob`, a
change to its cadence, cache use, or per-tick classification is a documented reason for the full
per-species TPS matrix; a test-fixture-only or constant-only change is not. Run
`retold:witches_only_assist_illagers_in_same_active_raid` when conditional Illager cooperation
changes and `retold:endermen_only_coordinate_shared_defense_in_stage_3` when the specialist
exclusion or Enderman stage boundary changes. Add an exact affected-species TPS selector only when
scan cadence, cache use, continuation, or responder movement work changes.

For the shared target-distance ceiling, run the exact
`retold:mob_target_ranges_are_species_aware_and_bounded` selector. It covers raw Mob targets for
mobs and Survival players, Stage 1/2 Zombie and Skeleton limits, Brain-backed Piglin writes,
source-aware retaliation, retained-target release, receiver-side Undead assistance, and the
long-range Ghast exception. Pair it with exact Undead, Phantom, Wildfire, Wither, faction,
visible-intent, village-defender, territory, and threat-retargeting selectors when the central hook
or specialist boundaries change. The staggered retention check is shared tick work, so validate
deliberately chosen representatives with separate exact TPS IDs: Zombie, Skeleton, Piglin, Ghast,
Phantom, Wither, Warden, Ender Dragon, and Wildfire for the current policy. Do not use a target
wildcard or the complete matrix; broad fixture environments can overlap and do not represent a
production failure.

For cross-family Undead target parity, run the exact
`retold:undead_targeting_does_not_prioritize_players` selector. It covers Zoglin and Zombie
Nautilus faction membership, mutual Undead tolerance, ordinary hostility, and the Zombie-horde,
Skeleton-ranged, Ghast-artillery, and Zoglin-rampage score boundaries. Add only the exact affected
per-mob TPS selector when faction membership, repeated dispatch, scans, sight, movement, or path
work changes. A score-only constant removal does not require a TPS rerun; adding Zoglin or Zombie
Nautilus faction/profile work requires `retold:mob_tps_zoglin` or
`retold:mob_tps_zombie_nautilus` respectively.

For retained Undead diplomacy, run the exact
`retold:undead_allies_reject_and_clear_vanilla_targets` selector. It covers immediate raw Mob
target rejection, explicit `RETALIATION` ownership, a tamed Zombie Nautilus as a valid target,
and cleanup of both Mob and Brain targets when it becomes wild again. Run the exact Zoglin and
Zombie Nautilus TPS selectors when the shared target-policy or per-tick cleanup path changes; do
not select the complete matrix unless the faction cache or generic target-cleanup architecture
changes more broadly.

For internally tolerant indiscriminate factions, run the exact
`retold:indiscriminate_factions_follow_living_target_rules` selector. It covers Cube Mob and
monument Guardian allied-target rejection, valid outsider targeting, and explicit Retold-owned
retaliation. When the shared live target-policy path changes, add the exact affected
`retold:mob_tps_slime`, `retold:mob_tps_magma_cube`, `retold:mob_tps_guardian`, and
`retold:mob_tps_elder_guardian` selectors; do not select the complete matrix unless faction
classification caching or generic target cleanup changes more broadly.

For Stage 1/Stage 2 Undead coordination, run the exact
`retold:undead_stage_two_expands_coordination` selector. It covers the short Stage 1 same-family
baseline, the wider Stage 2 Zombie-family convergence radius, stable sampled Skeleton-family
assistance, and source-aware faction-assist ownership. When the stage pressure radii, recruitment,
scan/sight work, or repeated continuation path changes, add the eight exact
`retold:mob_tps_zombie`, `retold:mob_tps_zombie_villager`, `retold:mob_tps_husk`,
`retold:mob_tps_drowned`, `retold:mob_tps_zombified_piglin`, `retold:mob_tps_skeleton`,
`retold:mob_tps_stray`, and `retold:mob_tps_bogged` selectors. These fixtures force Stage 2 and
restore the previous saved stage during cleanup. Do not select unrelated profiles unless a shared
cache, sight, budget, or ownership primitive changes.

For ranged Undead firing-lane tactics, run the exact
`retold:ranged_undead_hold_fire_and_reposition_around_allies` selector. It invokes the real
Skeleton ranged-attack method to cover arrow suppression while an Undead ally intersects the
eye-to-target segment, retained `UNDEAD_RANGED` attack control, lateral/back-step destination
selection, reopening the lane, and preserved arrow creation afterward. Pair it with
`retold:undead_stage_two_expands_coordination`,
`retold:undead_targeting_does_not_prioritize_players`, and
`retold:owned_target_refresh_does_not_reassert_weapon_pose` when the shared ranged owner, target
validity, or bow lifecycle changes. Because the firing-lane query and repositioning run repeatedly,
also run the exact `retold:mob_tps_skeleton`, `retold:mob_tps_stray`, and
`retold:mob_tps_bogged` selectors; the Zombie-family and unrelated ranged profiles are unnecessary
unless a shared cache, target, movement, or ownership primitive changes.

For Stage 2 natural-spawn pressure, run the exact
`retold:undead_stage_two_increases_natural_spawn_weights` selector. It covers all eight default tag
members, Stage 1 and Stage 3 non-application, the rounded 25% Stage 2 bonus, preservation of the
original spawn data, unrelated monsters, and unrelated spawn categories. This potential-spawn-list
hook does not change loaded-mob tick work and therefore does not by itself require per-mob TPS
selectors. Naturally measure spawn composition under vanilla caps and with representative
spawn-list datapacks before tuning the bonus.

For ordinary-predator self-defense, run
`retold:ordinary_predators_defend_themselves_after_damage`. It applies real damage to wild Wolf,
tamed Wolf, Fox, Cat, Ocelot, Dolphin, Spider, and Cave Spider fixtures, then verifies the target,
`ATTACK` control, `RETALIATION` ownership, continuation after transient damage memory clears, and
the tame-owner exclusion. Pair it with `retold:wounded_predator_flee_respects_threshold_and_exemptions`
when changing the health-priority boundary, `retold:passive_mobs_flee_every_successful_damage_source`
when changing shared damage routing, and the affected species-specific defense selector when a
special defense owner changes. The fix adds no scan, path, cadence, or per-tick allocation, so it
does not by itself justify repeating the seven ordinary-predator TPS selectors.

For badly wounded wild-predator flight, run
`retold:badly_wounded_wild_predators_flee_attackers`,
`retold:wounded_predator_flee_respects_threshold_and_exemptions`, and
`retold:wounded_predator_flee_lasts_ten_seconds` as separate exact selectors. Together they cover
all seven ordinary predator species, ordinary retaliation/target release, the strict below-25%
boundary, tamed/Undead/boss/territory exemptions, and reasoned ownership expiry. Add
`retold:passive_mobs_flee_every_successful_damage_source` when the shared flee memory changes and
`retold:dolphins_collectively_defend_attacked_podmates` when the low-health guard touches Dolphin
defense. Because the rule adds repeated path-backed continuation, run the seven exact
`retold:mob_tps_wolf`, `retold:mob_tps_fox`, `retold:mob_tps_cat`, `retold:mob_tps_ocelot`,
`retold:mob_tps_dolphin`, `retold:mob_tps_spider`, and `retold:mob_tps_cave_spider` selectors; the
danger fixture lowers wild ordinary predators through the threshold with real damage. Do not select
the other 75 profiles unless a shared cache, movement, dispatcher, or work-budget primitive changes.

For shared flee-route stability, run the exact
`retold:flee_destinations_stay_in_escape_corridor` and
`retold:flee_routes_remain_committed_while_navigation_is_valid` selectors. They separately cover
the straight escape corridor retained across an eight-tick think refresh and an unchanged active
path retained while flee speed changes. The adjacent water selector exercises the bounded
deterministic detour because its straight corridor is blocked; movement-budget deferral must remain
neutral instead of being interpreted as a reason to alternate sides. Pair them with
`retold:passive_mobs_flee_every_successful_damage_source`,
`retold:land_mob_flee_paths_do_not_enter_water`,
`retold:visible_attack_intent_triggers_pre_hit_response`,
`retold:ignited_creeper_causes_delayed_flight_except_creepers_and_undead`,
`retold:cats_retreat_from_unignited_creepers`,
`retold:badly_wounded_wild_predators_flee_attackers`, and
`retold:ground_navigation_detects_no_progress_and_recovers_combat` when the shared destination or
path-reuse contract changes. Use exact Cow, Wolf, Salmon, and Cat TPS selectors as the representative
land prey, wounded predator, aquatic prey, and Creeper-retreat set; do not select the complete matrix
unless a broader scan, cache, dispatcher, LOD, or work-budget primitive also changes.

For Wither threat selection, run the exact `retold:wither_prioritizes_serious_threats` selector.
It covers Ghast, Zoglin, and wild Zombie Nautilus diplomacy, primary/side-head rejection and
retained cleanup, the dynamic tamed Zombie Nautilus boundary, active-threat priority over nearer
passive prey and a player, and source-aware target ownership. Add the exact
`retold:mob_tps_wither` selector when dispatcher
cadence, scan/sight work, side-head validation, scoring, or generic-faction-loop delegation changes;
do not expand this boss-local path to the complete matrix.

For the rare Soul Sand Valley Wither Skeleton spawn, run the exact
`retold:wither_skeletons_spawn_rarely_in_soul_sand_valleys` selector. It reads the server's
modified biome registry and guards the exact biome, smallest-positive weight, solitary pack, and
absence from ordinary Nether Wastes. This data-only spawn-list change does not justify a per-mob
TPS run; naturally verify spawn frequency and placement in fresh Soul Sand Valley terrain.

For Dried Ghast natural availability, run the exact
`retold:nether_fossils_omit_dried_ghasts` selector. It invokes the vanilla Nether-fossil placement
path at a deterministic successful candidate, verifies that no Dried Ghast is generated, and
confirms that the block itself remains placeable. This narrow worldgen hook does not justify mob or
TPS selectors; naturally verify fresh Nether fossils across multiple seeds.

For the pure Earth Labyrinth topology, run the JUnit
`cz.xefensor.retold.worldgen.earth.EarthLabyrinthPlannerTest`. It samples deterministic layout
variation and requires 512 seeds to keep both levels, every cell, and the guardian chamber reachable
from the fixed entrance, with four or five arena-safe stairs, loops, adjacent unique passages, and
the intended 77- or 101-block footprint; it also guards stable world/structure-position seed
derivation. Run the
exact `retold:jungle_pyramids_use_earth_labyrinth_structure`,
`retold:earth_labyrinth_pieces_round_trip_without_upgrading_old_pyramids`,
`retold:earth_labyrinth_staircase_leaves_puzzle_room_safely`, and
`retold:earth_labyrinth_piece_places_connected_maze_geometry` GameTests for registry replacement,
new/old serialized-piece compatibility, safe lever-room egress, and representative placed cavern,
tunnel interior/shell, ladder, entrance geometry, and the huge guardian arena's 31-block core and
ten-block vertical clearance.
These tests do not prove natural placement. Verify multiple
fresh seeds, both footprint sizes, all pyramid orientations, height extremes, chunk borders, caves,
neighboring structures, and an upgraded-world pyramid before describing world generation as fully
verified. Run exact `retold:earth_guardian_foundation_spawns_and_persists_lifecycle` for guardian
registration/spawn-egg mapping, structure ownership, duplicate repair, Stage 1 dormancy, Stage 2
Survival-player awakening, entity save/load, authoritative encounter state, and the permanent
defeat boundary. This foundation deliberately does not test sensing, combat, or mutation.
Run exact `retold:earth_guardian_tracks_weighted_vibrations_without_sight` for accepted
Survival-player sources, weighted multiplayer clue replacement, direct-damage investigation,
absence of sight targeting, `EARTH_GUARDIAN` movement ownership, and remembered-clue save/load.
Vanilla vibration dispatch and particle networking cannot be exercised with clientless mock players;
verify real connected-player steps, sneaking, occlusion, projectiles, and simultaneous sources on a
dedicated server before describing the sensory path as fully verified.
Run exact `retold:earth_guardian_physically_investigates_vibrations` for actual movement toward a
remembered clue outside attack range with guardian ownership and no sight target. Run exact
`retold:earth_guardian_physically_patrols_maze` for physical idle patrol inside a bounded maze fixture.
These tests cover the production navigation handoff; complex generated routes and vertical links
still need natural-world verification. Changes to this path also select the exact guardian TPS case.
Run exact `retold:earth_guardian_uses_environmental_attacks` for rejected direct melee, the fixed-location 30-tick
warning, pending-hazard save/load, real protection-approved ceiling blocks with clear fall paths
becoming damaging falling debris, open-sky and obstructed-collapse fallback to lava, 3×3 floor
conversion including Bedrock and containers, independent break/place protection denial, griefing
disabled during windup, absence of scripted damage/ignition/launch, and no sight target. The synthetic
test drives the attack clock directly. Run exact
`retold:earth_guardian_ceiling_debris_hits_on_server_ticks` for ordinary entity ticking through the
full harmless warning and release, a target that moves after lock-on, physical falling-block travel,
and a real `minecraft:falling_block` damage source at the original location. Run exact
`retold:earth_guardian_lava_ground_burns_on_server_ticks` for real lava placement after the warning,
a victim sinking into the replaced floor and taking vanilla `minecraft:lava` damage, retained source
blocks, and safe ground after moving away from the locked patch. Run exact
`retold:earth_guardian_floor_collapse_causes_real_fall` for the bounded 3×3-by-four-block pit,
Bedrock/container removal, protected-column preservation, fixed target after moving away,
pending floor-warning save/load, legacy next-attack migration, and real `minecraft:fall` damage
on landing. The synthetic environmental test also checks the ceiling/lava/floor rotation and
disabling griefing during the floor warning. These tests do not establish
particle or sound readability, arena ceiling depletion, accumulated lava spread/fire or encounter
balance, connected-player networking, multiplayer, or claim-plugin integration.
Run exact `retold:earth_guardian_relocates_any_loaded_block` for the intentionally unrestricted
block policy. It proves that an outside-bound Chest/block entity and Bedrock are valid material,
that an outside vibration triggers excavation, that the exact block states pass through the
persisted 64-block reserve without duplication, and that independent `MOB_BREAK`/`MOB_PLACE`
protection denials remain authoritative. The synthetic fixture does not establish natural pursuit
cadence, maximum escape/destruction reach, fluid or unusual-state behavior, block-entity data loss,
many accumulated edits, client readability, multiplayer contention, or real claim-plugin behavior.
Verify those in a disposable generated world with `mobGriefing` both enabled and disabled before
describing unrestricted terrain shaping as naturally verified.
Run exact `retold:mob_tps_earth_guardian` when listener, memory, patrol, investigation, attack, or
terrain-edit cadence or work changes. The 50-active-guardian run passes all five phases below
50 ms/tick after adding the floor-collapse rotation, with a 10.104 ms/tick hunt/targeting peak on 2026-09-07.
Command-spawned guardians share the same terrain controller and bounded hazard checks, but this
benchmark disables `mobGriefing`: actual mutation and accumulated fluid-spread cost still need
separate natural-world profiling.

For sparse Nether forests, run the exact
`retold:nether_forests_use_sparse_desert_vegetation` selector. It reads the modified biome registry,
verifies that both forests replaced every targeted dense vanilla placement with the Retold sparse
variants, and guards representative retained lava-spring and quartz features. This data-only
vegetation-density change does not justify mob or TPS selectors. The developer should visually
verify Crimson and Warped Forest density, sightlines, transitions, and resource availability in
fresh chunks across multiple seeds; existing generated vegetation should remain unchanged.

For Overworld Nether-portal drain mappings or protection, run the exact
`retold:nether_portal_drain_maps_materials_and_protects_valuable_blocks` selector. It covers the
required living-ground-to-Coarse-Dirt intermediate state, proves the death phase cannot directly
corrupt stone, proves even living ground first encountered by the corruption resolver cannot skip
Coarse Dirt, then covers final soil/stone/masonry/wood state-preserving mappings, permanent water
evaporation, the valuable/container/portal-frame boundary, and a denying world-protection rule. For
palette membership changes, also run
`retold:nether_portal_drain_uses_explicit_palette`. It guards stable Netherrack, Blackstone, Warped
wood, Soul Soil, and Gravel; representative unchanged crafted/decorative blocks; Sand, Clay,
Sandstone, and brick conversions; frozen-block melting; small-plant and cactus death; exact coral
death; and the different outer/inner handling of waterlogged blocks. For
lava-resistance changes, run
`retold:nether_portal_lava_sources_linearly_resist_spread`. It proves that only source blocks inside
the full affected sphere count—excluding flowing lava and sources outside the radius—and guards
full, five-sixths, half, one-sixth, stopped, and over-saturated linear work rates. For
successful-travel triggering or work caps, run the separate exact
`retold:nether_portal_travel_pulses_are_bounded` selector. When candidate selection or the radial
front changes, run `retold:nether_portal_drain_advances_outward`, which requires every ordered
candidate distance to be monotonically nondecreasing and verifies that the corruption radius can
never exceed half the death radius, including both final 8/16-block shells. Do not substitute a
client wait for the bounded automated checks. When portal-size scaling, portal rectangle detection,
or the maximum offset index changes, run `retold:nether_portal_drain_scales_with_portal_area`; it
guards the standard 2×3 radius, per-block area growth, representative width/height composition, the
48-block cap, and complete maximum-radius index coverage. Naturally verify the
intended 30–45-minute visible outward maturation around outdoor and underground portals, unusual
frame sizes, adjacent portals, chunk borders and unloads,
save/reload, existing worlds, entities/items traveling both directions, real claim adapters,
multiplayer, and dedicated-server behavior. Subjective appearance and pacing approval belongs to
the developer.

For Strider lava sustenance specifically, the natural-food case must also assert that relief does
not consume the lava; pair it with `retold:hunger_survival_strider` and `retold:mob_tps_strider`.

For Villager golem construction, select the exact profession, eligibility, currency,
structure-animation, or player-placement test changed. Add
`retold:mob_tps_villager` only
when the repeated Villager dispatcher, scan, budget, ownership, or navigation path changes; the
player pumpkin-placement hook alone does not justify a TPS rerun. Long-running construction
fixtures must trade-lock manually assigned professions and select an allowed activity while testing
staged mechanics; otherwise vanilla may legitimately remove an unclaimed profession or Retold may
correctly pause for the inherited schedule.

For Villager torch maintenance, select the exact relighting test for magical casting, Nitwit
close-range fake-tool use, stage, village, range, priority, inventory conservation, or wall-state
behavior. The all-stage magical test forcibly turns an active caster away and requires the next
continuous action tick to restore body, head, and look-control alignment. Add the single
`retold:villager_relights_nearby_torches_in_one_maintenance_run` selector when consecutive-search,
batch-limit, success-cooldown, or indexed multi-torch behavior changes. Add the single
extinguished-torch drop regression only when torch conversion/indexing
changes. Add `retold:mob_tps_villager` when the dispatcher, index query, physical route, cooldown,
budget, or ownership hot path changes; do not expand to unrelated Villagers or every mob. Because
vanilla path creation is nondeterministic at the framework's random multi-million-block test
coordinates, the Nitwit GameTest begins at a valid supported close-use cell; verify the longer
physical approach naturally rather than weakening or faking production movement.

When fixing a regression, rerun only the exact reproducing test. Add another exact test only when a
separate changed contract needs validation. Do not run the unfiltered GameTest command.

## TPS Selection

Performance validation is required when a change can alter repeated tick work, scans, pathfinding,
cache behavior, work budgets, allocations, or loaded-group behavior. Start with affected species:

```bash
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:mob_tps_villager"
```

Use one explicit species TPS ID at a time. Do not use a profile/family wildcard. Do not run TPS
tests for documentation, test renames, one-off interaction plumbing outside tick paths, or
unrelated resources.

Do not run the complete per-mob TPS matrix. For a shared change, choose each genuinely affected
species deliberately and run its exact TPS ID as a separate command.

## Manual Verification

Choose manual checks by the same rule. A local animal-feeding change needs its natural animal setup,
not an unrelated dimension or multiplayer pass. Dedicated-server, multiplayer, fresh-world,
existing-world, long-session, and visual checks are separate environments and are required only when
the changed contract can differ there or when an integration milestone calls for them.

Report each environment separately and never infer an unperformed result from an automated pass.
