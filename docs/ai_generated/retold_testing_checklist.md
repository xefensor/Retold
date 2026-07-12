# Retold Testing Checklist

> AI-generated testing checklist. This file is meant for human maintainers and future AI coding agents. It lists practical in-game checks for validating current systems and rows marked `needs verification` in [`design_implementation_status.md`](design_implementation_status.md).

AI agents: when a feature changes, add or update the relevant checks here. Do not mark a design item complete only because code exists; verify the in-game behavior when the row depends on gameplay feel, worldgen, rendering, or AI timing. If a test fails, add or update the bug in [`retold_known_issues.md`](retold_known_issues.md). If a test exposes a missing planned feature or ambiguous design, update [`retold_design_risks.md`](retold_design_risks.md).

## Verified Release Smoke Tests

### 2026-07-12 Alpha Spine Smoke Test

Maintainer-reported status: passed before the Nether Star dragon egg shortcut was removed.

Verified path:

- Fresh world starts in Stage 1.
- Dragon kill advances to Stage 2.
- Stage 2 syncs after relog / multiplayer join.
- Water element can be obtained.
- Dragon egg accepts water element and persists that state.
- Temporary Nether Star shortcut hatched the egg at the time of testing.
- Stage 3 starts.
- Players in vanilla End are ejected.
- Overworld End portal sends player to Aender.
- Aender loads, saves, and survives restart.

Follow-up: rerun this smoke test after fire, earth, and air element paths are implemented, because Stage 3 no longer has a temporary survival shortcut.

## Stage Progression

- Start a fresh world and confirm default stage is Stage 1 with `/retold stage get`.
- Kill the Ender Dragon and confirm the world advances to Stage 2.
- Confirm Stage 2 sync reaches clients without reconnecting.
- Use the dragon egg ritual with `water_element` and confirm the offered element state is saved.
- Use the dragon egg ritual with `air_element` and confirm the offered element state is saved.
- Confirm `nether_star` does not hatch the egg.
- After fire, earth, and air element paths exist, offer all four elements and confirm the egg hatches.
- Confirm Stage 3 starts after the egg hatch path.
- Confirm players in vanilla End are ejected when Stage 3 begins.
- Confirm Overworld End portal travel goes to Aender in Stage 3.
- Confirm vanilla End remains reachable with commands for old builds.
- Use `/locate structure retold:air_temple` and confirm the Air Temple placeholder can be located.
- At an Air Temple placeholder, confirm crosswind, updraft, and interior gust zones push entities and that solid windbreaks reduce horizontal wind.

## Aender

- Enter Aender in Stage 3 and confirm stable spawn near the configured entry position.
- Confirm Aender has fixed daylight/light feel and no weather.
- Confirm water flows faster/farther than vanilla.
- Try placing lava in Aender and record result. Current docs say this is planned but no implementation was found.
- Place an Aender stabilizer and confirm the stable chunk region persists.
- Break a stabilizer and confirm unstable regions can regenerate/forget as designed.
- Confirm forcefield visuals appear around stable regions and do not spam or flicker.
- Confirm Aender chunks that should be unstable are not saved as permanent terrain.
- Profile Aender with multiple players/chunks loaded if stability or regeneration behavior changes.

## End Worldgen

- Generate fresh End terrain and confirm outer End islands are removed or masked as expected.
- Confirm End gateways do not spawn after dragon defeat.
- Locate fresh End City generation attempts and confirm End Cities do not generate.
- Confirm elytra remains an item but is not obtainable in survival through End Cities.
- Test existing-world behavior separately from fresh-world behavior.

## Structures And Worldgen

- In Stage 1, verify mansions and pillager outposts are delayed or suppressed.
- Advance to Stage 2 and verify delayed mansions/outposts can appear or retrogen as intended.
- Confirm structure mob suppression works while delayed structures are not active.
- Audit Ancient Cities, Deep Dark/Warden, Trial Chambers, trail ruins, and fossils after removal work is implemented.
- Verify stronghold count after the planned limit-to-3 work is implemented.

## Mob AI And Territory

- In a bastion, verify piglins and brutes warn before attack when the player has not attacked them.
- In a fortress, verify piglins/brutes/blazes use Nether Remnants territory warning where applicable.
- In an outpost and mansion, verify illagers warn before attacking players in territory.
- Confirm faction assist does not bypass warning before ATTACK level.
- Confirm direct retaliation still works when a player attacks a guard.
- Confirm creative and spectator players are dropped as valid targets.
- Use `/retoldbehavior warning` to inspect territory context, target, warning level, suspicion, pulse timing, and debug counters.
- Use `/retoldbehavior get`, `/retoldbehavior nearby`, and overlay mode to confirm short warning text remains useful.
- Test with about 100 loaded mobs and profile TPS/MSPT before and after AI changes.
- Test with mixed mob groups so caches, LOD, and work budgets are exercised under real conditions.

## Animal And Predator Behavior

- Verify hunger rises over time for mobs that should have hunger.
- Verify grazers use herd range, graze, flee, and regroup.
- Verify small foragers use species homes such as roosts or warrens.
- Verify wolves form packs, hunt, return, and defend dens/owners.
- Verify foxes/cats/ocelots use solo hunting/territory behavior.
- Verify dolphins use pod behavior and hunt fish.
- Verify special foragers keep species-specific behavior without breaking vanilla-special mechanics.

## Recipe Knowledge And Villagers

- Confirm recipes are hidden until known or taught.
- Confirm known recipe data persists per player.
- Confirm villager teaching offers profession-appropriate recipes.
- Confirm teaching costs and preview payloads work in multiplayer.
- Confirm there is no gamerule to restore the normal recipe book.
- Confirm advancement hiding does not hide critical progression feedback that the player still needs.

## Environment And QoL

- Confirm rain extinguishes normal torches and creates extinguished torch blocks.
- Confirm Aender torch rain behavior matches current design and implementation.
- Confirm beds do not skip night.
- After bed healing is added, confirm healing consumes hunger.
- After long death-drop despawn is added, compare despawn time against vanilla 5 minutes.
- After Nether portal spread is added, confirm spread is bounded and communicates energy drain clearly.

## Debug Commands

Useful commands while testing:

```mcfunction
/retold stage get
/retold stage set <1-3>
/retoldbehavior get
/retoldbehavior nearby
/retoldbehavior warning
/retoldbehavior toggle overlay
```

## Test Result Maintenance

When a test reveals a confirmed bug, add it to [`retold_known_issues.md`](retold_known_issues.md). When a test reveals an open design risk or missing planned feature, add it to [`retold_design_risks.md`](retold_design_risks.md). When a row is fully verified, update [`design_implementation_status.md`](design_implementation_status.md).
