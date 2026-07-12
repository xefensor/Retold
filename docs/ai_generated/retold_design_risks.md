# Retold Design Risks And Open Decisions

> AI-generated design-risk tracker. This file is for planned gaps, undecided design items, implementation watchpoints, and risky future work. Confirmed bugs belong in [`retold_known_issues.md`](retold_known_issues.md).

AI agents: before implementing anything here, read [`README.md`](README.md), [`retold_roadmap.md`](retold_roadmap.md), [`design_implementation_status.md`](design_implementation_status.md), and the owning subsystem docs. Ask the maintainer before implementing undecided or high-scope items.

## Open Design Risks

| Area | Risk / open item | Current note |
| --- | --- | --- |
| Aender | Lava placement ban is planned but no implementation was found. | Search found Aender water flow/weather hooks, but no lava bucket/fluid placement blocker. |
| Progression | Stage 1 Wither/Nether star End access gating is undecided. | Do not implement until maintainer decides. |
| Elements | Only water has a real element item/challenge path. | Fire, earth, and air exist in `RetoldElementType` but need real acquisition paths. |
| Aender | 8:1 Overworld travel scaling is planned but not implemented. | Current portal destination is fixed near Aender spawn. |
| Aender | Aender teleportation and late-game rewards are planned but not implemented. | Needs design before code. |
| End | End City/outer End removal needs fresh-world and existing-world verification. | Data/mixins exist, but generated-world behavior should be tested. |
| Elytra | Elytra should remain an item but not be survival-obtainable. | Depends on End Cities not generating and no alternate survival source being added unintentionally. |
| Worldgen | Ancient Cities, Deep Dark/Warden, Trial Chambers, trail ruins, and fossils are intended for survival removal. | Current status doc does not confirm all removals are implemented. |
| Worldgen | Strongholds should be limited to 3. | Not confirmed implemented. |
| Mobs | Sniffers and endermites should not be naturally/survival-spawnable if removed from progression. | They may remain in code and Retold AI. |
| Villages | Village reputation for stealing/crops/animals is planned but not implemented. | Territory warning exists for Illagers/Nether Remnants, not villages. |
| Stage 3 | Stage 3 illager behavior is enough for now but may need clearer player-facing feedback later. | Avoid major raid redesign without maintainer approval. |
| Piglins | Stage 3 piglin/pigman hiring or follower behavior is planned but not implemented. | Needs feature design. |
| Items | Tools/armor/ores, enchanting, mending removal, and combat reworks are still planned. | Large scope; ask before implementing. |
| Environment | Death drops should despawn much later than vanilla, not never. | No implementation confirmed. |
| Beds | Beds should not skip night; future healing can consume hunger. | Healing behavior not confirmed implemented. |
| Nether | Nether portal spread is planned as energy drain. | Needs bounded implementation design. |

## Watchpoints

- Do not let vanilla AI target assignment bypass territory warning.
- Do not let creative or spectator players remain valid aggro targets.
- Do not make Aender inaccessible through commands; vanilla End command access is intentional for old builds.
- Do not add a recipe-book restoration gamerule.
- Do not delete elytra as an item just because survival acquisition should be removed.
- Do not fully remove sniffer/endermite code only to satisfy survival-spawn removal.
- Keep AI performance changes measurable with loaded-mob tests.

## Maintenance Rule

Update this file when:

- the maintainer changes a design direction
- a planned gap is implemented
- an undecided item becomes planned or not planned
- an implementation risk is discovered during code review
- a design-risk item becomes a confirmed bug after testing
