-- ============================================================
--  MySQL 초기 데이터 자동 삽입 스크립트
--  Docker Compose 볼륨이 비어있을 때(최초 생성 시)만 실행됩니다.
--  볼륨이 이미 있으면 이 스크립트는 실행되지 않습니다.
--
--  k6 부하 테스트용 테스트 계정 5개를 자동으로 생성합니다.
--  비밀번호: Test1234! (BCrypt 해시값 동일)
-- ============================================================

USE authdb;

-- users 테이블이 없으면 삽입을 건너뜁니다 (Flyway/JPA가 먼저 스키마를 생성)
-- INSERT IGNORE를 사용하여 이미 존재하는 계정은 덮어쓰지 않습니다.

INSERT IGNORE INTO users (uuid, email, password, name, phone, birth_date, gender, role, active_profile_type, created_at, updated_at)
VALUES
  (UUID(), 'test1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '테스트유저1', '01011111111', '1990-01-01', 'MALE',   'USER', 'DEFAULT_1', NOW(), NOW()),
  (UUID(), 'test2@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '테스트유저2', '01022222222', '1991-02-02', 'FEMALE', 'USER', 'DEFAULT_1', NOW(), NOW()),
  (UUID(), 'test3@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '테스트유저3', '01033333333', '1992-03-03', 'MALE',   'USER', 'DEFAULT_1', NOW(), NOW()),
  (UUID(), 'test4@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '테스트유저4', '01044444444', '1993-04-04', 'FEMALE', 'USER', 'DEFAULT_1', NOW(), NOW()),
  (UUID(), 'test5@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '테스트유저5', '01055555555', '1994-05-05', 'MALE',   'USER', 'DEFAULT_1', NOW(), NOW());

-- 위 BCrypt 해시값은 "Test1234!" 를 암호화한 값입니다.
-- BCrypt는 동일 평문에서 매번 다른 해시가 생성되므로, Spring Security의 matches()로 검증됩니다.
