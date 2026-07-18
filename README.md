# Retold

Retold is a NeoForge mod that reimagines Minecraft as an alternative evolution of the 1.0 era.

The mod treats Minecraft as a world with one connected history instead of a collection of unrelated updates. Dimensions, structures, mobs, progression, recipes, and world rules are rebuilt around a single interpretation of the game: the Overworld, Nether, End, and Aender are tied together by energy, death, ancient civilizations, dragons, and the consequences of what the player does.

Retold is not official Minecraft lore. It is a total-conversion style project inspired by Minecraft 1.0, earlier development ideas, and selected later features that are changed, kept, or removed depending on whether they fit the world.

## Status

Retold is in active development and is not feature-complete.

Current target:

- Minecraft `26.2`
- NeoForge `26.2.0.7-beta`
- Java `25`
- Mod version: `0.2.0`
- Mod id: `retold`
- License: All Rights Reserved

Version and loader settings are defined in [gradle.properties](gradle.properties).

## Design Goals

Retold is built around four main pillars.

**Consistent lore:** dimensions, structures, mobs, items, and progression should feel like parts of the same world. Obsidian, bedrock, experience, emeralds, portals, enchanting, undead, pigmen, End crystals, and dragons all have a place in the same underlying logic.

**Meaningful progression:** major achievements should permanently affect the world. Killing the Ender Dragon is not just an ending; it changes the state of the world, unlocks new threats, and starts the path toward the Aender.

**Discovery first:** players should learn through exploration, observation, experimentation, and environmental storytelling instead of being handed exact instructions. Crafting, recipes, structures, and progression should be discoverable in-world.

**Living reactive world:** the world should react to the player and also move on its own. Mobs should have needs, homes, ranges, factions, enemies, allies, territory, and social behavior instead of only existing as player-facing encounters.

## World Premise

Retold interprets Minecraft as a magical world built around energy. Experience is the visible form of that energy, emeralds are a solid form valued by villagers, obsidian redirects energy, and bedrock represents a stronger dimensional boundary material.

The Nether is a realm of the dead, but also a real ecosystem with its own inhabitants and duties. Pigmen once guarded and cared for it through fortress nations, while blazes served as constructed guardians. The fall of that order explains zombified piglins, wandering undead, neglected souls, and the Nether's instability.

The End is a dying dimension. Its land has been drained, its crystals and obsidian pillars redirect energy, and the Ender Dragon is treated as an invader rather than a native creature. Retold uses that premise to turn the Ender Dragon's defeat into the start of a larger world change, not the end of the game.

The Aender is the dimension of change. It is a late-game sky-like dimension that replaces normal End progression after the dragon egg is hatched. It is meant to be unstable, bright, strange, and dangerous, while also becoming the source of powerful late-game building and travel possibilities.

## Core Ideas

Retold currently focuses on:

- a three-stage world progression model
- a late-game Aender dimension that replaces normal survival End progression
- a four-element dragon egg ritual framework
- a more dangerous and reactive world after the Ender Dragon is defeated
- undead and piglin stage changes tied to Nether lore
- recipes learned through knowledge and villager teaching instead of a normal recipe-book-first experience
- mobs with hunger, homes, ranges, factions, territory, warnings, hunting, fleeing, and group behavior
- worldgen and structure changes that make later content fit the mod's lore
- environmental pressure such as rain-extinguished torches

## World Stages

Retold splits progression into three world stages:

1. **Stage 1:** the world begins close to familiar Minecraft. The player works toward Nether access, the stronghold, and the Ender Dragon.
2. **Stage 2:** after the Ender Dragon is killed, the world becomes more dangerous. Undead stop fearing sunlight, some structures begin to appear, villagers and illagers become more important, and the dragon egg ritual becomes the next goal.
3. **Stage 3:** after the dragon egg is hatched, Aender access replaces normal End progression. Undead are removed from normal spawning, the Nether begins moving back toward balance, and the late-game opens around Aender exploration and future rewards.

The current implementation follows this structure, but not every planned stage rule is finished yet.

## Current Features

Implemented or strongly represented systems include:

- staged world progression
- Ender Dragon kill progression into Stage 2
- dragon egg element ritual framework
- water element path through the ocean monument / elder guardian
- air element path through the Air Temple / Gale Core encounter
- Aender dimension access after Stage 3
- horizontal Aender portals with 8:1 Overworld/Aender coordinate scaling
- custom Aender terrain, blocks, stability, lighting, and water behavior
- Aender lava vaporization
- End portal redirection to Aender after Stage 3
- delayed mansions and pillager outposts
- recipe knowledge gating
- villager recipe teaching
- Retold mob behavior profiles, hunger, homes/ranges, hunting, fleeing, and regrouping
- faction combat helpers
- territory warning for Nether Remnants and Illagers
- undead and piglin stage rules
- Enderman behavior changes after dragon death
- torch rain and extinguished torch behavior
- Aender chronolith time-control block

Some design goals are intentionally not finished yet, including the full four-element progression, remaining Aender travel rewards and in-dimension transport, tool and combat reworks, village reputation, and broader worldgen removals.

## Aender

The Aender is the mod's planned replacement for late End progression. It is a sky-like dimension based on instability, light, and change:

- water behaves differently there
- lava vaporizes there like water in the Nether
- horizontal Aender portals travel at an 8:1 ratio: one Overworld block maps to eight Aender blocks
- unstable chunks can regenerate differently
- stabilizer blocks can make regions permanent
- it is intended to become a difficult late-game dimension with powerful travel/building rewards

The current Aender portal uses the provisional frame block id `retold:dev_aender_portal_frame`. Frame deposits generate naturally inside Aender islands. A portal is a horizontal rectangular ring with an empty interior from 3x3 through 21x21 blocks; placing the final frame block activates it automatically. Portals work in both directions after Stage 3 and create a safe 3x3 counterpart when no nearby matching portal exists.

Travel from the Overworld to the Aender multiplies horizontal coordinates by eight; returning divides them by eight. Survival and adventure players must remain in the portal for at least 80 ticks (four seconds), with vanilla-style portal distortion and sound. During that charge, the server asynchronously prepares the destination view and incrementally refreshes stale chunks, then completes any remaining work before teleporting. Creative and spectator travel remains immediate by default.

When the last player leaves the Aender, its unstabilized reality is forgotten once. Returning players receive a newly generated reality around their destination, while stabilizer-protected chunks remain persistent. Arrival regeneration uses section-level clearing, rebuilt heightmaps, and chunk resends so the new terrain appears as one coherent view instead of visible old/new chunk seams.

Vanilla End access is still technically preserved for commands, so existing builds are not permanently lost when a world reaches Stage 3. In normal survival progression, however, the Aender is intended to take the End's place.

## Mobs And Factions

Retold mobs are intended to behave from more than their vanilla class. Their behavior is shaped by species, faction, profile, state, and nearby world situation.

Examples:

- animals can have hunger, grazing, foraging, homes, ranges, panic, and regrouping behavior
- wolves can form packs, hunt, return to dens, and defend owners
- predators can hunt and return after success or failure
- undead act as trapped dead souls and become more dangerous after the dragon dies
- Nether Remnants such as piglins, brutes, and blazes defend bastions and fortresses through warnings before combat
- Illagers defend outposts and mansions through the same territory-warning model
- guardians and elder guardians protect ocean monuments as part of the water element path

The goal is a world where mobs notice each other, defend places, flee danger, hunt, regroup, and react to player choices without turning every encounter into immediate vanilla aggression.

## Discovery And Recipes

Retold is designed around discovery. The normal recipe-book-first experience is replaced with recipe knowledge and villager teaching so crafting can feel like something the player learns through the world.

Villagers are treated as magical pacifists whose society values emeralds as energy-rich material. Their role is not just trading; they are also a way for players to learn recipes and understand progression without a direct written tutorial.

## Development

This is a Gradle/NeoForge project.

Common commands:

```bash
./gradlew build
./gradlew runClient
./gradlew runServer
./gradlew runData
```

If dependencies or generated metadata get stale:

```bash
./gradlew --refresh-dependencies
./gradlew clean
```

The project generates mod metadata from [src/main/templates](src/main/templates) through the `generateModMetadata` Gradle task.

## Project Layout

- [src/main/java/cz/xefensor/retold](src/main/java/cz/xefensor/retold) - mod source code
- [src/main/resources](src/main/resources) - assets, data, mixins, dimensions, tags, and overrides
- [src/main/templates](src/main/templates) - generated NeoForge mod metadata template
- [docs/internal](docs/internal) - internal design, status, and technical notes

## Credits

- **Jesse Schramm** — created the extinguished-torch textures.

## License

Retold uses separate licenses for code and creative assets:

- Code and non-asset materials are available under the [MIT License](LICENSE-CODE.md).
- Textures, audio, models, and other defined creative assets are [All Rights Reserved](LICENSE-ASSETS.md).
- Modpacks may redistribute the complete, unmodified Retold JAR, but the protected assets may not be extracted, reused, modified, or redistributed separately.

See the root [license notice](LICENSE) for the exact scope and third-party exceptions.

## Notes

The design is broad and intentionally experimental. Some planned features have changed, some are not implemented yet, and some may be redesigned before they are added.
