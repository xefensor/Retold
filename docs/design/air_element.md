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

The Air Temple challenge structure is implemented as a Stage 2 locatable structure:

- `/locate structure retold:air_temple`
- Generates only in `frozen_peaks`, `jagged_peaks`, and `stony_peaks`.
- Uses one compact floating island with small satellite islands and a crater below.
- Samples the temple-center peak biome once and stores a frozen or stony palette kind in the structure piece.
- Builds an open tuff/copper tower with several floors, polished tuff pillars, chiseled tuff details, copper grates, copper bulbs, and cut copper accents.
- Creates one large horizontal wind zone covering the island and tower volume.

The wind zone changes direction every 400 ticks, cycles east, south, west, north, emits dense cloud particles around affected players, and allows upwind blocks to shield players. The `air_element` item still has no completed survival acquisition reward path.

## Air Element Item

The Air Element should eventually be one of the four accepted dragon egg ritual elements. After the player offers it to the dragon egg in Stage 2, the egg records Air as absorbed.

## Future Direction

The Air challenge still needs reward/boss integration before it is a complete element path.

Potential goals:

- connect the temple to `air_element` acquisition
- add boss or encounter logic if the design still needs it
- keep traversal readable instead of random
- preserve player-built wind protection
- avoid expensive worldgen or large block-copy systems
- avoid collapse/falling-block entity spam

## Out Of Scope For Current Build

- Air boss implementation.
- Falling-block island collapse.
- Final Air Element reward/acquisition flow.
