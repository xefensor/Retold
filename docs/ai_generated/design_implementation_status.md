# Design Implementation Status

> AI-generated status tracker. This file compares the original `Minecraft_ Retold - Design Document.docx` against the current codebase. It is meant for human maintainers and future AI coding agents. Treat this as a working map, not as a final product spec.

AI agents: before implementing from this tracker, read [`README.md`](README.md), ask design/implementation questions when the intended behavior is unclear, and update this tracker when a feature changes status.

Source design document:

- `Minecraft_ Retold - Design Document.docx`

Related generated docs:

- [`retold_roadmap.md`](retold_roadmap.md)
- [`retold_mod_system.md`](retold_mod_system.md)
- [`retold_mob_ai_system.md`](retold_mob_ai_system.md)
- [`mob_ai_completion_matrix.md`](mob_ai_completion_matrix.md)
- [`retold_testing_checklist.md`](retold_testing_checklist.md)
- [`retold_design_risks.md`](retold_design_risks.md)
- [`retold_known_issues.md`](retold_known_issues.md)

## Notes On This Review

The `.docx` was reviewed by extracting its text from `word/document.xml`. Text content was reviewed, but image-only crafting diagrams may not be fully represented here.

Status labels:

| Status | Meaning |
| --- | --- |
| Implemented | The feature exists in code/data in recognizable form. |
| Partial | Some meaningful part exists, but the full design is not done. |
| Not implemented | No clear implementation was found. |
| Not planned | Maintainer clarified this should not be implemented. |
| Design only | Lore/intent exists but does not directly require code yet. |
| Deferred/TBD | The design document itself marks the idea as unfinished or speculative. |
| Needs verification | Code appears related, but in-game behavior should be tested before calling it done. |

## Current Maintainer Direction Source

Last design clarification: 2026-07-12.

Active maintainer direction lives in [`retold_roadmap.md`](retold_roadmap.md). This status tracker applies that direction to individual original-design rows so implementation status stays visible without duplicating the roadmap.

## Current Summary

Implemented or strongly represented:

- three world stages
- stage saved data and network sync
- dragon kill -> Stage 2
- dragon egg element ritual framework
- water and air element items
- Stage 3 End portal redirect to Aender
- Aender dimension, generator, block palette, and stabilizer chunks
- Aender water flow changes
- Aender weather blocking
- Aender chunk volatility/stability foundation
- Endermen stop eye-contact aggression in Stage 2/3
- undead stop burning/fearing sun in Stage 2
- undead and zombified piglins are cleansed/blocked in Stage 3
- living piglins are blocked before Stage 3 and enabled/immune in Stage 3
- villager teaching and recipe knowledge system
- advancement hiding / recipe book knowledge gating
- delayed mansions and outposts until Stage 2
- ocean monument / elder guardian water element path
- Retold mob AI, factions, warning territory, hunger/home/flee/hunt systems
- torch weather/extinguished torch system
- chronolith time-acceleration block
- generated End sky seed system

Largest missing or partial design areas:

- full four-element progression
- non-water element bosses/temples
- deciding Stage 1 Nether star / Wither End portal activation
- Aender late-game rewards and teleportation system
- Aender 8:1 travel scaling
- lava placement ban in Aender
- complete tools/armor/ores progression rework
- enchanting rework
- sword blocking/combat rework
- full villager economy/trade redesign
- full stage-specific Nether escalation including wildfires and possible Nether dragon
- possible player-facing Stage 3 illager escalation clarity beyond existing vanilla/AI support
- pigmen hiring/follower system
- C418/music-disc monster
- structure removals and boss temple content beyond current delayed structure work

## Core Pillars

| Design item | Status | Current implementation |
| --- | --- | --- |
| Consistent lore across dimensions, mobs, structures, progression | Partial | Many systems now align around stages, Aender, factions, and territory. Lore is mostly documented, not enforced everywhere. |
| Meaningful progression with permanent world changes | Partial | `RetoldStageManager`, `RetoldWorldData`, delayed structures, Stage 3 Aender redirect, undead/piglin changes. Not all intended stage changes exist. |
| Discovery first, low written tutorial | Partial | Recipe book/advancement hiding and villager teaching support discovery. Some debug/dev text exists intentionally. |
| Living reactive world | Partial | Retold AI, territory warning, torch weather, delayed structures, stage changes. Many reactive-world notes are still missing. |
| Improve performance/QoL | Partial | AI performance architecture exists. Broader mod performance is mixed by subsystem and needs continued profiling. |

## Lore Foundation

| Design item | Status | Current implementation |
| --- | --- | --- |
| Experience/energy as magical foundation | Partial | XP is used by enchanting/vanilla and chronolith drain. Emeralds power villager teaching costs. No global energy system yet. |
| Emeralds as solid energy | Partial | Villager teaching uses emerald cost. No emerald-to-XP or energy conversion. |
| Obsidian/bedrock as energy-direction materials | Design only | Current code uses vanilla portal/end concepts and Aender redirect. No deeper material-energy mechanics. |
| Enchanting table lore with lapis/SGA/bookshelves | Design only | No Retold enchanting rework implemented. |
| Nether as realm of dead with pigmen/ghasts/blazes | Partial | Nether Remnants faction/territory and Stage 3 piglin rules exist. Ghast artillery AI exists. Lore-specific ghast mechanics are not implemented. |
| End as dying/drained dimension | Partial | End sky work and Aender replacement exist. Vanilla End overhaul/removal is partial. |
| Nether dragon/Aender dragon backstory | Design only | No Nether dragon or Aender dragon entity/boss implementation found. |

## Progression And Stages

| Design item | Status | Current implementation |
| --- | --- | --- |
| Three world stages | Implemented | `RetoldWorldStage`, `RetoldWorldData`, `RetoldStageManager`, `/retold stage`. |
| Stage 1 mostly vanilla | Partial | Stage 1 is default. Some Retold systems still run globally, such as recipe knowledge and AI. |
| Stage 1 requires diamond pickaxe/Nether portal | Not implemented | No explicit Retold gating found. Vanilla rules apply unless affected elsewhere. |
| Stage 1 wither/Nether star required before End | Deferred/TBD | Maintainer is undecided. Current code has End progression, not End portal activation gating. |
| Stage 1 Ender Dragon kill advances to Stage 2 | Implemented | `RetoldEndProgressionEvents.onServerTickPost` checks dragon kill and calls `RetoldStageManager.setStage(...STAGE_2)`. |
| Stage 2 requires four classical elements | Partial | Still planned. `RetoldElementType` has water, fire, earth, and air. Water has a real acquisition path; Air has an item, dragon egg ritual support, and locatable placeholder temple structure, but no wind/boss acquisition path yet. Fire and earth still need items and paths. Elements should be doable in any order. |
| Stage 2 dragon egg hatches with elements | Partial | Dragon egg accepts implemented element items and hatches when all four enum elements are offered. Water can currently be acquired through a real path. Air can be offered if obtained through commands or future boss drops. The temporary Nether Star shortcut has been removed. |
| Stage 3 starts after hatching egg | Implemented | `RetoldStageManager.setStage(...STAGE_3)` in egg hatch flow. |
| Stage 3 Aender replaces End | Partial | Overworld End portal redirects to Aender and players are ejected from vanilla End. Vanilla End intentionally still exists for command access to old builds. |
| Stage 3 Overworld easier / undead gone | Partial | Current design only requires undead/zombified piglin removal for now. Undead natural/spawner spawn is blocked and existing undead are cleansed. |

## Aender

| Design item | Status | Current implementation |
| --- | --- | --- |
| Aender dimension exists | Implemented | `RetoldAenderDimensions`, `data/retold/dimension/aender.json`, custom generator registry. |
| Aender replaces End after egg hatch | Partial | End portal redirect is implemented for Stage 3. Vanilla End is intentionally not deleted and remains command-accessible. |
| Aender is sky-like/floating islands | Implemented | `AenderChunkGenerator`, `AenderIslandSampler`, Aender biome/data/assets. |
| Dimension of change / chunks regenerate differently | Partial | `AenderVolatility`, chunk events, reality/world tick systems, stabilizer behavior. Needs in-game verification for final feel. |
| Stabilizer block makes chunks permanent | Implemented / needs verification | `AenderStabilityData`, `AenderStabilizerEvents`, `aender_stabilizer`. |
| Water flows faster/farther | Implemented | `AenderWaterFluidMixin`, `AenderFlowingFluidMixin`. |
| Lava cannot be placed there | Not implemented | Still planned. Audit found Aender water flow/weather hooks but no lava bucket/fluid placement blocker. |
| One Aender block equals eight Overworld blocks | Not implemented | Still planned. Portal destination is fixed; no coordinate scaling/travel ratio found. |
| Only day/light | Implemented / needs visual verification | `data/retold/dimension_type/aender.json` sets `fixed_time: 6000`, skylight, and high ambient light; Aender weather is disabled. Client visuals still need in-game review. |
| Aender in-dimension teleportation setup | Not implemented | Still planned. No teleport network/system found beyond portal redirect. |
| Late-game OP building/travel rewards | Not implemented | Still planned. No Aender reward item set found beyond current blocks/entity/chronolith/dev tools. |
| Inverted Overworld colors / liminal feel | Partial | Aender textures/palette and lighting exist. Needs visual/design pass. |

## Stage Differences

| Design item | Status | Current implementation |
| --- | --- | --- |
| Stage 2 undead do not burn in sunlight | Implemented | `RetoldUndeadEvents`, `RetoldUndeadSunFear`. |
| Stage 2 undead more aggressive/damaging | Partial | Retold undead horde AI exists; explicit stage-based stat boost not confirmed. |
| Stage 2 villagers build iron golems | Not implemented / unclear | Golem events exist, but no confirmed villager auto-building stage rule. |
| Stage 2 villagers sell better items | Partial | Villager teaching exists. Broad trade refresh/better-trade progression not implemented. |
| Stage 2 Nether becomes more dangerous | Partial | Nether behavior/factions exist. No Nether dragon/wildfire escalation confirmed. |
| Wildfires spawn in Nether | Not implemented | No wildfire entity/system found. |
| Woodland mansions generate in Stage 2 | Partial | Mansions are tagged as delayed until Stage 2. Mansion scaling/redesign is not implemented. |
| Pillager outposts generate in Stage 2 | Implemented / needs verification | Outposts are tagged as delayed until Stage 2. |
| Endermen green eyes / no eye contact aggro in Stage 2+ | Partial / implemented | Eye-contact aggro removal is implemented. Green visuals/assets/mixins exist; verify in client. |
| Stage 3 undead no longer spawn | Implemented | Natural/spawner undead spawn cancellation and existing undead cleansing. |
| Stage 3 pigmen/piglins spawn instead of zombified piglins | Partial | Living piglins unblocked/immune; zombified piglins blocked/cleansed. Hiring/follower behavior not implemented. |
| Stage 3 illagers launch raids | Not implemented / partial | Current behavior is enough for now. Illager AI/factions/territory exist; future work may make Stage 3 illager pressure clearer to players. |

## Tools, Armor, Ores, Enchanting, Combat

| Design item | Status | Current implementation |
| --- | --- | --- |
| Wood cannot be obtained by hand | Not implemented |
| Flint multi-tool | Not implemented |
| Flint/copper/iron/steel/diamond tool progression | Not implemented |
| New station progression: clay furnace, stone furnace, blast furnace, enchanted station | Not implemented |
| Leather/copper/iron/steel/diamond armor progression | Not implemented |
| Diamond tools weak unless enchanted | Not implemented |
| Enchanting rework | Not implemented | Still planned. |
| Iron rods/sticks crafting changes | Not implemented |
| Scaffolding crafted with wooden sticks / normal wood | Not implemented |
| Glass dyeing max 8 but supports fewer | Not implemented |
| Flint spear | Not implemented |
| Boats/minecart crafting variants | Not implemented |
| Sword blocking / shield combat rework | Not implemented |
| Sword sweep on right-click | Not implemented |
| XP/energy affects damage/defense | Not implemented |
| Remove mending | Not implemented |
| Smithing table removed/merged with anvil | Not implemented |

## Recipe Discovery And Villagers

| Design item | Status | Current implementation |
| --- | --- | --- |
| Remove or heavily restrict recipe book | Partial / implemented foundation | `ServerRecipeBookMixin`, `RetoldKnownRecipeData`, recipe unlock context. |
| Hide advancements / reduce guide-like UI | Partial | `AdvancementVisibilityEvaluatorMixin`, data overrides. |
| In-world recipe learning | Partial | Villager teaching system implemented. Other discovery methods are not. |
| Gamerule to bring recipe book back | Not planned | Maintainer clarified this should not be added. |
| Villager as recipe teacher / Terraria-like system | Partial | Profession-based teaching data, custom merchant UI slot, emerald cost, preview payloads. |
| Librarian tells ingredients | Not implemented | Teaching unlocks recipes; ingredient explanation not found. |
| Villagers magical pacifists lore | Design only / partial | Villager teaching and golem/illager lore support. No full villager society simulation. |
| Villagers refresh trades every day | Not implemented |
| Villages angered by stealing/crops/animals | Not implemented | Still planned. Territory warning exists for Illagers/Nether Remnants, not village reputation. |

## Structures And Worldgen

| Design item | Status | Current implementation |
| --- | --- | --- |
| Delayed Stage 2 structures | Implemented foundation | `RetoldDelayedStructureRetrogen`, attachments, tags. |
| Mansions delayed to Stage 2 | Implemented / needs verification | `delayed_until_stage_2` includes `minecraft:mansion`. |
| Pillager outposts delayed to Stage 2 | Implemented / needs verification | Tag includes `minecraft:pillager_outpost`. |
| Mansion size/light redesign | Not implemented |
| Ancient Cities removed | Not implemented / unclear | Still planned. No direct removal found in checked Retold tags. |
| Trial Chambers removed | Not implemented / unclear | Still planned. No direct removal found in checked Retold tags. |
| End Cities removed | Implemented / needs worldgen verification | `data/minecraft/tags/worldgen/biome/has_structure/end_city.json` replaces the tag with an empty list. Existing worlds and generated chunks still need testing. |
| End gateways removed/reworked | Implemented | `EndGatewayGenerationMixin` cancels both random and position-based gateway spawning. |
| Ocean ruins/shipwrecks/buried treasure lore changes | Not implemented / unclear |
| Ocean ruins only near shore | Not implemented |
| Strongholds kept to 3 | Not implemented / unclear | Still planned. |
| Villages spawn farther/scarcer | Not implemented / unclear | `NoVillageNearWorldSpawnMixin` exists, but full village scarcity design not confirmed here. |
| Jungle/desert pyramids as boss tombs | Deferred/TBD | Maybe still planned, but design is not final. |
| Four element temples/challenges | Partial | Water/ocean path exists. Air has a design spec and placeholder locatable temple structure, but no wind/boss/collapse challenge yet. Fire and earth temples are missing. Element order should be free. |
| Sunflowers point to significant structure | Not implemented |

## Ocean Monument And Water Element

| Design item | Status | Current implementation |
| --- | --- | --- |
| Ocean monument challenge rework | Partial | Guardian mining pressure, elder guardian sentinel/boss behavior, protected mining pressure. |
| Monument has no enemies in phase one and populates in phase two | Not implemented / unclear | Guardian systems exist, but no confirmed Stage 1/2 spawn split. |
| Elder guardian drops water element | Implemented | `RetoldElderGuardianBoss.addGuaranteedWaterElementDrop`. |
| Mining fatigue rework | Partial | Guardian mining pressure and blocked-hit curse exist. Vanilla elder guardian fatigue is also modified by mixins. |
| Defeat boss by draining water / suffocation idea | Not implemented | Elder guardian dries/damages out of water, but not a complete room-drain boss design. |
| Heart of the Sea from elder guardian/sea boss | Not implemented |
| Conduit discovery improvement | Not implemented |

## Mobs And AI

| Design item | Status | Current implementation |
| --- | --- | --- |
| Living reactive mobs | Partial / broad | Full Retold AI system exists; see AI doc. |
| Mobs interact with each other, not only player | Partial | Factions, predator/prey, herd panic, pack behavior, assist systems. |
| Undead horde identity | Implemented / partial | `UNDEAD_HUNGRY`, `RetoldUndeadHordeEvents`, Stage 2/3 undead rules. |
| Skeletons as ranged undead | Implemented / partial | `UNDEAD_TOLERANT`, `RetoldSkeletonRangedEvents`. |
| Golems as magical defenders | Partial | Iron/snow golem defender/guard support. Copper/tuff golems not implemented. |
| Elementals: blaze/breeze/wildfire | Partial | Blaze territory guard; breeze special vanilla; wildfire missing. |
| Villagers pacifist / golem creators | Partial | Some golem/villager systems exist, but not full society/progression. |
| Witches as exiled villagers, loose ally | Implemented in AI design | Witch is commander support / loose illager ally. |
| Illagers Stage 2 spawn and Stage 3 radicalization | Partial | Delayed structures, illager factions/AI/territory. Current behavior is enough for now; future work may improve player-facing clarity. |
| Pigmen restored in Stage 3 | Partial | Piglins enabled, zombified piglins blocked/cleansed. Hiring/follower system is still planned. |
| Enderman changed after dragon death | Implemented / partial | Eye-contact aggro removed Stage 2/3, visuals likely present. |
| Phantoms reimagined as night pressure, not sleep mechanic | Partial | `RetoldPhantomStalkerEvents` exists; exact spawning/sleep relationship not fully verified. |
| C418/music-disc monster | Not implemented | Still planned. |
| Killer Bunny re-added | Not implemented | Still planned. |
| Iceologer in igloo | Not implemented | Still planned. |
| Bees smaller | Not implemented / unclear | Still planned. |
| Vex damage nerfed | Not implemented / unclear | Still planned. |
| Sniffer removed from survival spawning | Not implemented | Sniffer does not need complete code/entity removal, but should not be naturally/survival-spawnable if removed from progression. Current Retold AI still supports sniffers. |
| Endermites removed from survival spawning | Not implemented | Endermites do not need complete code/entity removal, but should not be naturally/survival-spawnable if removed from progression. Current Retold AI still supports endermites. |
| Breeze repurposed | Not implemented / unclear |
| Green axolotl | Not implemented / unclear | Still planned. |

## Nether

| Design item | Status | Current implementation |
| --- | --- | --- |
| Nether Remnants/pigmen/blazes as fortress faction | Partial / implemented AI | Nether Remnants faction/territory covers piglin, brute, blaze; fortress/bastion warning. |
| Nether dragon | Deferred/TBD | Possibly planned for the ending, but not final. |
| Nether dragon restores balance in Stage 3 | Partial | Stage 3 undead/piglin swap implies restored balance; no dragon. |
| Nether darker like 1.0 | Not implemented |
| Nether portal spread | Not implemented | Still planned as portal energy draining/spreading into surroundings. |
| Living piglins attack for shiny material | Partial / unclear | Piglin AI/faction exists; specific shiny-material rule not confirmed. |
| Pigmen can be hired/fight with player | Not implemented | Still planned for Stage 3 piglin/pigman behavior. |
| Wither different by dimension | Not implemented |
| Fight music during wither | Not implemented |
| Wither as first boss / Nether access gating | Not implemented |
| Basalt/soul valley kept, Nether forests changed/removed | Not implemented / unclear |

## End And Aender Content Decisions

| Design item | Status | Current implementation |
| --- | --- | --- |
| Scratch modern End update | Partial | Aender redirects late End access; End gateways are blocked, End City structure biome tags are emptied, and outer End density is masked. Existing-world behavior still needs testing. Vanilla End remains command-accessible by design. |
| No End Cities / no End islands / no survival elytra | Partial / needs worldgen verification | End City biome tag is empty, gateway spawning is cancelled, and `end/sloped_cheese` uses `retold:central_end_island_mask` outside radius 512. Elytra item should remain, but should not be obtainable in survival because End Cities do not generate. |
| End sky randomly generated | Implemented / partial | `RetoldEndSkyData`, `/retold sky randomize`, client generated sky texture. |
| Aender Eye / Eye of Ender symbiosis | Partial | `AenderEye` entity exists. Full lore/gameplay relation to ender pearls/eyes not implemented. |
| Ender pearl shell destroyed with blaze powder to retrieve eye | Not implemented |

## Farming, Blocks, Environment, QoL Notes

| Design item | Status | Current implementation |
| --- | --- | --- |
| Animals eat planted crops | Partial | Retold animal foraging/hunger exists. Specific crop-eating pressure needs verification. |
| Crops/non-progression progress in unloaded chunks | Not implemented |
| Rainbows | Not implemented |
| Enemies spawning deep, ignoring light | Not implemented |
| Moonlight spawning despite torches | Not implemented |
| Torches extinguished by rain | Implemented | `TorchWeatherEvents`, extinguished torch blocks. |
| Water torches from glow squids | Not implemented |
| Glowstone torches brighter | Not implemented |
| Glow squids and glow item frames actually glow | Not implemented |
| Tall grass lower hitbox when holding weapon | Not implemented |
| Pet door | Not implemented |
| Cats avoid creepers | Not implemented |
| Mobs flee creeper about to explode | Not implemented |
| Snowmen do 1 HP damage | Not implemented |
| Sticks/bones deal more damage than hand | Not implemented |
| Explosive block that drops all blocks | Not implemented |
| Items dropped on death despawn much later than vanilla | Not implemented | Updated design: drops should despawn after a long time, much longer than vanilla 5 minutes, not never. |
| Jump sprint not faster than sprint | Not implemented |
| Beds do not skip night / heal instead | Partial | Bed night skipping gamerule exists and is intentional. Future healing can consume hunger. |
| Beds explode in Nether/End with custom death message | Not implemented / unclear |

## Modern Update Review Decisions

| Design item | Status | Current implementation |
| --- | --- | --- |
| Remove endermites from survival spawning | Not implemented | Endermites can remain in code/AI, but should not be naturally/survival-spawnable. |
| Keep/adjust ocean monuments | Partial | Guardian/elder guardian rework exists. |
| Remove fossils | Not implemented / unclear | Still planned. |
| Rework shulker boxes/storage | Not implemented |
| Totems drain XP or fail without XP | Not implemented |
| Remove recipe book | Partial | Recipe knowledge/teaching direction stays. No restoration gamerule is planned. |
| Turtle scutes from killing turtles | Not implemented |
| Zombies stop targeting turtle eggs or target all animals | Not implemented / unclear |
| Tridents less OP/craftable | Not implemented |
| Ocean ruins/shipwrecks lore pass | Not implemented |
| Keep programmer-art-like visuals | Partial | Some custom/overridden textures exist; no full texture pack decision in code. |
| Remove Deep Dark/Ancient Cities/Warden | Not implemented / unclear | Still planned. |
| Remove trail ruins | Not implemented / unclear | Still planned. |
| Remove Trial Chambers | Not implemented / unclear | Still planned. |
| Chase the Skies ghast idea rejected | Not implemented / design note only |
| Baby mobs scaled-down adults can stay | Design only |

## Current Implementation Files To Know

| Area | Main files |
| --- | --- |
| Stage progression | `RetoldStageManager`, `RetoldWorldData`, `RetoldEndProgressionEvents`, `RetoldStageRuntimeEvents` |
| Aender | `RetoldAenderAccess`, `AenderChunkGenerator`, `AenderVolatility`, `AenderStabilityData`, `AenderStabilizerEvents` |
| Dragon egg elements | `RetoldEndProgressionEvents`, `RetoldElementType`, `WaterElementItem`, `RetoldRitualEffects` |
| Recipe knowledge | `RetoldKnownRecipeData`, `RetoldRecipeBookEvents`, `ServerRecipeBookMixin`, `AbstractFurnaceBlockEntityMixin` |
| Villager teaching | `RetoldVillagerTeaching`, `RetoldVillagerTeachingReloadListener`, `data/retold/villager_teaching` |
| Delayed structures | `RetoldDelayedStructureRetrogen`, `RetoldChunkStructureData`, `RetoldDelayedStructureHelper`, delayed structure tags |
| AI | `RetoldBehaviorEntityTickDispatcher`, `RetoldMobProfiles`, `RetoldAiControl`, `RetoldBehaviorPerf` |
| Territory warning | `RetoldTerritoryController`, `RetoldTerritoryRules`, `RetoldTerritoryTargetBlocker`, territory structure tags |
| Undead stage rules | `RetoldUndeadEvents`, `RetoldUndeadSunFear`, `RetoldUndeadCleansing` |
| Piglin stage rules | `RetoldPiglinEvents` |
| Enderman changes | `RetoldEndermanEvents`, `RetoldEndermanBehavior`, Enderman rendering mixins |
| Guardian/water element | `RetoldElderGuardianBoss`, `RetoldGuardianMiningPressure`, `RetoldOceanMonumentSupport` |
| Torch weather | `TorchWeatherEvents`, `ExtinguishedTorchBlock`, `ExtinguishedWallTorchBlock` |
| Commands/network | `RetoldCommands`, `RetoldNetworking` |

## Recommended Next Work

Use [`retold_roadmap.md`](retold_roadmap.md) for the active priority list. Use this file to check each feature's implementation status against the original design.

## Maintenance Rule

Update this file whenever a major design item moves from `Not implemented` to `Partial` or `Implemented`, or when the original design changes.
