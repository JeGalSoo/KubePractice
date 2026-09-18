-- =============================================
-- V2: 테스트 데이터 삽입
-- 콘서트 3개, 구역 3개씩, 좌석 100개씩
-- =============================================

-- 공연 데이터
INSERT INTO concert (id, title, artist, venue, concert_date, sale_open_at, sale_close_at, status) VALUES
(1, '2025 BLACKPINK WORLD TOUR', 'BLACKPINK', '서울 올림픽 주경기장', '2025-12-20 19:00:00', '2025-10-01 10:00:00', '2025-12-18 23:59:00', 'ON_SALE'),
(2, 'BTS PERMISSION TO DANCE ON STAGE', 'BTS', 'KSPO 돔', '2025-11-15 18:00:00', '2025-09-15 10:00:00', '2025-11-13 23:59:00', 'ON_SALE'),
(3, '2025 아이유 콘서트 HEREH', '아이유', '잠실 실내체육관', '2025-12-05 19:30:00', '2025-10-10 10:00:00', '2025-12-03 23:59:00', 'ON_SALE');

-- Concert 1 구역
INSERT INTO section (id, concert_id, name, price, total_seats, remain_seats) VALUES
(1, 1, 'S석', 165000, 100, 100),
(2, 1, 'A석', 132000, 150, 150),
(3, 1, 'B석', 99000, 200, 200);

-- Concert 2 구역
INSERT INTO section (id, concert_id, name, price, total_seats, remain_seats) VALUES
(4, 2, 'S석', 154000, 100, 100),
(5, 2, 'A석', 121000, 150, 150),
(6, 2, 'B석', 88000, 200, 200);

-- Concert 3 구역
INSERT INTO section (id, concert_id, name, price, total_seats, remain_seats) VALUES
(7, 3, 'S석', 143000, 100, 100),
(8, 3, 'A석', 110000, 150, 150),
(9, 3, 'B석', 77000, 200, 200);

-- Concert 1 S석 좌석 (10x10 = 100개)
INSERT INTO seat (section_id, row_num, col_num, seat_label, status)
SELECT 1, r.n, c.n, CONCAT(CHAR(64+r.n), '-', c.n), 'AVAILABLE'
FROM
    (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) r,
    (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) c
ORDER BY r.n, c.n;

-- Concert 2 S석 좌석 (10x10 = 100개)
INSERT INTO seat (section_id, row_num, col_num, seat_label, status)
SELECT 4, r.n, c.n, CONCAT(CHAR(64+r.n), '-', c.n), 'AVAILABLE'
FROM
    (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) r,
    (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) c
ORDER BY r.n, c.n;

-- Concert 3 S석 좌석 (10x10 = 100개)
INSERT INTO seat (section_id, row_num, col_num, seat_label, status)
SELECT 7, r.n, c.n, CONCAT(CHAR(64+r.n), '-', c.n), 'AVAILABLE'
FROM
    (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) r,
    (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) c
ORDER BY r.n, c.n;
