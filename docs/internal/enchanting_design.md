# Retold Enchanting Design

> Developer-confirmed design direction. This document records the current intended player-facing enchanting model. It does not claim implementation, and exact symbols, costs, thresholds, enchant compatibility, UI layout, clue placement, and balancing remain future work.

## Core Role In Progression

Enchanting should become relevant around iron-tier equipment and become substantially more important by diamond tier.

Current direction:

- iron equipment can benefit meaningfully from enchanting, but enchanting is not yet mandatory
- enchanting becomes increasingly important through later material progression
- diamond equipment is intended to rely strongly on enchanting rather than simply being a universally superior raw-material tier
- XP/experience is energy and should remain a resource that can compete with other Retold uses of energy

## Existing Lore Basis

Retold's established enchanting lore remains the foundation:

- experience/XP is usable magical energy
- obsidian in the enchanting table directs/bends that energy into the item
- lapis lazuli is the physical medium used to write the enchantment onto the item
- the writing uses the Standard Galactic Alphabet (SGA)
- bookshelves provide magical knowledge to the enchanting setup

The rework should make those ideas mechanically meaningful rather than leaving the SGA text as decorative nonsense.

## Player Learns A Magical Language

Enchanting should be a discovery/learning system, not primarily a random-roll system or a list of recipes handed to the player.

The player manually enters short SGA words at the enchanting table. At first the glyphs are not explained. The player learns their meanings by experimenting, observing successful enchantments, comparing repeated patterns, and finding in-world clues.

The system should be simple enough to learn naturally and not become a linguistics simulator.

## Three-Part Enchantment Words

Each enchantment is expressed as a three-symbol semantic word:

```text
[DOMAIN] [EFFECT] [MODIFIER]
```

The actual player-facing representation is three SGA glyphs. Internally, each glyph has a semantic role/concept.

### Domain

The first symbol describes what class of thing or action the enchantment belongs to.

Possible examples include:

- weapon
- armor
- tool
- bow/projectile weapon
- boots
- helmet

Exact domain vocabulary remains to be designed. Domains should be reused where possible instead of creating a unique symbol for every enchantment.

### Effect

The second symbol describes the main magical action.

Possible examples include:

- damage
- protect
- fire
- push
- move
- drop/yield
- work/speed
- breathe
- freeze

### Modifier

The third symbol specifies what the effect applies to or how it is specialized.

Possible examples include:

- general
- undead
- arthropod
- target
- more
- original block
- fire
- explosion
- projectile
- fall
- water

## Example Enchantment Meanings

These are semantic examples only. The final SGA glyph assignments are not chosen yet.

| Enchantment | Domain | Effect | Modifier | Interpreted meaning |
| --- | --- | --- | --- | --- |
| Sharpness | weapon | damage | general | weapon deals greater general damage |
| Smite | weapon | damage | undead | weapon deals greater damage to undead |
| Bane of Arthropods | weapon | damage | arthropod | weapon deals greater damage to arthropods |
| Fire Aspect | weapon | fire | target | weapon ignites the target |
| Knockback | weapon | push | target | weapon pushes the target |
| Looting | weapon | drop | more | weapon causes greater loot yield |
| Efficiency | tool | work/speed | general | tool performs work faster |
| Fortune | tool | drop | more | tool produces greater resource yield |
| Silk Touch | tool | drop | original | tool preserves the original block/material form |
| Protection | armor | protect | general | armor protects against general damage |
| Fire Protection | armor | protect | fire | armor protects against fire |
| Blast Protection | armor | protect | explosion | armor protects against explosions |
| Projectile Protection | armor | protect | projectile | armor protects against projectiles |
| Feather Falling | boots | protect | fall | boots protect against falling |
| Respiration | helmet | breathe | water | helmet improves underwater breathing |
| Aqua Affinity | helmet | work | water | helmet improves underwater work |
| Depth Strider | boots | move | water | boots improve movement through water |
| Frost Walker | boots | freeze | water | boots freeze water while moving |
| Power | bow | damage | projectile | bow projectiles deal greater damage |
| Punch | bow | push | projectile | bow projectiles push targets |
| Flame | bow | fire | projectile | bow projectiles ignite targets |

The exact mapping can change if a cleaner shared vocabulary is found. The important rule is that the words should be semi-compositional rather than arbitrary passwords.

## Learning Through Repeated Patterns

A major goal is that players can infer meanings from repeated symbols.

For example, after discovering semantic equivalents of:

```text
weapon + damage + general   -> Sharpness
weapon + damage + undead    -> Smite
weapon + damage + arthropod -> Bane of Arthropods
```

the player can notice that the first two glyphs remain identical while the third changes. From this they can begin to infer the language without the game explicitly providing a translation table.

Likewise:

```text
armor + protect + general    -> Protection
armor + protect + fire       -> Fire Protection
armor + protect + explosion  -> Blast Protection
armor + protect + projectile -> Projectile Protection
```

A knowledgeable player should eventually be able to predict some valid enchantment words they have never successfully cast before by combining concepts they have already learned.

This should reward understanding rather than brute-force memorization.

## Enchantment Level Comes From Energy

Enchantment level is not encoded as another word or symbol.

The three-glyph word defines **what** enchantment is being performed. The amount of experience/energy the player sacrifices determines **how strongly** it is performed.

Conceptually:

```text
[weapon] [damage] [general] + low energy  -> Sharpness I
[weapon] [damage] [general] + more energy -> Sharpness II
[weapon] [damage] [general] + high energy -> higher Sharpness level
```

Exact energy thresholds are future balancing work.

This keeps the language small: the player learns one semantic word for Sharpness rather than separate passwords for Sharpness I through V.

## Successful Enchantments Become Known

Retold should record only enchantments the player has successfully performed/discovered.

After successful discovery, the player's knowledge UI should show the ordinary readable enchantment name alongside the same SGA word used to cast it.

Conceptually:

```text
Sharpness        [SGA: weapon] [SGA: damage] [SGA: general]
Fire Protection  [SGA: armor]  [SGA: protect] [SGA: fire]
Efficiency       [SGA: tool]   [SGA: work]    [SGA: general]
```

The discovered entry represents knowledge of the enchantment itself, not a specific level. Level remains an energy decision at the moment of enchanting.

Once an enchantment has been discovered, Retold should avoid forcing the human player to manually memorize and re-enter the word forever. The known-enchantment interface may provide a convenience action such as selecting/filling an already-known word. Exact UI behavior remains to be designed.

## Failed Experiments Should Give Useful Feedback

Experimentation should not be blind. The table should distinguish at least conceptually between:

- a meaningless/invalid three-symbol combination
- a valid magical word that does not apply to the current item
- a valid compatible word but insufficient energy/setup
- a successful enchantment

Prefer visual/audio/item reactions over explicit technical error text where practical.

The feedback should let the player form hypotheses without directly translating unknown glyphs for them.

## Bookshelves And Knowledge

Bookshelves remain part of the enchanting setup because established Retold lore says the table receives knowledge from surrounding books.

Exact mechanics are not settled yet.

The intended separation is:

- **player knowledge**: what SGA meanings/words the player has personally discovered and can understand/use intentionally
- **table/bookshelf knowledge**: what magical knowledge the physical enchanting setup has access to
- **energy/XP**: power sacrificed to perform the enchantment and determine its strength
- **lapis**: physical writing medium for the SGA inscription
- **obsidian**: material that directs the energy into the item

Do not reduce player learning to simply finding an enchanted book or collecting a recipe token. The player should have to learn the language through use/observation.

The exact future role of enchanted books and how bookshelf knowledge limits or enables enchantments remains undecided.

## SGA Presentation

The final UI should use actual Standard Galactic Alphabet glyph rendering, not placeholder Unicode symbols.

The system does **not** need every SGA glyph to retain its Latin-letter meaning. Retold may use a deliberately limited set of SGA glyphs as semantic magical symbols (for example weapon, damage, undead, fire, protect, water) as long as the visual language remains recognizably SGA and internally consistent.

The final vocabulary should remain small enough for a player to start recognizing recurring glyphs naturally.

## Design Guardrails

- Do not turn enchanting into a random reroll loop if the language system can provide intentional discovery instead.
- Do not make each enchantment an arbitrary unrelated three-symbol password; reuse semantic components.
- Do not encode enchantment levels as additional language complexity; levels come from sacrificed energy.
- Do not reveal unknown words in the known-enchantment list before the player successfully discovers them.
- Do not require external documentation/wiki knowledge for mandatory enchanting progression.
- Do not require the human player to remember every discovered sequence forever; discovery should be meaningful once, while repeated use can become convenient.
- Keep the language compact enough that pattern recognition is rewarding instead of annoying.

## Still Undecided

- exact SGA glyph-to-concept assignments
- exact list and number of domain/effect/modifier concepts
- exact XP/energy thresholds for enchantment levels
- lapis costs and whether they change with strength
- bookshelf/setup requirements for each enchantment or complexity class
- exact role of enchanted books
- how clues to unknown glyph meanings/words are distributed through the world
- exact enchanting-table UI and input method
- exact compatibility/stacking rules for enchantments after the wider enchantment audit
