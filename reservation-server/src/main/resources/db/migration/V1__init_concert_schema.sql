-- =============================================
-- V1: 콘서트 예매 시스템 초기 스키마
-- =============================================

-- 공연 테이블
CREATE TABLE IF NOT EXISTS concert (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    title           VARCHAR(200)    NOT NULL COMMENT '공연 제목',
    artist          VARCHAR(100)    NOT NULL COMMENT '아티스트/출연진',
    venue           VARCHAR(200)    NOT NULL COMMENT '공연장',
    concert_date    DATETIME        NOT NULL COMMENT '공연 일시',
    sale_open_at    DATETIME        NOT NULL COMMENT '티켓 판매 시작 일시',
    sale_close_at   DATETIME        NOT NULL COMMENT '티켓 판매 종료 일시',
    description     TEXT            COMMENT '공연 설명',
    poster_url      VARCHAR(500)    COMMENT '포스터 이미지 URL',
    status          VARCHAR(20)     NOT NULL DEFAULT 'SCHEDULED' COMMENT 'SCHEDULED/ON_SALE/SOLD_OUT/ENDED',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공연 정보';

-- 구역 테이블 (S석/A석/B석)
CREATE TABLE IF NOT EXISTS section (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    concert_id      BIGINT          NOT NULL,
    name            VARCHAR(50)     NOT NULL COMMENT '구역명 (S석, A석, B석 등)',
    price           INT             NOT NULL COMMENT '구역 가격 (원)',
    total_seats     INT             NOT NULL COMMENT '총 좌석수',
    remain_seats    INT             NOT NULL COMMENT '잔여 좌석수',
    PRIMARY KEY (id),
    CONSTRAINT fk_section_concert FOREIGN KEY (concert_id) REFERENCES concert(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공연 구역';

-- 좌석 테이블
CREATE TABLE IF NOT EXISTS seat (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    section_id      BIGINT          NOT NULL,
    row_num         INT             NOT NULL COMMENT '열 번호',
    col_num         INT             NOT NULL COMMENT '행 번호',
    seat_label      VARCHAR(20)     NOT NULL COMMENT '좌석 레이블 (예: A-1)',
    status          VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE/HELD/RESERVED',
    version         BIGINT          NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전',
    PRIMARY KEY (id),
    CONSTRAINT fk_seat_section FOREIGN KEY (section_id) REFERENCES section(id),
    INDEX idx_seat_section_status (section_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='개별 좌석';

-- 예약 테이블
CREATE TABLE IF NOT EXISTS reservation (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '예약자 ID (backend의 user 참조)',
    seat_id         BIGINT          NOT NULL,
    concert_id      BIGINT          NOT NULL,
    strategy        VARCHAR(20)     NOT NULL COMMENT '예약 처리 전략 (DB_LOCK/REDIS_LOCK/KAFKA)',
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED/CANCELLED',
    expired_at      DATETIME        COMMENT '임시 점유 만료 시각 (PENDING 상태일 때)',
    confirmed_at    DATETIME        COMMENT '예약 확정 시각',
    cancelled_at    DATETIME        COMMENT '취소 시각',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reservation_seat (seat_id, status),
    CONSTRAINT fk_reservation_seat FOREIGN KEY (seat_id) REFERENCES seat(id),
    CONSTRAINT fk_reservation_concert FOREIGN KEY (concert_id) REFERENCES concert(id),
    INDEX idx_reservation_user (user_id),
    INDEX idx_reservation_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='예약 정보';
