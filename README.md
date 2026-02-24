## 🏟 온라인 실시간 토론 플랫폼 Debate Arena

<img width="2552" height="1296" alt="스크린샷 2025-08-17 210914" src="https://github.com/user-attachments/assets/35c13a4e-e9c5-4316-8957-f7ddec3f4f41" />

## 👨‍👦‍👦 Team

| 왕주영(팀장) | 김형수 | 김정택 | 권우상 | 지준오 |
| :---: | :---: | :---: | :---: | :---: |
| AI | BE | FE | FE | BE |


## 🌈 Service Introduce
#### 랜덤 주제에 대한 토론과 AI 청중단의 판정을 제공하는 실시간 토론 플랫폼


### 주요 기능

- OAuth2 기반 Google 로그인
- 실시간 매칭 시스템
- 실시간 STT (Speech-to-Text)
- 자동 토픽 교체 시스템
- 실시간 음성 통신 (WebSocket + WebRTC)

## 🛠️ 기술 스택

- **Vue.js** 3.5.17
- **TypeScript** 5.8.3
- **SpringBoot** 3.5.3
- **MySQL** 8.0.43
- **Redis** 8.0.3
- **FastApi**
- **Mediasoup** v3


## 🧬 System Archetacture

<img width="875" height="616" alt="diagram-export-2026 -2 -24 -오후-8_27_38" src="https://github.com/user-attachments/assets/f688893a-4313-47e8-94ab-e74677298e98" />

### 시스템 아키텍처 및 기술 구현

**🔹 마이크로서비스 구조**

| 서비스 이름    | 포트              | 주요 기능                                  |
| -------------- | ----------------- | ------------------------------------------ |
| `debate-arena` | 8080              | OAuth2 + JWT 인증, 토픽 관리               |
| `matching`     | 8081              | WebSocket 기반 매칭 서비스                 |
| `debate`       | 8082              | 토론방 관리, STT 처리                      |
| `mediasoup`    | 40000-49999 | 미디어 스트리밍 SFU 서버                   |
| `frontend`     | 3000              | Vue                       |

---

## [API 명세서](https://pushy-edge-187.notion.site/API-234770ac48778027b984c63aab6818b3)

## [기능 시연 영상](https://www.youtube.com/watch?v=TVr5NophoAg&feature=youtu.be)


