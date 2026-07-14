# Minecraft Retold Mod System

> AI-generated documentation. This file is meant for human maintainers and future AI coding agents. Sections named "Design rule", "Performance rules", "Adding New Features", and "Open Technical Watchpoints" are also implementation guidance for AI-assisted work.

AI agents: read [`README.md`](README.md) before implementing. Ask design/implementation questions before coding when behavior is ambiguous, and update the generated docs when a feature is completed.

This document describes the current Minecraft Retold mod as a whole: what the mod is trying to do, how the major systems fit together, and which code/data files own each system.

For the dedicated mob AI design and technical reference, see [`retold_mob_ai_system.md`](retold_mob_ai_system.md).

For the original-design versus current-implementation tracker, see [`design_implementation_status.md`](design_implementation_status.md).

For active priorities and maintainer direction, see [`retold_roadmap.md`](retold_roadmap.md).

For design risks, confirmed bugs, and in-game verification, see [`retold_design_risks.md`](retold_design_risks.md), [`retold_known_issues.md`](retold_known_issues.md), and [`retold_testing_checklist.md`](retold_testing_checklist.md).

## Mod Shape

Minecraft Retold is not one isolated feature. It is a progression, world, AI, and recipe overhaul built around staged world changes.

Current high-level model:

```text
world stage + dimension rules + structure timing + recipe knowledge + mob AI + client sync = Retold gameplay
```

The core ideas are:

- world progression moves through stages
- vanilla End access changes after progression
- the Aender dimension replaces late End access
- some structures and spawns are delayed until later stages
- recipe knowledge is earned or taught instead of being fully automatic
- villager teaching is a data-driven learning route
- mobs use Retold faction/territory/behavior systems
- torch weather and extinguished torch blocks add environmental pressure
- client visuals are synchronized for stage, sky, teaching UI, and chronolith beams

## Design Direction Source

Active design direction lives in [`retold_roadmap.md`](retold_roadmap.md). This architecture doc focuses on how the current code is organized and which subsystem owns each behavior.

## Entry Point

Main class:

- `Retold`

Startup responsibilities:

- register blocks/items
- register worldgen registries
- register chunk attachments
- register Aender chunk generator
- register entity types
- register game rules
- register network payloads
- register entity attributes/spawn placement
- register client-only hooks
- register gameplay event handlers
- register villager teaching reload listener

The main event registration is intentionally explicit. When adding a new system, add its event class here unless it is client-only or registered through a subsystem.

## Package Map

| Package | Purpose |
| --- | --- |
| `aender` | Aender dimension access, keys, generator registration |
| `aender/entity` | Aender-specific entity classes |
| `aender/generation` | Aender floating island terrain and volatility |
| `aender/stability` | Aender stabilizer chunks, regeneration, forcefield visuals |
| `behavior` | Retold mob AI system |
| `block` | custom blocks and block interaction behavior |
| `chronolith` | Aender chronolith time-acceleration system |
| `client` | client event registration and UI helpers |
| `client/render` | entity/beam/enderman rendering hooks |
| `client/sky` | End/Aender sky seed and generated sky texture |
| `client/stage` | client-side current world stage |
| `combat` | Retold-owned target source/memory helpers |
| `command` | `/retold` command tree |
| `effect` | ritual visual/audio effects |
| `enderman` | enderman behavior changes |
| `event` | gameplay event systems and progression glue |
| `faction` | faction identities and relationships |
| `item` | custom item behavior, currently including element item logic |
| `mixin` | vanilla behavior hooks and accessors |
| `network` | custom payload registration and handlers |
| `recipe` | known recipe storage and recipe unlock control |
| `registry` | blocks, items, entities, tags, game rules |
| `sky` | saved End sky seed data |
| `stage` | world stage saved data and runtime sync |
| `territory` | territory warning/reputation system |
| `undead` | undead helper behavior |
| `villager` | villager teaching system |
| `worldgen` | worldgen registry and structure tags |
| `worldgen/delayed` | stage-delayed structure generation and mob suppression |

## World Stage System

World stages are the backbone of Retold progression.

Technical owners:

- `RetoldWorldStage`
- `RetoldStageManager`
- `RetoldWorldData`
- `RetoldStageRuntime`
- `RetoldStageRuntimeEvents`
- `RetoldStageSyncPayload`
- `RetoldClientStage`

Saved data:

- stage id
- offered element mask
- dragon egg position

Stage behavior:

| Stage | Meaning |
| --- | --- |
| Stage 1 | early world state |
| Stage 2 | dragon killed, dragon egg ritual becomes relevant |
| Stage 3 | Aender access and later-world changes |

Stage transitions:

- Stage 1 -> Stage 2 happens after the Ender Dragon has previously been killed.
- Stage 2 -> Stage 3 happens when the dragon egg ritual completes.
- Setting Stage 2 queues known delayed structures for retrogen.
- Setting Stage 3 ejects players from the vanilla End and redirects later End portal access to Aender.

Developer command:

```mcfunction
/retold stage get
/retold stage set <1-3>
```

Design rule:

Stage changes should go through `RetoldStageManager.setStage`, not direct `RetoldWorldData` writes, because the manager performs sync and transition side effects.

## End Progression And Dragon Egg Ritual

Technical owner:

- `RetoldEndProgressionEvents`
- `RetoldRitualEffects`
- `RetoldElementType`
- `WaterElementItem`
- `AirElementItem`

Current ritual model:

- Dragon kill advances the world to Stage 2.
- In Stage 2, the dragon egg accepts element items.
- `water_element` and `air_element` are currently implemented element items.
- Element acquisition order should be free; no element path should require being completed first unless the maintainer changes the design.
- Offered elements are saved in `RetoldWorldData`.
- The dragon egg crack overlay reflects offered element count.
- When all currently required element values are offered, currently Water and Air, the egg is removed and Stage 3 starts. Fire and Earth can be added to the requirement when their paths exist.

Current limitation:

- `water_element` has the most complete challenge-to-reward path.
- `air_element` now has a WIP Air Temple/Gale Core acquisition path: locate the Air Temple in peak biomes, fight through the wind/tower encounter, defeat the Gale Core, and obtain the Air Element drop. The encounter is not final and still needs tuning/testing.
- Fire and earth still need items and acquisition paths.
- Because the temporary Nether Star shortcut has been removed, normal survival reaches Stage 3 through the currently implemented Water and Air element paths.

End crystal behavior:

- End crystal use around the exit portal is customized after Stage 1.
- Respawn/portal logic is stage-aware.

Design rule:

The dragon egg is the progression lock between the vanilla End phase and the Aender phase.

## Aender Dimension

Aender is the late-stage dimension.

Technical owners:

- `RetoldAenderDimensions`
- `RetoldAenderAccess`
- `RetoldAenderRegistries`
- `AenderChunkGenerator`
- `AenderIslandSampler`
- `AenderVolatility`
- `AenderWorldTickEvents`
- `AenderRealityTickEvents`
- `AenderChunkEvents`

Data files:

- `data/retold/dimension/aender.json`
- `data/retold/dimension_type/aender.json`
- `data/retold/worldgen/biome/aender.json`

Access behavior:

- Stage 3 redirects Overworld End portal use into `retold:aender`.
- Players in vanilla End are ejected when Stage 3 begins.
- Aender entry position is fixed near `0.5, 8.0, 0.5`.
- Vanilla End remains technically available through commands by design, so old player builds are not permanently inaccessible.

Generation behavior:

- Aender uses a custom chunk generator.
- Terrain is floating-island based.
- Chunks are generated from sampled island data.
- Decoration includes grass, plants, lakes, underside spurs/growth, and Aender block palette.
- `AenderVolatility` tracks generated/current chunk state for regeneration and stability behavior.

Design rule:

Aender owns the late-End experience. Vanilla End should not remain the normal post-Stage-3 destination.

## Aender Stability

Aender stability controls safe/stable chunks around stabilizers.

Technical owners:

- `AenderStabilityData`
- `AenderStabilizerEvents`
- `AenderRealityTickEvents`
- `AenderWorldTickEvents`
- `AenderChunkEvents`
- `AenderVolatility`

Block:

- `aender_stabilizer`

Behavior:

- Placing a stabilizer marks a 3x3 chunk halo as stable.
- Breaking a stabilizer removes that halo count.
- Stable chunk counts are saved in `AenderStabilityData`.
- Loaded chunks around a removed stabilizer are marked current before removing stability.
- Server tick renders a merged outer forcefield around stable regions for players.

Design rule:

Stability should be chunk-based and saved. Visual forcefields are feedback for stability boundaries, not the source of truth.

## Chronolith

The Aender chronolith is a development/time-control block.

Technical owners:

- `AenderChronolithBlock`
- `ChronolithController`
- `ChronolithChannel`
- `ChronolithFeedback`
- `ChronolithTuning`
- `ChronolithStopReason`
- `AenderChronolithEvents`
- `RetoldChronolithBeamPayload`
- `RetoldChronolithBeamClient`

Block:

- `aender_chronolith`

Behavior:

- Only works in the Overworld.
- One active block per dimension is allowed.
- A player toggles a channel on/off.
- Active channel advances time using server commands.
- XP is drained as time is advanced unless the player is creative.
- Channel stops when player leaves, moves too far, sneaks/manual stops, runs out of XP, block is removed, server stops, or another channel replaces it.
- Beam sync is sent to clients.

Design rule:

Chronolith state is runtime-only and must stop cleanly on logout/server stop/block removal.

## Blocks And Items

Technical owner:

- `RetoldBlocks`

Registered items/blocks include:

- `water_element`
- `aender_grass_block`
- `aender_soil`
- `aender_stone`
- `aender_log`
- `aender_leaves`
- `aender_stabilizer`
- `aender_chronolith`
- extinguished torch variants

Extinguished torch variants include:

- normal torch
- wall torch
- soul torch
- soul wall torch
- copper torch
- copper wall torch

Assets:

- `assets/retold/blockstates`
- `assets/retold/models`
- `assets/retold/textures`
- `assets/retold/lang/en_us.json`

Data:

- block loot tables under `data/retold/loot_table/blocks`
- item tag `data/retold/tags/item/torch_igniters.json`

Design rule:

Use `RetoldBlocks` for registered block/item references instead of duplicating identifiers in code.

## Torch Weather System

Rain can extinguish tracked torches.

Technical owners:

- `TorchWeatherEvents`
- `ExtinguishedTorchBlock`
- `ExtinguishedWallTorchBlock`
- `RetoldTags.TORCH_IGNITERS`

Behavior:

- Chunks are indexed gradually for lit torches.
- Lit torches are tracked by dimension and chunk.
- Rain checks tracked torches periodically.
- Some lit torches convert to extinguished variants.
- Extinguished torches can be relit with firestarter actions or items in `retold:torch_igniters`.
- Aender is excluded.

Performance rules:

- Chunk indexing is deferred and capped per tick.
- Tracked torches are removed on chunk unload.
- Runtime maps are cleared on server stop.

## Game Rules And Sleep

Technical owner:

- `RetoldGameRules`
- `RetoldSleepEvents`
- `BedBlockMixin`
- `ServerLevelSleepMixin`

Game rule:

- `retold:do_bed_night_skipping`

Behavior:

- The rule controls whether normal bed night skipping is allowed.
- Default is `false`.
- When night skipping is disabled, players can still lie down in valid Overworld-style beds during daytime.
- Daytime bed rest still respects normal bed validity checks such as obstruction, distance, and nearby rest-preventing monsters.
- `ServerLevelSleepMixin` prevents bed sleepers from advancing world time while the rule is disabled.
- `BedBlockMixin` only keeps Retold's Aender bed explosion behavior.

Design rule:

Sleep behavior should respect the game rule, not hard-code night skipping assumptions.

## Recipe Knowledge System

Retold tracks recipe knowledge separately from vanilla automatic unlock assumptions.

Technical owners:

- `RetoldKnownRecipeData`
- `RetoldRecipeBookEvents`
- `RetoldRecipeUnlockContext`
- `RetoldRecipeResultHelper`
- `RetoldCookingRecipeSiblingHelper`
- `ServerRecipeBookMixin`
- `AdvancementVisibilityEvaluatorMixin`
- `AbstractFurnaceBlockEntityMixin`

Saved data:

- per-player known recipe ids

Behavior:

- Crafting a recipe marks it known.
- Stonecutting and smithing are tracked by result where direct input matching is not enough.
- Cooking siblings can be unlocked together.
- Internal recipe unlocks are wrapped with `RetoldRecipeUnlockContext`.
- Vanilla recipe book behavior is intercepted through mixins.
- Many vanilla recipe advancements are included as data resources with reward/visibility changes.

Design rule:

Use `RetoldRecipeBookEvents.markKnownAndUnlockRecipe` or related helpers when teaching/unlocking recipes. Do not directly add recipe book entries from feature code.

## Villager Teaching

Villager teaching lets players learn recipes from villagers.

Technical owners:

- `RetoldVillagerTeaching`
- `RetoldVillagerTeachingEntry`
- `RetoldVillagerTeachingReloadListener`
- `RetoldTeachingSlotMenu`
- `RetoldTeachingGui`
- `RetoldTeachingPreviewClient`
- `RetoldTeachingPreviewScreen`
- `MerchantMenuAccessor`
- `MerchantMenuTeachingSlotMixin`
- `MerchantScreenMixin`
- `VillagerInvoker`

Data:

- `data/retold/villager_teaching/*.json`

Network:

- `RetoldLearnRecipePayload`
- `RetoldRequestTeachingPreviewPayload`
- `RetoldTeachingPreviewPayload`

Behavior:

- Player opens villager trading UI.
- Custom teaching slot shows an item whose recipe the player wants to learn.
- Server checks the villager profession teaching entry.
- Recipe can be matched by configured id/result.
- Player pays emeralds.
- Recipe is marked known and unlocked.
- Villager gets XP reward.
- Preview/status/cost are synced to client.

Design rule:

Teaching data is resource-driven by profession. Add or tune teaching through `villager_teaching` JSON before changing code.

## Worldgen And Delayed Structures

Retold delays some structures until later world stages.

Technical owners:

- `RetoldWorldgenRegistries`
- `RetoldStructureTags`
- `RetoldCentralEndIslandMaskDensityFunction`
- `RetoldWorldSpawnCache`
- `RetoldDelayedStructureRetrogen`
- `RetoldDelayedStructureHelper`
- `RetoldDelayedStructureIds`
- `RetoldDelayedStructureMobBlocker`
- `RetoldChunkStructureData`
- `RetoldAttachments`
- `RetoldChunkEditEvents`
- `RetoldPatrolStageEvents`
- `RetoldRetrogenDropBlocker`
- `RetoldClientChunkTracker`
- `DelayedStructurePlacementMixin`
- `NoVillageNearWorldSpawnMixin`

Data:

- `data/retold/tags/worldgen/structure/delayed_until_stage_2.json`
- `data/retold/neoforge/biome_modifier/stage3_piglins_nether.json`
- `data/retold/neoforge/structure_modifier/stage3_piglins_structures.json`

Behavior:

- Stage 1 can suppress/delay configured structures.
- Deferred chunks are remembered.
- Stage 2 queues known deferred chunks.
- Retrogen processes a small number of chunks per tick.
- Failed retrogen attempts can retry later.
- Structure mob spawns can be suppressed while a structure is delayed.
- Some spawn/structure behavior changes at Stage 3.

Performance rules:

- Retrogen is queued and capped per tick.
- Chunk data is stored in attachments.
- Deferred work is retried instead of forcing synchronous generation.

Design rule:

Delayed structures must be tracked through chunk attachment data so loaded worlds can recover after stage changes.

## Mob AI, Factions, And Territory

The full AI reference is in [`retold_mob_ai_system.md`](retold_mob_ai_system.md).

Primary packages:

- `behavior`
- `combat`
- `faction`
- `territory`

High-level systems:

- profile-based mob life behavior
- faction relationships
- territory warning and reputation
- target ownership
- invalid player target cleanup
- AI performance scheduling, caches, LOD, and budgets

Important design rule:

Mob behavior should be routed through profiles, Retold control ownership, and target helpers. Avoid new always-on entity tick subscribers for normal AI.

## Enderman Changes

Technical owners:

- `RetoldEndermanEvents`
- `RetoldEndermanBehavior`
- `EndermanRendererMixin`
- `EndermanParticleMixin`
- `LivingEntityTeleportParticleMixin`
- `RetoldEndermanEyesLayer`
- `RetoldEndermanParticleColor`

Behavior:

- In Stage 2 and Stage 3, Endermen have eye-contact aggro/freeze goals removed.
- Client assets/rendering provide custom Enderman appearance and particle behavior.

Design rule:

Enderman behavior changes are stage-gated. Do not globally remove vanilla Enderman behavior outside the intended stages.

## Undead, Piglins, Golems, And Faction Events

Technical owners:

- `RetoldUndeadEvents`
- `RetoldUndead`
- `RetoldUndeadCleansing`
- `RetoldUndeadSunFear`
- `RetoldPiglinEvents`
- `RetoldGolemEvents`
- `RetoldFactionCombatEvents`
- `RetoldFactionAssistEvents`

Behavior:

- Undead helper functions and event behavior support Retold hostility/fear rules.
- Piglins are integrated with Retold stage/faction/territory behavior.
- Golems integrate with defender behavior.
- Faction combat and assist are centralized so target ownership and warning rules are not bypassed.

Design rule:

Faction assist must respect territory warning and Retold target guards.

## Ocean Monument And Guardians

Technical owners:

- `RetoldElderGuardianEvents`
- `RetoldElderGuardianBoss`
- `RetoldElderGuardianSentinel`
- `RetoldGuardianMiningPressure`
- `RetoldGuardianAlertController`
- `RetoldGuardianDefenseAssist`
- `RetoldOceanMonumentSupport`
- `ElderGuardianMixin`
- `ElderGuardianInvulnerableHitMixin`

Behavior:

- Elder guardians become persistent monument sentinels.
- Duplicate elder guardian spawns in the same monument are blocked.
- Elder guardians in water can block incoming damage.
- Blocked hits apply feedback, knockback/bounce, and mining fatigue.
- Elder guardians guarantee a `water_element` drop if one is not already dropping.
- Guardians pressure players mining protected monument blocks.
- Guardian beam pressure can block mining and alert nearby guardians.

Design rule:

Monument defense is a purpose system, not ordinary animal AI. Guardian/elder guardian special behavior should be preserved.

## Aender Eye Entity

Technical owners:

- `AenderEye`
- `RetoldEntityTypes`
- `RetoldEntityEvents`
- `RetoldAenderEyeRenderer`

Entity:

- `retold:aender_eye`

Behavior:

- Custom ambient entity type with registered attributes/spawn placement.
- Client renderer handles visual presentation.

Design rule:

Register entity attributes/spawn placements in `RetoldEntityEvents`, not in random feature code.

## Client Systems

Technical owners:

- `RetoldClientEvents`
- `RetoldClientStage`
- `RetoldTeachingPreviewClient`
- `RetoldTeachingPreviewScreen`
- `RetoldChronolithBeamClient`
- `RetoldClientEndSky`
- `RetoldEndSkyPatcher`
- `RetoldGeneratedEndSkyTexture`
- `RetoldAenderEyeRenderer`
- `RetoldEndermanEyesLayer`

Client responsibilities:

- register client event handlers
- display teaching preview UI
- track client-side current stage
- render chronolith beams
- sync/render End sky seed and generated sky texture
- render Aender eye
- patch Enderman visuals

Design rule:

Server state is authoritative. Client systems render synced payload state and local visuals only.

## End Sky

Technical owners:

- `RetoldEndSkyData`
- `RetoldEndSkySeedSyncPayload`
- `RetoldClientEndSky`
- `RetoldEndSkyPatcher`
- `RetoldGeneratedEndSkyTexture`

Commands:

```mcfunction
/retold sky get
/retold sky randomize
```

Behavior:

- Server stores the End sky seed.
- Command can randomize the seed.
- Seed sync payload updates clients.
- Client sky code generates/patches the visible sky texture.

Design rule:

End sky randomization should update saved server data and sync to all players.

## Network Payloads

Technical owner:

- `RetoldNetworking`

Payloads:

| Payload | Direction | Purpose |
| --- | --- | --- |
| `RetoldStageSyncPayload` | server -> client | sync world stage |
| `RetoldEndSkySeedSyncPayload` | server -> client | sync End sky seed |
| `RetoldLearnRecipePayload` | client -> server | request villager recipe learning |
| `RetoldRequestTeachingPreviewPayload` | client -> server | request teaching preview refresh |
| `RetoldTeachingPreviewPayload` | server -> client | update teaching UI state |
| `RetoldChronolithBeamPayload` | server -> client | start/stop chronolith beam rendering |

Design rule:

Payload handlers should remain thin. They should call system owners instead of implementing gameplay logic in `RetoldNetworking`.

## Commands

Technical owner:

- `RetoldCommands`
- `RetoldCommandEvents`
- `RetoldBehaviorDebugEvents`

Commands:

```mcfunction
/retold stage get
/retold stage set <1-3>
/retold sky get
/retold sky randomize
/retoldbehavior ...
```

`/retold` is for general mod progression/debug controls.

`/retoldbehavior` is for AI and territory debugging; see the AI doc.

## Mixins

Mixin config:

- `src/main/resources/retold.mixins.json`

Main mixin groups:

| Area | Mixins |
| --- | --- |
| Recipe/progression | `ServerRecipeBookMixin`, `AdvancementVisibilityEvaluatorMixin`, `AbstractFurnaceBlockEntityMixin` |
| Villager teaching UI | `MerchantMenuAccessor`, `MerchantMenuTeachingSlotMixin`, `MerchantScreenMixin`, `VillagerInvoker` |
| World/stage/worldgen | `DelayedStructurePlacementMixin`, `NoVillageNearWorldSpawnMixin`, `EndDragonFightMixin`, `EndGatewayGenerationMixin`, `EndPortalBlockMixin` |
| Aender physics/rendering | `AenderChunkMapSaveMixin`, `AenderFlowingFluidMixin`, `AenderWaterFluidMixin`, `AenderWeatherMixin`, `AenderEntityLightingMixin`, `AenderRenderSectionRegionLightingMixin` |
| Mob AI/targeting | `MobTargetMixin`, `MobAggressiveMixin`, `MobBrainMemoryOwnerMixin`, `BrainMemoryMixin`, `PiglinAiMixin`, `PathNavigationMixin`, `MobHurtTargetMixin` |
| Guardian behavior | `ElderGuardianMixin`, `ElderGuardianInvulnerableHitMixin` |
| Enderman/client visuals | `EndermanRendererMixin`, `EndermanParticleMixin`, `LivingEntityTeleportParticleMixin`, `PortalParticleMixin` |
| Sleep | `BedBlockMixin`, `ServerLevelSleepMixin` |
| Accessors/invokers | `FoxInvoker`, `MerchantMenuAccessor`, `VillagerInvoker` |

Design rule:

Mixins are high-risk ownership points. Keep the gameplay rule in a named Retold system class when possible, and keep mixins as hooks, guards, or accessors.

## Resource And Data Layout

Retold resources are split by namespace:

- `assets/retold`: Retold block/item/entity/client assets
- `data/retold`: Retold dimensions, loot, tags, teaching data, biome/structure modifiers
- `data/minecraft`: overridden vanilla advancements/tags/worldgen data
- `assets/minecraft`: overridden vanilla textures, currently Enderman textures

Important data folders:

- `data/retold/villager_teaching`
- `data/retold/tags/worldgen/structure`
- `data/retold/neoforge/biome_modifier`
- `data/retold/neoforge/structure_modifier`
- `data/minecraft/advancement`

Design rule:

If a behavior is meant to be pack-tunable, prefer a JSON data file or tag over hard-coded ids.

## Persistence And Runtime State

Persistence and state owners:

| Class | Data |
| --- | --- |
| `RetoldWorldData` | world stage, offered elements, dragon egg position |
| `RetoldKnownRecipeData` | per-player known recipes |
| `RetoldEndSkyData` | End sky seed |
| `AenderStabilityData` | stable Aender chunk counts |
| `RetoldMobState` / `RetoldMobStates` | mob hunger/stress/confidence and home memory, cached in a weak map and saved to entity persistent NBT |
| `RetoldTerritoryMobState` / `RetoldTerritoryMobStates` | runtime territory warning posture/debug state, cached in a weak map |
| `RetoldTerritoryReputation` | runtime per-player territory suspicion/reputation entries |
| `RetoldChunkStructureData` | delayed structure/retrogen chunk metadata |

Design rule:

Use the owning persistence or runtime-state class for each subsystem. Do not create parallel runtime-only maps for state that must survive reloads.

## Performance Architecture

Main performance-sensitive systems:

- AI dispatcher, timing, scan cache, LOD, and work budgets
- delayed structure retrogen queue
- torch chunk indexing cap
- Aender forcefield rendering interval
- chronolith active channel map
- recipe/villager preview server checks

General performance rules:

- cap work per tick
- spread work by entity id or queue
- cache world scans and block searches
- avoid direct per-tick full-world scans
- keep client visuals separate from server truth
- prefer chunk-local metadata for worldgen recovery

## Debug And Validation

General commands:

```mcfunction
/retold stage get
/retold stage set <1-3>
/retold sky get
/retold sky randomize
/retoldbehavior perf
/retoldbehavior warning
```

Suggested validation passes:

- start a new world and confirm Stage 1
- kill dragon and confirm Stage 2
- offer `water_element` to dragon egg and confirm ritual state
- confirm `nether_star` does not hatch the dragon egg
- after all element paths exist, complete the egg ritual and confirm Stage 3
- verify End portal redirects to Aender at Stage 3
- place and break Aender stabilizers and confirm forcefield/stability behavior
- test chronolith start/stop/logout/block break
- test villager teaching preview and recipe unlock
- craft/smelt recipes and verify recipe knowledge behavior
- load delayed-structure chunks before and after Stage 2
- test torch extinguishing in rain and relighting
- test mob AI using `/retoldbehavior perf` and warning commands
- test guardian monument mining pressure and elder guardian water invulnerability

Build validation:

```bash
bash ./gradlew compileJava
```

## Adding New Features

Use this checklist:

1. Identify the owning subsystem.
2. Add registries in `registry` if new blocks, items, entities, game rules, or tags are needed.
3. Add data files if behavior should be pack-tunable.
4. Add saved data only when state must persist.
5. Add network payload only when client/server state must sync.
6. Add event registration in `Retold` only when needed.
7. Prefer system classes over mixin logic.
8. Keep mixins small and named after the vanilla hook they touch.
9. Add debug commands or output for stateful systems.
10. Compile and test the relevant progression path.

Do not:

- bypass `RetoldStageManager` for stage transitions
- write gameplay logic directly inside network handlers
- add uncapped per-tick world scans
- create new AI entity tick subscribers for normal mob behavior
- duplicate saved state in runtime-only maps
- put pack-tunable rules only in Java if a tag/data file would work
- let client-only state become authoritative

## Open Technical Watchpoints

These are areas to keep an eye on during future work:

- `Retold.java` has many event registrations and imports; grouping by subsystem would improve readability later.
- Recipe/advancement overrides under `data/minecraft` are broad and should be reviewed carefully when Minecraft updates.
- Mixins touch several sensitive vanilla systems; version updates need focused regression tests.
- AI performance is improved but should continue to be checked with `/retoldbehavior perf`.
- Aender generation/regeneration and stability effects should be tested with loaded chunks and multiplayer clients.
