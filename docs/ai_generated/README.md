# AI-Generated Retold Documentation

This folder contains AI-generated documentation for Minecraft Retold.

The files are intended for two audiences:

- human maintainers who need a current map of the mod
- AI coding agents that need project context before making changes

Some sections are written as implementation guidance or refactor rules. Treat those sections as instructions for future AI-assisted work unless they conflict with newer code or direct maintainer direction.

## Instructions For AI Agents

Before implementing any feature or refactor:

1. Read the relevant docs in this folder.
2. Check [`retold_roadmap.md`](retold_roadmap.md) for active design direction, undecided items, and not-planned items.
3. Check [`design_implementation_status.md`](design_implementation_status.md) for whether the feature is implemented, partial, missing, or unclear.
4. Check [`retold_design_risks.md`](retold_design_risks.md) for open design risks before changing a subsystem.
5. Check [`retold_known_issues.md`](retold_known_issues.md) for confirmed bugs before changing a subsystem.
6. Check [`retold_mod_system.md`](retold_mod_system.md) for the owning subsystem and technical entry points.
7. For mob behavior, factions, territory, or AI performance, also check [`retold_mob_ai_system.md`](retold_mob_ai_system.md) and [`mob_ai_completion_matrix.md`](mob_ai_completion_matrix.md).
8. Ask the maintainer implementation or design questions before coding if the design is ambiguous, incomplete, risky, undecided, or could be implemented in more than one reasonable way.
9. Do not silently invent major design decisions.
10. After finishing a feature, update the relevant docs in this folder, especially the roadmap/status/design-risks/bugs/testing docs.
11. If the feature changes mob AI, also update the AI docs/checklist as needed.

These docs should be updated after major system changes, especially changes to:

- world stages and progression
- Aender generation or stability
- recipe/villager teaching
- mob AI, factions, territory, or performance
- mixins and network payloads

## Accuracy Notes

Last targeted audit: 2026-07-12.
Last maintainer design clarification pass: 2026-07-12.

These docs are checked against code and resource files, but they are still generated working documentation. When a row says `needs verification`, it means code/data exists but the final in-game behavior should still be tested before treating it as complete.

## Doc Roles

Use the files this way:

| File | Role |
| --- | --- |
| [`retold_roadmap.md`](retold_roadmap.md) | active maintainer direction, priorities, undecided items, and not-planned items |
| [`design_implementation_status.md`](design_implementation_status.md) | original design compared with current implementation status |
| [`retold_mod_system.md`](retold_mod_system.md) | whole-mod architecture and subsystem ownership |
| [`retold_mob_ai_system.md`](retold_mob_ai_system.md) | mob AI architecture and implementation rules |
| [`mob_ai_completion_matrix.md`](mob_ai_completion_matrix.md) | mob AI completion checklist |
| [`retold_testing_checklist.md`](retold_testing_checklist.md) | practical in-game verification checklist |
| [`retold_design_risks.md`](retold_design_risks.md) | planned gaps, undecided decisions, and implementation watchpoints |
| [`retold_known_issues.md`](retold_known_issues.md) | confirmed bugs and failed tests only; it can be empty when no bugs are confirmed |

If docs conflict, prefer the newest maintainer clarification in the roadmap/status docs, then the current code for what is actually implemented. Ask the maintainer before resolving a design conflict in code.

Do not move planned missing features into the bug file. Keep them in [`retold_design_risks.md`](retold_design_risks.md) until a test proves a concrete behavior is broken.

Current generated docs:

- [`retold_roadmap.md`](retold_roadmap.md)
- [`design_implementation_status.md`](design_implementation_status.md)
- [`retold_mod_system.md`](retold_mod_system.md)
- [`retold_mob_ai_system.md`](retold_mob_ai_system.md)
- [`mob_ai_completion_matrix.md`](mob_ai_completion_matrix.md)
- [`retold_testing_checklist.md`](retold_testing_checklist.md)
- [`retold_design_risks.md`](retold_design_risks.md)
- [`retold_known_issues.md`](retold_known_issues.md)
