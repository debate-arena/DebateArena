# 코드 스타일 가이드 (Code Style Guide)

본 문서는 SSAFY 웹 프로젝트의 백엔드(Spring Boot) 및 프론트엔드(Vue.js) 개발 시 일관된 코드 작성을 위한 컨벤션을 정의합니다.

---

## 📦 공통 사항

- 주석은 **한글로 간결하게**, 명확한 의도를 전달할 수 있도록 작성합니다.
- 커밋 메시지, 브랜치명 등은 `commit_convention.md`, `branch_naming.md` 문서를 따릅니다.
- PR 생성 시에는 코드 스타일 가이드 준수 여부를 확인합니다.

---

## ☕ 백엔드 (Java Spring)

### 언어 및 스타일

- Java 17 이상 사용
- 들여쓰기: 4 spaces
- 중괄호 `{}`는 항상 새 줄에 작성 (K&R 스타일 지양)
- 파일당 public 클래스는 하나만 작성

### 패키지 구조

```text
com.ssafy
├── api           // 컨트롤러, 서비스, DTO 등 API 계층
├── common        // 공용 유틸, 예외 처리, 인증 등
├── config        // Spring 설정 파일
├── db            // Entity 및 Repository
└── GroupCallApplication.java
```

### 네이밍 규칙

| 항목      | 규칙 예시                                      |
| ------- | ------------------------------------------ |
| 클래스명    | `UpperCamelCase` (`UserController`)        |
| 메서드명    | `lowerCamelCase` (`getUserList`)           |
| 변수명     | `lowerCamelCase` (`userName`)              |
| 상수      | `UPPER_SNAKE_CASE` (`MAX_COUNT`)           |
| 패키지/폴더명 | 모두 소문자, 복수형 사용 (`controllers`, `services`) |

### 클래스별 책임 분리
- `Controller` → 요청 수신 및 응답 처리

- `Service` → 비즈니스 로직 수행

- `Repository` → DB 접근

- `DTO` → 요청/응답 모델 정의 (`PostReq`, `PostRes`, `UserRes` 등 명확히 구분)

### 기타
- `@Transactional`은 서비스 계층에서만 사용

- 응답 객체는 `BaseResponseBody` 또는 공통 응답 모델 사용

- Swagger 사용 시 `@ApiOperation`, `@ApiParam` 등 적극 활용

## 🌐 프론트엔드 (Vue.js)

### 기본 설정
- Vue 2 기반 CLI 프로젝트

- ESLint + Prettier 사용 (권장)

### 들여쓰기 및 포맷
- 들여쓰기: 2 spaces

- 세미콜론: 생략

- 작은 따옴표 `'` 사용

- 파일 끝에 개행 삽입

``` text
src/
├── assets/        // 이미지, 폰트 등 정적 자원
├── components/    // 재사용 가능한 Vue 컴포넌트
├── views/         // 페이지 단위 컴포넌트
├── router/        // vue-router 설정
├── store/         // Vuex 상태 관리
├── App.vue
└── main.js
```
### 컴포넌트 규칙
- 파일명은 `PascalCase.vue` (`UserProfile.vue`)
  
- 하나의 파일에는 하나의 컴포넌트만 정의
  
- `props`, `data`, `methods` 순서로 정리
  
- Composition API가 아닌 Options API 기반 사용 (CLI 템플릿 기준)


### 네이밍 규칙
| 항목    | 규칙 예시                          |
| ----- | ------------------------------ |
| 컴포넌트명 | `UserProfile.vue`              |
| 변수/함수 | `lowerCamelCase`               |
| 상수    | `UPPER_SNAKE_CASE`             |
| 디렉터리명 | `kebab-case` (`user-profile/`) |

###  코드 검사 및 린팅
- `npm run lint`로 스타일 검사를 수행합니다.
  
- 백엔드는 IntelliJ 기반 SonarLint, IDE 포맷팅 기능 활용을 권장합니다.
  
- 팀 내 통일을 위해 `.editorconfig` 사용을 고려할 수 있습니다.