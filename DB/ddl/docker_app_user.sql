-- 只在 Docker Compose 使用：後端在另一個容器，連線來源不是 localhost
-- 權限與 DB/ddl/02_accounts.sql 的 favorites_app 相同，只有 EXECUTE
-- 資料庫的埠不對主機開放，只有同一個 Compose 網路內的容器連得到
CREATE USER IF NOT EXISTS 'favorites_app'@'%' IDENTIFIED BY 'Favorites-App-2026';
GRANT EXECUTE ON favorites.* TO 'favorites_app'@'%';
