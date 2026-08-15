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

## Block And Item Extension Tags

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
| `retold:aquatic_school_forage_blocks` | Plants hungry school fish may consume. Defaults are seagrass, tall seagrass, kelp, and kelp plants. |
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
| `retold:squid_foods` | Dropped items accepted by hungry Squid and Glow Squid. Defaults contain raw fish only. |
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

## Faction Entity Tags

Compatibility datapacks can classify an entity type into one Retold faction by appending it to one
of these tags:

| Tag | Retold identity and inherited relationship rules |
| --- | --- |
| `retold:factions/nether_remnants` | Nether Remnant guards and territory members. |
| `retold:factions/illagers` | Full Illager members, including territory and raid relationships. |
| `retold:factions/undead` | Undead hostility and tolerance. Retold's default composes `minecraft:undead` and adds Ghasts. |
| `retold:factions/slimes` | Slime and Magma Cube diplomacy. |
| `retold:factions/aquatic_hostiles` | Guardian-family hostility. |
| `retold:factions/creepers` | Creeper identity used by global target-safety rules. |
| `retold:factions/arthropods` | Spider-family diplomacy. This intentionally does not include every entity in Minecraft's broader arthropod tag. |
| `retold:factions/silverfish` | Silverfish diplomacy and same-species swarm boundary. |
| `retold:factions/endermites` | Endermite diplomacy and same-species swarm boundary. |
| `retold:factions/nether_beasts` | Hoglin-style faction relationships. |
| `retold:factions/breezes` | Breeze faction relationships. |
| `retold:factions/wardens` | Warden faction relationships. |
| `retold:factions/bosses` | Boss faction relationships. |
| `retold:factions/creakings` | Creaking faction relationships. |
| `retold:factions/village_defenders` | Village Defender targeting and retaliation. |
| `retold:factions/enders` | Ender faction relationships. |
| `retold:alliances/illager_loose_allies` | Permanent non-hostile Illager alignment with combat cooperation only while both entities share the same active raid. This does not grant Illager territory membership. |

Faction describes diplomacy, target selection, assistance, retaliation, and configured territory
membership. It does not assign a Retold mob profile or take over the entity's daily-life AI. A
compatibility pack may therefore classify a third-party mob without opting it into hunger,
foraging, homes, or another profile. Unknown and untagged entity types remain unfactioned.

Add an entity type exactly as for the block example, using the entity-type path. For example, this
classifies `examplemod:ashen_guard` as an Undead entity:

```json
{
  "replace": false,
  "values": [
    "examplemod:ashen_guard"
  ]
}
```

Save it as `data/retold/tags/entity_type/factions/undead.json`. Use `replace: false` so built-in and
other compatibility-pack members remain available.

An entity type must belong to at most one full `retold:factions/*` tag. If it appears in multiple
full faction tags, Retold logs an error and treats it as unfactioned so datapack order cannot choose
its diplomacy silently. If it is both a full member and an Illager loose ally, Retold logs a warning
and full membership takes precedence. Classification caches and installed faction combat goals are
updated after server tag reloads, including for already-loaded mobs.

Players and a tamed Wolf that is actively defending are dynamic identities and are not represented
by entity-type tags. Undead mounts covered by the standard Undead tag stop inheriting generic
hostile Undead behavior only after they have a persisted owner reference. Retold deliberately does
not trust the vanilla tame flag here because Camel always reports itself as tamed and Skeleton-trap
horses can be marked tamed before a player owns them. Tags classify
identity only: entity classes that cannot target, retaliate, assist, or use territory behavior do
not gain those capabilities merely from membership.

## Recipe Discovery Integrations

Retold exposes one per-player recipe-visibility authority through
`cz.xefensor.retold.api.recipe.RetoldRecipeKnowledge`. Vanilla's recipe book and optional recipe
viewers must ask this authority instead of inferring discovery from their own recipe lists.
Crafting, smelting, blasting, smoking, campfire cooking, stonecutting, and smithing recipes are
managed by default and remain hidden until that player knows them. Knowledge is synchronized to
the client on login, respawn, dimension change, and whenever a recipe is learned.

Unknown third-party recipe types deliberately fail open: installing a machine mod does not make
all of its recipes disappear merely because Retold does not understand how the machine teaches
them. An integration that owns a reliable discovery route may register its `RecipeType` through
`RetoldRecipeKnowledge.registerDiscoveryManagedType`, retain the returned registration for as long
as the integration is active, and teach recipes through `teachAndUnlock`. The same registration
must be present on the logical server and client. Closing it restores fail-open behavior.

EMI and JEI adapters are not bundled yet. Their eventual adapters must filter through
`RetoldRecipeKnowledge.isVisibleTo` and refresh when the synchronized knowledge snapshot changes;
they must not create a separate discovery store or expose a managed recipe before vanilla would.
Both viewers must remain optional dependencies.

## World Protection Integrations

`cz.xefensor.retold.api.world.RetoldWorldProtection` is Retold's generic permission layer for
claim and protection addons. An addon registers one rule under its own unique identifier. Every
registered rule must allow a mutation; any denial prevents it. With no registered rules, all
checks allow the action and Retold behaves exactly as it did before this API existed. Closing a
registration removes that rule. A rule that throws unexpectedly is logged and fails closed for
that mutation so protected terrain is not damaged because an integration failed.

Rules receive the server level, an exact representative block position, inclusive affected block
bounds, mutation category, optional responsible entity, and optional subject identifier. Single
block actions use one-block bounds; portals and whole-chunk operations expose their full possible
area so an adapter can reject overlaps rather than checking only the center. Current categories
distinguish mob breaking and placement, other entity breaking, Aender portal creation,
delayed-structure retrogen, Aender chunk regeneration, and generic Retold world changes. Retold
currently routes the following owned operations through this layer:

- destructive animal forage, Panda bamboo consumption, weak-barrier breaking, Spider lair webs,
  and Villager torch maintenance
- Gale Core terrain damage
- Aender stale-chunk blanking and regeneration
- generated Aender counterpart portals and player-built portal activation
- delayed structure retrogen

The normal NeoForge entity-griefing hook and `mobGriefing` rule still apply before Retold's
position-aware mob checks. Protection rules supplement those standard checks; they do not replace
them. Concrete claim-mod adapters and their dedicated-server/multiplayer test matrix remain future
work.

## Public Java API Boundary

Only types below `cz.xefensor.retold.api` are public integration contracts. The initial supported
surface contains recipe knowledge/visibility and world-mutation protection. Other Retold packages
remain implementation details and may change during ordinary development; addons must not edit
Retold saved data or call internal managers directly.

Public API additions should remain source-compatible within a Retold minor release line. A
breaking public-API change requires an explicitly documented version transition and changelog
entry. Query methods do not expose mutable storage, registrations are removable handles, and
optional integrations must isolate references to absent third-party classes so Retold can always
start without them.

## Current Boundaries

Faction tags do not add Retold mob profiles to third-party entities. Documentation and explicit
opt-out support for modded-mob profiles, concrete EMI/JEI adapters, and claim-mod-specific
world-protection adapters remain planned work. Stage, faction, mob-profile, and Aender-specific
Java API surfaces are still under evaluation and are not public contracts yet.

The exact item/block audit intentionally retains checks that define a fixed progression currency
or ritual item, a specific tool tier, a lit/extinguished counterpart mapping, a portal or structure
invariant, or a block with unique replacement/loot behavior. Sniffer diggable terrain also remains
exact for now: a tag-backed attempt preserved registry membership but made the existing navigation
survival test unreliable, so Retold will not expose that extension point until its behavior can be
verified without weakening the regression test.

Report reproducible compatibility failures through Retold's normal support channels. Include both
mod versions, the relevant datapacks, whether the failure also occurs with only Retold installed,
and dedicated-server details when applicable.
