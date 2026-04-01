# skill_calculation 작성법

## 이 문서가 다루는 것

이 문서는 `data/<namespace>/esekai2/skill_calculation/*.json` 을 설명합니다.

`skill_calculation` 은 damage action이 참조하는 재사용 계산 payload입니다.

## 기본 구조

```json
{
  "hit_kind": "spell",
  "base_damage": {
    "fire": 18.0
  },
  "base_critical_strike_chance": 6.0,
  "base_critical_strike_multiplier": 160.0
}
```

## 주요 필드

### `hit_kind`

현재 대표적으로 아래 의미로 씁니다.

- `attack`
- `spell`

### `base_damage`

타입별 기본 피해입니다.

예시:

```json
"base_damage": {
  "physical": 12.0,
  "fire": 6.0
}
```

비어 있으면 안 됩니다.

### `base_critical_strike_chance`

기본 치명타 확률입니다. 퍼센트입니다.

### `base_critical_strike_multiplier`

기본 치명타 배율입니다. 퍼센트입니다.

100보다 작은 값은 의미가 약하므로 보통 100 이상으로 잡습니다.

## skill에서 calculation 쓰는 방법

```json
{
  "type": "damage",
  "calculation_id": "my_pack:fireball_primary_hit"
}
```

## inline damage와 calculation의 차이

### inline damage가 좋은 경우

- 한 스킬에서만 쓰는 단순 피해
- quick prototype

### calculation이 좋은 경우

- 여러 액션에서 재사용할 피해
- support가 `matching_calculation_id` 로 특정 대상을 잡아야 할 때
- crit metadata까지 분리 관리하고 싶을 때

## 새 calculation 추가 방법

1. `skill_calculation` 폴더에 새 JSON을 만듭니다.
2. `base_damage` 를 넣습니다.
3. 필요하면 crit chance, crit multiplier를 넣습니다.
4. skill action `damage.calculation_id` 에서 참조합니다.

## 기존 calculation 수정 방법

### 피해량만 조정

`base_damage` 값을 수정합니다.

### crit 성격 수정

- `base_critical_strike_chance`
- `base_critical_strike_multiplier`

### attack/spell 성격 수정

`hit_kind` 를 바꿉니다.

이 변경은 생각보다 영향이 큽니다.

## calculation 제거 방법

1. skill과 support가 이 id를 참조하는지 먼저 찾습니다.
2. `matching_calculation_id` 로 잡히는 support가 있으면 함께 수정합니다.
3. 참조가 없으면 파일을 삭제합니다.

## 자주 하는 실수

### calculation은 만들었는데 skill에서 `calculation_id` 를 안 쓰는 실수

이 경우 파일은 로드되지만 실제로 사용되지 않습니다.

### support가 calculation id 기준으로 damage를 고치는데 id를 바꾸는 실수

support override 대상이 같이 깨질 수 있습니다.
