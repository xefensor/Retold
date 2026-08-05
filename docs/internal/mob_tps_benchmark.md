# Per-Mob TPS Benchmark

> Baseline recorded on 2026-08-03. Absolute wall-clock values are machine-specific; use the same host and command when comparing revisions.

## Coverage

`RetoldPerMobTpsGameTests` registers one isolated GameTest for every loaded Retold mob profile. The current matrix registers 77 mob types; the latest complete baseline covers 75, while the original clean table below records the earlier 68-profile baseline. Each test creates 50 subjects in a habitat fixture suited to that species and measures five consecutive 80-server-tick phases after a 20-tick warmup:

1. `idle_rest`: ordinary loaded behavior with no injected food or opponent.
2. `dropped_food_forage`: dropped food plus profile-appropriate forage blocks.
3. `hunt_targeting`: profile-appropriate prey or combat targets.
4. `danger_social`: threats and nearby entities that exercise retaliation, assistance, flock, pack, or swarm behavior. Shared passive-flee profiles also take one real point of threat damage so this phase measures immediate damage flight and its remembered follow-through.
5. `habitat_day_night`: the opposite time-of-day/habitat condition, including special species stimuli where needed.

The fixture supplies water, caves, ceiling space, Nether ground, hives, flowers, bamboo, mud, sand, cobwebs, prey, threats, or Warden disturbances as appropriate. It disables `mobGriefing` during measurement so 50 destructive mobs cannot erase the shared fixture; behavior decisions and searches still run. Bosses and special mobs retain their relevant vanilla behavior, while the test-only Warden prevents distance despawning long enough to measure all phases.

Each result logs average milliseconds per actual server tick, sustainable TPS, active subject count, and Retold scan/path/sight/block-search counters. A phase fails at an average of 50 ms/tick or greater. The matrix tests each broad behavior family under load; focused GameTests and in-game checks are still required for exact interactions and natural terrain.

## Command

Run the complete isolated matrix:

```bash
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:mob_tps_*"
```

Run one species:

```bash
./gradlew runGameTestServer --args="net.neoforged.fml.startup.GameTestServer --tests retold:mob_tps_sniffer"
```

Search `run/logs/latest.log` for `MOB_TPS_RESULT` to inspect the phase records and debug counters.

## Baseline Result

All 68 tests and all 340 measured phases passed. The complete run took 1.509 minutes. The largest remaining phase was Bat hunting at 11.833 ms/tick, followed by Bat habitat/day-night at 11.474 ms/tick and Bat danger/social at 10.900 ms/tick.

The first matrix run exposed a Sniffer range-search multiplier: every candidate in a 37 x 9 x 37 search also performed an 11 x 5 x 11 nearby-diggable search. That produced phase averages of 216.917, 121.084, 127.910, and 132.434 ms/tick under active stimuli. A new range anchor now has to be a diggable block itself; existing stored range memories still use the separate nearby-diggable validation. After the fix, the Sniffer peak in the clean complete run is 3.340 ms/tick.

### Damage-Flee Workload Rerun

After the danger phase was changed to inflict real damage on shared passive-flee profiles, the complete 68-test/340-phase matrix passed again in 1.265 minutes on 2026-08-03. The highest phase in that run was Camel danger/social at 6.590 ms/tick; Bat danger/social was next at 6.069 ms/tick. Every newly exercised passive-flee danger phase remained below 7 ms/tick, including Trader Llama at 4.376, Mule at 4.304, Horse at 3.984, Llama at 3.991, Goat at 3.273, Sheep at 2.562, Mooshroom at 2.514, Cow at 2.414, Pig at 2.035, Chicken at 1.604, Rabbit at 1.453, and Donkey at 3.876 ms/tick. Absolute values are not directly comparable across host load, but the complete rerun stayed well below the 50 ms gate.

### Bat-Isolation Fix Rerun

After clientless-player synchronization and the broken-roost budget-window regression were hardened,
the complete 68-test/340-phase matrix passed again in 1.284 minutes on 2026-08-03. Bat
habitat/day-night was highest at 8.438 ms/tick, followed by Bat danger/social at 7.887 ms/tick and
Frog habitat/day-night at 7.455 ms/tick. All phases remained below the 50 ms/tick gate.

### Herd/School Ecology Expansion Rerun

After adding data profiles for Cod, Salmon, Tropical Fish, Pufferfish, Squid, and Glow Squid, the
expanded 74-test/370-phase matrix passed in 1.396 minutes on 2026-08-03. Every phase remained below
the 50 ms/tick gate. Skeleton idle/rest was the host-load-dependent overall peak at 7.516 ms/tick.
The six new aquatic profiles peaked at 2.905 ms/tick for Cod danger/social; Pufferfish peaked at
2.777, Tropical Fish at 2.199, Salmon at 2.077, Glow Squid at 1.718, and Squid at 1.377 ms/tick.
The four fish fixtures exercise exact-species school scans and aquatic path requests. The Squid
danger phase also exercises the final bounded same-species damage-panic propagation path.

### Animal Feeder Rerun

After the loaded Animal Feeder search was added to the ordinary food pipeline, the complete
74-test/370-phase matrix passed again in 1.232 minutes on 2026-08-03. Every phase remained below
50 ms/tick; Bat habitat/day-night was the host-load-dependent peak at 6.815 ms/tick. The benchmark
fixtures contain no feeders, so eligible land animals exercise cached negative lookups and shared
block-search backpressure rather than successful trough consumption. The focused feeder GameTests
cover the positive route and consumption path separately.

After the exact feeder-approach correction, the matrix passed 74/74 and 370/370 again in
1.395 minutes on 2026-08-04. Every phase remained below 50 ms/tick; Bat danger/social was the
host-load-dependent peak at 6.982 ms/tick. Because these fixtures contain no feeders, this confirms
that distinguishing exact route precision did not regress ordinary managed movement; the focused
real-Sheep fixture covers the zero-reach-range feeder path itself.

### Shared Feeding Pose Rerun

After every Retold consumption path was routed through the two-second stationary source-facing
pose, the matrix passed 74/74 and 370/370 again in 1.746 minutes on 2026-08-04. Every phase remained
below 50 ms/tick; Bat danger/social was the host-load-dependent peak at 9.692 ms/tick. The
dropped-food/forage fixtures exercise real consumption for applicable profiles, while focused tests
cover exact source memory, zero movement, body/look orientation, a feeder, and a non-pathfinding Bat.

### Villager Communal Food Rerun

Adding the `VILLAGER_COMMUNAL` profile expanded the matrix to 75 tests and 375 phases. The complete
2026-08-04 run passed 75/75 in 1.390 minutes with every phase below 50 ms/tick. The Villager fixture
supplies four ordinary barrels, persisted meeting-point context, and Bread so 50 Villagers exercise
positive cached container discovery and path ownership rather than only a negative lookup. Villager
phase averages were 2.774 idle/rest, 2.952 communal-food forage, 2.112 hunt/targeting, 3.319
danger/social, and 2.304 habitat/day-night ms/tick. Absolute values remain host-load-dependent.

### Farmer Communal Supply Focused Rerun

The Farmer supplier path changes repeated Villager tick work, but no other profile or shared
performance primitive changed. Following `testing_strategy.md`, only
`retold:mob_tps_villager` was rerun on 2026-08-04 rather than repeating the other 74 species. The
50-Villager fixture now makes half the sample Farmers with a small Bread surplus, a retained job
site, and shared village barrels. It explicitly starts their supplier behavior during the measured
daytime phase so the positive block-search and movement path is included instead of measuring only
sleeping or cached-negative behavior.

All five phases passed below 50 ms/tick: 8.193 idle/rest, 5.846 dropped-food/forage, 6.560
hunt/targeting, 7.395 danger/social, and 3.619 habitat/day-night ms/tick. The supplier phase recorded
four bounded block searches and three path requests; remaining simultaneous attempts were held by
the shared work budget. The latest complete 75-species/375-phase result remains the pre-supply
baseline above and was not rerun because this species-local change did not meet a broad-matrix
escalation condition.

### Villager Personal Stock Focused Rerun

After hungry Villagers were changed to consume carried food first and batch-restock up to 12 food
points, only `retold:mob_tps_villager` was rerun on 2026-08-04. The existing 50-Villager fixture
already includes personal Farmer food, empty consumers, positive Bread storage, and supplier work,
so it exercises both the inventory-first and storage-restock branches without widening the matrix.

All five phases passed below 50 ms/tick: 8.459 idle/rest, 5.208 dropped-food/forage, 5.781
hunt/targeting, 6.560 danger/social, and 3.172 habitat/day-night ms/tick. Batch restocking reduced
repeated storage work in the measured active phases; dropped-food/forage recorded 84 block searches
and 67 paths compared with 169 searches and 135 paths in the prior supplier run. Absolute timing
remains host-load-dependent, but the work counters confirm the intended reduction in repeated trips.

### Villager Livestock Tending Focused Rerun

Profession livestock tending adds one cached nearby-animal query to eligible loaded Villagers and
can start storage and pen navigation, so only `retold:mob_tps_villager` was rerun on 2026-08-04.
The central dispatcher, shared cache/budget primitives, and other species were unchanged, so the
complete matrix was not justified.

All five phases passed below 50 ms/tick: 6.864 idle/rest, 4.794 dropped-food/forage, 6.211
hunt/targeting, 6.292 danger/social, and 3.153 habitat/day-night ms/tick. The 50-Villager fixture
recorded bounded cached scans and storage/path work, with no phase above the 6.864 ms/tick peak.
Absolute timing remains host-load-dependent.

### Hunger-Satisfaction Breeding Focused Rerun

The global breeding dispatcher adds one constant-time check every 20 ticks to supported breedable
animals and performs a cached mate scan only after five uninterrupted satisfied minutes. Strider
and Nautilus also received their first positive hunger profiles, bringing registration from 75 to
77 species. Following `testing_strategy.md`, the 2026-08-04 validation used the synthetic 256-mob
work-budget test, representative `retold:mob_tps_cow`, and the two new profile selectors rather than
repeating 74 unaffected species.

The final mixed workload, rerun after readiness changed to persisted accumulated loaded ticks,
passed every budget assertion with all 256 animals alive and averaged 17.082 ms/server tick. The
final Cow phases averaged 3.996, 3.662, 2.946, 5.849, and 3.389 ms/tick. Strider
averaged 4.554, 4.551, 5.215, 3.546, and 2.130 ms/tick; Nautilus averaged 5.978, 5.127, 4.515,
3.580, and 2.327 ms/tick. All 15 phases remained below 50 ms/tick. The latest complete baseline
therefore remains 75/75 tests and 375/375 phases; the complete expanded 77/77 and 385/385 matrix
has not been run.

### Loaded Starvation Focused Rerun

Loaded starvation adds one constant-time critical-hunger check to the existing metabolism update
in each of the three current hunger owners: ordinary `PathfinderMob`s, Bats, and Villagers. The
2026-08-04 validation therefore reran only `retold:mob_tps_cow`, `retold:mob_tps_bat`, and
`retold:mob_tps_villager`; it did not repeat the 74 profiles that use no distinct hunger entry path.

All 15 phases passed below 50 ms/tick. Cow averaged 4.375, 3.988, 2.556, 6.852, and 3.069 ms/tick;
Bat averaged 5.892, 2.566, 10.703, 11.856, and 9.279 ms/tick; Villager averaged 10.129, 5.932,
5.905, 7.806, and 3.776 ms/tick. The 11.856 ms/tick Bat danger/social phase was the overall peak.
The complete 77-profile matrix remains unnecessary for this change because dispatch cadence,
scans, paths, caches, work budgets, allocations, and profile loading were unchanged.

### Strider Lava Sustenance Focused Rerun

Passive lava sustenance adds a constant-time local fluid check to the existing Strider species
dispatch and suppresses ordinary food search while the Strider is already sustained. Only
`retold:mob_tps_strider` was rerun on 2026-08-04; the other 76 species and shared performance
primitives were unchanged.

All five 50-Strider phases passed below 50 ms/tick: 6.328 idle/rest, 5.964
dropped-food/forage, 6.771 hunt/targeting, 3.662 danger/social, and 2.368 habitat/day-night. The
6.771 ms/tick hunt/targeting phase was the peak. The complete matrix was not selected because the
focused natural-food and survival tests cover the transaction and only this species-local tick path
changed.

## Results

The table below records the original clean baseline described above; later rerun summaries are
reported separately because wall-clock values are machine- and host-load-dependent.

Values are average milliseconds per server tick, sorted by each mob's most expensive phase.

| Mob | Profile | Idle/rest | Food/forage | Hunt/target | Danger/social | Habitat/day-night | Peak |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `minecraft:bat` | `bat_colony` | 1.218 | 1.754 | 11.833 | 10.900 | 11.474 | 11.833 |
| `minecraft:frog` | `amphibian_forager` | 2.526 | 2.146 | 2.791 | 5.670 | 5.873 | 5.873 |
| `minecraft:axolotl` | `aquatic_helper_predator` | 2.503 | 2.378 | 2.284 | 5.528 | 5.441 | 5.528 |
| `minecraft:snow_golem` | `territory_guard` | 4.945 | 2.350 | 3.856 | 2.622 | 1.622 | 4.945 |
| `minecraft:hoglin` | `nether_hungry` | 2.347 | 3.022 | 4.233 | 3.906 | 3.718 | 4.233 |
| `minecraft:wither` | `apex_or_boss` | 1.800 | 1.287 | 3.833 | 2.105 | 1.527 | 3.833 |
| `minecraft:creaking` | `special_vanilla` | 3.664 | 1.416 | 1.555 | 1.733 | 1.220 | 3.664 |
| `minecraft:spider` | `hungry_swarm_predator` | 1.165 | 1.454 | 2.283 | 3.577 | 1.569 | 3.577 |
| `minecraft:sniffer` | `sniffer_forager` | 2.318 | 1.666 | 3.340 | 2.982 | 2.721 | 3.340 |
| `minecraft:elder_guardian` | `aquatic_territory_guard` | 2.053 | 3.197 | 2.100 | 2.290 | 1.827 | 3.197 |
| `minecraft:ghast` | `ghast_artillery` | 2.564 | 2.521 | 2.698 | 2.052 | 2.939 | 2.939 |
| `minecraft:camel` | `hungry_grazer` | 2.796 | 2.569 | 2.074 | 2.772 | 2.201 | 2.796 |
| `minecraft:ravager` | `illager_raider` | 2.154 | 1.607 | 2.765 | 2.482 | 2.747 | 2.765 |
| `minecraft:panda` | `panda_bamboo` | 1.907 | 1.750 | 2.756 | 1.989 | 1.536 | 2.756 |
| `minecraft:piglin` | `nether_hungry` | 1.834 | 2.489 | 2.738 | 2.731 | 2.335 | 2.738 |
| `minecraft:wolf` | `pack_predator` | 0.904 | 1.057 | 1.275 | 2.491 | 1.737 | 2.491 |
| `minecraft:dolphin` | `aquatic_predator` | 2.333 | 2.335 | 2.210 | 2.043 | 1.311 | 2.335 |
| `minecraft:turtle` | `turtle_beach` | 1.311 | 1.104 | 1.250 | 2.311 | 1.813 | 2.311 |
| `minecraft:drowned` | `undead_hungry` | 1.446 | 1.599 | 1.472 | 2.285 | 1.839 | 2.285 |
| `minecraft:armadillo` | `armadillo_defensive` | 1.799 | 2.232 | 2.030 | 2.062 | 1.337 | 2.232 |
| `minecraft:rabbit` | `small_forager` | 2.046 | 2.163 | 2.204 | 2.009 | 1.182 | 2.204 |
| `minecraft:zoglin` | `zoglin_rampager` | 1.862 | 1.915 | 1.853 | 2.133 | 1.557 | 2.133 |
| `minecraft:horse` | `hungry_grazer` | 0.912 | 1.213 | 2.116 | 1.595 | 1.277 | 2.116 |
| `minecraft:magma_cube` | `slime_hungry` | 1.254 | 1.627 | 2.100 | 1.753 | 1.458 | 2.100 |
| `minecraft:slime` | `slime_hungry` | 1.707 | 1.880 | 1.775 | 1.985 | 1.889 | 1.985 |
| `minecraft:pillager` | `illager_raider` | 0.719 | 0.960 | 1.972 | 1.199 | 1.652 | 1.972 |
| `minecraft:fox` | `solo_opportunist` | 1.076 | 1.391 | 1.451 | 1.855 | 1.721 | 1.855 |
| `minecraft:mule` | `hungry_grazer` | 0.854 | 1.367 | 1.394 | 1.566 | 1.851 | 1.851 |
| `minecraft:goat` | `hungry_grazer` | 1.714 | 1.729 | 1.803 | 1.784 | 1.811 | 1.811 |
| `minecraft:bee` | `hive_colony` | 1.102 | 1.009 | 1.794 | 1.442 | 0.913 | 1.794 |
| `minecraft:vindicator` | `illager_raider` | 0.835 | 0.891 | 1.776 | 1.447 | 1.321 | 1.776 |
| `minecraft:illusioner` | `illager_raider` | 1.195 | 1.582 | 1.758 | 1.041 | 1.502 | 1.758 |
| `minecraft:ocelot` | `solo_opportunist` | 0.754 | 1.102 | 1.190 | 1.720 | 1.331 | 1.720 |
| `minecraft:bogged` | `undead_tolerant` | 0.988 | 1.480 | 1.278 | 1.445 | 1.715 | 1.715 |
| `minecraft:zombified_piglin` | `undead_hungry` | 0.763 | 1.047 | 1.005 | 1.663 | 1.141 | 1.663 |
| `minecraft:guardian` | `aquatic_territory_guard` | 1.146 | 1.637 | 1.211 | 1.563 | 1.012 | 1.637 |
| `minecraft:husk` | `undead_hungry` | 0.871 | 1.169 | 0.998 | 1.616 | 1.193 | 1.616 |
| `minecraft:evoker` | `commander_support` | 0.727 | 0.694 | 1.594 | 1.215 | 0.703 | 1.594 |
| `minecraft:cave_spider` | `hungry_swarm_predator` | 0.929 | 1.048 | 1.401 | 1.572 | 1.000 | 1.572 |
| `minecraft:endermite` | `small_arthropod_swarm` | 0.991 | 1.338 | 1.269 | 1.564 | 0.976 | 1.564 |
| `minecraft:cat` | `solo_opportunist` | 0.952 | 1.046 | 1.295 | 1.562 | 1.238 | 1.562 |
| `minecraft:zombie` | `undead_hungry` | 0.899 | 1.020 | 1.061 | 1.558 | 1.341 | 1.558 |
| `minecraft:polar_bear` | `protective_neutral` | 0.844 | 0.991 | 1.160 | 1.552 | 0.991 | 1.552 |
| `minecraft:donkey` | `hungry_grazer` | 1.126 | 1.255 | 1.478 | 1.537 | 1.455 | 1.537 |
| `minecraft:iron_golem` | `territory_guard` | 0.924 | 1.136 | 1.352 | 1.533 | 1.099 | 1.533 |
| `minecraft:witch` | `commander_support` | 0.703 | 0.879 | 1.500 | 1.075 | 0.741 | 1.500 |
| `minecraft:breeze` | `special_vanilla` | 1.123 | 1.088 | 1.234 | 1.489 | 1.012 | 1.489 |
| `minecraft:creeper` | `special_vanilla` | 0.783 | 1.035 | 1.064 | 1.469 | 0.760 | 1.469 |
| `minecraft:zombie_villager` | `undead_hungry` | 1.043 | 1.028 | 1.081 | 1.444 | 1.231 | 1.444 |
| `minecraft:sheep` | `hungry_grazer` | 0.745 | 1.074 | 1.115 | 1.443 | 1.038 | 1.443 |
| `minecraft:silverfish` | `small_arthropod_swarm` | 0.925 | 0.868 | 1.185 | 1.425 | 1.092 | 1.425 |
| `minecraft:warden` | `apex_or_boss` | 1.315 | 1.239 | 1.162 | 1.420 | 1.031 | 1.420 |
| `minecraft:trader_llama` | `hungry_grazer` | 0.844 | 0.989 | 1.154 | 1.400 | 1.111 | 1.400 |
| `minecraft:skeleton` | `undead_tolerant` | 1.380 | 1.263 | 1.118 | 1.348 | 0.958 | 1.380 |
| `minecraft:wither_skeleton` | `territory_guard` | 1.116 | 0.943 | 1.362 | 1.248 | 0.917 | 1.362 |
| `minecraft:cow` | `hungry_grazer` | 0.775 | 1.134 | 1.320 | 1.283 | 1.063 | 1.320 |
| `minecraft:llama` | `hungry_grazer` | 1.199 | 1.083 | 0.883 | 1.309 | 0.963 | 1.309 |
| `minecraft:piglin_brute` | `territory_guard` | 1.206 | 1.173 | 1.284 | 1.305 | 1.120 | 1.305 |
| `minecraft:vex` | `illager_raider` | 0.436 | 0.453 | 1.304 | 0.516 | 0.265 | 1.304 |
| `minecraft:pig` | `small_forager` | 0.678 | 1.024 | 0.922 | 1.277 | 1.027 | 1.277 |
| `minecraft:mooshroom` | `hungry_grazer` | 0.819 | 1.171 | 0.992 | 1.266 | 0.960 | 1.266 |
| `minecraft:chicken` | `small_forager` | 0.692 | 0.896 | 0.862 | 1.225 | 1.023 | 1.225 |
| `minecraft:phantom` | `phantom_stalker` | 0.676 | 0.714 | 0.954 | 1.179 | 0.742 | 1.179 |
| `minecraft:stray` | `undead_tolerant` | 1.130 | 1.032 | 1.015 | 1.148 | 0.797 | 1.148 |
| `minecraft:shulker` | `territory_guard` | 1.063 | 0.929 | 1.071 | 1.108 | 0.751 | 1.108 |
| `minecraft:enderman` | `special_vanilla` | 0.812 | 0.842 | 0.918 | 1.089 | 0.870 | 1.089 |
| `minecraft:blaze` | `territory_guard` | 0.999 | 0.994 | 0.941 | 1.071 | 0.894 | 1.071 |
| `minecraft:ender_dragon` | `apex_or_boss` | 0.325 | 0.205 | 0.446 | 0.498 | 0.193 | 0.498 |
