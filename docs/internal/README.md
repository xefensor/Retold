# Internal Retold Documentation

This folder contains maintained internal documentation for Minecraft Retold.

The files are intended for two audiences:

- human developers who need a current map of the mod
- AI coding agents that need project context before making changes

Some sections are written as implementation guidance or refactor rules. Treat those sections as instructions for future AI-assisted work unless they conflict with newer code or direct developer direction.

## Accuracy Notes

Last targeted audit: 2026-07-17.
Last developer design clarification pass: 2026-07-12.
Last documentation consolidation pass: 2026-07-17.

These docs are checked against code and resource files, but they are still working documentation. When a row says `needs verification`, it means code/data exists but the final in-game behavior should still be tested before treating it as complete.

## Doc Roles

Use the files this way:

| File | Role |
| --- | --- |
| [`retold_roadmap.md`](retold_roadmap.md) | active developer direction, priorities, undecided items, and not-planned items |
| [`design_implementation_status.md`](design_implementation_status.md) | original design compared with current implementation status |
| [`retold_mod_system.md`](retold_mod_system.md) | whole-mod architecture and subsystem ownership |
| [`retold_mob_ai_system.md`](retold_mob_ai_system.md) | mob AI architecture, implementation rules, and completion checklist |
| [`retold_design_risks.md`](retold_design_risks.md) | planned gaps, undecided decisions, and implementation watchpoints |
| [`retold_issues.md`](retold_issues.md) | confirmed issues, failed tests, and reproducible broken behavior |

If docs conflict, prefer the newest developer clarification in the roadmap/status docs, then the current code for what is actually implemented. Ask the developer before resolving a design conflict in code.

Do not move planned missing features into the issue tracker. Keep them in [`retold_design_risks.md`](retold_design_risks.md) until a test proves a concrete behavior is broken.

Current internal docs:

- [`retold_roadmap.md`](retold_roadmap.md)
- [`design_implementation_status.md`](design_implementation_status.md)
- [`retold_mod_system.md`](retold_mod_system.md)
- [`retold_mob_ai_system.md`](retold_mob_ai_system.md)
- [`retold_design_risks.md`](retold_design_risks.md)
- [`retold_issues.md`](retold_issues.md)

## AI Agent Instructions

Before implementing any feature or refactor:

1. Read the root [`README.md`](../../README.md) and the relevant docs in this folder.
2. Check [`retold_roadmap.md`](retold_roadmap.md) for active design direction, undecided items, and not-planned items.
3. Check [`design_implementation_status.md`](design_implementation_status.md) for whether the feature is implemented, partial, missing, or unclear.
4. Check [`retold_design_risks.md`](retold_design_risks.md) for open design risks before changing a subsystem.
5. Check [`retold_issues.md`](retold_issues.md) for confirmed issues before changing a subsystem.
6. Check [`retold_mod_system.md`](retold_mod_system.md) for the owning subsystem and technical entry points.
7. For mob behavior, factions, territory, or AI performance, also check [`retold_mob_ai_system.md`](retold_mob_ai_system.md).
8. Ask the developer implementation or design questions before coding if the design is ambiguous, incomplete, risky, undecided, or could be implemented in more than one reasonable way.
9. Do not silently invent major design decisions.
10. After finishing a feature, update the relevant docs in this folder, especially the roadmap/status/design-risks/issues docs.
11. If the feature changes mob AI, also update the AI docs/checklist as needed.

These docs should be updated after major system changes, especially changes to:

- world stages and progression
- Aender generation or stability
- recipe/villager teaching
- mob AI, factions, territory, or performance
- mixins and network payloads
