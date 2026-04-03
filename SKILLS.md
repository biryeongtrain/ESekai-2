# ESekai Skills Memory

이 문서는 작업 중 반복하지 말아야 할 실수와 사용자 지적사항을 기록하는 운영 메모리입니다.
에이전트는 작업 시작 전에 이 문서를 읽고, 사용자가 작업 방식이나 검증 방식에 대해 지적한 내용이 생기면 즉시 여기에 추가해야 합니다.

## Mandatory Checks

### GameTest Validation

- GameTest 소스가 존재하는 것만으로는 검증이 완료된 것으로 간주하지 않습니다.
- `src/gametest/resources/fabric.mod.json` 의 `fabric-gametest` entrypoints 에 해당 테스트 클래스가 등록되어 있는지 반드시 확인합니다.
- `runGameTest` 성공만 보고 검증 완료로 판단하지 않습니다.
- `build/junit.xml` 또는 동등한 리포트에 기대한 테스트 이름이 실제로 나타나는지 확인해야 합니다.
- 새 기능 검증 시 최소 확인 순서는 다음과 같습니다.
  1. 테스트 메서드가 존재함
  2. GameTest entrypoint 가 등록됨
  3. `runGameTest` 가 통과함
  4. 리포트에 해당 테스트명이 실제로 기록됨

### Agent Delegation

- `game-tester` 에게 테스트를 위임할 때는 테스트 파일만이 아니라 discovery 경로와 실행 증거까지 확인 범위에 포함시킵니다.
- `game-tester` 완료 조건에는 다음이 포함되어야 합니다.
  - 테스트 코드 추가 또는 확인
  - `src/gametest/resources/fabric.mod.json` entrypoint 확인 또는 수정
  - `runGameTest` 실행
  - `build/junit.xml` 에 테스트명 존재 확인
- 에이전트가 “테스트가 이미 있다”고 보고해도 실행 리포트로 교차 검증하기 전에는 완료로 처리하지 않습니다.
- `wait_agent` 의 짧은 timeout 은 진행 상황 확인일 뿐 실패 판정이 아닙니다.
- `game-tester` 같은 장시간 작업 에이전트는 짧은 wait 후 바로 중단하거나 대체하지 않습니다.
- 에이전트가 실제로 막혔는지 확인되기 전에는 충분한 시간을 두고 기다리며, 필요하면 추가 입력으로 범위를 명확히 하고 계속 진행시킵니다.
- 메인 에이전트가 서둘러 작업을 회수해 직접 처리하면, 사용자가 요구한 위임 구조를 깨뜨린 것으로 간주합니다.

## Feedback Log

### 2026-03-31

- 사용자 지적: ailment 가 붙어도 hidden MobEffect identity 가 1틱 뒤 사라져 poison/ignite/bleed 같은 periodic ailment 가 계속 ticking 되지 않을 수 있다.
- 원인:
  - `MobEffect.applyEffectTick(...)` 반환값을 “이번 틱에 데미지 같은 사이드이펙트가 있었는가”로 잘못 해석했다.
  - attachment-backed ailment runtime 에서는 비-damage 틱에 `false` 를 반환할 수 있는데, Minecraft 는 이를 effect 종료 신호로 사용한다.
  - 기존 GameTest 도 “한 번 체력이 줄었는가”만 보거나 `AilmentRuntime.tick(...)` 을 직접 호출해 live MobEffect lifecycle 을 충분히 검증하지 못했다.
- 재발 방지:
  - attachment-backed `MobEffect` 구현은 반환값을 “payload 가 아직 active 인가” 기준으로 설계한다.
  - periodic ailment 테스트는 최소한 다음을 함께 검증한다.
    - 첫 비-damage 틱 뒤에도 hidden effect identity 가 유지됨
    - 여러 interval 에 걸쳐 periodic damage 가 계속 발생함
    - 만료 시 payload 와 effect identity 가 둘 다 정리됨
  - runtime helper 를 직접 호출하는 테스트만으로 live MobEffect lifecycle 이 검증됐다고 간주하지 않는다.

- 사용자 지적: ailment periodic damage 는 너무 빠른 tick cadence 가 아니라 초 단위 cadence 로 적용되어야 한다.
- 원인:
  - DoT ailment 기본 `tick_interval_ticks` 를 `2` 로 두어 poison/ignite/bleed 가 지나치게 자주 damage 를 주고 있었다.
  - 짧은 duration ailment 가 있는 상태에서 interval 만 느리게 바꾸면 damage tick 이 아예 사라질 수 있는 구조도 함께 고려되지 않았다.
- 재발 방지:
  - ailment periodic damage cadence 변경 시 기본 interval 뿐 아니라 `tickCount`, expiry tick 처리, 짧은 duration fallback 을 같이 설계한다.
  - duration 이 interval 보다 짧은 ailment 는 expiry 시점 damage 여부를 명시적으로 결정하고 GameTest 로 고정한다.
  - poison/ignite/bleed 테스트는 “1초 전에는 안 터짐”, “1초 이상이면 주기적으로 터짐”, “짧은 지속시간은 expiry 에서 1회 처리됨”을 함께 검증한다.

- 사용자 지적: command 입력이 registry-backed identifier 인데 `StringArgumentType` 로 받으면 안 되고, `IdentifierArgument` 와 탭 컴플리트를 붙여야 한다.
- 원인:
  - 디버그 커맨드 구현에서 빠르게 surface 를 여는 데만 집중하면서, registry 입력의 타입 안정성과 authoring UX 를 충분히 고려하지 않았다.
  - `skill_id`, `stat_id`, `resource` 같은 값을 문자열로 받으면 잘못된 namespace 처리와 탭 컴플리트 누락이 같이 발생한다.
- 재발 방지:
  - registry-backed id 입력은 기본적으로 `IdentifierArgument` 를 사용한다.
  - command 입력이 registry나 enum 기반이면 argument 타입 자체와 suggestion provider 를 같이 설계한다.
  - `StringArgumentType` 는 free-form 텍스트나 정말 목록형 identifier 를 한 인자에 압축할 때만 예외적으로 사용한다.

- 사용자 지적: Minecraft `26.1` 환경에서는 `fabric-permissions-api` 같은 포함 의존성에 `modImplementation` 을 쓰지 말고 `implementation(include(...))` 를 사용해야 한다.
- 원인:
  - 예전 난독화/로더 관성대로 포함 의존성에 `modImplementation` 을 사용했다.
  - 현재 프로젝트 기준인 Minecraft `26.1` 의 비난독화 환경과 이 프로젝트의 의존성 wiring 규칙을 반영하지 못했다.
- 재발 방지:
  - ESekai `26.1` 환경에서 포함 의존성 추가 시 기본값은 `implementation(include(...))` 로 둔다.
  - `modImplementation` 은 기존 패턴이라는 이유로 관성적으로 쓰지 않고, 실제 Loom/Fabric wiring 필요성이 확인된 경우에만 사용한다.
  - 새 라이브러리 추가 전에는 같은 `build.gradle` 의 기존 dependency style 과 현재 Minecraft/Fabric 버전 계약을 먼저 확인한다.

- 사용자 지적: 병렬 작업 가능한 항목은 메인 에이전트가 먼저 선별하고, 그 하위 작업을 워커에게 직접 위임해야 한다.
- 원인:
  - 병렬 작업 의지는 있었지만, 사용자 확인 없이 어떤 단위를 병렬화할지 메인 에이전트가 충분히 먼저 정리하지 않으면 위임 구조가 모호해질 수 있다.
- 재발 방지:
  - 새 슬라이스 시작 시 먼저 `공용 foundation`, `독립 구현`, `GameTest`, `review` 중 어떤 단위가 병렬 가능한지 메인 에이전트가 선별한다.
  - 병렬 가능한 단위는 가능하면 먼저 워커 ownership 으로 위임하고, 메인 에이전트는 공용 경계 정의와 결과 병합에 집중한다.

### 2026-03-30

- 사용자 지적: 현재 구현에 대한 테스트 검증이 전혀 안 되고 있었다.
- 원인:
  - `SkillExternalEffectGameTests` 등 테스트 클래스는 존재했지만 `src/gametest/resources/fabric.mod.json` 의 `fabric-gametest` entrypoints 에 등록되지 않아 실제 실행되지 않았다.
  - `runGameTest` green 을 실제 신규 기능 검증으로 잘못 해석했다.
  - `game-tester` 위임 시 테스트 파일만 확인하고 discovery/entrypoint/report 확인을 빠뜨렸다.
- 재발 방지:
  - 신규 기능 GameTest 는 반드시 entrypoint 등록 여부까지 확인한다.
  - `runGameTest` 후 `build/junit.xml` 에 기대한 테스트명이 있는지 확인한다.
  - 에이전트 위임 완료를 수용하기 전에 실행 리포트로 검증 범위를 직접 확인한다.

- 사용자 지적: `game-tester` 가 실제 자기 기능을 못했다.
- 원인:
  - 위임 범위를 테스트 파일 수준으로만 제한해 runner 등록 경로를 보지 못하게 했다.
  - 소스 존재와 실행 보장을 혼동했다.
- 재발 방지:
  - 앞으로 `game-tester` 위임 프롬프트에는 entrypoint, 실행, 리포트 확인을 명시적으로 넣는다.
  - 필요하면 테스트 파일과 `src/gametest/resources/fabric.mod.json` 을 같은 ownership 으로 묶어서 위임한다.

- 사용자 지적: `game-tester` 에게 주는 시간이 너무 짧아서, 실제로 작업 중인데 메인 에이전트가 너무 빨리 실패처럼 취급하고 회수했다.
- 원인:
  - 짧은 `wait_agent` timeout 을 완료 실패와 비슷하게 해석했다.
  - 장시간 GameTest 작성/실행 작업에 필요한 대기 시간을 충분히 주지 않았다.
  - 진행 중인 에이전트에게 추가 지시 없이 메인 에이전트가 너무 빨리 직접 처리로 전환했다.
- 재발 방지:
  - `game-tester` 첫 대기는 짧은 polling 이 아니라 충분한 실행 시간을 주는 wait 로 시작한다.
  - timeout 은 실패가 아니라 미완료 신호로만 해석하고, 실제 blocker 가 확인되기 전에는 에이전트를 죽이거나 회수하지 않는다.
  - 테스트 위임 작업은 가능한 한 끝까지 `game-tester` ownership 으로 유지하고, 메인 에이전트는 결과 병합과 최종 검증에 집중한다.

- 사용자 지적: 서브 에이전트를 더 적극적으로 활용하고, 특히 `game-tester` 도 병렬로 늘려 속도를 높여야 한다.
- 원인:
  - 병렬 작업 계획은 제시했지만 실제 ownership 분리와 결과 병합 단계가 느슨했다.
  - 메인 에이전트가 워크트리에 반영된 서브 에이전트 산출물을 다시 스캔하지 않고 중복 파일/entrypoint 를 만들 위험이 있었다.
- 재발 방지:
  - 병렬 작업은 항상 `공용 foundation`, `개별 기능`, `GameTest`, `review` 단위로 ownership 을 분리한다.
  - 서브 에이전트 작업이 끝난 뒤에는 구현 전에 `git status` 와 관련 경로 재탐색으로 워크트리 변화를 다시 확인한다.
  - `game-tester` 는 가능하면 둘 이상 병렬로 운용하고, 메인 에이전트는 테스트 작성 대신 병합과 최종 검증에 집중한다.

- 사용자 지적: mana spending / insufficient-mana 검증은 반드시 양수 `CombatStats.MANA` 를 명시적으로 세팅한 player holder 기준으로 작성해야 한다.
- 원인:
  - 현재 skill runtime 의 mana gate 는 모든 cast 에 전역적으로 걸리는 것이 아니라, player cast 이면서 resolved `CombatStats.MANA` 가 양수일 때만 활성화되어야 한다.
  - 이 조건을 테스트가 명시하지 않으면 기존 non-player cast 나 zero-mana baseline 을 의도치 않게 깨뜨릴 수 있다.
- 재발 방지:
  - mana gate 관련 구현은 `ServerPlayer` + `resolved MANA > 0` 조건에서만 활성화한다.
  - mana 관련 GameTest 는 attacker/player holder 에 양수 `CombatStats.MANA` 를 명시적으로 세팅한다.
  - 기존 cast regression 은 zero-mana 또는 non-player source 에서 그대로 유지되는지 함께 확인한다.

- 사용자 지적: direct `executeOnCast` mana gating 은 attacker stat holder 의 `CombatStats.MANA` 가 양수일 때에만 기대된다.
- 원인:
  - resource runtime 을 실제 집행으로 확장하면서, 기존 direct runtime 테스트 전부에 mana gate 를 일반 적용하면 `default mana = 0` 인 테스트 기반과 충돌할 수 있다.
- 재발 방지:
  - direct player-sourced cast 의 mana gate 는 `attackerStats.resolvedValue(CombatStats.MANA) > 0` 인 경우에만 검증한다.
  - direct `executeOnCast` GameTest 는 mana-positive 케이스와 non-mana baseline 을 분리해 작성한다.
  - coverage 보고 시 direct runtime 테스트가 cooldown/dimension only 인지, mana-positive player gate 까지 포함하는지 명시한다.
