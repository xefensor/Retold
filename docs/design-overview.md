# Retold Design Overview

Retold treats Minecraft as a world with one connected history instead of a collection of unrelated updates. It is independent fiction, not official Minecraft lore.

## Design Pillars

### Consistent Lore

Dimensions, structures, mobs, items, and progression should feel like parts of the same world. Experience, emeralds, obsidian, bedrock, portals, enchanting, undead, pigmen, End crystals, and dragons share an underlying logic.

### Meaningful Progression

Major achievements should permanently affect the world. Defeating the Ender Dragon changes the world, unlocks new threats, and begins the path toward the Aender.

### Discovery First

Players should learn through exploration, observation, experimentation, and environmental storytelling instead of receiving exact instructions.

### A Living, Reactive World

Mobs should have needs, homes, ranges, factions, territory, and social behavior instead of existing only as encounters aimed at the player.

## World Premise

Retold interprets Minecraft as a magical world built around energy. Experience is its visible form, emeralds are a solid form valued by villagers, obsidian redirects it, and bedrock represents a stronger dimensional boundary.

The Nether is a realm of the dead and a real ecosystem. Pigmen once guarded it through fortress nations while blazes served as constructed guardians. Their fallen order explains zombified piglins, wandering undead, neglected souls, and instability.

The End is a dying dimension whose land has been drained. The Ender Dragon is an invader rather than a native creature, so its defeat starts a larger change instead of ending the game.

The Aender is the dimension of change: an unstable, bright, strange, and dangerous late-game realm that replaces normal survival End progression after the dragon egg ritual.

## World Stages

1. **Stage 1:** the player works toward Nether access, the stronghold, and the Ender Dragon.
2. **Stage 2:** defeating the dragon makes the world more dangerous and begins the four-element ritual.
3. **Stage 3:** hatching the egg redirects normal End access to the Aender and begins returning the Nether toward balance.

The current implementation temporarily requires Water and Air while Fire and Earth remain unfinished.

## The Aender

The Aender is based on instability, light, and change. Water and lava behave differently, unstable terrain can regenerate, stabilizers preserve selected regions, and horizontal portals connect it to the Overworld using an 8:1 coordinate scale. Its current art and rewards are foundations rather than final designs.

## Mobs, Factions And Discovery

Retold behavior is shaped by species, faction, profile, state, and nearby circumstances. Creatures can forage, panic, regroup, hunt, return home, and defend territory through warnings before combat.

Recipe knowledge and villager teaching replace a recipe-book-first experience. Structures and progression should provide enough environmental and social clues to progress without an external wiki while preserving discovery.

## Further Reading

- [`../ROADMAP.md`](../ROADMAP.md) — public priorities
- [`internal/retold_mod_system.md`](internal/retold_mod_system.md) — technical implementation
- [`internal/design_implementation_status.md`](internal/design_implementation_status.md) — detailed status
- [`internal/retold_mob_ai_system.md`](internal/retold_mob_ai_system.md) — mob-AI architecture
