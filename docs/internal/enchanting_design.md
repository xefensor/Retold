# Retold Enchanting Design

> Developer-confirmed design direction. This document records the current intended player-facing enchanting model. It does not claim implementation, and exact symbols, costs, thresholds, enchant compatibility, UI layout, loot distribution, and balancing remain future work.

## Core Role In Progression

Enchanting should become relevant around iron-tier equipment and become substantially more important by diamond tier.

Current direction:

- iron equipment can benefit meaningfully from enchanting, but enchanting is not yet mandatory
- enchanting becomes increasingly important through later material progression
- diamond equipment is intended to rely strongly on enchanting rather than simply being a universally superior raw-material tier
- XP/experience is energy and should remain a resource that can compete with other Retold uses of energy

## Existing Lore Basis

Retold's enchanting lore remains the foundation:

- experience/XP is usable magical energy
- obsidian in the enchanting table directs/bends that energy into the item
- lapis lazuli is the physical medium used to write the enchantment onto the item
- the writing uses the Standard Galactic Alphabet (SGA)

Bookshelves are **not** required around the enchanting table in the current design. The old idea that surrounding bookshelves provide the table with enchantment power/knowledge is superseded by the player-learning system described below.

The rework should make the SGA writing and magical-energy lore mechanically meaningful rather than leaving enchanting as a random roll with decorative text.

## Player Learns A Magical Language

Enchanting should be a discovery/learning system, not primarily a random-roll system or a list of recipes handed to the player.

The player manually enters short SGA words at the enchanting table. At first the glyphs are not explained. The player learns their meanings by using enchanted items/books, observing results, comparing repeated patterns, and experimenting with combinations.

The system should be simple enough to learn naturally and not become a linguistics simulator.

Retold should never provide a direct glyph-translation screen such as `this glyph = weapon` or `this glyph = fire`. Understanding individual glyphs belongs to the human player's reasoning, not to a progression checklist maintained by the UI.

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

## Unknown Enchantments And Item Tooltips

The readable enchantment name should depend on whether that player knows the spell.

An enchanted item always physically carries its SGA inscription, so the glyphs are visible even when the enchantment is unknown. An unknown enchantment should **not** expose its ordinary human-readable name in the tooltip.

Conceptually, an unknown enchanted item shows:

```text
Iron Sword
[SGA GLYPH] [SGA GLYPH] [SGA GLYPH] III
```

Once that player knows the spell, the same enchantment is shown as:

```text
Iron Sword
Sharpness III
[SGA GLYPH] [SGA GLYPH] [SGA GLYPH]
```

The same rule applies to enchanted books.

Unknown book:

```text
Enchanted Book
[SGA GLYPH] [SGA GLYPH] [SGA GLYPH] IV
```

Known book:

```text
Enchanted Book
Fire Protection IV
[SGA GLYPH] [SGA GLYPH] [SGA GLYPH]
```

If an item contains multiple enchantments, known and unknown enchantments may appear together. Each enchantment is identified independently according to that player's knowledge.

An unknown enchanted item still functions normally. The magic already written on the item does not require the player to understand it. Knowledge matters for identifying and intentionally reproducing the enchantment, not for activating an already-enchanted item.

The tooltip must never translate individual glyphs into their semantic concepts. It shows only the complete SGA word and, once known, the ordinary enchantment name.

## Enchanted Books As Loot And Language Evidence

Vanilla-style enchanted books remain lootable exploration rewards and retain their normal practical role: they contain a pre-written enchantment that can be transferred to compatible equipment through an anvil.

Before the spell is known, the book exposes its SGA word and level but not its readable enchantment name. The player can still preserve, compare, and experiment with unknown books.

Applying an enchanted book to a compatible item through an anvil is a major discovery route. When the player successfully transfers that enchantment, he experiences what the magic actually does and the spell becomes known to him.

At that moment:

- the enchantment's readable name becomes available to that player
- the complete three-glyph word is recorded as a known enchantment/recipe for the enchanting table
- future items/books carrying the same spell can show the readable name as well as the SGA word

The level on the enchanted book does not become part of the learned recipe. A Smite I book and a Smite V book contain the same SGA word; only the amount/strength of energy already bound into that physical book differs.

Finding or merely holding a book does **not** unlock its readable name or recipe. The player has to actually use the enchantment successfully through the anvil, or independently discover/cast it at the enchanting table.

## Learning Through Repeated Patterns

Once the player has identified several enchantments, the known names and persistent SGA words give enough evidence to reverse-engineer the language.

For example, after learning the semantic equivalents of:

```text
weapon + damage + general   -> Sharpness
weapon + damage + undead    -> Smite
weapon + damage + arthropod -> Bane of Arthropods
```

the player can notice that the first two glyphs remain identical while the third changes. From this he can begin to infer the language without the game explicitly providing a translation table.

Likewise:

```text
armor + protect + general    -> Protection
armor + protect + fire       -> Fire Protection
armor + protect + explosion  -> Blast Protection
armor + protect + projectile -> Projectile Protection
```

Cross-family repetition can provide stronger clues. If the same unknown glyph appears in Fire Aspect and Fire Protection after both spells have been identified, the player has good evidence that the shared glyph represents the fire concept.

No game UI should confirm the player's deduction of an individual glyph. The satisfaction comes from the human player understanding the pattern himself.

## Two Routes To Learning A Spell

There are two intended routes by which a spell becomes known and receives a convenient enchanting-table recipe entry.

### 1. Anvil Learning

The player obtains an enchanted book and successfully combines it with a compatible item in an anvil.

The transfer reveals/teaches the spell and records its SGA word for future intentional enchanting.

### 2. Language Deduction

The player manually enters a valid three-glyph word at an enchanting table and successfully performs the enchantment, even if he has never identified or used a book containing that complete spell.

For example, a player who has personally inferred the glyphs corresponding to `armor`, `protect`, and `fire` may try that combination and discover Fire Protection without ever having previously identified a Fire Protection book.

The second route is the long-term payoff of the language system: understanding the language lets the player genuinely work out new magic rather than only copying recipes already revealed by loot.

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

## Known Enchantments In The Enchanting Table

Once a spell becomes known through anvil use or a successful manual cast, it is saved as a known enchantment for that player.

The enchanting-table interface can then show the ordinary readable name alongside the same SGA word used to cast it.

Conceptually:

```text
Sharpness        [SGA: weapon] [SGA: damage] [SGA: general]
Fire Protection  [SGA: armor]  [SGA: protect] [SGA: fire]
Efficiency       [SGA: tool]   [SGA: work]    [SGA: general]
```

The known entry represents knowledge of the enchantment itself, not a specific level. Level remains an energy decision at the moment of enchanting.

Once an enchantment is known, Retold should avoid forcing the human player to manually memorize and re-enter the word forever. The known-enchantment interface may provide a convenience action such as selecting/filling an already-known word. Exact UI behavior remains to be designed.

The knowledge UI records complete spells only. It should **not** maintain or reveal a dictionary of individual glyph meanings.

## Enchanting Table Independence

The enchanting table no longer needs surrounding bookshelves to function or to gain enchantment power.

The table's role is to let the player intentionally write an SGA spell onto an item using lapis and sacrificed XP/energy. What the player can conveniently reproduce comes from his learned spells, not from nearby bookshelf count.

This removes the old bookshelf-ring progression and makes enchanted books, actual use, experimentation, and language deduction the sources of magical knowledge instead.

Bookshelves remain ordinary world/library blocks and may still matter to Librarians or other systems, but they are not an enchanting-table requirement unless a future design decision explicitly adds a new separate purpose.

## Failed Experiments Should Give Useful Feedback

Experimentation should not be blind. The table should distinguish at least conceptually between:

- a meaningless/invalid three-symbol combination
- a valid magical word that does not apply to the current item
- a valid compatible word but insufficient energy/resources
- a successful enchantment

Prefer visual/audio/item reactions over explicit technical error text where practical.

The feedback should let the player form hypotheses without directly translating unknown glyphs for him.

## SGA Presentation

The final UI should use actual Standard Galactic Alphabet glyph rendering, not placeholder Unicode symbols.

The system does **not** need every SGA glyph to retain its Latin-letter meaning. Retold may use a deliberately limited set of SGA glyphs as semantic magical symbols (for example weapon, damage, undead, fire, protect, water) as long as the visual language remains recognizably SGA and internally consistent.

The final vocabulary should remain small enough for a player to start recognizing recurring glyphs naturally.

## Design Guardrails

- Do not turn enchanting into a random reroll loop if the language system can provide intentional discovery instead.
- Do not make each enchantment an arbitrary unrelated three-symbol password; reuse semantic components.
- Do not encode enchantment levels as additional language complexity; levels come from sacrificed energy.
- Do not reveal individual glyph translations in UI, tooltips, progress screens, or automatic knowledge entries.
- Do not show an unknown enchantment's ordinary readable name on item/book tooltips before that player has learned it.
- Always keep the SGA inscription visible on enchanted items/books so unknown magic can be inspected and compared.
- Unknown enchanted items still work normally.
- Finding or holding an enchanted book does not automatically teach its enchantment.
- Successfully applying a book through an anvil teaches the complete spell and records its enchanting-table recipe.
- Successfully deducing and casting a valid word at the enchanting table also teaches the spell.
- Do not require bookshelves around the enchanting table.
- Do not require external documentation/wiki knowledge for mandatory enchanting progression.
- Do not require the human player to remember every discovered sequence forever; discovery should be meaningful once, while repeated use can become convenient.
- Keep the language compact enough that pattern recognition is rewarding instead of annoying.

## Still Undecided

- exact SGA glyph-to-concept assignments
- exact list and number of domain/effect/modifier concepts
- exact XP/energy thresholds for enchantment levels
- lapis costs and whether they change with strength
- exact distribution/balance of enchanted-book loot and any additional environmental language clues
- exact enchanting-table UI and glyph input method
- exact visual styling/order of SGA and readable-name tooltip lines
- whether applying multi-enchantment books teaches all successfully transferred spells or needs any special handling
- exact compatibility/stacking rules for enchantments after the wider enchantment audit
