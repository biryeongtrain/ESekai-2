# monster_stat 작성법

## 이 문서가 다루는 것

이 문서는 `data/<namespace>/esekai2/monster_stat/*.json` 을 설명합니다.

monster stat은 특정 엔티티 타입의 기본 스탯 베이스라인과, monster level table에서 가져올 축을 정합니다.

## 기본 구조

```json
{
  "entity_type": "minecraft:zombie",
  "base_stats": {
    "esekai2:armour": 5.0
  },
  "scaled_stats": {
    "esekai2:life": "life",
    "esekai2:evade": "evade",
    "esekai2:accuracy": "accuracy"
  }
}
```

## 주요 필드

### `entity_type`

대상 몬스터 엔티티 타입입니다.

### `base_stats`

고정 값으로 바로 주입할 stat 맵입니다.

예시:

```json
"base_stats": {
  "esekai2:armour": 5.0
}
```

### `scaled_stats`

monster level table에서 가져올 stat 축입니다.

현재 axis:

- `life`
- `accuracy`
- `evade`

예시:

```json
"scaled_stats": {
  "esekai2:life": "life",
  "esekai2:accuracy": "accuracy"
}
```

중요:

- 같은 stat을 `base_stats` 와 `scaled_stats` 양쪽에 동시에 넣으면 안 됩니다.
- 둘 중 하나는 반드시 있어야 합니다.

## 새 monster stat 추가 방법

1. `entity_type` 를 정합니다.
2. 고정 수치와 스케일링 수치를 나눕니다.
3. 생명력, 정확도, 회피처럼 레벨 테이블을 타야 하는 축은 `scaled_stats` 를 우선 고려합니다.

## 기존 monster stat 수정 방법

### 고정값 조정

`base_stats` 를 수정합니다.

### 레벨 기반 축 변경

`scaled_stats` 를 수정합니다.

예를 들어 accuracy를 더 이상 레벨 테이블에서 받지 않게 하고 싶다면, `scaled_stats` 에서 빼고 `base_stats` 로 이동합니다.

## 제거 방법

몬스터 기본 스탯이 완전히 사라지면 runtime fallback 동작에 의존하게 될 수 있으므로, 대체 정의가 없으면 삭제를 권장하지 않습니다.

## 자주 하는 실수

### 같은 stat을 base와 scaled 양쪽에 넣는 실수

허용되지 않습니다.

### 엔티티별 밸런스 차이를 전부 monster level table에 넣으려는 실수

개별 몬스터 고유 성격은 `base_stats` 에 두는 편이 관리가 쉽습니다.
