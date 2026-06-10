-- ============================================================
--  Flyway Migration V2: refresh_tokens 테이블에 device_id 추가
--
--  [변경 이유]
--  기존 구조: user당 refresh_token이 1개만 존재
--    → 새 디바이스 로그인 시 기존 토큰 삭제 → 다른 디바이스 강제 로그아웃
--    → k6 테스트에서 동일 계정 동시 로그인 시 InnoDB Lock Contention 유발
--
--  [변경 후]
--  device_id(UUID)로 기기를 구분 → 기기별로 독립적인 refresh_token 유지
--    → 노트북/휴대폰/태블릿 각각 로그인 상태 유지 가능
--    → 동시 로그인 시 서로 다른 device_id → 충돌 없음
--
--  [Flyway가 이 파일을 실행하는 시점]
--  서버 시작 시 flyway_schema_history 테이블을 확인하고,
--  V2가 아직 실행되지 않았으면 자동으로 이 ALTER TABLE을 실행합니다.
-- ============================================================

ALTER TABLE refresh_tokens
    ADD COLUMN device_id VARCHAR(36) NOT NULL DEFAULT (UUID()) COMMENT '디바이스/세션 식별자 (UUID)';

-- device_id 컬럼이 추가된 후 기존 UNIQUE 제약을 재설정합니다.
-- 기존: token 값 하나로 유일성 보장
-- 변경 후: (user_id, device_id) 조합으로 "같은 유저의 같은 기기" 유일성 보장
ALTER TABLE refresh_tokens
    ADD CONSTRAINT uq_user_device UNIQUE (user_id, device_id);
