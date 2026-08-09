# Changelog

All notable player-facing and technical changes should be tracked here.

Each release should be readable in two passes:

- **Player-facing:** what a player should notice.
- **Technical:** what changed in systems, data, commands, docs, or implementation behavior.

## Next - Unreleased

### Player-Facing

- Ruined Nether portals no longer generate with loot chests.
- Unknown enchantments on items and enchanted books now show only their three-glyph SGA inscription and level. After a player learns a spell by successfully transferring it from a book, its ordinary readable name returns and the SGA word remains directly below it. Multi-enchanted items reveal each spell independently.
- The enchanting table now replaces random bookshelf-powered offers with deterministic spell writing. Insert an item and three lapis, enter three SGA glyphs, choose level I-V, and write the enchantment for five experience levels per requested enchantment level. The editor supports physical A-Z, Backspace, number-row/keypad 1-5, and Enter controls, with duplicate submission blocked while a cast is pending. Invalid or incompatible attempts consume nothing and share one low-note/red-highlight rejection cue without revealing why they failed. Learned table-eligible spells appear in a paginated item-aware list with maximum levels, can refill their glyph word, and disable unsupported levels. A successful cast clears the inscription and briefly highlights the changed item; successfully deducing a new valid word teaches it.
- Refined the villager recipe-teaching interface with a dedicated emerald-toned panel, consistent spacing, a centered teaching slot, wrapped status and guidance text, aligned cost and action controls, and clear ready/success/rejection highlights. Enter or keypad Enter can activate an available lesson, successful teaching plays a Villager confirmation sound, and rejected stale requests play the Villager refusal sound.

### Technical

- Added a ruined-portal placement processor that omits template chests without affecting chests or other containers elsewhere, with focused GameTest coverage.
- Added the first Retold enchanting-learning route: completing an anvil operation with an enchanted book now records every book enchantment that actually transferred to or improved the output. Multi-enchantment books update persisted per-player knowledge in one synchronized transaction; incompatible and unchanged enchantments remain unknown. All 43 currently registered enchantments now have unique three-concept words drawn from a fixed 26-concept SGA vocabulary. The validated datapack spell catalog synchronizes to clients on join and reload, while both client catalog and knowledge snapshots clear on disconnect. A client tooltip transformer preserves unrelated and unmapped lines while applying knowledge-aware SGA presentation. Added a server-authoritative table request/menu transaction with active-container validation, atomic costs, vanilla enchantment eligibility/compatibility, plain-book conversion, synchronized item/lapis slots, and successful-cast learning. Vanilla random offer buttons and bookshelf power are disabled; anvil results and costs remain unchanged.
- Replaced the teaching panel's disabled-button backdrop with code-rendered panel and slot surfaces. Preview text now remains as translatable components across the version-2 network protocol, and explicit server outcomes drive client feedback. Client and server duplicate guards prevent repeated payment, learning, or Villager XP from rapid requests; focused GameTests cover transaction edge cases and payload round trips. Fixed a client crash caused by trying to inject into `MerchantScreen`'s inherited `containerTick` method; the feedback timer now uses a valid override.

## 0.4.0 - 2026-08-05

### Player-Facing

- Cows and mooshrooms now share one bovine herd-range identity, while horses/donkeys/mules and llamas/trader llamas retain their confirmed mixed groups. Cod, Salmon, Tropical Fish, and Pufferfish now pull isolated members back toward exact-species schools through aquatic paths. Squid and Glow Squid copy nearby panic only from their own exact species.
- Sheep, other ordinary livestock, small foragers, and fish now immediately run after taking damage from any source, including attacks by Zombies or players. Combat-capable mobs and species with specialized defense behavior keep their own reactions.
- Fixed extinguished soul and copper wall torches dropping nothing when broken.
- Mobs no longer deliberately target or melee creepers.
- Cats now hiss and retreat from nearby creepers. When a creeper's fuse starts, mobile mobs react after a short awareness-dependent delay and flee even if that interrupts combat or guard duty; zombie-family mobs deliberately hold their ground. Creepers retain their vanilla avoidance of cats and ocelots.
- Undead, slimes, and monument guardians now recognize living creatures outside the named faction table according to their confirmed hostility rules; the Wither now uses Undead diplomacy.
- Disabling `mobGriefing` now prevents animals from consuming forage blocks and prevents Gale Core attacks from cracking or breaking terrain. Animals can still eat dropped food.
- Witches and Illagers now remain mutually neutral outside raids. A witch only answers support calls from Illagers participating in the same active raid and drops Retold-owned raid-assist targets when it leaves.
- Raids can now begin only in Stage 3. Before then, entering a village keeps Bad Omen instead of converting it into Raid Omen, and direct vanilla raid creation is rejected; raids already underway are allowed to finish.
- Desperate animals can now break wooden fences and closed wooden fence gates after six seconds; actively hunting wild predators take three seconds. Breaches show cracking, drop the barrier normally, and have a ten-second cooldown. Tamed predators do not breach, and `mobGriefing=false` disables the behavior.
- Vex direct hits now deal half of their otherwise calculated damage.
- Professional adult villagers now refresh their existing trade stock when a new Minecraft day begins, even without rerolling their offers. Wandering Traders remain independent.
- Hungry villagers now use all accessible chests and barrels inside their remembered village as communal food stores, regardless of how the container was generated or placed. They eat their highest-value carried food before searching storage. When empty, they walk to a supported side of the nearest valid store and withdraw up to 12 vanilla food points—three Bread or twelve vegetables—then eat one item and retain the rest for later meals. Dispensers and other machine inventories are ignored, and danger, sleep, and trading retain priority.
- Adult Farmers now stock those communal chests and barrels with surplus Bread, Carrots, Potatoes, and Beetroot produced through their normal farming and bread-making behavior. They retain a personal food reserve, never deposit seeds or unrelated items, and yield to hunger, danger, sleep, and trading.
- Shepherds now tend Sheep and Goats, Leatherworkers tend Cows and Mooshrooms, and Butchers tend Pigs, Chickens, and Rabbits. An eligible Villager collects and consumes two suitable food items from village chest/barrel storage, approaches a valid adult pair, and relieves their hunger. Successfully tended adults and eligible offspring become persistent village livestock; player-handled, tamed, and otherwise unsupported animals are not newly claimed. A witnessed Survival player kill of village livestock causes a strong reputation loss, while monster, environmental, Creative, and Spectator deaths do not.
- Breedable animals no longer enter love mode immediately when a player or Villager feeds them. Every supported adult instead becomes ready after remaining fully satisfied for five loaded minutes; two compatible ready animals within eight blocks then use their normal vanilla mating and offspring behavior automatically. Hunger, panic, damage, or active danger resets readiness, and successful breeding costs each parent 40 hunger while retaining vanilla's five-minute parent cooldown. Readiness persists across save/reload, but unloaded time does not advance it.
- Every mob with active Retold hunger now takes one point of starvation damage whenever its species-specific hunger interval elapses at 100 hunger, including Bats and Villagers. Starvation can kill them, and feeding below 100 immediately stops further pulses. Slimes and Magma Cubes retain their existing critical-hunger split-or-die behavior instead of taking the generic damage.
- Armadillos and Turtles now participate in Retold hunger and seek their supported food. Predator kills now count as meals, including vanilla Frog tongue and Axolotl attacks. Bees and Sniffers keep their dedicated flower/ground-foraging ownership instead of wandering under the generic food-search fallback, and Sniffers can finish approaches to diggable ground with their large body.
- Hungry Pandas now actually consume and remove the bamboo block they forage from instead of repeatedly receiving food from an unchanged stalk. Bamboo produces no dropped item when eaten, hunger relief is granted only after successful removal, and `mobGriefing=false` prevents both the block change and the meal.
- Hungry Armadillos now dig suitable exposed soil for grubs without removing the ground, wild Nautiluses hunt living fish, and lava sustains Striders while Warped Fungus remains fallback food. Piglins receive food from Hoglins they kill, hungry undead receive food from non-undead victims, and Slimes/Magma Cubes receive food from non-Cube victims; Creepers and same-family kills never count as meals.
- Every active hunger species now has a production feeding route in a representative natural spawn habitat. Camels and desert Rabbits browse safe desert scrub while cactus remains a hazard, Goats scrape alpine stone/snow/gravel, Mooshrooms graze mycelium, and Armadillos can find grubs in red sand and terracotta. Piglins can forage Nether mushrooms, Cats can hunt wetland Frogs, Ocelots can use jungle Chickens, cave Spiders can hunt Bats, Bats catch abstract cave insects at night, linked Trader Llamas draw sustenance from their caravan, and hungry Slimes/Magma Cubes independently notice nearby valid prey when no swarm target exists. Renewable habitat forage is not destroyed and remains usable when `mobGriefing=false`.
- In every world stage, adult Villagers now relight weather-extinguished torches within eight blocks of themselves and within their village. Most professions stop, face the torch, and use a short ranged magical cast. Nitwits cannot use magic: they walk to a supported adjacent cell and visibly hold and use a temporary Flint and Steel for the full close interaction; it is never added to inventory and consumes no item or durability. Both methods preserve the normal/soul/copper and floor/wall variant and wait until precipitation can no longer reach it. Hunger, danger, sleep, trading, higher-priority work, and `mobGriefing=false` prevent or interrupt maintenance.
- Warm ocean ruin archaeology no longer yields Sniffer Eggs, removing Sniffers from ordinary survival progression while keeping them functional through commands and Creative.
- Ender Pearls and ordinary mob-spawn paths no longer create Endermites. Existing Endermites and those created through commands or Creative remain functional.
- All snowballs now deal one point of damage, including player-thrown, mob-thrown, and dispenser-style shots against Creepers. Their vanilla three-damage Blaze interaction remains unchanged.
- Silverfish and Endermites are now separate neutral factions and never join each other's swarm attacks. Each species still coordinates with its own kind and retains its existing enemies.
- Endermen now retaliate against any valid living attacker. Before Stage 3 only the victim responds; in Stage 3, nearby idle Endermen within 32 blocks join its defense. Endermen still never initiate combat with the Dragon, and the global Creeper and invalid-player target protections still apply.
- Axolotls no longer choose ordinary Guardians as random prey. An axolotl fights a Guardian only after being attacked or when it witnesses a nearby axolotl being attacked, and defensive bites do not count as feeding.
- Adult Polar Bears with nearby cubs now stand and sound a two-second warning before attacking a passive intruder. Retreating cancels the confrontation, while attacking the bear or cub triggers immediate defense.
- Hungry Spiders and Cave Spiders hunt full-sized passive animals only at night and may join one another's nearby hunts. Daylight ends those food hunts, while retaliation remains immediate at any time and proactive aggression toward players remains darkness-based.
- Recently fed Spiders and Cave Spiders can establish shared webbed lairs in genuinely dark, sheltered spaces with low raw skylight; ordinary outdoor nighttime darkness does not qualify. They slowly place or repair up to 50 persistent cobwebs, return to the lair during daylight, and immediately abandon that return to retaliate. Cobweb construction respects `mobGriefing`.
- Touching idle Slimes of the same size can now merge from size 1 to 2 and from size 2 to 4. Magma Cubes follow the same rule with other Magma Cubes, but the two species never merge together. The survivor waits 30 seconds before it can merge again; named, leashed, mounted, and fighting creatures are excluded.
- Slimes and Magma Cubes now attack only after reaching their configured hunt-hunger threshold. Feeding below it makes them abandon combat, and fed Cube Mobs cannot target or damage players, Iron Golems, or ordinary creatures. Hungry size-one Cube Mobs can now deal contact damage instead of being harmless.
- Larger Slimes and Magma Cubes now gain hunger faster without starving too quickly: hunger rises by one point per two size levels each interval, rounded up. Whenever a Cube Mob splits, every child inherits half of the parent's hunger. At 100 hunger, it splits into two half-size children at 50 hunger each; a size-one Cube Mob starves to death because it cannot split again. Starvation splits preserve swallowed items without duplication and cannot immediately merge back together.
- Slimes and Magma Cubes now seek and swallow dropped items even when completely fed, while living-prey hunting and contact damage still require their configured hunger threshold. They can swallow any entire dropped-item stack, preserve and return every swallowed item when killed, and retain components such as tool damage. Eating can grow them one size at a time up to size 10, but each step costs twice as much as the previous one: 16 items for size 2 through 4,096 for size 10, or 8,176 items total.
- Fixed Slimes and Magma Cubes recognizing distant dropped items without moving toward them. Their item pursuit now drives the specialized Cube Mob facing-and-hop controller instead of unsupported ordinary path navigation.
- Hunger-driven mobs now prefer suitable dropped food items over hunting living prey. They abandon ordinary hunts when reachable food is available, but do not abandon retaliation or territory defense to eat.
- Predators now abandon ordinary food hunts once feeding drops them below their hunt threshold. A satisfied Wolf leading an active pack hunt or search hands leadership to an available hungry member, while satisfied members stop scouting for or validating food prey; retaliation and territory defense remain protected.
- Bats now have hunger and form persisted colonies around genuinely dark, sheltered roost areas. During the day, awake Bats search upward across tall caves for individual supported ceiling cells, return along obstacle-aware three-dimensional routes, and settle after short staggered delays; they no longer mistake air below the cave floor for a roost. At night, hungry Bats prefer dropped Spider Eyes, then form hunting parties of up to five. A party chooses a shared direction for about 20 seconds, flies a loose parallel formation instead of wandering independently, and shares detected arthropod prey. Their searches, one-damage attacks, combat dodges, and selective danger reactions remain path-backed and locally separated.
- Fixed daytime Bats circling just below their chosen roost instead of sleeping. The return route now owns flight through a collision-safe final ceiling approach, then holds the Bat in place during its short settling delay so vanilla wandering cannot pull it away. Sleeping and settling Bats also keep their ceiling cells reserved, while Bats already stacked in one cell wake and move apart.
- Disturbing a sleeping Bat now starts ten seconds of owned panic flight instead of allowing it to settle again almost immediately. Player-proximity wake-ups, broken roost supports, and unrelated damage all interrupt pending shelter and settling behavior for the full recovery window.
- Bat colonies now spread their expensive work over time. High-ceiling searches stop as soon as a suitable personal column is found, travelling Bats retain their chosen ceiling and path, and each hunting party performs one shared decision instead of every member repeating the same scans and route updates.
- Hungry managed mobs now actively travel through their environment to search for food instead of waiting for food to enter their immediate scan radius. Ground search uses reachable vanilla navigation paths; predators retain their prey-search systems, and dropped edible items still take priority once found.
- Added a wooden Animal Feeder crafted from five planks. It stores one compatible stack: right-click inserts one food item, sneak-right-click with compatible food inserts as much of the held stack as fits, and sneak-right-click with an empty hand or incompatible item retrieves the stored stack. Hungry managed land animals, including livestock, pets, and eligible wild animals, path to the trough and consume one suitable item at a time. Aquatic mobs, Villagers, hostile monsters, Slimes, and Magma Cubes ignore it. Feeding remains available with `mobGriefing=false`.
- Fixed Sheep and other ground animals stopping one node short of an Animal Feeder. Feeder approaches now require the selected supported cell beside the trough instead of accepting vanilla navigation's ordinary one-block reach tolerance.
- Fixed Animal Feeders duplicating transferred food in Survival. Insertion copies only into feeder storage and removes exactly the accepted count from a Survival player's actual held stack. Creative insertion follows normal Creative behavior and leaves the held stack unchanged.
- Mobs now stop for two seconds and turn toward the actual food source after feeding. This shared pose covers dropped items, forage, Animal Feeders, held food, flowers, bamboo, prey bites and kills, Bat feeding, Sniffer forage, and Slime/Magma Cube swallowing; urgent danger, defense, combat, and territory duties still interrupt it.
- Mobs no longer keep eating while fleeing or performing another urgent duty. In particular, a Sheep that has already started vanilla grazing immediately abandons it when predator flight takes control, without eating the grass later; feeding resumes normally after danger ends.
- Hungry Striders now live off lava. Standing on or in lava passively relieves two hunger every ten seconds without consuming the lava, and a sustained Strider no longer abandons it for an ordinary autonomous food search. Warped Fungus remains valid food away from lava and for player interaction.
- Village chests and barrels now distinguish village property from player deposits. Unopened generated village loot and future Farmer/Villager deposits are village-owned, while player-added quantities remain safe to retrieve even after merging into a matching stack. Villagers who witness a Survival player take protected items react through vanilla reputation and trade-price gossip; breaking protected storage is severe enough to provoke vanilla Iron Golems. Creative and Spectator players are ignored, and ambiguous already-opened existing-world contents are not retroactively claimed. Operators can use `/retold village status` to summarize the executing player's standing among loaded village-context Villagers within 32 blocks, including average/worst/best reputation and possible golem hostility.
- Crops planted or replanted by Farmers are now persistently treated as village property while they grow. Player-planted crops remain the player's property. A witnessed mature harvest is minor theft; breaking an immature Farmer crop or trampling its farmland is stronger vandalism. Creative and Spectator players are ignored, and existing ambiguous crops are not retroactively claimed.

### Technical

- Added an at-transaction meal-priority guard shared by ordinary item/forage feeding, hunting meals, and specialized Panda, Sniffer, Armadillo, Frog, Axolotl, Bat, Bee, and Trader Llama paths. `EatBlockGoalMixin` applies the same ownership rule to vanilla grazing. The exact `retold:fleeing_sheep_cannot_eat_until_danger_ends` GameTest passes 1/1; broader GameTest and TPS matrices were not selected because the fix adds only constant-time ownership checks and the exact shared regression covers the changed contract.

- Added persisted `RetoldAnimalBreeding` state and a data-driven `retold:automatic_breeders` entity tag covering all 26 current vanilla breedable animal types. A bounded 20-tick dispatcher check arms compatible ready pairs while vanilla remains responsible for mate movement, genetics, tame ownership, Turtle eggs, Frog pregnancy, and other special births. Direct feeding now only applies the species' existing hunger relief; the interaction hook remembers the exact food before vanilla consumes it so feeding the final held item retains its correct value. Readiness stores accumulated loaded ticks instead of a world-time start, so unloaded gaps cannot complete it. Four focused GameTests cover tag/profile coverage, real player feeding including a one-item stack, sustained readiness, unloaded-gap persistence, interruption, retry state, Horse/Donkey compatibility, actual vanilla offspring creation, and parent hunger cost. Strider and Nautilus hunger profiles expand the TPS registry to 77 species; focused Cow, Strider, and Nautilus runs keep all 15 phases below 50 ms/tick, and the final 256-mob bounded-work test averages 17.082 ms/tick.
- Added `RetoldStarvationBehavior` as the shared critical-hunger owner for ordinary `PathfinderMob` profiles, the separate non-pathfinding Bat path, and the Villager communal-food path. Two isolated GameTests assert that every loaded positive-hunger profile reaches one of those owners and cover first critical damage through all three, terminal death, and exclusion of profiles with hunger disabled; the existing Cube Mob critical-hunger regression preserves splitting. Focused Cow, Bat, and Villager TPS reruns pass all 15 phases below 50 ms/tick, peaking at 6.852, 11.856, and 10.129 ms/tick respectively. The complete GameTest suite and 77-profile TPS matrix were not rerun because the focused selectors cover every hunger owner without changing shared dispatch, scans, paths, caches, or persistence.
- Added `RetoldHungerSurvivalGameTests`: 40 isolated species habitats require every positive-hunger profile to stay alive and lower hunger through production feeding, while a 41st registry guard prevents uncovered profiles. The matrix supplies live Sheep for Wolves and equivalent species-appropriate prey, forage, dropped food, aquatic habitat, and Villager storage. It exposed and fixed missing ordinary-predator kill relief, vanilla Frog/Axolotl kill credit, Sniffer/Bee ownership conflicts, large-Sniffer ground approach, and disabled Armadillo/Turtle feeding thresholds. The focused matrix passes 41/41. Armadillo and Turtle TPS pass all ten affected phases below 50 ms/tick, peaking at 9.776 and 10.744 ms/tick; no complete GameTest or TPS suite was selected.
- Upgraded the 41-case hunger-survival matrix so every species uses a real habitat source or living prey instead of a dropped-item substitute. The spawn-biome audit added renewable desert scrub, alpine, and mycelium forage; broader badlands and Nether substrates; cave-insect Bat sustenance; caravan Trader-Llama sustenance; and local prey acquisition for hungry Cube Mobs. Shared block-search capacity remains capped at eight starts per tick, but a weak fair-claim queue now prevents one repeatedly deferred environmental forager from starving behind the same seven earlier claimants; a deferred forager waits for its turn instead of wandering away from an already-present source. Two final different-order matrix runs pass 41/41. Bat, Camel, Rabbit, Slime, and Magma Cube TPS checks pass all 25 phases below 50 ms/tick, peaking at 8.276, 10.061, 4.292, 5.243, and 5.305 ms/tick respectively; the unrelated complete GameTest and 77-profile TPS suites were intentionally not run.
- Added two isolated Panda bamboo GameTests covering natural approach, hunger relief, exact block consumption, and the `mobGriefing=false` denial path. The focused group passes 2/2, the existing Panda hunger-survival case passes 1/1, and all five Panda TPS phases remain below 50 ms/tick with a 9.305 ms/tick peak. The complete GameTest and TPS suites were not selected for this species-local transaction.
- Added bounded cached Armadillo grub-soil discovery, Nautilus participation in controlled fish hunting, passive Strider lava sustenance with warped-fungus fallback, and death-event meal credit for Piglins, hungry undead, Slimes, and Magma Cubes. `retold:natural_food_*` passes 5/5, the exact Armadillo/Nautilus/Strider survival cases pass, and their latest affected 50-mob TPS checks peak at 5.760, 4.544, and 6.771 ms/tick respectively. The complete suites were not selected because focused acquisition, survival, exclusion, and affected hot-path coverage exercise the changed contracts.

- Added `AQUATIC_SCHOOL` and `LOOSE_AQUATIC_GROUP` profiles for four fish and both Squid types, bringing the data-driven matrix to 74 mobs and 370 phases. Fish cohesion uses central dispatch, `AQUATIC_SCHOOL` control ownership, cached exact-species scans, and real aquatic navigation. Successful Squid damage performs one bounded cached same-species panic broadcast before the existing receiver-side fallback. Three ecology GameTests cover land social groups and persisted ranges, exact-species fish routing, and Squid/Glow Squid panic isolation. The focused ecology batch passes 3/3, the expanded TPS matrix passes 74/74 with a 7.516 ms/tick overall peak, and the complete suite passes 150/150. The developer also reports that the ordered natural mixed-pen and water-terrain acceptance pass works.
- Routed successful damage into the shared remembered-flee owner for passive land and aquatic prey. Source entities, projectile/explosion positions, and source-less environmental damage all produce an escape direction; an integrated GameTest covers mob, player, environmental, aquatic, and predator non-regression cases, and the per-mob danger benchmark now applies real damage to shared-flee profiles.
- Added GameTest coverage for the drops of all six extinguished torch variants.
- Added a global mob-target safety policy and GameTest coverage for creeper protection and indiscriminate faction relationships.
- Added cached creeper awareness through the central behavior dispatcher, high-priority flee ownership for both pathfinding and flying mobs, remembered fuse danger, and GameTests for cats, animals, village defenders, zombies, and ghasts.
- Centralized Retold block-modification permission through NeoForge's entity-griefing hook, with GameTests for animal forage, Gale Core block damage, and vanilla creeper explosions.
- Separated permanent loose-alliance identity from active combat alignment and same-raid cooperation, with GameTest coverage for Witch/Illager neutrality, territory exclusion, raid assistance, and raid-exit cleanup.
- Added an authoritative Stage 3 raid-start policy, a Bad Omen conversion guard, and a narrow guard around vanilla raid creation. GameTest coverage calls the live vanilla creation path before Stage 3.
- Added territory GameTests covering all four configured structure tags, every Nether Remnant and Illager warning member, creative/spectator exclusion, warning-stage target suppression, final-warning escalation, target ownership, and immediate retaliation.
- Added data-driven weak-barrier breaching through the central AI dispatcher, control ownership, bounded cached block searches, and shared mob-griefing policy. GameTests cover vanilla and Aender tag membership, open-gate exclusion, both timing classes, normal drops, cooldown, tamed predators, and disabled mob griefing.
- Added an incoming-damage owner for the Vex nerf and a GameTest that verifies exact direct-hit health loss.
- Added persisted once-per-day villager stock refresh through the central behavior dispatcher, with open-menu synchronization and GameTest coverage for same-day limits and retained offer identity.
- Added a versioned server-global village-container ownership ledger keyed by dimension, physical chest/barrel position, and exact item components. Village loot unpacking and Villager-controlled Farmer, food, and retained-trade transfers update the ledger; menu-click and block-break hooks reconcile actual contents, consume unowned matching quantities first, and emit witnessed vanilla gossip without modifying ItemStacks. `RetoldVillageReputationStatus` provides the bounded query behind `/retold village status`. Four focused GameTests cover generated loot, SavedData round-trip, mixed ownership, real menu withdrawal, breaking, reputation, Creative exclusion, village-context filtering, aggregation, and the golem-hostility threshold.
- Added versioned per-position Farmer-crop provenance and a narrow `HarvestFarmlandMixin` around vanilla Farmer work. Player placement clears ownership, while mature crop breaking, immature crop breaking, and farmland trampling route through the shared bounded witness/gossip owner with different offense strengths. Four focused crop-reputation GameTests cover SavedData, growth retention, player separation, the real vanilla Farmer planting hook, harvest/vandalism penalties, trampling, and Creative exclusion.
- Added the data-driven `VILLAGER_COMMUNAL` profile and centrally dispatched loaded-world communal-food behavior. Discovery scans only loaded chunk block entities within 16 horizontal and four vertical blocks, requires a HOME, MEETING_POINT, JOB_SITE, or live village context, limits stores to accessible chests/barrels within 32 blocks of that village anchor, and reuses the shared cache, LOD, block-search budget, movement ownership, and feeding pose. Hungry Villagers consume personal food first and batch-restock up to 12 food points from those stores, while adult Farmers deposit only vanilla-produced food above their 24-point reserve. Villager hunger and exact entity/container inventories use existing persistence; no unloaded-time catch-up is simulated. Seven focused GameTests cover personal-first consumption, exact batch conservation and serialization, chest/barrel boundaries, machine and non-Farmer rejection, village bounds, danger priority, Farmer reserves, and real consumer/supplier pathing. The latest complete TPS baseline passes 75/75 tests and 375/375 phases below 50 ms/tick; the post-personal-stock Villager-only rerun also passes with an 8.459 ms/tick peak. The latest complete GameTest result remains the pre-supply 160/160 baseline because the maintained selection policy did not justify repeating unrelated tests.
- Stage 2+ Villagers now retain vanilla's five-agreeing-villager and local recent-golem eligibility rules, but replace an eligible instant spawn with visible construction only when the builder is a Cleric, Librarian, Armorer, Toolsmith, or Weaponsmith. Nitwits and all other professions cannot perform the magical construction. One eligible builder finds a reachable supported site, places four magical iron blocks and a regular pumpkin in timed steps, obtains one emerald from a Villager inventory or village chest/barrel, then animates a non-player-created Iron Golem. Emerald trades retain one physical emerald for this purpose, construction state saves with the builder, and the sequence yields to danger, hunger, sleep, trading, and higher-priority AI. Villager-built golems no longer award nearby players the player-summoning “Hired Help” advancement. Player-built Iron Golems cost five experience levels only when a valid structure successfully animates; Creative players, invalid/obstructed frames, Snow Golems, and Copper Golems are not charged. Five focused Golem GameTests pass.
- Added a loaded-chunk extinguished-torch index and low-priority `VILLAGER_TORCH_RELIGHT` ownership through the central Villager dispatcher. Three focused tests pass for all-stage behavior, village/range bounds, hunger/danger priority, wall-facing preservation, and sustained Nitwit close-range fake-tool use without inventory mutation; the existing six-variant drop regression passes. Only an active Nitwit physical-use animation bypasses the ordinary Villager cadence, and the affected 50-Villager TPS test passes all five phases with a 7.102 ms/tick peak.
- Replaced the warm-ocean-ruin archaeology table with its vanilla rewards minus the Sniffer Egg, with a GameTest that samples the live loot table and verifies command-created Sniffers remain functional.
- Added a centralized Endermite spawn-reason policy, an Ender Pearl mixin for the vanilla path that bypasses spawn events, and GameTest coverage for blocked survival spawning and retained command creation.
- Added a centralized incoming-damage owner for snowballs, with GameTest coverage for Snow Golem, player-thrown, ownerless/dispenser-style, Blaze, and Creeper cases.
- Split Silverfish and Endermite faction identities and restricted small-arthropod swarm recruitment to the same entity type, with GameTest coverage for neutrality, same-species coordination, and retained defender hostility.
- Added cached Enderman shared defense against living attackers, with retaliation ownership for the victim and Stage 3-only assist ownership for nearby Endermen, plus deterministic GameTest coverage for the stage boundary.
- Added source-aware Axolotl/Guardian target protection and cached nearby defensive assistance. Retold target ownership now writes and clears the brain-backed attack target used by Axolotls, with GameTest coverage for blocked vanilla/ordinary hunting and allowed retaliation, assistance, and damage.
- Moved proactive Polar Bear cub defense behind Retold AI ownership so vanilla proximity targeting cannot skip the warning. Added bounded cached threat discovery and GameTests for warning posture, target suppression, withdrawal, timed escalation, and immediate response to a cub attack.
- Exposed the validated controlled-hunt start operation for shared use and added an integrated Spider ecology GameTest covering daytime hunt rejection, night-only adult livestock hunting, Spider/Cave Spider swarm recruitment, owned targets, daylight disengagement, and retained daylight player neutrality.
- Added persisted `SPIDER_LAIR` homes and centrally dispatched Spider lair behavior. Construction requires recent feeding or a successful hunt, local Spider darkness, low raw skylight at both the Spider and supported placement, and the shared entity-griefing permission. Cached member scans and budgeted block searches support cross-type lair sharing, one-web-per-30-second expansion and repair, a 50-web cap, and low-priority daylight return ownership. An integrated GameTest covers bright and open-sky rejection, sheltered dark creation, sharing, cap, repair, `mobGriefing`, return, retaliation interruption, and night release.
- Added bounded cached same-species Slime/Magma Cube merging through the existing swarm profile pipeline. Merges preserve combined health condition and average hunger, discard the absorbed entity without death drops or splitting, persist the survivor's cooldown, and have GameTest coverage for compatibility, cooldown, and size limits.
- Extended vanilla Cube Mob collision damage to the Slime or Magma Cube's current valid non-player target. Vanilla already handled players and Iron Golems but ignored other contacted targets; the narrow mixin hook now reuses vanilla's own range, line-of-sight, damage, sound, enchantment, and invulnerability-time behavior while retaining faction and Creeper guards. It also enables vanilla contact damage for hungry size-one Cube Mobs. `RetoldSlimeHungerCombat` gates every vanilla and Retold target/damage path at the profile's hunt threshold and clears retained combat after feeding.
- Added survival-adjusted Cube Mob hunger scaling and a shared half-hunger owner using NeoForge's standard mob-split event plus the explicit starvation path. Hunger gained per interval is `ceil(size / 2)`, ranging from one point at sizes 1-2 through five points at sizes 9-10. Every vanilla death-split child and deterministic starvation-split child inherits half of its parent's current hunger. Starvation splitting also preserves swallowed storage exactly once, applies the existing merge cooldown, and kills size-one Cube Mobs through the normal death path; integrated GameTest coverage verifies every supported size's rate, arbitrary half-hunger inheritance, critical split, storage, cooldown, and terminal death.
- Added persistent exact-stack storage for items swallowed by Slimes and Magma Cubes, death-drop restoration with duplicate prevention, exponentially increasing food growth through size 10, and swallowed-content transfer during merging. Natural merging remains capped at size 4. Cube Mob GameTests cover arbitrary tools and blocks, damaged-item components, full-stack counts, both species, escalating growth, growth beyond size 4, the size-10 cap, release, and merge retention.
- Centralized dropped-food preference in `RetoldFoodBehaviorEvents` for hunger-driven profiles. A higher-priority food claim now interrupts ordinary `HUNT` control and clears prey, sprint, navigation, and strike state while retaining urgent retaliation and territory attacks. `RetoldMobRules.wantsDroppedFood` gives Slimes and Magma Cubes a separate unconditional item appetite without relaxing `RetoldSlimeHungerCombat`; GameTests cover Wolf hunting, fed Slime consumption and target rejection, hungry Slime hunting, and the retaliation exception.
- Registered the one-slot `animal_feeder` block, item, and block entity with exact-stack persistence, comparator output, a shaped five-plank recipe, loot, Functional Blocks placement, and a code-native trough model using the vanilla oak-plank texture. `RetoldAnimalFeederBehavior` adds a cached, LOD-aware, block-search-budgeted loaded-world food source after dropped food and before forage/random search. It uses ordinary `FOOD`/`FEED` ownership and reachable adjacent ground paths without treating consumption as terrain griefing. Its final approach now uses a zero-reach-range path and keys cached movement by that precision. Five focused feeder-environment GameTests cover interaction/persistence, species and diet boundaries, a real Sheep walking to the exact adjacent endpoint and consuming one item with `mobGriefing=false`, live-combat priority, and the shared stationary source-facing pose.
- Added `RetoldFeedingPose` as the common post-consumption owner. For 40 ticks it stops navigation, ordinary movement, sprinting, velocity, and Cube Mob controller movement while fixing body/head/look control on the remembered source position. It runs through the central dispatcher with `FOOD`/`FEED` priority below faction pressure and urgent purposes, so a higher-priority owner cancels the pose. Focused feeding tests pass 6/6, focused food tests pass 4/4, the relevant Bat scenario passes 1/1, all 74 TPS tests and 370 phases pass below 50 ms/tick with a 9.692 ms/tick peak, and the complete suite passes 155/155.
- Hardened Animal Feeder inventory controls with explicit normal-click single-item, sneak-click bulk insertion, empty-hand retrieval, exact Survival consumption, and non-consuming Creative assertions. Mock players are discarded after the interaction test so nearby Sheep are not attracted to leaked wheat holders in other tests. The updated direct interaction test passes 1/1. In the latest grouped reruns, every interaction assertion passed while the separately tracked Sheep-route contention regression timed out; that route immediately passed 1/1 in isolation. The latest complete run passed 152/155; its feeder-route, Bat-route, and fish-route failures are separately tracked full-suite contention regressions, while every inventory and TPS test passed.
- Hardened AI GameTest isolation found by the expanded suite: Axolotl defense now has a dedicated environment and waits for real Guardian damage within the existing timeout, while the Bat multi-attacker scenario clears stale fixture paths before requiring distinct new approach routes. The requirements are unchanged; focused Axolotl and 8/8 Bat checks plus the complete 154/154 suite pass.
- Added satisfied-predator hunt release and hunger-aware pack leadership. Pack sensing now excludes satisfied scouts, and an active hunt/search transfers from a satisfied Wolf leader to an available hungry member without disturbing retaliation or territory defense. Two integrated GameTests cover solo disengagement, urgent-target protection, leadership transfer, search ownership, and hunger-gated prey validation.
- Added the data-driven `BAT_COLONY` profile and persisted `BAT_ROOST` home type. The saved position remains a broad colony anchor rather than an exact hanging block. In daylight, each awake Bat performs a budgeted upward-only search within eight horizontal and 32 vertical blocks for a currently dark supported ceiling cell, follows a bounded vanilla `FlyingPathNavigation` route to that personal slot, and settles after its own randomized 8-to-40-tick delay. Occupied and pending ceiling cells remain reserved so colony members sleep in distinct positions. At night, hungry Bats recruit compatible nearby members into parties capped at five; the party retains one horizontal search direction for 20 seconds, assigns loose lateral lanes, rotates direction after that interval or repeated path failures, and shares detected arthropod prey. Feeding, search, hunt, dodge, and panic destinations remain path-backed, while frightened, feeding, and sheltering Bats cannot be recruited. The isolated GameTests additionally cover a 21-block-tall cave, distinct ceiling occupancy, repair of an already-stacked pair, daylight settling, five-member party limits, a separate sixth Bat, shared forward travel, and delayed direction changes.
- Optimized Bat colony hot paths. Roost discovery now tests the founding column first and exits at the first valid nearby ceiling instead of exhaustively scoring the entire three-dimensional volume. Explicit-center block-search caches no longer expire merely because the querying mob moved. Roost routes live for five seconds, unchanged flight destinations reuse their path, local separation vectors are cached for four ticks, party-wide thinking runs at most once per eight ticks, and incomplete parties retry recruitment at most once per 40 ticks. `/retoldbehavior perf` now reports the number of block-target positions actually examined, and the Bat GameTest verifies that a second party member reuses the pending shared decision.
- Bat home memories in loaded chunks must now point to an actual dark supported ceiling cell. Legacy ground-level homes and homes whose ceiling was broken are replaced by searching upward around the previous colony anchor, preventing daytime return paths from targeting the cave floor. A dedicated 64-Bat GameTest measures 200 real server ticks across daylight roosting and nighttime hunting, requires an average below the 50 ms 20-TPS budget, and reported 10.658 ms/tick in the latest focused run.
- `BatAiMixin` now reports vanilla resting-to-awake transitions to the Bat colony owner. The resulting ten-second `FLEE` claim clears shelter routes and settle timers and keeps vanilla Bat AI from reattaching while panic remains active. A real-tick regression verifies that the Bat remains awake after six seconds and releases panic after ten.
- Hardened Bat GameTest isolation without relaxing gameplay requirements. Login synchronization now ignores clientless mock players, with a dedicated regression, while broken-roost coverage retries across five shared AI budget windows and still requires a different dark supported ceiling, shelter ownership, and a real flying route. The focused Bat selection passes 7/7, the 68-species matrix passes all 340 phases, and the complete suite passes 141/141.
- `RetoldBehaviorMovement` now reports whether vanilla navigation actually started and caches only successful ground paths. A shared bounded active-food-search owner gives hungry non-predator profiles reachable search destinations; predator and specialized search owners remain independent. Focused GameTests cover ground search ownership/path creation and Bat three-dimensional search and detours.
- Reduced loaded-mob allocation pressure without changing behavior cadence or budgets. Mob profiles now resolve through an atomically replaced direct entity-type index instead of allocating a per-entity context every tick, LOD cache entries refresh in place, and entity-scan cache hits avoid constructing unused search bounds. The profile-loading GameTest also verifies the direct entity-type index. A new isolated 256-mob stress GameTest runs eight managed animal profiles for 200 server ticks, asserts that cached entity work remains effective and every expensive-work category stays within its global budget, and logs wall-clock tick time plus the complete AI work snapshot without using machine-dependent timing as a pass threshold.
- Added an isolated TPS matrix for all 68 profiled mobs. Each test loads 50 subjects and measures idle/rest, dropped-food/forage, hunt/target, danger/social, and habitat/day-night behavior separately, failing at the 50 ms/tick limit and logging Retold work counters. All 340 phases pass in the clean baseline. The matrix also exposed and fixed a nested Sniffer range scan, reducing its measured peak from over 216 ms/tick to 3.340 ms/tick.
- Consolidated the developer-confirmed mob, faction, territory, ecology, and unloaded-simulation design into the internal AI documentation and status trackers.

## 0.3.0 - 2026-07-27

Feature release focused on a richer, persistent Aender, safer portal travel, and the first in-game discovery path for the Air Temple.

### Player-Facing

- Journeyman cartographers now sell an Air Temple Explorer Map for 12 emeralds and a compass after the world reaches Stage 2. The map marks the nearest Air Temple with an exact X.
- Aender terrain now keeps the same reality across disconnects, saves, normal game/server restarts, and recovery after a crash. Volatile terrain receives a new seed only after the last player actually travels out of the dimension.
- Fresh worlds use improved Aender terrain composition that removes buried overlap surfaces and lets trees and boulders continue cleanly across chunk borders. Existing saves retain legacy generation, including newly explored chunks.
- Entering the Aender now charges for at least five seconds and waits longer when necessary until an asynchronously prepared 5x5 arrival core is genuinely safe and every already-loaded chunk in the destination view is current, stable, or blank. Old terrain is therefore removed before travel instead of visibly disappearing around the arriving player. The transition no longer forces that core, or the whole view-distance square, to generate synchronously, and preparation happens without technical action-bar text during normal gameplay.
- Automatically created Aender portal counterparts now search nearby island terrain first and use a supported floating platform at Y=100 when the destination area is entirely void, instead of appearing near the dimension floor.
- The Aender portal now preserves the Nether portal's exact animated pattern, timing, and transparency while recoloring it to the vivid Aender green palette at resource-load time. Its particles use the same palette and emerge above the horizontal surface with a gentle upward drift instead of spawning partly underneath it where players could not see them.
- Leaving the Aender now schedules all loaded volatile chunks for TPS-paced blanking before regenerating them incrementally in concentric rings from arriving players, preventing old and new island layouts from forming full-height seams or rebuilding in arbitrary coordinate order. Stabilized chunks remain intact.
- Blanking volatile Aender chunks now removes their non-player entities immediately, preventing mobs and dropped items from falling through the temporary empty terrain before regeneration reaches them.
- Aender deletion and regeneration now measure average server tick time and dynamically use available tick headroom. Recent chunk cost prevents the queue from starting extra work that is unlikely to fit; stale chunk-load callbacks and portal searches no longer bypass the queue with synchronous regeneration.
- Multiplayer Aender volatility now follows actually player-tracked generator regions instead of waiting for the entire dimension to empty. An area may change after its last watcher leaves even while other players remain elsewhere; regions watched by any player and stabilized chunks are never reset.
- Placed and broken blocks in Aender chunks now survive saves, quits, restarts, and normal crash recovery while their regional reality remains current. Persistent chunk signatures distinguish saved player edits from terrain that genuinely needs regeneration, and the obsolete unstable-chunk save/read interception no longer discards those chunks.
- Aender grass now spreads, responds to bonemeal, becomes snowy, and reverts to Aender soil under cover. Aender leaves now track logs, decay naturally, and drop renewable saplings and sticks; player-placed and upgraded legacy leaves remain persistent.
- Aender trees now form a complete renewable wood family with wood and stripped variants, planks, stairs, slabs, fences and gates, doors and trapdoors, redstone components, signs, hanging signs, boats, and chest boats. The family supports normal recipes, fuel, composting, axe stripping, tags, and survival drops; its current purple textures are explicitly AI-generated placeholders.
- Retold content now appears alongside equivalent vanilla content in Building Blocks, Natural Blocks, Functional Blocks, Tools & Utilities, Ingredients, Spawn Eggs, and permission-gated Operator Utilities; Retold does not add a separate creative tab. The Aender Stabilizer and Chronolith are available in ordinary Functional Blocks, while only the development portal frame is operator-gated. Aender Eye and Gale Core now have functional spawn eggs, and previously hidden block items have modern client-item definitions.
- Added credited AI-generated placeholder textures for the previously missing Aender Chronolith model and the new Aender Eye and Gale Core spawn eggs.
- Fresh worlds now assign either Aender Plains or an experimental inverted Aender Desert to each floating island. Stacked islands can have different biomes; plains islands use broad rolling surfaces, while desert islands are wider and flatter with denim-blue sand, periwinkle sandstone, lavender cacti, a rose-lavender atmosphere, and rising energy spores. Both biomes now vary between distinct island silhouettes, irregular height clusters, eroded openings, warped detached satellite fragments, biome-specific dunes/basins/undersides, clustered vegetation, and rare spires or impact-like energy craters. Volatile regeneration changes biome data together with terrain, while stabilized chunks preserve both.
- Substantial round and elongated plains islands and plateau or dune desert islands can now contain winding cave networks. Large eligible islands always receive at least one valid main passage and often receive multiple passages. Main routes use asymmetric broad turns and local meanders instead of following one nearly straight line; many split at interior junctions into shorter curving side branches, some of which end in chambers. A main passage may open at either or both ends through restrained surface, cliff-side, or underside entrances without carving fragile, split, crescent, twin, shelf, or strongly eroded islands into arches.
- Added Aenderite Ore in diamond-like deposits inside Aender Stone, biased toward island undersides. Most veins contain 3–4 or 4–8 blocks, with rare 8–12 block veins. It requires a Netherite Pickaxe, drops Fortune-compatible Raw Aenderite, and can be smelted or blasted into Aenderite Ingots; the ingot has no crafting use yet.

### Technical

- Added a data-driven Air Temple map-destination tag and a server-authoritative cartographer interaction hook. The hook preserves existing saved offers, supports already-generated cartographers in upgraded worlds, and avoids duplicate map trades.
- Added a GameTest covering Stage 1 gating, Stage 2 availability, preservation of existing saved offers, map marking, trade cost, and duplicate prevention.
- Added versioned `AenderRealityData` for the persisted reality seed, global and regional epochs, and fresh-world generator selection. Reality changes are saved before new terrain can generate.
- Added generator V2 with order-independent island-interval composition and chunk-halo placement for large decorations, then generator V3 with deterministic per-island 3D biomes; upgraded saves retain their persisted generator version.
- Added `AenderBiomeSource`, vertically resolved island-biome selection, regeneration-time biome replacement, and deterministic coverage for differently themed stacked islands.
- Cached each chunk's island candidates and horizontal terrain columns during 3D biome filling, avoiding repeated full island searches for every vertical biome cell.
- Added deterministic JUnit coverage for terrain interval composition and a GameTest for Aender reality serialization and legacy-version fallback.
- Extended Aender volatility coverage to verify the blank intermediate state used by progressive reality regeneration.
- Added deterministic tests for the adaptive tick-time regeneration budget.
- Added multiplayer region-lifecycle tests covering shared regions, watcher departure, final disconnect preservation, and transient dimension-transfer tracking gaps.
- Added the serialized `retold:aender_chunk_reality` chunk attachment with explicit current/stale states. Chunks upgraded without the attachment adopt their saved blocks into the current reality instead of being destructively regenerated once.
- Unloaded Aender chunks now evict their duplicate runtime generation signature while retaining the persistent chunk attachment, bounding that cache during long exploration sessions.
- Added Aender terrain-block tags, loot tables, and GameTest coverage for vegetation support, mining categories, harvesting behavior, and signature cache restoration.
- Split loaded-chunk replacement, entity reconciliation, heightmaps, lighting, and packet resends out of `AenderChunkGenerator` into `AenderLoadedChunkReplacement` and `AenderChunkSectionEditor`.
- Renamed the misleading runtime signature diagnostic to `Aender cached chunk signatures`.
- Added an indefinite, TPS-aware portal preparation state machine with unit coverage for its transition gate and a 5x5 asynchronously prepared safety core.
- Added deterministic coverage for Aender counterpart surface selection and the Y=100 void fallback.
- Added the data-driven Aender tree grower, NeoForge stripping/composting data maps, complete wood-family resources and entities, and GameTest coverage for renewal, tags, loot, stripping, signs, and boats.
- Added a pure deterministic Aender cave planner with cross-chunk tunnel continuity, guaranteed main passages on large eligible islands, asymmetric cubic turns, bounded connected branches, branch-end chambers, biome-specific vertical movement, conservative terrain-shell rules, overlap protection, and independent surface/side/underside profiles at both main endpoints. JUnit tests cover continuity, curvature, connected junctions, large-island guarantees, double-ended entrance balance, and fragile-archetype exclusion; GameTests verify actual cave carving and surface-cap breakthrough.
- Added a pure coordinate-deterministic Aenderite vein planner with cross-chunk coverage, Netherite-only harvest tags, ore loot and cooking recipes, vanilla creative-tab placement, survival GameTest assertions, and deterministic JUnit tests for structure and real-sampler frequency. Diamond-style retuning raises the same 9,409-chunk probe from the original 3.6% ore-bearing chunks to 31%, averaging about three placeable ore blocks per sampled chunk.

## 0.2.1 - 2026-07-20

Patch release focused on reliability, automated coverage, and the ocean-monument guardian crash fix.

### Player-Facing

- Updated packaged mod metadata to use Retold branding, the current generated version, and working project and issue-tracker links.
- Fixed territory suspicion carrying process-wide save and decay timing between worlds; reputation now belongs to the current saved world.
- Fixed a crash when a non-player mob, such as a drowned, damaged a guardian at an ocean monument.

### Technical

- Added separate MIT code and protected creative-asset licenses, including permission to redistribute the complete, unmodified Retold JAR in modpacks.
- Added structured bug and suggestion forms, a pull-request template, and monthly Gradle and GitHub Actions dependency checks.
- Added SHA-256 checksum assets to the release workflow.
- Updated the Gradle wrapper, ModDev, JUnit, and GitHub Actions dependencies used by builds and releases.
- Moved territory reputation into versioned Minecraft `SavedData` with safe one-time migration and retained backups for legacy JSON data.
- Updated NeoForge metadata to report the split code/asset license and routed Aender cleanup and recipe inspection failures through structured logging.
- Added deterministic GameTests for Aender portal shapes, coordinate scaling, counterpart creation, stability serialization, and volatile-chunk regeneration policy.
- Added deterministic Gale Core GameTests for activation, damage aggro, phase changes, disengagement, serialization, and duplicate-spawn repair.
- Fixed AI sight-cache cleanup detaching the current observer's fresh result and forcing avoidable repeat raycasts.
- Added a regression GameTest verifying that guardian defense assist safely ignores non-player attackers.

## 0.2.0 - 2026-07-18

Feature build focused on bidirectional Aender portal travel, more reliable Aender terrain transitions, and stronger foundations for Retold's mob AI and territory systems.

### Player-Facing

- Tuned the Air Temple Gale Core encounter: the boss now roams slightly while idle, aggroes when damaged by a valid player even outside its normal activation range, no longer deflects projectiles during phase two, and returns to the top tower area instead of a single exact block.
- Lava poured into the Aender now vaporizes like water in the Nether.
- Added bidirectional horizontal Aender portals. Their provisional frame block generates in Aender islands, supports rectangular interiors from 3x3 through 21x21 blocks, and activates when the final frame block completes the ring.
- Added 8:1 Aender travel scaling: Overworld horizontal coordinates are multiplied by eight when entering the Aender and divided by eight when returning.
- Added safe automatic 3x3 counterpart portals when no nearby destination portal exists.
- Survival and adventure players now charge an Aender portal for at least four seconds with portal distortion and ambient sound; creative and spectator travel remains immediate by default.
- Aender terrain now prepares during the survival portal charge and finishes before arrival, reducing visible chunk-by-chunk regeneration.
- Improved volatile Aender reality changes so unstabilized terrain consistently rerolls after the dimension becomes empty while stabilized chunks remain persistent.
- Fixed hostile spiders failing to acquire and attack valid nearby players in darkness; spiders also retaliate correctly when attacked.

### Technical

- Updated Gale Core targeting, idle movement, phase-two projectile deflection, and return-home logic while preserving existing saved home-position data.
- Added Aender lava vaporization to shared bucket emptying behavior.
- Added `AenderPortalBlock`, `AenderPortalFrameBlock`, `AenderPortalShape`, `AenderPortalData`, `AenderPortalLogic`, and the provisional `retold:dev_aender_portal_frame` block/assets/loot data.
- Added destination portal indexing, nearby portal validation, safe portal creation, world-border clamping, and Overworld/Aender coordinate conversion.
- Added synchronous arrival-view preparation plus asynchronous portal-ticket warm-up during the survival charge, capped at 16 refreshed chunks or 8 ms of main-thread work per tick.
- Changed empty-dimension volatility resets to occur once when the last player leaves, preventing repeated reality changes from invalidating portal warm-up work.
- Added generation signatures and blank-then-queued stale-chunk regeneration on load/arrival to prevent mixed-reality chunk seams during rapid dimension travel without synchronous chunk-load stalls.
- Expanded procedural island bounds to cover the full coast-warp reach, fixing terrain clipped into large flat walls at chunk boundaries.
- Replaced full-height per-block stale-chunk clearing with section-level replacement and fresh heightmap/light-state updates.
- Updated the README and internal architecture, implementation-status, roadmap, and design-risk docs for the completed portal/scaling work and remaining verification needs.
- Added JUnit 5 coverage for deterministic behavior and expanded NeoForge GameTests; CI now runs both unit tests and the GameTest server.
- Split the mob behavior package by subsystem ownership and moved event registration into explicit subsystem modules.
- Reworked territory escalation into an explicit state machine covering observation, warnings, attacks, and cooldown.
- Moved mob behavior profiles from hardcoded Java definitions into reloadable JSON data.
- Fixed controlled spider combat targeting and added regression GameTests for darkness-based player aggression and retaliation.

## 0.1.0  - 2026-07-15

First internal build for the current Retold survival spine, focused on Stage 2 Water + Air progression.

### Player-Facing

- Added the three-stage Retold progression model.
- Killing the Ender Dragon now advances the world into Stage 2.
- Added the Water Element path through ocean monument / elder guardian progression.
- Added the Air Temple path in mountain peak biomes, including wind hazards, Breezes, and the WIP Gale Core boss.
- The dragon egg currently hatches with the implemented required elements: Water and Air.
- Hatching the dragon egg advances the world into Stage 3 and redirects normal End portal travel to the Aender.
- Entering the Aender through a redirected End portal now creates a regeneration-resistant obsidian arrival platform like the vanilla End.
- Added the Aender dimension with custom terrain, bright fixed-light feel, custom blocks, water behavior changes, stability foundation, Aender Eye, and Aender Chronolith.
- Added staged mob/world changes including Stage 2 undead sunlight changes, Stage 3 undead cleansing, Stage 3 zombified piglin blocking/cleansing, and Stage 3 living piglin support.
- Added Retold mob behavior foundations: hunger, homes/ranges, hunting, fleeing, herding, packs, regrouping, faction combat, and territory warnings.
- Added recipe knowledge and villager recipe teaching.
- Added rain-extinguished torches and relighting support.
- Added a bed night-skipping gamerule; valid daytime bed rest is allowed when night skipping is disabled.

### Technical

#### Progression

- Added persistent world stage data and client sync.
- Added `/retold stage` debug command support.
- Added dragon egg element ritual framework.
- Registered Water Element and Air Element progression items.
- Removed the temporary Nether Star dragon egg shortcut.
- Temporarily limited dragon egg ritual requirements to Water and Air until Fire and Earth paths exist.
- Added Stage 3 vanilla End ejection for normal survival flow while preserving command access to existing End builds.

#### Aender

- Added `retold:aender` dimension, dimension type, biome data, and custom terrain generation.
- Added vanilla-style obsidian platform generation for Stage 3 Aender portal arrivals, including regeneration support.
- Added Aender chunk volatility and stability foundation.
- Added Aender Stabilizer block.
- Added Aender water flow changes and Aender weather blocking.
- Added Aender Eye entity.
- Added Aender Chronolith time-control block.
- Added Aender block set: Aender Grass Block, Aender Soil, Aender Stone, Aender Log, Aender Leaves, and Aender Stabilizer.

#### Air Temple And Air Element

- Added Stage 2 Air Temple structure and `/locate structure retold:air_temple` support.
- Added Air Temple generation in frozen peaks, jagged peaks, and stony peaks.
- Added floating main island, satellite islands, crater, and open tuff/copper tower generation.
- Added persistent Air Temple wind source data and a wind zone covering the island/tower area.
- Added horizontal cycling wind directions, body-height wind push, nearby upwind block shielding, and observer-visible wind particles.
- Added wind immunity for creative players, spectators, Breezes, and the Gale Core.
- Added retrogen protection so wind and boss spawning do not activate when an edited/skipped Air Temple chunk did not actually generate the tower.
- Added Breeze spawning on Air Temple islands and tower floors.
- Added Gale Core boss entity, boss bar, Air Element drop, projectile deflection, wind immunity, fall-damage immunity, activation near the tower top, grounded phase, aerial phase, line-of-sight targeting, return-home behavior, wall pressure, and wind-charge block cracking/breaking.
- Added Gale Core duplicate-spawn protection after reload.

#### Ocean Monument And Water Element

- Added guaranteed Water Element drop from elder guardians.
- Added guardian mining-pressure behavior.
- Added elder guardian boss/support behavior for the Water Element path.
- Added mining fatigue and blocked-hit pressure hooks for monument gameplay.

#### Mobs, Factions, And Stages

- Added Enderman behavior changes after dragon death and visual asset support for Retold eyes/skin variants.
- Added Retold mob behavior profile system.
- Added faction combat helpers.
- Added Nether Remnants territory warning support for piglins, brutes, and blazes.
- Added Illager territory warning support for outposts and mansions.
- Added guardian, elder guardian, undead, skeleton, phantom, golem, animal, predator, and faction behavior hooks.

#### Worldgen And Structure Rules

- Added delayed structure framework for Stage 2 mansions and pillager outposts.
- Added chunk edit tracking for delayed/retrogen-sensitive structures.
- Added structure mob suppression while delayed structures are inactive.
- Added End gateway generation cancellation.
- Added End City biome tag override to remove normal End City generation.
- Added outer End terrain masking foundation.
- Added generated End sky seed/system support and `/retold sky randomize` debug support.

#### Discovery, Recipes, And Villagers

- Added recipe knowledge persistence and recipe book knowledge gating.
- Added advancement visibility hiding foundation.
- Added villager recipe teaching framework and profession-based teaching data.
- Added emerald-cost recipe teaching flow.
- Added custom villager teaching preview/network support.

#### Environment And QoL

- Added Extinguished Torch, Extinguished Wall Torch, Extinguished Soul Torch, Extinguished Soul Wall Torch, Extinguished Copper Torch, and Extinguished Copper Wall Torch.
- Added torch relighting support through tagged igniter items.
- Added `retold:do_bed_night_skipping` gamerule support.

#### Metadata And Documentation

- Changed project version from `1.0.0` to `0.1.0` for the first internal build.
- Changed NeoForge Mods screen display name to `Retold`.
- Replaced example NeoForge mod metadata with a Retold build description.
- Added README status/development notes and internal design/status documentation.
- Renamed the generated documentation folder from `docs/ai_generated` to `docs/internal`.
- Consolidated standalone Air Element, release-note, and mob-AI checklist docs into the remaining internal reference docs.
- Centralized the full AI-agent instructions in `docs/internal/README.md`; other internal docs now link back to that shared section.
- Updated internal docs to describe the Air Temple/Gale Core path as implemented but still WIP.
- Updated the changelog format so release entries can serve both players and technical readers.

### Known WIP

- This build is not feature-complete.
- Fire and Earth element paths are not implemented yet.
- The Gale Core encounter is playable but still being tuned.
- Aender late-game rewards, Aender travel scaling, tool/combat reworks, broader village society, and broader removal/rework passes are still unfinished.
