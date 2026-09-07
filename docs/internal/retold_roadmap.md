# Retold Roadmap

> Developer-maintained, AI-assisted roadmap. This file is meant for human developers and future AI coding agents. It summarizes active design direction, not every historical idea from the original design document.

## Current Direction

Retold is still built around:

- three world stages
- four classical element paths plus Life and Death, with all six sacrifices completable in any order
- Aender replacing normal late End access while vanilla End remains command-accessible
- horizontal Aender portals using implemented 8:1 Overworld/Aender travel scaling
- elytra remaining as an item but not being survival-obtainable through End Cities
- recipe knowledge and villager teaching instead of a vanilla-style recipe-book restore
- Retold mob AI driven by species, faction, profile, state, and nearby world situation
- healthy ordinary predators defend themselves after successful damage; wild ordinary predators
  instead abandon hunting or retaliation and flee their attacker for ten seconds after a real hit
  leaves them below 25% health, while tamed defenders, Undead, bosses, and active territory duty
  remain exempt from wounded flight
- loaded school fish graze tagged seagrass/kelp, while Squid and Glow Squid consume tagged dropped
  raw fish without hunting living prey
- loaded land herds, Pig foraging groups, and exact-species fish schools keep their persisted range
  while compatible local food remains, then migrate together under hunger after depletion; there is
  no separate domesticated classification or player-defined enclosure flag
- Stage 1 Undead using a weaker short-range coordination baseline, with Stage 2 escalating through
  wider awareness, convergence, imperfect cross-family support, and a modest tagged natural-spawn
  weight increase under vanilla caps rather than direct stat buffs
- survival worldgen/spawn removal for some modern content instead of necessarily deleting all code support
- Crimson and Warped Forests remaining distinct biomes but generating as open fungal deserts, with
  colored nylium and rare fungal landmarks instead of dense trees and undergrowth
- beds not skipping night
- rain extinguishing normal torches
- every newly generated Jungle Pyramid containing a deterministic randomized two-level Earth
  Labyrinth reached through its lever-puzzle room, while already-generated pyramids remain untouched
- the Earth Guardian beginning as an indestructible Stage 1 statue, awakening on a Stage 2 chamber
  visit, sensing players through vibrations, roaming the maze, and permanently relocating any
  loaded block—including player blocks, block entities, Bedrock, and terrain outside the labyrinth—
  to open paths and build defenses; only administrative griefing/protection policy may deny edits;
  once awake it becomes damageable and attacks remembered positions through a telegraphed ceiling
  collapse, a persistent 3×3 pool of real lava replacing the ground beneath the marked position,
  or a 3×3 floor collapse up to four blocks deep with ordinary fall damage,
  without acquiring a sight target (floor-to-lava direction confirmed 2026-09-07)
- labyrinth side rooms containing traps, some treasure, and naturally spawning mobs in darkness,
  without dedicated spawners; the Earth Guardian drops Lodestone as the Earth egg sacrifice
  (confirmed 2026-09-07), with ordinary recipe/chest acquisition removed
- the full-sized Earth Guardian carving its own traversable tunnels and physical ramps between
  floors rather than relying on boss-sized pregenerated stairs (confirmed 2026-09-07)
- faster multi-block tunnel excavation and deliberate wall building from relocated material
  (confirmed 2026-09-08), superseding the initial one-block-per-second implementation
- the Earth Guardian pursuing more aggressively into close-range melee while retaining its
  environmental attacks (confirmed 2026-09-08); this supersedes the earlier environment-only
  attack restriction, not vibration-based tracking or administrative protection rules

## High Priority

These are the strongest next design-aligned areas:

1. Focus the next development pass on Villager and village systems. Naturally verify the current
   communal storage, Farmer supply, livestock tending, property reputation, Iron Golem construction,
   torch-maintenance, and trade-stock loops in ordinary, multiplayer, dedicated-server, and existing
   villages; identify remaining coordination and survival gaps; then design and implement the next
   coherent village-society slice.
2. Verify the newly completed six-sacrifice Dragon Egg gate in fresh, upgraded, and multiplayer worlds.
3. Complete the missing Earth path from its generated labyrinth and persisted vibration-investigating,
   terrain-relocating guardian: verify and tune its initial arrow-trap/treasure side rooms,
   environmental hazards, Lodestone reward, and natural dark-area spawning. Also
   naturally verify the maze across seeds, borders, and upgraded worlds, and verify and tune the
   initial roaming Wildfire Fire path, implemented Life and Death acquisition boundaries,
   cartographer Air Temple discovery map, and Air Temple/Gale Core path.
4. Decide whether Stage 1 needs Wither/Nether star End portal activation.
5. Add remaining Aender in-dimension teleportation and late-game travel/building rewards.
6. Replace the provisional `dev_aender_portal_frame` name/assets when the final portal-frame design is chosen.
7. Audit and verify survival removal for End Cities, outer End progression, Ancient Cities, Deep Dark/Warden, and Trial Chambers; keep Trail Ruins and fossils, and keep the implemented Sniffer and Endermite removals regression-tested.
8. Naturally verify hunger-satisfaction breeding across representative ordinary, aquatic, Nether,
   egg-laying, pregnant, mixed-equine, and tamed animals, including population growth and save/load.
9. Naturally verify the bounded unloaded ecosystem with crowded returns, starvation outcomes,
   Farmer crop/storage contention, vanilla spawn composition, multiplayer, dedicated servers,
   long sessions, and existing worlds.
10. Continue implementing the remaining confirmed mob/faction contract and profiling real
    loaded-mob tests.

## Planned Systems

These are still planned but need feature-specific design before implementation:

- tool, armor, ore, and station progression beyond the implemented Flint-through-Diamond spine, Spear ladder, alternative-acquisition tiers, six-attempt-per-chunk Copper frequency adjustment, and initial Aenderite material foundation; next naturally verify Copper density, loot/trade pacing, and the dynamic Diamond rule, then design remaining Netherite/Aenderite equipment boundaries
- enchanting rework beyond the implemented complete 43-spell SGA catalog, knowledge persistence/sync, anvil-learning route, knowledge-aware tooltips, and deterministic glyph-entry table; next verify/refine the composed client layout and dedicated multiplayer synchronization, then perform the wider enchantment audit
- broader enchanting acquisition changes beyond the implemented removal of Mending from new random loot and Librarian trades
- sword/shield combat rework
- Stage 3 piglin/pigman hiring or follower behavior
- longer death-drop despawn timer than vanilla
- bed healing that consumes hunger
- water torches, glowstone torches, rainbows, pet doors, and glow improvements
- C418/music-disc monster
- killer bunny
- iceologer
- smaller bees
- green axolotl
- broader village society work beyond the implemented loaded-world communal food, Farmer supply,
  profession livestock tending, property reputation, golem construction, torch maintenance, and
  daily trade-stock refresh

The detailed confirmed behavior contract is maintained in
[`retold_mob_ai_system.md`](retold_mob_ai_system.md#confirmed-gameplay-contract). Implementation
status must remain explicit in [`design_implementation_status.md`](design_implementation_status.md);
the contract being confirmed does not mean it is implemented.

## Mod Compatibility And Community Integration TODO

Retold should remain usable as a standalone overhaul, but its major systems should be extensible enough that modpack authors and other mods can integrate with them without Retold carrying hard dependencies on every supported project.

General rule:

- design every new system with mod and datapack compatibility in mind from the beginning; identify
  standard tags, Retold-owned semantic data/tags, optional integration boundaries, and safe behavior
  for unknown third-party content before hard-coding vanilla identities
- when materially changing an existing system, audit and improve the compatibility of the touched
  surface where it is safe and testable; do not launch unrelated broad rewrites, and record unsafe or
  unresolved extension points for later work
- compatibility improvements must preserve Retold's standalone defaults unless the developer has
  explicitly approved a gameplay change, with regression coverage for the unchanged defaults
- prefer data, tags, stable public hooks, and small optional adapters over hard-coded checks for individual mods
- distinguish **compatibility** (both mods work together), **integration** (Retold understands the other mod's systems), and **balance support** (the other mod preserves Retold's intended progression); broad compatibility is desirable, integration should be selective, and balance support should not be promised by default
- avoid direct dependencies unless an integration genuinely cannot be implemented safely as optional support
- third-party integrations should go through stable Retold-owned interfaces rather than writing internal saved data or calling implementation details directly

### High-priority compatibility work

- [x] Make faction membership data-driven instead of relying only on hard-coded vanilla entity IDs in `RetoldFactionMembers`.
  - Allow exact entity IDs and/or entity tags to opt into Retold factions.
  - Preserve Retold's built-in vanilla defaults.
  - Make it possible for a datapack or compatibility addon to classify a modded mob as Undead, Illager, Nether Remnant, Village Defender, Ender, etc. without Java patches in Retold.
  - Keep conditional relations such as Witch raid cooperation expressible without turning every special case into a generic faction member.
  - Implemented with one additive `retold:factions/*` entity-type tag per fixed Retold faction and
    `retold:alliances/illager_loose_allies` for conditional Witch-style alignment. Conflicting full
    memberships fail closed, a full faction suppresses a loose alliance, and reloads refresh cached
    classification plus goals for loaded mobs. Defaults preserve the former exact members while
    composing `minecraft:illager` and `minecraft:undead`; tamed undead mounts suppress generic
    hostile Undead identity at the entity level.

- [x] Audit Retold for places where standard Minecraft/NeoForge common tags should be used instead of exact vanilla item/block checks.
  - Prefer common material tags where the gameplay meaning is genuinely "any valid material of this type".
  - Keep Retold-specific semantic tags for concepts owned by Retold.
  - Add extension tags where useful for modpack authors, such as valid torch igniters, weak mob barriers, portal-related materials, or other future Retold systems.
  - Behavior-preserving audit completed for environmental mob resources, ordinary forage and food families, Spider-lair web counting, Illager village signals, Nether-remnant and Ocean-Monument guard anchors, protected monument blocks, consumable Campfire igniters, and leaf-preserving tools. Standard vanilla/NeoForge tags are nested where their defaults match exactly. Fixed progression, paired mapping, portal/structure, unique loot/replacement, and currently unverified Sniffer checks remain exact by design.

- [x] Add a stable recipe-visibility/knowledge hook shared by all recipe UIs.
  - Vanilla recipe-book behavior, EMI, JEI, and future viewers should be able to ask the same Retold authority whether a recipe is known/visible to a player.
  - Unknown Retold recipes must not become spoilers merely because a recipe-viewer mod is installed.
  - Consider separate states where an item/output is known but its exact recipe is still undiscovered.
  - `RetoldRecipeKnowledge` now exposes the authoritative per-player query, teaching operation,
    immutable known-id snapshot, and opt-in custom recipe-type registration. The client receives the
    same knowledge snapshot used by vanilla; separate output-only discovery remains only a future
    design option.

- [ ] Add optional EMI compatibility.
  - Hide or filter undiscovered recipes according to Retold recipe knowledge.
  - Do not require EMI for normal Retold operation.

- [ ] Add optional JEI compatibility.
  - Match the same recipe-discovery rules as vanilla/EMI rather than creating a separate knowledge model.
  - Do not require JEI for normal Retold operation.

- [x] Create a generic Retold world-protection permission layer before adding claim-mod-specific adapters.
  - Centralize checks such as `canMobBreak`, `canWorldModify`, `canStructureGenerate`, and `canPortalCreate` (exact API names TBD).
  - Route Retold-owned world mutation through this layer where practical: Aender chunk replacement/regeneration, counterpart portal construction, retrogen/delayed structures, Gale Core block breaking, and future environmental transformations.
  - Default behavior without a protection integration should preserve current Retold behavior.
  - Later adapters can map this layer to popular claims/protection systems without scattering mod checks across gameplay code.
  - `RetoldWorldProtection` is default-allow with uniquely identified, removable deny rules. It now
    gates position-aware Retold mob breaking/placement, Gale Core damage, Aender regeneration and
    portal creation, and delayed structure retrogen. Concrete claim adapters remain separate work.

### Data-driven mob compatibility

- [ ] Keep modded-mob AI participation opt-in/configurable through Retold's existing datapack mob-profile system.
  - Document how compatibility datapacks can assign Retold profiles to third-party entity IDs.
  - Ensure unknown entities are not accidentally taken over by Retold AI merely because they extend a vanilla class.

- [ ] Add explicit opt-out/extension mechanisms where the current profile model is insufficient.
  - Consider Retold tags/data for AI-managed, AI-excluded, territory-excluded, or faction-excluded entities if those distinctions are needed in practice.
  - A mod with sophisticated custom AI must be able to coexist without Retold overriding behavior it does not own.

- [x] Make faction + profile composition work cleanly for third-party mobs.
  - A compatibility pack should be able to say, for example, that a modded creature uses a predator/grazer profile while separately belonging to an existing Retold faction.
  - Preserve the design rule that profile describes daily life while faction describes diplomacy/relationships.
  - Faction tags now classify independently of the existing datapack profile registry; adding only
    a faction tag never opts an unknown entity into Retold's managed daily-life AI.

### Recipe and machine compatibility

- [x] Generalize recipe-discovery handling so unknown third-party recipe types fail open safely instead of being unintentionally blocked or spoiled.

- [x] Provide an extension point for integrations to register additional recipe types with Retold's discovery system.
  - This should support modded processing systems such as crushers, mixers, presses, sawmills, alloying, etc. without Retold knowing every machine mod directly.
  - Pack authors should be able to decide whether a third-party recipe type participates in Retold discovery.

- [ ] Use a large automation/processing mod such as Create as a compatibility stress test, without promising Create-specific balance support.
  - Test custom recipes, machines, automated crafting, moving blocks/contraptions, and world interaction for assumptions in Retold.
  - Add bespoke compatibility only when a concrete conflict justifies it.

### Public Retold integration API

- [x] Design a deliberately small, stable public API package, `cz.xefensor.retold.api`.
  - The initial supported surface intentionally contains only recipe discovery and world mutation
    protection. Stage, faction, profile, and Aender-specific contracts remain candidates rather than
    exposing their mutable implementation classes prematurely.

Potential API surfaces to evaluate:

- [ ] World stage read access and safe stage transition hooks (`RetoldStages` or equivalent).
- [ ] Faction registration/query hooks (`RetoldFactions`).
- [ ] Mob-profile integration/query hooks (`RetoldMobProfiles`).
- [x] Recipe knowledge and visibility (`RetoldRecipeKnowledge`).
- [x] World-protection/world-mutation permission hooks (`RetoldWorldProtection`).
- [ ] Aender queries/events that third-party integrations may legitimately need (`RetoldAender`).

API rules:

- [ ] Do not expose mutable implementation storage when a manager already owns side effects.
  - Example: external code should never directly edit Retold world-stage saved data; stage changes must continue through the stage manager/official API so synchronization and transition side effects occur.
- [x] Prefer events/queries over exposing internal classes.
- [x] Document API stability expectations before declaring interfaces public.
- [x] Keep optional integrations isolated so a missing third-party mod never causes classloading failures.
  - The core API contains no EMI, JEI, or claim-mod classes; concrete adapters will remain optional,
    separately loaded integration code.

### UI/information integrations

- [ ] Add optional Jade support after defining what information is intentionally discoverable.
  - Show useful public-facing names/state for Retold blocks/entities where appropriate.
  - Do not expose hidden stage information, exact internal AI state, faction debug data, hidden structure locations, hunger internals, or other information that violates Retold's Discovery First pillar unless explicitly designed as player-facing knowledge.

- [ ] Consider similar lightweight support for other information-overlay mods only after the same information-visibility policy exists.

### Scripting and modpack-author support

- [ ] Evaluate KubeJS/CraftTweaker-style hooks after the core public API is stable.
  - Useful operations may include reading the current Retold stage, reacting to stage transitions, registering faction membership, extending recipe discovery, or associating compatible structures/content with Retold stages.
  - Prefer exposing generic Retold APIs/events that scripting integrations can wrap rather than embedding scripting-specific logic into core gameplay.

### Equipment/accessory compatibility

- [ ] Revisit Curios/accessory compatibility only when Retold gains equipment whose design actually benefits from extra slots.
  - Do not make Curios a dependency merely for hypothetical future items.
  - Avoid assumptions that all meaningful equipped items must live exclusively in vanilla armor/hand slots if a generic query can be used instead.

### Mods that only need baseline compatibility

Retold does not need to preserve the intended balance of mods that deliberately bypass its progression. For mods such as waystones/teleportation, minimaps, large backpacks, ore multiplication, biome/worldgen overhauls, boss packs, or extra dimensions:

- [ ] Avoid crashes, corruption, or obviously broken interactions where reasonably possible.
- [ ] Document known incompatibilities when found.
- [ ] Do not add bespoke balance integration unless there is a concrete community need and the integration still fits Retold's design.

### Compatibility testing strategy

- [ ] Build a small representative compatibility test matrix rather than attempting to test every mod.
  - one recipe viewer (EMI/JEI)
  - one information overlay (Jade)
  - one claims/protection system once the protection API exists
  - one large automation/content mod such as Create
  - one substantial creature/mob mod to exercise profile/faction extensibility
  - one scripting/modpack customization tool once public hooks exist
- [ ] Test dedicated server and multiplayer behavior for integrations that affect world mutation, recipes, factions, or player knowledge.
- [ ] Keep compatibility fixes regression-tested when practical, especially for generic APIs that many mods can rely on.

## Native Retold Systems Vs External Companion Mods

Retold should not depend on another mod for a feature that is important to Retold's own simulation, progression, lore, or persistent world state when a focused native implementation is practical. External companion mods are best reserved for specialist rendering, performance, audio, and presentation work that does not define Retold's gameplay rules.

Decision rule:

> If removing a feature would materially change the simulation or the decisions the player makes, Retold should probably own that feature. If removing it would mostly make the game look, sound, or run worse, an external companion mod is usually appropriate.

This is also a maintenance rule. Native Retold systems move forward with Retold's Minecraft/NeoForge updates instead of being blocked by an abandoned dependency, while large specialist projects should not be reimplemented merely to avoid dependencies.

### Prefer native Retold implementations for world simulation

- [ ] Evaluate a native path wear/road-formation system inspired by the useful idea behind The Roads More Travelled rather than depending on it permanently.
  - Repeated travel should be able to leave persistent physical traces in the world.
  - Consider contributions from players, villagers, patrols, hired allies, or other meaningful traffic instead of only the player.
  - Allow unused paths to recover naturally where appropriate.
  - If villages maintain roads in the future, connect that to actual village activity rather than generating decorative roads with no simulated cause.
  - Exact design, block transitions, rates, and whether this becomes a confirmed Retold feature remain undecided until a focused design pass.

- [x] Keep Nether portal environmental corruption/spread native to Retold.
  - It is part of Retold's energy/dimension lore rather than a generic visual effect.
  - Implemented as permanent, stage-independent Overworld drain around loaded active portals: a deterministic nearest-to-farthest death front advances toward 16 blocks for a standard 2×3 portal while a material-aware Nether-corruption front follows continuously at no more than half its radius. Each extra interior portal block adds one outer-radius block, capped at 48; the inner radius remains half. Slow background progress and a capped pulse after every successful traveler advance the two fronts. Loaded lava sources throughout the full outer sphere linearly reduce both forms of work, reaching zero when their count matches or exceeds the portal's interior area; flowing lava does not count.
  - Retold controls affected and immune blocks through semantic tags, uses only vanilla Coarse Dirt/Netherrack/Blackstone/Crimson targets, never creates Soul Sand, Soul Soil, fire, magma, or lava, and routes every edit through world protection without force-loading chunks.

- [ ] Prefer native Retold logic for environmental changes that interact with world state or society.
  - candidate examples: limited block aging/weathering, vegetation reclaiming abandoned areas, village maintenance/repair, persistent animal traces, dens/nests, and meaningful snow accumulation
  - only implement the subset that supports Retold's design; do not recreate a large general-purpose weathering mod feature-for-feature

- [ ] If seasons become part of Retold, design them as a Retold-owned world system rather than inheriting another mod's complete gameplay model.
  - Seasons would need deliberate interaction with world stages, farming, animals, weather, daylight, and unloaded-world simulation.
  - The decision to add seasons at all remains separate from this ownership rule.

- [ ] Keep gameplay-level sound perception native when it affects AI or discovery.
  - Example: if the C418/music-disc creature or another mob tracks propagated sound, Retold should own the gameplay query/model.
  - A sound-rendering mod may still provide reverb/occlusion for the player independently.

### Prefer external specialist companion mods

Do not spend Retold development time recreating mature specialist systems unless Retold later needs behavior they fundamentally cannot provide.

- Distant Horizons/Voxy or other LOD rendering
- Sodium and other rendering/performance optimizations
- shader loaders and shader rendering infrastructure
- Sound Physics-style acoustic rendering when it is only presentation
- ambient-audio systems
- dynamic-light rendering
- purely cosmetic effects such as falling leaves

For these, prefer optional integration, Retold-specific presets, or seamless configuration/UI integration over forks. Fork only when a required Retold behavior cannot be achieved through configuration, API hooks, or a reasonable upstream contribution.

### Implementation philosophy for ideas borrowed from other mods

- [ ] Do not clone another mod wholesale merely because one of its ideas fits Retold.
- [ ] Identify the smallest underlying design idea that supports Retold, then implement a Retold-specific version integrated with existing systems.
- [ ] Avoid inheriting unrelated features, configuration complexity, or design assumptions from the inspiration mod.
- [ ] Keep native systems data-driven/configurable where that improves community compatibility without weakening Retold's intended defaults.
- [ ] Where feasible, expose the resulting native system through Retold's public API/tags so compatibility addons can participate without patching internals.

Examples of the intended distinction:

| Feature | Preferred ownership |
| --- | --- |
| Path formation / route wear | Retold-native candidate |
| Village road maintenance | Retold-native candidate |
| Nether portal corruption | Retold-native |
| Animals eating crops / ecosystem interactions | Retold-native |
| Animal tracks, dens, nests | Retold-native if added |
| Vegetation reclaim | Retold-native if added |
| Limited block aging/weathering | Retold-native if added |
| Snow accumulation with gameplay/world-state consequences | Retold-native if added |
| Seasons | Retold-native if adopted |
| Gameplay sound propagation used by AI | Retold-native |
| Dynamic lights | External companion |
| Falling leaves | External companion |
| Acoustic reverb/occlusion rendering | External companion |
| Ambient audio | External companion |
| Distant terrain LOD | External companion |
| General rendering/performance optimization | External companion |
| Shader infrastructure | External companion |

## Enough For Now

These areas are not finished forever, but the current direction is acceptable for now:

- current Stage 3 illager behavior, including the Stage 3-only raid-start gate, is enough for now
- Stage 3 should only remove/cleanse undead and zombified piglins for now, not broadly make the Overworld easier
- mansions and outposts should stay delayed to Stage 2 as currently designed
- Stage 2+ Iron Golem creation keeps vanilla's five-villager agreement and local recent-golem
  detection. Only Clerics, Librarians, Armorers, Toolsmiths, and Weaponsmiths may perform the
  magical construction for one emerald; all other professions, including Nitwits, are ineligible.
  A successful player-built Iron Golem costs five experience levels; Creative placement remains
  free.
- Adult Villagers may relight nearby dry weather-extinguished village torches in every stage.
  Most use ranged magic; Nitwits must path close and show a fake Flint and Steel interaction.
  This remains low-priority loaded-world maintenance and consumes no fuel item or tool durability.
- Current breedable animals reproduce after five continuous loaded minutes at full satisfaction;
  feeding only relieves hunger, interruptions reset readiness, and vanilla owns the actual birth.
  Keep the current eight-block mate range, one-minute retry, 40-hunger parent cost, and vanilla
  cooldown until natural population testing gives a concrete reason to tune them.

## Undecided

Do not implement these without asking the developer first:

- Stage 1 Wither/Nether star requirement before End access
- Desert Pyramids or additional pyramid boss tombs beyond the confirmed Jungle Pyramid Earth
  Labyrinth
- Nether dragon role in the ending
- Aender dragon role in the ending
- New Game+ / world ending ideas
- exact path/road formation design and whether it becomes a confirmed Retold gameplay feature
- exact Nether Remnant armor requirement (one qualifying piece or a full set)
- whether feeding a Slime to the maximum supported size should transform it into or summon a
  Slime boss; the boss identity, exact trigger, encounter behavior, and rewards are not designed

## Not Planned

Do not add these unless the developer changes direction:

- gamerule to restore the normal recipe book
- complete removal of elytra as an item
- complete code/entity removal of sniffers or endermites just because survival spawning is removed
- removal of Trail Ruins; they should remain part of normal world generation
- removal of fossils; they should remain part of normal world generation

## Roadmap Maintenance

Update this file when:

- the developer clarifies a design decision
- a high-priority item is implemented
- an undecided item becomes planned or dropped
- a planned item becomes explicitly not planned

Also update [`design_implementation_status.md`](design_implementation_status.md) and [`retold_design_risks.md`](retold_design_risks.md) when a roadmap change affects implementation status, design risks, or verification steps. Update [`retold_issues.md`](retold_issues.md) only for confirmed issues or failed tests.

## AI Agent Instructions

See the shared [AI Agent Instructions](README.md#ai-agent-instructions).
