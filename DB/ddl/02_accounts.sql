-- 資料庫帳號與授權，需要資料表已存在，且必須在 procedures 資料夾的腳本之前執行
-- 這份腳本可以重複執行

-- 所有 SP 的 definer，鎖定後無法登入
-- 它本身也要有 EXECUTE，SQL SECURITY DEFINER 的 SP 才能執行
CREATE USER IF NOT EXISTS 'favorites_definer'@'localhost' ACCOUNT LOCK;
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'favorites_definer'@'localhost';
GRANT SELECT ON favorites.app_user TO 'favorites_definer'@'localhost';
GRANT SELECT, INSERT ON favorites.user_account TO 'favorites_definer'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON favorites.product TO 'favorites_definer'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON favorites.like_list TO 'favorites_definer'@'localhost';
GRANT EXECUTE ON favorites.* TO 'favorites_definer'@'localhost';

-- 應用程式帳號，只能呼叫 SP
-- 密碼是公開的本機示範值
-- 部署到他人可連線的環境時，請用 ALTER USER 改掉，並把 FAVORITES_DB_PASSWORD 設成相同的值
-- 帳號已存在時保留原密碼
CREATE USER IF NOT EXISTS 'favorites_app'@'localhost' IDENTIFIED BY 'Favorites-App-2026';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'favorites_app'@'localhost';
GRANT EXECUTE ON favorites.* TO 'favorites_app'@'localhost';
