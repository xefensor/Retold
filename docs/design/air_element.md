# Air Element Design

## Purpose

The Air Element is one of the four Stage 2 dragon egg elements. Its role is to test freedom, height, movement, and control without turning the challenge into a simple combat gate.

Air should feel different from Water. Water is about adaptation and community pressure through the ocean monument path. Air is about movement through danger, using space well, and surviving a place that does not want to stay still.

## Element Philosophy

Retold's four elements should each express a different idea:

- Fire: power, will, desire, and force.
- Earth: substance, endurance, persistence, and weight.
- Air: freedom, detachment, motion, risk, and humor.
- Water: change, adaptation, community, and continuity.

Air should not reward brute force alone. It should reward players who stay mobile, read the environment, build smart windbreaks, and accept that the arena is unstable.

## World Role

After the Ender Dragon dies and the world enters Stage 2, ancient buried air temples can rise into the sky above mountain and highland regions.

The structure should look like a temple that once sank into the ground and was later ripped upward. The island should preserve signs of that origin:

- stone and copper temple ruins
- exposed dirt, roots, and underside debris
- broken foundations
- cracked interior halls
- satellite fragments around one main island
- a crater or torn ground scar below the raised temple

The crater is only a visual clue. There is no prebuilt fallen version of the temple.

## Discovery

Initial discovery is command/structure based.

The Air Temple should be a real structure so it can be located like a woodland mansion:

```mcfunction
/locate structure retold:air_temple
```

Future discovery can add in-world clues, but that is out of scope for the first version.

Current implementation note: `retold:air_temple` is registered as a locatable placeholder structure. Its current generated form is only a small sky-island/temple prototype used to prove placement, locating, and delayed-generation wiring before the final structure, wind, boss, and collapse systems are built.

## Placement

Air Temples generate in the Overworld during Stage 2.

Placement rules:

- Prefer mountain and highland regions.
- Generate above cloud height so clouds hide the lower island and crater view more than the temple itself.
- Rare like old Minecraft strongholds.
- Use one main island plus several satellite fragments.
- Avoid modified chunks when retrogening into existing worlds.
- Use the same safety philosophy as delayed mansions and outposts.

Recommended rarity for the first implementation: about three temples per world, seed-based.

Current implementation note: placement uses a three-count concentric-rings structure set biased toward mountain/highland biomes.

## Retrogen

Air Temples should be able to appear in already generated worlds after Stage 2 begins.

Retrogen rules:

- Candidate chunks are selected after Stage 2.
- If candidate chunks or the ground/crater area were modified by players, skip placement or search nearby.
- Do not overwrite player builds.
- The sky island and the crater below belong to the same placement event.
- Placement state must persist so the same temple is not generated repeatedly.

## Structure Layout

The Air Temple consists of:

- one main floating island
- several satellite islands/fragments
- an interior temple route
- wind traversal sections
- Breeze enemy placements
- an interior boss arena

The main island should carry most of the temple. Satellite fragments support traversal, optional loot, and collapse spectacle.

The final boss fight happens inside the temple, not on the exposed roof.

## Block Palette

Air uses temple blocks only. It does not add a new terrain block family.

Palette direction:

- stone or carved stone base
- copper, weathered copper, and oxidized copper accents
- copper machinery language for vents, shafts, and old wind mechanisms
- custom temple blocks are allowed if vanilla blocks are not enough

New blocks should be useful after the fight as decorative or functional temple blocks, similar in role to ocean monument blocks.

Temple blocks do not normally have gravity. They only behave like falling blocks during the post-boss collapse event.

## Wind

Wind is a visible environmental hazard shown with particles.

Wind affects:

- players
- mobs
- items
- projectiles

Wind should make simple straight bridging unreliable, but it should not ban building. The intended Minecraft solution is to build smarter:

- walls
- windbreaks
- enclosed paths
- railings
- protected bridges

Wind should push, redirect, or destabilize entities. It should not feel like random unavoidable punishment. Strong wind lanes need readable particles and consistent direction.

Wind stops when the boss dies.

## Enemies

Breezes are normal temple enemies.

They fit the Air challenge because they pressure movement and make exposed platforms dangerous.

Enemy placement should support the traversal challenge instead of turning every room into a combat room.

## Boss

The boss is Breeze-themed.

Working identity: a larger or more powerful Breeze-like entity bound to the temple's copper wind machinery.

Possible names:

- Gale Core
- Stormbound Breeze
- Tempest Core
- Breeze Heart
- Windcaller Breeze

Preferred current name: Gale Core.

Boss requirements:

- Fight takes place inside the temple.
- Boss is killable with normal weapons.
- Boss attacks should emphasize movement, gusts, knockback, and projectile disruption.
- Boss may summon Breezes or activate wind machinery.
- Boss drops `air_element` as a simple item drop.

No shrine activation or boss key is required for the first version.

## Collapse Event

When the boss dies:

1. Boss drops `air_element`.
2. Wind stops.
3. A 10 second delay begins.
4. The temple starts collapsing from the center of the boss arena.
5. Collapse spreads outward in a radial wave.
6. Satellite islands collapse when the wave reaches them.
7. Temple blocks become falling blocks in controlled batches.
8. Blocks fall naturally and stay where they land.
9. Players receive no slow falling, wind lift, teleport, or rescue help.

The fallen ruin is organic. There is no prebuilt fallen structure.

## Collapse Safety

The collapse must be throttled.

Implementation requirements:

- Cap falling block entities per tick.
- Persist collapse state so it can resume after chunk unload or server restart.
- Do not process unloaded chunks.
- Skip or specially handle fluids.
- Never convert bedrock or technical marker blocks into falling blocks.
- Handle block entities carefully.
- Prevent duplicate drops or duplicated tile entity contents.
- Avoid starting collapse twice.

The event should look organic, but it must not spawn the whole island as falling block entities in one tick.

## Air Element Item

The boss drops the Air Element directly.

The item should be one of the four accepted dragon egg ritual elements. After the player offers it to the dragon egg in Stage 2, the egg records Air as absorbed.

## Out Of Scope For First Version

- Non-command discovery clues.
- Full final boss model polish.
- Multiple Air Temple variants.
- A prebuilt fallen temple.
- Player rescue effects during collapse.
- Air blocks that always have gravity.
- Terrain block families unrelated to the temple.

## Open Implementation Decisions

- Final boss name.
- Exact per-tick falling block cap.
- Exact rarity and placement spacing.
- Exact cloud-height range.
- Final block palette and custom block list.
- Whether wind machinery blocks are decorative only or functional after being mined.
