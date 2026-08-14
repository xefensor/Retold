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

`./gradlew build` runs compilation, JUnit tests, PMD, and assembly; it does not run NeoForge
GameTests. It is the normal final code-quality check, not something to repeat after every edit.

## Focused GameTests

Use the exact `retold:` test ID for the changed contract. For example:

```bash
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:villager_paths_to_communal_food_storage"
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
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:panda_bamboo_breaks_and_feeds"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:bats_find_high_ceiling_and_search_in_five_member_parties"
```

Generic push, pull-request, and release workflows run `./gradlew build` but do not choose GameTests
automatically because the correct exact test depends on the changed behavior. The developer or
implementer records the exact GameTest command and result for the change before handoff.

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
ordinary `PathfinderMob`, separate non-pathfinding Bat, and Villager communal-food hunger owners,
first critical damage, terminal death, exclusion of profiles whose hunger interval is zero, and a
registry-wide assertion that every loaded positive-hunger profile reaches one of those owners. Add the exact
`retold:cube_mob_size_scales_hunger_and_starvation_splits` regression when the shared critical-
hunger dispatch changes, because Cube Mobs must split or die through their specialized rule rather
than receive generic damage. The rule adds only a constant-time check on an existing metabolism
tick, so use `retold:mob_tps_cow`, `retold:mob_tps_bat`, and `retold:mob_tps_villager` as the three
representative tick paths when that shared cadence changes, running each exact TPS ID separately.

For the viability of natural loaded feeding, choose one exact `retold:hunger_survival_<mob>` ID. The
test family registers one isolated habitat case for every managed profile whose hunger interval is
positive, plus a registry guard that fails when a new hunger profile has no case. Each mob starts near critical hunger and
must remain alive while production behavior lowers hunger from a profile-appropriate opportunity:
live prey for hunters such as Wolves, water prey for Axolotls and Dolphins, plants or ground for
foragers, dropped food where appropriate, and communal storage for Villagers. A Creative mock
observer models player-loaded full LOD without becoming prey. The matrix deliberately uses nearby
patches for consumption checks; use the existing species, food-search, and feeder tests for longer
route requirements. Run one exact `retold:hunger_survival_<mob>` selector while diagnosing a
species. Do not rerun the 41-test group; run another exact habitat case only when its own feeding
contract changed. Add only affected exact species TPS IDs when profile cadence, scans, or paths can
cost more.

For Panda bamboo consumption, run the exact natural-consumption or griefing-denial test. Its two
cases use separate environments because the entity-griefing gamerule and nearby bamboo fixtures are world-shared; combining them in
one concurrent environment can create false contention. The natural case requires production AI to
lower hunger and remove the exact nearby bamboo block, while the denial case requires
`mobGriefing=false` to preserve both block and hunger. Add `retold:hunger_survival_panda` when the
meal transaction changes and `retold:mob_tps_panda` when its scan, movement, or tick path changes.
Do not expand this species-local transaction to either complete suite.

For missing natural food-acquisition routes, run the exact kill-meal or exclusion test, then run the
exact `retold:hunger_survival_<mob>` case for each changed
environmental hunter or forager. Add only species whose repeated scan, movement, or targeting path
changed to TPS validation. A meal credited from an existing death event does not by itself justify
a TPS run, while adding cached block discovery or controlled prey hunting does. Do not rerun the
complete hunger-survival matrix.
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
behavior. Add the single extinguished-torch drop regression only when torch conversion/indexing
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
