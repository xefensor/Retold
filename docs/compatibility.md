# Retold Mod And Datapack Compatibility

Retold is designed to run without hard dependencies on other content mods. Datapacks and
compatibility addons can extend selected Retold-owned gameplay concepts through tags while the
default Retold behavior remains unchanged.

Compatibility means that projects can run together safely. It does not promise that another mod's
items, machines, dimensions, or progression will preserve Retold's intended balance.

## Compatibility-First Development

New Retold systems must consider mod and datapack compatibility as part of their initial design,
not as a retrofit after their implementation is complete. During design and review, identify which
concepts should use standard Minecraft or NeoForge tags, which Retold-owned semantics need their own
data or tags, and which integration points require a small stable hook. Unknown third-party content
must fail safely and must not be taken over merely because it resembles a vanilla implementation.

Existing systems do not need broad compatibility rewrites in isolation. When an existing subsystem
is materially changed, audit the touched item, block, entity, recipe, world-mutation, and API
assumptions and improve their extension points in the same focused change when that can be done
safely. If a safe compatibility design depends on an unresolved gameplay decision or cannot be
properly tested, document the limitation and defer it instead of weakening the current behavior.

Compatibility work must preserve Retold's built-in defaults and standalone behavior. New extension
points need focused regression coverage for those defaults, and should remain data-driven or
optional so installing no compatibility addon produces the same gameplay as before.

## Extension Tags

The following block and item tags are supported extension points:

| Tag | Meaning |
| --- | --- |
| `retold:armadillo_grub_soils` | Exposed ground from which a hungry Armadillo may obtain grubs without removing the block. |
| `retold:armadillo_scrub_range_blocks` | Exposed terrain that can anchor an Armadillo scrub range. |
| `retold:panda_bamboo_blocks` | Blocks a hungry Panda may seek and consume. Successful feeding removes the block, subject to `mobGriefing`. |
| `retold:turtle_beach_blocks` | Exposed beach terrain a Turtle may use when it is near water. |
| `retold:desert_browse_blocks` | Renewable, non-destructive habitat forage for hungry Camels and Rabbits. |
| `retold:goat_scrape_blocks` | Renewable, non-destructive habitat forage for hungry Goats. |
| `retold:mooshroom_grazing_blocks` | Renewable, non-destructive habitat forage for hungry Mooshrooms. |
| `retold:forage_crops` | Crop blocks grazers and small passive animals may consume. Tagged blocks are destroyed, subject to `mobGriefing`. |
| `retold:forage_flowers` | Flower blocks Bees may visit and herbivores may consume. Animal consumption destroys the block, subject to `mobGriefing`; Bee visits do not. |
| `retold:grazer_forage_plants` | Other plant blocks grazers may consume, including the default grasses and ferns. |
| `retold:small_passive_forage_plants` | Other plant blocks small passive animals may consume. |
| `retold:turtle_forage_blocks` | Blocks hungry Turtles may consume. |
| `retold:hoglin_forage_blocks` | Blocks hungry Hoglins may consume. |
| `retold:piglin_forage_blocks` | Blocks hungry Piglins may consume. |
| `retold:strider_forage_blocks` | Blocks hungry Striders may consume. |
| `retold:spider_lair_web_blocks` | Blocks counted toward a Spider lair's 50-web cap. Spiders still place vanilla Cobwebs. |
| `retold:illager_village_signal_blocks` | Blocks that make nearby Illagers recognize an area as village-like while roaming. |
| `retold:nether_remnant_guard_anchor_blocks` | Blocks Wither Skeletons and Blazes may use to recover their Nether-remnant guard post. |
| `retold:ocean_monument_guard_anchor_blocks` | Blocks Guardians may use to recover their monument guard post. |
| `retold:ocean_monument_protected_blocks` | Blocks whose mining can trigger Guardian pressure when they are part of a valid Ocean Monument. |
| `retold:weak_mob_barriers` | Blocks desperate eligible mobs may break through under Retold's normal ownership, timing, and `mobGriefing` rules. |
| `retold:campfire_consumable_igniters` | Items that light an unlit Campfire and are consumed one at a time in Survival. This is separate from durability-based igniters such as Flint and Steel. |
| `retold:leaf_preserving_tools` | Tools that suppress Retold's supplemental Stick drops from leaves and woody bushes. Ordinary block loot remains unchanged. |
| `retold:meat_foods` | Meat accepted by Retold predators, hungry Nether mobs, hungry undead, and Animal Feeders. It includes `minecraft:meat`. |
| `retold:fish_foods` | Fish accepted by Retold predators, Nautiluses, Guardians, and Animal Feeders. It includes `minecraft:fishes`. |
| `retold:berry_foods` | Berries accepted by Foxes and Animal Feeders. It includes `c:foods/berry`. |
| `retold:grazer_foods` | Dropped or feeder food accepted by Retold grazers. |
| `retold:small_passive_foods` | Dropped or feeder food accepted by Retold small passive animals. |
| `retold:flower_foods` | Dropped or feeder flower items accepted by Bees and eligible herbivores. |
| `retold:nether_fungus_foods` | Fungi accepted by hungry Nether mobs. It includes `c:mushrooms` and the vanilla Hoglin and Strider food tags. |
| `retold:bat_foods` | Dropped food accepted by hungry Bats. |
| `retold:feline_scavenge_foods` | Extra scavenged food accepted by Cats and Ocelots in addition to meat and fish. |
| `retold:torch_igniters` | Items players may use to relight Retold's extinguished torches. |

Only add content whose behavior matches the tag's complete meaning. For example, a block added to
`retold:panda_bamboo_blocks` must be safe to remove when consumed; a decorative prismarine-like
block should not enter `retold:ocean_monument_protected_blocks` unless it should participate in
monument mining pressure. An item added to `retold:campfire_consumable_igniters` must be safe to
consume one at a time; durability-based tools belong in their own interaction path instead.

The three renewable habitat-forage tags do not remove their blocks. They remain usable when
`mobGriefing=false`, use Retold's normal bounded forage search, and observe the existing 600-tick
per-mob repeat-use cooldown. Add only blocks that are safe to treat as an indefinitely renewable
food source.

Ordinary forage tags are destructive: successful consumption removes the tagged block and requires
`mobGriefing`. Compatibility packs must not add valuable, container-like, protected, or
state-bearing blocks unless that destruction is intended. Newly tagged foods receive the existing
generic relief for their diet category; established high-value Wheat/Hay and crop relief remains
unchanged.

Retold also respects the standard `minecraft:armadillo_food`, `minecraft:turtle_food`, and
`minecraft:panda_food` item tags in its dropped-food path. `retold:leaf_preserving_tools` includes
NeoForge's `c:tools/shear`, so ordinary modded shears work without an additional Retold-specific
entry.

## Adding A Block Or Item

Add a tag file under the compatibility datapack's namespace. This example allows Turtles to treat
`examplemod:black_sand` as beach terrain:

```json
{
  "replace": false,
  "values": [
    "examplemod:black_sand"
  ]
}
```

Save it as:

```text
data/retold/tags/block/turtle_beach_blocks.json
```

Use the corresponding `data/retold/tags/item/<tag-name>.json` path for an item tag. Keep `replace`
false so Retold's defaults and entries from other compatibility packs remain available.

## Current Boundaries

These tags extend existing Retold behavior; they do not add Retold mob profiles or faction
membership to third-party entities. Modded-mob faction membership, recipe-viewer integration,
world-protection adapters, and the stable public Java API remain planned work.

The exact item/block audit intentionally retains checks that define a fixed progression currency
or ritual item, a specific tool tier, a lit/extinguished counterpart mapping, a portal or structure
invariant, or a block with unique replacement/loot behavior. Sniffer diggable terrain also remains
exact for now: a tag-backed attempt preserved registry membership but made the existing navigation
survival test unreliable, so Retold will not expose that extension point until its behavior can be
verified without weakening the regression test.

Report reproducible compatibility failures through Retold's normal support channels. Include both
mod versions, the relevant datapacks, whether the failure also occurs with only Retold installed,
and dedicated-server details when applicable.
