# monster_level 작성법

## 이 문서가 다루는 것

이 문서는 `data/<namespace>/esekai2/monster_level/*.json` 을 설명합니다.

monster level은 1부터 100까지의 몬스터 성장 표입니다.

## 기본 구조

```json
{
  "entries": [
    {
      "level": 1,
      "damage": 4.99,
      "evasion_rating": 67.0,
      "accuracy_rating": 14.0,
      "experience_points": 20,
      "life": 22.0,
      "summon_life": 15.0,
      "magic_life_bonus_percent": 50.0,
      "rare_life_bonus_percent": 33.0,
      "map_life_bonus_percent": 0.0,
      "map_damage_bonus_percent": 0.0,
      "boss_life_bonus_percent": 0.0,
      "boss_damage_bonus_percent": 0.0,
      "boss_item_quantity_bonus_percent": 0.0,
      "boss_item_rarity_bonus_percent": 0.0
    }
  ]
}
```

실제 파일은 1레벨부터 100레벨까지 정확히 100개 entry가 있어야 합니다.

## entry 필드 설명

- `level`
- `damage`
- `evasion_rating`
- `accuracy_rating`
- `experience_points`
- `life`
- `summon_life`
- `magic_life_bonus_percent`
- `rare_life_bonus_percent`
- `map_life_bonus_percent`
- `map_damage_bonus_percent`
- `boss_life_bonus_percent`
- `boss_damage_bonus_percent`
- `boss_item_quantity_bonus_percent`
- `boss_item_rarity_bonus_percent`

percent 계열은 모두 0 이상이어야 합니다.

## 새 level table 추가 방법

1. 1부터 100까지 모든 row를 작성합니다.
2. 중간 레벨이 빠지지 않게 합니다.
3. rarity/map/boss bonus 계열도 같이 정리합니다.

## 기존 level table 수정 방법

### 생명력 곡선 조정

`life` 와 rarity bonus를 조정합니다.

### 정확도/회피 조정

- `accuracy_rating`
- `evasion_rating`

### map/boss 성격 조정

- `map_*`
- `boss_*`

## 제거 방법

monster level table은 거의 공용 기반이므로, 대체 table 없이 제거하는 것은 권장하지 않습니다.

## 자주 하는 실수

### row 개수가 100이 아닌 실수

허용되지 않습니다.

### level 번호가 중복되거나 빠지는 실수

허용되지 않습니다.

### percent 계열에 음수를 넣는 실수

허용되지 않습니다.
