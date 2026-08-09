# Living World, Roads, Environment, And Settlements

> Developer-confirmed design direction from the 2026-08-08/09 design pass. This is a design document, not an implementation-status claim. Exact numbers, thresholds, timings, block palettes, profession prerequisites, and performance strategies remain subject to implementation testing unless explicitly stated otherwise.

This document records the connected design for Retold's living-world systems: emergent roads, environmental reclaiming/weathering, village generation and growth, resource gathering and logistics, professions, trade, magic/energy, player-village reputation, and off-screen simulation.

The high-level rules in [`design_principles.md`](design_principles.md) remain authoritative. In particular:

- the player exists inside the same world rules as other actors rather than receiving blanket protection
- mandatory progression must remain discoverable in-game
- each system owns its specific timing inside shared time-scale guidance
- difficulty should come from behavior/rules rather than stat inflation
- simulation should remain Minecraft-like and avoid unnecessary micromanagement
- identity-defining world simulation should generally be native Retold functionality rather than a permanent companion-mod dependency

## 1. Simulation Fidelity: Real Where It Matters, Abstract Where It Does Not

Retold should simulate in detail what the player can directly observe or affect, while distant or unloaded processes may be abstracted when that avoids needless CPU cost.

General rule:

> If the player can see, interrupt, steal from, fight, redirect, build around, or otherwise materially affect a process, prefer a physical simulation. If the process is far away and inaccessible, an aggregate result is acceptable as long as it remains consistent with the world.

Examples:

- a nearby caravan should be a real Wandering Trader and llamas carrying real cargo
- a nearby villager should physically carry materials between storage and a construction site
- a house being built in front of the player should place real blocks
- a distant/unloaded caravan may be represented by travel state and an estimated outcome
- unloaded settlement economy may advance resource balances and project state without simulating every villager step

Do not replay thousands of missed ticks when an aggregate catch-up can produce the same believable result.

Physical world mutation while unloaded should be conservative. Large block-level consequences should generally materialize gradually when the area becomes active again rather than silently rewriting a settlement off-screen.

## 2. Emergent Road And Path System

### 2.1 Core Rule

Roads and paths should be consequences of actual movement through the world rather than decorative generated infrastructure.

Every grounded entity may contribute to terrain wear, but contribution strength differs by actor and movement. Players and intelligent entities have a larger practical impact; animals and other creatures can still create trails through repeated use without turning every enclosure into visual noise.

Do not require explicit tracking of travel direction. If entities repeatedly take similar trajectories, the worn blocks themselves naturally form a corridor. Crossings naturally form where those trajectories intersect.

### 2.2 Gradual Wear

Prefer gradual wear states rather than a binary `grass -> dirt_path` threshold.

A conceptual surface may progress through states such as:

1. untouched
2. lightly worn
3. worn
4. heavily worn
5. established path

The exact number of states and technical representation remain implementation decisions.

Different movement can contribute different wear strengths. Factors may include:

- actor category/intelligence
- entity size or mass where useful
- walking versus sprinting
- repeated concentrated traffic
- mounted travel
- terrain sensitivity
- wet/dry/weather state

Avoid over-modeling if simpler traffic accumulation produces the desired result.

### 2.3 Material And Biome-Specific Wear

There should not be one universal road surface.

Each suitable terrain material may have its own wear/recovery chain. Examples include:

- grass gradually exposing dirt before becoming an established path
- sand becoming compacted rather than turning into dirt
- snow becoming trampled and exposing the surface below
- mud becoming packed
- gravel compacting differently from vegetated soil
- forest/mossy ground losing vegetation before exposing its substrate

Hard surfaces may change little or not at all.

### 2.4 Weather

Weather affects how quickly a valid surface wears and recovers, but weather alone does not create a road.

Examples:

- wet soil may wear more quickly
- snow may show traffic quickly
- hard/dry surfaces may wear slowly
- favorable growing conditions may accelerate recovery of lightly used paths

Exact behavior is surface-specific.

### 2.5 Recovery

Unused paths should gradually recover.

- lightly worn paths recover relatively quickly
- long-established high-traffic roads retain visible history much longer
- recovery depends on material and environment
- abandoned trade/resource routes can slowly disappear back into the landscape

The path system should therefore record both use and abandonment through the world itself.

### 2.6 Gameplay Benefit

Established roads should provide a small practical benefit so infrastructure matters.

Potential benefits include:

- a modest movement advantage
- reduced penalties on otherwise awkward terrain
- pathfinding preference for intelligent entities
- weaker path preference for relevant animals

Do not make roads so strong that optimal play requires turning every surface around a base into road material.

The intended feedback loop is:

```text
repeated traffic
-> visible trail
-> slightly better/easier route
-> AI begins preferring it
-> traffic concentrates further
-> established road
```

### 2.7 Route Awareness

Local wear is the source of truth, but Retold may recognize connected high-wear corridors as routes so AI can treat them as infrastructure.

Do not make a manually authored road graph the source of truth. If the physical path changes, is blocked, or reclaims, route awareness should follow the world.

Route recognition/path preference must be designed with performance in mind and should use cached/local information instead of expensive world-wide searches per entity.

### 2.8 Player Interaction

Players should be able to deliberately influence the same system through normal Minecraft interactions rather than a road-building UI.

Examples may include:

- shovel interactions accelerating path formation on suitable surfaces
- normal planting/grass growth helping restore a path
- material-specific maintenance interactions

Exact tool rules remain feature-specific.

### 2.9 Geometry

Passive wear changes surfaces, not terrain geometry.

It should not automatically:

- flatten hills
- dig stairs
- fill holes
- terrace slopes
- construct bridges

Entities simply wear the terrain they actually traverse.

### 2.10 Intelligent Maintenance And Improvements

This is a later layer after basic road wear works well.

Intelligent societies may maintain and make small practical improvements to existing frequently used routes, but should not initially plan complete roads from scratch.

Examples for later design/testing:

- remove obstructing tall grass
- repair degraded road surface
- add local gravel/appropriate surfacing
- fill a very small hole
- add simple stairs
- bridge a small obstacle

Smart improvements should be implemented last, after passive wear/recovery and route preference are proven.

Different societies may use different maintenance palettes/styles while participating in the same underlying road system. Villagers, Illagers, Pigmen, and future societies can leave culturally distinct infrastructure without needing separate road mechanics.

### 2.11 Generated Roads

World-generated villages may start with a few established internal paths because the fiction is that the settlement existed before the player arrived.

After generation, those paths become ordinary runtime road-system state: used routes strengthen, unused routes recover, and new paths emerge from actual traffic.

## 3. Environmental Weathering And Reclaiming

### 3.1 Feature-Specific Rules

Do not create one universal "everything decays with age" rule.

Each process should be designed individually according to its material, environment, gameplay value, and reversibility.

Some changes can be cosmetic. Some can have gameplay consequences. The decision is feature-specific.

Avoid a general maintenance tax where players must constantly repair their base simply because time passes.

### 3.2 Reclaiming Through Inactivity

Areas with little use may gradually return toward natural states when that is appropriate.

Examples:

- dirt can become grass again
- lightly used paths can disappear
- moss/vines can expand onto suitable surfaces
- abandoned village edges can become visibly overgrown
- old resource sites can reclaim after being abandoned

Reclaiming should respond to actual use, not only elapsed time.

High traffic suppresses reclaiming. Low traffic enables recovery and vegetation growth.

Road traffic/activity data should be reused when practical instead of inventing a redundant activity system.

### 3.3 Source-Based Vegetation

Vegetation should primarily spread from plausible local sources and appropriate environmental conditions.

Examples:

- grass spreads from nearby grass
- moss favors damp/mossy surroundings
- vines spread from nearby vegetation

Very slow spontaneous establishment may be allowed where strict source-only spread would make environments implausibly static.

### 3.4 Timing

Different processes operate at different rates.

- light vegetation recovery can be noticeable over several Minecraft days
- road recovery depends strongly on prior wear/use
- moss/vines generally act over a longer horizon
- major material-state changes should usually be slower
- magical processes such as Nether-portal environmental transformation own their own timing

Do not let a player leave for one night and return to a completely overgrown build.

### 3.5 Player-Built Materials

Player-placed blocks are not globally protected from weathering/reclaiming.

If a material is subject to a natural world rule, a player-built use of that material may also be affected.

However, player-facing transformations should ideally be predictable and controllable/reversible through the world, for example cleaning, scraping, waxing, light/dryness, or other material-appropriate interactions.

### 3.6 Fire And Physical History

Fire and other strong physical events may leave persistent or long-lived marks even when they do not destroy a block outright.

Potential examples:

- charred/blackened wood
- soot on stone
- smoke staining around long-used fires
- cracked/scorched variants where justified

Start with a small set of meaningful transformations instead of multiplying decorative block variants without gameplay value.

### 3.7 No Passive Block Deletion

Ordinary weathering/reclaiming should not remove structural blocks simply because they are old or abandoned.

Surface/variant changes and vegetation are acceptable. Actual destruction should have a concrete cause such as:

- fire
- explosion
- mob action
- player action
- explicit magic
- another clearly designed destructive process

### 3.8 Settlements And Maintenance

Active settlements suppress or reverse some reclaiming through ordinary use and maintenance.

- active roads remain open
- used entrances remain clear
- villagers may remove minor moss/vines or repair surfaces
- abandoned sections gradually reclaim

A place should look abandoned because nobody has maintained/used it, not because worldgen selected a generic "ruin" skin.

### 3.9 Unloaded Catch-Up

Weathering/reclaiming may perform bounded aggregate catch-up after an area reloads.

Catch-up should consider relevant factors such as:

- elapsed time
- biome/environment
- nearby sources of vegetation
- recorded traffic/use
- material type

Cap the amount of visible change applied at once so a long-unloaded area does not transform implausibly on a single chunk load.

## 4. Settlement Model

### 4.1 Growth Is Need-Driven

There is no separate village XP bar, visible settlement level, research tree, or generic expansion phase.

A village progresses in the ordinary sense of the word:

- more villagers
- more valid housing
- more houses
- more resources and production
- more storage
- more resource sites
- more infrastructure
- greater specialization

New infrastructure is created because a real need exists.

Examples:

- housing shortage -> new housing
- food shortage -> more food production/new farm
- insufficient wood -> more forestry activity/resource access
- exhausted quarry -> replacement quarry
- insufficient storage -> additional storage
- newly supported profession -> required workstation/workspace
- repeated route use -> road maintenance later

Do not make settlements build arbitrary improvements simply because time passed.

### 4.2 Settlement-Wide Knowledge

Communal administrative knowledge is settlement-wide to avoid unnecessary deep simulation.

The settlement can know:

- resource totals and locations in communal storage
- current shortages/surpluses
- active resource sites
- current construction projects
- reserved/in-transit resources
- active tasks
- broad danger memory

Physical materials remain local even when information is global. If stone is stored across the settlement, somebody still needs to physically carry it to where it is needed.

### 4.3 Scheduler And Events

Routine settlement needs should be recomputed on a slow scheduler rather than every tick.

Important events may trigger immediate reevaluation, including:

- attack
- villager death
- critical storage change
- construction completion
- resource site exhaustion

Task/resource reservation should prevent duplicate work. Distinguish conceptually between:

- physically stored stock
- resources in transit
- resources reserved for work/projects
- remaining unmet need

### 4.4 Resource Priority

Communal resources are allocated according to current need rather than "who grabs them first."

General priority guidance:

1. survival/critical needs
2. safety
3. basic settlement operation
4. growth
5. comfort/improvement

Specific tasks use dynamic priority rather than a permanently fixed global order. A raid or food crisis can reorder priorities immediately.

### 4.5 Settlement Identity

Do not overcomplicate settlement identity.

A settlement is principally a spatially coherent group of villagers, used buildings, beds, storage, and infrastructure.

Use simple spatial/activity rules rather than deep citizenship/social graphs.

Two settlements that grow into each other may merge when their active areas are sufficiently connected/overlapping. Otherwise they remain separate.

## 5. Village Generation

### 5.1 Starting State

A generated village is a believable historical starting state for the runtime settlement simulation, not a finished static feature.

Most villages should generate small-to-medium. Large, already-developed villages should be rare.

Generated settlements should usually begin viable, with enough infrastructure and resources to function, but not enough reserves to coast indefinitely.

### 5.2 Starting Requirements

Generation should normally guarantee an appropriate baseline for the starting population, including:

- valid accessible sheltered beds
- a viable food-production path
- basic construction-resource access
- communal physical storage
- core professions/capabilities
- sensible paths between major starting functions

A village does not need every resource locally. Trade can solve some deficits. It must merely have a plausible way to survive and operate.

### 5.3 Starting Reserves

Generated villages receive finite physical starting reserves scaled to settlement size and professions.

Possible reserves include:

- food
- seeds
- wood
- stone/basic construction material
- profession-specific starting stock

These resources should live in real communal storage blocks. Once the settlement is active, the normal economy consumes and replenishes them.

### 5.4 Profession Mix

Do not choose starting professions purely at random.

Generation should:

- guarantee a viable core
- add extra professions according to size and environment
- reserve advanced professions mostly for rare developed starting settlements

Local environment can bias the mix without rigid biome presets. Examples:

- river/ocean access favors Fisherman
- good agricultural land favors stronger farming
- rocky terrain favors Mason/quarry activity
- good pasture favors Shepherd

### 5.5 Location Viability

Village placement should be stricter than vanilla rather than merely reducing frequency.

A candidate area should provide:

- reasonable terrain for a compact settlement
- enough space without extreme terraforming
- some viable food path
- access to at least basic materials in the wider area
- a generally plausible settlement location

Do not require complete local self-sufficiency.

### 5.6 Rarity And Distance

Villages should be noticeably rarer and farther apart than vanilla so each settlement has meaningful economic territory and long-distance trade has a reason to exist.

Do not enforce one perfectly uniform distance. Allow natural variation, occasional closer neighbors, and broad empty areas.

### 5.7 Compactness

Generated villages should be more compact on average than modern vanilla villages.

- small settlements: tight walkable cluster
- medium settlements: connected pockets following terrain and paths
- large rare settlements: broader but still coherent

Avoid giant arbitrary sprawl.

### 5.8 Blueprint Library

Retold can preserve modern biome-specific village visual styles.

Use the same broad blueprint language/library for both world generation and later settlement expansion so runtime-built additions look like plausible continuations of the original village.

Blueprints may rotate/mirror and adapt within bounded terrain tolerance. Prefer designed templates over fully procedural architecture.

### 5.9 Generated History

Generated villages may begin with subtle differences in age, maintenance, and recent history using the same state concepts runtime systems use.

Examples:

- different road wear
- light moss/vines where appropriate
- a partly used resource site
- varied reserve levels
- a less-maintained edge
- a field that looks recently expanded

Do not fake major dramatic events unless the game has an actual state/system representing them.

### 5.10 Starting In Trouble

Some generated villages may begin with real but recoverable problems.

Examples:

- food pressure
- insufficient workforce
- low reserves
- a partly exhausted quarry
- weak trade access
- elevated danger memory
- missing needed profession

Starting trouble should follow plausible local causes rather than arbitrary debuffs and should not usually doom the settlement before the player can interact with it.

### 5.11 Optional Mid-Project Generation

As a nice-to-have, a generated settlement may occasionally begin with an active construction or expansion project if that can reuse the normal runtime project state.

Do not build a separate worldgen-only construction simulation solely for this effect.

## 6. Housing, Population, Growth, And Decline

### 6.1 Population Growth

Do not model family trees or household genealogy.

Villagers are treated socially as a settlement population.

Population growth should depend on the settlement being able to support additional people, considering things such as:

- stable food supply
- valid housing capacity
- absence of acute crisis
- general economic capacity

Retain actual villager breeding rather than replacing population growth with abstract spawning.

### 6.2 Housing Capacity

Housing capacity is based on real accessible beds, not an abstract building count.

A valid housing bed must be:

- path-accessible
- usable/unblocked
- not already occupied
- inside at least a minimal sheltered/inhabitable space

Do not require a complicated architectural house-recognition system, but do not count beds simply thrown into an open field as complete housing.

Player-built valid housing should count normally.

### 6.3 Housing Shortage

Do not create a construction project the instant one bed disappears.

A housing deficit should persist long enough to be recognized as a real problem before triggering a new house project.

Blueprint size/capacity should respond to the actual shortage without requiring exact optimization.

### 6.4 Decline

Settlements can shrink as well as grow.

When population/resources fall:

- houses may become unused
- storage can become inactive
- resource sites can be abandoned
- routes lose traffic and reclaim
- unused fields can revert toward nature
- specialized professions may disappear when no longer supportable

Do not automatically demolish abandoned buildings. Decline is primarily loss of use/maintenance, not deletion.

Old infrastructure may later be reused if the settlement recovers.

### 6.5 Fully Abandoned Villages

If all villagers are gone, the settlement becomes an abandoned site rather than being deleted from the world conceptually.

- active management stops
- roads/fields gradually reclaim
- storage and buildings remain
- resource sites become abandoned

A later group of villagers may reoccupy and reuse the old settlement.

### 6.6 Creation Of New Settlements

Natural new villages do not spontaneously colonize the world through autonomous migration. Natural settlement geography primarily comes from worldgen.

Players can create new settlements by building a viable site and bringing villagers to it.

A player-created settlement can be recognized when it has a simple minimum such as:

- several villagers
- several valid sheltered beds
- communal storage/basic communal space
- enough separation from an existing settlement

It does not need to be self-sufficient before recognition. Once recognized, normal settlement needs can drive farms, professions, resource sites, and growth.

Do not require a "Create Village" UI button or mandatory Town Hall block.

## 7. Site Selection And Construction

### 7.1 Need Before Project

Construction begins because the settlement has a persistent need, not because a build timer fired.

### 7.2 Blueprint Selection

Use designed blueprint/template variants rather than fully procedural buildings.

Blueprints can:

- rotate/mirror
- select local/biome-appropriate material palettes
- tolerate bounded terrain variation
- use multiple variants for the same function

### 7.3 Site Selection

The settlement should seek a sufficiently good site, not calculate a globally perfect one.

Candidate scoring may prefer:

- reasonable terrain
- proximity to existing settlement/infrastructure
- proximity to established roads
- avoidance of farms, resource sites, storage, and other active projects
- no collision with existing buildings
- manageable access
- no major water/void/terrain conflict

Choose among good candidates with some variation so growth does not look sterile.

### 7.4 Terrain Changes

Do not flatten large areas for a blueprint.

Small foundations or limited local adaptation are acceptable. If terrain requires major reshaping, reject the site and find another.

### 7.5 Soft Growth Boundary

Settlements have a soft growth boundary rather than a hard invisible wall.

Sites farther from the settlement receive worse preference. As population and physical settlement size grow, the practical boundary expands.

Allow exceptions when a farther site is otherwise unusually suitable, but avoid kilometer-long accidental strip settlements.

### 7.6 Existing Player Builds

If a player-built structure already satisfies a settlement need, the village should use it rather than insisting on a generated/Retold-authored building.

Examples:

- valid sheltered beds can solve housing shortage
- useful communal storage can join settlement logistics
- a suitable workstation/workspace can support a profession

Judge function, not authorship.

### 7.7 Construction Logistics

Construction uses real resources.

The flow is:

1. settlement identifies need
2. blueprint/site is chosen
3. required resources are reserved
4. villagers physically deliver materials in batches to a local construction-site cache
5. magic-capable villagers place the blueprint block by block using those materials
6. if supplies run out, construction pauses
7. if interrupted, the partial building and remaining cache remain in the world

The local site cache should have a physical/world representation rather than being an invisible magical inventory where practical.

### 7.8 Block-By-Block Magical Construction

All magic-capable villagers can perform basic construction; Builder is not a separate profession.

Construction should visibly place blocks one by one or in very small groups. Magic can allow blocks to lift/fly from the local cache toward their blueprint position so building can be reasonably fast without looking like a structure popped into existence.

Construction may follow sensible stages such as foundation, walls, roof, and interior while still placing real blocks.

Multiple villagers may contribute simultaneously.

Nitwits cannot perform magical placement but can support logistics and site-cache delivery.

## 8. Resource Gathering, Resource Sites, And Logistics

### 8.1 Matter Is Conserved

Villager magic replaces tools/manual technique; it does not create arbitrary matter from nothing.

If the village needs wood, a real tree must be harvested. If it needs stone, real stone must be obtained. If it needs wool, sheep must provide it. If it needs crops, they must grow.

Magic changes how villagers perform work, not whether the material cycle exists.

### 8.2 Sustainable Renewable Gathering

Villagers use basic sustainable behavior for renewable resources without becoming a deep resource-management simulator.

Examples:

- Forester harvests real trees and replants saplings
- Farmers retain seed stock and replant crops
- livestock professions preserve viable breeding populations

Do not require managed forestry zones or complex optimization unless future gameplay justifies them.

### 8.3 Non-Renewable Resources

Villagers may obtain common non-renewable/basic mineral resources themselves, while rare/advanced materials can lean more heavily on specialization and trade.

Examples:

- ordinary stone/clay/sand: local gathering/resource sites
- common metals: specialized access/profession where appropriate
- rare/advanced resources: stronger trade/specialization/player role

### 8.4 Quarries And Resource Sites

Mining sites should normally be located away from the residential core so villagers do not destroy their own settlement while gathering material.

A resource site can have a simple remembered state such as:

- active
- low-yield/nearly exhausted
- exhausted
- abandoned

Villages reuse known sites rather than creating a new quarry every time work is needed.

Abandoned resource sites can later reclaim naturally.

### 8.5 Physical Logistics

Resources are physical items, not merely settlement counters.

Villagers carry material through their inventory between:

- resource sites
- processing locations
- communal storage
- construction caches

Distance therefore matters and traffic naturally contributes to road formation.

### 8.6 Distributed Communal Storage

Villages use multiple physical storage locations rather than one magical global chest.

Possible locations include:

- general communal store
- farm/food storage
- quarry/material storage
- workshop storage
- construction-site cache

The inventory is economically communal even though it is physically distributed.

Villagers know settlement-wide where communal resources are stored, but items do not teleport between locations.

### 8.7 Communal Ownership

Village resources are communal by default rather than owned by individual villagers or professions.

Personal villager inventory is primarily:

- carried resources
- short-term work buffer
- specific personal items only where future design requires them

## 9. Village Trade And Wandering Traders

### 9.1 Wandering Traders As Inter-Village Trade

Use Wandering Traders as the primary inter-village caravan/trade actors rather than introducing a new merchant profession.

They remain independent traveling merchants rather than employees owned by one village.

### 9.2 Simplified Trade Decision

Do not simulate a full market or detailed information-exchange graph.

A settlement can maintain simple trade relationships/known partners and broad resource states such as:

- critical shortage
- shortage
- balanced
- surplus

Shortage/surplus should be relative to expected consumption/projects, not fixed absolute item counts.

Examples:

- food stock depends on population and expected consumption
- wood/stone reserve depends on construction/maintenance needs

When one settlement has a useful surplus and another has a relevant deficit, the system can schedule trade.

### 9.3 Physical Cargo

When trade is in an active/player-relevant area:

- goods are removed from actual communal storage
- Wandering Trader/llamas carry actual inventory
- cargo is deposited into the destination settlement's physical storage
- if the caravan is destroyed, the destination truly does not receive the shipment

### 9.4 Off-Screen Caravan Abstraction

Far from players/unloaded, caravan travel may be abstracted instead of pathfinding thousands of blocks continuously.

When a caravan becomes player-relevant, represent it physically.

Off-screen risks/outcomes may be resolved in aggregate where needed, while nearby events should become real interactable encounters.

### 9.5 Stage 3 Pillager Attacks

In Stage 3, Pillagers may attack Wandering Trader caravans.

Prefer attacks that make sense from route/patrol activity rather than simply spawning attackers directly on the trader.

Consequences can include:

- stolen/lost cargo
- destination shortage persisting
- route danger increasing
- trade frequency decreasing on unsafe routes
- traffic shifting toward alternatives
- old roads reclaiming and new routes emerging

Avoid overbuilding a route-strategy simulation; keep the behavior readable and systemic.

### 9.6 Player Trading From Cargo

Players may buy/sell from the trader's actual cargo, letting them participate in the same economy as settlements.

A player's purchase can reduce what reaches the destination.

Critical cargo may have safeguards such as:

- some reserved amount not offered to players
- reduced availability
- higher prices as a player consumes more of a scarce shipment

Players may also supply goods the destination needs.

### 9.7 Prices

Prices may react lightly to local shortage/surplus.

- shortage tends to make goods more valuable
- surplus tends to make them cheaper
- critical shortages may reduce what the village/trader is willing to sell

Do not create full simulated inflation/order-book economics.

## 10. Village Professions

### 10.1 General Profession Rules

Professions represent real work/knowledge, not only trade tables.

Villagers can change profession when settlement needs change, especially while young/unskilled. Experienced specialists should be less willing to switch roles because specialization has meaning.

Do not use fixed profession quotas such as "one Farmer per five villagers." Settlement needs drive demand for work.

Workstations remain useful/required workspaces where appropriate, but merely placing a workstation should not automatically create an advanced profession in a struggling village.

Advanced professions become eligible naturally as the settlement grows in population, houses, resources, infrastructure, and spare labor capacity. There is no separate village XP/tech-tree system.

Unlocking a profession means it becomes possible, not that the village must immediately create one.

### 10.2 Recipe Teaching

Every profession can teach recipes relevant to that profession using Retold's existing villager-teaching direction.

Examples:

- Farmer: farming/food-related knowledge
- Mason: stone/clay/building processing
- Fletcher: bows/arrows/light wooden goods
- Leatherworker: leather goods
- Armorer: armor
- Toolsmith: tools
- Weaponsmith: weapons
- Shepherd: wool-related knowledge
- Fisherman: fishing/food-related recipes

Librarians also hold broader general/library knowledge appropriate to a library, especially recipes/knowledge that do not naturally belong to one profession.

Do not turn the Librarian into a complete living recipe book that reveals everything.

### 10.3 Farmer

Core/early profession.

Responsibilities:

- maintain existing fields
- plant and harvest real crops
- move production to communal storage
- preserve seed reserve
- establish new farms when existing production cannot meet sustained food needs

New farms use designed farm blueprints that already contain necessary layout/irrigation rather than requiring Farmer AI to design irrigation dynamically.

Farming magic replaces tools/manual action but does not make crops mature instantly by default.

### 10.4 Fisherman

Environment-dependent food profession rather than a universal core requirement.

Responsibilities:

- use real suitable water sites
- produce real fish items
- deliver catch to communal food storage
- potentially use a small dock/fishing-site blueprint

Yield can be influenced by environmental suitability without requiring exact simulation of every fish entity.

### 10.5 Shepherd

Livestock/resource profession.

Responsibilities:

- manage sheep as a real herd
- shear sheep through magic rather than tools
- deliver wool to communal storage
- preserve viable breeding population
- expand simple pasture/enclosure infrastructure when need justifies it
- support breeding when more wool/food capacity is needed

### 10.6 Butcher

Livestock-surplus processing profession.

Responsibilities:

- identify excess livestock
- slaughter animals without destroying the viable breeding herd
- produce meat/leather and other appropriate animal materials
- deliver/process those outputs through communal economy

### 10.7 Leatherworker

Established processing profession.

Consumes leather produced through livestock economy and turns it into leather goods such as armor, saddles, and other appropriate outputs.

Requires a stable enough settlement/livestock economy to justify specialization.

### 10.8 Mason

Core/early construction-material profession.

Responsibilities:

- establish/use quarry/resource sites away from residential core
- acquire stone/clay/basic mineral construction materials through magic
- transport/store material
- process raw stone into building variants
- prepare construction materials for settlement projects

### 10.9 Forester / Woodworker

Core/early profession introduced to close the fundamental wood-supply loop.

Responsibilities:

- locate suitable trees
- magically harvest real trees
- collect logs and saplings
- deliver wood to communal storage
- replant saplings/basic sustainable forestry
- process some wood into planks, sticks, and other basic components
- supply construction and professions such as Fletcher

Keep Forester/Woodworker as one profession initially. Split only if future gameplay clearly needs separate roles.

### 10.10 Fletcher

Established processing profession.

Responsibilities:

- process wood/sticks/feathers and similar light materials
- produce bows, arrows, and appropriate utility goods
- supply village defense/trade

### 10.11 Armorer / Toolsmith / Weaponsmith

Advanced metalworking specializations sharing part of the same resource/infrastructure base.

- Armorer: armor/defensive equipment
- Toolsmith: tools/work equipment
- Weaponsmith: weapons

They require access to metals, appropriate processing/workspaces, and enough settlement stability/labor capacity to support specialization.

### 10.12 Librarian

Developed knowledge profession.

Responsibilities:

- manage books/library infrastructure
- teach profession-specific library knowledge
- hold broader general knowledge appropriate to a library
- participate in future enchanting/knowledge systems where designed

Does not monopolize recipe teaching; all professions teach their own domains.

### 10.13 Cleric

Developed magic/energy specialist.

Responsibilities:

- manage/operate advanced magical energy use
- physically handle emerald energy for approved communal magic
- coordinate energy-intensive spellwork
- improve safe/effective use of concentrated energy
- participate in high-cost magic such as golem creation and future rituals where appropriate
- teach magic/energy-related knowledge

Clerics do not grant ordinary villagers their magic. Basic magic belongs to magic-capable villagers themselves.

### 10.14 Cartographer

Developed mapping/exploration profession.

Responsibilities:

- create maps
- maintain/extend settlement knowledge of important locations
- support explorer-map mechanics
- assist long-distance resource/trade/exploration decisions where useful
- teach map/navigation-related recipes

Do not make Cartographer a global omniscient radar.

### 10.15 Nitwit

Nitwit has no magical ability but remains a useful settlement member.

Responsibilities may include:

- material transport
- moving goods between communal storage
- filling construction-site caches
- simple non-magical maintenance
- physically relighting torches with Flint and Steel where existing design calls for it
- general logistics/helper labor

Nitwit cannot perform magical construction or advanced magical professions.

## 11. Villager Magic And Energy Economy

### 11.1 All Magic Costs Energy

Every magical action consumes energy.

A villager can draw energy from:

- their own personal energy
- external concentrated energy, principally emeralds

The distinction is cost, not whether the spell is "free."

### 11.2 Personal Energy

Low-cost everyday magic can safely draw from the villager because the cost is small enough to recover naturally.

Examples:

- harvesting/planting
- ordinary tree/stone gathering
- simple item/block manipulation
- ordinary block-by-block construction
- routine torch maintenance where magical

Personal energy recovers through:

- time
- rest
- sufficient food

Do not expose a mandatory mana HUD. Communicate exhaustion through behavior where necessary.

Villager AI should preserve a safe personal reserve. As energy drops:

- performance/working willingness can decrease
- the villager rests/refuses expensive magic
- ordinary AI should not deliberately drain itself to death

Only extreme/forced overextension should threaten health/death.

### 11.3 Emerald Energy

Emeralds are solid/concentrated energy and act as an external source for costly magic.

When used as energy, the emerald is consumed completely. Do not create depleted-emerald clutter by default.

Emerald energy allows high-cost magic without dangerously draining the caster(s).

### 11.4 Advanced Magic

Advanced magic is magic whose energy demand is unsafe or impractical to draw entirely from ordinary personal reserves.

Clear candidates include:

- Iron Golem creation
- major coordinated construction bursts where adopted
- selected advanced material processing
- future village-wide protective/ritual magic
- future magical infrastructure

Do not charge emeralds for every ordinary work action or create a village deadlock where zero emeralds means villagers cannot farm/gather basic resources.

### 11.5 Multiple Casters

Multiple magic-capable villagers may share the personal-energy burden of one advanced action.

A coordinated spell can use:

- safe personal contributions from several villagers
- emeralds for the remaining external energy requirement

This can reduce emerald demand while leaving participants tired afterward.

Cleric can coordinate high-cost magic but is not the only possible source of personal energy.

### 11.6 Emerald Cost Categories

Prefer coarse design categories rather than a visible universal "energy unit" economy.

Conceptually:

- minor advanced magic: small emerald cost
- moderate advanced magic: several emeralds
- major/ritual magic: feature-specific larger cost

Exact costs are balancing decisions.

### 11.7 Strategic Energy Reserve

The settlement should keep a strategic communal emerald reserve rather than treating every emerald as normal trade surplus.

Reserve size can respond to context such as:

- current danger
- planned advanced magic
- anticipated golem need
- long-term safety/stability

The settlement decides whether emerald spending is justified. Cleric physically manages/uses the approved energy.

### 11.8 Physical Energy Logistics

Emeralds for advanced magic must be physically transported near the action/project rather than remotely disappearing from a global counter.

They can be reserved and delivered to the local project cache or other nearby valid inventory.

### 11.9 Work Capacity

A village can postpone noncritical advanced magic even when it owns enough emeralds if:

- available casters are exhausted
- food situation is poor
- workforce is occupied by higher-priority work
- danger makes another task more important

This connects food, rest, workforce, and magical capacity without requiring a visible management spreadsheet.

### 11.10 Emerald Sources

Villages can acquire emeralds from multiple real sources, including:

- trade with players
- inter-village trade through Wandering Traders
- emerald ore in areas where it naturally exists

No single source needs to act as an infinite automatic faucet.

## 12. Village Safety And Threat Response

Do not begin with complex dynamic fortification design. Avoid requiring villagers to procedurally invent walls, gates, watchtowers, or chokepoints.

Keep defense primarily behavioral/resource-driven at first.

Settlements may retain a small broad danger memory based on things such as:

- recent attacks
- villager deaths
- caravan attacks
- raids
- hostile activity near the village

Use broad threat categories where useful, for example:

- Illagers
- Undead
- general monsters

Responses can include:

- higher golem priority
- earlier retreat/safer routines
- higher torch-maintenance priority
- reduced caravan/risky work activity
- abandonment of unsafe resource sites/routes
- moving or protecting important resources where practical

Threat memory decays during long peaceful periods.

Dynamic fortification construction is a later/optional idea, not a core requirement.

## 13. Player-Village Relationship And Reputation

### 13.1 Settlement-Wide Reputation

Long-term reputation is per-player, per-settlement rather than a separate persistent opinion for every villager.

Individual villagers may have immediate witness reactions, but the durable relationship belongs to the settlement.

### 13.2 Founder And Patron Status

If a player creates a new settlement by building the site and bringing villagers, that settlement treats the player as its founder/patron from the start.

A player can also become a trusted patron/mayor-like figure in a naturally generated settlement through sustained positive behavior.

Founder/patron status gives elevated baseline trust/tolerance, not ownership of the settlement or immunity from consequences.

A destructive founder can still become distrusted/hostile in practice.

### 13.3 Tolerance

High trust/founder status may increase tolerance for actions such as:

- taking some communal resources
- modifying infrastructure
- interacting with fields/livestock
- making useful changes to the settlement

It does not permit stripping the settlement of critical supplies, killing residents/livestock indiscriminately, or destroying housing without consequence.

### 13.4 Communication Of Relationship

Do not show a numeric reputation meter by default.

Communicate long-term relationship through:

- villager behavior/body language
- greetings/reactions
- trade prices
- willingness to trade
- teaching availability
- tolerance around communal property
- Iron Golem/defender response
- gifts at very high trust
- limited qualitative UI/icon/text where useful, without exposing exact hidden numbers

Short-term reactions should use immediate animations, sounds, attention, warnings, and witness behavior.

### 13.5 Gifts

At very high trust, villagers may occasionally give gifts.

Gifts should ideally be:

- profession-appropriate
- infrequent/contextual
- sourced from real communal stock where practical
- constrained so the player cannot farm them as a simple reward dispenser

Examples:

- Farmer: food
- Fletcher: arrows
- Mason: building materials
- Librarian: appropriate knowledge/book-related item
- Armorer/Toolsmith/etc.: modest profession-related goods

### 13.6 Gossip And Witness Sharing

Use/extend vanilla villager gossip-style sharing rather than inventing a deeply simulated social communication network.

Immediate incidents should begin with actual witnesses where appropriate. Villagers can then share positive/negative information through their ordinary social/gossip behavior until it becomes a settlement-wide reputation effect.

Founder/patron identity itself is settlement-level information rather than relying on gossip survival.

### 13.7 Positive And Negative Acts

Reputation change should distinguish severity/context rather than treating every act as the same number.

Potential categories:

- minor positive: ordinary helpful trade, small useful deliveries, routine assistance
- major positive: defending the village, saving residents, solving critical shortages, major useful infrastructure
- minor negative: small unauthorized communal theft, minor field/infrastructure damage
- major negative: killing villagers, destroying housing/storage, stealing critical reserves, repeated violence

Context matters. Taking a few carrots from major surplus differs from taking the last food during a shortage.

### 13.8 Repeated Behavior

Repeated small offenses should escalate because a pattern of behavior matters more than one incident.

Repeated positive behavior likewise builds durable trust over time.

### 13.9 Decay, Forgiveness, And Repair

Minor reputation events may fade with time.

Active helpful behavior should repair reputation faster than simply waiting.

Serious acts such as killing a villager should decay much more slowly and require sustained positive behavior to repair.

Do not create a simple "pay X emeralds to erase reputation" button.

### 13.10 Response Escalation

Relationship response should escalate gradually rather than switching directly from neutral to kill-on-sight.

Conceptual states may include:

- highly trusted
- trusted
- neutral
- distrusted
- hostile reputation
- open enemy

These are conceptual/qualitative states; exact implementation does not need to expose these names to the player.

Effects can progress from prices/tolerance to refusal of trade/teaching, stronger warnings, defender readiness, and finally open defense against continued violence.

## 14. Player Influence Without RTS Controls

Players should influence settlement priorities primarily through real world actions rather than a strategy-management screen.

Examples:

- build valid housing -> housing pressure falls
- add/expand field -> food capacity rises
- create useful storage -> logistics adapts
- deliver missing resource -> shortage resolves
- provide emeralds -> energy reserve rises
- build road/bridge -> pathfinding/use adapts
- bring livestock -> livestock economy can expand
- create suitable workstation/workspace -> profession becomes easier to support

Explicit mayor/prioritization controls are a later optional feature only if important player intent cannot be expressed naturally through world interaction.

## 15. Design And Implementation Guardrails

When implementing these systems:

- do not turn every good emergent idea into a giant framework before the core loop is proven
- implement the smallest useful version first
- reuse existing state/signals where possible (for example road traffic for reclaim suppression)
- keep task scheduling/bulk work bounded for server performance
- keep unloaded simulation aggregate and conservative
- prefer real inventory and physical logistics where the player can interact
- do not give the player blanket exemptions from world rules
- do not make settlement systems dependent on one perfect AI decision; "good enough" local choices are desirable
- preserve the ability for players to build useful structures that villagers recognize functionally
- keep exact numbers/timings/configuration data-driven where practical
- advanced features such as smart road improvements, complex defensive construction, or deeply optimized off-screen society simulation come after the simpler observable systems work

## 16. Open Detailed Design Work

The high-level direction above is confirmed, but detailed work still remains for areas such as:

- exact road wear states, textures, speed bonuses, and AI preference weights
- whether/how external TRMT assets may be reused legally; do not copy assets/code without compatible permission/licensing
- exact weathering transformations and reversibility rules
- concrete village blueprint library and site-scoring values
- exact minimum player-built-settlement recognition thresholds
- profession-specific unlock/eligibility conditions
- detailed resource yields and processing recipes
- exact off-screen settlement catch-up limits
- exact emerald costs for advanced magic
- exact golem/advanced-magic rituals and energy use
- exact reputation thresholds/feedback animations/gifts
- detailed trade-partner selection and caravan abstraction
- detailed Stage 3 Pillager caravan behavior
- final balance/performance tests for all simulation layers
