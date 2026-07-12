# Design Implementation Status

> AI-generated status tracker. This file compares the original `Minecraft_ Retold - Design Document.docx` against the current codebase. It is meant for human maintainers and future AI coding agents. Treat this as a working map, not as a final product spec.

AI agents: before implementing from this tracker, read [`README.md`](README.md), ask design/implementation questions when the intended behavior is unclear, and update this tracker when a feature changes status.

Source design document:

- `Minecraft_ Retold - Design Document.docx`

Related generated docs:

- [`retold_mod_system.md`](retold_mod_system.md)
- [`retold_mob_ai_system.md`](retold_mob_ai_system.md)
- [`mob_ai_completion_matrix.md`](mob_ai_completion_matrix.md)

## Notes On This Review

The `.docx` was reviewed by extracting its text from `word/document.xml`. Text content was reviewed, but image-only crafting diagrams may not be fully represented here.

Status labels:

| Status | Meaning |
| --- | --- |
| Implemented | The feature exists in code/data in recognizable form. |
| Partial | Some meaningful part exists, but the full design is not done. |
| Not implemented | No clear implementation was found. |
| Design only | Lore/intent exists but does not directly require code yet. |
| Deferred/TBD | The design document itself marks the idea as unfinished or speculative. |
| Needs verification | Code appears related, but in-game behavior should be tested before calling it done. |

## Current Summary

Implemented or strongly represented:

- three world stages
- stage saved data and network sync
- dragon kill -> Stage 2
- dragon egg element ritual framework
- water element item
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
- removing the temporary nether star egg shortcut
- Stage 1 Nether star End portal activation path
- Aender late-game rewards and teleportation system
- Aender 8:1 travel scaling
- lava placement ban in Aender
- complete tools/armor/ores progression rework
- enchanting rework
- sword blocking/combat rework
- full villager economy/trade redesign
- full stage-specific Nether escalation including wildfires/Nether dragon
- Stage 3 illager raid radicalization beyond existing vanilla/AI support
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
| Stage 1 wither/Nether star required before End | Partial / unclear | Design calls for Nether star End portal activation. Current code has End progression and a nether star dragon egg shortcut, not clear End portal activation gating. |
| Stage 1 Ender Dragon kill advances to Stage 2 | Implemented | `RetoldEndProgressionEvents.onServerTickPost` checks dragon kill and calls `RetoldStageManager.setStage(...STAGE_2)`. |
| Stage 2 requires four classical elements | Partial | `RetoldElementType` has water, fire, earth, and air. Only the water item/path is implemented. |
| Stage 2 dragon egg hatches with elements | Partial | Dragon egg accepts element items and hatches when all four enum elements are offered. Only water can currently be acquired through a real path, with nether star shortcut still present. |
| Stage 3 starts after hatching egg | Implemented | `RetoldStageManager.setStage(...STAGE_3)` in egg hatch flow. |
| Stage 3 Aender replaces End | Partial | Overworld End portal redirects to Aender and players are ejected from vanilla End. The End dimension still exists technically. |
| Stage 3 Overworld easier / undead gone | Partial | Undead natural/spawner spawn is blocked and existing undead are cleansed. Other difficulty reductions are not broadly implemented. |

## Aender

| Design item | Status | Current implementation |
| --- | --- | --- |
| Aender dimension exists | Implemented | `RetoldAenderDimensions`, `data/retold/dimension/aender.json`, custom generator registry. |
| Aender replaces End after egg hatch | Partial | End portal redirect is implemented for Stage 3. Vanilla End is not deleted. |
| Aender is sky-like/floating islands | Implemented | `AenderChunkGenerator`, `AenderIslandSampler`, Aender biome/data/assets. |
| Dimension of change / chunks regenerate differently | Partial | `AenderVolatility`, chunk events, reality/world tick systems, stabilizer behavior. Needs in-game verification for final feel. |
| Stabilizer block makes chunks permanent | Implemented / needs verification | `AenderStabilityData`, `AenderStabilizerEvents`, `aender_stabilizer`. |
| Water flows faster/farther | Implemented | `AenderWaterFluidMixin`, `AenderFlowingFluidMixin`. |
| Lava cannot be placed there | Not implemented | No clear lava-placement ban found. |
| One Aender block equals eight Overworld blocks | Not implemented | Portal destination is fixed; no coordinate scaling/travel ratio found. |
| Only day/light | Implemented / needs visual verification | `data/retold/dimension_type/aender.json` sets `fixed_time: 6000`, skylight, and high ambient light; Aender weather is disabled. Client visuals still need in-game review. |
| Aender in-dimension teleportation setup | Not implemented | No teleport network/system found beyond portal redirect. |
| Late-game OP building/travel rewards | Not implemented | No Aender reward item set found beyond current blocks/entity/chronolith/dev tools. |
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
| Stage 3 illagers launch raids | Not implemented / partial | Illager AI/factions/territory exist. Specific Stage 3 raid escalation not found. |

## Tools, Armor, Ores, Enchanting, Combat

| Design item | Status | Current implementation |
| --- | --- | --- |
| Wood cannot be obtained by hand | Not implemented |
| Flint multi-tool | Not implemented |
| Flint/copper/iron/steel/diamond tool progression | Not implemented |
| New station progression: clay furnace, stone furnace, blast furnace, enchanted station | Not implemented |
| Leather/copper/iron/steel/diamond armor progression | Not implemented |
| Diamond tools weak unless enchanted | Not implemented |
| Enchanting rework | Deferred/TBD |
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
| Gamerule to bring recipe book back | Not implemented | No recipe-book restoration gamerule found. |
| Villager as recipe teacher / Terraria-like system | Partial | Profession-based teaching data, custom merchant UI slot, emerald cost, preview payloads. |
| Librarian tells ingredients | Not implemented | Teaching unlocks recipes; ingredient explanation not found. |
| Villagers magical pacifists lore | Design only / partial | Villager teaching and golem/illager lore support. No full villager society simulation. |
| Villagers refresh trades every day | Not implemented |
| Villages angered by stealing/crops/animals | Not implemented | Territory warning exists for Illagers/Nether Remnants, not village reputation. |

## Structures And Worldgen

| Design item | Status | Current implementation |
| --- | --- | --- |
| Delayed Stage 2 structures | Implemented foundation | `RetoldDelayedStructureRetrogen`, attachments, tags. |
| Mansions delayed to Stage 2 | Implemented / needs verification | `delayed_until_stage_2` includes `minecraft:mansion`. |
| Pillager outposts delayed to Stage 2 | Implemented / needs verification | Tag includes `minecraft:pillager_outpost`. |
| Mansion size/light redesign | Not implemented |
| Ancient Cities removed | Not implemented / unclear | No direct removal found in checked Retold tags. |
| Trial Chambers removed | Not implemented / unclear | No direct removal found in checked Retold tags. |
| End Cities removed | Implemented / needs worldgen verification | `data/minecraft/tags/worldgen/biome/has_structure/end_city.json` replaces the tag with an empty list. Existing worlds and generated chunks still need testing. |
| End gateways removed/reworked | Implemented | `EndGatewayGenerationMixin` cancels both random and position-based gateway spawning. |
| Ocean ruins/shipwrecks/buried treasure lore changes | Not implemented / unclear |
| Ocean ruins only near shore | Not implemented |
| Strongholds kept to 3 | Not implemented / unclear |
| Villages spawn farther/scarcer | Not implemented / unclear | `NoVillageNearWorldSpawnMixin` exists, but full village scarcity design not confirmed here. |
| Jungle/desert pyramids as boss tombs | Not implemented |
| Four element temples/challenges | Partial concept only | Water/ocean path exists; other element temples are missing. |
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
| Illagers Stage 2 spawn and Stage 3 radicalization | Partial | Delayed structures, illager factions/AI/territory. Stage-specific radical raids missing. |
| Pigmen restored in Stage 3 | Partial | Piglins enabled, zombified piglins blocked/cleansed. Hiring/follower system missing. |
| Enderman changed after dragon death | Implemented / partial | Eye-contact aggro removed Stage 2/3, visuals likely present. |
| Phantoms reimagined as night pressure, not sleep mechanic | Partial | `RetoldPhantomStalkerEvents` exists; exact spawning/sleep relationship not fully verified. |
| C418/music-disc monster | Not implemented |
| Killer Bunny re-added | Not implemented |
| Iceologer in igloo | Not implemented |
| Bees smaller | Not implemented / unclear |
| Vex damage nerfed | Not implemented / unclear |
| Sniffer removed | Not implemented | Sniffer has Retold forager AI, so current implementation keeps it. |
| Endermites removed | Not implemented | Endermite is present in small arthropod swarm AI. |
| Breeze repurposed | Not implemented / unclear |
| Green axolotl | Not implemented / unclear |

## Nether

| Design item | Status | Current implementation |
| --- | --- | --- |
| Nether Remnants/pigmen/blazes as fortress faction | Partial / implemented AI | Nether Remnants faction/territory covers piglin, brute, blaze; fortress/bastion warning. |
| Nether dragon | Not implemented |
| Nether dragon restores balance in Stage 3 | Partial | Stage 3 undead/piglin swap implies restored balance; no dragon. |
| Nether darker like 1.0 | Not implemented |
| Nether portal spread | Not implemented |
| Living piglins attack for shiny material | Partial / unclear | Piglin AI/faction exists; specific shiny-material rule not confirmed. |
| Pigmen can be hired/fight with player | Not implemented |
| Wither different by dimension | Not implemented |
| Fight music during wither | Not implemented |
| Wither as first boss / Nether access gating | Not implemented |
| Basalt/soul valley kept, Nether forests changed/removed | Not implemented / unclear |

## End And Aender Content Decisions

| Design item | Status | Current implementation |
| --- | --- | --- |
| Scratch modern End update | Partial | Aender redirects late End access; End gateways are blocked, End City structure biome tags are emptied, and outer End density is masked. Existing-world behavior still needs testing. |
| No End Cities / no End islands / no elytra | Partial / needs worldgen verification | End City biome tag is empty, gateway spawning is cancelled, and `end/sloped_cheese` uses `retold:central_end_island_mask` outside radius 512. Elytra removal depends on no End City loot being generated in practice. |
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
| Items dropped on death never despawn | Not implemented |
| Jump sprint not faster than sprint | Not implemented |
| Beds do not skip night / heal instead | Partial | Bed night skipping gamerule exists. Healing behavior not implemented. |
| Beds explode in Nether/End with custom death message | Not implemented / unclear |

## Modern Update Review Decisions

| Design item | Status | Current implementation |
| --- | --- | --- |
| Remove endermites | Not implemented | Endermites are currently part of swarm AI. |
| Keep/adjust ocean monuments | Partial | Guardian/elder guardian rework exists. |
| Remove fossils | Not implemented / unclear |
| Rework shulker boxes/storage | Not implemented |
| Totems drain XP or fail without XP | Not implemented |
| Remove recipe book | Partial | Recipe knowledge gating exists; UI removal/restoration incomplete. |
| Turtle scutes from killing turtles | Not implemented |
| Zombies stop targeting turtle eggs or target all animals | Not implemented / unclear |
| Tridents less OP/craftable | Not implemented |
| Ocean ruins/shipwrecks lore pass | Not implemented |
| Keep programmer-art-like visuals | Partial | Some custom/overridden textures exist; no full texture pack decision in code. |
| Remove Deep Dark/Ancient Cities/Warden | Not implemented / unclear |
| Remove trail ruins | Not implemented / unclear |
| Remove Trial Chambers | Not implemented / unclear |
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

## Recommended Next Design-Aligned Work

High priority if the goal is matching the original design:

1. Finish the four-element progression model.
2. Replace the nether star egg shortcut with real element completion.
3. Decide and implement the Stage 1 End portal activation rule.
4. Add the missing non-water element structures/bosses.
5. Complete Stage 2 and Stage 3 world-difficulty rules.
6. Decide whether Aender should really have 8:1 travel and implement or remove that design.
7. Add Aender late-game rewards or building/travel tools.
8. Decide how much of the tools/armor/ores rework is still wanted before implementing any of it.
9. Audit modern structures/mobs that the design says to remove or repurpose.
10. Continue validating AI against `retold_mob_ai_system.md`.

Lower priority / speculative:

- C418 monster
- rainbows
- pet door
- glowstone/water torches
- Nether portal spread
- New Game+ / world ending
- travel-road style features

## Maintenance Rule

Update this file whenever a major design item moves from `Not implemented` to `Partial` or `Implemented`, or when the original design changes.
