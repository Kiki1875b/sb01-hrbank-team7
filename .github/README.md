# Team 7 

## 팀원 구성

허지웅 [GitHub](https://github.com/Kiki1875b?tab=repositories)

이성근 [GitHub](https://github.com/LeeSG-0114)

김희수 [GitHub](https://github.com/kaya-frog-ramer)

한성지 [GitHub](https://github.com/hyanyul)

이요한 [GitHub](https://github.com/ARlegro)


## 프로젝트 소개
`HR Bank` 프로젝트는 기업의 인적 자원 관리를 효율적으로 수행하기 위한 Open EMS 시스템으로 , Batch 기반으로 데이터를 백업을 처리하며 직원 정보, 직원 정보 내역, 부서 관리 등의 기능을 안정적으로 제공하는 SaaS 개발을 목적으로 합니다.
- 프로그래밍 교육 사이트의 Spring 백엔드 시스템 구축
- 프로젝트 기간 : 2025.03.14 ~ 2025.03.24

## 기술 스텍

- Backend : Spring Boot, Spring Data Jpa, Hibernate, Spring Batch, MapStruct
- Database : H2, PostgreSQL
- Tools
  - GitHub
  - InteliJ
  - Railway

## 팀원별 구현 기능 상세

### 허지웅
- Dashboard Domain API
- Batch Backup Process

### 이성근
- ChangeLog Domain API

### 김희수
- Department Domain API

### 한성지
- Employee Domain API

### 이요한 
- File Storage
- BinaryContent Domain API


## 프로젝트 구조

```text
루트
├─ checkstyle                     # 코드 스타일 설정
├─ gradle/wrapper
├─ hrbank-storage                # 파일 저장소
├─ src
│  ├─ main
│  │  ├─ java/team7/hrbank
│  │  │  ├─ common               # 배치, DTO, 예외, 유틸, 스케줄러 등 공통 모듈
│  │  │  ├─ config               # 설정 파일
│  │  │  └─ domain               # 비즈니스 도메인 모듈
│  │  │     ├─ backup            # 백업 도메인
│  │  │     ├─ binary
│  │  │     ├─ change_log
│  │  │     ├─ department
│  │  │     ├─ employee
│  │  │     └─ employee_statistic 
│  │  │        └─ controller/dto/entity/repository/service
│  └─ resources/static/assets/images # 정적 리소스
└─ test/java/team7/hrbank         # 도메인 별 테스트 코드

```


## 구현 홈페이지 

https://part1-hrbank-team7-production.up.railway.app

## 시스템 아키텍처

![image](https://github.com/user-attachments/assets/7e6b6ad9-0379-479d-9428-66934e4103f4)

## 추가 기능

*see : src/main/java/team7/hrbank/domain/emplyee_statistic/controller/StatController.java*

- POST /api/statistics/today :   금일 업데이트 된 employee 들에 대한 통계 업데이트 작업입니다
- POST /api/statistics/all : employee의 hiredate 가 금일이 아닌 employee 데이터를 통계에 추가하는 작업으로, 통계를 처음부터 다시 생성합니다. 
