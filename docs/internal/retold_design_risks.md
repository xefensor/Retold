# Retold Design Risks And Open Decisions

> Developer-maintained, AI-assisted design-risk tracker. This file is for planned gaps, undecided design items, implementation watchpoints, and risky future work. Confirmed issues belong in [`retold_issues.md`](retold_issues.md).

## Open Design Risks

| Area | Risk / open item | Current note |
| --- | --- | --- |
| Progression | Stage 1 Wither/Nether star End access gating is undecided. | Do not implement until developer decides. |
| Elements | Fire and Earth still need full item/challenge paths. | Water has the ocean monument path. Air has a WIP Air Temple/Gale Core acquisition path and a provisional Stage 2 cartographer map with an exact marker. Deterministic GameTests cover the map trade's stage gate and uniqueness plus critical encounter state transitions. The basic in-game map path was developer-verified on 2026-07-22; wider seed, upgraded-world retrogen, dedicated-server, and multiplayer verification remains. Encounter pacing, readability, and balance still need in-game tuning. |
| Air Temple discovery | An explorer map could select a delayed Air Temple start that retrogen later suppresses because an upgraded-world chunk was edited. The initial synchronous structure search and biome-preview rendering may also cause a noticeable interaction stall. | Basic cartographer interaction and exact-map behavior were developer-verified on 2026-07-22. Failed searches are throttled for five minutes per cartographer. In wider fresh and upgraded test worlds, measure the first interaction and confirm the marker leads to a generated tower, including a world where a nearby deferred start is permanently skipped. |
| Aender | The horizontal portal, 8:1 scaling, counterpart creation, persisted global/regional and per-chunk reality state, generator V3, player-scoped regional volatility, player-centered blank-then-regenerate transitions, adaptive tick-time budgets, indefinite safe-core arrival gate, living terrain blocks, complete wood family, experimental per-island Aender Desert, Aenderite deposits, and expanded topology/decorations need fresh-world, existing-world, rapid re-entry, high-view-distance, and multiplayer verification. | JUnit tests cover order-independent terrain interval composition, equivalent cached/direct vertical biome selection, biome-owned archetype families, relief differences, erosion silhouettes, satellite distribution, vegetation clearings, rare-formation planning/intersection, deterministic Aenderite size/underside bias/chunk-border continuity, adaptive budget scaling, active-region watcher transitions, and the portal transition gate. Deterministic GameTests cover shape lifecycle, scaling, counterpart creation/indexing, living terrain/wood/desert/ore survival data and vanilla creative-tab integration, reality/stability serialization, chunk attachment state/cache restoration, entity cleanup, blank transitions, and stable-versus-volatile policy. The GameTest server does not load the custom Aender dimension, so still verify crash recovery, V1/V2/V3 selection, desert frequency/palette/atmosphere, all island archetypes/undersides/detached satellites, Aenderite frequency/exposure and mining feedback, warped satellite coastlines, erosion openings, split ravines, clustered decorations, formation frequency, vertical biome transitions, biome replacement packets, shared and unattended multiplayer regions, ring regeneration, tick-load throttling, V3 borders, arrival safety, and client models in normal clients. Repeat the TPS-graph scenario to measure remaining per-chunk spikes after biome-column caching, paced blanking, measured-cost admission, and removal of synchronous stale-load/portal-search regeneration. `dev_aender_portal_frame` and all credited AI-generated Aender textures remain provisional. |
| Aender | A separate in-dimension teleportation network and late-game rewards are still planned. | Cross-dimension travel is implemented; remaining travel/reward design needs clarification before code. |
| End | End City/outer End removal needs fresh-world and existing-world verification. | Data/mixins exist, but generated-world behavior should be tested. |
| Elytra | Elytra should remain an item but not be survival-obtainable. | Depends on End Cities not generating and no alternate survival source being added unintentionally. |
| Worldgen | Ancient Cities, Deep Dark/Warden, Trial Chambers, trail ruins, and fossils are intended for survival removal. | Current status doc does not confirm all removals are implemented. |
| Worldgen | Strongholds should be limited to 3. | Not confirmed implemented. |
| Mobs | Sniffers and endermites should not be naturally/survival-spawnable if removed from progression. | They may remain in code and Retold AI. |
| Villages | Village reputation for stealing/crops/animals is planned but not implemented. | Territory warning exists for Illagers/Nether Remnants, not villages. |
| Stage 3 | Stage 3 illager behavior is enough for now but may need clearer player-facing feedback later. | Avoid major raid redesign without developer approval. |
| Piglins | Stage 3 piglin/pigman hiring or follower behavior is planned but not implemented. | Needs feature design. |
| Items | Tools/armor/ores, enchanting, mending removal, and combat reworks are still planned. | Aenderite now provides an ore/raw/ingot foundation but intentionally has no crafting uses. The larger progression remains undesigned; ask before implementing it. |
| Environment | Death drops should despawn much later than vanilla, not never. | No implementation confirmed. |
| Beds | Beds should not skip night; valid daytime bed rest is allowed when night skipping is disabled. | Healing behavior not confirmed implemented. |
| Nether | Nether portal spread is planned as energy drain. | Needs bounded implementation design. |

## Watchpoints

- Do not let vanilla AI target assignment bypass territory warning.
- Do not let creative or spectator players remain valid aggro targets.
- Do not make Aender inaccessible through commands; vanilla End command access is intentional for old builds.
- Keep Aender portal work bounded per tick. The charge may wait indefinitely for safety, but it must not force synchronous chunk generation or let an empty Aender repeatedly reset while destination chunks are being prepared.
- Do not add a recipe-book restoration gamerule.
- Do not delete elytra as an item just because survival acquisition should be removed.
- Do not fully remove sniffer/endermite code only to satisfy survival-spawn removal.
- Keep AI performance changes measurable with loaded-mob tests.

## Maintenance Rule

Update this file when:

- the developer changes a design direction
- a planned gap is implemented
- an undecided item becomes planned or not planned
- an implementation risk is discovered during code review
- a design-risk item becomes a confirmed issue after testing

## AI Agent Instructions

See the shared [AI Agent Instructions](README.md#ai-agent-instructions).
