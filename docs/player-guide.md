# Retold Player Handbook

**A beginner's journey and complete reference to the current mod**

For the **0.4.0 development source**, targeting **Minecraft 26.2**, **NeoForge 26.2.0.7-beta**, and **Java 25**. Updated 11 November 2026. Older release JARs may contain fewer features.

Start with Sticks and Flint, build your first home, learn recipes and magic, and discover a world whose creatures have needs and alliances. Defeat the dragon, complete six sacrifices, and prepare for the changing islands of the Aender.

The opening chapters assume you are new to Retold and explain the Minecraft basics needed to follow along. Later chapters teach animal care, village life, territory, bosses, and dimension travel. The appendices provide recipe diagrams, lesson prices, trades, equipment values, content IDs, and practical troubleshooting.

**Full progression and boss spoilers.** The solved enchanting alphabet and exact spell words are omitted. Retold is experimental; unfinished features and important current-build differences are identified where they affect your choices.

## Contents

- [1 Getting started](#1-getting-started)
- [2 Your first days and equipment](#2-your-first-days-and-equipment)
- [3 Learning recipes and Villager lessons](#3-learning-recipes-and-villager-lessons)
- [4 Enchanting from the beginning](#4-enchanting-from-the-beginning)
- [5 Keeping animals and pets](#5-keeping-animals-and-pets)
- [6 Understanding the living ecosystem](#6-understanding-the-living-ecosystem)
- [7 Creature field guide](#7-creature-field-guide)
- [8 Factions and territory](#8-factions-and-territory)
- [9 Living with Villagers](#9-living-with-villagers)
- [10 The three world stages](#10-the-three-world-stages)
- [11 Completing the Dragon Egg ritual](#11-completing-the-dragon-egg-ritual)
- [12 Structures and guardian expeditions](#12-structures-and-guardian-expeditions)
- [13 The Nether and the End](#13-the-nether-and-the-end)
- [14 Your first Aender expedition](#14-your-first-aender-expedition)
- [15 Other changes and experimental tools](#15-other-changes-and-experimental-tools)
- [16 Commands and server settings](#16-commands-and-server-settings)
- [17 Current build limits and unfinished work](#17-current-build-limits-and-unfinished-work)
- [Appendix A Recipes and processing](#appendix-a-recipes-and-processing)
- [Appendix B Villager lesson index](#appendix-b-villager-lesson-index)
- [Appendix C Equipment and trade reference](#appendix-c-equipment-and-trade-reference)
- [Appendix D Enchantment catalog without spell solutions](#appendix-d-enchantment-catalog-without-spell-solutions)
- [Appendix E Complete custom content catalog](#appendix-e-complete-custom-content-catalog)
- [Appendix F Troubleshooting](#appendix-f-troubleshooting)
- [Further reading and support](#further-reading-and-support)

## 1 Getting started

### What you are playing

Minecraft is a game about exploring a block-built world, collecting materials, making equipment, and surviving its dangers. Retold is a mod for Minecraft Java Edition: it changes that game rather than replacing it with a separate application. When this handbook says **vanilla**, it means ordinary Minecraft without Retold's changes.

Retold changes both your early survival routine and what happens after the Ender Dragon. You begin with primitive Flint equipment, learn recipes and magic, and live among creatures that respond to food, danger, and one another. Major achievements change the entire world's stage. The dragon fight opens a longer journey through six sacrifices and eventually the Aender dimension.

You do not need to memorize this handbook before playing. Read this chapter and the opening walkthrough, then look up a system when you meet it. The later chapters reveal progression and boss mechanics. Exact enchanting words and the solved alphabet are deliberately omitted so that the language remains a discovery.

### Install the matching version

This edition describes the **0.4.0 development source** for **Minecraft 26.2**, **NeoForge 26.2.0.7-beta**, and **Java 25**. An older released Retold JAR may have fewer features. Use the version supplied with the mod rather than assuming any Minecraft or loader release will work.

1. Create or choose a Minecraft Java instance using the matching NeoForge loader. An instance is a separate game installation with its own mods and settings.
2. Put the Retold JAR in that instance's `mods` folder. Keep it as a JAR; do not unpack it into loose files.
3. Start the NeoForge instance. Make a new **Survival** world for the most consistent introduction to progression.
4. For multiplayer, install the same Retold version on the server and on every player's client. The server runs the shared world; a client is each player's game application.
5. Copy an existing world's save somewhere safe before adding or updating the mod. Newly generated terrain and already explored terrain can behave differently.

Retold does not require a separate collection of content mods. Datapacks or other mods can change its defaults, so a server's handbook may need additional rules. Official download and support links are at the end.

### Learn these controls first

The following are ordinary keyboard-and-mouse defaults. Rebound controls take precedence; check **Options → Controls** if a key does something different.

| Action | Default control | Why you need it |
| --- | --- | --- |
| Walk and look | W/A/S/D and mouse | Move around and aim at blocks or creatures |
| Jump | Space | Climb one-block steps and cross gaps |
| Sneak | Left Shift | Move carefully; also modifies feeder interactions |
| Sprint | Left Control while moving | Travel faster, but some creatures react to sprinting |
| Break a block or attack | Left mouse button | Hold it to break a block; careless clicks can hurt creatures |
| Use an item, open a block, or place a block | Right mouse button | Open stations, light a Campfire, feed animals, and place materials |
| Open inventory | E | See carried items, armor, and the small crafting grid |
| Select a hotbar slot | Number keys 1–9 or mouse wheel | Choose the item held in your hand |
| Drop the held item | Q | Useful for dropped food, but also an easy way to lose valuables |

Right-clicking while aiming at a creature is different from dropping an item near it. Animals can have a broader autonomous diet than the items accepted by their ordinary hand-feeding interaction. Similarly, Shift-clicking an inventory slot moves items within a menu; sneaking while right-clicking a placed feeder performs a world interaction.

### Understand your inventory and crafting grids

A collected object becomes an **item** in your inventory. A **block item** can usually be placed back into the world. Most ordinary materials stack, often up to 64 per slot, while tools and other items have different limits. A recipe's quantity refers to items, not full stacks: “three Ingots” means three individual Ingots.

The inventory has a **2×2 crafting grid**. Place ingredients in the required arrangement and take the result. A placed Crafting Table opens a **3×3 grid**, which is needed for larger recipes. Merely putting ingredients into a grid does not complete crafting; you must take the output.

For example, a Log becomes Planks, and four Planks in a square make a Crafting Table. Retold changes how you obtain that first Log, so follow the Flint instructions in chapter 2 instead of trying the usual punch-a-tree opening.

The **recipe book** is a convenience for known recipes. An empty book does not mean crafting is impossible. Unless a server adds a separate restriction, you can arrange a valid unknown recipe manually. A removed recipe is different: no amount of arranging its old ingredients will restore it.

### Health, food, experience, and durability

Your red hearts represent health. One full heart equals two health points, so a creature with 120 health has sixty hearts' worth before defenses. Armor, resistances, and shields can change how much of an attack reaches that health.

Your own food bar is separate from Retold's hidden creature hunger. Eat suitable food by holding its use action. Keeping yourself fed supports ordinary survival and healing rules. Retold does not add a completed system that heals you simply for lying in bed.

Green experience orbs fill your experience bar and increase the level number above the hotbar. **Experience points and experience levels are different units.** An enchanting cost of fifteen levels subtracts fifteen levels; it is not fifteen individual orbs. The experimental Chronolith instead drains experience points.

Equipment has **durability**, meaning how much use it can withstand before breaking. A tool can wear out even when it is the correct tool. Retold's unenchanted Diamond items have especially low durability; chapter 2 explains how enchanting changes that.

In Survival, death uses ordinary Minecraft consequences unless the server changes them. Return for dropped equipment promptly. Retold's proposed longer death-drop lifetime is unfinished, so do not assume your belongings will wait indefinitely. A valid Overworld bed can set your respawn destination, but by default sleeping does not skip night.

### Words used throughout the handbook

| Word | Meaning |
| --- | --- |
| Mob | A living creature controlled by the game, such as a Cow, Zombie, or Villager |
| Passive, neutral, hostile | Broad descriptions of danger: peaceful, reactive under certain conditions, or ordinarily aggressive; Retold adds more context to these categories |
| Structure | A generated place such as a Jungle Pyramid, Ocean Monument, or Air Temple |
| Dimension | A separate world space, such as the Overworld, Nether, End, or Aender |
| Chunk | A 16×16 horizontal column of world terrain through its height |
| Loaded area | Terrain currently kept active by the game; distant areas may be unloaded and stop ordinary entity ticking |
| World stage | A saved, shared progression state that changes world rules for all players |
| Faction | A group that helps determine creatures' alliances, enemies, and defense rules |
| Territory | A particular defended place, with local guards and suspicion about intruders |
| Datapack | World/server data that can change recipes, tags, and other definitions without editing Retold's Java code |
| Operator | A player granted administrative command permission by the server |

A creature's remembered home is often a location in its behavior, not a newly generated building you can find. “A Wolf den” therefore does not necessarily mean a visible cave or constructed kennel. A chunk being unloaded also does not mean the real-world clock continues to simulate every action there; chapter 6 explains Retold's limited catch-up.

### Your immediate priorities

Find Sticks and Flint, obtain your first wood with a Flint Multi-tool, secure food and shelter, and establish covered lighting. Keep valuable storage away from active Nether portals and dangerous boss sites. Treat Villagers' belongings and guarded structures as property rather than free supplies.

Survival introduces the intended rules. Creative gives unlimited materials and different creature-targeting behavior, so a peaceful Creative encounter does not demonstrate how that creature treats a Survival player. Adventure and Spectator have their own restrictions as well.

## 2 Your first days and equipment

The numbered steps describe the equipment route; they are not an instruction to postpone eating or shelter. Secure food whenever you find a safe source, cook it as soon as you have fire, and place covered Torches as soon as Charcoal is available. If night arrives before you reach Steel, stay with the shelter and supplies you already have.

### Step 1 Gather Sticks and Flint

Find a tree and break its leaves. Retold gives leaves a chance to drop supplemental Sticks; several bushes and Dead Bushes can also supply them. Not every leaf drops a Stick, so keep gathering. Break Gravel to look for Flint. Keep enough for **two Flint in the Multi-tool and a third if you will use bare Flint to ignite your first Campfire**.

Punching a Log can break it, but it does not give you its wood without a correct harvesting tool. Start with the smaller resources rather than spending your first minutes repeatedly destroying trunks without collecting anything.

### Step 2 Make a Flint Multi-tool

Open your inventory and use its 2×2 grid:

```text
Flint   Flint
empty   Stick
```

Take the **Flint Multi-tool** from the output. Hold it and break Logs to collect wood. It has 48 durability, so keep spare Flint and avoid spending all its life on unnecessary blocks.

This tool combines primitive chopping, digging, and selected soft-material mining. It can harvest ordinary exposed Copper Ore, Sandstone and its supported variants, Tuff, and Calcite. It cannot collect ordinary Stone or Deepslate Copper Ore. Cutting wood also does not make it a full-featured Axe for every right-click action.

The normal Wooden and Stone tool, weapon, and Spear recipes are removed. You cannot bypass the Flint opening by following an ordinary Minecraft wooden-pickaxe tutorial.

**If nothing drops:** check the held tool, the block type, and whether your tool broke. Deepslate Copper looks different from ordinary Copper Ore and belongs to a later mining tier.

### Step 3 Turn wood into a working base

Craft a Log into Planks in the inventory grid. Four Planks in a square make one Crafting Table. Place it and right-click to open the larger grid. Two Planks placed vertically make four Sticks.

Set up a small shelter with a roof, an entrance you can close, and room for stations. A Chest made from eight Planks around an empty center stores your excess materials. Keep emergency food and a spare tool there. Avoid digging directly down into an unknown drop or working at a cliff edge while learning the controls.

Beds use the normal three-Wool-over-three-Planks recipe. They can set a valid Overworld respawn point, but **night continues while you rest** under Retold's default setting. Shelter and lighting remain useful even after you make a bed.

### Step 4 Build and light a Campfire

At the Crafting Table, arrange three Sticks and three Logs:

```text
empty   Stick   empty
Stick   empty   Stick
Log     Log     Log
```

Coal is not an ingredient. Place the Campfire and notice that it starts **unlit**. Hold bare Flint and right-click it to light it, consuming one Flint in Survival. Flint and Steel can also ignite it later. A waterlogged Campfire must be made suitable for lighting first.

A lit Campfire can cook four items at once. Hold a valid input and right-click the cooking surface to place items; wait for completed outputs to pop out, then collect them. The crafted Campfire does not need a continuing inventory of furnace fuel to keep this cooking fire going. Keep yourself off the flames.

### Step 5 Fire eight Bricks

Find Clay, commonly in or near water, and collect Clay Balls. Put them on the lit Campfire. **One Clay Ball becomes one Brick after thirty seconds** at normal game speed. Four cooking positions let you make eight Bricks in two batches.

Bricks are individual items here. Do not combine them into the decorative Bricks block when the recipe asks for eight Brick items.

Use the eight Bricks to make the **Brick Furnace**:

```text
Brick   Brick   Brick
Brick   empty   Brick
Brick   Brick   Brick
```

This block occupies the vanilla Smoker's place in the game, with a new name and an earlier progression role. You are not looking for a second, separate custom furnace with the same function.

### Step 6 Smelt Copper and make a Pickaxe

Harvest exposed ordinary Copper Ore with the Flint Multi-tool to obtain Raw Copper. Open the Brick Furnace. Put the material to process into the input slot, suitable fuel into the fuel slot, and take the finished product from the result slot.

The Brick Furnace cooks food, smelts Raw Copper and supported Copper Ore inputs into Ingots, and turns burnable Logs into Charcoal. Suitable wooden fuel gets you started before you have Coal. Charcoal is fuel and a Torch ingredient; it is different from decorative Coal-colored blocks or raw ore.

Make a Copper Pickaxe at the Crafting Table:

```text
Copper   Copper   Copper
empty    Stick    empty
empty    Stick    empty
```

Each “Copper” is one Copper Ingot. Use the Pickaxe to collect ordinary Stone as Cobblestone. Copper mines normal Stone slowly in Retold; slow progress here does not mean you chose a completely unusable tool.

### Step 7 Make a normal Furnace and obtain Iron

Eight Cobblestone around an empty center make a **Furnace**. It is a different station from the Brick Furnace. Place it and smelt Raw Iron into Iron Ingots using ordinary fuel. Find Iron in ordinary Stone while you are still below Steel; dark Deepslate ores are not an early shortcut.

Iron gives you useful tools and utilities such as a Bucket, Shield, and Flint and Steel. A Bucket needs three Iron Ingots. Flint and Steel needs one Iron Ingot and one Flint in a shapeless recipe. Ingredient quantities and grids are collected in Appendix A.

Iron tools still do not harvest Deepslate or Diamond Ore correctly under Retold's material rules. Make Steel before committing to deep mining.

### Step 8 Turn Iron into Steel

First obtain three Smooth Stone. Smelt Cobblestone into Stone, then smelt that Stone again into Smooth Stone. These are three distinct item names; decorative Smooth Stone Slabs will not substitute for the blocks.

Make a Blast Furnace:

```text
Iron          Iron       Iron
Iron          Furnace    Iron
Smooth Stone  Smooth Stone  Smooth Stone
```

Use five Iron Ingots, one normal Furnace, and three Smooth Stone. The uneven word widths in the diagram do not change its three columns.

Put an Iron Ingot in the Blast Furnace's input and ordinary suitable fuel below it. **One Iron Ingot becomes one Steel Ingot in five seconds** at normal tick speed. Charcoal works as fuel; it is not an additional fixed-ratio ingredient mixed into the crafting grid.

Three Steel Ingots and two Sticks make a Steel Pickaxe in the same arrangement as the Copper Pickaxe. Steel also makes an Axe, Shovel, Hoe, Sword, Spear, Helmet, Chestplate, Leggings, and Boots. Appendix A contains every pattern.

### Step 9 Establish food and reliable lighting

Use food you can obtain safely, and cook suitable raw meat or fish. Three Wheat in a horizontal row make Bread. A crop farm is useful for your own food and for animals, but do not assume every plant a creature eats will be left untouched. Grazers and hungry foragers can consume their surroundings.

One Coal or Charcoal above one Stick makes four normal Torches. Place them under solid cover where possible. Exposed normal, Soul, and Copper torches can go out in rain or snow. Extinguished torches stop lighting the area; their appearance is a clue, not a different permanent crafting ingredient.

Relight a torch by right-clicking with **Flint and Steel or a Fire Charge**. Bare Flint is your early **Campfire** igniter, not the default extinguished-torch igniter. Flint and Steel loses durability; a Fire Charge is consumed. A torch you relight while still exposed to weather can go out again. Breaking an extinguished torch returns the corresponding ordinary lit torch item.

### Know which equipment to bring

| Tier | Your practical role for it | Important limit |
| --- | --- | --- |
| Flint | Obtain first Logs, soft materials, and exposed ordinary Copper Ore | Short-lived; not a Stone or Deepslate Pickaxe |
| Copper | Begin ordinary Stone mining and basic equipment | Slow on Stone; cannot collect Steel-gated deep materials |
| Iron | Useful tools, armor, utilities, and Blast Furnace construction | Still below the Deepslate/Diamond drops gate |
| Steel | Harvest Deepslate and both Diamond Ore variants; stronger complete equipment set | Cannot collect Obsidian or Ancient Debris |
| Diamond | Harvest Obsidian and Ancient Debris; bridge to Netherite | Fragile until enchanted |
| Netherite | Ordinary late upgrade and successful Aenderite mining | Needs the vanilla upgrade resources and Smithing Table |
| Gold | Optional fast, fragile equipment | Not a replacement for the required mining ladder |
| Aenderite | Ore, raw material, and Ingot foundation | No completed equipment or Ingot crafting uses |

Breaking a block and successfully harvesting it are different. Steel's special permission covers Deepslate, its supported processed forms and ores, and ordinary Diamond Ore. It does not override every other block's unbreakability or loot rules.

Copper is less frequent in newly generated terrain: its two modified ore placements use six attempts per chunk instead of sixteen. Existing ore is not retroactively removed. Iron and Diamond generation remain ordinary Minecraft in this part of the overhaul.

### Make Diamond last

Unenchanted Diamond tools and Spears have a maximum durability of **64**. Player armor is also fragile: Helmet 66, Chestplate 96, Leggings 90, Boots 78. Save the first Diamond Pickaxe for important mining instead of wearing it out on a mountain of common blocks.

**Any enchantment restores ordinary Diamond maximum durability, including a curse alone.** You can use the enchanting table or apply an appropriate enchanted book at an Anvil. Chapter 4 teaches both routes. Creating an Enchanting Table itself requires Obsidian, so a fragile first Pickaxe can still perform the necessary Obsidian mining before you improve it.

Enchanting does not promise to erase existing item damage. Removing every enchantment makes Diamond fragile again while preserving damage; a heavily damaged stripped item can have only one final use. A Grindstone is therefore not a free way to keep enchantment-strength durability without enchantments.

Diamond Horse and Nautilus Armor has 96 durability before enchanting and 528 after, and takes wear from hits. Other Horse/Nautilus Armor remains indestructible under these changes. Wolf Armor and Horse/Nautilus Armor support compatible Chestplate enchantments; Fire Protection reduces fire harm without making the creature fireproof.

### Repair and upgrade with care

Flint is the Flint equipment repair material; Steel Ingots repair Steel equipment through the applicable repair workflow. Anvil repairs and combinations use normal costs and compatibility. Read the preview before taking the result, particularly if a change removes enchantments or alters a fragile Diamond item.

Netherite still uses the ordinary Smithing Table workflow: an appropriate Netherite Upgrade Smithing Template, Diamond equipment, and a Netherite Ingot. The wider Retold Netherite redesign is unfinished. Aenderite Ore needs a Netherite Pickaxe for its drops; owning Diamond equipment alone is not enough.

Equipment can also come from loot, trades, or other players. New smith trades are deliberately staged: Copper at Apprentice, Iron at Expert, and Diamond at Master. Those are trade progression levels, not the world stage and not a universal limit on Villager lessons.

### A useful first-expedition checklist

Before leaving your base for a long trip, bring food, an appropriate Pickaxe, a weapon, spare building blocks, covered-lighting supplies, an igniter, and a plan for returning. Keep spare tools and some materials at home rather than taking every resource into your first risky encounter. For a new player, a recoverable short expedition teaches more than losing the only working equipment far away.

You are ready to move beyond the opening when you can replace your equipment, supply your own food, keep a small safe base, and explain which material your current tool can harvest. Learning recipes, working with Villagers, and enchanting then make that progress more reliable.

## 3 Learning recipes and Villager lessons

Retold changes how you discover crafting instructions. You can know that an item exists without knowing how to make it, and you can carry a finished item without unlocking its recipe. Recipes and items are separate things: a recipe is a set of inputs and a method, while an item is the result you can hold and use.

Under ordinary world settings, an unknown valid recipe still works if you arrange its ingredients correctly. An empty recipe book does not mean crafting is disabled. It means the book has not learned the instructions for you. Use the opening recipes in this handbook to make your first tools and stations, then build up your own collection of discoveries.

### What makes a recipe known

Successfully crafting an item can record the recipe you used. Taking processed output from a Furnace, Brick Furnace, or Blast Furnace also provides a learning route. Some matching cooking recipes can be learned together when they use the same ingredient and produce the same result in different stations.

That last condition matters. Learning to process Raw Copper does not necessarily reveal every recipe that produces a Copper Ingot. Copper Ore, Deepslate Copper Ore, and Raw Copper are different inputs, even though they can lead to the same metal. Likewise, the same finished food can have Furnace, Brick Furnace, and Campfire preparation methods.

Picking up ingredients no longer gives you the usual automatic list of possibilities. Finding or buying a finished item is useful, but possession alone is not a general recipe unlock. You can use an item you found even when its crafting instructions remain unknown.

Recipe knowledge belongs to your player and is saved. Another player's discoveries do not automatically become yours. Everyone shares the world's stage, but each player has their own recipe and enchantment knowledge.

For this development build, do not rely on Campfire cooking or Smithing alone to reveal recipe-book entries. Their direct learning paths need further verification. This concerns discovering the instructions; the supported processing and Smithing operations themselves can still be performed. The beginner progression does not require every operation to appear in the book before you use it.

### A lesson is different from a trade

A normal trade exchanges items. For example, buying a tool gives you a tool to use. A **Villager lesson** exchanges Emeralds for crafting knowledge. It teaches a recipe for a finished sample you already possess; it does not hand you a newly made item or provide the ingredients.

The two functions share the Villager's trading screen, but their slots and payments are separate. Buying equipment follows the Villager's available offers and profession level. Lessons depend on the profession's configured teaching list, your sample, your existing knowledge, and your Emeralds. You do not have to reach the equipment trade's mastery level to take a lesson that is otherwise available.

A Toolsmith can teach tools, a Farmer can teach food, and a Librarian can teach selected books and workstations. Their professions are useful clues, not promises that they know every related recipe. Check the teaching panel for the actual answer. Wandering Traders do not teach through this system.

### Taking your first lesson

Open a professional Villager's ordinary trading screen. Look for the extra teaching panel beside the normal offers. It has a single sample slot, a status message, an Emerald cost, and a **Learn** button.

1. Put **one finished example of the item you want to make** in the teaching slot. Show Bread to ask about Bread. Putting Wheat there asks about Wheat's own recipe; it does not ask what can be made from Wheat.
2. Keep the required Emeralds in your player inventory. The teaching slot is for the sample, and it does not accept Emeralds.
3. Read the status and price. An available lesson enables Learn and highlights the sample slot.
4. Click **Learn**, or press **Enter** or **keypad Enter** while the lesson is available.
5. Close the screen to recover your sample. You can also take it back from the sample slot; it is not consumed as the lesson payment.

A successful lesson spends the displayed Emerald cost and records the recipe. It also gives the Villager experience, which can help it advance its profession level and gain further ordinary offers. Repeating a lesson you already know does not provide another paid unlock or repeat experience reward.

The panel gives brief visual and sound feedback when the server accepts or rejects a request. Wait for its updated result before clicking again. The button temporarily pauses while a request is pending, so rapid clicking is unnecessary.

### Worked example with Bread

Suppose you have a piece of Bread from exploration, trading, or another player, but you have never crafted it yourself. You want the recipe to become visible in your book.

Find a Farmer, open the trading screen, and put that Bread in the teaching slot. The current Bread lesson costs **one Emerald**. Keep at least one Emerald in your inventory and choose Learn. On success, you pay one Emerald and learn the recipe; the Bread remains your sample and comes back when you close the menu.

You still need the ingredients to make more. Bread uses three Wheat across a crafting row. Learning the recipe supplies the instructions, not a stockpile of Wheat and not another free loaf.

A Campfire is another useful example, with a different price: a Fisherman can teach its recipe for **two Emeralds** when you show a Campfire sample. The normal Campfire recipe is already provided in the opening walkthrough, so you can also make one manually without buying that lesson. An Enchanting Table lesson costs **twelve Emeralds** from a suitable Librarian or Cleric; that does not replace the materials needed to build the table.

### Choosing the right teacher

Use this as a starting point, then check the complete lesson reference for individual items and prices. The panel remains the final check for an item in your current world.

| Profession | Examples of teaching topics |
| --- | --- |
| Farmer | Bread, several foods, seeds, Hay Bale, Composter |
| Fisherman | Fishing Rod, Barrel, Campfire, cooked fish |
| Butcher | Cooked meat and the Brick Furnace |
| Toolsmith | Flint Multi-tool, Copper/Iron/Steel/Gold/Diamond tools, selected workstations |
| Weaponsmith | Spears, swords, axes, Shield, Crossbow |
| Armorer | Selected metal armor, Shield, Anvil, Blast Furnace |
| Cleric | Brewing Stand, selected potion ingredients, Eye of Ender, Enchanting Table |
| Librarian | Books, paper, Bookshelf, Lectern, Enchanting Table |
| Fletcher | Arrows, bows, Crossbow, Target, Dispenser |
| Leatherworker | Leather armor, Bundle, Book, Cauldron, frames |
| Mason | Selected brick and stone building recipes, Stonecutter |
| Shepherd | Carpets, beds, Painting, Loom |
| Cartographer | Map, Compass, Cartography Table, selected frames |

Some items have more than one teacher, and their prices can differ. A Book lesson, for example, does not have to come from the same profession as an Enchanting Table lesson. Choosing a cheaper available teacher does not change the resulting recipe.

The current teaching data also contains entries whose recipe identifiers do not match available recipes in this Minecraft version. These include certain stew, wool, banner-pattern, and polished-wall entries. They are separated from available lessons in the reference appendix. Do not spend a journey looking for a teacher on the assumption that every configured entry can currently be learned; an unavailable panel is a reason to check that appendix.

### When a lesson is unavailable

Start with the simplest checks. Is the sample in the extra teaching slot? Is it the finished output you actually want? Does this profession teach it? Do you have enough Emeralds in your inventory? Have you already learned it?

A sample is not a universal request to explain its ingredients. The current interface unlocks selected recipes; it does not give a separate spoken ingredient explanation. An unsupported item or missing recipe entry can therefore leave the lesson unavailable even if the item itself is perfectly usable.

Rejected, unaffordable, unsupported, and already-known lessons do not consume Emeralds. The sample is not a fee, so take it back when finished. If your inventory has no room, be ready to collect returned items around you rather than leaving a valuable sample behind.

You can continue experimenting with valid manual recipes, use the recipe atlas, or obtain instructions through another available route. Dedicated EMI and JEI support is unfinished, so do not assume an additional recipe viewer follows all of Retold's discovery rules.

## 4 Enchanting from the beginning

Enchanting adds magic to equipment. Retold keeps enchantments as working item properties, but changes how you identify and reproduce them. Instead of choosing a random table offer, you enter a short inscription made of **three Standard Galactic Alphabet glyphs**.

The alphabet is a discovery puzzle. This chapter explains the controls and learning routes without giving you solved words or translating the symbols. Compare inscriptions you encounter and keep your own notes. Repeated shapes can become useful clues as you collect more examples.

### What you need to begin

There are two useful stations: an **Anvil** for applying enchanted books, and an **Enchanting Table** for writing inscriptions. Both use experience levels, so keep some experience after mining and combat rather than spending everything immediately.

An Anvil uses three Iron Blocks and four additional Iron Ingots: **thirty-one Iron Ingots in total**. An Enchanting Table uses **one Book, two Diamonds, and four Obsidian**. The recipe diagrams appear in the crafting reference. A Book can be made from three Paper and one Leather; three Sugar Cane make three Paper.

The ordinary route to mining Obsidian uses a Diamond Pickaxe. Retold's unenchanted Diamond equipment is fragile, but it still works: you can collect the Obsidian needed for your first table before enchanting the pickaxe. Its low durability is a reason to save it for the tasks that need it.

For table use, obtain Lapis Lazuli and enough experience levels. **Bookshelves are not required and do not increase the table's power.** You do not need to build a ring of shelves or wait for random offers to refresh.

### Understanding an enchanted item

An unknown enchanted item still has working magic. You can equip it or use it normally even if you cannot read its enchantment name. Its tooltip shows the three-glyph inscription and, where applicable, an enchantment level.

Once you learn that spell, its readable name appears alongside the inscription. That knowledge belongs to your player. Another player can see the same item differently because they have learned different spells.

Finding a book, buying one, carrying an enchanted weapon, or reading its tooltip does not by itself teach the spell. Keep the distinction clear: **possessing working magic is different from learning how to write it**.

### Learning through an Anvil

An enchanted book contains magic you can transfer to compatible equipment. At an Anvil, put the equipment in the left input and the enchanted book in the right. Inspect the output and the ordinary Anvil cost, then take the completed result when you want to pay.

A spell becomes known when that completed operation actually adds it to the result or increases its level. Merely placing the inputs in the Anvil is not enough. If the book's spell is incompatible, or would leave the result unchanged, it does not count as a learned transfer.

A book with several enchantments is handled one spell at a time: each transferred or improved enchantment can become known, while incompatible or unchanged ones remain unknown. Combining ordinary enchanted equipment is still an Anvil operation, but the learning route described here depends on transferring enchantments from a book.

You learn the spell itself, rather than a separate word for each level. Learning a low-level version can therefore give you the inscription used for its higher legal levels later. You still need compatible equipment, a table-eligible spell, and the resources for the level you request.

### Starting with an empty Known spells list

An empty list is normal for a new player. It means you have not yet learned any eligible spells to display. You have two routes forward.

The first is to obtain an enchanted book and complete a compatible Anvil transfer. Afterwards, the learned spell can appear in the table's list if the table is allowed to write it.

The second is to experiment at the table. You can copy the three visible glyphs from an unknown enchanted item or book into your own notes, then enter the same shapes. You do not have to know the readable name before trying the inscription. Start with level I while learning the controls, and choose a suitable target; a plain Book is a useful option for a table-eligible spell.

A successful valid inscription teaches its spell. A copied word can still fail if that enchantment is unavailable at the table, incompatible with the target, conflicting with existing magic, or requested at an illegal level. The game does not translate those failures into hints about the alphabet.

### Writing at the table

Open the Enchanting Table and place your target item in its item slot. Put at least **three Lapis Lazuli** in the Lapis slot. A plain Book is also a valid target and becomes an Enchanted Book after a successful cast.

Enter three glyphs, either by clicking the onscreen glyph keyboard or using the physical A–Z keys. These keys select glyph shapes; they do not reveal the symbols' meanings. Each entry fills the next empty position. Once all three positions are filled, extra letters do not add a fourth symbol.

Choose a level with the Roman-numeral buttons, the number keys 1–5, or keypad 1–5. Check the displayed cost, then click **Write**, press **Enter**, or press **keypad Enter**. A successful cast applies the spell, records your knowledge, clears the inscription, and briefly highlights the target in green.

You can add further compatible enchantments to equipment in later casts. Each cast has its own cost. The selected level is the spell's requested final level, not a number of levels to add to its current strength.

### Editing and using the spell list

You do not need to close the table to correct an inscription:

| Control | Effect |
| --- | --- |
| Backspace | Remove the last glyph |
| Click an inscription position | Remove that glyph and every glyph after it |
| Clear | Empty all three inscription positions |
| Click a known spell | Fill its complete inscription |
| Left and right list arrows | Move between pages of known spells |

Known spells appear four per page. With no target inserted, the list can show your known table-eligible spells. Inserting an item filters out choices that do not fit it, conflict with its current enchantments, or are already at their maximum level.

A missing list entry does not necessarily mean you forgot the spell. Remove the target to check whether filtering explains its absence. Some learned treasure spells remain unavailable at the table entirely.

For known words, the controls respect the registered maximum level and disable excess choices. Manually entered unknown words keep generic controls, without a preview that tells you whether the word is correct. Choosing a word from the list also keeps the selected level within its limit.

### Paying for a cast

Every successful table cast costs **three Lapis Lazuli**, plus **five experience levels for each requested enchantment level**:

| Requested enchantment level | Lapis Lazuli | Experience levels |
| --- | --- | --- |
| I | 3 | 5 |
| II | 3 | 10 |
| III | 3 | 15 |
| IV | 3 | 20 |
| V | 3 | 25 |

These are player experience **levels**, not individual experience points. A level III cast costs fifteen levels even if it upgrades an existing lower-level copy of that enchantment. The Lapis cost stays three.

For example, with twenty experience levels and three Lapis, a successful level III cast spends the three Lapis and leaves you with five levels. Make sure the spell actually supports level III; the table cannot invent a higher maximum for a weaker spell.

An invalid, conflicting, unaffordable, or non-improving cast spends nothing. Failure leaves the inscription in place and gives a generic red highlight and low sound. Check your target, level, resources, and existing enchantments before assuming the word itself is wrong. A valid word that successfully applies a different spell from the one you intended still counts as a paid success.

### Compatibility and unavailable spells

Normal enchantment conflicts still matter. Fortune and Silk Touch cannot share the same pickaxe, and the main Protection alternatives conflict with one another on the same armor piece. Knowing both inscriptions does not remove those restrictions.

The table can write only enchantments allowed by its normal eligibility rules. Learning a treasure enchantment through an Anvil does not automatically make it available there. The reference distinguishes table-eligible spells from mapped spells that need another acquisition route; it does not expose their glyph solutions.

**Mending is excluded from new random loot and Librarian trades in this build.** Existing Mending equipment continues to work. Do not plan your first enchanting setup around buying a new Mending book, and do not mistake the missing offer for a lesson you need to unlock.

### Why this matters for Diamond

Unenchanted Diamond tools and Spears have a maximum durability of only **sixty-four**. Diamond armor also has reduced durability. Applying **any enchantment**, including a curse, restores the equipment's normal full Diamond durability while that enchantment remains.

Removing every enchantment makes the item fragile again. Its existing damage is preserved, so stripping a heavily used item can leave only one final use. Keep this in mind before using a Grindstone. Enchanting does not erase damage or turn that transition into a free repair.

Diamond Horse and Nautilus Armor follows the same fragile-until-enchanted principle. Wolf, Horse, and Nautilus Armor can receive the supported Chestplate-style enchantments, but normal compatibility and material behavior still apply. Retold's wider enchanting balance and acquisition changes remain in development; learning the current controls does not make every planned spell or equipment feature available.

## 5 Keeping animals and pets

A Retold farm needs a food supply, safe surroundings, and room for animals to reach what they eat. Putting two cows behind a fence is only the beginning. They can graze the enclosure bare, become hungry, search for another feeding range, and eventually try to escape. A named pet also needs care: a name tag and taming do not prevent starvation while the creature is active in the world.

Many creatures have Retold hunger, including livestock, ordinary predators, fish, Bees, Bats, and Villagers. Some do not. Polar Bears, Skeletons, most guards, and several specialist monsters have no Retold hunger cycle. A creature does not necessarily need feeding merely because another member of its faction does.

### Four ways to provide a meal

**Direct feeding** means using an item on the animal. Its ordinary species-specific interaction still matters: the expanded Retold diet does not make every listed food a valid right-click item. For supported adults, normal breeding food relieves hunger instead of instantly producing love mode. Taming, healing, growing a baby, and other special vanilla interactions can still have their own conditions.

**Dropped food** means throwing a compatible item on the ground. Hungry creatures with that diet can approach and consume it. This lets a species eat more foods than its familiar vanilla breeding item. A cow can eat a dropped Carrot, for example, even though Wheat remains its familiar direct interaction. Nearby predators or Slimes may also notice loose food, so dropping a stack is not a private delivery to one chosen animal.

**An Animal Feeder** stores food in a wooden trough. It supplies suitable animals that can physically reach it. It is useful for a maintained enclosure because food stays in a container until a meal is taken.

**Foraging** means the animal obtains food from its surroundings. Some meals remove real plants or crops; others represent browsing or digging without destroying the habitat. A Mooshroom can graze Mycelium without removing it, whereas a Panda eats the Bamboo block it reaches. Do not assume all natural feeding leaves the landscape unchanged.

After a meal, many creatures briefly stop and face the food source while eating. Danger takes priority: an animal being chased or attacked may abandon its feeding attempt. Food somewhere in the pen is not enough if the animal cannot reach it or cannot stop fleeing long enough to eat.

### What to feed each creature

The table describes the current default diets. The dropped-food column also describes what a suitable feeder can offer where that species uses feeders. It does not expand the animal's direct right-click controls. Datapacks can change these defaults.

| Creature | Food you can supply | Food or feeding behavior in its habitat |
| --- | --- | --- |
| Cows, Mooshrooms, Sheep, Goats, Horses, Donkeys, Mules, Llamas, Trader Llamas, Camels | Wheat, Hay Bales, Apples, Carrots, Potatoes, Beetroot, grasses, and ferns | Grazing plants, flowers, and crops. Mooshrooms also browse Mycelium; Goats scrape Stone, Snow Blocks, Packed Ice, and Gravel; Camels browse Dead Bushes. Those special habitat meals do not remove the source. |
| Pigs, Chickens, Rabbits | Wheat Seeds, Beetroot Seeds, Melon Seeds, and Pumpkin Seeds; Carrots, Potatoes, Beetroot, and Dandelions | Grasses, flowers, and crops. Rabbits can also browse Dead Bushes without removing them. These three species share a broader diet than their usual vanilla breeding foods suggest. |
| Wolves | Raw or cooked meat and fish, including Rotten Flesh | Hunt Sheep, Rabbits, Chickens, and fish when hungry. Tamed Wolves do not start independent prey hunts. |
| Foxes | Meat, fish, Sweet Berries, and Glow Berries | Hunt Rabbits, Chickens, and fish; can carry food toward their den. |
| Cats and Ocelots | Meat, fish, and Phantom Membranes | Can hunt Rabbits, Chickens, Frogs, fish, and Phantoms. Tamed Cats stop ordinary independent prey hunts. |
| Dolphins | Meat and fish as compatible dropped food | Hunt fish and share hunting activity with their pod. They do not use land-animal feeders. |
| Spiders and Cave Spiders | Meat and fish as dropped food | Hunt suitable animals, fish, and Bats at night. They are not feeder livestock. |
| Armadillos | Spider Eyes and their ordinary species food | Dig for abstract grubs in suitable exposed ground, including dirt-like soil, Red Sand, Terracotta, and Mud, without removing it. |
| Turtles | Seagrass | Feed on suitable Seagrass and remember beach and water locations. |
| Pandas | Bamboo | Approach and consume planted Bamboo; the reached block disappears without producing a dropped item. |
| Parrots | Their ordinary seed foods, plus Wheat, Carrots, Potatoes, Beetroot, and Melon Slices as dropped food | Can forage planted crops. |
| Bees | Flowers | Flower visits provide meals without destroying the flower. |
| Cod, Salmon, Tropical Fish, Pufferfish | Maintain suitable aquatic plants | Graze Seagrass, Tall Seagrass, Kelp, and Kelp Plants. They do not share the Squid diet of dropped raw fish. |
| Squid and Glow Squid | Dropped raw Cod, Salmon, Tropical Fish, or Pufferfish | Do not hunt living fish. They do not use feeders. |
| Nautiluses | Fish as dropped food | Wild hungry Nautiluses hunt living fish; tamed ones stop autonomous hunting. |
| Frogs | Their normal direct feeding interaction remains relevant | Hunt suitable small arthropods and small Slimes or Magma Cubes. Do not rely on a feeder or generic dropped-food diet for them. |
| Axolotls | Their normal direct feeding interaction remains relevant | Hunt suitable fish, Squid, Glow Squid, and Drowned. Guardians are a defensive fight rather than ordinary food. They do not use feeders. |
| Bats | Dropped Spider Eyes | Hunt arthropods at night and can catch abstract cave insects. They do not use feeders. |
| Piglins, Hoglins, Striders | Their expanded dropped diet includes meat, Crimson and Warped Fungus, and red and brown mushrooms | Hoglins forage Crimson Fungus; Piglins can forage Crimson Fungus and mushrooms. Striders can forage Warped Fungus, and lava sustains them without being consumed. |
| Slimes and Magma Cubes | Every kind of dropped item | Swallow items even when completely fed. Living-prey combat is governed separately by hunger. |
| Villagers | Bread, Carrots, Potatoes, and Beetroot in their own inventory or accessible village Chests and Barrels | Farmers can supply communal surplus. Villagers do not eat from Animal Feeders. |

Existing or Creative-created Sniffers obtain food through their special ground-foraging behavior. Normal survival acquisition of Sniffers is disabled, so they are not part of the ordinary new-world farming route.

### Using an Animal Feeder

Craft the trough from five Planks, with Planks in the two upper corners and across the bottom row. Place it where animals can stand beside it; a food container behind an obstacle is not a working feeding station.

| Interaction | Result |
| --- | --- |
| Right-click with suitable food | Insert one item |
| Sneak-right-click with suitable food | Insert as much of the held stack as fits |
| Sneak-right-click with an empty hand or an unsuitable item | Retrieve the stored food |

The feeder holds one stack and animals consume one item per meal. It saves its contents normally. Its inventory and comparator behavior also allow ordinary storage arrangements.

The trough accepts several food categories because different animals use it. Acceptance does not mean every nearby animal can eat its contents. A trough containing meat can be useful to a Wolf and useless to the cows beside it. Use the diet table and provide an accessible approach, then watch for an actual meal.

Aquatic groups, Villagers, hostile monsters, Slimes, and Magma Cubes do not use the trough. Some specialist animals also need habitat or prey rather than a compatible stored item. One feeder is not a universal solution for an entire mixed collection.

### Breeding through sustained care

Retold replaces immediate item-triggered breeding with sustained satisfaction for supported animals. Both adults must stay well fed, without panic, damage, or an active combat target, for **five minutes** at normal game speed. Ready compatible adults within eight blocks can then begin their ordinary mating behavior.

Feeding two hungry cows once does not guarantee that they will breed five minutes later. They need to remain well fed throughout the wait. If they eat their grass, become hungry again, or keep fleeing a nearby predator, their preparation can reset. After a birth, both parents become hungrier and retain the normal age cooldown before they can breed again.

Vanilla still handles genetics, tame ownership, Horse and Donkey crosses, Turtle eggs, Frog pregnancy, and other special birth behavior. Babies do not immediately become breeders. They also are not exempt from the needs of their species: keep their habitat supplied rather than treating a newborn as safe because it is young.

There is no separate population-count cap on this reproduction. A well-supported group can grow, so allow for the extra food and space its offspring will need. A local herd splitting into several social groups does not mean reproduction has been capped.

### Keeping an enclosure working

Imagine a small cow pen whose floor is mostly grass. At first, grazing supports the herd. As usable food disappears, the cows become hungry and may seek another range. A feeder with reachable Wheat can keep that place worth staying in. Adding more fence without adding food does not solve the underlying problem.

Desperate eligible ground animals can break wooden fences and closed wooden fence gates. Actively hunting wild predators can also break these barriers to reach prey. You can see cracking before the block breaks, and the broken block drops normally. Doors, trapdoors, walls, iron barriers, and open gates are outside the default weak-barrier set. Tamed predators, leashed animals, and passengers do not use this particular breach behavior.

If cows will not settle or breed, check the food itself, the approach to it, and nearby danger before assuming the breeding system failed. If a feeder is ignored, try a simple supported position beside the animals and one known compatible food. If plants are not being consumed on a server, check its griefing rules: disabling mob terrain changes prevents destructive forage and fence breaking, while dropped food and feeder meals can still work.

## 6 Understanding the living ecosystem

Retold creatures remember places, respond to each other, and have reasons to interrupt what they were doing. A Wolf stopping a hunt, a Chicken returning to a roost, or fish leaving a depleted plant patch can be ordinary behavior rather than a broken target or lost path.

### Homes and feeding ranges

A home is usually a remembered location. Wolves and Foxes can have den locations, Chickens roosts, Rabbits warrens, and aquatic creatures feeding or home ranges. These names do not mean the animal automatically builds a new den building or excavates a Rabbit tunnel. Spider lairs are a separate exception because their webs are real placed blocks.

Herds and foraging groups organize around local ranges. Cows and Mooshrooms can share a bovine group. Horses, Donkeys, and Mules can mix, as can Llamas and Trader Llamas. Many other land groups are species-specific. Cod, Salmon, Tropical Fish, and Pufferfish form separate schools of their own species, even though fish can share panic more broadly.

Usable food helps hold a range in place. When hunger rises and nearby food is depleted or inaccessible, a group may migrate toward a better area. There is no permanent enclosure flag that makes player-placed animals forget these needs. Maintaining food where you keep them is what makes that location attractive.

### Day, night, weather, and rest

Species have different daily rhythms. Grazers and Pigs tend to rest at night. Chickens return to roost during their inactive periods and in rain. Foxes and Wolves are more active around dusk, night, and dawn and may rest by day. Cats and Ocelots have their own dusk and night activity, including late-night rest; Dolphins also have a late-night resting period.

Watch the animal's situation before interpreting a pause. A Cat can lie down, a Fox sleep, a Wolf sit, or a Camel settle near its home. Food needs and danger can interrupt these routines. A hungry animal may search when its satisfied neighbor is resting, and a frightened Rabbit may return to its warren after a chase.

Social safety also affects behavior. Creatures can recover from danger near a familiar home or compatible group. Isolation can make social animals more nervous, while meals and successful hunts can improve their willingness to act. These are background states, not normal Survival bars that you must constantly monitor.

### Predators do not know everything

Ordinary food hunting follows prey diets. A hungry Wolf does not automatically select a player as its next meal. Wolves, Foxes, Cats, Ocelots, Dolphins, and Spiders seek different kinds of prey, and retaliation is separate from feeding. A predator that attacks after being hurt is defending itself, even if it was not hungry beforehand.

Predators use sight, nearby audible activity, and short-range scent. A distant scent clue can guide a search without revealing a perfect live target through every wall. Losing sight of prey can lead to searching around its last known location rather than following its current position without limit.

Long chases can tire ordinary predators. A hunter that stays far from its prey or repeatedly loses sight can slow down, abandon the pursuit, and take a break before hunting again. A blocked or impossible route can also make it give up or reconsider. Do not assume that a predator stopping means it has become friendly; the next encounter may have different conditions.

Real damage matters too. A successful hit from a living attacker that leaves an ordinary wild predator below one quarter of its health can make it abandon its ordinary hunt or retaliation and flee for ten seconds. Tamed defenders, Undead, bosses, and active territory duties have different priorities. This is not a universal surrender rule for every enemy.

### Danger can spread through a group

Prey can flee a nearby hungry predator before the first bite. A successful damaging hit also causes passive creatures to panic, and nearby companions may react. Some creatures can respond when they see a mob genuinely targeting them, before it lands its attack. They do not read a player's crosshair or decide that merely holding a sword proves hostile intent.

Social defenders can help an injured ally, but help depends on species, faction, ownership, awareness, and whether the helper is already busy. Standing tame Wolves of the same owner can cooperate; a sitting pet does not join ordinary owner defense. Dolphins can defend an injured podmate even when not hungry. A creature with another urgent task may not join the same fight.

Creepers have special safety rules. Mobs do not deliberately select or directly melee them. Cats hiss and retreat, while Creepers retain their avoidance of Cats. Many mobile non-Undead creatures flee a lit fuse, including defenders that would otherwise stand and fight. Undead and other Creepers do not use that escape response.

### What happens when you leave an area

Unloaded creatures are not continuously acting out every moment somewhere beyond view. When you return, Retold can reconcile up to **seven Minecraft days** of missed ecosystem time. This uses time that advanced in the world, not the number of real days a stopped save sat on your disk. Closing a single-player world for a week does not itself create a week of simulated hunger.

Reconciliation changes real things. Food can leave a feeder or village store, plants can be consumed, and a suitable reachable wild prey animal can disappear without dropping loot or experience. Breeding preparation can advance across periods that remained well fed. A foodless herd, Pig group, or fish school can relocate together when a safe reachable destination is available. This process does not break barriers to invent an escape route.

Named mobs and tamed animals, including tamed horses, have protection from death during this hunger catch-up and stop at one health. **Untamed unnamed horses are not automatically protected.** Catch-up predation also protects named or tamed prey and animals being ridden or carrying passengers. These protections do not make pets safe from ordinary starvation or attacks while their area is loaded.

Some Farmer crop work and ordinary spawning can also be reconciled under limited conditions. This is not general offline crop growth or a complete offscreen village economy. On returning, inspect actual food and animal health rather than assuming an area was perfectly frozen or fully simulated in your absence.

## 7 Creature field guide

The entries below describe Retold's distinct behavior families. Ordinary Minecraft actions can remain alongside them: a special diet does not replace every taming, riding, or interaction rule. Custom boss encounters and their progression rewards are explained in the guardian and dimension chapters.

### Farm animals and smaller wildlife

**Cows and Mooshrooms** share herds, feeding ranges, and group panic. Mooshrooms have renewable Mycelium grazing in addition to the broader grazer diet. **Sheep and Goats** also graze and group; Goats can obtain habitat meals by scraping suitable rock, snow, ice, or gravel. **Horses, Donkeys, Mules, Llamas, Trader Llamas, and Camels** use grazer life behavior, with compatible mixed groups where described earlier. Camels have Dead Bush browsing, and a Trader Llama linked to a living Wandering Trader can receive caravan sustenance.

**Pigs, Chickens, and Rabbits** forage rather than behaving like identical miniature grazers. Pigs use feeding ranges, Chickens return to roost, and Rabbits can hide at remembered warrens after panic. Rabbits also have Dead Bush browsing. Their expanded dropped-food diet is useful when maintaining a mixed small-animal area.

**Wolves** organize pack hunts, search together, regroup, and return toward their den. Hungry members matter when choosing who starts or continues a hunt, and a satisfied leader can give way to a hungry member. Den defense specifically includes Skeleton-family enemies such as Skeletons, Strays, Bogged, and Wither Skeletons. A remembered den does not make every passing player a territorial enemy.

**Foxes** are opportunistic solo hunters with den memory. A Fox carrying edible food may take it home rather than immediately eating where it found it. **Cats and Ocelots** also have individual territories and hunger-driven prey hunting. Cats hiss and withdraw around Creepers; do not interpret that retreat as a plan to kill the Creeper.

**Polar Bears** have no Retold hunger cycle. Adults protecting cubs can stand, face an intruder, and give a short warning before attacking. Leaving promptly can avoid escalation; harming them is a different situation and can provoke immediate defense. Nearby adults may help.

**Armadillos** distinguish ordinary walking from visible sprinting nearby. Sprinting and genuine danger can trigger defense or retreat; hungry ones can perform a grub-dig meal in suitable ground. **Turtles** remember beaches and can flee toward water. In the current build, any visible Survival player coming within roughly five and a half blocks can frighten a Turtle, even without sprinting. Approach with space rather than relying on peaceful walking to keep it still.

**Pandas** remember Bamboo groves, approach Bamboo to eat, and can seek another stalk if one is unreachable. They remove the reached block without drops. **Parrots** forage seeds and crops. Tame Parrots, including shoulder riders, can warn an owner of a recent attacker or a nearby mob actively targeting the owner. The cue is a low-pitched hurt-like call with angry particles; the Parrot is warning, not becoming a combat pet.

**Bees** feed through flowers and are generally peaceful around their work. Actual damage, breaking their hive, or harvesting a full hive with Shears or a Bottle without smoke can bring colony defense. Smoke matters, and nearby Bees already occupied with something else are not guaranteed to join every response.

### Rivers, oceans, and caves

**Cod, Salmon, Tropical Fish, and Pufferfish** form exact-species schools around feeding ranges. Keep suitable aquatic plants available if maintaining them. Hunger and depletion can move a school, and danger can panic fish beyond a single species. **Squid and Glow Squid** instead use separate same-species danger groups and eat dropped raw fish; they are not living-fish hunters.

**Dolphins** hunt fish in pods and can defend an injured podmate. Hunger drives food hunts, but a real defensive incident does not require an empty stomach. **Nautiluses** hunt fish while wild and hungry; tamed ones stop autonomous hunting. Capturing a fish or Axolotl in a bucket does not by itself turn it into a tame pet.

**Frogs** use wetland homes and hunt suitable small prey, including small arthropods and appropriately small Slimes or Magma Cubes. Both ordinary tongue kills and Retold feeding attacks can contribute meals. **Axolotls** have water-range behavior and suitable aquatic prey. They do not routinely hunt ordinary Guardians for food; a Guardian attack can instead cause retaliation or genuine assistance.

**Bats** are active members of the cave ecosystem. They use dark supported ceiling roosts, return during the day, and seek individual hanging places. At night they can form small hunting parties, seek Spider Eyes, hunt arthropods, or catch abstract cave insects. Disturbing a roost causes panic and a delay before settling again, rather than an immediate return to the same ceiling spot.

### Spiders, swarms, and Cubes

Hungry **Spiders and Cave Spiders** can hunt animals and Bats at night. Daylight ends ordinary food hunting but does not prevent retaliation. Their familiar player aggression still depends on darkness. Recently fed Spiders can establish shared webbed lairs in genuinely dark sheltered places, add or repair persistent webs, and return by day. An exposed field becoming dark at night is not enough shelter. Web construction obeys the server's mob terrain-change rules.

**Silverfish and Endermites** have separate identities and coordinate with their own species. They do not act as one interchangeable swarm. Normal survival Endermite spawning, including the Ender Pearl chance, is disabled, while existing, Creative, or command-created specimens still function.

**Slimes and Magma Cubes** are dangerous when hungry enough and stop ordinary prey combat when fed. Even a hungry size-one member can damage a valid target. They tolerate fellow Cube creatures for ordinary targeting and do not deliberately hunt Creepers.

Their appetite for items is separate: **even a fully fed Slime or Magma Cube can swallow every kind of dropped item**. It stores the exact stack and releases stored items on death. Feeding items also lets it grow through increasingly expensive sizes, up to size ten. Compatible same-species small natural sizes can merge, but size ten is currently a large ordinary creature, not a special boss transformation.

At maximum hunger, larger Cubes split into smaller hungry children; size-one Cubes die. Normal splitting also passes part of the parent's hunger to its children. Keeping one fed can reduce living-prey aggression, but it does not make loose valuables safe beside it.

### Undead and specialist enemies

**Zombies, Husks, Drowned, Zombie Villagers, and Zombified Piglins** use hunger and local horde behavior. They can obtain meals from living victims. **Skeletons, Strays, and Bogged** do not have this hunger loop; they maintain ranged spacing, avoid firing through an Undead ally, and try to find a clearer angle.

Stage 2 expands Undead awareness and cooperation and removes sunlight protection as a reliable player tactic. The change does not simply add health or damage. Combat fire still matters where the creature is vulnerable. Stage 3 cleanses Retold Undead across dimensions, including **Ghasts, Zoglins, and wild Zombie Nautiluses**. A name tag does not protect a wild Undead creature from stage cleansing.

**Ghasts** retain long-range artillery behavior with Undead relationships. **Zoglins** retain aggressive rampage behavior while tolerating other Undead. **Zombie Nautiluses** keep their specialist vanilla role, with taming changing their generic Undead relationship.

Wild **Skeleton Horses, Zombie Horses, and Camel Husks** can be hostile. Claimed mounts defend themselves and their owner without independent prey hunting. Mounting an already-tame, ownerless, riderless Skeleton Horse or Camel Husk can claim it; Zombie Horses retain their bucking and taming process. Ownership is important because claimed mounts no longer have the same generic Undead identity as wild ones.

**Phantoms** are rare solitary open-sky threats at night or in suitable dark storm conditions. They are independent of insomnia, so sleeping is not a complete counter. They commit to a damaging swoop, then recover before stalking again. **Withers** retain their flight, heads, healing, and destruction but recognize Retold Undead relationships and choose serious living threats rather than always favoring a player.

### Guards and encounter creatures

**Guardians and Elder Guardians** immediately defend against suitable living intruders; they do not use the same gradual territorial warning as Illagers. Guardian beams and monument pressure can interfere with mining. The Elder is invulnerable while in water, vulnerable when exposed, and suffers drying damage. Its Water progression role requires solving that encounter rather than winning an ordinary underwater damage race.

**Blazes, Piglin Brutes, Wither Skeletons, Shulkers, Iron Golems, and Snow Golems** have guard-purpose behavior, including returning toward posts and limiting pursuit. A guard returning home is not necessarily forgetting its faction. **Piglins and Hoglins** also have food-related behavior, while **Striders** receive passive sustenance from lava and keep Warped Fungus as a food option.

**Pillagers, Vindicators, Ravagers, Vexes, and Illusioners** have raider or support roles. Idle raiders can roam, while structures, raids, danger, and nearby village activity can take priority. **Evokers** and appropriately allied **Witches** can support from behind other combatants. Vex direct strikes deal half their otherwise calculated damage.

**Creepers, Endermen, Breezes, Creakings, the Ender Dragon, and the Warden** retain much of their specialist vanilla identity. Endermen ignore gaze after Stage 1 but still retaliate, with Stage 3 adding nearby group defense. The **Gale Core, Wildfire, Earth Guardian, and Aender Eye** have their own encounter or dimension roles rather than ordinary farm-animal life.

### Supported does not always mean obtainable

Normal survival acquisition of Sniffers and Endermites is disabled. Wardens and their usual survival route are removed, and normal End City progression does not supply Shulkers. Existing or Creative-created specimens can retain working AI. Illusioner behavior support likewise does not establish a new ordinary survival spawn route.

Do not plan around future mob concepts as if they were installed encounters. Natural Killer Bunny reintroduction, Iceologers, the music-disc monster, the old display-statue Tuff Golem concept, and a special size-ten Slime boss remain unfinished or undecided.

## 8 Factions and territory

Faction relationships explain why two monsters may ignore one another, defend each other, or fight even when the player is nearby. A faction does not give every member the same diet, awareness, or combat style. Ordinary prey hunting, retaliation, and defending a structure are also different reasons for selecting a target.

### The relationships you will notice

| Group | Practical relationship |
| --- | --- |
| Undead | Generally tolerate other Undead and attack suitable living outsiders, excluding Creepers. Retold includes Ghasts, Zoglins, and wild Zombie Nautiluses in this family. |
| Nether Remnants | Piglins, Brutes, Blazes, and Wildfires share an alignment and oppose Undead and other threats. |
| Illagers | Structure defenders and raiders; Witches are loose allies whose combat cooperation depends on a shared active raid. |
| Village Defenders | Iron and Snow Golems protect against danger; idle Endermen and Nether Remnants are not automatic enemies. |
| Enders | Endermen and Shulkers have their own relationship family. Enderman retaliation and Stage 3 group defense are especially relevant to players. |
| Guardians | Tolerate other Guardians and immediately oppose suitable living outsiders. |
| Cubes and smaller groups | Slimes and Magma Cubes tolerate their family; Spider families, Silverfish, Endermites, and other groups have their own narrower relationships. |

The player is not the universal first choice in every mixed fight. An Undead creature may select nearby living prey, and an enemy actively harming a defender's ally may become more urgent than another bystander. Conversely, watching two factions fight does not grant safe passage through their occupied structure.

### Entering an occupied structure

Four structure settings use the main territorial warning system: **Bastions, Nether Fortresses, Pillager Outposts, and Woodland Mansions**. Remnant territory applies in the Nether. A physically present defender must notice the intruder; an empty abandoned building does not warn by itself.

A guard can observe you, issue a warning, move into a threatening position, give a final warning, and eventually attack. Look for angry particles, repeated hostile sounds, and deliberate attention or repositioning. A ranged defender may raise or charge its weapon during the final warning. These are reasons to leave, even before it fires.

Consider an Outpost approach. If a Pillager notices you and starts warning, back away and give it space. Walking closer, lingering nearby, opening a container, or breaking blocks can add suspicion. The absence of an immediate arrow does not mean the Pillager has accepted you. At a Fortress, a Blaze can participate in the equivalent Remnant warning process.

Territorial container rules are strict: witnessed opening of a listed container can count as stealing even before you move an item. This differs from village property, where actual protected withdrawals matter. Treat occupied Bastion or Mansion containers as an action against their defenders, not a harmless inventory preview.

### Escalation and leaving

Suspicion is remembered for you in that territory. Leaving the doorway or briefly moving out of sight does not instantly erase it. Once guards have lost sight, suspicion can gradually decay; repeated trespass or serious offenses can make a quick return much more dangerous.

There is no universal safe countdown that lets you take a few actions before combat. Earlier suspicion, close approach, property interference, and direct violence change the situation. Attacking a defender can trigger immediate retaliation, and nearby allies may help. Ordinary faction assistance is not supposed to turn a peaceful warning into an instant attack merely because another guard noticed you.

If withdrawal seems ineffective, move farther away and stop interacting with the structure or its occupants. A defender may retain a genuine attack target for a while rather than immediately returning to a warning posture. Creative or Spectator behavior is not a useful rehearsal for how the same encounter treats Survival players.

### Stage changes and special encounters

Living Piglin and Brute natural or structure spawning is blocked before Stage 3. Blazes still make Nether structures relevant earlier, and Stage 2 introduces roaming Wildfire encounters. Do not assume every Remnant encounter offers the same opportunity to negotiate: a roaming boss and its escorts have their own combat rules.

New Illager patrols and raids require Stage 3. Before then, Bad Omen does not proceed into a new raid. This start rule does not cancel a raid already in progress. During an active raid, ordinary structure-warning behavior gives way to raid activity.

Gold armor, better armor, and a good reputation are **not implemented guarantees of admission**. Reputation-gated Nether bartering, hired Brutes, Piglin followers, and employer contracts remain future work. For the current build, respond to the guards you actually encounter rather than assuming an equipment-based safe-passage rule.

## 9 Living with Villagers

Villagers are hungry, peaceful residents with access to local supplies. A village is therefore more than a set of trading menus and freely available chests. Sharing food can support it, while taking its property can damage your standing with witnesses.

### Communal food and useful storage

Villagers eat carried Bread, Carrots, Potatoes, or Beetroot first. When personal food runs out, a hungry Villager can walk to an accessible village Chest or Barrel, withdraw real food, eat one item, and carry some for later. They remember discovered stores and share that knowledge locally, but a remembered chest still needs to exist, contain food, and remain accessible.

Player placement does not make a Chest private. If you put food storage where Villagers can use it in their village, they may take supplies from it. Animal Feeders and machine inventories are outside this communal food system. A clearly reachable Chest or Barrel is the straightforward way to provide food.

Farmers keep a personal reserve and deposit surplus crops or Bread. This can replenish communal supplies, but it is not unlimited production: access, actual crops, danger, daily routines, and room in storage matter. If Villagers seem hungry despite nearby food, check that it is one of their foods, in an appropriate container, and reachable from a supported adjacent position.

### Livestock care

Some professions use real communal supplies to tend hungry adult livestock:

| Profession | Animals | Feed used for tending |
| --- | --- | --- |
| Shepherd | Sheep and Goats | Wheat |
| Leatherworker | Cows and Mooshrooms | Wheat |
| Butcher | Pigs and Rabbits | Carrots |
| Butcher | Chickens | Wheat Seeds |

Tending relieves hunger rather than instantly causing a birth. The same five-minute satisfaction and safety conditions still control breeding. A Butcher near two Pigs does not bypass those rules, and missing or unreachable communal feed can prevent the task.

Animals actually tended by the village can become village-owned. Two village-owned parents can pass that ownership to offspring. Player-associated unowned livestock has protection against being newly claimed, and that protection can pass through a parent to its offspring. Simply moving an animal inside village bounds is not sufficient to claim it.

Interacting with an already village-owned animal does not turn it into your property. Keep this distinction in mind before killing livestock beside a village: apparent proximity is not the only ownership information the world remembers.

### Taking supplies and damaging property

Generated village supplies and items produced or deposited by Villagers can have village ownership. Player additions remain unowned, even when they merge into the same stack. When contents are withdrawn, the unowned portion is used first. This lets you retrieve remaining player contributions without treating every item in the chest as community property.

For example, depositing Wheat into a village store does not mark your added quantity as stolen goods. Villagers can still consume or use it. If you later withdraw everything, your remaining contribution can be removed first, but the rest may belong to the village. The chest's appearance does not show an ordinary Survival ownership counter.

Farmer-planted or replanted crops also carry ownership through growth. Taking a mature owned crop is minor theft; destroying an immature owned crop or trampling its farmland is stronger vandalism. Player planting has its own provenance rather than automatically becoming village-owned merely because of location. Ambiguous older-world property is not universally claimed after installing an update.

Witnesses matter. A nearby village-context Villager who can see a Survival player commit an offense can react with unhappy or angry feedback and a disapproving sound. Theft, vandalism, breaking protected storage, or directly killing owned livestock can damage vanilla reputation, affect trade relationships, and eventually lead to Iron Golem hostility. Monsters and environmental deaths are not treated as the player committing the same offense.

The operator command `/retold village status` can inspect nearby loaded standing and possible Golem hostility. Ordinary Survival does not provide a complete reputation meter over every Villager. Respond to their feedback and keep your own supplies distinct if you do not want them used communally.

### Trades and daily stock

Professional adult Villagers refresh the stock of their existing offers when the Overworld day advances. This restores availability rather than rerolling the whole trade list. Wandering Traders do not use this daily refresh rule.

Recipe teaching is a separate service described in the crafting chapter. Neither a profession nor a refreshed inventory means every planned village economy feature has been completed. Villagers remain pacifists; their food, work, property, construction, and maintenance systems provide the current village loop.

### Iron Golems and village lighting

Village and structure Iron Golem spawning is blocked in Stage 1. From Stage 2 onward, an otherwise eligible village uses staged construction instead of an instant Golem appearance. Normal local conditions still matter, including the agreement of five Villagers and whether a recent Golem is already nearby.

Clerics, Librarians, Armorers, Toolsmiths, and Weaponsmiths can build. The builder needs a suitable reachable work site and one real Emerald available through Villagers or accessible village storage. It supplies the frame magically, places the Iron Blocks and Pumpkin in stages, then uses the Emerald to animate it. The visible frame is protected from player breaking while construction is active.

Hunger, danger, sleep, trading, and disabled mob terrain changes can pause or prevent work. If no Golem appears, first check village eligibility, builder profession, accessible Emerald supplies, and the work site. Construction is not a promise that every five Villagers immediately produce a defender on demand.

Building your own Iron Golem uses a different payment. In Survival, completing a valid frame requires at least **five experience levels**, which are spent when animation succeeds. Creative is free. Failed frames, Snow Golems, and Copper Golems are outside that charge. A Golem built by Villagers is their construction, rather than your player-summoning advancement.

Adult Villagers also maintain eligible dry extinguished village torches in every stage. Most use a brief magical cast; Nitwits physically approach and show a temporary Flint and Steel. They can interrupt this work for more urgent needs. Supply cover against weather and keep routes open rather than assuming a Villager can continuously relight exposed torches during ongoing precipitation.

## 10 The three world stages

Retold divides a world's progression into three **stages**. A stage is a saved condition shared by everyone on the server, across all dimensions. It is not a character level. If one player defeats the Ender Dragon or completes the last ritual offering, the resulting changes affect the whole world.

Stage 1 begins when the world starts. Stage 2 begins after the Ender Dragon has been defeated. Stage 3 begins when the Dragon Egg has accepted all six required sacrifices. The dragon is therefore a turning point rather than the end of Retold's survival progression.

Your recipe knowledge and enchantment knowledge remain personal. Joining an advanced world does not automatically teach you everything its other players have learned.

### What changes at each stage

| World rule | Stage 1 | Stage 2 | Stage 3 |
| --- | --- | --- | --- |
| Your main direction | Establish equipment, reach the Nether and End, defeat the dragon | Gather and sacrifice six artifacts | Explore the Aender and the changed world |
| Undead and sunlight | Ordinary sunlight vulnerability remains where applicable | Sunlight no longer burns them or drives them into shelter | Retold Undead are cleansed and their spawning is blocked |
| Undead cooperation | Shorter-range coordination | Wider coordination and increased pressure from selected natural-spawn entries | Cleansing replaces this threat |
| Enderman eye contact | Can provoke an attack | No longer provokes an attack | Still ignored; nearby Endermen can defend an attacked member |
| Living Piglins and Piglin Brutes | Normal natural and structure spawning blocked | Still blocked | Enabled; protected from zombification |
| Air Temples and Woodland Mansions | Their placement is delayed | Become available | Remain available |
| Pillager Outposts | Delayed | Still delayed in this development build | Become available |
| Wildfires | No natural Retold encounters | Roaming Nether encounters begin | Encounters remain possible |
| Earth Guardian | Indestructible dormant statue | Can awaken when approached | An undefeated guardian can still awaken |
| Pillager patrols and new raids | Blocked | Blocked | Enabled |
| Overworld End portals | Lead to the End | Still lead to the End | Lead to the Aender |

The Outpost row describes current behavior. Earlier design notes put Outposts in Stage 2, so this build's timing differs from that documented intent.

Stage 2 makes daylight less dependable. Zombies and Skeletons can remain active outside, cooperate over greater distances, and converge on trouble. The spawning adjustment increases the relative weight of selected existing Undead entries by about a quarter. It does not raise the monster cap or guarantee a quarter more monsters, and it adds no automatic health or damage bonus. Ordinary combat fire and lava still hurt creatures that are vulnerable to them.

Stage 3 cleansing uses Retold's Undead classification, which includes creatures such as Ghasts and Zoglins as well as familiar Zombies and Skeletons. Claimed mounts have separate ownership rules. Creepers, Spiders, Illagers, hungry predators, and other threats remain, so completing the ritual does not make the Overworld peaceful.

### Reaching the first dragon

The route to the first Ender Dragon still uses a **stronghold**, an underground structure containing an End portal. Craft Eyes of Ender from Ender Pearls and Blaze Powder. Endermen provide Pearls; Blazes in Nether Fortresses provide Rods that craft into Blaze Powder. A thrown Eye travels toward a stronghold; recover it when possible and follow repeated throws. An Eye can break, so bring more than the number needed for the portal.

Find the stronghold's portal room and put Eyes into its empty portal-frame sockets. There are twelve sockets altogether, although some may already contain Eyes. Filling the last empty socket opens the portal. There is no additional Retold requirement to defeat the Wither before this first End visit.

Recommended preparations are dependable weapons, armor, food, blocks, a ranged weapon, and a valid Overworld respawn point. Beds are dangerous in the End. End Crystals on the Obsidian towers heal the dragon; destroying those crystals removes that support, but their explosions can hurt you. Fight the dragon while avoiding its attacks and the island's edges. Retold restricts the later outer-End exploration route, described in Chapter 13.

Defeating the dragon advances the shared world to Stage 2. The transition has sound feedback. Collect the Dragon Egg for the next stage of progression and return to prepare for several different expeditions. When Stage 3 eventually begins, players still in the vanilla End are sent to their respawn destination. Administrative commands can still reach the old End, preserving access to existing builds.

## 11 Completing the Dragon Egg ritual

The ritual requires six **artifacts**: special items gathered through different challenges. Four represent Air, Water, Fire, and Earth. Life and Death are separate forces, represented by a Totem of Undying and Nether Star.

You need one accepted offering for each force across the whole world. You do not need six artifacts per player, and you do not have to gather them in a particular order. Different players can complete different expeditions and contribute to the same saved progress.

| Force | Artifact to sacrifice | Where to obtain it |
| --- | --- | --- |
| Water | Heart of the Sea | Defeat an Elder Guardian at an Ocean Monument |
| Air | Heavy Core | Defeat the Gale Core at an Air Temple |
| Fire | Nether Reactor Core | Defeat a Wildfire in the Nether |
| Earth | Lodestone | Defeat the Earth Guardian beneath a newly generated Jungle Pyramid |
| Life | Totem of Undying | Defeat an Evoker; a Woodland Mansion provides a route before raids unlock |
| Death | Nether Star | Defeat the Wither |

### Collecting the egg

The egg is a block, not an item you can simply mine. A normal click makes it teleport nearby. One collection method is to let it fall onto a Torch: after locating the teleported egg, carefully excavate below it, place the Torch two blocks beneath the egg with one supporting block still between them, then remove that supporting block. The falling egg drops as an item when it reaches the Torch. A Piston can also dislodge it. Keep its landing area safe.

### Offering an artifact

1. Make sure the world has reached Stage 2 by defeating the Ender Dragon.
2. Place the Dragon Egg as a block somewhere you can safely reach it.
3. Hold one of the six artifacts and right-click the egg with it.
4. Read the feedback. A successful offering is absorbed, and the egg's cracking appearance reflects progress.
5. Repeat with the other five distinct artifacts whenever you obtain them.

You do not need an altar, a surrounding block pattern, or dropped items on the ground. Use the held artifact directly on the placed egg. In Survival, each successful offering consumes one item. An offering that has already been accepted is rejected without consuming another copy. Outside Stage 2, the egg does not respond to these offerings.

For example, suppose your group returns from an Ocean Monument with a Heart of the Sea. You hold it and use it on the egg; one Heart disappears and Water is saved as completed. A friend later defeats a Gale Core and sacrifices its Heavy Core, completing Air. Another Heart is unnecessary: using one again reports that Water has already been absorbed. The remaining checklist is Fire, Earth, Life, and Death, regardless of which player finishes each task.

Offering the sixth distinct artifact completes the ritual. The egg hatches and disappears, and the world enters Stage 3. Decide as a group when to make that final sacrifice: it changes mobs, raid availability, and normal End-portal travel for everyone.

### What counts and what does not

The egg recognizes the item's identity. It does not secretly track which particular boss produced that stack. Existing valid artifacts from an older world can still work. Older Retold Water Element and Air Element items also remain accepted for compatibility, although current encounters reward Heart of the Sea and Heavy Core instead.

Several vanilla shortcuts have changed. New buried treasure no longer supplies a Heart of the Sea. The ordinary Lodestone recipe is removed, along with new Lodestones from Ruined Portal and Bastion Bridge chest loot. Trial Chambers are not newly generated, so their usual Heavy Core route is absent. Use the guardian paths described here instead of relying on those vanilla sources.

Water, Life, and Death artifacts can be kept before Stage 2 if you obtain them earlier; the egg still waits for Stage 2 to accept them. The Air Temple, natural Wildfire encounters, and awakened Earth Guardian have later-stage access rules. An artifact can also have another useful purpose: a Totem can save your life and a Nether Star can become a Beacon. Keep a separate copy if you want that use as well as the sacrifice.

Progress survives ordinary saving and restarting. Old Stage 2 worlds must supply any missing required sacrifices; worlds already in Stage 3 are not sent backward by this expanded checklist. Natural end-to-end progression and upgraded-world encounters remain experimental, so an older world can require travel into newly generated terrain for a missing artifact.

## 12 Structures and guardian expeditions

An expedition combines finding a site, reaching its important room, understanding its defense, and bringing the reward home. The following preparation suggestions are starting points, not guarantees of an easy fight. Difficulty, terrain, equipment, and other creatures affect each encounter.

When comparing boss health, **two health points equal one heart**. Health numbers describe the creature's body; shields, armor, and other defenses can make the actual fight longer.

### Water at an Ocean Monument

An **Ocean Monument** is the large Prismarine structure found underwater. A Cartographer's ordinary Ocean Explorer Map or ocean exploration can help you locate one. Retold uses the monument's Elder Guardian as the Water challenge. Newly generated monuments keep one persistent Elder rather than the usual three; older saved monuments can retain their existing Elders.

Bring equipment for working underwater as well as fighting. Food, breathing support, blocks, tools, and a way to manage water are useful preparations. Establish a recovery point outside the monument before carrying your best supplies into it.

The decisive rule is that **the Elder Guardian cannot be damaged while it is in water**. Repeated underwater sword strikes or arrows will not overcome this defense. Blocked attacks produce water feedback, repel the attacker or projectile, and apply Mining Fatigue III, which slows mining. The intended practical problem is to drain or otherwise expose the Elder so it is no longer in water.

Ordinary Guardians complicate that work. They defend the monument and can call nearby allies when attacked. Mining important monument blocks increases defensive pressure. Guardians with an active, unobstructed attack on you can interrupt your mining, so a tunnel straight through the building is not always a reliable shortcut. Breaking their line of sight or dealing with nearby guards gives you room to work; a wall helps only while it actually obstructs the beam.

Once dry, the Elder becomes vulnerable and takes drying damage as well. Its ordinary health is 80 points, or forty hearts. Defeat it and recover the guaranteed Heart of the Sea, then keep that item for the egg. Finding another buried-treasure chest will not replace this expedition's reward in the current build.

### Air at the Air Temple

Air Temples become available in Stage 2 above **Frozen Peaks, Jagged Peaks, and Stony Peaks**. They are high Overworld structures, not a separate dimension. Expect floating islands, smaller surrounding islands, and a Copper-and-Tuff tower well above the ground.

For the survival discovery route, level a Cartographer to **Journeyman or higher**, then interact with that villager after Stage 2 begins. It can gain an Air Temple Explorer Map with a base price of twelve Emeralds and one Compass. The map marks the temple with an X. Existing Cartographers can gain the offer without replacing their profession or rerolling their saved trades. An eligible destination still has to be found for the offer to appear.

Pack for a climb and for falling. Building blocks, food, a ranged weapon, and fall protection are useful. Plan a route back down before reaching the tower top. Air Temple wind pushes exposed creatures, changes between cardinal directions about every twenty seconds, and affects the surrounding airspace as well as the floors. Breezes and the Gale Core ignore that wind.

Nearby solid cover on the **upwind** side can shelter your whole body. Upwind means the direction the wind comes from. Watch the moving wind particles, then put shelter between yourself and that direction. Protection is local: a distant wall does not block wind everywhere behind it, and a low wall may leave part of your body exposed.

The **Gale Core** waits near the tower top and activates for nearby Survival or Adventure visitors. It has 160 health, or eighty hearts. Its first phase resembles a grounded Breeze fight. At half health it permanently changes to aerial movement, keeping its distance and using higher positions. Normal Breeze projectile deflection applies before this transition; the aerial phase removes that deflection, creating a different opportunity for ranged attacks.

Its wind charges do not directly remove your health, but displacement, falls, and collapsing footing remain dangerous. The charges can crack nearby eligible blocks during flight and on impact, eventually breaking them without useful mining drops. Cracking is a cue to move away from that cover or floor. Containers, fluids, unbreakable blocks, and very hard blocks have exclusions; terrain changes also obey griefing and protection rules.

If the boss or its target leaves the encounter's area, it drops pursuit and returns toward its tower home. That does not promise a full health reset or a fresh first phase. Defeat it, recover the Heavy Core, and descend carefully. The tower may have changed during the fight. Air encounter balance and presentation remain under development.

### Fire from a roaming Wildfire

There is no Fire temple to locate. From Stage 2 onward, **Wildfires** can appear in loaded Nether terrain near active players, accompanied by three to five Blazes. Explore the Nether and watch for the larger shielded leader. Its group can patrol in a line before spreading out for combat.

Encounter spawning is separate from ordinary biome monster weights. It still obeys Peaceful mode, the mob-spawning rule, valid placement, and distance checks. The system makes periodic attempts, but unsuitable terrain or another nearby Wildfire can prevent success. Waiting thirty seconds does not guarantee that one will appear.

Prepare for fire, flying enemies, and knockback near dangerous terrain. Fire Resistance can help against fire, but the close-range shockwave and being pushed into a drop remain separate problems. A route across solid ground gives you more options than a narrow ledge beside a deep lava pool.

A Wildfire has 120 body health, or sixty hearts, armor, and **four shields**. Each shield absorbs damage before the body becomes vulnerable. Shield breaks have flame and sound feedback, and a short protected interval follows each break. Do not judge remaining progress only from the apparent size of a single hit: excess damage on one shield does not simply remove all the others.

The leader fires stronger fireballs than an ordinary Blaze and uses a damaging shockwave against close enemies. The wave pushes nearby opponents away. Escorts remain an active threat while you concentrate on the leader, so maintain space and keep track of the whole group rather than rushing straight into melee.

At half health, the Wildfire can retreat toward suitable deep lava. It clears combat, submerges fully, and restores both health and shields while its eyes remain under the lava. It resurfaces after a full recovery. Ordinary fire, Soul Fire, or shallow contact with lava does not provide the same recovery. A wounded leader moving down into a deep pool is therefore an important cue: allowing a long recovery can undo substantial fighting progress.

Wildfires and their encounter escorts also fight Retold Undead, so you may find the group involved in Nether faction combat. That can change the immediate situation without making them friendly to you. Defeating the Wildfire guarantees one Nether Reactor Core for the Fire sacrifice. Retrieve it before loose items are lost in the surrounding terrain.

### Earth beneath the Jungle Pyramid

Every newly generated **Jungle Pyramid**, also called a Jungle Temple, includes an Earth Labyrinth below it. Existing vanilla pyramids are not retrofitted. If an old pyramid has no expanded passage, explore newly generated terrain rather than assuming a missed crafting or stage requirement.

Solve the pyramid's familiar lever puzzle to reveal its secret room and the access route beneath it. The labyrinth is a large, seeded two-level cave maze with loops, level connections, dark branches, traps, and occasional treasure. Its layout differs between sites. Bring lighting, food, useful tools, and a way to mark your route. Feed ordinary survival needs before beginning a long search underground.

Pressure plates can trigger dispensers containing real, finite arrow supplies. Treasure side rooms can contain metals, gems, and food. Darkness still permits ordinary natural mob spawning; these are not rooms that become safe merely because a special spawner was broken. Explore cautiously and keep the route to the pyramid identifiable.

The **Earth Guardian** begins as an indestructible statue. In Stage 1 it cannot be defeated or moved. After Stage 2 begins, approaching its chamber as a Survival or Adventure player starts a short awakening and reveals the boss encounter. Do not waste weapons on the dormant statue.

The awakened guardian has 250 health, or 125 hearts, and armor. It is **blind**: it investigates remembered vibration locations rather than acquiring ordinary sight of a player through walls. Walking, mining, placing blocks, using containers, projectiles, explosions, and damage can provide clues. Sneaking reduces movement noise; it does not make loud construction or mining silent. Hearing has a limited range, and it remembers a source for a time rather than tracking every player's location perfectly.

Blindness does not remove its melee threat. When it reaches the player whose sound it remembers, it can strike at close range if there is a clear path to the hit. Its attack comes from the Iron Golem combat system, so treat it as a heavy, variable hit rather than a harmless touch. Fresh obstruction checks prevent ordinary strikes straight through a wall, but the guardian can excavate that wall.

Watch for the environmental warnings. It can mark a remembered location and, after about 1.5 seconds, pull actual ceiling blocks down, replace a 3×3 ground patch with persistent lava, or collapse a 3×3 patch into a pit up to four blocks deep. Dust can identify threatened ceiling material; smoke and ground effects warn of hazards near the marked position. The position stops following you once selected. Move away promptly rather than remaining on the mark to finish an attack.

The guardian can keep pursuing and striking during a warning. Its lava behaves like real lava, and pits remain after the initial fall. If there is no usable ceiling, a planned ceiling attack can become a lava attack instead. Do not rely on an open ceiling to disable the encounter's danger.

**The guardian permanently reshapes loaded terrain.** It excavates routes and ramps and builds barriers behind itself. This can continue outside the labyrinth. Player blocks, containers, mechanisms, fluids, and even Bedrock are valid material; maze walls and the structure boundary do not contain it. Relocated containers retain their block state rather than their stored inventory data. Keep valuable storage and builds away from an active guardian. Server griefing and protection rules can limit these changes.

Defeating the guardian permanently clears that labyrinth's encounter and yields one Lodestone. Older already-defeated encounters do not respawn merely because a later build added this reward, so an upgraded world may need another pyramid. Recover the drop carefully around persistent lava. Natural generation, pursuit, destruction, and balance still need broader playtesting.

### Life from an Evoker

An **Evoker** is an Illager spellcaster that drops a Totem of Undying. In Stage 2, a Woodland Mansion provides a route to one before raids become available. A Cartographer's Woodland Explorer Map can help locate a mansion. Its placement can have been delayed until this stage, so newly unlocked terrain and an older already-generated site may differ.

Explore with a retreat route and expect other Illagers. Evokers summon hostile Vexes and use fang attacks. Give the spellcaster priority when practical rather than allowing repeated summons while fighting every nearby enemy. Retold's Vex and faction behavior can add to the pressure.

Keep the Totem for the egg. If you want a spare for personal protection, obtain another: a Totem sacrificed in the ritual is consumed. This Life path currently uses the Evoker and Totem relationship; a separate custom Life boss or temple is not required.

### Death from the Wither

The **Wither** is a player-summoned boss. Obtain three Wither Skeleton Skulls from Wither Skeletons and four Soul Sand or Soul Soil blocks. Build a T shape from the four base blocks, with one central stem and three blocks across its top. Leave the spaces beside the stem empty. Put one skull on each top block, placing the final skull last.

```text
Skull  Skull  Skull
Soul   Soul   Soul
air    Soul   air
```

This diagram is a front view. “Soul” means Soul Sand or Soul Soil. Summoning does not work in Peaceful mode. Choose the location before placing the last skull: the Wither charges up and then explodes, and its later attacks can damage the surrounding world. A site near valuable storage or village buildings is a poor place to learn the fight.

Prepare armor, food, a reliable melee weapon, and a way to handle its flying opening. Its low-health armored phase resists ordinary arrows, making a close-range plan important. Defeating it yields the Nether Star used for Death. Retold does not currently require an earlier Wither kill to open the first End portal, so there is no need to spend a Star on an extra, invented progression gate.

## 13 The Nether and the End

The Nether and End are existing Minecraft dimensions with Retold changes. The Aender, covered next, is the new dimension. Air Temple and Earth Labyrinth expeditions stay in the Overworld.

### Making your first Nether portal

A standard Nether portal is an upright Obsidian rectangle with a **two-block-wide, three-block-high empty interior**. The complete outside rectangle is four blocks wide and five high. Its four corners may be omitted, leaving ten required Obsidian blocks. This differs from the flat Aender portal described later.

Harvest Obsidian using a Diamond Pickaxe or another suitable later tool, then build the frame and ignite the inside with Flint and Steel. Stand in the resulting purple field to travel. Carry an igniter and record the portal's location on arrival so you can find and relight your return route if necessary.

Choose its Overworld site carefully. Retold adds permanent environmental drain around an active Nether portal. A large portal beside your farm or wooden house can become an expensive mistake even before you visit the Nether often.

### Understanding portal drain

Two fronts advance through loaded surroundings. An outer front kills or withers eligible vegetation and living ground. An inner front follows behind it, converting selected materials toward Nether forms and evaporating water. A standard portal reaches an outer radius of sixteen blocks and an inner radius of eight. Larger interiors increase those radii, up to forty-eight outside; the inner radius is half, rounded down.

Leaving the portal active permits slow background work. Successful travelers add pulses, and this includes more than players. Simply avoiding travel does not stop an active portal's background spread. Deactivating the portal stops that source; already changed terrain remains changed.

Loaded **lava source blocks** within the affected sphere resist the spread. A source is a full originating lava block, rather than its flowing extensions. Six sources can stop further drain from a standard six-block interior after the system updates its count. Larger portals need sources equal to their interior block area. The lava is not consumed, and stopping the drain does not reverse it.

A practical approach is to build a separate portal site away from gardens, water features, and valuable construction, then contain the required lava sources safely if you want it to remain active. Flowing lava alone is inadequate. Another option is to deactivate it between trips. Both choices need ordinary care around lava and relighting.

Conversions follow selected material rules. Containers, protected valuable blocks, and the portal's Obsidian have exclusions, while unclassified modded blocks are not automatically converted. The system does not create lava or fire as a drain product. Do not interpret those exclusions as protection for an entire nearby building.

### Exploring the changed Nether

The Nether has darker classic-style lighting and dark-red fog. Crimson and Warped Forests remain biomes, but new terrain gives them scattered fungi and vegetation rather than their usual dense growth. Basalt Deltas and Soul Sand Valleys remain; the latter can also have rare solitary Wither Skeletons. Stage 2 adds Wildfires, and Stage 3 enables living Piglin populations while cleansing Undead.

Nether fossils remain, but no longer naturally place Dried Ghasts. Crafting and existing Dried Ghasts still provide their separate hydration and growth route. New Ruined Portals retain their structures but omit their chest placement.

### What you will not find through the usual route

| Vanilla destination or source | Current Retold generation rule |
| --- | --- |
| Ancient Cities | No new ordinary generation |
| Trial Chambers | No new ordinary generation |
| End Cities | No new ordinary generation |
| Deep Dark | Removed from the normal fresh Overworld biome preset |
| Outer End islands and dragon gateways | Normal outer-End terrain and gateway progression suppressed |
| Warm Ocean Ruin archaeology | No normal Sniffer Egg reward |
| Villages very close to world spawn | New candidate sites within roughly 512 blocks are excluded |

The End keeps its central dragon journey and a generated per-world sky. Normal End City Elytra and Shulker acquisition is unavailable in new-world progression, and the usual crystal arrangement does not respawn the dragon after Stage 1. After the six sacrifices, Overworld End portals take players to the Aender instead.

These are generation and acquisition rules, not deletion of every related item or entity. Existing structures, equipment, custom datapacks, Creative, and commands can differ. Retold also delays certain structures and may place them later in eligible, unobserved areas; sufficiently edited sites can be skipped. Backups matter when updating an established world, and an old region is not a reliable demonstration of fresh generation.

## 14 Your first Aender expedition

The **Aender** is a bright realm of floating islands above a void. After the Dragon Egg ritual advances the world to Stage 3, enter an Overworld End portal to reach its central Obsidian landing platform. That entrance is separate from the horizontal return portals you will build inside the dimension.

### Before you enter

Recommended supplies include food, blocks for safe movement, dependable equipment, an Iron-or-better Pickaxe, and room for collected materials. Bring a Netherite Pickaxe if you want Aenderite Ore drops. Fall protection and a clear Overworld respawn plan are useful; they do not replace planning a return route.

Expect permanent midday and no weather. Water flows faster and farther than in the Overworld. Emptying a lava bucket vaporizes its contents instead of placing lava. **Beds explode, and Respawn Anchors do not work**, so do not try to establish an ordinary respawn point here.

Keep the initial landing area clear. Each entry through an Overworld End portal recreates its five-by-five Obsidian floor and clears three blocks of headroom. Storage or construction inside that footprint can be broken independently of the wider terrain's instability.

### Gather material for a return portal

Explore from the platform carefully and remember the route. Current fresh worlds contain Aender Plains and an experimental desert biome, with varied island heights, caves, lakes, vegetation, and hanging terrain. Older saves may use an earlier generator. Endermen and passive flying Aender Eyes inhabit the dimension. The Eyes observe from a distance and avoid close approach; they currently have no loot reward.

Look for **Development Aender Portal Frame** deposits embedded in Aender Stone. These are clusters of material, not ready-built portals. Mine them with an Iron, Steel, Diamond, or Netherite Pickaxe; an inadequate tool can destroy the block without giving you the frame. Collect at least sixteen.

Build your return portal on a safe supporting platform. Lay the frame flat, all at one height, with its corners included and a three-by-three air opening:

```text
F F F F F
F . . . F
F . . . F
F . . . F
F F F F F
```

This is a view from above. `F` is a frame block; each dot is an empty interior position. Put a solid floor directly below the opening so you can remain inside the portal field. The field itself does not support your weight. Placing the last frame activates it automatically; no igniter is needed. Breaking the border invalidates it.

Step into the field to travel to the Overworld. The system reuses a suitable nearby counterpart or creates one with support. Returning here through that Overworld counterpart requires a charge: entry into the Aender takes at least five seconds in Survival or Adventure and can wait longer while the arrival area is prepared. Stay in the field until travel completes. Larger horizontal portals may have rectangular interiors from three through twenty-one blocks on each side.

### Understand where the portal leads

Horizontal Aender portals connect only the Overworld and Aender, and their travel is unlocked in Stage 3. They do not send you back through the stronghold's End portal.

Their coordinate rule is unusual: **Overworld X and Z multiply by eight on entry to the Aender; Aender X and Z divide by eight on return**. For example, Overworld X=100, Z=200 corresponds approximately to Aender X=800, Z=1600. A return portal at that Aender location searches around Overworld X=100, Z=200. Safe placement and world borders can adjust the exact exit.

### Know what can disappear

Unstable Aender terrain can regenerate when regions stop being watched or when the final player actually travels out of the dimension. Player construction, stored items, portal arrangements, and non-player creatures in unstable areas can be lost. Another player remaining somewhere else in the Aender does not preserve every region you explored.

Saving, quitting, or restarting alone does not automatically reroll the dimension. Your edits are saved until the world's legitimate region or departure rules make that terrain change. Nevertheless, an ordinary survival base here is not a dependable permanent store. Bring valuables home and treat bridges and return arrangements as things that may need rebuilding on a later expedition.

An **Aender Stabilizer** can preserve its own chunk and the eight surrounding chunks, forming a chunk-aligned 48×48 area through the full world height. A visible particle boundary marks protection; overlapping fields can preserve a larger area. Removing the last protection releases the area, although terrain still watched by players waits before replacement.

The Stabilizer is currently a Creative or administrative development block without normal survival acquisition. Its existence does not mean you have missed a craftable solution. A complete earned survival building loop remains unfinished.

### Materials to bring home

Aender trees provide a renewable wood family: Logs, stripped forms, Planks, construction pieces, doors, signs, and boats. Natural leaves can decay and drop Saplings or Sticks; use Shears or Silk Touch to collect the leaves themselves. Saplings let you grow more trees, and logs can become Charcoal. Aender Grass spreads across Aender Soil and accepts Bonemeal. Ordinary grass-block harvesting yields Soil unless Silk Touch preserves the grass block.

Desert islands add Aender Sand, Sandstone, and lavender Cacti. These Cacti grow to three blocks high without producing the Overworld Cactus Flower.

Aenderite Ore favors lower portions of islands. Only a **Netherite Pickaxe** gives its proper ore drops. Silk Touch preserves the ore; otherwise it yields Raw Aenderite, with Fortune affecting the amount. Smelt or blast the raw material into an Aenderite Ingot. The material currently has no equipment or ingot crafting use, so collect it as an unfinished late-game resource rather than expecting an immediate armor upgrade.

The Aender's terrain, return travel, multiplayer regeneration, and late-game rewards are still experimental. Discoveries here expand the current sandbox, but a separate internal teleport network, Aenderite equipment, and Nether or Aender dragon bosses are not completed progression steps.

## 15 Other changes and experimental tools

### Recognizing the changing world

Endermen provide a visible stage clue as well as a behavioral one. Their eyes are purple in Stage 1, green in Stages 2 and 3, and their bodies become white with green particles in Stage 3. Eye-contact aggression stops after Stage 1, but harming them can still provoke retaliation. Color is a useful clue, not permission to attack safely.

The Aender has bright, relatively flat lighting and green ambient particles. Its appearance does not prevent void falls or remove the instability rules. The Nether's darker presentation also does not mean every lighting rule has changed: ordinary block light, brightness settings, and vision effects still matter.

The current boss models, Steel equipment artwork, and Aender artwork are development presentations. Animations and visual cues accompany the implemented mechanics, but final appearance and encounter readability are still being reviewed.

### A distant figure

Occasionally, a distant human-shaped figure can appear for a few seconds and disappear when approached or watched. It is an atmospheric effect shown to the individual player, not a creature to fight, tame, or farm. It has no combat reward. Its schedule uses game time; it is not a promise that someone else sees the same figure beside you.

### Snowballs

Snowballs can deal one health point in cases where an ordinary vanilla hit would be harmless. Their stronger three-point Blaze interaction remains. One health point is half a heart. This is a small combat change rather than a replacement for a dependable weapon, and ordinary target immunity rules can still matter.

### Aender Stabilizers

A Stabilizer protects its own chunk and the eight neighboring chunks in the Aender. That is a chunk-aligned 48×48 horizontal area through the full height, rather than a circle around its model. Particle boundaries show the protected region; the field is feedback for stability, not a physical wall that keeps enemies out.

Overlapping fields can maintain protection when one block is removed. When the last protection disappears, watched terrain is retained until it becomes eligible to change, so removing a block does not require an immediate disappearance beneath a connected player.

**The current build has no normal survival recipe or natural loot source for the Stabilizer.** It is available for Creative or administrative experiments. Do not plan a survival base around a recipe that the game does not yet supply. Chapter 14 explains why this matters before your first building project.

### The Aender Chronolith

Despite its name, the Chronolith currently works only in the **Overworld**. It is a development time-control block, with no survival recipe. Find it in Creative or obtain it administratively rather than treating it as a completed late-game reward.

Right-click to start a channel while you have experience, remain within eight blocks, and let its beam and sound show that it is active. It ramps up its time advance, clears weather when activated, and drains experience **points**. Creative users do not pay. Only one channel can run at a time.

Right-click again or sneak to stop. Leaving range, losing the player connection, dying or changing away from the valid situation, removing the block, running out of experience, or stopping the server also ends the channel. It advances world time; it is not a guarantee that every crop and creature receives additional simulated growth ticks.

### Finding content in Creative

Retold adds its items to the ordinary Creative categories beside related Minecraft objects. There is no separate Retold tab you must unlock. Search for the item's displayed name if you cannot find its category. The provisional portal frame also appears among operator utilities when the necessary permission is available.

The old Water Element and Air Element items remain for save compatibility. Current guardian rewards are Heart of the Sea and Heavy Core. An item being present in Creative does not mean it has a current Survival recipe or natural acquisition route.

## 16 Commands and server settings

Commands are instructions typed into the chat or a server console. Open chat, enter the command beginning with `/`, and press Enter. Tab completion helps with names and arguments. In single player, cheats must be enabled for administrative commands; on a server, its owner decides who is an operator.

Both Retold command roots require **operator permission level 2**. They are useful for diagnosis and experiments, but none is a required step in normal Survival progression. A new player can read the main handbook without learning the debugging controls.

In the reference below, `<value>` means a required value and `[value]` an optional one. Do not type the angle or square brackets. For example, `/retold stage set 2` is the real command, not `/retold stage set <2>`.

### World and presentation commands

| Command | What it does |
| --- | --- |
| `/retold stage get` | Show the shared world stage |
| `/retold stage set <stage>` | Change the stage; allowed values are 1, 2, and 3 |
| `/retold village status` | Inspect nearby loaded village standing and possible Golem hostility; run as a player |
| `/retold sky get` | Show the saved vanilla End sky seed |
| `/retold sky randomize` | Randomize the vanilla End sky seed and update clients |
| `/retold testworld` | Build a content gallery above the player in the current Overworld save |
| `/retold testworld <x> <y> <z>` | Choose the gallery's minimum platform corner |

The sky commands concern the vanilla End's generated sky, not weather or stability in the Aender. Village status is a nearby loaded-context diagnostic, not a worldwide reputation record.

Stage changes invoke real transition behavior. Setting Stage 3 can remove Undead, change travel, and move players out of the End. Setting the stage backward does **not** rebuild terrain, resurrect removed creatures, or restore a complete previous progression state. Use a separate save for experiments.

### Bed night skipping

Retold's custom game rule defaults to false:

```mcfunction
/gamerule retold:do_bed_night_skipping false
```

Allow ordinary night skipping with:

```mcfunction
/gamerule retold:do_bed_night_skipping true
```

This setting changes the world's resting behavior. It does not implement the planned bed-healing system or make beds safe in dimensions where they explode.

### Inspect a creature

Most single-creature commands use the eligible mob you are looking at within approximately twenty-four blocks. Look directly at it rather than merely standing nearby. Commands that take a radius accept whole numbers from one through ninety-six.

| Command | What it tells you |
| --- | --- |
| `/retoldbehavior toggle` | Toggle the looked-at-creature debug display |
| `/retoldbehavior toggle overlay` | Alternate form of the same toggle |
| `/retoldbehavior get` | Creature behavior identity and current state |
| `/retoldbehavior home` | Remembered home or range |
| `/retoldbehavior guardpost` | Guard-post information |
| `/retoldbehavior warning` | Territory and warning state |
| `/retoldbehavior pack` | Hunting-pack information |
| `/retoldbehavior showhomes [radius]` | Nearby home markers; default radius 32 |
| `/retoldbehavior nearby [radius]` | Nearby managed-creature summaries; default radius 24 |
| `/retoldbehavior targets [radius]` | Target candidates for the looked-at creature; default radius 18 |
| `/retoldbehavior count` | Active behavior-state and control counts |
| `/retoldbehavior perf` | AI work and cache counters |
| `/retoldbehavior perf reset` | Reset counters before another measurement |

The debug values are not ordinary Survival HUD meters. A hunger value rising toward one hundred means the creature is hungrier; zero is well fed. Reading a state helps explain a particular animal, but it is not proof that every species uses that state in the same way. Some have no Retold metabolism.

### Change a creature for an experiment

| Command | Accepted value or effect |
| --- | --- |
| `/retoldbehavior sethunger <value>` | Set hunger from 0 to 100 |
| `/retoldbehavior addhunger <amount>` | Adjust hunger by −100 through 100 |
| `/retoldbehavior setstress <value>` | Set stress from 0 to 100 |
| `/retoldbehavior addstress <amount>` | Adjust stress by −100 through 100 |
| `/retoldbehavior setconfidence <value>` | Set confidence from 0 to 100 |
| `/retoldbehavior addconfidence <amount>` | Adjust confidence by −100 through 100 |
| `/retoldbehavior setguardpost` | Set an eligible guard's post to its current position |
| `/retoldbehavior clearguardpost` | Remove its saved post |
| `/retoldbehavior clearhome` | Clear the remembered home |
| `/retoldbehavior clearcontrol` | Release its current Retold AI control |

For example, look at an eligible Cow and run `/retoldbehavior sethunger 70` to examine hungry behavior, then inspect it with `get`. Setting a number does not add food, build a route to a feeder, or freeze that number against subsequent updates. These commands modify the test subject; do not confuse their effects with naturally occurring behavior.

### Build a test gallery

Create a disposable Creative world with cheats, enter the Overworld, and run `/retold testworld`. The command builds a lit aerial gallery near the world height limit and teleports you to its entrance. It builds **inside the current save** rather than creating a new save or dimension.

The gallery uses the live catalog: currently 52 items, 37 block types, and six entity types, including boats. It supplies useful copies, workstations, equipment displays, a Dragon Egg, and ritual artifacts. Exhibit creatures are frozen, persistent, and invulnerable; they demonstrate content and rendering rather than natural AI.

The default site is centered over your horizontal position. Explicit coordinates choose the minimum corner. At the current catalog size, reserve about **66×104×10 blocks of empty space** and keep the entire footprint loaded and ticking. Occupancy or protection failures reject construction without clearing terrain. If activation is incomplete, remain near the intended center and retry; low simulation distance can limit which sites are ready.

The command initially preserves stage, game mode, and game rules. Interactive exhibits and supplies can still do real things: a portal can travel, a Chronolith can advance time, and using the six artifacts on the egg can advance progression.

### Ordinary content commands

Minecraft's `/give` and `/summon` accept Retold IDs where appropriate. Item and entity IDs are different kinds of identifiers; the complete custom catalog is in Appendix E.

```mcfunction
/give @s retold:flint_multi_tool
/give @s retold:steel_pickaxe
/give @s retold:animal_feeder
/give @s retold:dev_aender_portal_frame 16
/give @s retold:aender_stabilizer
/give @s retold:aender_chronolith
```

Here `@s` means the executing entity, so use these examples as a player. Spawn eggs and command-created bosses can behave differently from natural dormant or structure-bound encounters. Keep destructive Earth Guardians out of valuable worlds.

### Server settings and other mods

The ordinary Mob Griefing setting affects many Retold creature block changes: destructive forage, weak-fence breaches, Spider webs, guardian terrain attacks, and some village work. Dropped-food consumption and meals from an accessible feeder do not require destructive grazing. Disabling griefing can therefore also change how you must feed a farm.

It is not a single switch that restores vanilla behavior or freezes all world systems. Nether portal drain and Aender regeneration have their own rules. A claim mod also needs an appropriate integration to enforce its claims through Retold's protection hooks; the hooks existing does not guarantee an adapter for every server plugin or mod.

Datapacks can extend selected diets, factions, recipes, protected blocks, and teaching data. New recipes or loot can intentionally create alternative artifact sources. Another mod may run alongside Retold while changing its intended balance, so use the server's specific rules when they differ from this handbook.

## 17 Current build limits and unfinished work

This is a development-build handbook. Implemented means there is a current mechanic to explain; it does not mean every seed, natural encounter, visual effect, server, or old world has been fully verified. The most significant remaining checks concern full six-artifact progression, destructive Earth encounters, Aender travel and regeneration, upgraded saves, multiplayer, and long-term ecosystem balance.

Some distinctions affect what you should try in Survival:

- **Pillager Outposts currently wait for Stage 3.** Earlier design documents describe Stage 2. The handbook reports the build's behavior without treating that discrepancy as a new approved design.
- **Aender Stabilizers and Chronoliths have no ordinary survival acquisition route.** The portal frame does have a natural deposit source despite its provisional name.
- **Aenderite has ore, raw material, and Ingots, but no completed equipment or Ingot crafting uses.**
- **Certain configured Villager lessons use unavailable recipe references.** Appendix B separates them from available lessons. Direct Campfire/Smithing recipe discovery also needs further verification; successful processing is not a promise of a recipe-book unlock.
- **Turtles currently flee nearby visible eligible players**, despite the intended distinction between peaceful walking and threatening behavior. Armadillos do implement a peaceful-walking exception. Do not assume the same exception for Turtles in this build.
- **Existing or Creative content is broader than normal survival acquisition.** Supporting Sniffer, Endermite, Shulker, Warden, or Illusioner behavior does not restore their ordinary removed or unavailable generation routes.

The following are future or incomplete systems, rather than hidden steps you are expected to discover:

- Nether/Aender dragon bosses, a complete Aender teleport network, and finished late-game travel/building rewards.
- Piglin/Brute hiring, followers, employment contracts, and completed gold-armor/reputation admission rules.
- A complete village economy or every planned profession's society behavior beyond the present supply, property, tending, maintenance, trade, and construction loops.
- A broader sword-blocking/combat redesign, completed equipment balance, and a complete brewing overhaul.
- Hunger-paid bed healing, longer death-drop lifetimes, water/glowstone torches, and general offline crop growth.
- A special maximum-size Slime boss, natural Killer Bunny encounters, Iceologers, the proposed music-disc monster, smaller Bees, and a green Axolotl variant.

If something differs from the handbook, first check the build version and the world's datapacks. A small reproducible report should name the dimension and stage, describe the actions and expected result, and include relevant server/client logs where available. State whether it happened in a fresh or old world and in single player or multiplayer. The support and issue links are listed at the end.

## Appendix A Recipes and processing

This atlas covers every added recipe file and the replaced Brick Furnace recipe, the Campfire rewritten by Retold, and selected ordinary recipes needed for the journey. It does not reproduce every unchanged Minecraft recipe.

### Reading the diagrams

Each letter or symbol occupies one ingredient slot. `·` means an empty slot. The legend gives the item and total quantity. Take the output to complete crafting. A shaped recipe needs the displayed arrangement; a shapeless recipe allows its ingredients in any positions. Ordinary mirrored shaped arrangements can also work.

A recipe listing “any Planks” accepts the appropriate plank family, including Aender Planks where supported. A Log, Plank, Stick, Brick item, and Bricks block are different inputs. Recipes below use individual item counts, not stack counts.

### Retold starter recipes

#### Flint Multi-tool

**Output:** 1 × Flint Multi-tool.

**Station:** Inventory grid or Crafting Table.

```text
F  F
·  S
```

`F` = Flint (2); `S` = Stick (1).

#### Flint Spear

**Output:** 1 × Flint Spear.

**Station:** Crafting Table.

```text
·  ·  X
·  #  ·
#  ·  ·
```

`#` = Stick (2); `X` = Flint (1).

#### Campfire

**Output:** 1 × Campfire.

**Station:** Crafting Table.

```text
·  S  ·
S  ·  S
L  L  L
```

`S` = Stick (3); `L` = any Log (3).

#### Brick Furnace

**Output:** 1 × Brick Furnace.

**Station:** Crafting Table.

```text
B  B  B
B  ·  B
B  B  B
```

`B` = Brick (8).

#### Animal Feeder

**Output:** 1 × Animal Feeder.

**Station:** Crafting Table.

```text
P  ·  P
P  P  P
```

`P` = any Planks (5).

The Campfire is placed unlit. Bare Flint lights it and is consumed in Survival; it is not another crafting ingredient. Feeders are filled by interaction after placement. The Brick Furnace needs suitable fuel for processing.

### Steel tools weapons and armor

Use Steel Ingots in these patterns. Steel is produced in a Blast Furnace as listed in the processing table. These are the ten Steel equipment recipes; there is no Steel Horse, Nautilus, or Wolf Armor recipe.

#### Steel Axe

**Output:** 1 × Steel Axe.

**Station:** Crafting Table.

```text
X  X
X  #
·  #
```

`#` = Stick (2); `X` = Steel Ingot (3).

#### Steel Boots

**Output:** 1 × Steel Boots.

**Station:** Crafting Table.

```text
X  ·  X
X  ·  X
```

`X` = Steel Ingot (4).

#### Steel Chestplate

**Output:** 1 × Steel Chestplate.

**Station:** Crafting Table.

```text
X  ·  X
X  X  X
X  X  X
```

`X` = Steel Ingot (8).

#### Steel Helmet

**Output:** 1 × Steel Helmet.

**Station:** Crafting Table.

```text
X  X  X
X  ·  X
```

`X` = Steel Ingot (5).

#### Steel Hoe

**Output:** 1 × Steel Hoe.

**Station:** Crafting Table.

```text
X  X
·  #
·  #
```

`#` = Stick (2); `X` = Steel Ingot (2).

#### Steel Leggings

**Output:** 1 × Steel Leggings.

**Station:** Crafting Table.

```text
X  X  X
X  ·  X
X  ·  X
```

`X` = Steel Ingot (7).

#### Steel Pickaxe

**Output:** 1 × Steel Pickaxe.

**Station:** Crafting Table.

```text
X  X  X
·  #  ·
·  #  ·
```

`#` = Stick (2); `X` = Steel Ingot (3).

#### Steel Shovel

**Output:** 1 × Steel Shovel.

**Station:** Crafting Table.

```text
X
#
#
```

`#` = Stick (2); `X` = Steel Ingot (1).

#### Steel Spear

**Output:** 1 × Steel Spear.

**Station:** Crafting Table.

```text
·  ·  X
·  #  ·
#  ·  ·
```

`#` = Stick (2); `X` = Steel Ingot (1).

#### Steel Sword

**Output:** 1 × Steel Sword.

**Station:** Crafting Table.

```text
X
X
#
```

`#` = Stick (1); `X` = Steel Ingot (2).

### Aender wood recipes

Collect Aender wood from trees or grow Saplings. These are its complete custom crafting recipes. The wood can also participate in suitable ordinary generic wood recipes through its tags. Stripping a Log or Wood with an appropriate Axe is an interaction, not a crafting-grid recipe.

#### Aender Boat

**Output:** 1 × Aender Boat.

**Station:** Crafting Table.

```text
#  ·  #
#  #  #
```

`#` = Aender Planks (5).

#### Aender Button

**Output:** 1 × Aender Button.

**Station:** Inventory grid or Crafting Table.

**Method:** combine 1 × Aender Planks in any grid positions.

#### Aender Chest Boat

**Output:** 1 × Aender Chest Boat.

**Station:** Inventory grid or Crafting Table.

**Method:** combine 1 × Chest + 1 × Aender Boat in any grid positions.

#### Aender Door

**Output:** 3 × Aender Door.

**Station:** Crafting Table.

```text
#  #
#  #
#  #
```

`#` = Aender Planks (6).

#### Aender Fence

**Output:** 3 × Aender Fence.

**Station:** Crafting Table.

```text
W  #  W
W  #  W
```

`#` = Stick (2); `W` = Aender Planks (4).

#### Aender Fence Gate

**Output:** 1 × Aender Fence Gate.

**Station:** Crafting Table.

```text
#  W  #
#  W  #
```

`#` = Stick (4); `W` = Aender Planks (2).

#### Aender Hanging Sign

**Output:** 6 × Aender Hanging Sign.

**Station:** Crafting Table.

```text
X  ·  X
#  #  #
#  #  #
```

`#` = Stripped Aender Log (6); `X` = Iron Chain (2).

#### Aender Planks

**Output:** 4 × Aender Planks.

**Station:** Inventory grid or Crafting Table.

**Method:** combine 1 × Aender Log or Wood, including stripped forms in any grid positions.

#### Aender Pressure Plate

**Output:** 1 × Aender Pressure Plate.

**Station:** Inventory grid or Crafting Table.

```text
#  #
```

`#` = Aender Planks (2).

#### Aender Sign

**Output:** 3 × Aender Sign.

**Station:** Crafting Table.

```text
#  #  #
#  #  #
·  X  ·
```

`#` = Aender Planks (6); `X` = Stick (1).

#### Aender Slab

**Output:** 6 × Aender Slab.

**Station:** Crafting Table.

```text
#  #  #
```

`#` = Aender Planks (3).

#### Aender Stairs

**Output:** 4 × Aender Stairs.

**Station:** Crafting Table.

```text
#  ·  ·
#  #  ·
#  #  #
```

`#` = Aender Planks (6).

#### Aender Trapdoor

**Output:** 2 × Aender Trapdoor.

**Station:** Crafting Table.

```text
#  #  #
#  #  #
```

`#` = Aender Planks (6).

#### Aender Wood

**Output:** 3 × Aender Wood.

**Station:** Inventory grid or Crafting Table.

```text
#  #
#  #
```

`#` = Aender Log (4).

#### Stripped Aender Wood

**Output:** 3 × Stripped Aender Wood.

**Station:** Inventory grid or Crafting Table.

```text
#  #
#  #
```

`#` = Stripped Aender Log (4).

### Retold processing recipes

For furnace-style stations, put the listed input in the input slot and ordinary suitable fuel in the fuel slot. Fuel is consumed according to its burn time rather than a separate fixed recipe ratio. Campfire cooking uses a lit placed Campfire and its cooking positions.

| Input | Station | Output | Time at normal tick speed |
| --- | --- | --- | --- |
| Raw Aenderite | Blast Furnace | 1 × Aenderite Ingot | 5 seconds |
| Raw Aenderite | Furnace | 1 × Aenderite Ingot | 10 seconds |
| Clay Ball | Lit Campfire | 1 × Brick | 30 seconds |
| burnable Log | Brick Furnace | 1 × Charcoal | 5 seconds |
| Copper Ore | Brick Furnace | 1 × Copper Ingot | 5 seconds |
| Deepslate Copper Ore | Brick Furnace | 1 × Copper Ingot | 5 seconds |
| Raw Copper | Brick Furnace | 1 × Copper Ingot | 5 seconds |
| Iron Ingot | Blast Furnace | 1 × Steel Ingot | 5 seconds |

### Essential ordinary recipes

These retained recipes help a newcomer follow the handbook. Wood and material variants can have their own equivalent recipes. The Oak Planks and White Bed examples below use their named materials; other wood colors and bed colors use their corresponding variants. Wooden and Stone tools remain removed despite ordinary Minecraft tutorials showing them.

#### Oak Planks

**Output:** 4 × Oak Planks.

**Station:** Inventory grid or Crafting Table.

**Method:** combine 1 × Oak Log or Oak Wood, including stripped forms in any grid positions.

#### Stick

**Output:** 4 × Stick.

**Station:** Inventory grid or Crafting Table.

```text
#
#
```

`#` = any Planks (2).

#### Crafting Table

**Output:** 1 × Crafting Table.

**Station:** Inventory grid or Crafting Table.

```text
#  #
#  #
```

`#` = any Planks (4).

#### Chest

**Output:** 1 × Chest.

**Station:** Crafting Table.

```text
#  #  #
#  ·  #
#  #  #
```

`#` = any Planks (8).

#### White Bed

**Output:** 1 × White Bed.

**Station:** Crafting Table.

```text
#  #  #
X  X  X
```

`#` = White Wool (3); `X` = any Planks (3).

#### Torch

**Output:** 4 × Torch.

**Station:** Inventory grid or Crafting Table.

```text
X
#
```

`#` = Stick (1); `X` = Coal or Charcoal (1).

#### Copper Pickaxe

**Output:** 1 × Copper Pickaxe.

**Station:** Crafting Table.

```text
X  X  X
·  #  ·
·  #  ·
```

`#` = Stick (2); `X` = Copper Ingot (3).

#### Furnace

**Output:** 1 × Furnace.

**Station:** Crafting Table.

```text
#  #  #
#  ·  #
#  #  #
```

`#` = Cobblestone, Blackstone, or Cobbled Deepslate (8).

#### Blast Furnace

**Output:** 1 × Blast Furnace.

**Station:** Crafting Table.

```text
I  I  I
I  X  I
#  #  #
```

`#` = Smooth Stone (3); `I` = Iron Ingot (5); `X` = Furnace (1).

#### Iron Pickaxe

**Output:** 1 × Iron Pickaxe.

**Station:** Crafting Table.

```text
X  X  X
·  #  ·
·  #  ·
```

`#` = Stick (2); `X` = Iron Ingot (3).

#### Iron Sword

**Output:** 1 × Iron Sword.

**Station:** Crafting Table.

```text
X
X
#
```

`#` = Stick (1); `X` = Iron Ingot (2).

#### Bucket

**Output:** 1 × Bucket.

**Station:** Crafting Table.

```text
#  ·  #
·  #  ·
```

`#` = Iron Ingot (3).

#### Flint and Steel

**Output:** 1 × Flint and Steel.

**Station:** Inventory grid or Crafting Table.

**Method:** combine 1 × Iron Ingot + 1 × Flint in any grid positions.

#### Shield

**Output:** 1 × Shield.

**Station:** Crafting Table.

```text
W  o  W
W  W  W
·  W  ·
```

`W` = any Planks (6); `o` = Iron Ingot (1).

#### Ladder

**Output:** 3 × Ladder.

**Station:** Crafting Table.

```text
#  ·  #
#  #  #
#  ·  #
```

`#` = Stick (7).

#### Bread

**Output:** 1 × Bread.

**Station:** Crafting Table.

```text
#  #  #
```

`#` = Wheat (3).

#### Bow

**Output:** 1 × Bow.

**Station:** Crafting Table.

```text
·  #  X
#  ·  X
·  #  X
```

`#` = Stick (3); `X` = String (3).

#### Arrow

**Output:** 4 × Arrow.

**Station:** Crafting Table.

```text
X
#
Y
```

`#` = Stick (1); `X` = Flint (1); `Y` = Feather (1).

#### Paper

**Output:** 3 × Paper.

**Station:** Crafting Table.

```text
#  #  #
```

`#` = Sugar Cane (3).

#### Book

**Output:** 1 × Book.

**Station:** Inventory grid or Crafting Table.

**Method:** combine 3 × Paper + 1 × Leather in any grid positions.

#### Enchanting Table

**Output:** 1 × Enchanting Table.

**Station:** Crafting Table.

```text
·  B  ·
D  #  D
#  #  #
```

`#` = Obsidian (4); `B` = Book (1); `D` = Diamond (2).

#### Anvil

**Output:** 1 × Anvil.

**Station:** Crafting Table.

```text
I  I  I
·  i  ·
i  i  i
```

`I` = Block of Iron (3); `i` = Iron Ingot (4).

#### Block of Iron

**Output:** 1 × Block of Iron.

**Station:** Crafting Table.

```text
#  #  #
#  #  #
#  #  #
```

`#` = Iron Ingot (9).

#### Diamond Pickaxe

**Output:** 1 × Diamond Pickaxe.

**Station:** Crafting Table.

```text
X  X  X
·  #  ·
·  #  ·
```

`#` = Stick (2); `X` = Diamond (3).

#### Smithing Table

**Output:** 1 × Smithing Table.

**Station:** Crafting Table.

```text
@  @
#  #
#  #
```

`#` = any Planks (4); `@` = Iron Ingot (2).

#### Netherite Ingot

**Output:** 1 × Netherite Ingot.

**Station:** Crafting Table.

**Method:** combine 4 × Netherite Scrap + 4 × Gold Ingot in any grid positions.

#### Netherite Upgrade Smithing Template

**Output:** 2 × Netherite Upgrade Smithing Template.

**Station:** Crafting Table.

```text
#  S  #
#  C  #
#  #  #
```

`#` = Diamond (7); `C` = Netherrack (1); `S` = Netherite Upgrade Smithing Template (1).

#### Blaze Powder

**Output:** 2 × Blaze Powder.

**Station:** Inventory grid or Crafting Table.

**Method:** combine 1 × Blaze Rod in any grid positions.

#### Eye of Ender

**Output:** 1 × Eye of Ender.

**Station:** Inventory grid or Crafting Table.

**Method:** combine 1 × Ender Pearl + 1 × Blaze Powder in any grid positions.

#### Ordinary processing needed for progression

| Input | Station | Output and purpose |
| --- | --- | --- |
| Cobblestone | Furnace and fuel | Stone; smelt it again for Smooth Stone |
| Stone | Furnace and fuel | Smooth Stone for the Blast Furnace |
| Raw Iron | Furnace and fuel | Iron Ingot |
| Ancient Debris | Furnace or Blast Furnace and fuel | Netherite Scrap |
| Diamond Pickaxe + Netherite Ingot + Netherite Upgrade Smithing Template | Smithing Table | Netherite Pickaxe; the template and Ingot are consumed |

The Smithing Template duplication recipe needs an existing template. It does not create your first template from Diamonds alone. Obtain the first through its ordinary acquisition route. Obsidian and portal frames are also collected or placed as blocks; the diagrams do not invent a craft for them.

### Removed or absent recipes

The six Wooden recipes and six Stone recipes for Axe, Hoe, Pickaxe, Shovel, Spear, and Sword are removed. The ordinary Lodestone recipe is removed. Existing items can still have uses; a removed recipe does not erase every existing stack.

There is no current crafting recipe for an Aender Stabilizer, Aender Chronolith, Development Aender Portal Frame, Aender Sandstone, or Aenderite equipment. Frames have a natural deposit source; Stabilizer and Chronolith acquisition remains administrative/Creative. Aender Sand does not currently substitute for ordinary Sand/Red Sand in Glass smelting or TNT crafting.

## Appendix B Villager lesson index

Prices are Emeralds paid to learn instructions, not the price of buying the finished item. Supply the finished sample in the separate teaching slot and keep Emeralds in your inventory. The sample is returned. Profession levels that gate equipment trades do not add a corresponding mastery gate to these lessons.

The tables below list lessons whose recipes exist in this build’s bundled defaults. Multiple cooking methods for the same output are grouped when their price matches; show the finished food. The teaching panel determines the available lesson in your world. These are source-based defaults, not a promise that another datapack preserves every entry.

### Armorer

| Finished sample | Emeralds | Recipe method |
| --- | --- | --- |
| Anvil | 8 | Crafting |
| Blast Furnace | 3 | Crafting |
| Diamond Boots | 10 | Crafting |
| Diamond Chestplate | 16 | Crafting |
| Diamond Helmet | 12 | Crafting |
| Diamond Leggings | 14 | Crafting |
| Golden Boots | 5 | Crafting |
| Golden Chestplate | 6 | Crafting |
| Golden Helmet | 5 | Crafting |
| Golden Leggings | 6 | Crafting |
| Iron Boots | 4 | Crafting |
| Iron Chestplate | 5 | Crafting |
| Iron Helmet | 4 | Crafting |
| Iron Leggings | 5 | Crafting |
| Shield | 4 | Crafting |
| Steel Boots | 7 | Crafting |
| Steel Chestplate | 9 | Crafting |
| Steel Helmet | 7 | Crafting |
| Steel Leggings | 9 | Crafting |

### Butcher

| Finished sample | Emeralds | Recipe method |
| --- | --- | --- |
| Brick Furnace | 2 | Crafting |
| Cooked Chicken | 1 | Brick Furnace, Campfire, Furnace |
| Cooked Mutton | 1 | Brick Furnace, Campfire, Furnace |
| Cooked Porkchop | 1 | Brick Furnace, Campfire, Furnace |
| Cooked Rabbit | 1 | Brick Furnace, Campfire, Furnace |
| Steak | 1 | Brick Furnace, Campfire, Furnace |

### Cartographer

| Finished sample | Emeralds | Recipe method |
| --- | --- | --- |
| Cartography Table | 2 | Crafting |
| Compass | 3 | Crafting |
| Empty Map | 3 | Crafting |
| Glow Item Frame | 3 | Crafting |
| Item Frame | 2 | Crafting |
| Paper | 2 | Crafting |

### Cleric

| Finished sample | Emeralds | Recipe method |
| --- | --- | --- |
| Blaze Powder | 3 | Crafting |
| Brewing Stand | 3 | Crafting |
| Enchanting Table | 12 | Crafting |
| Eye of Ender | 5 | Crafting |
| Fermented Spider Eye | 3 | Crafting |
| Glistering Melon Slice | 3 | Crafting |
| Golden Carrot | 3 | Crafting |
| Magma Cream | 3 | Crafting |
| Respawn Anchor | 8 | Crafting |

### Farmer

| Finished sample | Emeralds | Recipe method |
| --- | --- | --- |
| Beetroot Soup | 2 | Crafting |
| Bread | 1 | Crafting |
| Cake | 3 | Crafting |
| Composter | 1 | Crafting |
| Cookie | 1 | Crafting |
| Hay Bale | 1 | Crafting |
| Melon | 1 | Crafting |
| Melon Seeds | 1 | Crafting |
| Mushroom Stew | 2 | Crafting |
| Pumpkin Pie | 2 | Crafting |
| Pumpkin Seeds | 1 | Crafting |
| Wheat | 1 | Crafting |

### Fisherman

| Finished sample | Emeralds | Recipe method |
| --- | --- | --- |
| Barrel | 1 | Crafting |
| Campfire | 2 | Crafting |
| Cooked Cod | 1 | Brick Furnace, Campfire, Furnace |
| Cooked Salmon | 1 | Brick Furnace, Campfire, Furnace |
| Fishing Rod | 2 | Crafting |

### Fletcher

| Finished sample | Emeralds | Recipe method |
| --- | --- | --- |
| Arrow | 2 | Crafting |
| Bow | 3 | Crafting |
| Crossbow | 4 | Crafting |
| Dispenser | 4 | Crafting |
| Fletching Table | 2 | Crafting |
| Spectral Arrow | 3 | Crafting |
| Stick | 2 | Crafting |
| Target | 3 | Crafting |

### Leatherworker

| Finished sample | Emeralds | Recipe method |
| --- | --- | --- |
| Book | 2 | Crafting |
| Bundle | 3 | Crafting |
| Cauldron | 3 | Crafting |
| Glow Item Frame | 3 | Crafting |
| Item Frame | 2 | Crafting |
| Leather Boots | 2 | Crafting |
| Leather Cap | 2 | Crafting |
| Leather Pants | 2 | Crafting |
| Leather Tunic | 2 | Crafting |

### Librarian

| Finished sample | Emeralds | Recipe method |
| --- | --- | --- |
| Book | 3 | Crafting |
| Book and Quill | 3 | Crafting |
| Bookshelf | 3 | Crafting |
| Cartography Table | 2 | Crafting |
| Enchanting Table | 12 | Crafting |
| Lectern | 3 | Crafting |
| Paper | 1 | Crafting |

### Mason

| Finished sample | Emeralds | Recipe method |
| --- | --- | --- |
| Block of Quartz | 3 | Crafting |
| Brick Slab | 2 | Crafting |
| Brick Stairs | 2 | Crafting |
| Brick Wall | 2 | Crafting |
| Bricks | 2 | Crafting |
| Chiseled Stone Bricks | 3 | Crafting |
| Cut Red Sandstone | 2 | Crafting |
| Cut Sandstone | 2 | Crafting |
| Polished Andesite | 2 | Crafting |
| Polished Andesite Slab | 2 | Crafting |
| Polished Andesite Stairs | 2 | Crafting |
| Polished Diorite | 2 | Crafting |
| Polished Diorite Slab | 2 | Crafting |
| Polished Diorite Stairs | 2 | Crafting |
| Polished Granite | 2 | Crafting |
| Polished Granite Slab | 2 | Crafting |
| Polished Granite Stairs | 2 | Crafting |
| Smooth Quartz Block | 3 | Furnace |
| Stone Brick Slab | 2 | Crafting |
| Stone Brick Stairs | 2 | Crafting |
| Stone Brick Wall | 2 | Crafting |
| Stone Bricks | 2 | Crafting |
| Stonecutter | 3 | Crafting |

### Shepherd

| Finished sample | Emeralds | Recipe method |
| --- | --- | --- |
| Black Bed | 2 | Crafting |
| Black Carpet | 1 | Crafting |
| Blue Bed | 2 | Crafting |
| Blue Carpet | 1 | Crafting |
| Brown Bed | 2 | Crafting |
| Brown Carpet | 1 | Crafting |
| Cyan Bed | 2 | Crafting |
| Cyan Carpet | 1 | Crafting |
| Gray Bed | 2 | Crafting |
| Gray Carpet | 1 | Crafting |
| Green Bed | 2 | Crafting |
| Green Carpet | 1 | Crafting |
| Light Blue Bed | 2 | Crafting |
| Light Blue Carpet | 1 | Crafting |
| Light Gray Bed | 2 | Crafting |
| Light Gray Carpet | 1 | Crafting |
| Lime Bed | 2 | Crafting |
| Lime Carpet | 1 | Crafting |
| Loom | 2 | Crafting |
| Magenta Bed | 2 | Crafting |
| Magenta Carpet | 1 | Crafting |
| Orange Bed | 2 | Crafting |
| Orange Carpet | 1 | Crafting |
| Painting | 2 | Crafting |
| Pink Bed | 2 | Crafting |
| Pink Carpet | 1 | Crafting |
| Purple Bed | 2 | Crafting |
| Purple Carpet | 1 | Crafting |
| Red Bed | 2 | Crafting |
| Red Carpet | 1 | Crafting |
| White Bed | 2 | Crafting |
| White Carpet | 1 | Crafting |
| Yellow Bed | 2 | Crafting |
| Yellow Carpet | 1 | Crafting |

### Toolsmith

| Finished sample | Emeralds | Recipe method |
| --- | --- | --- |
| Copper Axe | 3 | Crafting |
| Copper Hoe | 3 | Crafting |
| Copper Pickaxe | 3 | Crafting |
| Copper Shovel | 3 | Crafting |
| Diamond Axe | 10 | Crafting |
| Diamond Hoe | 8 | Crafting |
| Diamond Pickaxe | 12 | Crafting |
| Diamond Shovel | 8 | Crafting |
| Flint Multi-tool | 3 | Crafting |
| Golden Axe | 4 | Crafting |
| Golden Hoe | 3 | Crafting |
| Golden Pickaxe | 4 | Crafting |
| Golden Shovel | 3 | Crafting |
| Grindstone | 2 | Crafting |
| Iron Axe | 4 | Crafting |
| Iron Hoe | 3 | Crafting |
| Iron Pickaxe | 4 | Crafting |
| Iron Shovel | 3 | Crafting |
| Smithing Table | 2 | Crafting |
| Steel Axe | 7 | Crafting |
| Steel Hoe | 5 | Crafting |
| Steel Pickaxe | 7 | Crafting |
| Steel Shovel | 5 | Crafting |

### Weaponsmith

| Finished sample | Emeralds | Recipe method |
| --- | --- | --- |
| Copper Axe | 4 | Crafting |
| Copper Spear | 4 | Crafting |
| Copper Sword | 4 | Crafting |
| Crossbow | 4 | Crafting |
| Diamond Axe | 12 | Crafting |
| Diamond Spear | 14 | Crafting |
| Diamond Sword | 14 | Crafting |
| Flint Spear | 3 | Crafting |
| Golden Axe | 5 | Crafting |
| Golden Spear | 5 | Crafting |
| Golden Sword | 5 | Crafting |
| Grindstone | 2 | Crafting |
| Iron Axe | 5 | Crafting |
| Iron Spear | 5 | Crafting |
| Iron Sword | 5 | Crafting |
| Shield | 3 | Crafting |
| Steel Axe | 8 | Crafting |
| Steel Spear | 8 | Crafting |
| Steel Sword | 8 | Crafting |

### Configured lessons with unavailable default recipe references

These entries remain in the teaching data but their referenced recipe is absent from the bundled defaults. They are listed here so that a missing lesson does not become a mystery or an instruction to find a different Villager. This does not mean the item itself is removed: it means this particular configured lesson has an unresolved reference. Prices shown are configured prices, not payable working offers. No gameplay reproduction is claimed for these source-level gaps.

| Profession | Configured topic | Configured Emeralds |
| --- | --- | --- |
| Butcher | Rabbit Stew | 2 |
| Butcher | Suspicious Stew | 3 |
| Cartographer | Banner Pattern | 4 |
| Cartographer | Field Masoned Banner Pattern | 4 |
| Cartographer | Bordure Indented Banner Pattern | 4 |
| Farmer | Rabbit Stew | 3 |
| Mason | Polished Andesite Wall | 2 |
| Mason | Polished Diorite Wall | 2 |
| Mason | Polished Granite Wall | 2 |
| Shepherd | White Wool | 1 |
| Shepherd | Orange Wool | 1 |
| Shepherd | Magenta Wool | 1 |
| Shepherd | Light Blue Wool | 1 |
| Shepherd | Yellow Wool | 1 |
| Shepherd | Lime Wool | 1 |
| Shepherd | Pink Wool | 1 |
| Shepherd | Gray Wool | 1 |
| Shepherd | Light Gray Wool | 1 |
| Shepherd | Cyan Wool | 1 |
| Shepherd | Purple Wool | 1 |
| Shepherd | Blue Wool | 1 |
| Shepherd | Brown Wool | 1 |
| Shepherd | Green Wool | 1 |
| Shepherd | Red Wool | 1 |
| Shepherd | Black Wool | 1 |

Direct Campfire and Smithing discovery also need further verification. A recipe appearing here for a lesson is not evidence that merely cooking or upgrading automatically teaches it. Refer to chapter 3 for the reliable crafting, furnace-output, and lesson workflows.

## Appendix C Equipment and trade reference

### Current Flint and Steel values

These are current development values, not a final balance promise. Material mining speed is a base value, not a guaranteed time to break every block. Weapon attack attributes are not interchangeable with damage after armor, cooldown, movement, or other combat rules.

| Equipment | Durability or defense | Important properties |
| --- | --- | --- |
| Flint Multi-tool and Flint Spear | 48 base durability | Repair material: Flint; Multi-tool base mining speed 2 |
| Steel tools and weapons | 750 base durability | Base mining speed 7 for the material; repair with Steel |
| Steel Helmet | 275 durability; 3 armor points | 1 armor toughness |
| Steel Chestplate | 400 durability; 7 armor points | 1 armor toughness |
| Steel Leggings | 375 durability; 6 armor points | 1 armor toughness |
| Steel Boots | 325 durability; 3 armor points | 1 armor toughness |
| Unenchanted Diamond tools/Spear | 64 maximum | Any enchantment restores ordinary Diamond maximum durability |
| Unenchanted Diamond player armor | Helmet 66; Chestplate 96; Leggings 90; Boots 78 | Existing damage remains when enchantments change |
| Diamond Horse/Nautilus Armor | 96 before enchanting; 528 after | Takes actual wear from hits |

### Changed equipment trades

These are possible offers, not a guarantee that every individual Villager displays the entire list. Smith trade levels are Novice I, Apprentice II, Journeyman III, Expert IV, and Master V. The retained common buying offers still matter for earning Emeralds and leveling the profession.

The price column gives the configured base in Emeralds. Demand, reputation, and enchantment cost can affect the displayed price. Uses are the configured maximum trades per stock, not the number of items you can obtain forever. Professional Villagers refresh existing stock when the Overworld day advances; that refresh does not reroll their item selection.

| Profession and minimum trade level | Possible equipment | Base Emeralds | Uses per stock |
| --- | --- | --- | --- |
| Toolsmith Apprentice; Weaponsmith Apprentice | Copper Axe | 10 | 6 |
| Armorer Apprentice | Copper Boots | 8 | 6 |
| Armorer Apprentice | Copper Chestplate | 12 | 6 |
| Armorer Apprentice | Copper Helmet | 9 | 6 |
| Toolsmith Apprentice | Copper Hoe | 8 | 6 |
| Armorer Apprentice | Copper Leggings | 11 | 6 |
| Toolsmith Apprentice | Copper Pickaxe | 12 | 6 |
| Toolsmith Apprentice | Copper Shovel | 8 | 6 |
| Weaponsmith Apprentice | Copper Spear | 12 | 6 |
| Weaponsmith Apprentice | Copper Sword | 10 | 6 |
| Toolsmith Master; Weaponsmith Master | Diamond Axe | 60 | 1 |
| Armorer Master | Diamond Boots | 48 | 1 |
| Armorer Master | Diamond Chestplate | 64 | 1 |
| Armorer Master | Diamond Helmet | 52 | 1 |
| Toolsmith Master | Diamond Hoe | 48 | 1 |
| Armorer Master | Diamond Leggings | 60 | 1 |
| Toolsmith Master | Diamond Pickaxe | 64 | 1 |
| Toolsmith Master | Diamond Shovel | 48 | 1 |
| Weaponsmith Master | Diamond Spear | 60 | 1 |
| Weaponsmith Master | Diamond Sword | 56 | 1 |
| Toolsmith Master | Enchanted Diamond Axe | 64 | 1 |
| Armorer Master | Enchanted Diamond Boots | 64 | 1 |
| Armorer Master | Enchanted Diamond Chestplate | 64 | 1 |
| Toolsmith Master | Enchanted Diamond Pickaxe | 64 | 1 |
| Weaponsmith Master | Enchanted Diamond Spear | 64 | 1 |
| Weaponsmith Master | Enchanted Diamond Sword | 64 | 1 |
| Toolsmith Expert; Weaponsmith Expert | Iron Axe | 30 | 3 |
| Armorer Expert | Iron Boots | 24 | 3 |
| Armorer Expert | Iron Chestplate | 32 | 3 |
| Armorer Expert | Iron Helmet | 26 | 3 |
| Toolsmith Expert | Iron Hoe | 24 | 3 |
| Armorer Expert | Iron Leggings | 30 | 3 |
| Toolsmith Expert | Iron Pickaxe | 32 | 3 |
| Toolsmith Expert | Iron Shovel | 24 | 3 |
| Weaponsmith Expert | Iron Spear | 30 | 3 |
| Weaponsmith Expert | Iron Sword | 28 | 3 |
| Wandering Trader; uncommon offer | Enchanted Iron Pickaxe | 36 | 1 |

The Wandering Trader’s enchanted Iron Pickaxe has a configured base of 36 Emeralds before enchantment-related cost. It is a costly alternative, not a required first-day purchase. Steel is produced and crafted in the normal Retold progression; it is not inserted as another required smith trade tier.

## Appendix D Enchantment catalog without spell solutions

This index lists spell names, maximum levels, and table availability in the current defaults. It does not disclose the three-glyph words or translate their symbols. Knowing a name from this handbook does not make your character know its inscription.

A “Yes” still requires compatible equipment, an appropriate level, no conflicting enchantment, sufficient payment, and a genuine improvement. A “No” means learning the spell through an Anvil does not make the default table reproduce it. Other acquisition routes can differ; Mending is specifically removed from new random loot and Librarian trades.

| Enchantment | Maximum | Default table writable |
| --- | --- | --- |
| Aqua Affinity | 1 | Yes |
| Bane of Arthropods | 5 | Yes |
| Curse of Binding | 1 | No |
| Blast Protection | 4 | Yes |
| Breach | 4 | Yes |
| Channeling | 1 | Yes |
| Density | 5 | Yes |
| Depth Strider | 3 | Yes |
| Efficiency | 5 | Yes |
| Feather Falling | 4 | Yes |
| Fire Aspect | 2 | Yes |
| Fire Protection | 4 | Yes |
| Flame | 1 | Yes |
| Fortune | 3 | Yes |
| Frost Walker | 2 | No |
| Impaling | 5 | Yes |
| Infinity | 1 | Yes |
| Knockback | 2 | Yes |
| Looting | 3 | Yes |
| Loyalty | 3 | Yes |
| Luck of the Sea | 3 | Yes |
| Lunge | 3 | Yes |
| Lure | 3 | Yes |
| Mending | 1 | No |
| Multishot | 1 | Yes |
| Piercing | 4 | Yes |
| Power | 5 | Yes |
| Projectile Protection | 4 | Yes |
| Protection | 4 | Yes |
| Punch | 2 | Yes |
| Quick Charge | 3 | Yes |
| Respiration | 3 | Yes |
| Riptide | 3 | Yes |
| Sharpness | 5 | Yes |
| Silk Touch | 1 | Yes |
| Smite | 5 | Yes |
| Soul Speed | 3 | No |
| Sweeping Edge | 3 | Yes |
| Swift Sneak | 3 | No |
| Thorns | 3 | Yes |
| Unbreaking | 3 | Yes |
| Curse of Vanishing | 1 | No |
| Wind Burst | 3 | No |

## Appendix E Complete custom content catalog

### Items you can hold

The catalog contains 52 Retold item types. Vanilla items with changed roles, such as Heart of the Sea, Heavy Core, Lodestone, and the Brick Furnace, keep their Minecraft identities and are described in the relevant chapters. IDs below are primarily useful for operators and modpack authors.

| Name and item ID | Acquisition and role |
| --- | --- |
| Aender Boat — `retold:aender_boat` | Craft from Aender wood using Appendix A; follows its corresponding building/utility role. |
| Aender Button — `retold:aender_button` | Craft from Aender wood using Appendix A; follows its corresponding building/utility role. |
| Aender Cactus — `retold:aender_cactus` | Natural Aender desert plant; grows without the vanilla Cactus Flower output. |
| Aender Chest Boat — `retold:aender_chest_boat` | Craft from Aender wood using Appendix A; follows its corresponding building/utility role. |
| Aender Chronolith — `retold:aender_chronolith` | Creative/admin acquisition; Overworld experience-powered time control. |
| Aender Door — `retold:aender_door` | Craft from Aender wood using Appendix A; follows its corresponding building/utility role. |
| Aender Eye Spawn Egg — `retold:aender_eye_spawn_egg` | Creative or operator tools; creates the named creature. Not a survival recipe. |
| Aender Fence — `retold:aender_fence` | Craft from Aender wood using Appendix A; follows its corresponding building/utility role. |
| Aender Fence Gate — `retold:aender_fence_gate` | Craft from Aender wood using Appendix A; follows its corresponding building/utility role. |
| Aender Grass Block — `retold:aender_grass_block` | Natural Aender surface; Silk Touch keeps Grass, ordinary harvesting gives Soil. |
| Aender Hanging Sign — `retold:aender_hanging_sign` | Craft from Aender wood using Appendix A; follows its corresponding building/utility role. |
| Aender Leaves — `retold:aender_leaves` | Natural Aender trees; suitable Shears/Silk Touch collect Leaves; decay can supply Saplings. |
| Aender Log — `retold:aender_log` | Harvest Aender trees; craft Planks, grow more with Saplings. |
| Aender Planks — `retold:aender_planks` | Craft from Aender wood using Appendix A; follows its corresponding building/utility role. |
| Aender Pressure Plate — `retold:aender_pressure_plate` | Craft from Aender wood using Appendix A; follows its corresponding building/utility role. |
| Aender Sand — `retold:aender_sand` | Natural Aender terrain; harvest using suitable tools. |
| Aender Sandstone — `retold:aender_sandstone` | Natural Aender desert material; no current crafting recipe. |
| Aender Sapling — `retold:aender_sapling` | Aender Leaves drops; plant to grow renewable Aender trees. |
| Aender Sign — `retold:aender_sign` | Craft from Aender wood using Appendix A; follows its corresponding building/utility role. |
| Aender Slab — `retold:aender_slab` | Craft from Aender wood using Appendix A; follows its corresponding building/utility role. |
| Aender Soil — `retold:aender_soil` | Natural Aender terrain; harvest using suitable tools. |
| Aender Stabilizer — `retold:aender_stabilizer` | Creative/admin acquisition; experimental Aender 3×3-chunk stability field. |
| Aender Stairs — `retold:aender_stairs` | Craft from Aender wood using Appendix A; follows its corresponding building/utility role. |
| Aender Stone — `retold:aender_stone` | Natural Aender terrain; harvest using suitable tools. |
| Aender Trapdoor — `retold:aender_trapdoor` | Craft from Aender wood using Appendix A; follows its corresponding building/utility role. |
| Aender Wood — `retold:aender_wood` | Craft from Aender wood using Appendix A; follows its corresponding building/utility role. |
| Aenderite Ingot — `retold:aenderite_ingot` | Smelt/blast raw Aenderite; no current Ingot crafting uses. |
| Aenderite Ore — `retold:aenderite_ore` | Natural Aender islands; Netherite required for drops, Silk Touch to collect ore. |
| Air Element — `retold:air_element` | Legacy compatibility artifact; accepted by the egg, not the current guardian reward. |
| Animal Feeder — `retold:animal_feeder` | Craft five Planks; fill by interaction with suitable animal food. |
| Development Aender Portal Frame — `retold:dev_aender_portal_frame` | Mine embedded Aender deposits with Iron-tier or better correct Pickaxe; portal border. |
| Earth Guardian Spawn Egg — `retold:earth_guardian_spawn_egg` | Creative or operator tools; creates the named creature. Not a survival recipe. |
| Flint Multi-tool — `retold:flint_multi_tool` | Craft from Flint and a Stick; the opening harvesting tool. |
| Flint Spear — `retold:flint_spear` | Craft from Flint and two Sticks; an early Spear weapon. |
| Gale Core Spawn Egg — `retold:gale_core_spawn_egg` | Creative or operator tools; creates the named creature. Not a survival recipe. |
| Nether Reactor Core — `retold:nether_reactor_core` | Guaranteed Wildfire reward; Fire sacrifice. |
| Raw Aenderite — `retold:raw_aenderite` | Mine Aenderite Ore correctly; smelt/blast into an Ingot. |
| Steel Axe — `retold:steel_axe` | Blast Iron for the Ingot; craft Steel equipment using Appendix A. |
| Steel Boots — `retold:steel_boots` | Blast Iron for the Ingot; craft Steel equipment using Appendix A. |
| Steel Chestplate — `retold:steel_chestplate` | Blast Iron for the Ingot; craft Steel equipment using Appendix A. |
| Steel Helmet — `retold:steel_helmet` | Blast Iron for the Ingot; craft Steel equipment using Appendix A. |
| Steel Hoe — `retold:steel_hoe` | Blast Iron for the Ingot; craft Steel equipment using Appendix A. |
| Steel Ingot — `retold:steel_ingot` | Blast Iron for the Ingot; craft Steel equipment using Appendix A. |
| Steel Leggings — `retold:steel_leggings` | Blast Iron for the Ingot; craft Steel equipment using Appendix A. |
| Steel Pickaxe — `retold:steel_pickaxe` | Blast Iron for the Ingot; craft Steel equipment using Appendix A. |
| Steel Shovel — `retold:steel_shovel` | Blast Iron for the Ingot; craft Steel equipment using Appendix A. |
| Steel Spear — `retold:steel_spear` | Blast Iron for the Ingot; craft Steel equipment using Appendix A. |
| Steel Sword — `retold:steel_sword` | Blast Iron for the Ingot; craft Steel equipment using Appendix A. |
| Stripped Aender Log — `retold:stripped_aender_log` | Strip with an appropriate Axe; stripped Wood also has a recipe. |
| Stripped Aender Wood — `retold:stripped_aender_wood` | Strip with an appropriate Axe; stripped Wood also has a recipe. |
| Water Element — `retold:water_element` | Legacy compatibility artifact; accepted by the egg, not the current guardian reward. |
| Wildfire Spawn Egg — `retold:wildfire_spawn_egg` | Creative or operator tools; creates the named creature. Not a survival recipe. |

### Placed states without separate custom items

These account for the remaining registered blocks. A wall form is normally placed using its ordinary item, and an extinguished torch is created by weather rather than a separate Survival crafting recipe.

| Block ID | Meaning |
| --- | --- |
| `retold:aender_portal` | Live noncolliding portal surface created by completing the horizontal border. |
| `retold:aender_wall_hanging_sign` | Wall-mounted Aender Sign or Hanging Sign state, placed using the corresponding sign item. |
| `retold:aender_wall_sign` | Wall-mounted Aender Sign or Hanging Sign state, placed using the corresponding sign item. |
| `retold:extinguished_copper_torch` | Unlit weather state of the corresponding normal, Soul, or Copper floor/wall torch. |
| `retold:extinguished_copper_wall_torch` | Unlit weather state of the corresponding normal, Soul, or Copper floor/wall torch. |
| `retold:extinguished_soul_torch` | Unlit weather state of the corresponding normal, Soul, or Copper floor/wall torch. |
| `retold:extinguished_soul_wall_torch` | Unlit weather state of the corresponding normal, Soul, or Copper floor/wall torch. |
| `retold:extinguished_torch` | Unlit weather state of the corresponding normal, Soul, or Copper floor/wall torch. |
| `retold:extinguished_wall_torch` | Unlit weather state of the corresponding normal, Soul, or Copper floor/wall torch. |

### Custom entities

| Entity ID | Role |
| --- | --- |
| `retold:aender_eye` | Passive watching/avoiding Aender flyer; no current loot reward |
| `retold:gale_core` | Air Temple boss; Heavy Core reward |
| `retold:wildfire` | Roaming Nether miniboss with Blaze escorts; Nether Reactor Core reward |
| `retold:earth_guardian` | Blind, terrain-changing labyrinth guardian; Lodestone reward |
| `retold:aender_boat` | Boat entity created by using the Aender Boat item |
| `retold:aender_chest_boat` | Chest Boat entity created by using the Aender Chest Boat item |

The six entity registrations include two boats, not six new living species. Ordinary Minecraft creatures with Retold behavior remain their original entity types.

## Appendix F Troubleshooting

### I break trees but collect no Logs

Use a correct harvesting tool such as the Flint Multi-tool. Punching can break a Log without giving its drops. Gather Flint and Sticks first, and check that the tool has not broken.

### The Campfire will not cook

Placed Campfires begin unlit. Use bare Flint or a suitable igniter, then put a valid input on the cooking surface. A waterlogged or otherwise invalid Campfire cannot simply be lit. Wait for the output and collect it when it pops out.

### I have Bricks but the furnace recipe fails

Use eight individual Brick items around an empty center, not eight Bricks blocks. This makes the Brick Furnace. A normal Furnace instead uses accepted stone material such as Cobblestone.

### I mine deep ore and nothing useful drops

Check the harvesting tier. Deepslate and Diamond Ore require Steel or an appropriate later tool. Steel still cannot collect Obsidian/Ancient Debris, and Aenderite Ore needs Netherite. Slow breaking does not prove a tool is correct for drops.

### A Villager will not teach the item

Show the finished item in the added teaching slot, choose a suitable profession, keep enough Emeralds in your inventory, and check whether you already know it. Some configured lessons have unresolved recipe references; Appendix B marks them. A normal trade slot and an ingredient sample ask different questions.

### I own an enchanted book but the spell is unknown

Ownership is not learning. Successfully transfer or improve the book’s compatible enchantment at an Anvil. Merely holding it, viewing it, or combining without a useful transfer does not teach the spell.

### A known spell is missing from the table list

Check the inserted item, existing enchantments, conflicts, maximum level, and table eligibility. A treasure spell can be known but unavailable at the table. Appendix D lists default table availability.

### The table rejects an inscription without saying why

That generic failure is intentional. Check the three filled slots, level, target compatibility, three Lapis, required experience levels, and whether the cast improves the item. Unknown words are not given a free validity preview. Failed casts preserve resources.

### My animals ignore the food or feeder

Check the species diet, the feeding route, physical access, and danger. A feeder can accept a food for one species while the animals beside it cannot eat it. Fish and several specialists do not use the ordinary land feeder. Hand-feeding items are not identical to the expanded dropped-food list.

### My animals will not breed

Supported adults need five calm, well-fed minutes and compatible ready partners nearby. Feeding once is not immediate love mode. Hunger, attacks, panic, age cooldowns, lack of a partner, or inaccessible food can interrupt progress.

### An enclosure was emptied or a fence broke

Animals can deplete forage and migrate; desperate eligible ground animals or hunting wild predators can break weak wooden barriers. Check food supply, predator access, and stronger barrier choices. A named pet also needs food while loaded.

### The village is angry even though I only took food

Village-produced or generated supplies can be owned property. Witnessed withdrawal, crop damage, or livestock killing can affect reputation. Your own deposited quantities are separate, but that does not make the village’s original stock yours.

### A guarded structure attacked after I opened a chest

Territory suspicion and village ownership are different rules. Opening listed containers can itself count as a territorial offense, even without taking an item. Prior suspicion persists, and direct harm can bypass warnings. Withdraw before escalation rather than expecting a fresh grace period.

### The Elder Guardian takes no damage

Check whether it is in water. Its Water-path defense blocks attacks while wet. Draining or exposing it and dealing with the ordinary Guardians is the challenge; repeated underwater hits are not the solution.

### The egg ignores my artifact

The world must be in Stage 2, the egg must be placed, and you must use the correct held artifact directly. Duplicates are unnecessary and rejected. Consult the six-item checklist; an altar pattern or throwing items on the ground does not replace interaction.

### My Aender portal does not activate or I fall through it

Use the Development Aender Portal Frame, complete the flat border including corners, and leave a valid interior. Put a solid floor under the noncolliding field. Remain in it for the travel charge; Aender entry can wait for destination preparation. Travel requires Stage 3.

### My Aender construction disappeared

Unstable regions can regenerate when no longer watched or after the last player travels out. The initial arrival platform is separately rebuilt and cleared on each redirected entry. Saving alone does not make every unstable building permanent, and another player elsewhere does not protect the whole dimension.

### I cannot find an Outpost or a removed modern structure

Outposts currently wait for Stage 3. Air Temples and Mansions become available in Stage 2. Fresh ordinary Ancient City, Trial Chamber, and End City generation is disabled; old terrain or datapacks can differ. A missing structure is not always bad luck.

### The command is missing or rejected

Check permission level, spelling, player-only requirements, and the current dimension. Most behavior commands also require an eligible looked-at creature. A gallery needs an empty, fully active footprint. Command output is more useful than repeatedly trying the same invalid situation.

## Further reading and support

Retold is independently developed by Xefensor. It is not an official Minecraft product, and its setting is independent fiction rather than official Minecraft lore.

The handbook describes the current development source and bundled defaults. Different releases, older worlds, datapacks, or server settings can change the experience. Natural encounter balance, connected multiplayer, existing-world progression, and final visuals remain incompletely verified. No new gameplay tests are claimed by the preparation of this documentation.

Project references:

- [Installation and project overview](../README.md)
- [Dragon Egg ritual and Earth path](element-ritual.md)
- [Test gallery instructions](test-world.md)
- [Mod and datapack compatibility](compatibility.md)
- [Changelog](../CHANGELOG.md) and [public roadmap](../ROADMAP.md)
- [Detailed implementation status](internal/design_implementation_status.md)
- [Equipment progression](internal/tool_armor_ore_progression.md)
- [System reference](internal/retold_mod_system.md) and [mob behavior reference](internal/retold_mob_ai_system.md)

Official downloads and support: [GitHub](https://github.com/xefensor/Retold), [Modrinth](https://modrinth.com/mod/retold), [CurseForge](https://www.curseforge.com/minecraft/mc-mods/retold), and [Discord](https://discord.gg/S3g98zEY8a). Report reproducible problems through the project's [issue tracker](https://github.com/xefensor/Retold/issues).

This handbook was prepared with AI assistance and checked against the current source and resource catalogs. Source-level uncertainty is identified beside the affected feature; it is not a claim of a completed gameplay test.
