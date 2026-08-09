# Retold Enchanting Design

> Developer-confirmed design direction. This document records the current intended player-facing enchanting model. It does not claim implementation. Exact glyph assignments, XP thresholds, lapis quantities, UI styling, loot distribution, and balance remain future work unless explicitly stated otherwise.

## Core Role In Progression

Enchanting should become relevant around iron-tier equipment and become substantially more important by diamond tier.

Current direction:

- iron equipment can benefit meaningfully from enchanting, but enchanting is not yet mandatory
- enchanting becomes increasingly important through later material progression
- diamond equipment is intended to rely strongly on enchanting rather than simply being a universally superior raw-material tier
- XP/experience is usable magical energy and should compete with other Retold uses of energy

## Existing Lore Basis

Retold's enchanting lore remains the foundation:

- experience/XP is usable magical energy
- obsidian in the enchanting table directs/bends that energy into the item
- lapis lazuli is the physical medium used to write the enchantment onto the item
- the writing uses the Standard Galactic Alphabet (SGA)

Bookshelves are **not** required around the enchanting table. The old bookshelf-ring power/knowledge mechanic is superseded by the player-learning system described below.

## Player Learns A Magical Language

Enchanting is a discovery/learning system rather than a random-roll system.

The player enters short SGA words at the enchanting table. Individual glyph meanings are never directly translated by the game. The human player learns the language by using enchanted books/items, observing results, comparing repeated patterns, and experimenting.

Retold should never provide a glyph dictionary such as `this glyph = weapon` or `this glyph = fire`.

The language should remain small and regular enough that learning it feels rewarding rather than like memorizing arbitrary passwords.

## Three-Part Enchantment Words

Each enchantment is expressed as a three-symbol semantic word:

```text
[DOMAIN] [EFFECT] [MODIFIER]
```

The player sees three actual SGA glyphs. Internally those glyphs represent reusable concepts.

### Domain

The first glyph describes the broad item/action domain. Possible concepts include:

- weapon
- armor
- tool
- bow/projectile weapon
- boots
- helmet

### Effect

The second glyph describes the main magical action. Possible concepts include:

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

The third glyph specializes the effect. Possible concepts include:

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

Exact vocabulary and glyph assignments remain to be finalized. Reuse concepts wherever possible.

## Example Enchantment Meanings

These are semantic examples only; final SGA glyph assignments are not chosen yet.

| Enchantment | Domain | Effect | Modifier |
| --- | --- | --- | --- |
| Sharpness | weapon | damage | general |
| Smite | weapon | damage | undead |
| Bane of Arthropods | weapon | damage | arthropod |
| Fire Aspect | weapon | fire | target |
| Knockback | weapon | push | target |
| Looting | weapon | drop | more |
| Efficiency | tool | work/speed | general |
| Fortune | tool | drop | more |
| Silk Touch | tool | drop | original |
| Protection | armor | protect | general |
| Fire Protection | armor | protect | fire |
| Blast Protection | armor | protect | explosion |
| Projectile Protection | armor | protect | projectile |
| Feather Falling | boots | protect | fall |
| Respiration | helmet | breathe | water |
| Aqua Affinity | helmet | work | water |
| Depth Strider | boots | move | water |
| Frost Walker | boots | freeze | water |
| Power | bow | damage | projectile |
| Punch | bow | push | projectile |
| Flame | bow | fire | projectile |

The important rule is that enchantments are semi-compositional. Shared meanings should produce shared glyphs instead of every enchantment being an unrelated code.

## Unknown Enchantments And Tooltips

Readable enchantment names depend on that player's knowledge.

An enchanted item always carries its SGA inscription, so the glyph word is visible even when the spell is unknown. An unknown enchantment does **not** expose its ordinary readable name.

Conceptually:

```text
Iron Sword
[SGA] [SGA] [SGA] III
```

After the player learns that spell:

```text
Iron Sword
Sharpness III
[SGA] [SGA] [SGA]
```

The same rule applies to enchanted books. Items with multiple enchantments identify each enchantment independently according to the player's knowledge.

Unknown enchanted items still function normally. The existing inscription already contains working magic; understanding is only required to identify and intentionally reproduce it.

Tooltips never translate individual glyphs into semantic concepts.

## Enchanted Books As Loot And Language Evidence

Vanilla-style enchanted books remain lootable exploration rewards and retain their practical role as pre-written enchantments that can be transferred to compatible equipment through an anvil.

Before a spell is known, its enchanted book shows its SGA word and level but not the readable enchantment name.

Simply finding, holding, or inspecting the book does not teach the spell.

Successfully combining the enchanted book with a compatible item in an anvil teaches the spell. At that point:

- the readable enchantment name becomes known to that player
- the three-glyph word becomes a known enchanting-table recipe
- future items/books carrying that spell can show the readable name

The level on a found book is not part of the learned spell. Every level of one enchantment uses the same three-glyph word.

## Learning Through Repeated Patterns

Known enchantments provide labelled examples from which the human player can work backward.

For example:

```text
weapon + damage + general   -> Sharpness
weapon + damage + undead    -> Smite
weapon + damage + arthropod -> Bane of Arthropods
```

The repeated first two glyphs let the player infer that they probably represent the shared weapon/damage concepts.

Likewise:

```text
armor + protect + general    -> Protection
armor + protect + fire       -> Fire Protection
armor + protect + explosion  -> Blast Protection
armor + protect + projectile -> Projectile Protection
```

Cross-family repetition provides further evidence. A glyph shared between Fire Aspect and Fire Protection, for example, gives the player a reason to suspect it represents fire.

The game never confirms such individual-glyph deductions in a UI.

## Two Routes To Learning A Spell

A spell becomes known in either of two ways.

### 1. Anvil Learning

The player successfully applies an enchanted book to a compatible item in an anvil. The transferred spell is then recorded as known.

### 2. Language Deduction

The player manually assembles a valid three-glyph word at the enchanting table and successfully casts it, even if the complete spell has never previously been identified from a book.

This is the long-term payoff of the language system: a player who understands enough recurring glyphs can genuinely deduce new magic.

## Enchanting Table Interaction

### SGA Glyph Input

The enchanting-table interface contains a keyboard/sheet of SGA glyphs.

The player clicks glyphs to assemble the three-symbol enchantment word in order:

```text
[DOMAIN] [EFFECT] [MODIFIER]
```

The interface should make assembling and correcting the current three-glyph sequence straightforward. Exact visual layout is future UI work.

### Energy / Enchantment Strength

The player explicitly chooses the desired enchantment strength using a simple control, conceptually a small set of levels such as `1-5`.

A button row, slider, or similarly simple control is acceptable. The important behavior is:

- the chosen strength determines how much XP/energy is sacrificed
- the SGA word determines the enchantment itself
- enchantment level is never encoded by additional glyphs
- an enchantment cannot exceed its own supported maximum level even if the generic UI can represent a wider range

Exact XP costs/thresholds remain balancing work.

### Lapis Cost

Lapis is the material with which the magical symbols are physically written onto the item.

Therefore lapis cost is tied to the amount of writing rather than enchantment strength itself:

> more symbols written -> more lapis required

The current standard enchantment word contains three glyphs, so normal three-glyph spells have the same basic writing length. The exact amount of lapis consumed per glyph remains to be balanced.

If Retold later introduces magical inscriptions with a different number of glyphs, their lapis cost should naturally reflect that additional or reduced writing.

### Deterministic Resolution

Enchanting never fails randomly.

If the entered word is valid for the item and the required XP/energy and lapis are available, the enchantment succeeds every time.

There are no random rolls for whether the chosen enchantment works.

### Invalid Or Incompatible Words

If the entered sequence does not produce a valid applicable enchantment in the current context, **nothing happens**.

Do not provide separate diagnostic feedback revealing whether:

- the word is meaningless
- the word is real but incompatible with the item
- the player's hypothesis about a glyph is close

This avoids the enchanting table itself becoming an automatic language-decoding tool. The player learns by finding examples, comparing known spells, and testing hypotheses.

Resource consumption for a no-op attempt should not create a punitive guessing tax; exact implementation handling can be chosen accordingly.

### Known-Enchantment Recipe Panel

Once a spell is known, the enchanting table provides a recipe-book-like list of known enchantments.

A known entry shows the readable enchantment name together with its complete SGA word. Selecting a known enchantment should conveniently fill its glyph sequence into the input rather than requiring the human player to memorize and retype it forever.

The player then chooses the desired energy/strength and performs the enchantment normally.

The recipe panel records complete known spells only. It never exposes a dictionary of individual glyph meanings.

## Enchantment Level Comes From Energy

The three-glyph word defines **what** magic is performed. The amount of XP/energy sacrificed determines **how strongly** it is performed.

Conceptually:

```text
[weapon] [damage] [general] + lower energy  -> Sharpness I
[weapon] [damage] [general] + more energy   -> Sharpness II
[weapon] [damage] [general] + still more    -> higher Sharpness
```

The player learns one Sharpness word, not a separate word for every Sharpness level.

## Enchanting Table Independence

The enchanting table does not require surrounding bookshelves to function or to increase enchanting power.

Its role is to let the player intentionally write an SGA spell onto an item using lapis and sacrificed XP/energy.

Bookshelves remain ordinary library/world blocks and may matter to villagers or other systems, but they are not part of enchanting-table progression unless a later explicit design decision introduces a different purpose.

## SGA Presentation

The final UI should use actual Standard Galactic Alphabet glyph rendering rather than placeholder Unicode symbols.

Retold does not need every SGA glyph to retain its Latin-letter meaning. A deliberately limited subset may instead represent semantic magical concepts such as weapon, damage, undead, fire, protect, and water, as long as the system remains internally consistent and recognizably SGA.

## Design Guardrails

- Enchanting is deterministic, not a random reroll system.
- Every enchantment is a reusable semantic `domain + effect + modifier` word rather than an arbitrary password.
- Enchantment level comes from sacrificed XP/energy, not additional glyphs.
- Lapis represents the physical writing and therefore scales with the amount of glyph writing.
- The enchanting table uses a clickable SGA keyboard/sheet for manual word assembly.
- Known spells appear in a recipe-book-like interface and can conveniently refill their glyph sequence.
- Invalid/incompatible attempts do not reveal diagnostic information that would automatically decode the language.
- Never reveal individual glyph translations in UI, tooltips, progress screens, or knowledge entries.
- Unknown enchantments show their SGA inscription and level but not their readable name.
- Unknown enchanted items still function normally.
- Finding or holding an enchanted book does not automatically teach its enchantment.
- Successfully applying an enchanted book through an anvil teaches the complete spell.
- Successfully deducing and casting a valid spell manually also teaches it.
- Do not require bookshelves around the enchanting table.
- Do not require external wiki knowledge for mandatory progression.
- Discovery should matter once; known-spell convenience prevents repetitive memorization from becoming busywork.

## Still Undecided

- exact SGA glyph-to-concept assignments
- exact list and number of domain/effect/modifier concepts
- exact XP/energy costs for each enchantment strength/level
- whether the strength control is buttons, a slider, or another compact control
- exact lapis consumed per written glyph
- exact distribution/balance of enchanted-book loot and any additional environmental language clues
- exact enchanting-table UI layout and visual styling
- exact visual styling/order of SGA and readable-name tooltip lines
- whether applying multi-enchantment books teaches all successfully transferred spells or needs special handling
- exact compatibility/stacking rules after the wider enchantment audit
