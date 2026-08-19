# Retold Asset Credits And Status

This record complements [`LICENSE`](LICENSE) and [`LICENSE-ASSETS.md`](LICENSE-ASSETS.md).

## Credited Contributors

| Contributor | Work | Status |
| --- | --- | --- |
| Jesse Schramm | Extinguished-torch textures | Included |
| Xefensor | Gameplay screenshots dated 2026-07-18 | Repository documentation |
| Xefensor | Runtime-generated horizon-figure skin encoded from a developer-supplied and approved visual source | Included; 2026-08-19 |
| Xefensor, with drawing instruction and review from Codex | Hand-drawn Aender Sand texture created in Krita | Original replacement included; 2026-08-14 |
| OpenAI image generation, directed and processed by Codex | Aender wood-family placeholder textures: planks, stripped log, sapling, door, trapdoor, signs, boats, and derived model atlases | AI-generated placeholder; 2026-07-22 |
| OpenAI image generation, directed and processed by Codex | Aender Chronolith block and Aender Eye/Gale Core spawn-egg placeholder textures | AI-generated placeholder; 2026-07-22 |
| OpenAI image generation, directed and processed by Codex | Experimental Aender Desert placeholder textures: Aender sand, sandstone, and cactus | AI-generated placeholders; Aender Sand superseded on 2026-08-14 |
| OpenAI image generation, directed and processed by Codex | Aenderite Ore, Raw Aenderite, and Aenderite Ingot placeholder textures | AI-generated placeholder; 2026-07-22 |
| Future original artists | Final replacements for provisional Aender and other development assets | To be credited when work is accepted |

## Provisional And Generated Assets

The current Aender visuals include AI-generated textures and other provisional development assets. They exist to test terrain, lighting, blocks, portals, and gameplay while original artwork is developed. Aender Sand is the first accepted hand-drawn replacement for one of these generated textures.

These assets are explicitly **placeholders** and do not represent the intended final visual identity. Placeholder status does not grant permission to extract or reuse them; the asset license still applies.

The provisional portal frame uses the development identifier `retold:dev_aender_portal_frame`. Its name and visuals are planned to change when the final design is chosen.

The Aender wood-family placeholders were generated specifically for Retold from prompts describing original purple pixel-art wood components. Mechanical post-processing removed backgrounds, resized the sources to Minecraft texture dimensions, split the door sheet, and assembled sign and boat model textures from the generated material. No Minecraft texture pixels were used in those derived files.

The Aender Chronolith and spawn-egg placeholders were generated specifically for Retold from prompts describing original purple time-rune stone, a green-eyed Aender egg, and a pale cyan/gold Gale Core egg. Mechanical post-processing removed chroma-key backgrounds and reduced the sources to 16x16 game textures. No Minecraft texture pixels were used in these files.

The experimental Aender Desert placeholders were generated specifically for Retold from prompts describing original deep denim energy sand, medium periwinkle layered sandstone with a separate smoother top face, and lavender ribbed cactus material. Mechanical post-processing reduced the generated material sources to 16x16 game textures and derived the cactus top from the original generated cactus material. Developer-provided inverted-desert screenshots were used only as color, mood, and block-face-layout references; no pixels from them or from Minecraft textures were included. The generated Aender Sand texture is retained only in Git history: Xefensor replaced the active file with an original hand-drawn Krita texture on 2026-08-14.

The Aenderite placeholders were generated specifically for Retold from prompts describing muted mint-green and deep teal mineral chips, a raw mineral lump, and a refined ingot. Mechanical post-processing removed chroma-key backgrounds and reduced the item sources to 16x16. The ore pattern was rebuilt as original pixel clusters over Retold's existing animated Aender Stone base and repeated across all animation frames. A developer-provided End Portal Frame image was used only as a green-palette reference; no pixels or frame geometry from that image or from Minecraft textures were included.

## AI Texture Audit

The active PNG textures explicitly credited as OpenAI-generated were audited against their introduction commits on 2026-08-14. Companion JSON and `.mcmeta` files are implementation metadata and are not counted as creative textures. Of the 24 credited generated PNGs, 23 remain active AI-generated placeholders and one has been replaced:

| Batch | Active AI-generated placeholder files | Replacement status |
| --- | --- | --- |
| Aender wood family | `block/aender_door_bottom.png`, `block/aender_door_top.png`, `block/aender_hanging_sign.png`, `block/aender_planks.png`, `block/aender_sapling.png`, `block/aender_sign.png`, `block/aender_trapdoor.png`, `block/stripped_aender_log.png`, `block/stripped_aender_log_top.png`, `entity/boat/aender.png`, `entity/chest_boat/aender.png`, `item/aender_boat.png`, `item/aender_chest_boat.png` | 13 placeholders remain |
| Chronolith and spawn eggs | `block/aender_chronolith.png`, `item/aender_eye_spawn_egg.png`, `item/gale_core_spawn_egg.png` | 3 placeholders remain |
| Aender Desert | `block/aender_cactus_side.png`, `block/aender_cactus_top.png`, `block/aender_sandstone.png`, `block/aender_sandstone_top.png` | The generated `block/aender_sand.png` was replaced by Xefensor's hand-drawn texture; 4 placeholders remain |
| Aenderite | `block/aenderite_ore.png`, `item/raw_aenderite.png`, `item/aenderite_ingot.png` | 3 placeholders remain |

## Third-Party Material

Minecraft, NeoForge, and other third-party names, APIs, templates, or materials remain subject to their own terms. NeoForge MDK template files identified by `TEMPLATE_LICENSE.txt` retain that license.

The Aender portal references the installed `minecraft:block/nether_portal` texture and creates a green sprite at resource-load time. Retold does not package a copied or modified Minecraft portal texture.

If an asset's authorship, source, license, or status is unclear, do not redistribute or reuse it separately. Contact the developer first.
