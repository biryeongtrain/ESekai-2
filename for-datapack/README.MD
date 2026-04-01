# ESekai Datapack Authoring Guide

이 폴더는 ESekai의 datapack 작업자를 위한 작업 문서 모음입니다.

이 문서들의 목표는 다음 3가지입니다.

1. 어떤 기능을 datapack으로 수정할 수 있는지 빠르게 찾게 합니다.
2. 각 기능을 새로 추가하는 방법, 기존 내용을 수정하는 방법, 안전하게 제거하는 방법을 설명합니다.
3. 실제 런타임 의미와 주의점을 같이 적어, 작업자가 JSON만 보고도 동작을 예측할 수 있게 합니다.

## 시작하기

작업을 시작하기 전에 먼저 아래 문서를 읽으세요.

- [공통 작업 규칙](./common-rules.md)

## 스킬 데이터팩

### 스킬 정의

- [스킬 기본 구조와 파일 작성법](./skill-overview.md)

### 스킬 액션, 타겟, 조건, 프레디킷

- [액션, 타겟, 조건, 프레디킷 상세](./skill-actions.md)

### 스킬 리소스, 쿨다운, 차지, 버스트

- [리소스와 런타임 집행 규칙](./skill-runtime-resources.md)

### 버프, 디버프, 정화, 상태이상

- [이펙트와 상태이상 작성법](./skill-effects-and-ailments.md)

### 스킬 서포트

- [skill_support 작성법](./skill-supports.md)

### 값 표현식

- [skill_value 와 값 표현식 작성법](./skill-values.md)

### 계산식

- [skill_calculation 작성법](./skill-calculations.md)

## 플레이어 데이터팩

### 리소스 축

- [player_resource 작성법](./player-resources.md)

### 전투 스탯

- [stat 작성법](./stats.md)

### 레벨 보상

- [player_progression 작성법](./player-progression.md)

## 아이템 데이터팩

### 아이템 affix

- [affix 작성법](./item-affixes.md)

## 몬스터 데이터팩

### 몬스터 기본 수치

- [monster_stat 작성법](./monster-stats.md)

### 몬스터 레벨 테이블

- [monster_level 작성법](./monster-levels.md)

### 몬스터 affix 와 affix pool

- [monster_affix / monster_affix_pool 작성법](./monster-affixes.md)

## 추천 읽는 순서

스킬 작업자라면 아래 순서가 가장 자연스럽습니다.

1. [공통 작업 규칙](./common-rules.md)
2. [스킬 기본 구조와 파일 작성법](./skill-overview.md)
3. [액션, 타겟, 조건, 프레디킷 상세](./skill-actions.md)
4. [이펙트와 상태이상 작성법](./skill-effects-and-ailments.md)
5. [스킬 서포트](./skill-supports.md)
6. [값 표현식](./skill-values.md)
7. [계산식](./skill-calculations.md)

리소스나 레벨링을 만진다면 아래를 추가로 읽으세요.

- [player_resource 작성법](./player-resources.md)
- [stat 작성법](./stats.md)
- [player_progression 작성법](./player-progression.md)

아이템과 몬스터 작업자는 아래 문서를 중심으로 보면 됩니다.

- [affix 작성법](./item-affixes.md)
- [monster_stat 작성법](./monster-stats.md)
- [monster_level 작성법](./monster-levels.md)
- [monster_affix / monster_affix_pool 작성법](./monster-affixes.md)
