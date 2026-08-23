# Dragon Egg Ritual And Sacrifices

> Current design direction as of 2026-08-23. This document records the intended player-facing design; several parts are not implemented yet.

## Role In Progression

After the Ender Dragon is defeated and the world enters Stage 2, the Dragon Egg becomes the focus of the ritual that leads to Stage 3 and the Aender.

The ritual uses **six artifact sacrifices**. Four represent the classical elements and two represent the fundamental forces of Life and Death:

| Force | Sacrifice | Guardian / source | Current direction |
| --- | --- | --- | --- |
| Air | **Heavy Core** | **Gale Core** | Air Temple / Gale Core path exists and now rewards the Heavy Core. |
| Water | **Heart of the Sea** | **Elder Guardian** | Ocean Monument / Elder Guardian path exists and now rewards the Heart of the Sea. |
| Fire | **Nether Reactor Core** | **Wildfire** | Initial path implemented: a rare Stage 2+ Nether guardian roams, attacks undead, and guarantees the core. |
| Earth | **Undecided** | **Custom Earth Guardian** | Confirmed Jungle Pyramid labyrinth path. The blind guardian permanently excavates and relocates maze-owned blocks while hunting players through the structure. |
| Death | **Nether Star** | **Wither** | The Wither is the Death-associated challenge and the Nether Star is its sacrifice. |
| Life | **Totem of Undying** | **Evoker** | The Evoker/Totem connection matches Retold's existing illager lore around avoiding death. |

The **Nether Reactor Core** is the confirmed Fire artifact. The Earth artifact remains undecided;
Lodestone is still a candidate, but the design must prevent its ordinary acquisition from bypassing
the guardian path if it is retained.

## Ritual Rules

- The six sacrifices may be completed in any order unless a later encounter design introduces a justified dependency.
- Each artifact is offered only once.
- **A successful offering consumes the artifact.** The ritual is a sacrifice, not a check that merely requires the player to possess the item.
- Stage 3 begins only after all six required sacrifices have been accepted by the Dragon Egg.
- Air, Water, Fire, and Earth are the four classical elements. Life and Death are intentionally separate from that elemental group rather than being described as two additional elements.

The egg recognizes an artifact by its item identity, regardless of which exact entity or chest
produced that stack. Retold controls unintended acquisition routes directly instead of placing a
hidden provenance marker on encounter-earned items. Trial Chambers are disabled in newly generated
terrain, and buried treasure no longer provides a Heart of the Sea, keeping the Heavy Core and
Heart paths tied to their named encounters. Totems and Nether Stars retain their intended Evoker
and Wither acquisition paths. Existing items from older worlds remain valid.

## Encounter Direction

### Air — Gale Core

The existing Air Temple and Gale Core encounter remain the Air path. The Gale Core now drops the
**Heavy Core**. The temporary custom Air Element remains registered and accepted only so existing
worlds do not lose held progression items.

### Water — Elder Guardian

The Ocean Monument and Elder Guardian remain the Water path. Elder Guardians now guarantee a
**Heart of the Sea**, and buried treasure no longer supplies one. The temporary custom Water
Element remains registered and accepted only for existing-world compatibility.

### Fire — Wildfire

The **Wildfire** is the Fire-associated roaming miniboss. From Stage 2 onward, it appears very
rarely throughout the Nether rather than in a boss room, accompanied by three to five Blazes. It is
a much stronger Blaze-derived Nether Remnant with reinforced shields, powerful fireballs, and a
close-range shockwave, and it uses Retold faction targeting to attack undead. While roaming, it
leads its Blaze escorts in a numbered single-file patrol; the formation disperses as soon as the
group enters combat or the wounded leader retreats toward fire.

Each Wildfire guarantees one **Nether Reactor Core**. The Dragon Egg accepts and consumes that core
as Fire. Natural rarity, terrain fit, combat pacing, presentation, and Blaze-escort formation still
need in-game verification.

### Earth — Custom Earth Guardian

Earth uses a new Retold mob rather than repurposing an existing Minecraft boss.

Every newly generated Jungle Pyramid contains a deterministic, seed-randomized **Earth
Labyrinth** beneath it. Solving the pyramid's existing lever puzzle reveals the staircase into the
maze. Already-generated Jungle Pyramids remain untouched; upgraded worlds must explore newly
generated terrain to find the enhanced structure. The intended footprint is approximately 40–60
blocks wide across two connected levels. The upper maze floor lies 64 blocks below the pyramid
base, and the lower floor lies another seven blocks down. A long walkable switchback staircase
connects the secret room to the fixed entrance; the maze has a fixed central guardian chamber,
multiple stairs, loops, alternate routes, and randomized room modules. The exact trap, puzzle, and
treasure-room contents remain to be designed.

The labyrinth should read as an ancient cave network that was cut, reinforced, and occupied rather
than as a regular block-built grid. Its implemented base geometry offsets junctions from their
logical cells, bends connecting tunnels through deterministic intermediate points, and uses
separated rough tunnels, irregular junction caverns, and a depth-aware Stone, Deepslate, Andesite,
and Tuff palette. Cobblestone and Mossy Cobblestone are limited to ruined structural accents and
the access stairs.

Before Stage 2, the guardian is an immovable, indestructible ancient jungle stone statue in the
central chamber. It has no AI, boss bar, damage, knockback, pushing, or portal movement. Reaching
Stage 2 makes it eligible to awaken, but it remains dormant until a player returns to the chamber.
Its persisted encounter lifecycle is `DORMANT → AWAKENING → ROAMING/COMBAT → DEFEATED`. A defeated
labyrinth remains permanently cleared and its guardian does not respawn.

The guardian is blind. It patrols and navigates through its knowledge of the labyrinth while mining,
movement, block placement, containers, projectiles, explosions, direct damage, and very close
ground contact provide differently weighted vibration clues. Sneaking reduces movement noise, and
the guardian investigates a remembered vibration position instead of receiving perfect awareness
through walls. In multiplayer it chooses among actual vibration sources rather than automatically
knowing every player's position.

Its defining combat behavior is **permanently reshaping the maze**. The guardian excavates only
maze-owned blocks to create paths for itself, retains the removed material as a bounded reserve,
and relocates that material into persistent barriers elsewhere. It may roam and fight throughout
the labyrinth before withdrawing toward the central chamber under pressure. It never edits the
entrance staircase, containers, puzzle mechanisms, player-placed blocks, block entities, unrelated
cave terrain, or positions outside the labyrinth. Every edit must obey `mobGriefing`, NeoForge's
entity-griefing hook, and `RetoldWorldProtection`. Relocation rather than unlimited creation avoids
an infinite block farm while leaving a permanent physical record of the encounter.

The guardian's final name, exact model and palette, attacks, room-module contents, and Earth
sacrifice are still to be designed. The initial implementation replaces new Jungle Pyramid starts
with a compatible composite structure under the same registry id, attaches a serialized labyrinth
piece, and places the deterministic two-level maze, safe secret-room staircase, carved cave
tunnels, irregular junction caverns, and ladder connections using a provisional natural-stone
palette. The statue,
guardian behavior, encounter persistence, room modules, reward, ritual wiring, and final visual
treatment are not implemented yet.

### Death — Wither

The **Wither** is associated with Death and provides the **Nether Star** sacrifice. The Dragon Egg
now accepts and consumes the Nether Star as the Death offering.

The separate roadmap question of whether a Wither/Nether Star should also be required before the first Ender Dragon remains unresolved. That decision should account for the Nether Star already having a required Stage 2 role so the progression does not accidentally require redundant Wither kills without a good reason.

### Life — Evoker

The **Evoker** is associated with Life through the **Totem of Undying**. In Retold lore, evokers'
experimentation with energy and avoiding death already gives this pairing a direct worldbuilding
connection. The Dragon Egg now accepts and consumes the Totem as the Life offering.

The broader Life-path encounter presentation remains open design work; the accepted artifact and
its Evoker source are now implemented.

## Implementation Gap

The current implementation is intentionally behind this design:

- `RetoldRitualOffering` reserves stable saved-state bits for all six sacrifices.
- The Dragon Egg accepts Heavy Core, Heart of the Sea, Nether Reactor Core, Totem of Undying, and
  Nether Star, consumes successful offerings, and rejects duplicates without consuming them.
- The legacy `WATER_ELEMENT` and `AIR_ELEMENT` items remain accepted but are no longer encounter
  rewards or Creative-tab progression entries.
- The temporary hatch threshold is Water, Air, Life, and Death so Stage 3 remains
  survival-obtainable while the other acquisition paths are unfinished.
- Fire is accepted and persisted but is not yet required for hatching. Earth is represented in
  saved ritual state but is not yet accepted or required.
- `RetoldRitualOffering.EARTH` still carries `Lodestone` as its unexposed working label. Because no
  Earth item is accepted yet and the saved mask stores only the stable Earth bit, the final artifact
  can remain undecided without changing current survival behavior or moving that save-format bit.

Implementation should turn on the complete six-sacrifice hatch requirement only when Earth is
survival-obtainable.
