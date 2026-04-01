# 액션, 타겟, 조건, 프레디킷 상세

## 이 문서가 다루는 것

이 문서는 skill graph 안에서 실제로 쓰는 4가지 요소를 설명합니다.

1. `targets`
2. `acts`
3. `ifs`
4. `en_preds`

## 1. 타겟 선택자 `targets`

현재 지원되는 target type은 아래 3개입니다.

- `self`
- `target`
- `aoe`

### `self`

시전자 자신을 대상으로 합니다.

```json
{
  "type": "self"
}
```

### `target`

현재 primary target을 대상으로 합니다.

```json
{
  "type": "target"
}
```

### `aoe`

반경 내 대상을 대상으로 합니다.

```json
{
  "type": "aoe",
  "radius": 3.0
}
```

`radius` 는 숫자 대신 value expression도 사용할 수 있습니다.

```json
{
  "type": "aoe",
  "radius": {
    "type": "times_to_cast"
  }
}
```

`targets` 자체에도 `en_preds` 를 붙일 수 있습니다.

```json
{
  "type": "aoe",
  "radius": 3.0,
  "en_preds": [
    {
      "type": "has_target"
    }
  ]
}
```

## 2. 조건 `ifs`

현재 지원되는 condition type은 아래 4개입니다.

- `on_spell_cast`
- `on_hit`
- `on_entity_expire`
- `x_ticks_condition`

### `on_spell_cast`

시전 시점에 반응합니다.

### `on_hit`

projectile 또는 runtime entity가 hit를 만들었을 때 반응합니다.

### `on_entity_expire`

runtime entity가 만료될 때 반응합니다.

### `x_ticks_condition`

고정 tick 간격으로 실행됩니다.

```json
{
  "type": "x_ticks_condition",
  "interval": 6
}
```

`interval` 도 value expression을 받을 수 있습니다.

## 3. 프레디킷 `en_preds`

현재 predicate type은 아래와 같습니다.

- `always`
- `random_chance`
- `has_target`
- `has_effect`
- `has_resource`
- `cooldown_ready`
- `has_charges`
- `has_burst_followup`

### `random_chance`

```json
{
  "type": "random_chance",
  "chance": 0.5
}
```

`chance` 는 0.0부터 1.0까지의 값으로 쓰는 것이 안전합니다.

### `has_target`

현재 target이 있을 때만 통과합니다.

```json
{
  "type": "has_target"
}
```

### `has_effect`

현재 가장 강력한 상태이상/버프 게이트입니다.

단일 effect:

```json
{
  "type": "has_effect",
  "effect_id": "minecraft:speed",
  "subject": "self"
}
```

복수 effect:

```json
{
  "type": "has_effect",
  "effect_ids": [
    "minecraft:speed",
    "esekai2:poison"
  ],
  "match": "any_of",
  "subject": "target"
}
```

반전:

```json
{
  "type": "has_effect",
  "effect_id": "esekai2:freeze",
  "subject": "target",
  "negate": true
}
```

`subject` 는 아래 3개를 씁니다.

- `primary_target`
- `self`
- `target`

`match` 는 아래 2개를 씁니다.

- `any_of`
- `all_of`

### `has_resource`

지정 리소스가 현재 얼마나 남았는지 검사합니다.

```json
{
  "type": "has_resource",
  "resource": "mana",
  "subject": "self",
  "amount": 10.0
}
```

`amount` 는 value expression을 받을 수 있습니다.

### `cooldown_ready`

현재 prepared skill의 cooldown이 비어 있을 때 통과합니다.

### `has_charges`

현재 prepared skill이 charge를 1개 이상 가지고 있을 때 통과합니다.

### `has_burst_followup`

현재 prepared skill이 burst follow-up을 아직 더 쓸 수 있을 때 통과합니다.

## 4. 액션 `acts`

현재 지원되는 action type은 아래와 같습니다.

- `sound`
- `damage`
- `heal`
- `resource_delta`
- `apply_effect`
- `apply_buff`
- `remove_effect`
- `apply_ailment`
- `apply_dot`
- `projectile`
- `summon_at_sight`
- `summon_block`
- `sandstorm_particle`

아래는 authoring 관점에서 자주 쓰는 필드만 추렸습니다.

## `sound`

```json
{
  "type": "sound",
  "sound_id": "minecraft:block.amethyst_block.hit",
  "volume": 1.0,
  "pitch": 1.0
}
```

## `damage`

inline base damage:

```json
{
  "type": "damage",
  "base_damage": {
    "physical": 10.0,
    "fire": 4.0
  }
}
```

calculation reference:

```json
{
  "type": "damage",
  "calculation_id": "my_pack:fireball_primary_hit"
}
```

추가 필드:

- `hit_kind`
- `base_critical_strike_chance`
- `base_critical_strike_multiplier`

## `heal`

```json
{
  "type": "heal",
  "amount": 4.0
}
```

## `resource_delta`

```json
{
  "type": "resource_delta",
  "resource": "mana",
  "amount": 4.0
}
```

이 액션은 지정 리소스를 증가 또는 감소시킵니다.

## `projectile`

```json
{
  "type": "projectile",
  "component_id": "default_entity_name",
  "entity_id": "esekai2:skill_projectile",
  "life_ticks": 10,
  "gravity": false
}
```

중요:

- `component_id` 는 `attached.entity_components` 에 같은 이름으로 존재해야 합니다.
- projectile 본체는 후속 on-hit, on-expire rule을 그 버킷에서 읽습니다.

## `summon_at_sight`

```json
{
  "type": "summon_at_sight",
  "component_id": "strike_echo",
  "entity_id": "minecraft:blaze",
  "life_ticks": 2,
  "gravity": false
}
```

## `summon_block`

```json
{
  "type": "summon_block",
  "component_id": "impact_block",
  "block_id": "minecraft:ender_chest",
  "life_ticks": 4
}
```

## `sandstorm_particle`

```json
{
  "type": "sandstorm_particle",
  "particle_id": "sandstorm:magic",
  "anchor": "caster_hand",
  "offset_y": 1.0
}
```

주요 필드:

- `particle_id`
- `anchor`
- `offset_x`
- `offset_y`
- `offset_z`

## 액션에 predicate 붙이기

action 자체에도 `en_preds` 를 둘 수 있습니다.

```json
{
  "type": "heal",
  "amount": 6.0,
  "en_preds": [
    {
      "type": "has_resource",
      "resource": "mana",
      "subject": "self",
      "amount": 10.0
    }
  ]
}
```

## 새 액션 구성 추가 방법

1. 기존 skill의 가장 가까운 샘플을 찾습니다.
2. target을 먼저 정합니다.
3. 그 target에서 실행할 action을 넣습니다.
4. event 기반이면 `ifs` 를 붙입니다.
5. gating이 필요하면 rule 또는 action에 `en_preds` 를 붙입니다.

## 수정 방법

### 타겟만 바꿀 때

`targets` 배열만 바꿉니다.

### 이벤트만 바꿀 때

`ifs` 를 바꿉니다.

### 같은 액션을 더 조건적으로 만들 때

`en_preds` 를 추가합니다.

## 제거 방법

### 액션 하나만 제거

해당 rule의 `acts` 에서 그 액션만 지우면 됩니다.

### rule 자체 제거

그 rule의 역할이 완전히 사라지면 rule 전체를 제거합니다.

### entity component 제거

그 component를 참조하는 `projectile`, `summon_at_sight`, `summon_block` 도 같이 점검하세요.
