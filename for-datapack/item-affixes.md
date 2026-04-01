# affix 작성법

## 이 문서가 다루는 것

이 문서는 `data/<namespace>/esekai2/affix/*.json` 을 설명합니다.

item affix는 아이템이 가지는 prefix/suffix modifier의 정의입니다.

## 기본 구조

```json
{
  "translation_key": "affix.my_pack.trinket_life_t1",
  "kind": "prefix",
  "group_id": "my_pack:trinket_life",
  "tier": 1,
  "minimum_item_level": 20,
  "scope": "global",
  "item_families": [
    "trinket"
  ],
  "modifier_ranges": [
    {
      "stat": "esekai2:life",
      "operation": "add",
      "min_value": 10.0,
      "max_value": 15.0
    }
  ]
}
```

## 주요 필드

### `translation_key`

표시용 번역 키입니다.

### `kind`

아래 2개 중 하나입니다.

- `prefix`
- `suffix`

### `group_id`

같은 계열 affix를 묶는 그룹 id입니다.

나중에 exclusivity나 tier 계층을 읽기 쉽게 만드는 데 중요합니다.

### `tier`

낮을수록 더 강한 티어입니다.

`1` 이상이어야 합니다.

### `minimum_item_level`

이 affix가 등장할 수 있는 최소 item level입니다.

### `scope`

현재 아래 2개입니다.

- `local`
- `global`

의미:

- `local`: 아이템 로컬 문맥에서 적용
- `global`: 소유자 전체 문맥에 적용

### `item_families`

현재 아래 family를 씁니다.

- `weapon`
- `armour`
- `trinket`

### `modifier_ranges`

roll 가능한 stat modifier 범위입니다.

예시:

```json
{
  "stat": "esekai2:accuracy",
  "operation": "add",
  "min_value": 12.0,
  "max_value": 24.0
}
```

## 새 affix 추가 방법

1. 대상 아이템 family를 정합니다.
2. prefix인지 suffix인지 정합니다.
3. group id를 정합니다.
4. tier와 minimum item level을 정합니다.
5. modifier range를 넣습니다.

## 기존 affix 수정 방법

### 수치 조정

`modifier_ranges.min_value`, `max_value` 를 수정합니다.

### 등장 조건 조정

- `minimum_item_level`
- `item_families`

### local/global 성격 변경

`scope` 를 수정합니다.

이 변경은 영향이 큽니다.

## affix 제거 방법

1. 같은 group의 다른 tier가 있는지 확인합니다.
2. 아이템 롤 밸런스에 구멍이 나지 않는지 확인합니다.
3. 문제가 없으면 파일을 삭제합니다.

## 자주 하는 실수

### `min_value` 가 `max_value` 보다 큰 실수

허용되지 않습니다.

### tier 숫자를 직관과 반대로 적는 실수

낮은 tier가 더 강합니다.

### local/global 의미를 혼동하는 실수

skill용 stat은 local affix와 global affix의 의미가 달라질 수 있습니다. 기존 샘플을 먼저 보고 결정하세요.
