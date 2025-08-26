# 브랜치 네이밍 컨벤션

## 브랜치 전략

- **main**  
  실제 배포되는 안정적인 버전입니다.  
  오직 검증된 코드만 merge됩니다.

- **develop**  
  다음 배포를 위한 개발 브랜치입니다.  
  새로운 기능은 develop 브랜치에서 파생된 하위 브랜치에서 작업한 뒤, 해당 브랜치에 merge합니다.

---

## 브랜치 네이밍 규칙

형식: <상위브랜치>/<작업유형>/<작업내용-또는-이슈번호>


예시:
- `develop/feat/login`
- `develop/fix/button-crash`
- `develop/doc/api-docs`
- `develop/test/signup-validation`

---

## 작업 유형 접두어

| 접두어    | 설명 |
|-----------|------|
| `feat`    | 새로운 기능 추가 (feature) |
| `fix`     | 버그 수정 (bug fix) |
| `chore`   | 빌드 설정, 패키지 업데이트, 배포 등 기타 자잘한 작업 |
| `doc`     | 문서 작성 및 수정 (README, API 문서 등) |
| `refactor`| 기능 변경 없이 코드 구조 개선 |
| `style`   | 코드 포맷팅, 세미콜론 누락, 들여쓰기 등 스타일 수정 |
| `test`    | 테스트 코드 추가 및 수정 |

---

## 권장 네이밍 예시

| 작업 내용               | 브랜치 이름                          |
|------------------------|--------------------------------------|
| 로그인 기능 추가        | `develop/feat/login`                |
| 회원가입 오류 수정      | `develop/fix/signup-error`          |
| README 업데이트        | `develop/doc/update-readme`        |
| ESLint 설정 변경       | `develop/chore/update-eslint`      |
| 중복 코드 리팩토링     | `develop/refactor/remove-duplication` |
| 버튼 스타일 변경       | `develop/style/button-margin`       |
| 로그인 테스트 코드 추가 | `develop/test/login-validation`     |

---

## 기타 규칙
- 브랜치 이름은 **소문자**로 작성합니다.
- 여러 단어는 `하이픈(-)`으로 구분합니다.
- 작업 단위가 명확하게 드러나도록 이름을 지정합니다.