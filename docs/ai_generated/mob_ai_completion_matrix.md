# Retold Mob AI Completion Matrix

> AI-generated working checklist. This file is meant for human maintainers and future AI coding agents tracking whether the mob AI system matches the intended design.

This is the working checklist for calling the mob AI system done. It reflects the current code structure:
profiles in `RetoldMobProfiles`, factions in `RetoldFactionMembers` and territory warning in `territory/`.

Full design and technical architecture: [`retold_mob_ai_system.md`](retold_mob_ai_system.md).
Whole-mod architecture reference: [`retold_mod_system.md`](retold_mod_system.md).

## Global Rules

| Area | Expected behavior | Done when |
| --- | --- | --- |
| Target ownership | Retold-owned targets go through `RetoldCombatTargets` / `RetoldFactionTargetMemory`. | Direct `setTarget`, `setAggressive`, `ATTACK_TARGET`, and `ANGRY_AT` writes only exist in low-level guard helpers, and debug shows source/current target ownership. |
| Invalid players | Creative and spectator players are never valid retained targets. | `/retoldbehavior get` shows no lasting target or brain target for creative/spectator players. |
| Faction assist | Nearby allies can help only when target gating allows it. | Assist does not bypass warning-stage players in territory. |
| Territory warning | Nether Remnants and Illagers warn before attack in configured structures. | Bastion, fortress, outpost, and mansion all show warning progression before attack. |
| Retaliation | Directly attacking a guard can still trigger immediate retaliation. | Player hit on guard bypasses warning only for retaliation, not passive sight. |
| Debug | Debug output explains why a mob is or is not controlled. | `get`, `nearby`, `toggle overlay`, `targets`, `warning`, `home`, `guardpost`, and `pack` give actionable state. |

## Territory Factions

| Faction | Members | Territory | Expected player behavior |
| --- | --- | --- | --- |
| Nether Remnants | piglin, piglin brute, blaze | bastion remnant, fortress in Nether | Notice player, warn, posture, move into warning positions, escalate by suspicion, attack only at `ATTACK` or retaliation. |
| Illagers | pillager, vindicator, evoker, illusioner, ravager, vex | pillager outpost, mansion | Same warning/reputation rules as Nether Remnants. |
| Illager loose ally | witch | none as full member | Can align with Illagers for support behavior, but is not a full territory faction member. |

## Managed Behavior Profiles

| Profile | Mobs | Primary behavior to validate |
| --- | --- | --- |
| Hungry grazer | cow, mooshroom, sheep, goat, horse, donkey, mule, llama, trader llama, camel | Hunger, grazing/eating, home range, herd panic/flee. |
| Small forager | pig, chicken, rabbit | Hunger, foraging, home return, predator flee. |
| Pack predator | wolf | Pack creation, pack hunt/search/return, den defense, target ownership. |
| Solo opportunist | fox, cat, ocelot | Solo home behavior, opportunistic hunting, flee/return. |
| Aquatic predator | dolphin | Pod behavior, aquatic targeting, controlled combat. |
| Hungry swarm predator | spider, cave spider | Hunger, swarm hunting/scavenging, target ownership. |
| Hive colony | bee | Hive/home behavior, defense, controlled targeting. |
| Nether hungry | piglin, hoglin | Hunger behavior plus faction/territory interactions for piglins. |
| Undead hungry | zombie, zombie villager, husk, drowned, zombified piglin | Hunger/horde behavior, faction targeting, undead tolerance. |
| Undead tolerant | skeleton, stray, bogged | Ranged behavior, faction targeting, no hunger loop. |
| Phantom stalker | phantom | Stalking behavior and owned attack target. |
| Ghast artillery | ghast | Artillery targeting and faction exclusions. |
| Zoglin rampager | zoglin | Rampage targeting and owned attack target. |
| Slime hungry | slime, magma cube | Hunger/faction behavior. |
| Small arthropod swarm | silverfish, endermite | Swarm behavior and faction targeting. |
| Protective neutral | polar bear | Defensive targeting, no invalid player retention. |
| Armadillo defensive | armadillo | Defensive/flee behavior. |
| Panda bamboo | panda | Bamboo hunger/forage behavior. |
| Sniffer forager | sniffer | Foraging and home/range behavior. |
| Turtle beach | turtle | Beach/home behavior. |
| Amphibian forager | frog | Foraging and controlled prey targeting. |
| Aquatic helper predator | axolotl | Helper targeting and controlled combat. |
| Aquatic territory guard | guardian, elder guardian | Guard behavior, special elder guardian behavior, target ownership. |
| Territory guard | iron golem, snow golem, piglin brute, blaze, shulker, wither skeleton | Guard post return/leash behavior and faction/territory interactions where applicable. |
| Commander support | evoker, witch | Support behavior, Illager coordination, target ownership. |
| Illager raider | pillager, vindicator, ravager, vex, illusioner | Illager faction behavior and territory warning where applicable. |
| Special vanilla | creeper, enderman, breeze, creaking | Mostly vanilla behavior with Retold target safety protections. |
| Apex or boss | warden, wither, ender dragon | Mostly exempt from managed AI; keep special/boss behavior intact. |

## In-Game Validation Checklist

Run these before declaring the system done:

| Test | Command focus | Pass condition |
| --- | --- | --- |
| Bastion player entry | `/retoldbehavior warning` on piglin and brute | `Near territory: yes`, visible intruder found, warning target set, warning level progresses before attack. |
| Fortress player entry | `/retoldbehavior warning` on blaze | Same as bastion. |
| Outpost player entry | `/retoldbehavior warning` on pillager | Same as bastion. |
| Mansion player entry | `/retoldbehavior warning` on vindicator/evoker/illusioner | Same as bastion. |
| Creative/spectator entry | `/retoldbehavior get` and `warning` | No retained mob target, warning target, attack target, `ATTACK_TARGET`, or `ANGRY_AT`. |
| Illegal action | break/steal in territory, then `warning` | Suspicion increases and nearby witnesses react without instant vanilla attack unless threshold is reached. |
| Direct guard hit | hit guard, then `warning` | Retaliation attack starts immediately and is marked as started attack. |
| Multiple players | `nearby` and `warning` | Suspicion is per player and per faction territory context. |
| Faction assist near warning | `warning` and `nearby` | Assist does not convert warning-stage player into an immediate attack target. |
| Leave and return | `warning` over time | Suspicion decays as designed and warning reacquires when player returns. |

## Remaining Finish Criteria

1. Compile after every AI pass.
2. Keep `rg` scans clean for unauthorized target writes.
3. Validate the four territory structures in-game.
4. Tune warning timings only after structure behavior is confirmed.
5. Split the current large worktree into reviewable commits before merging.
