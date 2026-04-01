# 이펙트와 상태이상 작성법

## 이 문서가 다루는 것

이 문서는 아래 기능을 설명합니다.

- `apply_effect`
- `apply_buff`
- `remove_effect`
- `apply_dot`
- `apply_ailment`
- `has_effect`

현재 built-in ailment는 아래 7개입니다.

- `ignite`
- `shock`
- `poison`
- `bleed`
- `chill`
- `freeze`
- `stun`

## 1. `apply_effect` 와 `apply_buff`

둘은 같은 MobEffect application surface를 사용합니다.

새로 작성할 때는 `apply_effect` 를 권장합니다.

예시:

```json
{
  "type": "apply_effect",
  "effect_id": "minecraft:slowness",
  "duration_ticks": 10,
  "amplifier": 0,
  "refresh_policy": "add_duration",
  "ambient": false,
  "show_particles": true,
  "show_icon": true
}
```

주요 필드:

- `effect_id`
- `duration_ticks`
- `amplifier`
- `refresh_policy`
- `ambient`
- `show_particles`
- `show_icon`

`refresh_policy` 로 현재 쓸 수 있는 값:

- `overwrite`
- `longer_only`
- `add_duration`

### 추가 방법

1. 대상 rule에 `apply_effect` 액션을 넣습니다.
2. vanilla effect id 또는 ESekai effect id를 지정합니다.
3. duration과 amplifier를 정합니다.
4. refresh policy를 정합니다.

### 수정 방법

- 지속시간 조정: `duration_ticks`
- 강도 조정: `amplifier`
- 갱신 방식 조정: `refresh_policy`

### 제거 방법

해당 action을 지우거나, 같은 skill 안에서 `remove_effect` 로 대체합니다.

## 2. `remove_effect`

정화와 purge에 쓰는 액션입니다.

지원 방식은 3가지입니다.

1. 단일 `effect_id`
2. 복수 `effect_ids`
3. broad purge `purge`

### 단일 제거

```json
{
  "type": "remove_effect",
  "effect_id": "minecraft:speed"
}
```

### 복수 제거

```json
{
  "type": "remove_effect",
  "effect_ids": [
    "minecraft:speed",
    "esekai2:poison"
  ]
}
```

### broad purge

```json
{
  "type": "remove_effect",
  "purge": "negative"
}
```

`purge` 값:

- `positive`
- `negative`
- `all`

### explicit id와 purge를 같이 쓰는 경우

합집합으로 처리됩니다.

예시:

```json
{
  "type": "remove_effect",
  "purge": "positive",
  "effect_ids": [
    "esekai2:poison"
  ]
}
```

이 경우 positive effect를 지우고, poison도 같이 지웁니다.

### built-in ailment 정화

`remove_effect` 는 built-in ailment effect를 지울 때 attachment payload도 같이 정리합니다.

즉 아래는 poison의 화면상 effect만 지우는 것이 아니라, 실제 ailment 상태도 같이 제거합니다.

```json
{
  "type": "remove_effect",
  "effect_id": "esekai2:poison"
}
```

## 3. `has_effect`

rule, target, action 모두에서 쓸 수 있는 효과 게이트입니다.

### 단일 검사

```json
{
  "type": "has_effect",
  "effect_id": "esekai2:freeze",
  "subject": "target"
}
```

### 복수 검사

```json
{
  "type": "has_effect",
  "effect_ids": [
    "minecraft:speed",
    "esekai2:shock"
  ],
  "match": "all_of",
  "subject": "self"
}
```

### 반전 검사

```json
{
  "type": "has_effect",
  "effect_id": "esekai2:stun",
  "subject": "self",
  "negate": true
}
```

## 4. `apply_dot`

generic skill-owned DoT 입니다.

예시:

```json
{
  "type": "apply_dot",
  "dot_id": "searing_brand",
  "base_damage": {
    "physical": 2.0
  },
  "duration_ticks": 8,
  "tick_interval": 2
}
```

주요 필드:

- `dot_id`
- `base_damage`
- `duration_ticks`
- `tick_interval`

이 DoT는 ailment가 아니라 skill-owned periodic damage입니다.

## 5. `apply_ailment`

built-in ailment 부여 액션입니다.

예시:

```json
{
  "type": "apply_ailment",
  "ailment_id": "ignite",
  "chance": 100.0,
  "duration_ticks": 8,
  "potency_multiplier": 100.0
}
```

주요 필드:

- `ailment_id`
- `chance`
- `duration_ticks`
- `potency_multiplier`
- `refresh_policy`

`refresh_policy` 는 아래 3개를 씁니다.

- `stronger_only`
- `longer_only`
- `overwrite`

주의:

- `add_duration` 은 ailment refresh policy에서 쓰지 않습니다.

## ailment별 authoring 메모

### `ignite`

fire 계열 DoT ailment입니다.

### `shock`

피해 증폭 계열 ailment입니다.

### `poison`

DoT ailment입니다.

### `bleed`

physical 계열 DoT ailment입니다.

### `chill`

이동 속도 저하 계열입니다.

### `freeze`

이동과 cast를 막는 control ailment입니다.

### `stun`

이동과 cast를 막는 control ailment입니다.

## refresh policy 선택 가이드

### stronger_only

더 강한 상태만 기존 상태를 교체하게 만들고 싶을 때 씁니다.

대체로 아래에 어울립니다.

- `ignite`
- `shock`
- `poison`
- `bleed`
- `chill`

### longer_only

더 긴 지속시간만 기존 상태를 교체하게 만들고 싶을 때 씁니다.

대체로 아래에 어울립니다.

- `freeze`
- `stun`

### overwrite

새로 들어온 상태가 항상 기존 상태를 교체하게 만들고 싶을 때 씁니다.

## 새 이펙트형 스킬 추가 방법

### 버프 스킬

1. `self` target을 둡니다.
2. `apply_effect` 를 넣습니다.
3. `effect_id`, `duration_ticks`, `amplifier` 를 정합니다.

### 디버프 스킬

1. `target` target을 둡니다.
2. `apply_effect` 또는 `apply_ailment` 를 넣습니다.

### 정화 스킬

1. `self` 또는 `target` target을 둡니다.
2. `remove_effect` 를 넣습니다.
3. explicit 제거인지 purge인지 정합니다.

## 수정 방법

### 버프/디버프 지속시간 변경

`duration_ticks` 를 바꿉니다.

### ailment 갱신 정책 변경

`refresh_policy` 를 바꿉니다.

### 정화 범위 확장

- 단일 id면 `effect_id` 변경
- 복수 제거면 `effect_ids` 확장
- 분류 정화면 `purge` 추가

## 제거 방법

### 특정 ailment 적용 제거

`apply_ailment` 액션을 삭제합니다.

### cleanse 기능 제거

`remove_effect` 액션을 삭제합니다.

### `has_effect` gating 제거

해당 predicate를 `en_preds` 에서 삭제합니다.

## 자주 하는 실수

### `apply_buff` 와 `apply_effect` 를 다른 시스템으로 생각하는 실수

지금은 같은 계열입니다. 새 작업은 `apply_effect` 를 권장합니다.

### ailment id에 namespace를 섞는 실수

샘플은 `ignite`, `freeze`, `stun` 같은 짧은 id를 사용합니다. 현재 프로젝트 규칙에 맞춰 기존 샘플과 같은 형태를 유지하세요.

### purge와 explicit 제거가 서로 덮어쓴다고 생각하는 실수

합집합입니다.
