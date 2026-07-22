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
| Aenderite generation | Aenderite appeared not to generate during in-game testing because only about 3.6% of a 9,409-chunk real-sampler probe contained any planned vein: roughly one ore-bearing chunk per 27 sampled chunks, which was rare rather than the approved uncommon distribution. | Developer in-game report on 2026-07-22 plus a code-level probe: 341 ore-bearing chunks, 1,149 planned blocks, and 1,134 valid Aender Stone replacements. | Planner cells reduced from 24 to 18 blocks and acceptance increased from 22% to 32%. The same probe now finds 808 ore-bearing chunks (8.6%), 2,851 planned blocks, and 2,802 valid replacements; a real-sampler JUnit test bounds the ratio between 7% and 16%. Needs fresh or legitimately regenerated Aender terrain verification before moving to Resolved Issues. |

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
