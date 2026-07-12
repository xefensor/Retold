# Retold Roadmap

> AI-generated roadmap. This file is meant for human maintainers and future AI coding agents. It summarizes active design direction, not every historical idea from the original design document.

AI agents: read [`README.md`](README.md), [`design_implementation_status.md`](design_implementation_status.md), [`retold_design_risks.md`](retold_design_risks.md), [`retold_known_issues.md`](retold_known_issues.md), and the relevant system docs before implementing. Ask the maintainer design or implementation questions before coding if a roadmap item can be interpreted in more than one reasonable way.

## Current Direction

Retold is still built around:

- three world stages
- four-element progression in any order
- Aender replacing normal late End access while vanilla End remains command-accessible
- elytra remaining as an item but not being survival-obtainable through End Cities
- recipe knowledge and villager teaching instead of a vanilla-style recipe-book restore
- Retold mob AI driven by species, faction, profile, state, and nearby world situation
- survival worldgen/spawn removal for some modern content instead of necessarily deleting all code support
- beds not skipping night
- rain extinguishing normal torches

## High Priority

These are the strongest next design-aligned areas:

1. Finish the four-element progression model.
2. Replace the `nether_star` dragon egg shortcut with real element completion.
3. Add missing fire, earth, and air element item/challenge paths.
4. Decide whether Stage 1 needs Wither/Nether star End portal activation.
5. Implement Aender 8:1 Overworld travel scaling.
6. Implement the Aender lava placement ban.
7. Add Aender teleportation and late-game travel/building rewards.
8. Audit and enforce survival removal for End Cities, outer End progression, Ancient Cities, Deep Dark/Warden, Trial Chambers, trail ruins, fossils, sniffers, and endermites.
9. Add village reputation for stealing, crop breaking, and animal killing.
10. Continue AI validation and performance profiling against real loaded-mob tests.

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

Do not implement these without asking the maintainer first:

- Stage 1 Wither/Nether star requirement before End access
- jungle/desert pyramids as boss tombs
- Nether dragon role in the ending
- Aender dragon role in the ending
- New Game+ / world ending ideas
- travel-road style features

## Not Planned

Do not add these unless the maintainer changes direction:

- gamerule to restore the normal recipe book
- complete removal of elytra as an item
- complete code/entity removal of sniffers or endermites just because survival spawning is removed

## Roadmap Maintenance

Update this file when:

- the maintainer clarifies a design decision
- a high-priority item is implemented
- an undecided item becomes planned or dropped
- a planned item becomes explicitly not planned

Also update [`design_implementation_status.md`](design_implementation_status.md), [`retold_design_risks.md`](retold_design_risks.md), and [`retold_testing_checklist.md`](retold_testing_checklist.md) when a roadmap change affects implementation status, design risks, or verification steps. Update [`retold_known_issues.md`](retold_known_issues.md) only for confirmed bugs or failed tests.
