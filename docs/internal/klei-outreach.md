# Sharing Retold with Klei

This is the local preparation record for a personal invitation to explore Retold. Nothing here has
been sent or published. The video context is the transcript Xef supplied in the conversation for
[How can we make Minecraft feel alive? (It isn’t structures)](https://www.youtube.com/watch?v=cBmt2FNP1Ug).
No external transcript retrieval succeeded; the guide does not invent timestamps or quotations.

## The package

Run from the repository root:

```bash
python3 tools/export_klei_field_guide.py
```

The exporter uses Python Markdown, PySide6, and pypdf, all already present in the working environment.
It renders the eight-page guide, embeds its existing screenshot, copies the full handbook and the
current configured JAR, includes license/credit files, and writes checksums and build identity.
It produces `output/klei-field-guide/` and `output/Retold-for-Klei.zip`. It does not rebuild or
modify the JAR, change gameplay, run Minecraft, send a message, or publish anything.

The HTML and PDF share the same Markdown content and field-guide styling. The PDF uses Qt’s
offscreen rich-text renderer with a print stylesheet and explicit page frames; it requires no browser
process or network access. Qt’s supported rich-text layout is documented in
[Supported HTML Subset](https://doc.qt.io/qt-6/richtext-html-subset.html). Chromium printing was
attempted here, but the environment denied process socket operations; the Qt renderer works within
those restrictions. PDF navigation is added from the laid-out text so links remain clickable.

The JAR version follows gradle.properties; the guide was updated for the 0.5.0 alpha release on
13 November 2026. The complete handbook remains the preceding 11 November development edition.
`BUILD.json` records the packaged JAR’s checksum,
file timestamp, source revision, dirty-worktree status, and static package checks. Preserve that
identity when sharing it. If the JAR or relevant sources change, check the claims and regenerate.

The eight-page source is [../klei-field-guide.md](../klei-field-guide.md). Keep the complete
[player handbook](../player-guide.md) as the optional reference. Recipient-facing files live inside
the ZIP; this preparation record and the capture checklist do not.

## Introduction draft

The exporter copies only the following message into `output/klei-message.txt`, outside the ZIP.
It deliberately makes no claim that footage is attached or that the mod is fully tested.

<!-- message:start -->
Subject: Retold — something to play with after your living-world video

Hey Klei,

Your video about making Minecraft feel alive made me want to send you Retold, a mod I've been working on.

The ecology is the part I thought you might enjoy. Animals need food, herds can move when their feeding grounds run out, and breeding depends on keeping animals fed and safe. Predators have hunger too. I’m interested in how those rules interact with the places players build.

It’s still experimental, especially the long-term balance. I use AI assistance for development, and some of the development artwork is generated. The design direction is mine; the project’s credits are included.

I've attached the build with a short field guide and the full handbook if you want to look anything up. Page 7 has a few things you can try straight away in Creative, or you can start a fresh Survival world.

Just wanted to share it so you could check it out and play around with it.

Xefensor
<!-- message:end -->

## Gameplay capture for Xef

**Status: not recorded.** The repository’s AGENTS.md assigns gameplay screenshots, video capture,
and subjective game inspection to the developer. No ecology footage or interface screenshots have
been fabricated. The field guide includes one existing, attributed earlier-build weather screenshot.

Use the exact packaged JAR, a fresh disposable cheats-enabled world, and ordinary active animals.
The gallery disables exhibit AI. Leave mob griefing enabled when demonstrating destructive grazing
or fence behavior. Keep the relevant chunks active. Debug hunger changes are allowed for setup,
but state that they were used. Use the guide’s page 7 procedures; keep the initial setup out of the
finished clip only if the caption still makes it clear.

Aim for 75–90 seconds of footage:

| Approximate edit time | Capture | What the caption must establish |
| --- | --- | --- |
| 0–20 seconds | A Cow consuming a real grass/crop block, then feeding at an accessible Wheat trough | Food can change the surroundings; show the block/stack change rather than only an idle animal. |
| 20–45 seconds | Two continuously well-fed adult Cows progressing to mating and a birth | At least five minutes of sustained care precedes readiness; label time compression and any hunger commands. |
| 45–75 seconds | A wild hungry Wolf pursuing suitable prey; show the successful kill if one occurs and the subsequent state | A meal reduces hunger and clears the current hunt. Do not imply a guaranteed forty-minute cooldown. |
| 75–90 seconds | A wide view of the same place or a brief range comparison | Leave room for an unscripted interaction. Do not claim this proves long-term ecosystem stability. |

Record the waits before editing. If a condition does not occur, retain the honest observation and
change the scene rather than staging an outcome with teleportation or kills. Keep before/after
debug-state screenshots in the local record when commands were used. There is no need to simulate
weeks of balance to make this introductory clip.

Useful additional screenshots, if time allows:

- Animal Feeder with its clear approach and actual food supply; caption the interaction used.
- Villager teaching panel with a supported finished sample and its displayed price.
- Enchanting interface showing controls without spelling out a solved word.

Put finished media under `output/klei-field-guide/` and regenerate the ZIP with the exporter’s
`--include-media` option. It includes only top-level PNG, JPEG, WebP, MP4, or WebM files other than
its generated page previews; optional media is not required to render or package the guide.

## Claim audit

These are implementation observations, not a report of new in-game testing. Paths below are
relative to `src/main/java/cz/xefensor/retold/` unless marked otherwise.

| Guide claim | Owning source / relevant existing coverage |
| --- | --- |
| Primitive tool opening and changed mining ladder | `progression/RetoldToolProgressionEvents.java`; `progression/RetoldToolProgressionGameTests.java`; source recipe and block tags. |
| Food depletion and range migration; feeder can anchor range | `behavior/home/RetoldHerdRangeEvents.java`, `behavior/food/RetoldRangeForage.java`, `behavior/food/RetoldAnimalFeederBehavior.java`. |
| Sustained five-minute satisfaction, nearby compatible adults | `behavior/breeding/RetoldAnimalBreeding.java`; existing `animal_breeding_requires_sustained_satisfaction` GameTest. |
| No breeding population cap | Same breeding implementation; existing `unloaded_breeding_uses_food_satisfaction_without_population_cap` GameTest. |
| Successful hunt relieves hunger and clears current target | `behavior/hunting/RetoldControlledHuntingEvents.java:onLivingDeath`; `behavior/profiles/RetoldMobRules.java`. Remaining hunger/state still matter. |
| Predator perception and fatigue | `behavior/hunting/RetoldPredatorSearchEvents.java`, `behavior/hunting/RetoldPredatorStaminaEvents.java`. |
| Seven-day bounded world-time catch-up | `behavior/ecology/RetoldUnloadedCatchUpPlan.java`, `RetoldUnloadedEcosystemCatchUp.java`; corresponding JUnit and GameTests. |
| Polar Bears have no Retold hunger cycle | `src/main/resources/data/retold/mob_profiles/polar_bear.json`: hunger interval zero, predator false. |
| Gallery is an asset inspection tool with frozen exhibits | `command/showcase/RetoldShowcaseEntities.java`; commands in `command/RetoldCommands.java`. |
| Commands and hunger direction | `behavior/debug/RetoldBehaviorDebugEvents.java`; `/retoldbehavior` requires level 2 and looks at an eligible creature within approximately 24 blocks. |

Do not turn the video’s specific suggestions into claims about Retold: there is no implemented
lichen/substrate succession loop, Muskox, universal predator-loot suppression, universal fixed
post-kill cooldown, or habitat-quality production bonus established by this review. Local collapse
is allowed by the confirmed Retold design; no stable-equilibrium guarantee is made.

## Validation and remaining checks

For this documentation/package task, follow `testing_strategy.md`: validate the exports, links,
formatting, claims, copied bytes, metadata, and `git diff --check`. Do not run unrelated gameplay
suites. The exporter checks that the bundled JAR matches the available compiled classes and current
packaged resources, and rejects source/build inputs newer than that JAR. Those checks do not prove
runtime behavior or replace an in-game check.

Xef still needs to exercise the three natural/command-assisted scenarios and record the clip.
Multiplayer, dedicated-server, old-world, complete-progression, and long-session balance results
remain outside this document preparation. No sending or publishing is authorized by this file.

Documentation preparation checks completed on 11 November 2026:

- PDF export: eight A4 pages, eight navigation bookmarks, and 15 hyperlink hit areas. Every page
  fits its content frame; rendered pages were inspected, including the dense food/playground pages.
- Guide/handbook HTML: local files and internal anchors resolve; the guide screenshot is embedded.
  External URLs were preserved but not network-verified.
- Package identity: all 943 available compiled classes and 2,435 current source resource files
  match the existing JAR byte for byte. The JAR’s SHA-256 is
  `424434d4a5b5dad38e68ca42432a65013b43a194e99cab586133262c79cc392d`.
- ZIP: integrity, every included file’s checksum, unchanged handbook copies, and the original JAR
  bytes were verified. The message draft and local capture instructions are outside the ZIP.
- Python syntax and new-file whitespace checks pass; `git diff --check` passes. No Gradle build,
  new GameTest run, game-client run, server playtest, or new gameplay capture was performed.
