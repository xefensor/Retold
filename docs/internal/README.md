# Internal Retold Documentation

This folder contains maintained internal documentation for Retold.

The files are intended for two audiences:

- human developers who need a current map of the mod
- AI coding agents that need project context before making changes

Some sections are written as implementation guidance or refactor rules. Treat those sections as instructions for future AI-assisted work unless they conflict with newer code or direct developer direction.

## Accuracy Notes

Last targeted audit: 2026-08-03.
Last developer design clarification pass: 2026-08-09.
Last documentation consolidation pass: 2026-08-09.

These docs are checked against code and resource files, but they are still working documentation. When a row says `needs verification`, it means code/data exists but the final in-game behavior should still be tested before treating it as complete.

## Doc Roles

Use the files this way:

| File | Role |
| --- | --- |
| [`retold_roadmap.md`](retold_roadmap.md) | active developer direction, priorities, undecided items, and not-planned items |
| [`design_principles.md`](design_principles.md) | developer-confirmed high-level design rules used to judge future Retold systems |
| [`living_world_and_settlements.md`](living_world_and_settlements.md) | confirmed design for roads, environmental reclaiming/weathering, village generation/growth, professions, logistics, trade, magic/energy, and player-village reputation |
| [`golem_equipment_design.md`](golem_equipment_design.md) | confirmed design for pacifist-villager metalworking, oversized Iron Golem armor/weapons, block-scale material costs, and settlement defense investment |
| [`enchanting_design.md`](enchanting_design.md) | confirmed enchanting direction: player-learned SGA language, domain/effect/modifier words, energy-based levels, discovery, and known-enchantment recording |
| [`design_implementation_status.md`](design_implementation_status.md) | original design compared with current implementation status |
| [`retold_mod_system.md`](retold_mod_system.md) | whole-mod architecture and subsystem ownership |
| [`retold_mob_ai_system.md`](retold_mob_ai_system.md) | mob AI architecture, implementation rules, and completion checklist |
| [`mob_ai_work_report_2026-08-03.md`](mob_ai_work_report_2026-08-03.md) | dated consolidated record of the mob/faction design and implementation pass |
| [`mob_ai_handoff_prompt.md`](mob_ai_handoff_prompt.md) | copy-paste context and ordered tasks for continuing the mob work in a new conversation |
| [`mob_tps_benchmark.md`](mob_tps_benchmark.md) | reproducible 68-species TPS matrix method and baseline results |
| [`testing_strategy.md`](testing_strategy.md) | risk-based rules for choosing focused checks and escalating to complete suites |
| [`retold_design_risks.md`](retold_design_risks.md) | planned gaps, undecided decisions, and implementation watchpoints |
| [`retold_issues.md`](retold_issues.md) | confirmed issues, failed tests, and reproducible broken behavior |

If docs conflict, prefer the newest developer clarification in the roadmap/design-principles/specific design docs/status docs, then the current code for what is actually implemented. Ask the developer before resolving a design conflict in code.

Do not move planned missing features into the issue tracker. Keep them in [`retold_design_risks.md`](retold_design_risks.md) until a test proves a concrete behavior is broken.

Current internal docs:

- [`retold_roadmap.md`](retold_roadmap.md)
- [`design_principles.md`](design_principles.md)
- [`living_world_and_settlements.md`](living_world_and_settlements.md)
- [`golem_equipment_design.md`](golem_equipment_design.md)
- [`enchanting_design.md`](enchanting_design.md)
- [`design_implementation_status.md`](design_implementation_status.md)
- [`retold_mod_system.md`](retold_mod_system.md)
- [`retold_mob_ai_system.md`](retold_mob_ai_system.md)
- [`mob_ai_work_report_2026-08-03.md`](mob_ai_work_report_2026-08-03.md)
- [`mob_ai_handoff_prompt.md`](mob_ai_handoff_prompt.md)
- [`mob_tps_benchmark.md`](mob_tps_benchmark.md)
- [`testing_strategy.md`](testing_strategy.md)
- [`retold_design_risks.md`](retold_design_risks.md)
- [`retold_issues.md`](retold_issues.md)

## AI Agent Instructions

The root [`AGENTS.md`](../../AGENTS.md) is the organized entry point for AI coding agents. It defines required reading, authority order, engineering guardrails, validation expectations, documentation maintenance, and the definition of done.

The documents in this folder remain the detailed technical source. Agents should start with `AGENTS.md`, then read the task-specific documents listed there before implementing a feature, fix, or refactor.
