# Retold Issues

> Internal issue tracker. This file is meant for confirmed issues, failed tests, and reproducible broken behavior that need follow-up. Design decisions, planned missing features, and undecided work belong in [`retold_design_risks.md`](retold_design_risks.md), [`retold_roadmap.md`](retold_roadmap.md), or [`design_implementation_status.md`](design_implementation_status.md).

## Open Issues

No confirmed issues are currently documented here.

When adding an issue, include:

- affected area
- observed behavior or problem
- expected behavior or desired outcome
- reproduction steps
- relevant command/debug output
- whether it is confirmed in a fresh world, existing world, singleplayer, server, or both

| Area | Issue | Reproduction / Evidence | Status |
| --- | --- | --- | --- |
| Aender portal | When an automatically created counterpart has no nearby island terrain, replaceable void support allowed it to be placed near the dimension floor instead of at a useful altitude. | Developer in-game report on 2026-07-22. | Fix implemented to skip void columns and use a supported Y=100 fallback; needs real-Aender verification before moving to Resolved Issues. |
| Aender portal | Ambient particles spawned partly below the horizontal portal plane and were occluded instead of being visible to a player standing beside it. | Developer screenshot and in-game report on 2026-07-22. | Spawn height moved above the rendered plane with upward/outward drift; needs client visual verification before moving to Resolved Issues. |
| Aenderite generation | Aenderite initially appeared not to generate because only about 3.6% of a 9,409-chunk real-sampler probe contained a planned vein. After an initial increase to 8.6%, the developer still could not find it during normal in-game inspection and approved matching diamond-like abundance. | Developer reports on 2026-07-22; the original probe found 341 ore-bearing chunks and 1,134 valid replacements. A full-pipeline `ProtoChunk` GameTest confirms planned Aenderite survives terrain and decoration generation. A read-only scan of the newest local test save (`New World (44)`) found Aenderite in 361 of 1,396 saved chunks whose palettes contain Aender Stone. | Diamond-style tuning now uses mostly 3–4 and 4–8 block veins with rare 8–12 block veins. The 9,409-chunk probe finds 2,917 ore-bearing chunks (31%) and 28,209 placeable blocks, approximately three per sampled chunk. Deterministic tests enforce the frequency, density, size, underside bias, and chunk-border behavior; fresh or legitimately regenerated Aender terrain still needs in-game verification. |

## Resolved Issues

Move issues here only after the implementation is complete and the behavior has been tested in game.

| Area | Issue | Resolution |
| --- | --- | --- |
| Aender persistence | An obsolete chunk-storage mixin cancelled saves and disk reads for every unstabilized Aender chunk, leaving terrain region files empty and discarding placed/broken blocks after reload. | Removed the obsolete interception so vanilla region storage saves terrain together with the persistent reality signature. Verified on an isolated copy of the affected world across three dedicated-server starts: place/save/reload, break/save/reload, with the formerly empty region file populated. |
| Ocean monument | Guardian defense assist treated non-player attackers as nullable players, crashing when a drowned or another mob damaged a guardian. | Non-player damage sources are now rejected before player target validation; a GameTest reproduces drowned-to-guardian damage and verifies it completes safely. |
| Mob AI performance | Sight-cache cleanup could remove the current observer mapping after its entry list was obtained, so the fresh result was written to a detached list. | Cleanup now runs before the observer list is obtained; a deterministic GameTest verifies expired-observer removal, retained fresh results, and immediate cache reuse. |

## Maintenance Rule

Update this file when:

- a player reports reproducible broken behavior
- an issue is fixed and verified

Do not put design gaps here. For example, "Aender teleportation is planned but not implemented" is a design/implementation gap, not a confirmed issue.

## AI Agent Instructions

See the shared [AI Agent Instructions](README.md#ai-agent-instructions).
