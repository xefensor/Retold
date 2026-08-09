# Retold Enchanting Test Guide

> Developer/testing cheat sheet for the current Minecraft 26.2 implementation. This intentionally
> reveals every spell word and should not be treated as player-facing documentation.

## Quick Table Test

1. Open an enchanting table. Bookshelves are unnecessary and must not change the result.
2. Put the test item in the left table slot and at least three lapis lazuli in the lapis slot.
3. Choose level `I-V`. Selecting a known spell disables levels above its displayed maximum; for an
   unknown manually entered word, use the maximum in the table below.
4. Click the three glyphs in the listed word from left to right.
5. Press **Write**.

The keyboard is arranged in alphabetic/debug-code order:

```text
A B C D E F G H I
J K L M N O P Q R
S T U V W X Y Z
```

These letters are only positions for testers. The screen renders them with Minecraft's SGA font.
For example, Sharpness is `X`, then `E`, then `J`.

The same actions are available from the physical keyboard:

- `A-Z`: append the corresponding glyph
- `Backspace`: remove the last glyph
- number row or keypad `1-5`: select the requested level
- `Enter` or keypad Enter: Write

Holding Enter or clicking rapidly must submit at most one cast while the server response is pending.

For any row marked **Yes**, a plain Book is also a valid target and should become an Enchanted
Book. This is the easiest way to verify a word independently of equipment compatibility.

## Costs And Expected Success

Every successful cast consumes exactly:

| Requested level | Experience levels | Lapis |
| --- | ---: | ---: |
| I | 5 | 3 |
| II | 10 | 3 |
| III | 15 | 3 |
| IV | 20 | 3 |
| V | 25 | 3 |

After a successful cast:

- the item receives exactly the requested enchantment level
- a Book becomes an Enchanted Book
- the player learns the spell
- the readable enchantment name appears in tooltips
- the three inscription slots clear and the changed item slot receives a brief green highlight
- the spell appears in the table's **Known spells** panel when it is valid for the inserted item;
  selecting it refills the three glyphs and disables levels above the displayed maximum
- Creative players with infinite materials receive the result without spending lapis or levels

## Complete Spell Test Matrix

`Table cast? No` is intentional. Those seven enchantments retain words for found-item tooltips,
enchanted-book evidence, and anvil learning, but the table rejects and omits them from its Known
spells panel to preserve vanilla enchanting-table eligibility.

| Enchantment | Word | Max | Table cast? | Representative compatible item |
| --- | --- | ---: | --- | --- |
| Protection | `AQJ` | IV | Yes | Iron Chestplate |
| Fire Protection | `AQH` | IV | Yes | Iron Chestplate |
| Feather Falling | `AQG` | IV | Yes | Iron Boots |
| Blast Protection | `AQF` | IV | Yes | Iron Chestplate |
| Projectile Protection | `AQP` | IV | Yes | Iron Chestplate |
| Respiration | `AQW` | III | Yes | Iron Helmet |
| Aqua Affinity | `AYW` | I | Yes | Iron Helmet |
| Thorns | `AET` | III | Yes | Iron Chestplate |
| Depth Strider | `ANW` | III | Yes | Iron Boots |
| Frost Walker | `ANK` | II | **No** | Iron Boots via enchanted book/anvil |
| Curse of Binding | `LCA` | I | **No** | Iron Chestplate via enchanted book/anvil |
| Soul Speed | `ANV` | III | **No** | Iron Boots via enchanted book/anvil |
| Swift Sneak | `ANT` | III | **No** | Iron Leggings via enchanted book/anvil |
| Sharpness | `XEJ` | V | Yes | Iron Sword |
| Smite | `XEV` | V | Yes | Iron Sword |
| Bane of Arthropods | `XEB` | V | Yes | Iron Sword |
| Knockback | `XRL` | II | Yes | Iron Sword |
| Fire Aspect | `XHL` | II | Yes | Iron Sword |
| Looting | `XZM` | III | Yes | Iron Sword |
| Sweeping Edge | `XEO` | III | Yes | Iron Sword |
| Efficiency | `UYJ` | V | Yes | Iron Pickaxe |
| Silk Touch | `UZT` | I | Yes | Iron Pickaxe |
| Unbreaking | `LQJ` | III | Yes | Iron Pickaxe |
| Fortune | `UZM` | III | Yes | Iron Pickaxe |
| Power | `DEP` | V | Yes | Bow |
| Punch | `DRP` | II | Yes | Bow |
| Flame | `DHP` | I | Yes | Bow |
| Infinity | `DZM` | I | Yes | Bow |
| Luck of the Sea | `IZJ` | III | Yes | Fishing Rod |
| Lure | `INL` | III | Yes | Fishing Rod |
| Loyalty | `XNT` | III | Yes | Trident |
| Impaling | `XEW` | V | Yes | Trident |
| Riptide | `XNW` | III | Yes | Trident |
| Channeling | `XHP` | I | Yes | Trident |
| Multishot | `DEO` | I | Yes | Crossbow |
| Quick Charge | `DYJ` | III | Yes | Crossbow |
| Piercing | `DEL` | IV | Yes | Crossbow |
| Density | `XEG` | V | Yes | Mace |
| Breach | `XEA` | IV | Yes | Mace |
| Wind Burst | `XRG` | III | **No** | Mace via enchanted book/anvil |
| Lunge | `XNL` | III | Yes | Iron Spear |
| Mending | `LSJ` | I | **No** | Durable item via enchanted book/anvil |
| Curse of Vanishing | `LCV` | I | **No** | Iron Pickaxe via enchanted book/anvil |

## Expected No-Op Tests

Each case below must leave the item, lapis count, and experience level unchanged:

- enter a word not present in the matrix, such as `AAA`
- enter a real word on an incompatible item, such as Sharpness `XEJ` on Iron Boots
- manually enter an unknown spell word and select a level above its maximum, such as Aqua Affinity
  `AYW` level II; a known Aqua Affinity selection disables levels II-V instead
- attempt a **Table cast? No** word, such as Mending `LSJ`, even on a Book
- provide fewer than three lapis
- provide fewer experience levels than the selected level requires
- cast the same or a lower level over an equal/higher existing level
- cast a vanilla-conflicting enchantment over an existing enchantment

The screen deliberately gives the same quiet low note and brief red item-slot highlight for invalid,
incompatible, unavailable, conflicting, and non-upgrading attempts. The inscription remains entered,
and the cue never identifies the rejection reason. This prevents the table from automatically
decoding the language.

## Vanilla Conflict Checks

Refill lapis before each attempt. Apply the first enchantment successfully, then attempt the second
on the same item. The second cast must be a no-op with no cost.

| First enchantment | Conflicting second enchantment | Suggested item |
| --- | --- | --- |
| Protection `AQJ` | Fire Protection `AQH` | Iron Chestplate |
| Sharpness `XEJ` | Smite `XEV` | Iron Sword |
| Fortune `UZM` | Silk Touch `UZT` | Iron Pickaxe |
| Multishot `DEO` | Piercing `DEL` | Crossbow |
| Loyalty `XNT` | Riptide `XNW` | Trident |

Other current vanilla-exclusive groups are:

- Protection, Fire Protection, Blast Protection, and Projectile Protection
- Sharpness, Smite, Bane of Arthropods, Impaling, Density, and Breach when an item supports both
- Fortune and Silk Touch
- Depth Strider and Frost Walker
- Infinity and Mending
- Multishot and Piercing
- Riptide against Loyalty or Channeling

## Learning And Tooltip Checks

### Manual deduction

1. Start with an unknown table-castable spell, for example Sharpness `XEJ`.
2. Its pre-existing enchanted item/book tooltip should show only the SGA word and level.
3. Cast `XEJ` successfully at any valid level.
4. The tooltip should now show `Sharpness <level>` with `XEJ` beneath it.
5. Sharpness should now be selectable from **Known spells**.

### Known-spell item filtering

1. With no item inserted, verify Known spells shows learned table-castable spells and their maximum
   Roman-numeral levels, but omits learned treasure/non-table spells such as Mending.
2. Insert Iron Boots. Protection should remain listed while Sharpness should disappear.
3. Insert an Iron Sword. Sharpness should appear again.
4. Give that sword Smite. Sharpness should disappear because the two spells conflict.
5. Put a spell on an item at its maximum level. That spell should no longer be offered for that item.
6. Remove the item. The general table-eligible known list should return.

### Enchanted-book/anvil learning

1. Obtain an Enchanted Book containing a spell the player does not know.
2. Merely holding or reading the book must not teach it.
3. Apply it to a compatible item and take the anvil output.
4. Only enchantments that transferred or improved should become known.
5. On a multi-enchantment book, incompatible or unchanged entries must remain unknown.

## Verification Still Needed

Automated tests cover the catalog, costs, atomic cast validation, book conversion, menu slot commit,
request/success payload encoding, known-spell item filtering, maximum-level lookup, conflicts, and
learning foundations. This manual pass should concentrate on:

- glyph/button alignment and readability at different GUI scales
- item and lapis slot interaction, shift-clicking, and closing the screen
- page controls and long known-enchantment names
- physical and keypad controls, including Shift-letter input and rapid/repeated Enter
- immediate item, lapis, XP, tooltip, inscription clearing, success highlight, and known-list updates
  after casting
- the same low-note/red-highlight cue for every rejected cast, with the inscription preserved
- Survival versus Creative behavior
- a separate client connected to a dedicated server
