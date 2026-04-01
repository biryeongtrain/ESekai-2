# 리소스와 런타임 집행 규칙

## 이 문서가 다루는 것

이 문서는 skill `config` 에 들어가는 런타임 집행 필드를 설명합니다.

주요 대상:

- `resource`
- `resource_cost`
- `cast_time_ticks`
- `cooldown_ticks`
- `times_to_cast`
- `charges`
- `charge_regen`
- `disabled_dims`

## 핵심 원칙

skill config 숫자는 단순 설명용 숫자가 아닙니다.

현재 ESekai는 아래 항목을 실제 runtime gate로 집행합니다.

- resource cost
- cooldown
- charges
- times_to_cast burst
- disabled dimensions

즉, datapack에서 이 값을 바꾸면 실제 시전 가능 여부와 반복 사용 방식이 바뀝니다.

## `resource`

기본값은 `mana` 입니다.

예시:

```json
"resource": "mana"
```

또는

```json
"resource": "guard"
```

주의:

- `resource` 는 미리 `player_resource` registry에 등록된 리소스여야 합니다.
- 존재하지 않는 리소스를 쓰면 cast가 안전하게 막히거나 warning이 날 수 있습니다.

## `resource_cost`

성공 cast 시 소비되는 리소스 양입니다.

예시:

```json
"resource_cost": 12.0
```

중요한 런타임 규칙:

- cast가 실제로 성공했을 때만 소비됩니다.
- dimension block, ailment block, cooldown block, charge block, mana block으로 막히면 소비되지 않습니다.
- support가 config override로 바꿀 수 있습니다.

## `cast_time_ticks`

기본 사용 시간입니다.

예시:

```json
"cast_time_ticks": 16
```

이 값은 prepared-state value로도 다시 참조할 수 있습니다.

## `cooldown_ticks`

성공 cast 후 시작되는 cooldown입니다.

예시:

```json
"cooldown_ticks": 20
```

중요:

- 실패한 cast는 cooldown을 시작하지 않습니다.
- support override가 있으면 merged 값이 실제 runtime에 반영됩니다.
- `cooldown_ready` predicate나 `cooldown_remaining` value expression과 같이 쓸 수 있습니다.

## `charges`

최대 charge 수입니다.

예시:

```json
"charges": 2
```

의미:

- `charges > 0` 이면 스킬은 충전형 스킬이 됩니다.
- cast 성공 시 charge 1개를 소모합니다.
- charge가 0이면 cast는 막힙니다.

## `charge_regen`

charge 회복 시간입니다. 초 단위입니다.

예시:

```json
"charge_regen": 1.0
```

중요:

- `charges > 0` 이면 `charge_regen > 0` 이어야 합니다.
- 이 값은 틱이 아니라 초입니다.

## `times_to_cast`

burst window 안에서 총 몇 번까지 cast할 수 있는지 정합니다.

예시:

```json
"times_to_cast": 3
```

현재 런타임 의미:

- opener 포함 총 3회
- burst window 길이: 10 ticks
- 같은 skill을 성공 cast하면 expiry가 다시 갱신됨
- 다른 skill을 성공 cast하면 기존 burst가 즉시 리셋됨
- follow-up cast도 매번 resource, cooldown, charge를 다시 검사함

즉, `times_to_cast` 는 무료 연속 시전이 아닙니다.

각 cast는 여전히 비용을 지불합니다.

## `disabled_dims`

사용 금지 dimension 목록입니다.

예시:

```json
"disabled_dims": [
  "minecraft:overworld"
]
```

현재 차단 순서에서 dimension 검사는 매우 앞에서 일어납니다.

따라서 막힌 cast는 아래 부작용을 만들지 않습니다.

- mana 소비
- cooldown 시작
- charge 소모
- burst 진행

## support override와의 관계

support는 아래 config를 덮어쓸 수 있습니다.

- `resource`
- `resource_cost`
- `cast_time_ticks`
- `cooldown_ticks`
- `times_to_cast`
- `charges`

즉, 최종 런타임 의미는 base skill JSON만 보면 끝나지 않습니다.

socketed support까지 포함한 merged 결과가 실제 값입니다.

## prepared-state 재사용과의 관계

아래 값들은 value expression에서 다시 읽을 수 있습니다.

- `resource_cost`
- `use_time_ticks`
- `cooldown_ticks`
- `max_charges`
- `times_to_cast`

예시:

```json
{
  "type": "resource_delta",
  "resource": "mana",
  "amount": {
    "type": "use_time_ticks"
  }
}
```

## 새 리소스형 스킬 추가 방법

1. `config.resource` 를 정합니다.
2. 그 리소스가 `player_resource` 에 등록되어 있는지 확인합니다.
3. `resource_cost` 를 넣습니다.
4. 필요하면 `charges`, `charge_regen`, `times_to_cast`, `cooldown_ticks` 를 같이 넣습니다.

## 기존 리소스형 스킬 수정 방법

### 비용만 바꿀 때

`resource_cost` 만 바꿉니다.

### 마나 스킬을 guard 스킬로 바꿀 때

`resource` 와 `resource_cost` 를 같이 바꾸는 편이 안전합니다.

### burst 성격만 바꿀 때

`times_to_cast` 만 바꿉니다.

### charged skill로 바꿀 때

`charges` 와 `charge_regen` 을 함께 추가합니다.

## 스킬 제거나 단순화 방법

### cooldown 제거

```json
"cooldown_ticks": 0
```

### charges 제거

```json
"charges": 0
```

이 경우 `charge_regen` 도 같이 제거하거나 0으로 맞추는 편이 좋습니다.

### burst 제거

```json
"times_to_cast": 1
```

## 자주 하는 실수

### `charge_regen` 을 틱으로 넣는 실수

초 단위입니다.

### `times_to_cast` 를 cooldown 면제로 생각하는 실수

아닙니다. follow-up cast도 cooldown 검사를 받습니다.

### 등록되지 않은 resource를 쓰는 실수

반드시 `player_resource` 문서를 보고 먼저 resource를 등록하세요.
