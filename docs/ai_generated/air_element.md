# Air Element Design

## Purpose

The Air Element is one of the four Stage 2 dragon egg elements. Its role is to test freedom, height, movement, and control without turning the challenge into a simple combat gate.

Air should feel different from Water. Water is about adaptation and community pressure through the ocean monument path. Air is about movement through danger, using space well, and surviving unstable conditions.

## Element Philosophy

Retold's four elements should each express a different idea:

- Fire: power, will, desire, and force.
- Earth: substance, endurance, persistence, and weight.
- Air: freedom, detachment, motion, risk, and humor.
- Water: change, adaptation, community, and continuity.

Air should not reward brute force alone. It should reward players who stay mobile, read the environment, build smart protection, and adapt quickly.

## Current Implementation

The Air Temple challenge structure is implemented as a Stage 2 locatable structure and the Air Element now has a WIP boss/reward path:

- `/locate structure retold:air_temple`
- Generates only in `frozen_peaks`, `jagged_peaks`, and `stony_peaks`.
- Uses one naturalized floating island with small satellite islands and a carved crater below.
- Samples the temple-center peak biome once and stores a frozen or stony palette kind in the structure piece.
- Builds an open tuff/copper tower with several floors, polished tuff pillars, chiseled tuff details, copper grates, copper bulbs, and cut copper accents.
- Creates one large horizontal wind zone covering the island and tower volume.
- Spawns vanilla Breezes on the islands at runtime; Breezes are intentionally immune to the temple wind.
- Spawns the `Gale Core` boss near the top of the tower at runtime.

The wind zone changes direction every 400 ticks, cycles east, south, west, north, emits dense cloud particles for players inside the zone regardless of game mode, and allows upwind blocks to shield entities. Wind shielding is limited to blockers close enough to matter, so a wall only protects a short distance behind it.

Wind currently affects all eligible entities in the active temple volume except creative players, spectators, no-physics entities, Breezes, and the Gale Core. Player velocity is explicitly synced when pushed.

## Gale Core Boss State

The Gale Core is implemented but not finished. Current behavior:

- Custom large Breeze-like entity with a boss bar.
- Drops `air_element` on death.
- Does not take fall damage.
- Is not affected by Air Temple wind.
- Activates when a survival/adventure player reaches the top floor near it.
- Phase 1 uses mostly vanilla Breeze-style ground behavior.
- At half health, Phase 2 switches to custom aerial movement.
- Phase 2 keeps more distance from the player, prefers higher positions, avoids arena edges, avoids clipping into blocks, and uses a less-perfect flight pattern instead of a fixed circle.
- If the target or boss leaves the combat area, the boss clears aggro and flies back to its stored tower-top home.
- The combat area is based on the Air Temple wind volume plus a 20-block margin.
- Gale Core wind-charge impacts do not deal health damage, but they create a block-breaking splash effect that slowly cracks nearby breakable blocks and breaks them after repeated hits.

The block-breaking splash is deliberately bounded:

- Only Gale Core-owned `BreezeWindCharge` impacts apply it.
- It affects a small number of nearby blocks per impact.
- It skips air, fluids, block entities, unbreakable blocks, and very hard blocks.
- Blocks break without drops to avoid item spam.
- Crack overlays decay if the block is not hit again.

## Air Element Item

The Air Element should eventually be one of the four accepted dragon egg ritual elements. After the player offers it to the dragon egg in Stage 2, the egg records Air as absorbed.

## Future Direction

The Air challenge has a playable boss/reward spine, but it is still WIP and needs gameplay tuning before it should be treated as finished.

Potential goals:

- tune Gale Core attacks, movement, pacing, and phase readability
- decide whether the block-breaking splash should break temple blocks, player blocks, or only selected block tags
- add stronger telegraphs/particles/sounds for the Gale Core block-breaking attack
- review the Air Temple tower layout against the boss movement and wind hazard
- tune Breeze island spawns and whether they should respawn
- keep traversal readable instead of random
- preserve player-built wind protection
- avoid expensive worldgen or large block-copy systems
- avoid collapse/falling-block entity spam

## Out Of Scope For Current Build

- Falling-block island collapse.
- Claiming the Gale Core encounter is complete/final.
- Fire and Earth element paths.
