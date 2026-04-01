# skill_support 작성법

## 이 문서가 다루는 것

이 문서는 `data/<namespace>/esekai2/skill_support/*.json` 을 설명합니다.

support는 base skill을 직접 복제하지 않고도 아래를 바꿀 수 있게 해 줍니다.

- 태그
- conditional stat modifier
- skill config
- 액션 파라미터
- appended action
- appended rule

## 기본 구조

```json
{
  "identifier": "my_pack:support_example",
  "effects": [
    {
      "skill_condition": {
        "required_tags": [
          "spell"
        ]
      },
      "config_overrides": [],
      "action_parameter_overrides": [],
      "appended_actions": [],
      "appended_rules": []
    }
  ]
}
```

## top-level 필드

### `identifier`

support의 내부 id입니다.

### `effects`

실제 변경 단위 배열입니다.

한 support 안에 여러 effect를 넣어도 됩니다.

각 effect는 tag 조건이 맞을 때만 적용됩니다.

## `skill_condition`

현재 주로 아래 2개를 씁니다.

- `required_tags`
- `excluded_tags`

예시:

```json
"skill_condition": {
  "required_tags": ["melee"],
  "excluded_tags": ["projectile"]
}
```

## `added_tags`

base skill에 태그를 더합니다.

예시:

```json
"added_tags": [
  "attack"
]
```

## `added_conditional_stat_modifiers`

스탯 modifier를 추가합니다.

예시:

```json
"added_conditional_stat_modifiers": [
  {
    "modifier": {
      "stat": "esekai2:skill_resource_cost",
      "operation": "add",
      "value": 4.0,
      "source_id": "my_pack:support_cost_boost"
    },
    "skill_condition": {
      "required_tags": ["spell"]
    }
  }
]
```

## `config_overrides`

현재 지원되는 config override 필드는 아래입니다.

- `resource`
- `resource_cost`
- `cast_time_ticks`
- `cooldown_ticks`
- `times_to_cast`
- `charges`

예시:

```json
"config_overrides": [
  {
    "resource": "guard",
    "resource_cost": 3.0,
    "cooldown_ticks": 12
  }
]
```

중요:

- override는 whole replace입니다.
- selected cast와 direct cast 모두 같은 merged 결과를 사용합니다.

## `action_parameter_overrides`

특정 action type의 파라미터를 바꿉니다.

예시:

```json
"action_parameter_overrides": [
  {
    "action_type": "apply_ailment",
    "field_overrides": [
      {
        "path": {
          "type": "parameter",
          "parameter_key": "refresh_policy"
        },
        "value": "overwrite"
      }
    ]
  }
]
```

### `action_type`

override할 액션 종류입니다.

예시:

- `damage`
- `apply_effect`
- `apply_ailment`
- `apply_dot`
- `remove_effect`
- `heal`
- `resource_delta`

### `matching_calculation_id`

같은 type action이 여러 개 있을 때 특정 calculation만 좁혀서 바꿀 수 있습니다.

예시:

```json
"matching_calculation_id": "my_pack:fireball_primary_hit"
```

### `field_overrides`

실제 수정 목록입니다.

현재 path type은 아래 2개입니다.

- `parameter`
- `calculation_id`

대부분은 `parameter` 를 씁니다.

예시:

```json
{
  "path": {
    "type": "parameter",
    "parameter_key": "duration_ticks"
  },
  "value": "16.0"
}
```

리스트 교체가 필요한 경우는 배열도 넣을 수 있습니다.

예시:

```json
{
  "path": {
    "type": "parameter",
    "parameter_key": "effect_ids"
  },
  "value": [
    "minecraft:slowness",
    "esekai2:poison"
  ]
}
```

## `appended_actions`

기존 rule 뒤에 액션을 더합니다.

예시:

```json
"appended_actions": [
  {
    "type": "damage",
    "calculation_id": "my_pack:fireball_support_bonus_hit"
  }
]
```

## `appended_rules`

기존 route 버킷에 rule 자체를 추가합니다.

지원 대상:

- `on_cast`
- `entity_component`

예시:

```json
"appended_rules": [
  {
    "target": {
      "type": "on_cast"
    },
    "rules": [
      {
        "targets": [
          {
            "type": "target"
          }
        ],
        "acts": [
          {
            "type": "sound",
            "sound_id": "minecraft:block.note_block.bell"
          }
        ]
      }
    ]
  }
]
```

component bucket에 붙일 때:

```json
"target": {
  "type": "entity_component",
  "component_id": "default_entity_name"
}
```

## 새 support 추가 방법

1. `skill_condition` 으로 대상 태그를 좁힙니다.
2. config를 바꿀지, action을 바꿀지, rule을 추가할지 결정합니다.
3. 가장 작은 변경부터 넣습니다.

권장 패턴:

- cost나 cooldown 조정: `config_overrides`
- effect duration 조정: `action_parameter_overrides`
- 부가 효과 추가: `appended_actions`
- 후속 event route 추가: `appended_rules`

## 기존 support 수정 방법

### 태그 조건 수정

`skill_condition.required_tags`, `excluded_tags` 를 조정합니다.

### 특정 action 수치 수정

해당 `action_type` 의 `field_overrides` 만 조정합니다.

### skill runtime 비용이나 burst를 수정

`config_overrides` 를 조정합니다.

## support 제거 방법

1. 어떤 skill이 이 support를 실제로 쓰는지 먼저 확인합니다.
2. 영향이 없다면 파일을 삭제합니다.
3. 영향이 크면 먼저 해당 support를 item/socket 쪽에서 빼고 삭제합니다.

## 자주 하는 실수

### tag 조건 없이 너무 넓게 적용하는 실수

가능하면 `required_tags` 를 넣으세요.

### `action_type` 는 맞는데 실제 파라미터 키를 틀리는 실수

action 문서를 같이 보고 정확한 `parameter_key` 를 쓰세요.

### `effect_ids` 처럼 리스트 필드를 문자열 하나로 덮어쓰는 실수

리스트 필드는 배열 형태로 넣는 편이 안전합니다.
