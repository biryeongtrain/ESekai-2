# skill_value 와 값 표현식 작성법

## 이 문서가 다루는 것

이 문서는 `data/<namespace>/esekai2/skill_value/*.json` 과 skill graph 안에서 쓰는 value expression을 설명합니다.

skill value는 재사용 가능한 숫자 표현식입니다.

## skill_value 기본 구조

```json
{
  "value": {
    "type": "stat",
    "stat": "esekai2:skill_resource_cost"
  }
}
```

또는 다른 value를 참조할 수 있습니다.

```json
{
  "value": {
    "type": "reference",
    "reference_id": "my_pack:skill_resource_cost_snapshot"
  }
}
```

## value expression이 들어갈 수 있는 곳

현재 대표적으로 아래 필드에 들어갑니다.

- action `amount`
- action `duration_ticks`
- action `amplifier`
- action `chance`
- action `potency_multiplier`
- action `tick_interval`
- action `life_ticks`
- action `volume`
- action `pitch`
- selector `radius`
- condition `interval`
- predicate `amount`

## 현재 지원되는 expression type

- `constant`
- `reference`
- `stat`
- `resource_current`
- `resource_max`
- `resource_cost`
- `use_time_ticks`
- `cooldown_ticks`
- `max_charges`
- `times_to_cast`
- `cooldown_remaining`
- `charges_available`
- `burst_remaining`

## 1. 상수값

간단한 숫자는 그냥 숫자로 넣어도 됩니다.

```json
"amount": 4.0
```

또는 명시적으로:

```json
"amount": {
  "type": "constant",
  "value": 4.0
}
```

실제 authoring에서는 보통 숫자 그대로 쓰는 편이 더 간단합니다.

## 2. reference

다른 `skill_value` 를 참조합니다.

```json
{
  "type": "reference",
  "reference_id": "my_pack:skill_resource_cost_snapshot"
}
```

여러 스킬에서 같은 숫자 규칙을 재사용할 때 유용합니다.

## 3. stat

공격자 쪽 stat을 읽습니다.

```json
{
  "type": "stat",
  "stat": "esekai2:skill_resource_cost"
}
```

## 4. resource_current

현재 리소스 양을 읽습니다.

```json
{
  "type": "resource_current",
  "resource": "mana",
  "subject": "self"
}
```

## 5. resource_max

리소스 최대치를 읽습니다.

```json
{
  "type": "resource_max",
  "resource": "guard",
  "subject": "self"
}
```

## 6. prepared-state 값

아래 5개는 support merge 이후 최종 prepared 값을 읽습니다.

- `resource_cost`
- `use_time_ticks`
- `cooldown_ticks`
- `max_charges`
- `times_to_cast`

예시:

```json
{
  "type": "resource_cost"
}
```

```json
{
  "type": "times_to_cast"
}
```

이 값들은 support가 config를 바꾸면 그 결과까지 반영됩니다.

## 7. runtime-state 값

아래 3개는 현재 스킬의 live runtime state를 읽습니다.

- `cooldown_remaining`
- `charges_available`
- `burst_remaining`

이 값들은 주로 predicate나 반복 행동 제어에 씁니다.

## prepared-state 재사용 예시

```json
{
  "type": "heal",
  "amount": {
    "type": "resource_cost"
  }
}
```

```json
{
  "type": "x_ticks_condition",
  "interval": {
    "type": "cooldown_ticks"
  }
}
```

```json
{
  "type": "aoe",
  "radius": {
    "type": "times_to_cast"
  }
}
```

## 새 value 추가 방법

1. `skill_value` 폴더에 새 JSON을 만듭니다.
2. `value` 에 expression을 넣습니다.
3. 스킬 쪽에서는 `reference_id` 로 참조합니다.

## 기존 value 수정 방법

같은 value를 여러 스킬이 참조할 수 있으므로, 수정 전에 참조처를 찾는 것이 좋습니다.

### 안전한 수정

- 상수값 조정
- stat id 교체
- resource id 교체

### 위험한 수정

- reference 체인 구조 변경
- widely shared value id의 의미 변경

## value 제거 방법

1. `reference_id` 로 이 값을 참조하는 스킬이나 다른 value가 없는지 찾습니다.
2. 참조가 없으면 파일을 삭제합니다.

## 자주 하는 실수

### prepared-state 값을 config 자기 자신 안에서 다시 참조하려는 실수

지원하지 않습니다. cycle로 간주되고 fallback/warning 경로를 탑니다.

### 숫자면 다 value file로 빼는 실수

한 군데만 쓰는 상수는 inline 숫자가 더 읽기 쉽습니다.
