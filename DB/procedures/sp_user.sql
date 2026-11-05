-- 使用者與扣款帳號相關的 Stored Procedure
-- 參數只有 IN，結果用 result set 回傳，直接用參數化 CALL 呼叫即可
-- SP 不 COMMIT，交易由呼叫端 COMMIT 或 ROLLBACK
-- 新增帳號時若帳號已被登記，MySQL 回報錯誤碼 1062，由呼叫端轉成 409
-- 執行順序見 DB\README.md，必須在 ddl\02_accounts.sql 之後

USE favorites;
-- SP 會沿用建立時的 sql_mode 與字元集，這裡固定下來，不受伺服器設定影響
SET NAMES utf8mb4;
SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

DROP PROCEDURE IF EXISTS sp_user_find_for_login;
DROP PROCEDURE IF EXISTS sp_user_profile_get;
DROP PROCEDURE IF EXISTS sp_user_accounts_list;
DROP PROCEDURE IF EXISTS sp_user_account_add;

DELIMITER //

-- 登入驗證：只回傳比對密碼需要的欄位
CREATE DEFINER = 'favorites_definer'@'localhost'
PROCEDURE sp_user_find_for_login(IN p_user_id VARCHAR(64))
    READS SQL DATA
    SQL SECURITY DEFINER
BEGIN
    SELECT user_id, password_hash
    FROM app_user
    WHERE user_id = p_user_id;
END //

-- 個人資料與預設扣款帳號，不含密碼雜湊
CREATE DEFINER = 'favorites_definer'@'localhost'
PROCEDURE sp_user_profile_get(IN p_user_id VARCHAR(64))
    READS SQL DATA
    SQL SECURITY DEFINER
BEGIN
    SELECT user_id, user_name, email, account
    FROM app_user
    WHERE user_id = p_user_id;
END //

-- 使用者可選用的扣款帳號
CREATE DEFINER = 'favorites_definer'@'localhost'
PROCEDURE sp_user_accounts_list(IN p_user_id VARCHAR(64))
    READS SQL DATA
    SQL SECURITY DEFINER
BEGIN
    SELECT account, account_name
    FROM user_account
    WHERE user_id = p_user_id
    ORDER BY account;
END //

-- 新增扣款帳號，並回傳剛新增的那一筆
CREATE DEFINER = 'favorites_definer'@'localhost'
PROCEDURE sp_user_account_add(
    IN p_user_id      VARCHAR(64),
    IN p_account      VARCHAR(32),
    IN p_account_name VARCHAR(50))
    MODIFIES SQL DATA
    SQL SECURITY DEFINER
BEGIN
    INSERT INTO user_account (user_id, account, account_name)
    VALUES (p_user_id, p_account, p_account_name);

    SELECT account, account_name
    FROM user_account
    WHERE user_id = p_user_id
      AND account = p_account;
END //

DELIMITER ;
