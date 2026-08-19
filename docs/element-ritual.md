# Dragon Egg Ritual And Sacrifices

> Current design direction as of 2026-08-18. This document records the intended player-facing design; several parts are not implemented yet.

## Role In Progression

After the Ender Dragon is defeated and the world enters Stage 2, the Dragon Egg becomes the focus of the ritual that leads to Stage 3 and the Aender.

The ritual uses **six artifact sacrifices**. Four represent the classical elements and two represent the fundamental forces of Life and Death:

| Force | Sacrifice | Guardian / source | Current direction |
| --- | --- | --- | --- |
| Air | **Heavy Core** | **Gale Core** | Air Temple / Gale Core path already exists, but currently rewards the temporary Air Element item. |
| Water | **Heart of the Sea** | **Elder Guardian** | Ocean Monument / Elder Guardian path already exists, but currently rewards the temporary Water Element item. |
| Fire | **Nether Reactor Core** | **Wildfire** | Working item choice. Wildfire is planned as a Stage 2 Nether guardian that roams the Nether and attacks undead. |
| Earth | **Lodestone** | **Custom Earth Guardian** | Working item choice. The guardian's defining mechanic is terrain construction: it places/builds blocks between itself and the player, forcing the player to mine through its defenses. |
| Death | **Nether Star** | **Wither** | The Wither is the Death-associated challenge and the Nether Star is its sacrifice. |
| Life | **Totem of Undying** | **Evoker** | The Evoker/Totem connection matches Retold's existing illager lore around avoiding death. |

The **Nether Reactor Core** and **Lodestone** are current working choices and may still change if a better Fire or Earth artifact is found.

## Ritual Rules

- The six sacrifices may be completed in any order unless a later encounter design introduces a justified dependency.
- Each artifact is offered only once.
- **A successful offering consumes the artifact.** The ritual is a sacrifice, not a check that merely requires the player to possess the item.
- Stage 3 begins only after all six required sacrifices have been accepted by the Dragon Egg.
- Air, Water, Fire, and Earth are the four classical elements. Life and Death are intentionally separate from that elemental group rather than being described as two additional elements.

## Encounter Direction

### Air — Gale Core

The existing Air Temple and Gale Core encounter remain the Air path. The intended artifact reward is the **Heavy Core**, replacing the temporary custom Air Element reward.

### Water — Elder Guardian

The Ocean Monument and Elder Guardian remain the Water path. The intended artifact reward is the **Heart of the Sea**, replacing the temporary custom Water Element reward.

### Fire — Wildfire

The **Wildfire** is the Fire-associated guardian. It should exist as part of the Stage 2 Nether rather than only as an isolated boss-room encounter: it roams the Nether, acts as a guardian of the realm, and attacks undead.

The current Fire sacrifice is the **Nether Reactor Core**. Exact acquisition and encounter mechanics are still to be designed.

### Earth — Custom Earth Guardian

Earth uses a new Retold mob rather than repurposing an existing Minecraft boss.

Its defining combat behavior is **building with the terrain**. The guardian creates block barriers and defenses between itself and the player. Reaching it requires the player to mine through those defenses, making mining itself a central part of the encounter rather than using an ordinary combat boss with an Earth visual theme.

The current Earth sacrifice is the **Lodestone**. The guardian's final name, appearance, location, exact block-placement rules, and acquisition flow are still to be designed.

### Death — Wither

The **Wither** is associated with Death and provides the **Nether Star** sacrifice.

The separate roadmap question of whether a Wither/Nether Star should also be required before the first Ender Dragon remains unresolved. That decision should account for the Nether Star already having a required Stage 2 role so the progression does not accidentally require redundant Wither kills without a good reason.

### Life — Evoker

The **Evoker** is associated with Life through the **Totem of Undying**. In Retold lore, evokers' experimentation with energy and avoiding death already gives this pairing a direct worldbuilding connection.

The exact Life-path encounter and Totem acquisition rules are still open design work.

## Implementation Gap

The current implementation is intentionally behind this design:

- `RetoldElementType` currently contains only Water, Fire, Earth, and Air.
- The Dragon Egg currently requires only Water and Air while Fire and Earth remain unfinished.
- The current ritual recognizes Retold's temporary `WATER_ELEMENT` and `AIR_ELEMENT` items.
- Gale Core currently drops the temporary Air Element.
- Elder Guardian currently provides the temporary Water Element.
- Fire, Earth, Life, and Death are not yet wired into the Dragon Egg ritual.

Implementation should migrate toward the six sacrifices above without treating the current temporary two-item requirement as the final design.
