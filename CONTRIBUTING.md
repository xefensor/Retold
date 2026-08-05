# Contributing To Retold

Contributions should support Retold's connected lore, meaningful progression, discovery-first design, or living reactive world.

## Before Starting

- Use the [bug form](https://github.com/xefensor/Retold/issues/new?template=bug-report.yml) for reproducible broken behavior.
- Use the [suggestion form](https://github.com/xefensor/Retold/issues/new?template=suggestion.yml) for focused proposals.
- Use [Discord](https://discord.gg/S3g98zEY8a) for support and early discussion.
- Discuss large features, progression or world-generation changes, and all creative assets with the developer first.

## Setup

The current target is recorded in [`gradle.properties`](gradle.properties). Useful commands:

```bash
./gradlew build
./gradlew pmdMain pmdTest
./gradlew runClient
./gradlew runServer
./gradlew runGameTestServer
./gradlew runData
```

## Pull Requests

1. Start from current `master` and create a focused branch.
2. Keep unrelated formatting and refactors out of the change.
3. Update tests, documentation, and `CHANGELOG.md` when affected.
4. Run appropriate validation and describe it honestly.
5. Open a pull request and complete its template.

Small changes are easier to verify. A technically sound proposal may still be declined when it conflicts with Retold's design.

## Validation

Use the risk-based selector rules in
[`docs/internal/testing_strategy.md`](docs/internal/testing_strategy.md). Start with the narrowest
JUnit or `retold:` GameTest selector that covers the changed contract. Do not run the complete
GameTest suite or complete per-mob TPS matrix by default; broad runs need a shared-system,
test-isolation, performance-baseline, release/milestone, or explicit developer reason.

Every code contribution should run `./gradlew build` once before handoff, which includes unit tests
and PMD static analysis but not NeoForge GameTests. Test only the relevant risks where they exist:

- gameplay and progression in a fresh survival world
- world generation across seeds, chunk borders, and existing worlds
- dimensions in both directions, including death and repeated travel
- networking on a dedicated server with a separate client
- multiplayer state across simultaneous players and reconnects
- visual changes with screenshots or video

If relevant validation could not be performed, say so in the pull request.

## AI-Assisted Contributions

AI assistance is allowed, but contributors remain responsible for understanding, reviewing, and testing the result. When AI materially assists a change, mention it in the pull request and explain how the output was verified. Unreviewed bulk-generated code, meaningless generated tests, and changes the contributor cannot explain are not acceptable.

## Licensing And Assets

- Code and non-asset contributions are provided under [`LICENSE-CODE.md`](LICENSE-CODE.md).
- Creative assets use [`LICENSE-ASSETS.md`](LICENSE-ASSETS.md) and require prior discussion.
- Contribute only material you created or have the legal right to contribute.
- Include source, authorship, license, and required attribution for approved assets.
- Do not submit extracted or modified Minecraft assets unless their use is demonstrably permitted.

See [`ASSET_CREDITS.md`](ASSET_CREDITS.md) for the attribution record.
