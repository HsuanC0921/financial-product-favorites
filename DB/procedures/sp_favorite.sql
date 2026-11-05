-- 金融商品喜好清單的 Stored Procedure
-- 參數只有 IN，結果用 result set 回傳，直接用參數化 CALL 呼叫即可
-- SP 不 COMMIT，交易由呼叫端 COMMIT 或 ROLLBACK
-- 業務錯誤以 SQLSTATE 45000 拋出：
--   FAVORITE_NOT_FOUND         此使用者沒有這筆資料
--   FAVORITE_VERSION_CONFLICT  資料存在但版本已變更
-- 執行順序見 DB\README.md，必須在 ddl\02_accounts.sql 之後

USE favorites;
-- SP 會沿用建立時的 sql_mode 與字元集，這裡固定下來，不受伺服器設定影響
SET NAMES utf8mb4;
SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

DROP PROCEDURE IF EXISTS sp_favorite_list;
DROP PROCEDURE IF EXISTS sp_favorite_get;
DROP PROCEDURE IF EXISTS sp_favorite_create;
DROP PROCEDURE IF EXISTS sp_favorite_update;
DROP PROCEDURE IF EXISTS sp_favorite_delete;

DELIMITER //

-- Keyset 分頁：第一頁傳 0，之後傳上一頁最後一筆的 sn
CREATE DEFINER = 'favorites_definer'@'localhost'
PROCEDURE sp_favorite_list(IN p_user_id VARCHAR(64), IN p_after_sn BIGINT)
    READS SQL DATA
    SQL SECURITY DEFINER
BEGIN
    SELECT l.sn, p.product_name, p.price, p.fee_rate, l.purchase_quantity,
           l.account, l.total_fee, l.total_amount, l.note, l.created_at, l.version
    FROM like_list l
    JOIN product p ON p.no = l.product_no
    WHERE l.user_id = p_user_id
      AND l.sn > p_after_sn
    ORDER BY l.sn
    LIMIT 100;
END //

-- 讀取單筆，編輯前用來取得最新內容與版本
CREATE DEFINER = 'favorites_definer'@'localhost'
PROCEDURE sp_favorite_get(IN p_user_id VARCHAR(64), IN p_sn BIGINT)
    READS SQL DATA
    SQL SECURITY DEFINER
BEGIN
    SELECT l.sn, p.product_name, p.price, p.fee_rate, l.purchase_quantity,
           l.account, l.total_fee, l.total_amount, l.note, l.created_at, l.version
    FROM like_list l
    JOIN product p ON p.no = l.product_no
    WHERE l.user_id = p_user_id
      AND l.sn = p_sn;
END //

-- 新增喜好，同時新增它專屬的 Product，兩筆寫入由呼叫端的交易一起提交
-- 第一個 INSERT 產生的 product.no 馬上接給 like_list 使用
-- 第二個 INSERT 之後，LAST_INSERT_ID() 變成 like_list.sn，所以最後回傳的是 sn
CREATE DEFINER = 'favorites_definer'@'localhost'
PROCEDURE sp_favorite_create(
    IN p_user_id           VARCHAR(64),
    IN p_product_name      VARCHAR(100),
    IN p_price             DECIMAL(12,2),
    IN p_fee_rate          DECIMAL(9,8),
    IN p_purchase_quantity INT,
    IN p_account           VARCHAR(32),
    IN p_total_fee         DECIMAL(20,2),
    IN p_total_amount      DECIMAL(20,2),
    IN p_note              VARCHAR(200))
    MODIFIES SQL DATA
    SQL SECURITY DEFINER
BEGIN
    INSERT INTO product (product_name, price, fee_rate)
    VALUES (p_product_name, p_price, p_fee_rate);

    INSERT INTO like_list (user_id, product_no, purchase_quantity, account,
                           total_fee, total_amount, note)
    VALUES (p_user_id, LAST_INSERT_ID(), p_purchase_quantity, p_account,
            p_total_fee, p_total_amount, p_note);

    SELECT LAST_INSERT_ID() AS sn, 0 AS version;
END //

-- 修改喜好與它專屬的 Product，以 p_expected_version 做樂觀鎖
CREATE DEFINER = 'favorites_definer'@'localhost'
PROCEDURE sp_favorite_update(
    IN p_user_id           VARCHAR(64),
    IN p_sn                BIGINT,
    IN p_expected_version  BIGINT,
    IN p_product_name      VARCHAR(100),
    IN p_price             DECIMAL(12,2),
    IN p_fee_rate          DECIMAL(9,8),
    IN p_purchase_quantity INT,
    IN p_account           VARCHAR(32),
    IN p_total_fee         DECIMAL(20,2),
    IN p_total_amount      DECIMAL(20,2),
    IN p_note              VARCHAR(200))
    MODIFIES SQL DATA
    SQL SECURITY DEFINER
BEGIN
    -- 條件式 UPDATE 會鎖住該列
    -- 帶同版本的並行呼叫會等前一筆 commit，之後條件不再成立，回傳版本衝突
    UPDATE like_list
    SET purchase_quantity = p_purchase_quantity,
        account           = p_account,
        total_fee         = p_total_fee,
        total_amount      = p_total_amount,
        note              = p_note,
        version           = version + 1
    WHERE sn = p_sn
      AND user_id = p_user_id
      AND version = p_expected_version;

    IF ROW_COUNT() = 0 THEN
        IF EXISTS (SELECT 1 FROM like_list WHERE sn = p_sn AND user_id = p_user_id) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'FAVORITE_VERSION_CONFLICT';
        END IF;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'FAVORITE_NOT_FOUND';
    END IF;

    UPDATE product p
    JOIN like_list l ON l.product_no = p.no
    SET p.product_name = p_product_name,
        p.price        = p_price,
        p.fee_rate     = p_fee_rate
    WHERE l.sn = p_sn
      AND l.user_id = p_user_id;

    SELECT p_expected_version + 1 AS version;
END //

-- 刪除喜好，連同它專屬的 Product 一起刪
-- like_list 有外鍵指向 product，所以要先刪 like_list，刪之前先記下 product_no
CREATE DEFINER = 'favorites_definer'@'localhost'
PROCEDURE sp_favorite_delete(
    IN p_user_id          VARCHAR(64),
    IN p_sn               BIGINT,
    IN p_expected_version BIGINT)
    MODIFIES SQL DATA
    SQL SECURITY DEFINER
BEGIN
    DECLARE v_product_no BIGINT;

    SET v_product_no = (SELECT product_no FROM like_list
                        WHERE sn = p_sn AND user_id = p_user_id);

    DELETE FROM like_list
    WHERE sn = p_sn
      AND user_id = p_user_id
      AND version = p_expected_version;

    IF ROW_COUNT() = 0 THEN
        IF EXISTS (SELECT 1 FROM like_list WHERE sn = p_sn AND user_id = p_user_id) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'FAVORITE_VERSION_CONFLICT';
        END IF;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'FAVORITE_NOT_FOUND';
    END IF;

    DELETE FROM product WHERE no = v_product_no;
END //

DELIMITER ;
