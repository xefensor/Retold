# Retold Design Principles

> Developer-confirmed design guidance. This document records current high-level rules for future Retold systems. It is not an implementation-status tracker and does not mean every described system exists yet.

## 1. Save And World Longevity

Do not spend significant development effort on formal save-migration guarantees yet. Retold does not currently have an established player base whose long-lived worlds need that promise.

Revisit save-format versioning, migration policy, and world-upgrade guarantees before Retold has enough players that world compatibility becomes a real public commitment.

## 2. The Player Exists Inside The World

Retold should not treat the player as an exception to the simulation by default.

Do not globally protect blocks merely because a player placed them. A system should decide what it can affect based on that system's own rules and fiction.

Examples:

- weathering may affect player-built materials if that material naturally weathers
- animals may eat crops planted by players
- path wear may occur on terrain modified by players if that surface is valid for path wear
- portal-related environmental effects may affect a player base if that is a natural consequence of portal behavior

Exceptions should exist when the feature itself justifies them. Retrogen is a strong example: inserting a large delayed structure can overwrite substantial player work with no natural local cause, so it should avoid occupied/developed areas. This can also make sense in-world: Illagers would not normally choose to establish a major structure where somebody is already clearly settled.

General rule:

> Ask whether this system should affect this place or block, not whether the player touched it.

A good Retold system should ideally be understandable and exploitable as a rule of the world rather than as an effect that only happens to the player.

## 3. Discovery And Feedback

All mandatory main progression must be discoverable from inside the game without requiring a wiki, external guide, or exact written tutorial.

Retold should prefer diegetic teaching through:

- environment and structure layout
- mob behavior
- particles and animation
- sound
- block states
- item reactions
- NPC behavior
- repeated visual language

Optional mechanics, secrets, optimizations, easter eggs, and rare interactions may remain substantially more obscure.

Experimentation should not be blind. When a player is close to the correct solution, the game should normally provide meaningful non-text feedback when possible. A failed ritual, incomplete portal, rejected item, or damaged reputation should visibly or audibly react in a way that helps the player form a better hypothesis.

## 4. Time Model

Retold should use shared time categories as design guidance, while each system still owns its exact timings.

Useful categories:

- **Fast**: seconds to minutes; combat, fleeing, immediate hunger reactions, short AI states
- **Daily**: roughly several Minecraft days; breeding, village maintenance, parts of ecosystem behavior
- **Long-term**: many Minecraft days or longer; path formation, reclaiming, weathering, persistent settlement/world change

Do not choose timings merely for realism. Choose timings so changes are readable, meaningful, and give the player a chance to understand and influence them.

For unloaded areas, prefer bounded aggregate catch-up where appropriate rather than attempting to replay every missed tick.

## 5. Ecosystem Stability

Retold should aim for a believable ecosystem, not a perfectly realistic ecological simulation.

Local ecological collapse is acceptable. A player, predator population, food shortage, or other pressure may locally eliminate or heavily reduce a species.

Global extinction of ordinary species is not a useful simulation goal in Minecraft's effectively unbounded world.

Populations may return organically through ordinary spawning, migration-like behavior, breeding, or other systems. Avoid obvious instant rubber-banding whose only purpose is to force a fixed population count.

The simulation should tolerate local consequences without allowing accidental implementation bugs or runaway feedback loops to destroy huge regions unrealistically quickly.

## 6. Multiplayer State Ownership

Do not force all multiplayer state into one universal ownership rule. Decide ownership feature by feature according to what the state represents.

Default guidance:

- physical world changes are shared
- personal knowledge is usually per-player
- personal relationships/reputation are usually per-player
- settlement or faction state depends on the specific system
- major world progression is shared world state

For every multiplayer feature, explicitly ask who owns the state and why.

Examples:

- killing the Ender Dragon can advance the whole world
- recipe discovery should not automatically teach every player unless deliberately designed that way
- one player's reputation with a village need not automatically become another player's reputation
- physical raid damage, path formation, structure changes, and Aender/world-stage changes are shared because they exist in the common world

## 7. Death Drops

Keep the death system close to Minecraft rather than adding graves or a new corpse mechanic by default.

Intended direction:

- items dropped specifically by a player's death should not despawn due to the normal item age timer
- otherwise they remain ordinary world items and can still be picked up, merged, burned, destroyed by hazards, removed by commands/admin actions, etc.
- do not pre-emptively add a complicated persistence optimization unless measurements show that permanent death drops create a real performance problem

If performance becomes an issue later, optimize the implementation without changing the player-facing rule if practical.

## 8. Difficulty Philosophy

Retold should not use stat inflation as its primary difficulty tool.

Changing health, damage, armor, or similar numbers is a last resort for correcting a specific encounter or entity whose base values are genuinely wrong for its role.

Prefer difficulty created by:

- different behavior
- new interactions
- changed environmental rules
- better cooperation and targeting
- loss or alteration of previously safe assumptions
- new threats or faction activity
- encounter mechanics that require different player decisions

Do not make Stage 2 or multiplayer harder merely by multiplying enemy health/damage when the same goal can be achieved through better systemic design.

## 9. Lore Boundaries

Retold should explain things whose existence or behavior creates a meaningful question for the world. It does not need to explain the existence of every ordinary thing.

Three useful levels:

1. **Cosmology/progression-critical** — dimensions, dragons, portals, End crystals, obsidian/bedrock, undead, Aender, world-stage changes, and other foundational concepts need coherent explanations.
2. **Worldbuilding** — major artificial structures, civilizations, factions, and unusual behaviors should make sense in context even when they do not need a deep cosmological explanation.
3. **Ordinary world** — cows, chickens, rain, trees, dirt, squid, and similar ordinary Minecraft things can simply exist.

A giant artificial underground complex naturally asks who built it, why, and how it relates to the rest of the world. A cow does not require a creation myth.

## 10. Minecraftiness

Retold should deepen Minecraft without automatically turning it into a different survival game.

Minecraft feel is important, but it is not an absolute rule that always overrides realism or simulation. Evaluate each feature individually.

A realistic/systemic feature is a good fit when it creates interesting decisions, emerges naturally from the world, remains understandable, and does not introduce unnecessary micromanagement.

Good examples may include:

- animals eating crops
- paths forming through repeated travel
- selected environmental weathering

Features such as thirst, global temperature micromanagement, inventory weight, complex injury simulation, or similar survival obligations need a much stronger gameplay reason than simply being realistic.

Prefer using Minecraft's existing language when possible: blocks, particles, animation, sound, mob behavior, item states, and world changes instead of adding a new HUD/menu for every system.

## 11. Semantic World Events And Hooks

Do not build a generic Retold event framework in advance just because it may be useful someday.

When two or more Retold systems, or a legitimate public integration, need to react to the same semantic game event, consider introducing a Retold-owned event/hook for that event.

Examples that may eventually justify shared hooks include:

- world stage changed
- settlement threatened
- faction territory violated
- structure discovered
- portal energy consumed
- an area becoming repeatedly travelled

Build these evolutionarily when real reuse exists rather than creating framework abstraction before there is a concrete consumer.

## 12. Stage 3 And Long-Term Endgame

Stage 3 should transition Retold from mandatory progression into a more open sandbox endgame. It should not mean that challenge disappears.

The Overworld can become friendlier for long-term building while challenge remains through systems such as:

- Aender as dangerous late-game wilderness
- Illagers and raids
- faction conflicts
- difficult optional encounters
- environmental challenges
- rare world events where they fit

Do not add a Stage 4 merely to keep a linear progression chain going.

The intended long-term direction is that completing the main progression opens Minecraft into a strong sandbox phase: building, infrastructure, exploration, world development, and optional danger continue without another mandatory gate.

Aender rewards should strongly support late-game sandbox play, travel, building, and world manipulation where appropriate while the dimension itself can remain highly dangerous.

## 13. Native Retold Features Vs Companion Mods

If a feature affects Retold's lore, progression, world simulation, persistent world state, or important gameplay decisions, Retold should generally own the feature natively when it becomes important to Retold's identity.

If a feature primarily improves rendering, performance, audiovisual presentation, or cosmetic fidelity without defining the world's gameplay rules, it can remain an external companion mod.

Likely native categories include:

- path wear/road formation if adopted
- Nether portal environmental transformation
- selected weathering/reclaim systems
- village maintenance and world-reactivity systems
- ecosystem simulation
- seasons if Retold makes seasons a gameplay system
- gameplay-level sound propagation if entities/mechanics depend on it

Likely external specialist categories include:

- LOD rendering such as Distant Horizons/Voxy
- renderer/performance optimization
- shader infrastructure
- acoustic rendering such as reverb/occlusion presentation
- ambient audio presentation
- dynamic lights
- purely cosmetic particles/effects

If an external mod already implements an idea Retold wants, do not automatically make it a permanent dependency. It can serve as inspiration, a reference implementation, or a temporary solution. Core Retold identity should generally remain under Retold's control.

Do not clone an entire external mod merely because Retold likes one idea from it. Prefer implementing the smallest Retold-specific system that captures the desired gameplay rule.

## Applying These Principles

These are design guides, not automatic answers. Features must still be designed individually.

When proposing or implementing a new system, ask:

- Does the player live under the same world rule as everything else?
- Can mandatory behavior/progression be discovered in-game?
- Is its timing appropriate for how players perceive the change?
- Is multiplayer state owned by the correct scope?
- Does difficulty come from decisions rather than cheap stat scaling?
- Does the feature need lore, or merely need not contradict existing lore?
- Does it still feel like Minecraft?
- Is the feature important enough to Retold's identity that Retold should own it?
