# Dragon Egg Ritual And Sacrifices

> Current design direction as of 2026-09-07. This document records the intended player-facing design; several parts are not implemented yet.

## Role In Progression

After the Ender Dragon is defeated and the world enters Stage 2, the Dragon Egg becomes the focus of the ritual that leads to Stage 3 and the Aender.

The ritual uses **six artifact sacrifices**. Four represent the classical elements and two represent the fundamental forces of Life and Death:

| Force | Sacrifice | Guardian / source | Current direction |
| --- | --- | --- | --- |
| Air | **Heavy Core** | **Gale Core** | Air Temple / Gale Core path exists and now rewards the Heavy Core. |
| Water | **Heart of the Sea** | **Elder Guardian** | Ocean Monument / Elder Guardian path exists and now rewards the Heart of the Sea. |
| Fire | **Nether Reactor Core** | **Wildfire** | Initial path implemented: a rare Stage 2+ Nether guardian roams, attacks undead, and guarantees the core. |
| Earth | **Lodestone** | **Custom Earth Guardian** | Confirmed Jungle Pyramid labyrinth path. The blind guardian permanently excavates and relocates any loaded block while following players inside or outside the structure. |
| Death | **Nether Star** | **Wither** | The Wither is the Death-associated challenge and the Nether Star is its sacrifice. |
| Life | **Totem of Undying** | **Evoker** | The Evoker/Totem connection matches Retold's existing illager lore around avoiding death. |

The **Nether Reactor Core** is the Fire artifact, and **Lodestone** is the confirmed Earth artifact.
The Earth Guardian drops one Lodestone through its entity loot table (subject to normal mob-loot
rules). The ordinary Lodestone recipe and new Ruined Portal/Bastion Bridge chest Lodestones are
removed. Existing stacks remain valid; datapacks can deliberately override these acquisition rules.

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
generated terrain to find the enhanced structure. The implemented footprint is 77 or 101 blocks
wide across two connected levels. The upper maze floor lies 64 blocks below the pyramid
base, and the lower floor lies another fourteen blocks down. A long walkable switchback staircase
connects the secret room to the fixed entrance; the maze has a fixed central guardian chamber,
multiple stairs, loops, alternate routes, and randomized trap/treasure side branches. Additional
puzzle or combat-room systems are not part of the confirmed room direction.

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

Its defining combat behavior is **permanently reshaping the world around it**. The guardian can
excavate any non-air block in a loaded chunk to create paths for itself, retains the removed block
state as a bounded reserve, and relocates that material into persistent barriers elsewhere. There
is no labyrinth boundary or block whitelist: player builds, containers, puzzle mechanisms, block
entities, fluids, and normally indestructible blocks such as Bedrock are all valid. Every edit still
obeys `mobGriefing`, NeoForge's entity-griefing hook, and `RetoldWorldProtection`, allowing server
administrators and claim integrations to disable it. Relocation rather than unlimited creation
avoids an infinite block farm while leaving a permanent physical record of the encounter.

The guardian's final model and palette still need design approval. Side branches now receive up to
six content rooms per floor: pressure-plate arrow traps and occasional treasure chests. Rooms add
no lighting or spawners; normal darkness, biome, difficulty, distance, and mob-cap rules govern
natural mobs. Loot and finite dispenser ammunition use Retold-owned loot tables. Content version 1
is saved on new starts; older starts retain empty rooms, including their not-yet-generated chunks.
The initial implementation replaces new Jungle Pyramid starts
with a compatible composite structure under the same registry id, attaches a serialized labyrinth
piece, and places the deterministic two-level maze, safe secret-room staircase, carved cave
tunnels, irregular junction caverns, and ladder connections using a provisional natural-stone
palette. It now registers `retold:earth_guardian`, spawns exactly one structure-owned guardian at
the fixed chamber, and persists `DORMANT`, `AWAKENING`, `ROAMING`, and `DEFEATED` encounter state.
The guardian is an immovable, indestructible Stage 1 statue; a Survival player entering its chamber
in Stage 2 starts a three-second awakening and reveals its boss bar. The current Iron Golem model
and texture are code-level placeholders. After awakening, it uses Minecraft's vibration system to
patrol and investigate weighted remembered player clues without acquiring a sight target; the clue
and listener state survive reloads. Vibration clues may originate outside the maze. While pursuing
one, its terrain controller batches permanent changes: it excavates any loaded
non-air block into a persisted 128-state reserve and later spends that exact material on an
entity-clear barrier behind it. When ordinary navigation stops, a short route builder clears
three-wide body passages and constructs one-block ramp steps with longer landings, using only
solid full-block states from its reserve for support. It retains its 3.2-block height and physically
walks the carved route. Uphill/downhill heading persists across reloads; short work segments are
recomputed from the actual position, and player edits can invalidate and rebuild them. Excavation
and backfill share a budget of up to 12 changes every ten ticks (half a second). Once a segment
is walkable, spare budget can build a supported 3×3 wall three to six blocks behind the guardian,
at most once every forty ticks. Walls require at least 18 stored states and consume only solid
full-block states, leaving material for subsequent traversal; occupied and protected cells stay empty.
There is no block-type, ownership, or labyrinth-bound restriction on excavation;
only `mobGriefing`, NeoForge's entity-griefing hook, and Retold world protection can deny an edit.
Once roaming, it becomes damageable and aggressively closes on heard positions. At contact range
it can strike the heard Survival player with its Iron Golem attack, at most once every 30 ticks.
A fresh obstruction check prevents wall hits, and Creative/Spectator players remain excluded.
Pursuit is one-third faster than the initial investigation pass and continues during environmental
warnings; reaching a clue no longer discards it before contact combat. No live sight target is acquired.
A remembered vibration within 24 blocks marks the floor for a
30-tick environmental warning. The guardian cycles through pulling up to five real blocks out
of a valid ceiling as damaging falling debris, turning a 3×3 layer of ground beneath the marked
position into real lava sources, and opening a 3×3 pit up to four blocks deep. The pit remains in the
world and causes ordinary falling and landing damage; protected blocks stop excavation in their
column. Creatures left above the lava patch fall into lava and take normal
Minecraft contact damage; the attack adds no scripted damage or launch. Ceiling dust identifies the actual blocks
being loosened, and every selected block must have an empty vertical fall path to the marked area.
If no protected-safe, unobstructed ceiling block exists, the collapse falls back to the eruption.
The location does not follow the player after the warning
begins, so dodging remains possible without giving the blind guardian sight-like tracking. The
cooldown, selected hazard, target, and pending warning persist across save/load. Ceiling removal
uses the normal entity-griefing and Retold world-protection policy; lava conversion checks both break
and place permission. Lava sources persist and can flow, burn surroundings, and affect any creature
according to ordinary Minecraft rules. The initial trap/treasure rooms, Lodestone reward, and
six-offering ritual are implemented; natural-world generation, balance, reward retrieval around
lava, multiplayer progression, and final presentation still require verification.

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

## Implementation Status

The initial acquisition paths and six-sacrifice gate are wired:

- `RetoldRitualOffering` reserves stable saved-state bits for all six sacrifices.
- The Dragon Egg accepts Heavy Core, Heart of the Sea, Nether Reactor Core, Lodestone, Totem of
  Undying, and Nether Star, consumes successful offerings, and rejects duplicates without consumption.
- The legacy `WATER_ELEMENT` and `AIR_ELEMENT` items remain accepted but are no longer encounter
  rewards or Creative-tab progression entries.
- All six are now required, in any order. Old offering bits retain their meanings; unfinished
  Stage 2 worlds must supply their missing Fire/Earth artifacts. Worlds already in Stage 3 stay there.
- Existing generated vanilla pyramids gain no labyrinth; existing Retold labyrinth guardians can
  drop the newly wired reward, while new room content requires a newly generated structure start.
  Previously defeated guardians stay defeated; those worlds need another encounter for Lodestone.
- Natural encounter balance, multi-seed/chunk-border generation, and dedicated multiplayer and
  upgraded-world end-to-end progression still need verification.
