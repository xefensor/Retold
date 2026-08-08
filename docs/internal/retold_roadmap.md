# Retold Roadmap

> Developer-maintained, AI-assisted roadmap. This file is meant for human developers and future AI coding agents. It summarizes active design direction, not every historical idea from the original design document.

## Current Direction

Retold is still built around:

- three world stages
- four-element progression in any order
- Aender replacing normal late End access while vanilla End remains command-accessible
- horizontal Aender portals using implemented 8:1 Overworld/Aender travel scaling
- elytra remaining as an item but not being survival-obtainable through End Cities
- recipe knowledge and villager teaching instead of a vanilla-style recipe-book restore
- Retold mob AI driven by species, faction, profile, state, and nearby world situation
- survival worldgen/spawn removal for some modern content instead of necessarily deleting all code support
- beds not skipping night
- rain extinguishing normal torches

## High Priority

These are the strongest next design-aligned areas:

1. Finish the four-element progression model.
2. Add missing Fire and Earth element item/challenge paths, verify the initial cartographer Air Temple discovery map, and continue tuning the Air Temple/Gale Core path.
3. Decide whether Stage 1 needs Wither/Nether star End portal activation.
4. Add remaining Aender in-dimension teleportation and late-game travel/building rewards.
5. Replace the provisional `dev_aender_portal_frame` name/assets when the final portal-frame design is chosen.
6. Audit and enforce survival removal for End Cities, outer End progression, Ancient Cities, Deep Dark/Warden, Trial Chambers, trail ruins, and fossils; keep the implemented Sniffer and Endermite removals regression-tested.
7. Naturally verify the implemented village-reputation loop: generated/Villager-produced storage,
   Farmer-planted crops, profession-tended livestock, witness sight, trade prices, and Iron Golem
   hostility in ordinary, multiplayer, dedicated-server, and existing villages.
8. Naturally verify hunger-satisfaction breeding across representative ordinary, aquatic, Nether,
   egg-laying, pregnant, mixed-equine, and tamed animals, including population growth and save/load.
9. Implement the confirmed mob/faction behavior contract in dependency order: global target safety and affiliations, existing species families, society/hiring features, then the bounded ecosystem simulation.
10. Continue AI validation and performance profiling against real loaded-mob tests.

## Planned Systems

These are still planned but need feature-specific design before implementation:

- full tools, armor, ores, and station progression rework beyond the initial Aenderite material foundation
- enchanting rework
- mending removal
- sword/shield combat rework
- Stage 3 piglin/pigman hiring or follower behavior
- Nether portal spread as portal energy draining surroundings
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
- bounded unloaded-ecosystem catch-up for hunger, feeding, predation, breeding, carrying capacity, migration, and spawning

The detailed confirmed behavior contract is maintained in
[`retold_mob_ai_system.md`](retold_mob_ai_system.md#confirmed-gameplay-contract). Implementation
status must remain explicit in [`design_implementation_status.md`](design_implementation_status.md);
the contract being confirmed does not mean it is implemented.

## Mod Compatibility And Community Integration TODO

Retold should remain usable as a standalone overhaul, but its major systems should be extensible enough that modpack authors and other mods can integrate with them without Retold carrying hard dependencies on every supported project.

General rule:

- prefer data, tags, stable public hooks, and small optional adapters over hard-coded checks for individual mods
- distinguish **compatibility** (both mods work together), **integration** (Retold understands the other mod's systems), and **balance support** (the other mod preserves Retold's intended progression); broad compatibility is desirable, integration should be selective, and balance support should not be promised by default
- avoid direct dependencies unless an integration genuinely cannot be implemented safely as optional support
- third-party integrations should go through stable Retold-owned interfaces rather than writing internal saved data or calling implementation details directly

### High-priority compatibility work

- [ ] Make faction membership data-driven instead of relying only on hard-coded vanilla entity IDs in `RetoldFactionMembers`.
  - Allow exact entity IDs and/or entity tags to opt into Retold factions.
  - Preserve Retold's built-in vanilla defaults.
  - Make it possible for a datapack or compatibility addon to classify a modded mob as Undead, Illager, Nether Remnant, Village Defender, Ender, etc. without Java patches in Retold.
  - Keep conditional relations such as Witch raid cooperation expressible without turning every special case into a generic faction member.

- [ ] Audit Retold for places where standard Minecraft/NeoForge common tags should be used instead of exact vanilla item/block checks.
  - Prefer common material tags where the gameplay meaning is genuinely "any valid material of this type".
  - Keep Retold-specific semantic tags for concepts owned by Retold.
  - Add extension tags where useful for modpack authors, such as valid torch igniters, weak mob barriers, portal-related materials, or other future Retold systems.

- [ ] Add a stable recipe-visibility/knowledge hook shared by all recipe UIs.
  - Vanilla recipe-book behavior, EMI, JEI, and future viewers should be able to ask the same Retold authority whether a recipe is known/visible to a player.
  - Unknown Retold recipes must not become spoilers merely because a recipe-viewer mod is installed.
  - Consider separate states where an item/output is known but its exact recipe is still undiscovered.

- [ ] Add optional EMI compatibility.
  - Hide or filter undiscovered recipes according to Retold recipe knowledge.
  - Do not require EMI for normal Retold operation.

- [ ] Add optional JEI compatibility.
  - Match the same recipe-discovery rules as vanilla/EMI rather than creating a separate knowledge model.
  - Do not require JEI for normal Retold operation.

- [ ] Create a generic Retold world-protection permission layer before adding claim-mod-specific adapters.
  - Centralize checks such as `canMobBreak`, `canWorldModify`, `canStructureGenerate`, and `canPortalCreate` (exact API names TBD).
  - Route Retold-owned world mutation through this layer where practical: Aender chunk replacement/regeneration, counterpart portal construction, retrogen/delayed structures, Gale Core block breaking, and future environmental transformations.
  - Default behavior without a protection integration should preserve current Retold behavior.
  - Later adapters can map this layer to popular claims/protection systems without scattering mod checks across gameplay code.

### Data-driven mob compatibility

- [ ] Keep modded-mob AI participation opt-in/configurable through Retold's existing datapack mob-profile system.
  - Document how compatibility datapacks can assign Retold profiles to third-party entity IDs.
  - Ensure unknown entities are not accidentally taken over by Retold AI merely because they extend a vanilla class.

- [ ] Add explicit opt-out/extension mechanisms where the current profile model is insufficient.
  - Consider Retold tags/data for AI-managed, AI-excluded, territory-excluded, or faction-excluded entities if those distinctions are needed in practice.
  - A mod with sophisticated custom AI must be able to coexist without Retold overriding behavior it does not own.

- [ ] Make faction + profile composition work cleanly for third-party mobs.
  - A compatibility pack should be able to say, for example, that a modded creature uses a predator/grazer profile while separately belonging to an existing Retold faction.
  - Preserve the design rule that profile describes daily life while faction describes diplomacy/relationships.

### Recipe and machine compatibility

- [ ] Generalize recipe-discovery handling so unknown third-party recipe types fail open safely instead of being unintentionally blocked or spoiled.

- [ ] Provide an extension point for integrations to register additional recipe types with Retold's discovery system.
  - This should support modded processing systems such as crushers, mixers, presses, sawmills, alloying, etc. without Retold knowing every machine mod directly.
  - Pack authors should be able to decide whether a third-party recipe type participates in Retold discovery.

- [ ] Use a large automation/processing mod such as Create as a compatibility stress test, without promising Create-specific balance support.
  - Test custom recipes, machines, automated crafting, moving blocks/contraptions, and world interaction for assumptions in Retold.
  - Add bespoke compatibility only when a concrete conflict justifies it.

### Public Retold integration API

- [ ] Design a deliberately small, stable public API package, tentatively `cz.xefensor.retold.api`.

Potential API surfaces to evaluate:

- [ ] World stage read access and safe stage transition hooks (`RetoldStages` or equivalent).
- [ ] Faction registration/query hooks (`RetoldFactions`).
- [ ] Mob-profile integration/query hooks (`RetoldMobProfiles`).
- [ ] Recipe knowledge and visibility (`RetoldRecipeKnowledge`).
- [ ] World-protection/world-mutation permission hooks (`RetoldWorldProtection`).
- [ ] Aender queries/events that third-party integrations may legitimately need (`RetoldAender`).

API rules:

- [ ] Do not expose mutable implementation storage when a manager already owns side effects.
  - Example: external code should never directly edit Retold world-stage saved data; stage changes must continue through the stage manager/official API so synchronization and transition side effects occur.
- [ ] Prefer events/queries over exposing internal classes.
- [ ] Document API stability expectations before declaring interfaces public.
- [ ] Keep optional integrations isolated so a missing third-party mod never causes classloading failures.

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

## Enough For Now

These areas are not finished forever, but the current direction is acceptable for now:

- villages only need current distance/scarcity work for now
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
- jungle/desert pyramids as boss tombs
- Nether dragon role in the ending
- Aender dragon role in the ending
- New Game+ / world ending ideas
- travel-road style features
- exact Nether Remnant armor requirement (one qualifying piece or a full set)
- exact time cap and calculation granularity for unloaded-ecosystem catch-up
- whether feeding a Slime to the maximum supported size should transform it into or summon a
  Slime boss; the boss identity, exact trigger, encounter behavior, and rewards are not designed

## Not Planned

Do not add these unless the developer changes direction:

- gamerule to restore the normal recipe book
- complete removal of elytra as an item
- complete code/entity removal of sniffers or endermites just because survival spawning is removed

## Roadmap Maintenance

Update this file when:

- the developer clarifies a design decision
- a high-priority item is implemented
- an undecided item becomes planned or dropped
- a planned item becomes explicitly not planned

Also update [`design_implementation_status.md`](design_implementation_status.md) and [`retold_design_risks.md`](retold_design_risks.md) when a roadmap change affects implementation status, design risks, or verification steps. Update [`retold_issues.md`](retold_issues.md) only for confirmed issues or failed tests.

## AI Agent Instructions

See the shared [AI Agent Instructions](README.md#ai-agent-instructions).
