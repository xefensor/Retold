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
2. Add missing Fire and Earth element item/challenge paths, and continue tuning the Air Temple/Gale Core path.
3. Decide whether Stage 1 needs Wither/Nether star End portal activation.
4. Add remaining Aender in-dimension teleportation and late-game travel/building rewards.
5. Replace the provisional `dev_aender_portal_frame` name/assets when the final portal-frame design is chosen.
6. Audit and enforce survival removal for End Cities, outer End progression, Ancient Cities, Deep Dark/Warden, Trial Chambers, trail ruins, fossils, sniffers, and endermites.
7. Add village reputation for stealing, crop breaking, and animal killing.
8. Continue AI validation and performance profiling against real loaded-mob tests.

## Planned Systems

These are still planned but need feature-specific design before implementation:

- full tools, armor, ores, and station progression rework
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
- vex nerf

## Enough For Now

These areas are not finished forever, but the current direction is acceptable for now:

- villages only need current distance/scarcity work for now
- current Stage 3 illager behavior is enough for now
- Stage 3 should only remove/cleanse undead and zombified piglins for now, not broadly make the Overworld easier
- mansions and outposts should stay delayed to Stage 2 as currently designed

## Undecided

Do not implement these without asking the developer first:

- Stage 1 Wither/Nether star requirement before End access
- jungle/desert pyramids as boss tombs
- Nether dragon role in the ending
- Aender dragon role in the ending
- New Game+ / world ending ideas
- travel-road style features

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
