-- 選用：分頁展示資料，不屬於標準的 5 支腳本
-- 新增一位有 120 筆喜好的使用者，用來操作「載入更多」（單次最多回 100 筆）
-- 在 seed.sql 之後執行，重建資料庫就會清掉
-- 示範登入密碼（僅限本機）：F6789012345 / DemoUserF#2026

USE favorites;
SET NAMES utf8mb4;
SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

START TRANSACTION;

INSERT INTO app_user (user_id, user_name, email, account, password_hash) VALUES
    ('F6789012345', '周大偉', 'wei.zhou@example.com', '6666555111',
     '$2a$12$wV/6hSx6EHgc5zToyvUeWuWfpikLK5Y7rp.EXgfej8jzXStKfmGQ.');

INSERT INTO user_account (user_id, account, account_name) VALUES
    ('F6789012345', '6666555111', '主要帳戶');

-- 商品編號從 1000 起，避開 seed.sql 用掉的 1 到 19
-- 16 檔商品輪流使用，價格、數量、費率用固定公式變化，每次執行結果都一樣
INSERT INTO product (no, product_name, price, fee_rate)
WITH RECURSIVE n (i) AS (
    SELECT 1
    UNION ALL
    SELECT i + 1 FROM n WHERE i < 120
)
SELECT 1000 + i,
       ELT(i % 16 + 1,
           '心悅台灣成長 ETF', '清和全球科技 ETF', '如意永續股息 ETF', '玉衡投資級債 ETF',
           '義豐多元資產 ETF', '重光亞洲成長 ETF', '如初低波動 ETF', '山嵐短天期債 ETF',
           '玉潔全球ESG ETF', '潔源綠能 ETF', '冰心新興市場債 ETF', '清泉公用事業 ETF',
           '山海基礎建設 ETF', '高峰半導體 ETF', '水岸不動產 ETF', '長青高股息 ETF'),
       ROUND(10 + (i * 37 % 9000) / 100, 2),
       CAST(ELT(i % 5 + 1, '0.001425', '0.0005', '0.002', '0', '0.003') AS DECIMAL(9,8))
FROM n;

-- 金額算法與其他資料相同：手續費四捨五入到分，總金額 = 本金 + 手續費
INSERT INTO like_list (user_id, product_no, purchase_quantity, account,
                       total_fee, total_amount, note, created_at)
SELECT 'F6789012345', p.no, q.qty, '6666555111',
       ROUND(p.price * q.qty * p.fee_rate, 2),
       p.price * q.qty + ROUND(p.price * q.qty * p.fee_rate, 2),
       '',
       TIMESTAMP('2026-09-01 00:00:00') + INTERVAL (p.no - 1000) HOUR
FROM product p
JOIN (SELECT no, 1 + (no - 1000) * 13 % 2000 AS qty FROM product WHERE no >= 1000) q
  ON q.no = p.no
WHERE p.no >= 1000
ORDER BY p.no;

COMMIT;
