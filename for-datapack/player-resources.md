# player_resource 작성법

## 이 문서가 다루는 것

이 문서는 `data/<namespace>/esekai2/player_resource/*.json` 을 설명합니다.

player resource는 스킬이 소비하거나 회복하는 리소스 축을 정의합니다.

현재 샘플:

- `mana`
- `guard`

## 기본 구조

```json
{
  "max_stat": "esekai2:mana",
  "regeneration_per_second_stat": "esekai2:mana_regeneration_per_second",
  "starts_full": true
}
```

## 주요 필드

### `max_stat`

이 리소스의 최대치를 결정하는 stat입니다.

반드시 존재해야 합니다.

### `regeneration_per_second_stat`

초당 회복량 stat입니다.

선택 필드입니다.

없으면 자연 회복이 없습니다.

### `starts_full`

새 상태를 만들 때 최대치로 시작할지 여부입니다.

`mana` 는 보통 `true`, `guard` 같은 축은 `false` 로 둘 수 있습니다.

## 새 resource 추가 방법

1. 먼저 관련 stat을 추가합니다.
2. `player_resource` 파일을 만듭니다.
3. skill의 `config.resource` 에서 이 id를 참조합니다.

예시:

```json
{
  "max_stat": "my_pack:fury",
  "regeneration_per_second_stat": "my_pack:fury_regeneration_per_second",
  "starts_full": false
}
```

## 기존 resource 수정 방법

### 최대치 stat 교체

`max_stat` 를 바꿉니다.

이 변경은 현재 max clamp에 직접 영향이 있습니다.

### 회복량 추가

`regeneration_per_second_stat` 를 추가합니다.

### 시작 상태 변경

`starts_full` 을 바꿉니다.

## resource 제거 방법

1. skill config나 support config override에서 이 resource를 쓰는 곳을 먼저 찾습니다.
2. 관련 stat도 같이 정리할지 결정합니다.
3. 참조가 없으면 파일을 삭제합니다.

## 주의점

### `regeneration_per_second_stat` 와 `max_stat` 을 같은 stat으로 두지 마세요

허용되지 않습니다.

### skill에서 resource 이름을 먼저 쓰고 resource 정의를 나중에 만들지 마세요

가능하면 resource registry를 먼저 만든 뒤 skill이 참조하게 하세요.
