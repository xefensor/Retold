# Retold Asset Credits And Status

This record complements [`LICENSE`](LICENSE) and [`LICENSE-ASSETS.md`](LICENSE-ASSETS.md).

## Credited Contributors

| Contributor | Work | Status |
| --- | --- | --- |
| Jesse Schramm | Extinguished-torch textures | Included |
| Xefensor | Gameplay screenshots dated 2026-07-18 | Repository documentation |
| Xefensor | Runtime-generated horizon-figure skin encoded from a developer-supplied and approved visual source | Included; 2026-08-19 |
| Xefensor, with drawing instruction and review from Codex | Hand-drawn Aender Sand texture created in Krita | Included 2026-08-14; superseded by generated Sand on 2026-10-12 at developer request |
| OpenAI image generation, directed and processed by Codex | July Aender wood-family, Chronolith, spawn-egg, desert, and Aenderite placeholders | Introduced 2026-07-22; historical sources for the current development set |
| OpenAI image generation, directed and processed by Codex under Xefensor direction | Aender textures, Flint/Steel equipment, Nether Reactor Core, Earth Guardian/Gale Core textures and eggs | Development artwork; final in-game approval pending |
| Codex (OpenAI), under Xefensor direction | Earth Guardian geometry/poses and Gale Core commander armor geometry/rendering | Development models; final in-game approval pending |
| Codex (OpenAI) | Original-art-only offline asset review image dated 2026-10-23 | Repository documentation; model/texture preview, not an in-game screenshot |

## Current Development Artwork

The generated textures and original boss models remain **development artwork**. Approval to create
these assets does not establish final visual approval. The developer owns review of the installed
appearance, including world tiling, cutouts, equipment scale, armor trims, animation, shaders,
resource packs, and multiplayer. Offline previews are not in-game screenshots.

The Aender direction retains static material surfaces, vanilla-scale pixel detail, the original
inverted-world color families, and irregular grass fringes. Purple vegetation, blue soil/bark and
desert materials, and cool stone follow the [original palette](docs/media/aender-original-palette.png).
Original plank artwork is retained. Grass and ore use their exact Soil and Stone host pixels.
The portal effect is separate from these static block materials. The provisional portal frame
retains `retold:dev_aender_portal_frame`; final naming remains open.

Equipment uses cool silver Steel and knapped Flint. Earth Guardian is a blind weathered stone
statue. Gale Core uses the installed Breeze head, glowing face, wind and animations with an original
shallow pale circlet and four thin floating plates around a visible installed Heavy Core. These
plates are presentation only. Nether Reactor Core uses original blue-and-brass artwork. Aender Eye
uses the installed vanilla Eye of Ender appearance, as directed by the developer; no dedicated
Retold eye sprite is packaged.

The active 2026-10-23 revision improves nine existing textures using seven generated sources:
Steel's three atlases, Earth Guardian, Gale plate faces, Flint multi-tool, Nether Reactor Core,
Aender grass top and leaves. A further polish replaces Steel and Guardian material sources with
limited-palette panel sheets. Steel has distinct front/back plates and separate bracer, greave and
boot materials; its existing coverage masks are byte-identical. Guardian has quieter carved stone
planes, separate face/back/side materials and sparse olive moss. Existing UVs, geometry and gameplay
are unchanged. Gale's earlier rim material is retained during packing. The refreshed
[offline review image](docs/media/asset-review-2026-10-23.png) uses the actual Retold meshes and
original textures without a running game or extracted vanilla pixels. It does not verify
the installed Breeze wind, Heavy Core rendering, lighting or other connected-world appearance.

The following paths are relative to `src/main/resources/assets/retold/textures/`. The inventory
contains 35 Aender and 23 equipment/boss generated PNGs; metadata and Java geometry are excluded.

| Family | Active generated texture paths |
| --- | --- |
| Aender terrain | `block/aender_grass_block_top.png`, `block/aender_grass_block_side.png`, `block/aender_soil.png`, `block/aender_stone.png`, `block/aender_log.png`, `block/aender_log_top.png`, `block/aender_leaves.png` |
| Aender wood family | `block/aender_door_bottom.png`, `block/aender_door_top.png`, `block/aender_hanging_sign.png`, `block/aender_planks.png`, `block/aender_sapling.png`, `block/aender_sign.png`, `block/aender_trapdoor.png`, `block/stripped_aender_log.png`, `block/stripped_aender_log_top.png`, `entity/boat/aender.png`, `entity/chest_boat/aender.png`, `item/aender_boat.png`, `item/aender_chest_boat.png`, `item/aender_door.png`, `item/aender_sign.png`, `item/aender_hanging_sign.png` |
| Aender Chronolith and egg | `block/aender_chronolith.png`, `item/aender_eye_spawn_egg.png` |
| Aender Desert | `block/aender_cactus_side.png`, `block/aender_cactus_top.png`, `block/aender_sandstone.png`, `block/aender_sandstone_top.png`, `block/aender_sand.png` |
| Aenderite | `block/aenderite_ore.png`, `item/raw_aenderite.png`, `item/aenderite_ingot.png` |
| Aender infrastructure | `block/aender_stabilizer.png`, `block/dev_aender_portal_frame.png` |
| Steel inventory | `item/steel_ingot.png`, `item/steel_sword.png`, `item/steel_pickaxe.png`, `item/steel_axe.png`, `item/steel_shovel.png`, `item/steel_hoe.png`, `item/steel_spear.png`, `item/steel_helmet.png`, `item/steel_chestplate.png`, `item/steel_leggings.png`, `item/steel_boots.png` |
| Steel worn armor | `entity/equipment/humanoid/steel.png`, `entity/equipment/humanoid_leggings/steel.png`, `entity/equipment/humanoid_baby/steel.png` |
| Flint tools | `item/flint_multi_tool.png`, `item/flint_spear.png` |
| Held spears | `item/steel_spear_in_hand.png`, `item/flint_spear_in_hand.png` |
| Fire artifact | `item/nether_reactor_core.png` |
| Earth Guardian | `entity/earth_guardian.png`, `item/earth_guardian_spawn_egg.png` |
| Gale Core | `entity/gale_core.png`, `item/gale_core_spawn_egg.png` |

## Sources And Processing

OpenAI's built-in image-generation tool produced the original source artwork. Mechanical processing
reduces it with nearest-neighbor sampling, preserves binary cutout alpha, and packs material into
the block/entity UV layouts. Inventory sprites use 16x16 pixels and held spears use 32x32 pixels.
Steel has separate adult, leggings, and 26.2 baby armor atlases. The generated artwork uses the
[Retold Asset License](LICENSE-ASSETS.md); development status grants no additional reuse rights.

| Record | Contents |
| --- | --- |
| [`retold_asset_revision_2026-10-23.json`](docs/internal/retold_asset_revision_2026-10-23.json) | Current revision's exact prompts, seven active local sources, nine active output paths, per-source packing helpers, output sizes/hashes, and superseded Steel/Guardian/Eye provenance |
| [`aender_asset_prompts_2026-10-12.json`](docs/internal/aender_asset_prompts_2026-10-12.json) | Aender prompts, selected sources, palette direction and processing |
| [`retold_asset_prompts_2026-10-12.json`](docs/internal/retold_asset_prompts_2026-10-12.json) | Equipment, artifact and initial boss prompts/source locations |
| [`gale_core_breeze_prompts_2026-10-12.json`](docs/internal/gale_core_breeze_prompts_2026-10-12.json) | Current Breeze-style Gale armor and matching egg sources |
| [`gale_core_commander_prompts_2026-10-12.json`](docs/internal/gale_core_commander_prompts_2026-10-12.json) | Superseded brass-crown/commander experiment, retained as provenance |

Working sources, processing scripts and offline previews are optional local files under the ignored
`output/imagegen/` directory, including `aender-refresh-v2/`, `retold-equipment-and-bosses/`,
`gale-core-breeze/`, `revision-2026-10-23/`, and `polish-2026-10-23/`. They are absent from a clean
checkout and are not required by the build or tests. Earlier active manifests link the current revision through
`active_revision_reference`; its output mapping identifies replacement sources. Shipped artwork
lives under the texture paths listed above.

The July 2026 Aender batches were generated for Retold without Minecraft texture pixels. Developer
screenshots and an End Portal Frame image supplied palette/layout references only. The original
generated Sand was replaced by Xefensor's accepted Krita drawing on 2026-08-14; the developer then
requested generated denim-blue Sand on 2026-10-12. Historical versions retain their authorship.
The rejected Gale brass crown, golem-like face and broken wind bands are superseded by the current
Breeze presentation; they do not describe the installed model. The generated Aender Eye entity
sprites from 2026-10-12 and 2026-10-23 are also historical, superseded at developer request by the
installed vanilla Eye of Ender. Their provenance records remain intact; the custom spawn egg is
a separate asset.

## Third-Party Material

Minecraft, NeoForge, and other third-party names, APIs, templates, or materials remain subject to
their own terms. NeoForge MDK template files identified by `TEMPLATE_LICENSE.txt` retain that license.

The Wildfire entity packages Mojang Studios' 64x64 Minecraft Dungeons Wildfire texture as
`textures/entity/wildfire.png`, sourced from the Minecraft Wiki file
[`T HoveringInferno.png`](https://minecraft.wiki/w/File:T_HoveringInferno.png). It remains copyright
Mojang Studios/Microsoft, is excluded from the Retold Asset License, and is included for this
Minecraft mod subject to the [Minecraft Usage Guidelines](https://www.minecraft.net/usage-guidelines).
Its dedicated Java model maps the texture without modifying the pixels. The Dungeons shadow
texture is not packaged. Wildfire's egg references the installed Blaze Spawn Egg model.

Gale Core resolves Breeze geometry, textures, shaders and keyframes, plus the Heavy Core block
model, from installed Minecraft resources. Original circlet/plate geometry and its atlas are Retold
assets. No Breeze or Heavy Core texture/model files are packaged. Standard item parents, armor
geometry and trims remain runtime Minecraft resources, including resource-pack replacements.

Aender Eye directly reuses the installed `minecraft:textures/item/ender_eye.png` texture as an
emissive billboard, including resource-pack texture replacements. No Eye of Ender texture or model
files are copied into Retold.

The Aender portal derives its green sprite at resource load from the installed
`minecraft:block/nether_portal` texture. Retold packages no copied or modified portal texture.

If an asset's authorship, source, license, or status is unclear, contact the developer before
redistributing or reusing it separately.
