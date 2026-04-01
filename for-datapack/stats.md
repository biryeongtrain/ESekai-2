# stat 작성법

## 이 문서가 다루는 것

이 문서는 `data/<namespace>/esekai2/stat/*.json` 을 설명합니다.

stat은 ESekai의 거의 모든 시스템이 공유하는 기본 숫자 축입니다.

사용처 예시:

- 플레이어 life, mana, guard
- resistance
- ailment threshold
- freeze/stun duration increased
- skill resource cost
- skill use time
- skill cooldown
- item affix modifier target
- monster stat baseline

## 기본 구조

```json
{
  "translation_key": "stat.my_pack.fury",
  "default_value": 0.0,
  "min_value": 0.0,
  "max_value": 100.0
}
```

## 주요 필드

### `translation_key`

표시용 번역 키입니다.

blank면 안 됩니다.

### `default_value`

기본값입니다.

### `min_value`

선택 필드입니다.

최소 메타데이터이자 validation 기준으로 사용됩니다.

### `max_value`

선택 필드입니다.

최대 메타데이터이자 validation 기준으로 사용됩니다.

## 새 stat 추가 방법

1. 새 stat id를 정합니다.
2. 의미에 맞는 default/min/max를 정합니다.
3. 실제로 이 stat을 쓰는 resource, affix, monster, progression, skill value가 있는지 연결합니다.

## 기존 stat 수정 방법

### default만 수정

새 플레이어나 fallback 값이 바뀝니다.

### min/max 수정

validation과 의미가 같이 바뀝니다.

특히 resistance, resource, threshold 계열은 이 수정의 영향이 큽니다.

## stat 제거 방법

stat은 참조처가 많아서 제거 비용이 큽니다.

먼저 아래를 전부 찾으세요.

- player_resource
- affix
- player_progression
- monster_stat
- monster_affix
- skill_value
- 코드상 built-in stat 참조

## 자주 하는 실수

### `default_value` 가 `min_value` 보다 작은 실수

허용되지 않습니다.

### `min_value` 가 `max_value` 보다 큰 실수

허용되지 않습니다.

### stat만 만들고 실제 시스템 연결을 안 하는 실수

stat 파일만 있다고 바로 게임 의미가 생기지는 않습니다.
