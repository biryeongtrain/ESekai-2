# 스킬 기본 구조와 파일 작성법

## 이 문서가 다루는 것

이 문서는 `data/<namespace>/esekai2/skill/*.json` 파일을 작성하는 기본 방법을 설명합니다.

이 문서를 먼저 읽고, 세부 동작은 아래 문서로 이어서 보세요.

- [액션, 타겟, 조건, 프레디킷 상세](./skill-actions.md)
- [리소스와 런타임 집행 규칙](./skill-runtime-resources.md)
- [이펙트와 상태이상 작성법](./skill-effects-and-ailments.md)

## skill 파일이 하는 일

skill 파일은 스킬 하나의 전체 정의입니다.

skill은 크게 4개 영역으로 나뉩니다.

1. `identifier`
2. `config`
3. `attached`
4. 선택 필드인 `manual_tip`, `effect_tip`, `disabled_dims`

## 기본 예시

```json
{
  "identifier": "my_pack:ember_bolt",
  "config": {
    "resource_cost": 8.0,
    "cast_time_ticks": 12,
    "cooldown_ticks": 0,
    "tags": [
      "spell",
      "projectile"
    ]
  },
  "attached": {
    "on_cast": [
      {
        "targets": [
          {
            "type": "target"
          }
        ],
        "acts": [
          {
            "type": "sound",
            "sound_id": "minecraft:item.firecharge.use"
          },
          {
            "type": "damage",
            "base_damage": {
              "fire": 14.0
            }
          }
        ]
      }
    ],
    "entity_components": {}
  }
}
```

## 필드 설명

### `identifier`

스킬의 내부 id입니다.

주의:

- 실제 registry id는 파일 경로 namespace와 합쳐집니다.
- `identifier` 문자열도 실제 id와 맞춰 쓰는 것이 좋습니다.
- blank 문자열은 허용되지 않습니다.

### `config`

스킬의 실행 설정입니다.

주요 필드는 아래와 같습니다.

- `casting_weapon`
- `resource`
- `resource_cost`
- `cast_time_ticks`
- `cooldown_ticks`
- `style`
- `cast_type`
- `swing_arm`
- `apply_cast_speed_to_cd`
- `times_to_cast`
- `charges`
- `charge_regen`
- `tags`

런타임 의미는 [리소스와 런타임 집행 규칙](./skill-runtime-resources.md) 에 자세히 정리되어 있습니다.

### `attached`

실행 그래프입니다.

`attached` 는 다음 2개 버킷을 가집니다.

- `on_cast`
- `entity_components`

`on_cast` 는 스킬 시전 성공 시 즉시 시작되는 규칙 목록입니다.

`entity_components` 는 projectile, summon 계열 액션이 만든 런타임 엔티티가 나중에 실행할 규칙 버킷입니다.

### `manual_tip`

스킬 설명용 번역 키입니다.

이 필드는 UI나 문서용 설명 텍스트에 가깝습니다. 스킬의 실제 동작은 바꾸지 않습니다.

### `effect_tip`

효과 설명용 번역 키입니다.

역시 런타임 동작보다 설명 텍스트에 가까운 필드입니다.

### `disabled_dims`

이 스킬을 사용할 수 없는 dimension id 목록입니다.

예시:

```json
"disabled_dims": [
  "minecraft:overworld"
]
```

현재 월드가 이 목록에 포함되면 cast는 실행되지 않습니다.

## `attached.on_cast` 구조

`on_cast` 는 `SkillRule` 배열입니다.

각 rule은 아래 구조를 가집니다.

```json
{
  "targets": [],
  "acts": [],
  "ifs": [],
  "en_preds": []
}
```

의미는 다음과 같습니다.

- `targets`: 누구에게 적용할지
- `acts`: 실제로 실행할 액션
- `ifs`: 어떤 이벤트나 주기에 반응할지
- `en_preds`: rule 자체를 열지 닫을지 결정하는 predicate

## `attached.entity_components` 구조

`entity_components` 는 이름이 붙은 rule 버킷 맵입니다.

예시:

```json
"entity_components": {
  "default_entity_name": [
    {
      "targets": [
        {
          "type": "target"
        }
      ],
      "acts": [
        {
          "type": "damage",
          "calculation_id": "my_pack:fireball_primary_hit"
        }
      ],
      "ifs": [
        {
          "type": "on_hit"
        }
      ]
    }
  ]
}
```

이름이 중요한 이유는 아래 액션들이 `component_id` 로 이 버킷을 참조하기 때문입니다.

- `projectile`
- `summon_at_sight`
- `summon_block`

## 새 스킬 추가 방법

1. `data/<namespace>/esekai2/skill/` 아래에 새 JSON 파일을 만듭니다.
2. `identifier` 를 새 id로 정합니다.
3. `config` 에 기본 resource, cast time, cooldown, tags를 넣습니다.
4. `attached.on_cast` 에 최소 1개 rule을 넣습니다.
5. projectile이나 summon을 쓴다면 `entity_components` 도 같이 추가합니다.

권장 시작점:

- 단순 direct skill: `restorative_pulse.json`
- effect skill: `battle_focus.json`
- ailment skill: `kindling_strike.json`
- projectile skill: `fireball.json`
- cleanse skill: `cleanse_focus.json`

## 기존 스킬 수정 방법

### 수치만 바꾸고 싶을 때

보통 아래만 바꾸면 됩니다.

- `resource_cost`
- `cast_time_ticks`
- `cooldown_ticks`
- `duration_ticks`
- `amount`
- `base_damage`

### 동작을 바꾸고 싶을 때

`acts` 배열을 수정합니다.

예를 들어 heal을 damage로 바꾸고 싶다면 아래 둘을 함께 점검하세요.

- 액션 `type`
- 액션에 필요한 payload 필드

### 시전 제한을 바꾸고 싶을 때

아래를 확인합니다.

- `disabled_dims`
- `resource`
- `resource_cost`
- `charges`
- `charge_regen`
- `times_to_cast`

## 스킬 제거 방법

1. 해당 skill id를 참조하는 support나 selected cast 경로가 없는지 먼저 찾습니다.
2. `calculation_id`, `component_id`, support override 대상인지 확인합니다.
3. 참조가 없으면 파일을 제거합니다.

가장 위험한 제거는 아래입니다.

- projectile skill 제거
- support가 붙는 유명 skill 제거
- calculation이나 value reference를 많이 쓰는 skill 제거

## 자주 하는 실수

### `identifier` 와 파일 경로 namespace를 다르게 적는 실수

가능하면 맞춰 쓰세요. 런타임 디버깅이 훨씬 쉬워집니다.

### `entity_components` 를 만들지 않고 `projectile` 만 추가하는 실수

projectile이 살아 있어도, 후속 event rule이 없으면 on-hit나 on-expire 동작은 생기지 않습니다.

### `charges` 를 주고 `charge_regen` 을 빼먹는 실수

`charges > 0` 이면 `charge_regen > 0` 이어야 합니다.

### `times_to_cast` 를 cooldown 대체라고 생각하는 실수

아닙니다. `times_to_cast` 는 burst window입니다. 각 cast는 여전히 resource, cooldown, charge를 다시 검사합니다.
