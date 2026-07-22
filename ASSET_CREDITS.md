# Retold Asset Credits And Status

This record complements [`LICENSE`](LICENSE) and [`LICENSE-ASSETS.md`](LICENSE-ASSETS.md).

## Credited Contributors

| Contributor | Work | Status |
| --- | --- | --- |
| Jesse Schramm | Extinguished-torch textures | Included |
| Xefensor | Gameplay screenshots dated 2026-07-18 | Repository documentation |
| OpenAI image generation, directed and processed by Codex | Aender wood-family placeholder textures: planks, stripped log, sapling, door, trapdoor, signs, boats, and derived model atlases | AI-generated placeholder; 2026-07-22 |
| OpenAI image generation, directed and processed by Codex | Aender Chronolith block and Aender Eye/Gale Core spawn-egg placeholder textures | AI-generated placeholder; 2026-07-22 |
| Future original artists | Final replacements for provisional Aender and other development assets | To be credited when work is accepted |

## Provisional And Generated Assets

The current Aender visuals include AI-generated textures and other provisional development assets. They exist to test terrain, lighting, blocks, portals, and gameplay while original artwork is developed.

These assets are explicitly **placeholders** and do not represent the intended final visual identity. Placeholder status does not grant permission to extract or reuse them; the asset license still applies.

The provisional portal frame uses the development identifier `retold:dev_aender_portal_frame`. Its name and visuals are planned to change when the final design is chosen.

The Aender wood-family placeholders were generated specifically for Retold from prompts describing original purple pixel-art wood components. Mechanical post-processing removed backgrounds, resized the sources to Minecraft texture dimensions, split the door sheet, and assembled sign and boat model textures from the generated material. No Minecraft texture pixels were used in those derived files.

The Aender Chronolith and spawn-egg placeholders were generated specifically for Retold from prompts describing original purple time-rune stone, a green-eyed Aender egg, and a pale cyan/gold Gale Core egg. Mechanical post-processing removed chroma-key backgrounds and reduced the sources to 16x16 game textures. No Minecraft texture pixels were used in these files.

## Third-Party Material

Minecraft, NeoForge, and other third-party names, APIs, templates, or materials remain subject to their own terms. NeoForge MDK template files identified by `TEMPLATE_LICENSE.txt` retain that license.

The Aender portal references the installed `minecraft:block/nether_portal` texture and creates a green sprite at resource-load time. Retold does not package a copied or modified Minecraft portal texture.

If an asset's authorship, source, license, or status is unclear, do not redistribute or reuse it separately. Contact the developer first.
