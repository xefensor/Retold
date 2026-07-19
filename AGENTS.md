# Retold Agent Guide

This file is the entry point for AI coding agents working on Retold. It defines how to understand the project, make changes, validate them, and keep the repository's documentation accurate.

Detailed design and implementation rules remain in [`docs/internal/`](docs/internal/README.md). Do not duplicate those documents here.

## Authority And Conflicts

Use this order when instructions or sources disagree:

1. The developer's current, explicit instruction.
2. Confirmed design decisions in [`docs/internal/retold_roadmap.md`](docs/internal/retold_roadmap.md) and [`docs/internal/design_implementation_status.md`](docs/internal/design_implementation_status.md).
3. Architecture and subsystem rules in [`docs/internal/retold_mod_system.md`](docs/internal/retold_mod_system.md) and [`docs/internal/retold_mob_ai_system.md`](docs/internal/retold_mob_ai_system.md).
4. Current code and tests for what is actually implemented.
5. Public summaries such as [`README.md`](README.md) and [`ROADMAP.md`](ROADMAP.md).

Do not silently resolve a design conflict in code. Explain the conflict and ask the developer. Planned, partial, and `needs verification` work must not be described as complete.

## Before Changing Anything

1. Inspect the worktree and preserve unrelated developer changes.
2. Read [`README.md`](README.md), [`CONTRIBUTING.md`](CONTRIBUTING.md), and the relevant documents below.
3. Locate the subsystem that owns the behavior before adding a new event handler, mixin, cache, or state store.
4. Check the detailed roadmap, implementation status, design risks, and confirmed issues.
5. Ask the developer before coding when a choice affects progression, lore, player-facing design, world compatibility, creative assets, or another undecided area.
6. Keep the change focused. Do not combine feature work with unrelated formatting or broad refactors.

### Required Reading By Task

| Task | Read first |
| --- | --- |
| Any gameplay or architecture change | [`retold_mod_system.md`](docs/internal/retold_mod_system.md) |
| Mob behavior, factions, territory, targeting, or AI performance | [`retold_mob_ai_system.md`](docs/internal/retold_mob_ai_system.md) |
| New feature or progression work | [`retold_roadmap.md`](docs/internal/retold_roadmap.md), [`design_implementation_status.md`](docs/internal/design_implementation_status.md) |
| Aender, worldgen, dimensions, portals, or existing-world changes | [`retold_design_risks.md`](docs/internal/retold_design_risks.md), relevant sections of [`retold_mod_system.md`](docs/internal/retold_mod_system.md) |
| Bug fix | [`retold_issues.md`](docs/internal/retold_issues.md), related GitHub issue, owning subsystem docs |
| Assets, models, textures, or audio | [`LICENSE-ASSETS.md`](LICENSE-ASSETS.md), [`ASSET_CREDITS.md`](ASSET_CREDITS.md) |

## Repository Map

| Location | Purpose |
| --- | --- |
| `src/main/java/cz/xefensor/retold/` | Java implementation |
| `src/main/resources/` | hand-maintained resources, mixin config, and data |
| `src/generated/resources/` | generated data output; update through data generation |
| `src/test/java/` | JUnit tests for isolated logic |
| `docs/` | public design material and developer documentation |
| `docs/internal/` | detailed architecture, status, risks, and AI implementation guidance |
| `.github/` | CI, issue forms, and pull-request workflow |

Registration is composed through `RetoldSubsystems` and the classes under `module/`. Prefer adding behavior to the owning subsystem over expanding the mod entry point.

## Engineering Rules

### Architecture And Ownership

- Keep gameplay rules in named Retold system classes. Mixins should normally be hooks, guards, accessors, or narrowly scoped vanilla interception points.
- Route world-stage changes through `RetoldStageManager.setStage` so persistence, synchronization, and transition effects remain consistent.
- Keep server gameplay state authoritative. Client code should render, present feedback, or consume synchronized state rather than decide progression.
- Prefer Minecraft/NeoForge lifecycle-aware persistence such as `SavedData`. If custom files are necessary, use versioned, failure-safe writes and reset static state across server/world lifecycles.
- Prefer data-driven definitions for profiles, tags, recipes, loot, structures, and other content that does not require hard-coded behavior.
- Reuse registries, modules, helpers, and ownership systems already present. Do not create a parallel framework for the same concern.
- Avoid growing already-large classes with unrelated responsibilities. Extract a cohesive helper or subsystem when the new behavior has its own state, policy, or validation needs.

### Mob AI And Performance

- Use mob profiles and the established behavior pipeline instead of adding ordinary always-on entity tick subscribers.
- Acquire movement and combat ownership through `RetoldAiControl`.
- Route Retold-owned targets through the target/faction ownership helpers; do not bypass warning or faction rules with arbitrary `setTarget` calls.
- Use `RetoldAiScanCache`, `RetoldAiSightCache`, block-search caches, LOD, and work budgets for expensive world queries.
- Keep hot-path work bounded and measurable. Do not add repeated broad entity scans, block scans, pathfinding, or allocations per mob tick.
- Preserve creative/spectator exclusions, retaliation rules, and target-source ownership.
- When changing performance behavior, compare the relevant `/retold debug` counters and test with realistic loaded-mob counts.

### Code Quality

- Match the existing Java style and package structure. Prefer clear names and explicit state transitions over clever compression.
- Use `Retold.LOGGER`; do not add `System.out` or `System.err` calls.
- Do not silently swallow broad exceptions. Catch expected failures narrowly and log unexpected failures with context and the exception.
- Document why a non-obvious workaround, mixin, compatibility guard, or intentionally stale cache behavior is required.
- Remove dead compatibility paths when their reason no longer applies, but verify world and data compatibility first.
- Do not treat compilation as proof that gameplay is correct.

### Assets And Licensing

- Code and non-asset contributions use [`LICENSE-CODE.md`](LICENSE-CODE.md).
- Textures, models, audio, and other creative assets use [`LICENSE-ASSETS.md`](LICENSE-ASSETS.md).
- Do not introduce extracted or modified Minecraft assets unless their use is demonstrably permitted.
- Record the source, author, license, and placeholder status of approved assets in [`ASSET_CREDITS.md`](ASSET_CREDITS.md).
- Discuss new creative assets and generated placeholder assets with the developer before committing them.

## Validation

Run the smallest useful checks while developing, then the complete checks required by the change.

```bash
./gradlew build
./gradlew runGameTestServer
```

Other useful commands:

```bash
./gradlew runClient
./gradlew runServer
./gradlew runData
```

| Change | Minimum validation |
| --- | --- |
| Documentation only | Check links, headings, claims, and formatting against current code |
| Pure logic or state transition | Add/update JUnit tests and run `./gradlew build` |
| Gameplay behavior | Add/update a GameTest when practical; run build, GameTests, and focused in-game checks |
| Mob AI | Test ownership, target clearing, difficulty, player modes, performance budgets, and relevant debug counters |
| Worldgen or structures | Test multiple seeds and chunk borders in fresh worlds; check existing-world behavior when applicable |
| Dimension, portal, or progression | Test both directions, repeated travel, death/reconnect, fresh and existing worlds, and relevant stages |
| Networking | Test a dedicated server with a separate client; include multiple players when state can diverge |
| Visual or asset change | Provide screenshots or video and verify attribution/license records |

If a relevant check cannot be run, state exactly what was not verified and why. Never claim an in-game, dedicated-server, multiplayer, or existing-world result that was not actually tested.

## Documentation Maintenance

Update only the documents affected by the change:

| Document | Update when |
| --- | --- |
| `CHANGELOG.md` | A player-facing release change is added, changed, or fixed |
| `ROADMAP.md` | Public Now/Next/Later priorities change |
| `docs/internal/retold_roadmap.md` | Detailed developer direction or an undecided item changes |
| `docs/internal/design_implementation_status.md` | A design item becomes implemented, partial, missing, or verified |
| `docs/internal/retold_design_risks.md` | A planned gap, open decision, compatibility concern, or verification risk changes |
| `docs/internal/retold_issues.md` | Testing confirms a reproducible bug, or that bug is fixed and verified |
| `docs/internal/retold_mod_system.md` | Whole-mod architecture, ownership, lifecycle, or entry points change |
| `docs/internal/retold_mob_ai_system.md` | Mob AI architecture, profiles, ownership, performance, or completion status changes |
| `ASSET_CREDITS.md` | An asset, placeholder, author, source, or license changes |

Missing planned features belong in roadmap/status/risk documents, not in `retold_issues.md`. Only confirmed broken behavior belongs in the issue tracker.

## Definition Of Done

A change is complete only when:

- it follows current developer direction and the owning subsystem's rules
- the implementation is focused and does not overwrite unrelated work
- appropriate automated tests exist or the reason they are impractical is documented
- required Gradle and in-game validation has been performed honestly
- server, multiplayer, existing-world, performance, and visual risks were considered where relevant
- player-facing behavior, internal status, risks, and asset credits are updated where affected
- the pull request explains the change, its risks, AI assistance, and exactly how it was verified

When uncertain, stop before inventing a major rule and ask the developer a specific implementation question.
