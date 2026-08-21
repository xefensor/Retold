# Dragon Egg Ritual And Sacrifices

> Current design direction as of 2026-08-19. This document records the intended player-facing design; several parts are not implemented yet.

## Role In Progression

After the Ender Dragon is defeated and the world enters Stage 2, the Dragon Egg becomes the focus of the ritual that leads to Stage 3 and the Aender.

The ritual uses **six artifact sacrifices**. Four represent the classical elements and two represent the fundamental forces of Life and Death:

| Force | Sacrifice | Guardian / source | Current direction |
| --- | --- | --- | --- |
| Air | **Heavy Core** | **Gale Core** | Air Temple / Gale Core path exists and now rewards the Heavy Core. |
| Water | **Heart of the Sea** | **Elder Guardian** | Ocean Monument / Elder Guardian path exists and now rewards the Heart of the Sea. |
| Fire | **Nether Reactor Core** | **Wildfire** | Initial path implemented: a rare Stage 2+ Nether guardian roams, attacks undead, and guarantees the core. |
| Earth | **Lodestone** | **Custom Earth Guardian** | Working item choice. The guardian's defining mechanic is terrain construction: it places/builds blocks between itself and the player, forcing the player to mine through its defenses. |
| Death | **Nether Star** | **Wither** | The Wither is the Death-associated challenge and the Nether Star is its sacrifice. |
| Life | **Totem of Undying** | **Evoker** | The Evoker/Totem connection matches Retold's existing illager lore around avoiding death. |

The **Nether Reactor Core** is the confirmed Fire artifact. The **Lodestone** remains a working Earth choice and may still change.

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

Its defining combat behavior is **building with the terrain**. The guardian creates block barriers and defenses between itself and the player. Reaching it requires the player to mine through those defenses, making mining itself a central part of the encounter rather than using an ordinary combat boss with an Earth visual theme.

The current Earth sacrifice is the **Lodestone**. The guardian's final name, appearance, location, exact block-placement rules, and acquisition flow are still to be designed.

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

Implementation should turn on the complete six-sacrifice hatch requirement only when Earth is
survival-obtainable.
