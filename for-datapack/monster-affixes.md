# monster_affix / monster_affix_pool 작성법

## 이 문서가 다루는 것

이 문서는 아래 2개 registry를 함께 설명합니다.

- `data/<namespace>/esekai2/monster_affix/*.json`
- `data/<namespace>/esekai2/monster_affix_pool/*.json`

monster affix는 개별 affix 정의이고, monster affix pool은 특정 몬스터가 어떤 affix 후보를 가질지를 정합니다.

## 1. monster_affix

### 기본 구조

```json
{
  "translation_key": "affix.my_pack.monster.brutal_life_prefix",
  "kind": "prefix",
  "weight": 1,
  "minimum_monster_level": 1,
  "allowed_rarities": [
    "magic",
    "rare"
  ],
  "modifier_ranges": [
    {
      "stat": "esekai2:life",
      "operation": "add",
      "min_value": 200.0,
      "max_value": 200.0
    }
  ]
}
```

### 주요 필드

- `translation_key`
- `kind`
- `weight`
- `minimum_monster_level`
- `allowed_rarities`
- `modifier_ranges`

`kind` 는 아래 2개입니다.

- `prefix`
- `suffix`

`allowed_rarities` 는 현재 아래를 씁니다.

- `normal`
- `magic`
- `rare`
- `unique`

보통 affix는 `magic`, `rare` 중심으로 구성합니다.

`modifier_ranges` 구조는 item affix와 비슷합니다.

## 2. monster_affix_pool

### 기본 구조

```json
{
  "entity_type": "minecraft:zombie",
  "default_spawn_context": {
    "level": 68,
    "rarity": "magic",
    "map_monster": false,
    "boss_monster": false
  },
  "candidate_affix_ids": [
    "my_pack:zombie_brutal_life_prefix",
    "my_pack:zombie_fetid_chaos_suffix"
  ]
}
```

### 주요 필드

#### `entity_type`

대상 몬스터 엔티티 타입입니다.

#### `default_spawn_context`

기본 롤 문맥입니다.

필드:

- `level`
- `rarity`
- `map_monster`
- `boss_monster`

주의:

- `boss_monster` 가 `true` 면 `map_monster` 도 `true` 여야 합니다.

#### `candidate_affix_ids`

이 몬스터가 후보로 사용할 affix id 목록입니다.

비어 있으면 안 됩니다.

## 새 monster affix 추가 방법

1. `monster_affix` 에 개별 affix 파일을 만듭니다.
2. `weight`, `minimum_monster_level`, `allowed_rarities` 를 정합니다.
3. `monster_affix_pool` 에 그 affix id를 추가합니다.

## 기존 monster affix 수정 방법

### 등장 확률 조정

`weight` 를 조정합니다.

### 등장 레벨 조정

`minimum_monster_level` 를 조정합니다.

### 적용 대상 rarity 조정

`allowed_rarities` 를 수정합니다.

### 실제 능력치 조정

`modifier_ranges` 를 수정합니다.

## monster affix 제거 방법

1. 해당 affix를 참조하는 `candidate_affix_ids` 를 먼저 정리합니다.
2. pool에서 제거한 뒤 affix 파일을 삭제합니다.

## pool 수정 방법

### 특정 몬스터에 후보 추가

`candidate_affix_ids` 에 새 affix id를 넣습니다.

### 기본 spawn 문맥 조정

`default_spawn_context` 를 수정합니다.

이 값은 현재 world-driven context가 더 확장되기 전 기본 기준 역할을 합니다.

## 자주 하는 실수

### affix 파일은 만들었는데 pool에 안 넣는 실수

이 경우 affix는 존재하지만 실제 롤 후보가 아닙니다.

### pool에서 affix id를 잘못 적는 실수

namespace까지 포함한 정확한 id를 넣어야 합니다.

### `weight` 를 0으로 두는 실수

허용되지 않습니다.
