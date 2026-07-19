# Changelog

All notable player-facing and technical changes should be tracked here.

Each release should be readable in two passes:

- **Player-facing:** what a player should notice.
- **Technical:** what changed in systems, data, commands, docs, or implementation behavior.

## Next - Unreleased

### Player-Facing

- Updated packaged mod metadata to use Retold branding, the current generated version, and working project and issue-tracker links.
- Fixed territory suspicion carrying process-wide save and decay timing between worlds; reputation now belongs to the current saved world.

### Technical

- Added separate MIT code and protected creative-asset licenses, including permission to redistribute the complete, unmodified Retold JAR in modpacks.
- Added structured bug and suggestion forms, a pull-request template, and monthly Gradle and GitHub Actions dependency checks.
- Added SHA-256 checksum assets to the release workflow.
- Moved territory reputation into versioned Minecraft `SavedData` with safe one-time migration and retained backups for legacy JSON data.

## 0.2.0 - 2026-07-18

Feature build focused on bidirectional Aender portal travel, more reliable Aender terrain transitions, and stronger foundations for Retold's mob AI and territory systems.

### Player-Facing

- Tuned the Air Temple Gale Core encounter: the boss now roams slightly while idle, aggroes when damaged by a valid player even outside its normal activation range, no longer deflects projectiles during phase two, and returns to the top tower area instead of a single exact block.
- Lava poured into the Aender now vaporizes like water in the Nether.
- Added bidirectional horizontal Aender portals. Their provisional frame block generates in Aender islands, supports rectangular interiors from 3x3 through 21x21 blocks, and activates when the final frame block completes the ring.
- Added 8:1 Aender travel scaling: Overworld horizontal coordinates are multiplied by eight when entering the Aender and divided by eight when returning.
- Added safe automatic 3x3 counterpart portals when no nearby destination portal exists.
- Survival and adventure players now charge an Aender portal for at least four seconds with portal distortion and ambient sound; creative and spectator travel remains immediate by default.
- Aender terrain now prepares during the survival portal charge and finishes before arrival, reducing visible chunk-by-chunk regeneration.
- Improved volatile Aender reality changes so unstabilized terrain consistently rerolls after the dimension becomes empty while stabilized chunks remain persistent.
- Fixed hostile spiders failing to acquire and attack valid nearby players in darkness; spiders also retaliate correctly when attacked.

### Technical

- Updated Gale Core targeting, idle movement, phase-two projectile deflection, and return-home logic while preserving existing saved home-position data.
- Added Aender lava vaporization to shared bucket emptying behavior.
- Added `AenderPortalBlock`, `AenderPortalFrameBlock`, `AenderPortalShape`, `AenderPortalData`, `AenderPortalLogic`, and the provisional `retold:dev_aender_portal_frame` block/assets/loot data.
- Added destination portal indexing, nearby portal validation, safe portal creation, world-border clamping, and Overworld/Aender coordinate conversion.
- Added synchronous arrival-view preparation plus asynchronous portal-ticket warm-up during the survival charge, capped at 16 refreshed chunks or 8 ms of main-thread work per tick.
- Changed empty-dimension volatility resets to occur once when the last player leaves, preventing repeated reality changes from invalidating portal warm-up work.
- Added generation signatures and synchronous stale-chunk regeneration on load/arrival to prevent mixed-reality chunk seams during rapid dimension travel.
- Expanded procedural island bounds to cover the full coast-warp reach, fixing terrain clipped into large flat walls at chunk boundaries.
- Replaced full-height per-block stale-chunk clearing with section-level replacement and fresh heightmap/light-state updates.
- Updated the README and internal architecture, implementation-status, roadmap, and design-risk docs for the completed portal/scaling work and remaining verification needs.
- Added JUnit 5 coverage for deterministic behavior and expanded NeoForge GameTests; CI now runs both unit tests and the GameTest server.
- Split the mob behavior package by subsystem ownership and moved event registration into explicit subsystem modules.
- Reworked territory escalation into an explicit state machine covering observation, warnings, attacks, and cooldown.
- Moved mob behavior profiles from hardcoded Java definitions into reloadable JSON data.
- Fixed controlled spider combat targeting and added regression GameTests for darkness-based player aggression and retaliation.

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
- Changed NeoForge Mods screen display name to `Retold`.
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
