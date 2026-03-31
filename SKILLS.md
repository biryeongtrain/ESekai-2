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
