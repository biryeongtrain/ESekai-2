# ESekai2

ESekai2 is a server-side Fabric RPG mod foundation inspired by Path of Exile.

This project currently focuses on data-driven combat systems and server-authoritative RPG runtime:

- stats and combat stat aggregation
- damage calculation and mitigation
- data-driven skill execution graphs
- resources, cooldowns, charges, and burst casts
- ailments, buffs, debuffs, purge, and utility actions
- progression, item affixes, and monster affix runtime

## Status

Current release channel: `alpha`

Current mod version: `0.1.0-alpha.1`

Current release verdict: `development / technical preview only`

This build is suitable for development servers, technical previews, and datapack experimentation. It is not a stable content-complete public release yet.

## Environment

- Minecraft: `26.1`
- Java: `25`
- Fabric Loader: `0.18.5`
- Environment: `server`

## Installation

1. Install Fabric Loader for Minecraft `26.1`.
2. Put `ESekai2-0.1.0-alpha.1.jar` into the server `mods` folder.
3. Install the required Fabric API dependency.
4. Keep the bundled Polymer and Sandstorm dependency chain available in the server environment expected by this project.

## Current Alpha Scope

- server-side stat, damage, and mitigation foundation
- data-driven skill preparation and execution with selected-cast and support merge paths
- runtime enforcement for `resource_cost`, `cooldown_ticks`, `charges`, `times_to_cast`, and `disabled_dims`
- mana regeneration plus named-resource runtime support
- prepared-state graph reuse for `resource_cost`, `use_time_ticks`, `cooldown_ticks`, `max_charges`, and `times_to_cast`
- `apply_effect`, `remove_effect`, purge, `apply_dot`, `apply_ailment`, `heal`, and `resource_delta`
- `IGNITE`, `SHOCK`, `POISON`, `BLEED`, `CHILL`, `FREEZE`, and `STUN`
- progression rewards and equipment-backed player combat stats
- item affix persistence and monster affix runtime
- Fabric GameTest-backed regression coverage with a current baseline of `377` required tests

## Known Limitations

- This is still an alpha release.
- Dedicated server smoke and stable release checklist closure are not done yet.
- Trinkets-backed equipment injection is still a follow-up item.
- Java `25` with the current Mixin stack emits an unsupported compatibility warning during test runtime.
- The monster affix invalid-config fallback path still emits an expected warning stacktrace during validation.
- Public gameplay balance and datapack compatibility should be treated as subject to change between alpha releases.

## Validation

The current release candidate is validated with:

`./gradlew --console=plain compileJava compileGametestJava runGameTest`

Current baseline: `377` required Fabric GameTests green.

## Release Readiness

- Suitable now for: development servers, internal alpha distribution, datapack experimentation
- Not suitable yet for: stable public release or compatibility promises
- Main remaining release work:
  - dedicated server smoke / stable checklist closure
  - remaining release stabilization cleanup
  - Trinkets runtime integration follow-up

## Repository

- Homepage: <https://github.com/biryeongtrain/ESekai-2>
- Issues: <https://github.com/biryeongtrain/ESekai-2/issues>
- Sources: <https://github.com/biryeongtrain/ESekai-2>
