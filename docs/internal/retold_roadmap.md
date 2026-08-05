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
