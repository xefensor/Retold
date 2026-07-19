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

## Resolved Issues

Move issues here only after the implementation is complete and the behavior has been tested in game.

| Area | Issue | Resolution |
| --- | --- | --- |
| Ocean monument | Guardian defense assist treated non-player attackers as nullable players, crashing when a drowned or another mob damaged a guardian. | Non-player damage sources are now rejected before player target validation; a GameTest reproduces drowned-to-guardian damage and verifies it completes safely. |
| Mob AI performance | Sight-cache cleanup could remove the current observer mapping after its entry list was obtained, so the fresh result was written to a detached list. | Cleanup now runs before the observer list is obtained; a deterministic GameTest verifies expired-observer removal, retained fresh results, and immediate cache reuse. |

## Maintenance Rule

Update this file when:

- a player reports reproducible broken behavior
- an issue is fixed and verified

Do not put design gaps here. For example, "Aender teleportation is planned but not implemented" is a design/implementation gap, not a confirmed issue.

## AI Agent Instructions

See the shared [AI Agent Instructions](README.md#ai-agent-instructions).
