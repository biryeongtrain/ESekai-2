# player_progression 작성법

## 이 문서가 다루는 것

이 문서는 `data/<namespace>/esekai2/player_progression/*.json` 을 설명합니다.

player progression은 레벨별 경험치 요구량과 레벨 도달 보상을 정의합니다.

## 기본 구조

```json
{
  "experience_to_next_level": [
    100,
    200,
    300,
    0
  ],
  "granted_modifiers": {
    "2": [
      {
        "stat": "esekai2:life",
        "operation": "add",
        "value": 5.0,
        "source_id": "my_pack:level_2_life"
      }
    ]
  }
}
```

실제 파일은 1레벨부터 100레벨까지 총 100개 값이 필요합니다.

## `experience_to_next_level`

각 레벨에서 다음 레벨로 가기 위한 경험치입니다.

중요 규칙:

- 정확히 100개가 있어야 합니다.
- 100레벨 값은 반드시 `0` 이어야 합니다.

즉, 마지막 줄은 항상 더 이상 올라갈 레벨이 없음을 뜻합니다.

## `granted_modifiers`

레벨 도달 시 부여되는 stat modifier 맵입니다.

키는 문자열 형태의 레벨 번호입니다.

예시:

```json
"granted_modifiers": {
  "3": [
    {
      "stat": "esekai2:mana",
      "operation": "add",
      "value": 10.0,
      "source_id": "my_pack:player_level_3_mana"
    }
  ]
}
```

modifier는 [stat](./stats.md) 문서의 stat id를 참조합니다.

`operation` 은 현재 아래를 씁니다.

- `add`
- `increased`
- `more`

## 새 progression 추가 방법

1. 1부터 100까지 `experience_to_next_level` 을 전부 작성합니다.
2. 필요하면 `granted_modifiers` 를 레벨별로 추가합니다.
3. 100레벨 값을 `0` 으로 둡니다.

## 기존 progression 수정 방법

### 경험치 곡선 수정

`experience_to_next_level` 배열을 수정합니다.

### 레벨 보상 수정

`granted_modifiers` 의 해당 레벨만 수정합니다.

## progression 제거 방법

대체 progression을 먼저 준비한 뒤 참조를 옮기는 편이 안전합니다.

## 자주 하는 실수

### 100개가 아닌 배열을 넣는 실수

허용되지 않습니다.

### `granted_modifiers` 키를 숫자가 아닌 문자열로 잘못 적는 실수

레벨 번호 문자열이어야 합니다.

### 100레벨 경험치를 0이 아닌 값으로 두는 실수

허용되지 않습니다.
