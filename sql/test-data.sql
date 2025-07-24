-- 기존 데이터 삭제 (스크립트를 여러 번 실행할 경우를 대비)
-- 참조 무결성을 위해 자식 테이블부터 삭제합니다.
SET FOREIGN_KEY_CHECKS = 0; -- 외래 키 제약 조건 비활성화
TRUNCATE TABLE expenditure;
TRUNCATE TABLE user_asset;
TRUNCATE TABLE category;
TRUNCATE TABLE user;
TRUNCATE TABLE chat_message;
TRUNCATE TABLE user_challenge;
TRUNCATE TABLE challenge;
SET FOREIGN_KEY_CHECKS = 1; -- 외래 키 제약 조건 다시 활성화

-- 테스트에 필요한 데이터 삽입
-- 사용자 (user_id = 1)
INSERT INTO `user` (name, email, password, point, nickname)
VALUES ('testuser', 'test@example.com', 'password123', 0, '테스트유저');

-- 추가 사용자 데이터
INSERT INTO `user` (name, email, password, point, nickname)
VALUES ('testuser2', 'test2@example.com', 'password123', 0, '테스트유저2');

-- 카테고리 (category_id = 1, 2)
INSERT INTO `category` (category_id, name, icon_url)
VALUES (1, '식비', 'default_icon_url'),(2, '교통비', 'default_icon_url');

-- 자산 (asset_id = 1, user_id = 1)
INSERT INTO `user_asset` (user_id, asset_name, balance, bank_name, created_at, bank_account, bank_id, bank_pw, connected_id) 
VALUES (1, '테스트은행 계좌', 1000000, '테스트은행', NOW(), '110-123-456789', 'test_bank_id', 'test_pw', 'test_conn_id');

-- 챌린지 1개
INSERT INTO challenge (category_id, title, summary, description)
VALUES (1, '매일 1시간 걷기', '건강을 위한 첫 걸음', '매일 1시간씩 걸으며 건강한 습관을 기릅니다.');

-- 챌린지 참여
INSERT INTO user_challenge (user_id, challenge_id, status, period, progress, start_date, end_date, point)
VALUES 
(1, 1, 'ongoing', 30, 5, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 100),
(2, 1, 'ongoing', 30, 2, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 100);