# Retold Test Gallery

Create a new **Creative** world with **cheats enabled**, enter the Overworld, and run:

```mcfunction
/retold testworld
```

The command builds a lit aerial gallery centered above you near the world height limit and teleports
you to its entrance after a successful build.
On a server, it requires operator permission level 2 and must be run by a player. It builds inside
the current save; it does not create a new save or dimension.

The gallery contains:

- every registered Retold item in a labeled frame, with usable copies in the entrance chests
- every registered Retold block, including wall torches, sign variants, complete doors, and a live horizontal Aender portal
- every registered Retold entity, plus adult and small armor stands wearing Steel equipment
- workshop stations, the six Dragon Egg sacrifices, an egg, and testing supplies

Currently this covers **52 items, 37 blocks, and 6 entity types**. The catalog is read from the live
registries, including the legacy Water and Air Element tokens. It covers Retold content; natural
structures, complete progression paths, and the full vanilla catalog need their own tests.

Mobs and boats are persistent, invulnerable, and frozen on the server so they stay in place without
attacking or disappearing on Peaceful. Client rendering still runs, but this is a visual inspection
gallery rather than a natural AI or encounter test. Use copies from the supply chests to try tools,
equipment, and placeable items. The portal can perform real dimension travel; the Chronolith can
be activated by interaction.

To choose the platform's minimum corner yourself, supply block coordinates:

```mcfunction
/retold testworld 0 300 0
```

For an explicit origin, first move near the gallery's horizontal center: approximately `33, 52`
for the example above. Allow nearby chunks to become active before running the command.

At the current catalog size, reserve **66 × 104 × 10 blocks of empty air**, starting at that corner
and extending east, south, and upward. The depth can grow as content is added. The command checks
the whole reserved space, build height, world border, loaded entity sections, entities, and Retold
world/portal protection before placing anything. An occupied or denied location is rejected.
If the area is "not fully active yet," stay near its center and retry shortly. The entire footprint
must be inside active entity-ticking chunks; very low simulation distances may need increasing.
Rejection places nothing and does not teleport you or queue a later build. Choose another empty
corner to create another gallery; running it again does not overwrite an existing one.

The command preserves the player's game mode, world stage, and game rules. Use a disposable test
world for experiments involving the live portal, ritual, or other functional blocks. Subjective
in-game appearance, animation, shader/resource-pack comparisons, and final visual approval remain
the developer's responsibility.
