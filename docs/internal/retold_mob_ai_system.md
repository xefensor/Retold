# Retold Mob AI System

> Internal documentation. This file is meant for human developers and future AI coding agents. Sections named "Design rule", "Adding A New Mob Behavior", "Validation Checklist", and "Refactor Rules" are also implementation guidance for AI-assisted work.

This document describes the current Retold mob AI design and the technical structure that implements it.
It is meant to be the main reference before adding behavior, debugging behavior, or doing more performance work.

Whole-mod architecture reference: [`retold_mod_system.md`](retold_mod_system.md).
Design risks: [`retold_design_risks.md`](retold_design_risks.md).
Known issues: [`retold_issues.md`](retold_issues.md).

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
| `HIVE_COLONY` | bees | hive and flower loop with Retold state awareness |
| `NETHER_HUNGRY` | piglins, hoglins | nether hunger behavior where relevant |
| `UNDEAD_HUNGRY` | zombies, husks, drowned, zombified piglins | horde pressure |
| `UNDEAD_TOLERANT` | skeletons, strays, bogged | ranged undead pressure |
| `TERRITORY_GUARD` | iron golems, brutes, blazes, shulkers, wither skeletons | guard post/zone behavior |
| `COMMANDER_SUPPORT` | evokers, witches | support and pressure from behind allies |
| `ILLAGER_RAIDER` | pillagers, vindicators, ravagers, vexes, illusioners | illager roaming and territory behavior |
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

- Nether Remnants: piglins, piglin brutes, blazes
- Illagers: pillagers, vindicators, evokers, illusioners, ravagers, vexes
- Witch: loose illager ally/support, not full member
- Village defenders: iron golems, snow golems, dynamically defending tamed wolves
- Undead: not one cozy social faction; zombies horde, skeletons tolerate undead, wither skeletons guard fortress
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

Territory mob state is separate and currently lives in `RetoldTerritoryMobStates` as runtime weak-map state for warning posture/debug values. Per-player suspicion/reputation is owned by `RetoldTerritoryReputation`.

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
- `RetoldHeldFoodConsumptionEvents`
- `RetoldForageBlockSearch`
- `RetoldBlockTargetSearch`
- `RetoldFeedingAnimations`
- `RetoldRangeForage`

Hunger should not override territory guard purpose, special boss behavior, or urgent flee/combat behavior.

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
- profile-specific home events

Home/range types:

- `WOLF_DEN`
- `DOLPHIN_POD_RANGE`
- `HERD_RANGE`
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

Important design rule:

Herd animals have ranges, not dens. Guards have territory purpose, not cozy home life.

## Flee, Regroup, Hunt, And Combat

Flee:

- `RetoldControlledFleeEvents`
- prey flees from serious threats
- fish and land prey are handled
- panic can spread through nearby herd-like mobs

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

Combat target ownership:

- `RetoldAiTargets`
- `RetoldCombatTargets`
- `RetoldFactionTargetMemory`
- `RetoldTargetSource`

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
2. checks if the entity is a `PathfinderMob`
3. gets the cached profile once from `RetoldAiTickContext`
4. routes only relevant behavior handlers for that profile
5. applies dispatcher-level cadence gates before calling handlers

Examples:

- grazers route to flee, recovery/repair, regroup, herd range
- small foragers route to flee, recovery/repair, regroup, small home
- pack predators route to predator search, pack hunting, pack home
- solo opportunists route to predator search, solo home, held food
- territory guards route to guard post logic
- illager raiders route to roaming and territory guard logic

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

- profile/path lookup cache avoids repeated registry/profile resolution
- scan cache reuses nearby entity queries by mob, position, shared bucket, and radius bucket
- sight cache avoids repeated line-of-sight raycasts
- block search cache avoids repeated forage/block scans

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
| Faction assist | Nearby allies can help only when target gating allows it. | Assist does not bypass warning-stage players in territory. |
| Territory warning | Nether Remnants and Illagers warn before attack in configured structures. | Bastion, fortress, outpost, and mansion all show warning progression before attack. |
| Retaliation | Directly attacking a guard can still trigger immediate retaliation. | Player hit on guard bypasses warning only for retaliation, not passive sight. |
| Debug | Debug output explains why a mob is or is not controlled. | `get`, `nearby`, `toggle overlay`, `targets`, `warning`, `home`, `guardpost`, and `pack` give actionable state. |

### Territory Factions

| Faction | Members | Territory | Expected player behavior |
| --- | --- | --- | --- |
| Nether Remnants | piglin, piglin brute, blaze | bastion remnant, fortress in Nether | Notice player, warn, posture, move into warning positions, escalate by suspicion, attack only at `ATTACK` or retaliation. |
| Illagers | pillager, vindicator, evoker, illusioner, ravager, vex | pillager outpost, mansion | Same warning/reputation rules as Nether Remnants. |
| Illager loose ally | witch | none as full member | Can align with Illagers for support behavior, but is not a full territory faction member. |

### Managed Behavior Profiles

| Profile | Mobs | Primary behavior to validate |
| --- | --- | --- |
| Hungry grazer | cow, mooshroom, sheep, goat, horse, donkey, mule, llama, trader llama, camel | Hunger, grazing/eating, home range, herd panic/flee. |
| Small forager | pig, chicken, rabbit | Hunger, foraging, home return, predator flee. |
| Pack predator | wolf | Pack creation, pack hunt/search/return, den defense, target ownership. |
| Solo opportunist | fox, cat, ocelot | Solo home behavior, opportunistic hunting, flee/return. |
| Aquatic predator | dolphin | Pod behavior, aquatic targeting, controlled combat. |
| Hungry swarm predator | spider, cave spider | Hunger, swarm hunting/scavenging, target ownership. |
| Hive colony | bee | Hive/home behavior, defense, controlled targeting. |
| Nether hungry | piglin, hoglin | Hunger behavior plus faction/territory interactions for piglins. |
| Undead hungry | zombie, zombie villager, husk, drowned, zombified piglin | Hunger/horde behavior, faction targeting, undead tolerance. |
| Undead tolerant | skeleton, stray, bogged | Ranged behavior, faction targeting, no hunger loop. |
| Phantom stalker | phantom | Stalking behavior and owned attack target. |
| Ghast artillery | ghast | Artillery targeting and faction exclusions. |
| Zoglin rampager | zoglin | Rampage targeting and owned attack target. |
| Slime hungry | slime, magma cube | Hunger/faction behavior. |
| Small arthropod swarm | silverfish, endermite | Swarm behavior and faction targeting. |
| Protective neutral | polar bear | Defensive targeting, no invalid player retention. |
| Armadillo defensive | armadillo | Defensive/flee behavior. |
| Panda bamboo | panda | Bamboo hunger/forage behavior. |
| Sniffer forager | sniffer | Foraging and home/range behavior. |
| Turtle beach | turtle | Beach/home behavior. |
| Amphibian forager | frog | Foraging and controlled prey targeting. |
| Aquatic helper predator | axolotl | Helper targeting and controlled combat. |
| Aquatic territory guard | guardian, elder guardian | Guard behavior, special elder guardian behavior, target ownership. |
| Territory guard | iron golem, snow golem, piglin brute, blaze, shulker, wither skeleton | Guard post return/leash behavior and faction/territory interactions where applicable. |
| Commander support | evoker, witch | Support behavior, Illager coordination, target ownership. |
| Illager raider | pillager, vindicator, ravager, vex, illusioner | Illager faction behavior and territory warning where applicable. |
| Special vanilla | creeper, enderman, breeze, creaking | Mostly vanilla behavior with Retold target safety protections. |
| Apex or boss | warden, wither, ender dragon | Mostly exempt from managed AI; keep special/boss behavior intact. |

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

Animal life:

- grazers create/use herd range
- small foragers use home/range behavior
- prey flee and regroup
- predators hunt and return
- wolves keep pack/den behavior
- dolphins keep pod behavior

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
