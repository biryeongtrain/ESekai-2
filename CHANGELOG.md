# Changelog

## 0.1.0-alpha.1

Current public alpha baseline for server-side RPG foundation work.

- prepared Fabric mod metadata and alpha publishing surface
- added and refreshed publish-facing documentation
- shipped server-side stat, damage, mitigation, progression, and affix foundations
- shipped data-driven skill runtime with selected cast, support merge, resources, cooldowns, charges, and burst casts
- shipped effect utility and ailment runtime including buffs, debuffs, purge, DoT, and `IGNITE/SHOCK/POISON/BLEED/CHILL/FREEZE/STUN`
- shipped named-resource runtime support and prepared-state graph reuse for skill authoring
- validated the alpha baseline with `./gradlew --console=plain compileJava compileGametestJava runGameTest`
- current validation baseline: `377` required Fabric GameTests green
