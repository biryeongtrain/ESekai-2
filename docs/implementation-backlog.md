# ESekai Implementation Backlog

이 문서는 다음 세션이나 다음 에이전트가 현재 구현 상태와 다음 작업 순서를 빠르게 파악하기 위한 기준 문서입니다.

## Working Rules Snapshot

- 모든 구현은 최소 기능 단위로 계획을 세우고 사용자 컨펌을 받은 뒤 진행합니다.
- 게임 콘텐츠 데이터는 datapack 으로 관리합니다.
- 서버 운영 옵션, 디버그 옵션, 콘텐츠가 아닌 설정성 데이터는 config 로 관리합니다.
- 테스트는 Fabric GameTest 를 최우선으로 사용합니다.
- `api` 패키지에 추가되는 공개 타입과 공개 멤버에는 다른 개발자가 이해할 수 있도록 Javadoc 을 작성합니다.
- 우선 구현 범위는 `스킬 시스템`, `데미지 계산`, `아이템 affix`, `스탯 시스템` 입니다.
- `패시브 트리`는 현재 구현 범위에서 제외합니다.
- `explorer` 서브에이전트는 기본적으로 `gpt-5.3-codex-spark` 와 `medium` effort 를 사용합니다.
- PoE 기반 데미지 축은 현재 기준으로 하드코딩합니다.
  - `PHYSICAL`
  - `FIRE`
  - `COLD`
  - `LIGHTNING`
  - `CHAOS`
  - `FIXED`
- `FIXED` 데미지는 모든 저항과 방어력에 의해 감소되지 않는 특수 타입으로 취급합니다.

## Current Status

### Progress Snapshot

- 완료된 기반 작업은 현재 24개입니다.
- 현재 활성 백로그는 2개입니다.
  - `Full data-driven skill system`
  - `Ailment system`
- 마지막 안정 검증 기준은 `./gradlew --console=plain compileJava compileGametestJava runGameTest` 입니다.
- 마지막 안정 기준에서는 required Fabric GameTest 144개가 전부 통과했습니다.
- 최신 재실행 기준인 `./gradlew --console=plain runGameTest` 에서는 총 157개 GameTest 중 required GameTest 1건이 실패했습니다.
  - `esekai-gametest:skill_execution_game_tests_fireball_runtime_projectile_hits_target_and_restores_expire_block`
  - 실패 메시지: `Projectile hit should damage the zombie target on tick 10`

### Aggregate Coverage

- 테스트 인프라
  - GameTest infrastructure 와 smoke path 가 정리되어 있습니다.
- 스탯 시스템
  - `StatDefinition`, `StatModifier`, `StatInstance`, `StatHolder` 가 구현되어 있습니다.
  - 전투 공용 스탯 축과 datapack stat registry 가 정리되어 있습니다.
- 데미지 계산
  - damage type, breakdown, mitigation, hit/dot split, conversion, extra damage, exposure, penetration, crit, accuracy/evasion 이 구현되어 있습니다.
- 아이템 시스템
  - item affix foundation, affix classification, item level 이 구현되어 있습니다.
  - Trinkets seam 은 준비되어 있지만 실제 장착 주입은 아직 연결되지 않았습니다.
- 스킬 시스템
  - skill tag foundation 과 minimum runtime foundation 이 구현되어 있습니다.
  - 현재는 minimum runtime 까지이며, fully data-driven execution 은 backlog 에 남아 있습니다.
- 몬스터 시스템
  - monster stat baseline, monster level scaling, rarity 기반 item level derivation 이 구현되어 있습니다.
  - monster affix registry, weighted roll, live entity attach, runtime holder resolution 이 구현되어 있습니다.
- 레벨 시스템
  - player level/xp, monster level profile, item level 저장과 파생 규칙이 구현되어 있습니다.

### Completed Foundations

1. GameTest infrastructure
   - Gradle wrapper 가 복구되어 있습니다.
   - `runGameTest` 실행 경로가 설정되어 있습니다.
   - GameTest 전용 `fabric.mod.json` 이 있습니다.
   - 스모크 테스트가 실제로 실행되고 있습니다.

2. StatDefinition
   - `esekai2:stat` dynamic registry 기반이 구현되어 있습니다.
   - `StatDefinition` 은 codec 과 정적 validation 을 가집니다.
   - 샘플 stat datapack 과 GameTest 가 있습니다.

3. StatModifier
   - `StatModifier` 와 `StatModifierOperation` 공개 API 가 구현되어 있습니다.
   - 현재 operation 은 `ADD`, `INCREASED`, `MORE` 입니다.
   - 샘플 modifier decode GameTest 가 있습니다.

4. StatInstance
   - `StatInstance` 공개 API 가 구현되어 있습니다.
   - 현재 계산 규칙은 `ADD -> INCREASED -> MORE -> clamp` 순서로 고정되어 있습니다.
   - 기본값 사용, modifier bucket 계산, min/max clamp GameTest 가 있습니다.

5. StatHolder
   - `StatHolder` 공개 인터페이스와 `StatHolders` 생성 진입점이 구현되어 있습니다.
   - 현재 구현은 stat key 기준 lazy 생성, base value 변경, exact modifier 추가/제거를 지원합니다.
   - 다중 stat 독립성, lazy 생성, exact remove, base value 변경 GameTest 가 있습니다.

6. Derived combat stat foundation
   - `CombatStats` 공개 상수 집합이 구현되어 있습니다.
   - 현재 전투 파생 스탯 축은 생명력, 마나, 에너지 보호막, 방어도, 회피, 4종 저항, 4종 최대 저항으로 고정되어 있습니다.
   - main datapack 기본 정의와 combat stat GameTest 가 있습니다.

7. Damage type foundation
   - `DamageType` 공개 enum 이 구현되어 있습니다.
   - 현재 hardcoded damage type 축은 `PHYSICAL`, `FIRE`, `COLD`, `LIGHTNING`, `CHAOS`, `FIXED` 로 고정되어 있습니다.
   - `FIXED`는 mitigation bypass 타입으로 고정되어 있고, damage type GameTest 가 있습니다.

8. Damage breakdown model
   - `DamagePortion` 과 `DamageBreakdown` 공개 API 가 구현되어 있습니다.
   - 다중 데미지 타입 성분을 하나의 불변 aggregate 로 합성할 수 있습니다.
   - `DamageBreakdown` 은 typed map codec 과 GameTest 를 가집니다.

9. Mitigation layer foundation
   - `DamageMitigations` 와 `DamageMitigationResult` 공개 API 가 구현되어 있습니다.
   - 물리 피해는 PoE 방어도 공식, 원소/카오스 피해는 저항과 최대 저항, `FIXED`는 bypass 규칙으로 처리됩니다.
   - mitigation 결과는 `incomingDamage`, `mitigatedDamage`, 부호 있는 `mitigationDelta` 로 노출됩니다.

10. Damage calculation core
   - `DamageScalingOperation`, `DamageScalingTarget`, `DamageScaling`, `HitDamageCalculation`, `DamageCalculationResult`, `DamageCalculations` 공개 API 가 구현되어 있습니다.
   - hit 계산은 `base -> ADD -> INCREASED -> MORE -> mitigation` 순서로 고정되어 있습니다.
   - scaling 은 explicit entry 입력으로 받고, 최종 결과는 mitigation 결과를 포함한 계산 결과 타입으로 노출됩니다.

11. Hit and damage over time split
   - `DamageOverTimeCalculation`, `DamageOverTimeResult`, `DamageCalculations.calculateDamageOverTime(...)` 공개 API 가 구현되어 있습니다.
   - hit 와 dot 는 공통 scaling 파이프라인을 공유하지만 mitigation 경로가 분리되어 있습니다.
   - dot 는 PoE식으로 물리 방어도를 무시하고, 원소/카오스 저항은 계속 사용하며, `FIXED`는 bypass 를 유지합니다.

12. Damage conversion and extra damage
   - `DamageConversion`, `ExtraDamageGain` 공개 API 가 구현되어 있습니다.
   - hit 와 dot 는 공통 transformation 파이프라인에서 `conversion -> gain as extra -> scaling` 순서를 공유합니다.
   - conversion 은 source 타입별 총합 100% cap 후 비례 정규화되고, extra damage 는 conversion 이후 breakdown 을 기준으로 생성됩니다.

13. Penetration and exposure
   - `ElementalExposure`, `ResistancePenetration` 공개 API 가 구현되어 있습니다.
   - 원소 exposure 는 hit 와 dot 양쪽에 적용되고, penetration 은 hit 에만 적용됩니다.
   - mitigation 순서는 `exposure -> resistance cap -> penetration -> final mitigation` 으로 고정되어 있습니다.

14. Critical system
   - `HitKind`, `HitContext`, `CriticalStrikeResult` 공개 API 가 구현되어 있습니다.
   - hit 계산은 `scaling -> critical strike -> mitigation` 순서를 사용하고, dot 경로는 crit 를 사용하지 않습니다.
   - crit chance 는 attack/spell 분리 stat 축과 `HitContext` base crit 값으로 계산되고, crit multiplier 는 모든 typed hit damage 성분에 공통 적용됩니다.

15. Accuracy and evasion
   - `HitResolutionResult` 공개 API 와 attack hit resolution 계층이 구현되어 있습니다.
   - attack hit 계산은 `scaling -> accuracy/evasion -> critical strike -> mitigation` 순서를 사용하고, spell hit 는 accuracy/evasion 을 우회합니다.
   - hit chance 는 PoE식 accuracy/evasion 공식을 사용하며 `5%..100%` clamp 가 적용되고, miss 시 hit 결과는 zero damage 로 종료됩니다.

16. Item affix foundation
   - `AffixDefinition`, `AffixModifierDefinition`, `RolledAffix`, `Affixes` 공개 API 와 `esekai2:affix` dynamic registry 가 구현되어 있습니다.
   - affix 는 `WEAPON`, `ARMOUR`, `TRINKET` item family gating 과 ranged modifier definition 을 가지며, rolled affix 는 concrete `StatModifier` snapshot 을 저장합니다.
   - `Trinkets (Polymer Port)` 는 compile-only seam 으로 연결되어 있고, 샘플 affix datapack 및 Item affix GameTest 가 있습니다.

17. Item affix classification
   - affix 는 `PREFIX`/`SUFFIX`, `group_id`, `tier`, `minimum_item_level`, `LOCAL`/`GLOBAL` scope 를 가집니다.
   - tier 는 개별 affix entry 로 표현되고, roll 시점에 family 와 minimum item level 둘 다 검증됩니다.
   - rolled affix 는 metadata 를 스냅샷하지 않고 `affixId` 를 기준으로 정의에서 classification 정보를 다시 조회합니다.

18. Skill tag foundation
   - `SkillTag`, `SkillTagCondition`, `SkillDefinition`, `ConditionalStatModifier` 공개 API 와 `esekai2:skill` dynamic registry 가 구현되어 있습니다.
   - 현재 built-in skill tag 축은 `ATTACK`, `SPELL`, `PROJECTILE`, `MELEE`, `AOE`, `MINION`, `TOTEM`, `TRAP`, `MINE` 으로 고정되어 있습니다.
   - skill definition 은 tags 와 기본 resource cost, cast time, cooldown 을 가지며, conditional modifier 는 required/excluded skill tag 조건으로 skill 문맥을 참조할 수 있습니다.

19. Skill system minimum foundation
   - `SkillHitPayload`, `SkillUseContext`, `PreparedSkillHit`, `Skills`, `SkillStats` 공개 API 와 skill hit preparation runtime 이 구현되어 있습니다.
   - skill definition 은 `hit_payload` 를 포함하며, sample skill fixture 는 `basic_strike` 와 `fireball` 로 제공됩니다.
   - runtime stat 축은 `skill_resource_cost`, `skill_use_time_ticks`, `skill_cooldown_ticks` 로 고정되어 있고, `SkillTagGameTests` 는 registry load, codec round-trip, runtime stat resolution, `Skills.prepareHit(...)`, `DamageCalculations.calculateHit(...)` 통합을 검증합니다.

20. Monster stat baseline
   - `MonsterStatDefinition`, `MonsterRegistries`, `MonsterStats` 공개 API 와 `esekai2:monster_stat` dynamic registry 가 구현되어 있습니다.
   - `minecraft:zombie` sample baseline fixture 가 제공되고, runtime resolver 는 `EntityType` 또는 `LivingEntity` 기준으로 fresh `StatHolder` 를 반환합니다.
   - monster stat definition 이 없으면 `Optional.empty()` 를 반환하며, `MonsterStatGameTests` 는 registry load, codec round-trip, validation, resolver output, fresh holder 보장을 검증합니다.

21. Level system foundation
   - `LevelRules`, `LevelProgressionEntry`, `LevelProgressionDefinition`, `LevelRegistries`, `PlayerLevelState`, `PlayerLevels` 공개 API 가 구현되어 있습니다.
   - player progression 은 `esekai2:player_progression` dynamic registry 와 `esekai2:default` datapack fixture 로 관리되고, player level state 는 server-side `SavedData` 로 유지됩니다.
   - `LevelProgressionGameTests` 는 registry load, codec round-trip, invalid progression validation, player level state codec, single/multi level up, 100레벨 clamp 를 검증합니다.

22. Monster level scaling and item level
   - `MonsterLevelEntry`, `MonsterLevelDefinition`, `MonsterLevelContext`, `MonsterLevelProfile`, `MonsterLevels`, `MonsterRarity` 공개 API 와 `esekai2:monster_level` dynamic registry 가 구현되어 있습니다.
   - monster stat baseline 은 이제 `scaled_stats` 축을 가질 수 있고, `minecraft:zombie` sample fixture 는 `life`, `accuracy`, `evade` 를 monster level table 로부터 주입받습니다.
   - `ItemLevels` 는 ItemStack level 저장/조회/삭제와 monster rarity 기반 item level derivation 을 제공하고, `MonsterLevelGameTests` 와 `ItemLevelGameTests` 는 PoE식 rarity offset, map/boss bonus, scaled stat projection, item level storage 를 검증합니다.

23. Monster affix integration
   - `MonsterAffixDefinition`, `MonsterAffixPoolDefinition`, `RolledMonsterAffix`, `MonsterAffixState`, `MonsterAffixes` 공개 API 와 `esekai2:monster_affix`, `esekai2:monster_affix_pool` dynamic registry 가 구현되어 있습니다.
   - monster affix 는 `PREFIX`/`SUFFIX`, `allowed_rarities`, `minimum_monster_level`, `weight`, ranged stat modifier definition 을 가지며, rarity별 affix 개수는 서버 config `config/esekai2-server.json` 의 `monster_affix_counts` 에서 조절됩니다.
   - `Mob.finalizeSpawn(...)` 경로와 lazy runtime initialization 을 통해 live entity attachment 가 붙고, `MonsterStats.resolveRuntimeHolder(...)` 는 scaled baseline + rolled affix modifiers 를 합친 fresh `StatHolder` 를 반환합니다.

24. Full data-driven skill system
   - 최종 목표는 Age of Exile 처럼 skill definition, payload, targeting, runtime stat scaling, execution behavior 를 거의 전부 datapack 으로 기술 가능하게 만드는 것입니다.
   - 현재 minimum skill foundation 위에 effect schema, execution node, target selector, delivery type, resource/runtime rule, scaling source 를 단계적으로 데이터화해야 합니다.
   - 이 항목은 `high priority` 로 유지하고, skill/monster/item 확장 전에 current hardcoded execution 경계를 최대한 datapack-driven schema 로 전환하는 것이 목표입니다.
   - 현재 합의된 방향은 Age of Exile 의 generated spell JSON 구조를 강하게 참고하되, 파티클만 `Sandstorm` effect id 참조로 치환하는 것입니다.
   - 목표 스키마는 top-level `config` 와 `attached` 를 두고, `attached.on_cast` 및 이후 `attached.entity_components` 가 `targets + acts + ifs + en_preds` rule graph 를 구성하는 형태입니다.
   - `acts` 는 AoE 식 action union 을 따르되 ESekai 에서는 `sandstorm_particle` action 을 추가하고, 이 action 은 `particle_id: Identifier` 만 받아 실제 effect 정의와 실행은 Sandstorm 에 맡깁니다.
   - Sandstorm particle 정의는 ESekai skill datapack 이 아니라 Sandstorm 측 resource/config 로 관리하고, ESekai 는 참조와 실행 타이밍만 책임지는 경계를 유지합니다.

### Existing Verification Baseline

- 마지막 안정 기준으로 통과한 명령은 `./gradlew --console=plain compileJava compileGametestJava runGameTest` 입니다.
- 마지막 안정 기준에서는 required Fabric GameTest 144개 전부 통과입니다.
- 최신 재실행 기준인 `./gradlew --console=plain runGameTest` 에서는 required GameTest 1건이 실패했습니다.
- 현재 관찰된 실패 테스트는 아래와 같습니다.
  - `esekai-gametest:skill_execution_game_tests_fireball_runtime_projectile_hits_target_and_restores_expire_block`
  - `Projectile hit should damage the zombie target on tick 10`
- 현재 유지되어야 하는 GameTest 범위는 아래와 같습니다.
  - 모드 로드 스모크 테스트
  - `StatDefinition` 로드 테스트
  - `StatModifier` decode 테스트
  - `StatInstance` 기본값 테스트
  - `StatInstance` modifier bucket 순서 테스트
  - `StatInstance` min/max clamp 테스트
  - `StatHolder` lazy 생성 테스트
  - `StatHolder` 다중 stat 독립성 테스트
  - `StatHolder` exact modifier 제거 테스트
  - `StatHolder` base value 변경 테스트
  - `CombatStats` 기본 정의 테스트
  - `CombatStats` 저항 stat 정의 테스트
  - `CombatStats` 최대 저항 stat 정의 테스트
  - `CombatStats` holder materialization 테스트
  - `DamageType` stable id 테스트
  - `DamageType` fixed bypass 테스트
  - `DamageType` non-fixed mitigation 테스트
  - `DamageBreakdown` aggregate merge 테스트
  - `DamageBreakdown` aggregate-to-aggregate merge 테스트
  - `DamageBreakdown` codec round-trip 테스트
  - `DamageMitigation` 최대 저항 cap 테스트
  - `DamageMitigation` 음수 카오스 저항 증폭 테스트
  - `DamageMitigation` PoE 방어도 공식 테스트
  - `DamageMitigation` fixed bypass 테스트
  - `DamageMitigation` 혼합 breakdown 테스트
  - `DamageMitigation` empty breakdown 테스트
  - `DamageCalculation` flat ADD 신규 타입 생성 테스트
  - `DamageCalculation` all-target INCREASED 테스트
  - `DamageCalculation` specific MORE 테스트
  - `DamageCalculation` scaling 후 mitigation 적용 테스트
  - `DamageCalculation` fixed scaling 후 bypass 테스트
  - `DamageCalculation` invalid all-target ADD validation 테스트
  - `DamageCalculation` scaling codec round-trip 테스트
  - `DamageCalculation` empty calculation 테스트
  - `DamageOverTime` physical armour 무시 테스트
  - `DamageOverTime` elemental resistance 테스트
  - `DamageOverTime` negative chaos resistance 증폭 테스트
  - `DamageOverTime` fixed bypass 테스트
  - `Hit/DamageOverTime` physical mitigation 차이 테스트
  - `DamageOverTime` empty calculation 테스트
  - `DamageTransformation` conversion 이동 테스트
  - `DamageTransformation` over-cap conversion 정규화 테스트
  - `DamageTransformation` conversion 후 extra damage 테스트
  - `DamageTransformation` hit/dot transformation 공유 테스트
  - `DamageTransformation` conversion codec round-trip 테스트
  - `DamageTransformation` extra gain codec round-trip 테스트
  - `DamageTransformation` self conversion validation 테스트
  - `DamageTransformation` self extra gain validation 테스트
  - `DamageMitigation` elemental exposure hit 적용 테스트
  - `DamageMitigation` elemental exposure dot 적용 테스트
  - `DamageMitigation` penetration cap 이후 적용 테스트
  - `DamageMitigation` chaos penetration 음수 저항 증폭 테스트
  - `DamageMitigation` elemental exposure codec round-trip 테스트
  - `DamageMitigation` penetration codec round-trip 테스트
  - `DamageMitigation` exposure type validation 테스트
  - `DamageMitigation` penetration type validation 테스트
  - `DamageCalculation` exposure/penetration after scaling 테스트
  - `DamageOverTime` exposure 적용 테스트
  - `CombatStats` critical strike stat 정의 테스트
  - `CriticalStrike` roll 기반 crit 성공 테스트
  - `CriticalStrike` roll 기반 non-crit 테스트
  - `CriticalStrike` attack/spell stat 분리 테스트
  - `CriticalStrike` crit chance clamp 테스트
  - `CriticalStrike` crit multiplier clamp 테스트
  - `CriticalStrike` mixed typed damage scaling 테스트
  - `CriticalStrike` mitigation 이전 적용 테스트
  - `Accuracy/Evasion` attack hit 성공 테스트
  - `Accuracy/Evasion` attack miss 테스트
  - `Accuracy/Evasion` hit chance 최소 clamp 테스트
  - `Accuracy/Evasion` hit chance 최대 clamp 테스트
  - `Accuracy/Evasion` spell bypass 테스트
  - `Accuracy/Evasion` miss zero damage 테스트
  - `Accuracy/Evasion` hit 이후 crit/mitigation 연계 테스트
  - `Accuracy/Evasion` hit roll validation 테스트
  - `ItemAffix` registry loading 테스트
  - `ItemAffix` codec round-trip 테스트
  - `ItemAffix` empty family validation 테스트
  - `ItemAffix` inverted modifier range validation 테스트
  - `ItemAffix` non-positive tier validation 테스트
  - `ItemAffix` invalid minimum item level validation 테스트
  - `ItemAffix` shared group distinct tier 테스트
  - `ItemAffix` availability helper 테스트
  - `ItemAffix` classification metadata decode 테스트
  - `ItemAffix` snapshot within range 테스트
  - `ItemAffix` distinct roll snapshot 테스트
  - `ItemAffix` family gating 테스트
  - `ItemAffix` item level gating 테스트
  - `SkillTag` registry loading 테스트
  - `SkillTag` skill definition codec round-trip 테스트
  - `SkillTag` condition codec round-trip 테스트
  - `SkillTag` required/excluded overlap validation 테스트
  - `SkillTag` conditional stat modifier decode 테스트
  - `SkillTag` required tag match 테스트
  - `SkillTag` excluded tag allow 테스트
  - `SkillTag` excluded tag reject 테스트
  - `SkillTag` empty condition match 테스트
  - `SkillTag` blank translation key validation 테스트
  - `SkillTag` negative resource cost validation 테스트
  - `SkillTag` negative cast time validation 테스트
  - `SkillTag` negative cooldown validation 테스트
  - `SkillSystem` skill registry load with payload 테스트
  - `SkillSystem` payload codec round-trip 테스트
  - `SkillSystem` definition codec round-trip with payload 테스트
  - `SkillSystem` context roll validation 테스트
  - `SkillSystem` runtime base values 테스트
  - `SkillSystem` attacker stat modifier runtime resolution 테스트
  - `SkillSystem` conditional modifier runtime resolution 테스트
  - `SkillSystem` excluded tag runtime block 테스트
  - `SkillSystem` prepare hit integration 테스트
  - `SkillSystem` calculate hit integration 테스트
  - `MonsterStat` registry loading 테스트
  - `MonsterStat` codec round-trip 테스트
  - `MonsterStat` empty base stats validation 테스트
  - `MonsterStat` non-finite value validation 테스트
  - `MonsterStat` zombie entity type resolution 테스트
  - `MonsterStat` missing definition empty optional 테스트
  - `MonsterStat` fresh holder per call 테스트
  - `LevelProgression` registry load 테스트
  - `LevelProgression` codec round-trip 테스트
  - `LevelProgression` missing level validation 테스트
  - `PlayerLevelState` codec round-trip 테스트
  - `PlayerLevel` single level up 테스트
  - `PlayerLevel` multi level up 테스트
  - `PlayerLevel` max level clamp 테스트
  - `MonsterLevel` registry load 테스트
  - `MonsterLevel` codec round-trip 테스트
  - `MonsterLevel` missing level validation 테스트
  - `MonsterLevel` negative bonus validation 테스트
  - `MonsterLevel` rarity item level offset 테스트
  - `MonsterLevel` rarity/map/boss multiplier 테스트
  - `MonsterLevel` scaled zombie holder projection 테스트
  - `ItemLevel` set/get/clear 테스트
  - `ItemLevel` invalid range validation 테스트
  - `ItemLevel` rarity-based derivation 테스트
  - `CriticalStrike` hit context roll validation 테스트
  - `CriticalStrike` hit context base chance validation 테스트
  - `CriticalStrike` hit context base multiplier validation 테스트

## Ordered Backlog

아래 순서는 현재 기준 권장 구현 순서입니다. 각 항목은 별도의 최소 기능 단위 작업으로 취급합니다.

1. Full data-driven skill system
   - 목적: 현재 minimum skill runtime 을 Age of Exile 지향의 fully data-driven skill schema 로 확장한다.
   - 포함 범위:
     - skill effect schema datapack 화
     - target/delivery/execution node datapack 화
     - hit 외 effect 타입 확장
     - runtime stat scaling 과 condition 축의 데이터화
   - 현재 합의된 설계 스냅샷:
     - top-level 구조는 AoE generated spell JSON 과 유사하게 `identifier`, `config`, `attached`, `manual_tip`, `disabled_dims`, `effect_tip` 축을 우선 고려한다.
     - `attached` 는 `on_cast` 와 이후 `entity_components` 를 포함하는 이벤트 기반 실행 그래프이며, 각 rule 은 `targets`, `acts`, `ifs`, `en_preds` 를 가진다.
     - `targets` 초기 축은 `self`, `target`, `aoe` 를 우선 지원한다.
     - `ifs` 초기 축은 `on_spell_cast`, `on_hit`, `on_entity_expire`, `x_ticks_condition` 을 우선 지원한다.
     - `acts` 초기 축은 `sound`, `damage`, `projectile`, `summon_at_sight`, `summon_block`, `sandstorm_particle` 을 우선 지원한다.
     - AoE 의 `particles_in_radius` 를 그대로 복제하지 않고 ESekai 는 `sandstorm_particle` action 을 도입한다.
     - `sandstorm_particle` action 은 `particle_id` 만 받고, 실제 파티클 authoring 은 Sandstorm 쪽에 둔다.
     - AoE 의 `value_calculation` 류 참조는 ESekai 쪽 `calculation_id` 또는 동급 registry reference 로 치환한다.
   - 우선 컨펌 단위:
     - `AoE-compatible spell schema foundation + sandstorm_particle on_cast 지원`
   - 1차 작업 범위:
     - `SkillDefinition` 을 `config + attached` 중심 구조로 재편
     - `SkillAction`, `SkillCondition`, `SkillTargetSelector` 공개 API 추가
     - `attached.on_cast` rule 해석기 추가
     - 기존 `basic_strike`, `fireball` 을 새 schema 로 마이그레이션
     - `sandstorm_particle` action 을 `on_cast` 경로에 연결
   - 후속 구현 순서:
     - `entity_components` runtime
     - projectile or summon entity event routing
     - `damage/support` integration
     - AoE 식 calculation reference 와 predicate 축 확장
   - 완료 기준:
     - 신규 스킬 1종을 코드 수정 없이 datapack 만으로 추가 가능
     - 기존 sample skill 이 data-driven execution 경로로 동작
     - GameTest 로 datapack-defined skill 실행 검증

2. Ailment system
   - 목적: PoE 감성의 주요 상태이상 계산 축을 도입한다.
   - 포함 범위:
     - `IGNITE`
     - `CHILL`
     - `FREEZE`
     - `SHOCK`
     - `POISON`
     - `BLEED`
     - `STUN`
   - 제외 범위:
     - 모든 세부 공식의 완전 복제
   - 완료 기준:
     - 최소 1개 hit 기반 ailment 와 1개 dot 기반 ailment 가 계산 경로에 연결됨
     - GameTest 로 상태이상 부여 또는 결과 검증

## Next Focus

- 현재 다음 최소 작업은 `Full data-driven skill system` 입니다.
- 그 다음 핵심 작업은 `Ailment system` 입니다.
- 선행 기반은 준비되어 있습니다.
  - `Item affix foundation`
  - `Item affix classification`
  - `StatModifier`
  - `CombatStats`
  - `Skill tag foundation`
  - `Skill system minimum foundation`
  - `Level system foundation`
  - `Monster level scaling and item level`
  - `Monster stat baseline`
  - `Monster affix integration`

## PoE Parity Candidates

아래 항목은 ESekai 가 PoE 감각에 가까워지기 위해 중장기적으로 고려해야 하는 후보들입니다.

- 방어 계층
  - 방어도, 원소 저항, 카오스 저항, 저항 최대치
- 자원 및 생존 축
  - 생명력, 마나, 에너지 보호막
- 피해 계산 세분화
  - hit 와 damage over time 분리
  - damage conversion
  - gain as extra
  - penetration
  - exposure
- 공격 판정
  - critical strike chance
  - critical strike multiplier
  - accuracy
  - evasion
- 스킬 구조
  - skill tags
  - resource cost
  - cast time
  - attack time
  - cooldown
- 아이템 구조
  - prefix / suffix
  - affix group
  - affix tier
  - item level restriction
  - local mod / global mod
- 상태이상
  - ignite
  - chill
  - freeze
  - shock
  - poison
  - bleed
  - stun

## Immediate Next Recommendation

현재 가장 자연스러운 다음 최소 작업은 `Full data-driven skill system` 입니다.

이유는 다음과 같습니다.

- monster 쪽 baseline, level scaling, affix integration 이 준비되었으므로, 이제 남은 큰 병목은 current minimum skill runtime 을 fully data-driven schema 로 끌어올리는 단계입니다.
- 그 다음 단계에서 ailment system 을 붙이면 skill content 와 상태이상 부여를 datapack 실행 그래프에 자연스럽게 연결할 수 있습니다.
- skill, item, monster, damage 기반은 이미 준비되어 있어 다음은 skill schema 전환에 집중하는 것이 맞습니다.

즉, 지금 기준 병목은 `Full data-driven skill system` 입니다.

그 다음 우선순위는 현재 기준으로 아래 순서를 권장합니다.

1. `Full data-driven skill system`
2. `Ailment system`

이 순서를 잡은 이유는 다음과 같습니다.

- full data-driven skill system 을 올리면 현재 minimum skill runtime 을 콘텐츠 제작 가능한 datapack 시스템으로 바꿀 수 있습니다.
- ailment system 은 data-driven skill execution 축이 정리된 뒤 연결하는 편이 후속 schema 확장이 덜 꼬입니다.
- skill foundation 과 monster/item/combat 기반은 준비되어 있어, 다음은 skill schema 전환에 집중하면 됩니다.

## Update Rule

이 문서를 갱신할 때는 아래 원칙을 지킵니다.

- 이미 끝난 작업은 `Completed Foundations` 로 올립니다.
- 다음에 해야 할 최소 단위만 `Ordered Backlog` 에 남깁니다.
- 구현 순서가 바뀌면 이유를 같이 적습니다.
- 새 공개 API 가 추가되면 테스트 기준도 함께 갱신합니다.
