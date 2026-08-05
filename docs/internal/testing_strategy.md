# Retold Test Selection Strategy

This document defines how to choose validation for a change. The goal is to catch the regressions a
change can plausibly cause without repeatedly running unrelated, long suites.

## Core Rule

Before running tests, identify:

1. the files and owning subsystem changed
2. the behavior or contract that could regress
3. the smallest automated selector that exercises that behavior
4. whether the change crosses a shared boundary that justifies broader validation

Start with that smallest selector. If it passes and none of the escalation conditions below apply,
stop. Do not run the complete GameTest suite or complete per-mob TPS matrix merely because code
changed or because a task is being handed off.

Record the exact commands and results. When a broad suite is run, also record the specific reason it
was necessary.

## Validation By Change

| Change | Required validation | Do not run by default |
| --- | --- | --- |
| Documentation only | `git diff --check`; inspect changed links, headings, and claims against the current code | Gradle, GameTests, TPS tests |
| Java compile/style or isolated refactor with no behavior change | `./gradlew build` once before handoff | GameTests and TPS tests |
| Pure logic or state transition | Focused JUnit test with `./gradlew test --tests '<class-or-pattern>'`; `./gradlew build` once before handoff | GameTests unless Minecraft integration is involved |
| Local gameplay behavior | The narrowest matching GameTest selector; `./gradlew build` once before handoff | Complete GameTest suite |
| Species-specific mob behavior | Matching behavior GameTests; add the one-species TPS selector only when tick cadence, scans, navigation, allocations, or loaded-group cost changed | Complete TPS matrix and complete GameTest suite |
| Shared mob behavior used by several known species | Focused behavior GameTests plus representative affected species; run focused TPS selectors only for the affected profiles when hot-path cost changed | Complete suites unless the change reaches the broad shared systems listed below |
| Resources, recipes, loot, tags, or profiles | Focused loader/registration test and the affected behavior test when one exists; `./gradlew build` once before handoff | Unrelated gameplay suites |
| Worldgen, dimension, networking, persistence, or visuals | Focused automated coverage plus only the relevant manual environments described in `AGENTS.md` | Unrelated mob and TPS suites |

`./gradlew build` runs compilation, JUnit tests, PMD, and assembly; it does not run NeoForge
GameTests. It is the normal final code-quality check, not something to repeat after every edit.

## Focused GameTests

Use the narrowest `retold:` selector that includes the changed contract. For example:

```bash
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:villager_communal_food_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:villager_paths_to_communal_food_storage"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:farmer_communal_supply_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:village_container_ownership_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:village_crop_reputation_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:village_animal_reputation_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:golem_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:villager_relight*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:extinguished_torches_drop_matching_lit_items"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:animal_feeder_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:starvation_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:hunger_survival_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:natural_food_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:panda_bamboo_*"
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:*bat*"
```

The Villager consumer transaction, consumer route, and Farmer supply selectors intentionally use
separate isolated GameTest environments. Run those three narrow selectors instead of combining
their route fixtures through a broader wildcard.

For village-container provenance or witnessed theft, use
`retold:village_container_ownership_*`. It covers generated village loot, persisted exact-stack
ownership, Villager/Farmer additions, mixed player and village quantities, menu withdrawal,
container breaking, vanilla gossip, Creative exclusion, and the bounded village-status summary.
Add the directly affected communal
food, Farmer supply, or exact golem-currency selector only when its transaction hook changes. These
operations run on loot unpack, inventory transfer, menu click, or block break rather than a tick
path, so they do not justify a TPS selector unless repeated AI discovery or transfer cadence also
changes.

For village crop ownership and reputation, use `retold:village_crop_reputation_*`. It drives the
real vanilla Farmer planting behavior through `HarvestFarmlandMixin` and covers persisted Farmer
provenance, player-placement clearing, mature harvest theft, immature crop vandalism, farmland
trampling, witnesses, and Creative exclusion. This adds event-time work plus one constant-time
SavedData update when vanilla Farmer work actually changes a crop; it does not justify a Villager
TPS rerun unless the Farmer behavior cadence or search path changes.

For profession livestock tending and village-animal reputation, use
`retold:village_animal_reputation_*`. It covers the Shepherd/Leatherworker/Butcher species split,
exact two-item storage conservation, hunger relief without immediate love mode, persisted entity
ownership, player-handled protection, both village-owned and player-associated offspring inheritance, witnessed `-50` gossip, and monster,
environmental, and Creative exclusions. Add `retold:mob_tps_villager` when the repeated tending
dispatcher, cached animal scan, storage search, control ownership, or navigation changes. Do not
run unrelated species or the complete suites for entity-persistent provenance or death-event-only
changes.

For the global hunger-satisfaction breeding contract, use `retold:animal_breeding_*`. It covers all
currently tagged vanilla breeder types and their positive hunger profiles, real player feeding
without direct love mode, the five-minute full-satisfaction gate, hungry-mate rejection, actual
vanilla Cow offspring, 40-hunger parent cost, one-minute retry state, entity save/load,
Horse/Donkey compatibility, and readiness interruption. Add `retold:village_animal_reputation_*`
only when Villager feeding or livestock provenance changes. Because the dispatcher adds one
constant-time 20-tick check to breedable animals and performs a cached scan only after five
satisfied minutes, use the 256-mob bounded-work test plus one representative ordinary breeder TPS
case; add affected new profile cases separately. Do not run the complete GameTest suite or full TPS
matrix unless shared primitives, isolation, a milestone/release baseline, or an explicit developer
request justifies them.

For loaded starvation, use `retold:starvation_*`. It covers the ordinary `PathfinderMob`, separate
non-pathfinding Bat, and Villager communal-food hunger owners, first critical damage, terminal
death, exclusion of profiles whose hunger interval is zero, and a registry-wide assertion that
every loaded positive-hunger profile reaches one of those owners. Add the exact
`retold:cube_mob_size_scales_hunger_and_starvation_splits` regression when the shared critical-
hunger dispatch changes, because Cube Mobs must split or die through their specialized rule rather
than receive generic damage. The rule adds only a constant-time check on an existing metabolism
tick, so use `retold:mob_tps_cow`, `retold:mob_tps_bat`, and `retold:mob_tps_villager` as the three
representative tick paths when that shared cadence changes. Do not run the complete GameTest suite
or TPS matrix unless another documented escalation condition applies.

For the viability of natural loaded feeding, use `retold:hunger_survival_*`. It registers one
isolated habitat case for every managed profile whose hunger interval is positive, plus a registry
guard that fails when a new hunger profile has no case. Each mob starts near critical hunger and
must remain alive while production behavior lowers hunger from a profile-appropriate opportunity:
live prey for hunters such as Wolves, water prey for Axolotls and Dolphins, plants or ground for
foragers, dropped food where appropriate, and communal storage for Villagers. A Creative mock
observer models player-loaded full LOD without becoming prey. The matrix deliberately uses nearby
patches for consumption checks; use the existing species, food-search, and feeder tests for longer
route requirements. Run one exact `retold:hunger_survival_<mob>` selector while diagnosing a
species, then rerun the 41-test group after a shared feeding or coverage change. Add only affected
species TPS selectors when profile cadence, scans, or paths can cost more. This focused matrix does
not justify either complete suite by itself.

For Panda bamboo consumption, use `retold:panda_bamboo_*`. Its two cases use separate environments
because the entity-griefing gamerule and nearby bamboo fixtures are world-shared; combining them in
one concurrent environment can create false contention. The natural case requires production AI to
lower hunger and remove the exact nearby bamboo block, while the denial case requires
`mobGriefing=false` to preserve both block and hunger. Add `retold:hunger_survival_panda` when the
meal transaction changes and `retold:mob_tps_panda` when its scan, movement, or tick path changes.
Do not expand this species-local transaction to either complete suite.

For missing natural food-acquisition routes, use `retold:natural_food_*` for kill-meal credit and
family/Creeper exclusions, then run the exact `retold:hunger_survival_<mob>` case for each changed
environmental hunter or forager. Add only species whose repeated scan, movement, or targeting path
changed to TPS validation. A meal credited from an existing death event does not by itself justify
a TPS run, while adding cached block discovery or controlled prey hunting does. Do not rerun the
complete hunger-survival matrix unless profile coverage or broadly shared feeding behavior changed.
For Strider lava sustenance specifically, the natural-food case must also assert that relief does
not consume the lava; pair it with `retold:hunger_survival_strider` and `retold:mob_tps_strider`.

For Villager golem construction, use `retold:golem_*` for the profession whitelist, one-off
eligibility, currency, structure-animation, and player-placement contracts. Add
`retold:mob_tps_villager` only
when the repeated Villager dispatcher, scan, budget, ownership, or navigation path changes; the
player pumpkin-placement hook alone does not justify a TPS rerun.

For Villager torch maintenance, use `retold:villager_relight*` for magical casting, Nitwit
close-range fake-tool use, stage, village, range, priority, inventory conservation, and wall-state
behavior. Add the single extinguished-torch drop regression only when torch conversion/indexing
changes. Add `retold:mob_tps_villager` when the dispatcher, index query, physical route, cooldown,
budget, or ownership hot path changes; do not expand to unrelated Villagers or every mob. Because
vanilla path creation is nondeterministic at the framework's random multi-million-block test
coordinates, the Nitwit GameTest begins at a valid supported close-use cell; verify the longer
physical approach naturally rather than weakening or faking production movement.

When fixing a regression, rerun the reproducing test and its directly related group. A focused
failure does not justify running a broader suite before the focused failure is stable.

Run the complete GameTest suite only when at least one of these is true:

- a change affects a shared lifecycle, registration, test environment, or persistence boundary used
  across otherwise unrelated systems
- a common dispatcher, AI ownership/target policy, movement adapter, profile loader, or broad mixin
  changes in a way that can affect many unrelated test groups
- multiple subsystems were intentionally integrated and focused selectors cannot cover their
  interaction
- a failure appears only under full-suite ordering, shared-budget contention, or test isolation
- this is an explicit release, merge, or milestone gate
- the developer explicitly asks for the complete suite

The unfiltered command is:

```bash
./gradlew runGameTestServer
```

## TPS Selection

Performance validation is required when a change can alter repeated tick work, scans, pathfinding,
cache behavior, work budgets, allocations, or loaded-group behavior. Start with affected species:

```bash
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:mob_tps_villager"
```

Use several explicit species selectors or the smallest profile/family selector available when a
shared behavior affects a known subset. Do not run TPS tests for documentation, test renames,
one-off interaction plumbing outside tick paths, or unrelated resources.

Run the complete per-mob TPS matrix only when:

- the central dispatcher, LOD, caches, shared work budgets, profile schema/loader, or a broadly used
  per-tick behavior changes
- a change is expected to affect most profiles or the matrix's registration/fixture coverage
- focused measurements reveal a regression whose scope is unclear
- this is an explicit performance baseline, release, or milestone gate
- the developer explicitly asks for the complete matrix

The complete matrix command is:

```bash
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:mob_tps_*"
```

Do not automatically run both the complete GameTest suite and complete TPS matrix. Each needs its
own justification.

## Manual Verification

Choose manual checks by the same rule. A local animal-feeding change needs its natural animal setup,
not an unrelated dimension or multiplayer pass. Dedicated-server, multiplayer, fresh-world,
existing-world, long-session, and visual checks are separate environments and are required only when
the changed contract can differ there or when an integration milestone calls for them.

Report each environment separately and never infer an unperformed result from an automated pass.
