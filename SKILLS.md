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
