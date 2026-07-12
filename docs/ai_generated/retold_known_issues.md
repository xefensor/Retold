# Retold Known Bugs

> AI-generated bug tracker. This file is meant for confirmed bugs, failed tests, and reproducible broken behavior only. Design decisions, planned missing features, and undecided work belong in [`retold_design_risks.md`](retold_design_risks.md), [`retold_roadmap.md`](retold_roadmap.md), or [`design_implementation_status.md`](design_implementation_status.md).

AI agents: before fixing a bug here, read [`README.md`](README.md), check the owning system in [`retold_mod_system.md`](retold_mod_system.md), and ask the maintainer questions if the fix could change gameplay direction.

## Open Bugs

No confirmed bugs are currently documented here.

When adding a bug, include:

- affected area
- observed behavior
- expected behavior
- reproduction steps
- relevant command/debug output
- whether it is confirmed in a fresh world, existing world, singleplayer, server, or both

| Area | Bug | Reproduction | Status |
| --- | --- | --- | --- |

## Resolved Bugs

Move bugs here only after the implementation is complete and the behavior has been tested in game.

| Area | Bug | Resolution |
| --- | --- | --- |

## Maintenance Rule

Update this file when:

- a test from [`retold_testing_checklist.md`](retold_testing_checklist.md) fails
- a player reports reproducible broken behavior
- a bug is fixed and verified

Do not put design gaps here. For example, "Aender lava ban is planned but not implemented" is a design/implementation gap, not a confirmed bug.
