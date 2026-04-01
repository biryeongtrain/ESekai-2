# 공통 작업 규칙

## 이 문서가 다루는 범위

이 문서는 ESekai에서 datapack으로 수정 가능한 기능을 작업할 때 공통으로 지켜야 할 기준을 정리합니다.

현재 이 문서가 다루는 주요 registry 폴더는 다음과 같습니다.

- `data/<namespace>/esekai2/skill`
- `data/<namespace>/esekai2/skill_support`
- `data/<namespace>/esekai2/skill_value`
- `data/<namespace>/esekai2/skill_calculation`
- `data/<namespace>/esekai2/player_resource`
- `data/<namespace>/esekai2/stat`
- `data/<namespace>/esekai2/player_progression`
- `data/<namespace>/esekai2/affix`
- `data/<namespace>/esekai2/monster_stat`
- `data/<namespace>/esekai2/monster_level`
- `data/<namespace>/esekai2/monster_affix`
- `data/<namespace>/esekai2/monster_affix_pool`

config로 관리되는 항목은 이 폴더의 문서 범위가 아닙니다.

## 기본 원칙

### 1. 파일 경로가 곧 registry 경로입니다

예를 들어 아래 파일은 `esekai2:fireball` 스킬이 아닙니다.

```text
data/my_pack/esekai2/skill/fireball.json
```

이 파일의 실제 id는 `my_pack:fireball` 입니다.

즉, namespace는 파일 내용이 아니라 `data/<namespace>` 폴더 이름으로 정해집니다.

### 2. id는 가능하면 고정하세요

한 번 공개된 id를 바꾸면, 아래처럼 그 id를 참조하던 다른 데이터가 전부 깨질 수 있습니다.

- 스킬에서 `calculation_id` 로 참조하던 계산식
- support에서 `matching_calculation_id` 로 지정하던 대상
- 다른 액션이나 predicate에서 참조하던 effect/resource/stat
- monster affix pool이 참조하던 affix id

새 버전을 만들고 싶다면 기존 파일을 덮어쓰기보다 새 id를 추가하는 방식이 더 안전합니다.

### 3. datapack과 config를 섞지 마세요

작업자가 자주 혼동하는 항목은 아래입니다.

- 게임 콘텐츠 데이터: datapack
- 서버 운영 옵션, 디버그 옵션, fallback 정책: config

예를 들어 아래는 datapack 쪽입니다.

- 스킬 정의
- 스킬 서포트
- 플레이어 리소스 정의
- 스탯 정의
- affix 정의
- 몬스터 수치와 몬스터 affix

반대로 서버 운영용 설정은 datapack 문서에 넣지 않습니다.

## 파일 추가 방법

### 새 기능을 추가할 때

1. 대상 registry 문서를 먼저 읽습니다.
2. 기존 샘플과 가장 비슷한 파일을 하나 고릅니다.
3. 새 namespace와 새 파일명으로 복사합니다.
4. id 충돌이 없는지 확인합니다.
5. 참조 대상이 되는 id가 있으면, 참조되는 파일도 함께 추가합니다.

예를 들어 새 projectile spell을 만들면 보통 아래가 같이 필요할 수 있습니다.

- `skill`
- `skill_calculation`
- `skill_support`
- `skill_value`

### 기존 기능을 수정할 때

1. 어떤 id를 누가 참조하는지 먼저 찾습니다.
2. 상위 문서에서 해당 필드의 런타임 의미를 다시 확인합니다.
3. 숫자 조정만 필요한지, 참조 id를 바꾸는 수정인지 구분합니다.

가장 위험한 수정은 아래 2개입니다.

- id 변경
- enum 문자열 변경

이 둘은 가능한 한 피하세요.

### 기능을 제거할 때

1. 먼저 참조 관계를 찾습니다.
2. 참조가 남아 있으면 제거하지 않습니다.
3. 단순 삭제보다, 먼저 미사용 상태를 만들어 두는 것이 안전합니다.

예를 들어 support를 제거할 때는 아래를 먼저 확인해야 합니다.

- socket 아이템이나 선택 스킬 흐름에서 그 support를 쓰는지
- appended rule이나 config override가 다른 동작에 기대고 있는지

## 네이밍 규칙

권장 규칙은 아래와 같습니다.

- 파일명은 snake_case
- 같은 계열은 접두어를 맞춤
- id는 사람이 보고 역할을 짐작할 수 있게 작성

좋은 예시:

- `fireball.json`
- `support_cast_echo.json`
- `zombie_brutal_life_prefix.json`
- `trinket_life_t1.json`

피해야 하는 예시:

- `test1.json`
- `new_skill_final.json`
- `tmp_affix.json`

## 숫자와 단위

ESekai에서 자주 쓰는 단위는 아래와 같습니다.

- 틱 기반 시간: `cast_time_ticks`, `cooldown_ticks`, `duration_ticks`, `life_ticks`
- 초 기반 재생/회복: `charge_regen`, `*_regeneration_per_second`
- 퍼센트 기반 수치:
  - crit chance
  - crit multiplier
  - ailment chance
  - potency multiplier
  - resistance
  - various increased / bonus stats

주의:

- `charge_regen` 은 초 단위입니다.
- `times_to_cast` 는 총 캐스트 횟수입니다.
- `resource_cost` 는 실제 성공 cast 때만 소비됩니다.

## 검증 방법

문서 작업자도 최소한 아래 기준은 알아두는 것이 좋습니다.

기본 검증 명령:

```bash
./gradlew --console=plain compileJava compileGametestJava runGameTest
```

현재 문서 기준 안정 baseline:

- required Fabric GameTest 377개 green

문서를 수정하는 것만으로는 테스트가 필요 없지만, 실제 datapack JSON을 바꾸면 위 명령 기준으로 확인하는 것이 안전합니다.

## 작업 전 체크리스트

- 수정 대상 registry를 정확히 골랐는가
- namespace와 파일명이 원하는 id를 만들고 있는가
- 참조 대상 id가 실제로 존재하는가
- 숫자 단위가 틱인지 초인지 구분했는가
- 해당 문서의 추가/수정/삭제 절차를 읽었는가

## 작업 후 체크리스트

- 새 id와 참조 id를 다시 확인했는가
- 제거한 파일을 참조하는 곳이 남아 있지 않은가
- 기존 샘플과 구조가 일치하는가
- 런타임 의미를 바꾸는 수정이면 support와 selected cast 경로까지 영향이 있는지 확인했는가
