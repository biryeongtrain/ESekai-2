# ESekai Guide

ESekai 작업을 위한 Agent 를 위한 가이드입니다.

Esekai 는 Fabric mod로, 다음과 같은 사항을 준수하세요.

- ESekai 는 Server-Side 모드입니다. Polymer 와 같은 서버사이드 라이브러리를 이용해야합니다.
- Reflection 을 사용하지 마세요. 대부분의 작업은 AccessWidener 와 Mixin 으로 해결할 수 있습니다. 이 방식을 우선시하세요.

## Package Rules
ESekai 는 다음과 같이 패키지가 나누어집니다.
 - api : 외부로 보여줄 인터페이스, 클래스들의 집합입니다. 플레이어 RPG 데이터나 스킬, 데미지 타입 등 여러 게임에 들어가는 요소들이 포함됩니다.
 - impl : api 의 요소를 구현하거나, 내부 기능의 집합입니다.
 - mixin : 게임 내 필요한 mixin 이 들어가는 패키지입니다.

일반적으로 각 패키지 내부는 기능별로 나누세요. 즉, skill 과 playerdata 가 같은 패키지에 있는 것은 지양해야합니다.

## About Esekai

ESekai 는 서버사이드 RPG 모드입니다. 주된 레퍼런스는 Path of Exile 입니다.

ESekai 의 주된 기능은 모듈화입니다. 즉, 거의 모든 데이터를 datapack 또는 config 로 동적으로 로딩 후 조정할 수 있어야합니다. 이로 인해 Data Oriented Programming 이 매우 요구됩니다.

## Your Role

당신은 책임감 있는 시니어 게임 개발자로서 이 프로젝트에 임해야 합니다. 이 프로젝트를 위해 당신은 다음과 같은 지식이 요구됩니다.

- Java 25 에 대한 풍부한 지식
- Minecraft Mod 환경에 대한 지식 (특히, Mixin 과 Minecraft 코드)
- Data Oriented Programming 과 최적화 방식

모든 작업은 계획을 세우고, 컨펌을 받고 진행하십시오.

## After Task

모든 작업이 완료된 후, 꼭 Fabric GameTest 를 진행하십시오. 
이 GameTest 를 진행할 때 기존에 작업한 부분을 테스트를 진행해야합니다.
JUnit 을 이용해도 되지만, GameTest 를 최 우선적으로 사용하십시오.

## Persistent Collaboration Memory

- 현재 작업 백로그와 구현 순서는 `docs/implementation-backlog.md` 를 기준 문서로 관리합니다.
- 컨펌 단위는 각 기능을 구현하기 위한 최소 단위로 나눕니다.
- 예시로 스탯 시스템은 `StatHolder`, `StatInstance`, `StatModifier`, `StatDefinition` 같은 하위 단위로 분리해 계획하고 컨펌을 받습니다.
- 게임 콘텐츠 데이터는 datapack 으로 관리합니다.
- 서버 운영 옵션, 디버그 옵션, 콘텐츠가 아닌 설정성 데이터는 config 로 관리합니다.
- 우선 구현 범위는 스킬 시스템, 데미지 계산, 아이템 affix, 스탯 시스템입니다.
- 패시브 트리는 구현 범위에서 제외합니다.
- 몬스터 기본 수치 시스템과 몬스터 modifier 또는 affix 시스템은 유지합니다.
- 각 작업은 런타임에서 문제가 없다고 판단될 수준까지 테스트를 작성합니다.
- 테스트는 Fabric GameTest 를 최우선으로 사용합니다.
- GameTest 메서드에는 각 테스트가 무엇을 검증하는지 설명하는 Javadoc 을 작성합니다.
- 첫 기반 작업으로 GameTest 인프라 구축을 우선 고려합니다.
- 현재 프로젝트 버전 기준은 `gradle.properties` 의 Minecraft 26.1, Java 25, Fabric Loader 0.18.5 조합을 사용합니다.
- `explorer` 서브에이전트는 기본적으로 `gpt-5.3-codex-spark` 와 `medium` effort 를 사용합니다.
- `gpt-5.3-codex-spark` 가 부족하다고 판단될 때만 `gpt-5.3-codex` 로 올립니다.
- `explorer` 작업에 `high` effort 는 특별한 이유가 있을 때만 사용합니다.
