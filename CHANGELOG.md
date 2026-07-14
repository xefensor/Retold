# Changelog

All notable player-facing and technical changes should be tracked here.

Each release should be readable in two passes:

- **Player-facing:** what a player should notice.
- **Technical:** what changed in systems, data, commands, docs, or implementation behavior.

## 0.1.0  - 2026-07-15

First internal build for the current Retold survival spine, focused on Stage 2 Water + Air progression.

### Player-Facing

- Added the three-stage Retold progression model.
- Killing the Ender Dragon now advances the world into Stage 2.
- Added the Water Element path through ocean monument / elder guardian progression.
- Added the Air Temple path in mountain peak biomes, including wind hazards, Breezes, and the WIP Gale Core boss.
- The dragon egg currently hatches with the implemented required elements: Water and Air.
- Hatching the dragon egg advances the world into Stage 3 and redirects normal End portal travel to the Aender.
- Entering the Aender through a redirected End portal now creates a regeneration-resistant obsidian arrival platform like the vanilla End.
- Added the Aender dimension with custom terrain, bright fixed-light feel, custom blocks, water behavior changes, stability foundation, Aender Eye, and Aender Chronolith.
- Added staged mob/world changes including Stage 2 undead sunlight changes, Stage 3 undead cleansing, Stage 3 zombified piglin blocking/cleansing, and Stage 3 living piglin support.
- Added Retold mob behavior foundations: hunger, homes/ranges, hunting, fleeing, herding, packs, regrouping, faction combat, and territory warnings.
- Added recipe knowledge and villager recipe teaching.
- Added rain-extinguished torches and relighting support.
- Added a bed night-skipping gamerule; valid daytime bed rest is allowed when night skipping is disabled.

### Technical

#### Progression

- Added persistent world stage data and client sync.
- Added `/retold stage` debug command support.
- Added dragon egg element ritual framework.
- Registered Water Element and Air Element progression items.
- Removed the temporary Nether Star dragon egg shortcut.
- Temporarily limited dragon egg ritual requirements to Water and Air until Fire and Earth paths exist.
- Added Stage 3 vanilla End ejection for normal survival flow while preserving command access to existing End builds.

#### Aender

- Added `retold:aender` dimension, dimension type, biome data, and custom terrain generation.
- Added vanilla-style obsidian platform generation for Stage 3 Aender portal arrivals, including regeneration support.
- Added Aender chunk volatility and stability foundation.
- Added Aender Stabilizer block.
- Added Aender water flow changes and Aender weather blocking.
- Added Aender Eye entity.
- Added Aender Chronolith time-control block.
- Added Aender block set: Aender Grass Block, Aender Soil, Aender Stone, Aender Log, Aender Leaves, and Aender Stabilizer.

#### Air Temple And Air Element

- Added Stage 2 Air Temple structure and `/locate structure retold:air_temple` support.
- Added Air Temple generation in frozen peaks, jagged peaks, and stony peaks.
- Added floating main island, satellite islands, crater, and open tuff/copper tower generation.
- Added persistent Air Temple wind source data and a wind zone covering the island/tower area.
- Added horizontal cycling wind directions, body-height wind push, nearby upwind block shielding, and observer-visible wind particles.
- Added wind immunity for creative players, spectators, Breezes, and the Gale Core.
- Added retrogen protection so wind and boss spawning do not activate when an edited/skipped Air Temple chunk did not actually generate the tower.
- Added Breeze spawning on Air Temple islands and tower floors.
- Added Gale Core boss entity, boss bar, Air Element drop, projectile deflection, wind immunity, fall-damage immunity, activation near the tower top, grounded phase, aerial phase, line-of-sight targeting, return-home behavior, wall pressure, and wind-charge block cracking/breaking.
- Added Gale Core duplicate-spawn protection after reload.

#### Ocean Monument And Water Element

- Added guaranteed Water Element drop from elder guardians.
- Added guardian mining-pressure behavior.
- Added elder guardian boss/support behavior for the Water Element path.
- Added mining fatigue and blocked-hit pressure hooks for monument gameplay.

#### Mobs, Factions, And Stages

- Added Enderman behavior changes after dragon death and visual asset support for Retold eyes/skin variants.
- Added Retold mob behavior profile system.
- Added faction combat helpers.
- Added Nether Remnants territory warning support for piglins, brutes, and blazes.
- Added Illager territory warning support for outposts and mansions.
- Added guardian, elder guardian, undead, skeleton, phantom, golem, animal, predator, and faction behavior hooks.

#### Worldgen And Structure Rules

- Added delayed structure framework for Stage 2 mansions and pillager outposts.
- Added chunk edit tracking for delayed/retrogen-sensitive structures.
- Added structure mob suppression while delayed structures are inactive.
- Added End gateway generation cancellation.
- Added End City biome tag override to remove normal End City generation.
- Added outer End terrain masking foundation.
- Added generated End sky seed/system support and `/retold sky randomize` debug support.

#### Discovery, Recipes, And Villagers

- Added recipe knowledge persistence and recipe book knowledge gating.
- Added advancement visibility hiding foundation.
- Added villager recipe teaching framework and profession-based teaching data.
- Added emerald-cost recipe teaching flow.
- Added custom villager teaching preview/network support.

#### Environment And QoL

- Added Extinguished Torch, Extinguished Wall Torch, Extinguished Soul Torch, Extinguished Soul Wall Torch, Extinguished Copper Torch, and Extinguished Copper Wall Torch.
- Added torch relighting support through tagged igniter items.
- Added `retold:do_bed_night_skipping` gamerule support.

#### Metadata And Documentation

- Changed project version from `1.0.0` to `0.1.0` for the first internal build.
- Changed NeoForge Mods screen display name to `Minecraft: Retold`.
- Replaced example NeoForge mod metadata with a Retold build description.
- Added README status/development notes and internal design/status documentation.
- Renamed the generated documentation folder from `docs/ai_generated` to `docs/internal`.
- Consolidated standalone Air Element, release-note, and mob-AI checklist docs into the remaining internal reference docs.
- Centralized the full AI-agent instructions in `docs/internal/README.md`; other internal docs now link back to that shared section.
- Updated internal docs to describe the Air Temple/Gale Core path as implemented but still WIP.
- Updated the changelog format so release entries can serve both players and technical readers.

### Known WIP

- This build is not feature-complete.
- Fire and Earth element paths are not implemented yet.
- The Gale Core encounter is playable but still being tuned.
- Aender late-game rewards, Aender travel scaling, tool/combat reworks, village reputation, and broader removal/rework passes are still unfinished.
