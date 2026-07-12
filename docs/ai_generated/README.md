# AI-Generated Retold Documentation

This folder contains AI-generated documentation for Minecraft Retold.

The files are intended for two audiences:

- human maintainers who need a current map of the mod
- AI coding agents that need project context before making changes

Some sections are written as implementation guidance or refactor rules. Treat those sections as instructions for future AI-assisted work unless they conflict with newer code or direct maintainer direction.

## Instructions For AI Agents

Before implementing any feature or refactor:

1. Read the relevant docs in this folder.
2. Check [`design_implementation_status.md`](design_implementation_status.md) for whether the feature is implemented, partial, missing, or unclear.
3. Check [`retold_mod_system.md`](retold_mod_system.md) for the owning subsystem and technical entry points.
4. For mob behavior, factions, territory, or AI performance, also check [`retold_mob_ai_system.md`](retold_mob_ai_system.md).
5. Ask the maintainer implementation or design questions before coding if the design is ambiguous, incomplete, risky, or could be implemented in more than one reasonable way.
6. Do not silently invent major design decisions.
7. After finishing a feature, update the relevant docs in this folder, especially the implementation-status tracker.
8. If the feature changes mob AI, also update the AI docs/checklist as needed.

These docs should be updated after major system changes, especially changes to:

- world stages and progression
- Aender generation or stability
- recipe/villager teaching
- mob AI, factions, territory, or performance
- mixins and network payloads

## Accuracy Notes

Last targeted audit: 2026-07-12.

These docs are checked against code and resource files, but they are still generated working documentation. When a row says `needs verification`, it means code/data exists but the final in-game behavior should still be tested before treating it as complete.

Current generated docs:

- [`retold_mod_system.md`](retold_mod_system.md)
- [`retold_mob_ai_system.md`](retold_mob_ai_system.md)
- [`mob_ai_completion_matrix.md`](mob_ai_completion_matrix.md)
- [`design_implementation_status.md`](design_implementation_status.md)
