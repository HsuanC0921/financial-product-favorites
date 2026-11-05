# 安全性

下表列出已處理的威脅與做法。

| 威脅 | 處理方式 |
| --- | --- |
| SQL Injection | 資料庫存取一律是參數化的 `CALL sp_xxx(?, ?)`，沒有拼接 SQL 字串。應用程式用的資料庫帳號只有 EXECUTE 權限，不能直接讀寫資料表。測試裡含 SQL 語法的使用者 ID，會被當成一般文字。 |
| CSRF | 用「Cookie 加標頭」的做法：伺服器發一個 `XSRF-TOKEN` Cookie，前端把同樣的值放進 `X-XSRF-TOKEN` 標頭，兩者對不上就回 403。 |
| 密碼保存 | 密碼用 BCrypt（強度 12）存雜湊，不存明文。 |
| 使用者 ID 列舉 | 密碼錯誤和使用者 ID 不存在，回應一模一樣。使用者 ID 不存在時，Spring 會用同強度的假雜湊比對，所以兩種情況花的時間也差不多。 |
| Session | Session Cookie 設成 HttpOnly 與 SameSite=Lax，閒置 30 分鐘就過期，登入時換發新的 Session，降低 Session Fixation 的風險。沒登入的請求不會建立 Session。 |
| 越權存取 | 使用者 ID 只從登入狀態取得，不接受請求帶入。請求出現沒定義的欄位（例如夾帶 `userId`）直接回 400。每支 Stored Procedure 都用 `user_id` 過濾，讀取、修改或刪除別人的資料，都和資料不存在一樣回 404。 |
| 輸入驗證 | 在入口用 Bean Validation 檢查型別、長度與範圍，資料庫再用 CHECK 和外鍵把關，兩邊各自獨立。數量帶小數直接回 400，不會被截成整數。 |
| 資料庫最小權限 | Stored Procedure 以 `SQL SECURITY DEFINER` 執行，definer 這個資料庫帳號被鎖定，不能登入。它對 `app_user` 只能讀，對 `user_account` 只能讀和新增，只有 `product` 與 `like_list` 能改和刪。 |
| 金額正確性 | 手續費和總金額由後端用 `BigDecimal` 計算，四捨五入到分，資料庫用 `DECIMAL` 存，不用浮點數。總手續費和總金額不接受前端傳入，請求帶了就回 400。 |
| 並行修改 | 用版本號做樂觀鎖，修改和刪除要帶 `If-Match`，版本過期回 412。同一版本同時修改，只會有一個成功。 |
| 錯誤訊息 | 未預期的錯誤只回通用訊息，堆疊和細節只寫進伺服器記錄。 |
| 相依套件弱點 | 針對已公開的弱點覆寫套件版本，見[相依套件修補](#相依套件修補)。 |
| 示範密碼 | 專案裡的密碼都是公開的本機示範值，資料庫只存 BCrypt 雜湊。要部署到別人連得到的環境時，請用 `ALTER USER` 換掉 `favorites_app` 的密碼，並把 `FAVORITES_DB_PASSWORD` 設成同一個值。 |

## 相依套件修補

後端把 Spring Boot 管理的 `json-smart.version` 覆寫成 2.6.1，修補測試相依套件的 [CVE-2026-113602](https://github.com/netplex/json-smart-v2/pull/305)。等 Spring Boot 管理的版本包含這個修補後，就可以移除這項設定。
