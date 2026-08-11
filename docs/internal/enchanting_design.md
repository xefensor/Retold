# Retold Enchanting Design

> Developer-confirmed design direction. This document records the current intended player-facing enchanting model. The complete 43-enchantment semantic catalog, fixed 26-concept SGA vocabulary and client synchronization, per-player knowledge persistence and synchronization, successful-anvil-application learning route, knowledge-aware item/book tooltips, server-authoritative deterministic casting transaction, and first player-facing enchanting-table interface are implemented. Final visual styling, loot distribution, balance beyond the confirmed casting costs, and the wider enchantment audit remain future work unless explicitly stated otherwise.

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

The grammar uses six broad domain concepts (`armor`, `bow`, `fishing`, `item`, `tool`, and
`weapon`), nine effect concepts (`bind`, `damage`, `fire`, `move`, `protect`, `push`, `restore`,
`work`, and `yield`), and reusable modifier concepts. A concept may appear in more than one grammar
position when its meaning genuinely applies there, such as `armor`, `fire`, or `item`.

## Fixed SGA Vocabulary

Retold assigns exactly 26 semantic concepts to Minecraft's built-in `minecraft:alt` SGA glyphs.
The Latin glyph codes below are implementation/debug notation only; normal player-facing rendering
uses their SGA shapes and never exposes the concept translations.

| Glyph code | Semantic concept | Typical grammar use |
| --- | --- | --- |
| A | armor | domain or modifier |
| B | arthropod | modifier |
| C | bind | effect |
| D | bow | domain |
| E | damage | effect |
| F | explosion | modifier |
| G | fall | modifier |
| H | fire | effect or modifier |
| I | fishing | domain |
| J | general | modifier |
| K | ice | modifier |
| L | item | domain or modifier |
| M | more | modifier |
| N | move | effect |
| O | multiple | modifier |
| P | projectile | modifier |
| Q | protect | effect |
| R | push | effect |
| S | restore | effect |
| T | self | modifier |
| U | tool | domain |
| V | undead | modifier |
| W | water | modifier |
| X | weapon | domain |
| Y | work | effect |
| Z | yield | effect |

## Complete Enchantment Words

For a spoiler-heavy test matrix with maximum levels, representative items, table availability,
costs, and failure cases, use [`enchanting_test_guide.md`](enchanting_test_guide.md).

Crossbows use the `bow` domain; tridents, maces, and swords use `weapon`; all armor slots use
`armor`. This keeps the vocabulary small enough for the 26-glyph alphabet while preserving shared
patterns that players can deduce.

| Enchantment | Domain | Effect | Modifier | Glyph word |
| --- | --- | --- | --- | --- |
| Protection | armor | protect | general | AQJ |
| Fire Protection | armor | protect | fire | AQH |
| Feather Falling | armor | protect | fall | AQG |
| Blast Protection | armor | protect | explosion | AQF |
| Projectile Protection | armor | protect | projectile | AQP |
| Respiration | armor | protect | water | AQW |
| Aqua Affinity | armor | work | water | AYW |
| Thorns | armor | damage | self | AET |
| Depth Strider | armor | move | water | ANW |
| Frost Walker | armor | move | ice | ANK |
| Curse of Binding | item | bind | armor | LCA |
| Soul Speed | armor | move | undead | ANV |
| Swift Sneak | armor | move | self | ANT |
| Sharpness | weapon | damage | general | XEJ |
| Smite | weapon | damage | undead | XEV |
| Bane of Arthropods | weapon | damage | arthropod | XEB |
| Knockback | weapon | push | item | XRL |
| Fire Aspect | weapon | fire | item | XHL |
| Looting | weapon | yield | more | XZM |
| Sweeping Edge | weapon | damage | multiple | XEO |
| Efficiency | tool | work | general | UYJ |
| Silk Touch | tool | yield | self | UZT |
| Unbreaking | item | protect | general | LQJ |
| Fortune | tool | yield | more | UZM |
| Power | bow | damage | projectile | DEP |
| Punch | bow | push | projectile | DRP |
| Flame | bow | fire | projectile | DHP |
| Infinity | bow | yield | more | DZM |
| Luck of the Sea | fishing | yield | general | IZJ |
| Lure | fishing | move | item | INL |
| Loyalty | weapon | move | self | XNT |
| Impaling | weapon | damage | water | XEW |
| Riptide | weapon | move | water | XNW |
| Channeling | weapon | fire | projectile | XHP |
| Multishot | bow | damage | multiple | DEO |
| Quick Charge | bow | work | general | DYJ |
| Piercing | bow | damage | item | DEL |
| Density | weapon | damage | fall | XEG |
| Breach | weapon | damage | armor | XEA |
| Wind Burst | weapon | push | fall | XRG |
| Lunge | weapon | move | item | XNL |
| Mending | item | restore | general | LSJ |
| Curse of Vanishing | item | bind | undead | LCV |

Mending remains mapped because the enchantment stays registered for existing items, commands, and
Creative testing. Retold removes it only from new random-loot and Librarian-trade selection, so an
existing Mending item remains renderable and functional.

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

The current implementation uses a deliberately minimal presentation:

- an unknown mapped enchantment replaces its vanilla name line with one dark-purple SGA word and
  its ordinary Roman-numeral level
- a known mapped enchantment retains the ordinary vanilla name-and-level line and adds its SGA word
  immediately below
- each enchantment on a multi-enchanted item or book is handled independently
- unmapped third-party enchantments retain their original tooltip line instead of becoming blank

The glyphs use Minecraft's built-in `minecraft:alt` font. Exact colors and final line layout may be
refined with the enchanting-table UI, but the known/unknown information boundary is implemented.

## Enchanted Books As Loot And Language Evidence

Vanilla-style enchanted books remain lootable exploration rewards and retain their practical role as pre-written enchantments that can be transferred to compatible equipment through an anvil.

Before a spell is known, its enchanted book shows its SGA word and level but not the readable enchantment name.

Simply finding, holding, or inspecting the book does not teach the spell.

Successfully combining the enchanted book with a compatible item in an anvil teaches the spell. At that point:

- the readable enchantment name becomes known to that player
- the three-glyph word becomes a known enchanting-table recipe
- future items/books carrying that spell can show the readable name

When one book carries multiple enchantments, the completed anvil operation teaches every spell
that actually transferred to or improved the output item. Incompatible or unchanged enchantments
that did not transfer remain unknown.

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

The implemented compact interface places the three inscription slots and level buttons above the
ordinary item/lapis inventory slots. A 26-glyph SGA keyboard fills the adjacent panel. Glyphs append
from left to right; clicking an inscription slot removes that glyph and every glyph after it, while
a Clear button resets the whole word. Physical `A-Z` keys enter the corresponding glyphs,
`Backspace` removes the last glyph, number-row or keypad `1-5` chooses strength, and `Enter` writes.
Control- and Alt-modified shortcuts remain available to the underlying screen. A pending request
temporarily disables Write so key repeat or rapid clicking cannot submit the same cast twice.

### Energy / Enchantment Strength

The player explicitly chooses the desired enchantment strength using a simple control, conceptually a small set of levels such as `1-5`.

The implemented control is a five-button `I-V` row. The important behavior is:

- the chosen strength determines how much XP/energy is sacrificed
- the SGA word determines the enchantment itself
- enchantment level is never encoded by additional glyphs
- an enchantment cannot exceed its own supported maximum level even if the generic UI can represent a wider range

When the current word is already known, the screen shows that enchantment's registered maximum and
disables levels above it. A manually entered unknown word retains the generic `I-V` controls so the
client does not reveal whether the attempted word resolves or why it might fail.

Each cast sacrifices exactly five experience levels per requested enchantment level:

| Enchantment level | Experience-level cost |
| --- | --- |
| I | 5 |
| II | 10 |
| III | 15 |
| IV | 20 |
| V | 25 |

An enchantment cannot be requested above its own registered maximum level.

### Lapis Cost

Lapis is the material with which the magical symbols are physically written onto the item.

Therefore lapis cost is tied to the amount of writing rather than enchantment strength itself:

> more symbols written -> more lapis required

The current standard enchantment word contains three glyphs. Each glyph consumes one lapis lazuli,
so a valid standard cast consumes exactly three lapis regardless of enchantment level.

If Retold later introduces magical inscriptions with a different number of glyphs, their lapis cost should naturally reflect that additional or reduced writing.

### Deterministic Resolution

Enchanting never fails randomly.

If the entered word is valid for the item and the required XP/energy and lapis are available, the enchantment succeeds every time.

There are no random rolls for whether the chosen enchantment works.

The current server transaction preserves vanilla enchanting-table eligibility, item applicability,
and enchantment compatibility. Enchantments excluded from the vanilla enchanting table, such as
Mending, cannot be cast there even though they retain language words for found-item and book
identification. A plain book retains vanilla table behavior and becomes an enchanted book after a
successful cast. Creative players with infinite materials are not charged.

### Invalid Or Incompatible Words

If the entered sequence does not produce a valid applicable enchantment in the current context, **nothing happens**.

Do not provide separate diagnostic feedback revealing whether:

- the word is meaningless
- the word is real but incompatible with the item
- the player's hypothesis about a glyph is close

This avoids the enchanting table itself becoming an automatic language-decoding tool. The player learns by finding examples, comparing known spells, and testing hypotheses.

Invalid, unavailable, incompatible, conflicting, non-upgrading, or unaffordable attempts consume
neither lapis nor experience and do not alter the input item. The transaction prepares and verifies
the complete output before committing any cost. Internal failure states exist for server validation,
but the player-facing interface collapses them into the same generic rejection. A successful
server-authoritative cast clears the inscription and briefly highlights the changed target slot in
green. Every rejected cast preserves the inscription and gives the same quiet low note and brief red
slot highlight, without identifying the failure category.

### Known-Enchantment Recipe Panel

Once a spell is known, the enchanting table provides a paginated recipe-book-like list of readable
enchantment names and maximum levels. With no target inserted it contains every known spell eligible
for the enchanting table. With a target inserted it shows only known spells that can currently be
written onto that item, excluding non-table spells, incompatible/conflicting spells, and spells
already present at their maximum. Selecting one refills all three glyph slots while retaining or
clamping the currently chosen level. The list never includes unknown spells and never exposes
individual concept translations.

A known entry shows the readable enchantment name and maximum level. Selecting it conveniently fills
the complete SGA sequence into the inscription rather than requiring the human player to memorize and
retype it forever.

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
- Each requested enchantment level costs five experience levels.
- Lapis represents the physical writing; one lapis per glyph makes a standard cast cost three.
- The enchanting table uses a clickable SGA keyboard/sheet for manual word assembly.
- Known spells appear in a recipe-book-like interface and can conveniently refill their glyph sequence.
- Invalid/incompatible attempts do not reveal diagnostic information that would automatically decode the language.
- Never reveal individual glyph translations in UI, tooltips, progress screens, or knowledge entries.
- Unknown enchantments show their SGA inscription and level but not their readable name.
- Unknown enchanted items still function normally.
- Finding or holding an enchanted book does not automatically teach its enchantment.
- Successfully applying an enchanted book through an anvil teaches the complete spell.
- A multi-enchantment book teaches every spell actually transferred to or improved on the output;
  incompatible or unchanged spells remain unknown.
- Successfully deducing and casting a valid spell manually also teaches it.
- Do not require bookshelves around the enchanting table.
- Do not require external wiki knowledge for mandatory progression.
- Discovery should matter once; known-spell convenience prevents repetitive memorization from becoming busywork.

## Still Undecided

- exact distribution/balance of enchanted-book loot and any additional environmental language clues
- final enchanting-table visual styling and responsive layout refinement
- exact visual styling/order of SGA and readable-name tooltip lines
- exact compatibility/stacking rules after the wider enchantment audit
