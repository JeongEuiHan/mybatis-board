# Spring Boot REST API 게시판 프로젝트

Spring Boot 기반 REST API 게시판 프로젝트입니다.  
단순 기능 구현이 아니라 **서버가 클라이언트 요청을 끝까지 책임지는 구조**를 직접 설계하고 구현하는 것을 목표로 개발했습니다.

---

## 이 프로젝트를 만든 이유

웹 서비스를 사용하며,
화면(UI)에서 막아둔 기능도 URL 직접 호출이나 API 조작을 통해
쉽게 우회될 수 있다는 점을 경험했습니다.

그래서 이 프로젝트는 단순한 게시판 구현이 아니라,
**클라이언트 요청을 신뢰하지 않고 서버가 최종 책임을 지는 구조**를
직접 설계하고 구현하는 것을 목표로 시작했습니다.

이를 위해 다음과 같은 방향으로 프로젝트를 설계했습니다.

- 화면 제어에 의존하지 않고 서버에서 권한을 검증
- 중복 요청과 악의적인 요청에도 데이터 무결성이 유지되는 구조
- 규칙이 흩어지지 않도록 Service 계층에 비즈니스 로직 집중

이를 통해 기능 구현을 넘어,
실제 서비스 환경에서도 안전하게 운영 가능한 구조를 만드는 경험을 목표로 했습니다

---

## Project Goals

- REST API 기반 게시판 시스템 설계
- JWT 기반 인증/인가 구조 구현
- 서버 중심의 권한 검증 및 데이터 무결성 설계

---

## 기술 스택

### Backend
- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA
- JWT (Authentication / Authorization)

### Frontend
- React
- Axios
- Vite

### Database
- MySQL

### Tool
- Postman
- Gradle
- Git / GitHub
---

## 기술적 의사결정 (Why)

### Spring Boot
Spring Boot는 인증, 보안, 트랜잭션 같은 백엔드 필수 기능을
프레임워크 차원에서 일관되게 관리할 수 있어 선택했습니다.

Controller는 요청/응답 전달만 담당하고,
권한 검증과 비즈니스 규칙은 Service 계층에서 처리하도록 분리해
요청 검증 로직이 한 곳에 모이도록 설계했습니다.

### Spring Data JPA
Spring Data JPA를 사용해 도메인(Entity) 중심으로 구조를 설계했습니다.

게시글, 사용자, 좋아요 간의 연관관계를 엔티티로 명확히 표현하고,
Service 계층에서 트랜잭션 단위로 데이터를 처리하도록 구성해
비즈니스 규칙이 데이터 변경과 함께 일관되게 적용되도록 했습니다.

ORM 사용 시 N+1 같은 성능 이슈가 발생할 수 있음을 인지하고 있으며,
현재 프로젝트에서는 기본 조회 구조를 유지하되
조회 API 확장 시 fetch join 또는 EntityGraph 적용을 개선 과제로 남겨두었습니다.

### MySQL
MySQL을 사용해 애플리케이션 로직뿐만 아니라
DB 제약 조건으로도 데이터 무결성을 보장하도록 설계했습니다.

좋아요 기능에서는 (user_id, board_id)에 유니크 제약을 적용해
동일 사용자의 중복 요청이 DB 레벨에서도 저장되지 않도록 했으며,
Service 로직 검증과 DB 제약을 함께 사용하는 이중 안전장치 구조를 구현했습니다.

---

## Key Features

### 회원 / 인증
- 회원 가입 / 로그인
- JWT 기반 인증 처리
- Role 기반 접근 제어

### 게시판
- 게시글 CRUD
- 카테고리별 게시판 분리
- 작성자만 수정/삭제 가능

### 좋아요
- 사용자당 게시글 1회만 가능
- Service 검증 + DB 유니크 제약으로 중복 방지

---

## 프로젝트 구성

```text
boardapi/
├─ board/        # Spring Boot REST API 서버
└─ frontend/     # React 클라이언트

```

---

##  아키텍처

<p align="center">
  <img src="./docs/screenshots/architecture.svg" width="800" alt="System Architecture">
</p>

- Controller는 요청/응답만 담당
- **권한 검증과 비즈니스 규칙은 Service 계층에서 처리**
- 규칙이 한 곳에 모이도록 설계해 기능 확장 시에도 기준 유지
---

## 🗂 ERD

<p align="center">
  <img src="./docs/screenshots/erderd4.svg" width="800" alt="ERD Diagram">
</p>

- (user_id, board_id) 유니크 제약으로 좋아요 중복 방지

---

## Troubleshooting (핵심 문제 해결)

### 1. 인증 실패(401)와 권한 부족(403)이 구분되지 않던 문제

**문제 상황**
- 인증되지 않은 요청과 권한이 없는 요청이 동일한 에러로 처리
- 클라이언트에서 원인 파악이 어려움

**해결**
- JWT 인증 필터 적용
- 인증 실패(401)와 권한 부족(403) 응답 분리

**결과**
- 인증 여부와 권한 상태를 명확히 구분 가능

- Authorization 헤더 없이 요청 → 401 Unauthorized  
![Authorization 없음](docs/screenshots/auth-before.PNG)

- Authorization 헤더(JWT) 포함 요청 → 200 OK 
![Authorization 포함](docs/screenshots/auth-after.PNG)

---

### 2. 게시판 카테고리 권한 우회 문제

**문제 상황**
- 프론트엔드 제어 또는 Controller 분기로 권한 처리
- URL 직접 호출로 권한 우회 가능

**해결**
- 모든 게시판 접근 정책을 **Service 계층에서 중앙 관리**
- 카테고리 + 사용자 등급 기준으로 서버에서 최종 검증

**검증 결과**

- SILVER 사용자가 GOLD 게시판 작성 시 → **403 Forbidden**
- 클라이언트 조작과 무관하게 서버에서 정책 강제
  
![GOLD 게시판 권한 차단](docs/screenshots/forbidden-gold.PNG)

### 3. 좋아요 중복 처리 문제

#### 문제 상황
- 동일 사용자의 중복 좋아요 요청으로 카운트 증가 가능

#### 해결
- Service 계층에서 좋아요 존재 여부 검증
- DB 레벨에서 (user_id, board_id) 유니크 제약 적용

```java
likeRepository.existsByUser_LoginIdAndBoardId(loginId, boardId);
```
이를 통해 로직 검증 + DB 제약이라는
이중 안전장치 구조를 적용했습니다.

**결과**
- 로직 + DB 제약의 이중 안전장치 구조 확보
- 동시 요청 상황에서도 데이터 무결성 유지

---

##  테스트
- JUnit5 + Mockito 기반 Service 계층 단위 테스트
- DB/외부 의존성을 Mock 처리해 권한 검증과 비즈니스 로직 분기에 집중
**검증 내용**
- 게시판 목록 조회 시 카테고리·검색 조건별 Repository 호출 및 정렬 정책 검증
- GOLD 게시판 접근 권한 검증 (SILVER 접근 시 AccessDeniedException)
- 작성자 불일치 시 게시글 수정 제한
- 게시글 수정 시 이미지 교체 로직 검증 (기존 이미지 삭제 → 새 이미지 저장)

Service 정책 검증을 목적으로 한 테스트이며,
쿼리/매핑 검증은 @DataJpaTest로 확장 가능하도록 분리했습니다.

---

###  프로젝트를 통해 느낀 점
- 서버는 클라이언트 요청을 신뢰해서는 안 된다
- 권한 검증은 Controller가 아닌 **Service 책임**
- 데이터 무결성은 **로직 + DB 제약**으로 함께 보장해야 한다
- “기능이 된다”와 “서비스로 안전하다”는 전혀 다르다


---

## 실행 방법
1. Backend 실행
   - MySQL 설정
   - application.yml 환경변수 설정
   - `./gradlew bootRun`

2. Frontend 실행
   - `npm install`
   - `npm run dev`
  
---

### Links
- GitHub Repository: https://github.com/JeongEuiHan/boardapi-project
