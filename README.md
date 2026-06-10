# 🚀 대규모 트래픽 및 실시간 처리를 위한 MSA 기반 웹 서비스

본 프로젝트는 대용량 데이터 처리와 실시간 양방향 통신, 그리고 고가용성을 고려하여 설계된 **MSA(Microservices Architecture) 기반의 웹 애플리케이션**입니다.
React 프론트엔드와 Spring Boot 기반의 분산 백엔드 서버로 구성되어 있으며, 실시간 채팅, 게시판, 검색, 성능 모니터링 등 다양한 도메인을 포괄합니다. 특히 포트폴리오 작성을 위한 심층적인 트러블슈팅 및 성능 최적화 경험을 담고 있습니다.

---

## 🏗️ 아키텍처 및 시스템 구성도

본 프로젝트는 단일 모놀리식 서버의 한계를 극복하기 위해 역할을 세분화한 마이크로서비스로 구성되었습니다.

*   **API Gateway (`gateway-server`):** 모든 클라이언트의 요청을 단일 진입점으로 받아 적절한 백엔드 서비스로 라우팅합니다.
*   **Config Server (`config-server`, `config-repo`):** 분산된 여러 서버들의 설정 파일(yml)을 중앙 집중화하여 관리합니다.
*   **Core Backend (`backend`):** 사용자 인증(OAuth2), 게시판, 댓글 등 핵심 비즈니스 로직을 담당합니다.
*   **Chat Server (`chat-server`):** WebFlux와 WebSocket/STOMP를 활용하여 실시간 1:1 및 다대다 채팅 기능을 독립적으로 처리합니다.
*   **Batch Server (`batch-server`):** 대용량 데이터의 주기적인 아카이빙 및 배치 처리를 담당합니다.

---

## 🛠️ 기술 스택 (Tech Stack)

### Backend
*   Java, Spring Boot, Spring Cloud (Gateway, Config)
*   Spring Security, OAuth2, JWT
*   Spring Data JPA, QueryDSL
*   WebSocket, STOMP, Spring WebFlux

### Frontend
*   React, Axios, WebSocket Client

### Infrastructure & Database
*   **RDBMS:** MySQL (Primary, Archive 분리)
*   **NoSQL / Search:** Elasticsearch (Nori 한글 형태소 분석기 적용)
*   **Cache / Message Broker:** Redis, Apache Kafka / Zookeeper
*   **Database Migration:** Flyway
*   **Proxy:** Nginx
*   **Container:** Docker, Docker Compose

### Monitoring & DevOps
*   Prometheus, Grafana
*   Spring Boot Actuator, AOP 기반 커스텀 성능 로깅

---

## 💡 주요 기능 (Key Features)

1.  **인증 및 인가 (Authentication & Authorization)**
    *   OAuth2 및 JWT 기반의 안전한 로그인 시스템 구축
    *   권한 기반(작성자/관리자)의 게시글 및 댓글 수정/삭제 접근 제어
2.  **실시간 채팅 시스템 (Real-time Chat)**
    *   WebSocket과 STOMP 프로토콜을 활용한 실시간 양방향 통신
    *   Redis를 활용한 분산 환경에서의 사용자 온라인 상태(TTL 기반) 관리 및 채팅 세션 공유
    *   1:1 다이렉트 메시지 및 공개/비공개 채팅방 검색 및 참여 기능
    *   페이징(Pagination)을 적용한 과거 채팅 내역 최적화 조회
3.  **게시판 및 검색 (Board & Search)**
    *   게시글 및 댓글 작성, 수정, 삭제 기능 (JPA 연관관계 매핑 활용)
    *   Elasticsearch와 Nori 플러그인을 활용한 고성능 풀텍스트 검색(Full-text Search) 기능
4.  **성능 모니터링 및 로깅 (Monitoring & Logging)**
    *   AOP(Aspect Oriented Programming)를 적용하여 비즈니스 로직 침범 없이 전 구간 DB 쿼리 및 메서드 실행 시간 측정
    *   Prometheus와 Grafana를 연동하여 실시간 시스템 리소스, API 응답 시간 및 트래픽 모니터링 대시보드 구축

---

## 🔥 핵심 트러블슈팅 (Trouble Shooting)

프로젝트 개발 간 마주친 주요 문제와 해결 과정은 [PORTFOLIO_POINTS.md](./PORTFOLIO_POINTS.md) 에 상세히 기록되어 있습니다.

**주요 해결 사례 요약:**
*   **AOP 성능 로깅과 읽기 전용 트랜잭션 충돌 해결:** `@Transactional(readOnly = true)` 환경에서 성능 로그를 DB에 저장하려다 발생한 예외를 `TransactionTemplate`과 `PROPAGATION_REQUIRES_NEW`를 도입하여 트랜잭션을 격리함으로써 해결.
*   **MSA 분산 환경의 WebSocket CORS 및 페이징 이슈:** 백엔드 채팅 서버 분리 후 발생한 브라우저 CORS 연결 거부 문제를 WebFlux `SimpleUrlHandlerMapping` 설정으로 해결. 웹소켓 상의 `Pageable` 객체 파싱 오류를 REST API 분리 및 프론트엔드 역순 정렬 렌더링으로 극복.
*   **Redis 캐시 직렬화 및 Elasticsearch 매핑 에러:** DTO 객체의 직렬화(`java.io.Serializable`) 규격 준수 및 Elasticsearch 도커 컨테이너 내 플러그인(`nori`) 수동 설치 후 이미지 재생성을 통한 인프라 오류 해결.
*   **연쇄적인 의존성 주입 실패 디버깅:** Flyway 마이그레이션 스키마 충돌, OAuth2 빈 생성 누락 등으로 인한 `UnsatisfiedDependencyException`을 로그 추적 및 설정 파일 세분화(`backend-dev.yml` 등)를 통해 단계적으로 해결.

---

## 🚀 시작하기 (Getting Started)

### 요구 사항
*   Docker & Docker Compose
*   Java 17+
*   Node.js 18+

### 실행 방법

1.  **인프라 실행 (Database, Redis, Kafka, Elasticsearch 등)**
    ```bash
    docker-compose up -d
    ```

2.  **백엔드 서버 실행**
    각 서비스 폴더(`./config-server`, `./gateway-server`, `./backend`, `./chat-server`)에서 Spring Boot 애플리케이션을 실행합니다. (Config Server를 가장 먼저 실행해야 합니다.)

3.  **프론트엔드 서버 실행**
    ```bash
    cd frontend
    npm install
    npm start
    ```

---

## 📈 향후 개선 계획 (Future Work)
*   **Resilience4j Circuit Breaker 적용:** Kafka 연동 구간 및 외부 API 호출 시 장애 전파를 차단하는 서킷 브레이커 패턴 구현
*   **CI/CD 파이프라인 구축:** GitHub Actions와 Docker를 활용한 자동 빌드 및 배포 자동화 구현
*   **Config Server Git 저장소 전환:** 현재 `native`(로컬 파일) 방식에서 Git 저장소 방식으로 전환하여 설정 변경 이력 관리 및 롤백 기능 확보
