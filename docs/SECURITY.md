# 安全性

下表列出已處理的威脅與做法。

| 威脅 | 處理方式 |
| --- | --- |
| SQL Injection | 資料庫存取一律是參數化的 `CALL sp_xxx(?, ?)`，沒有拼接 SQL 字串。應用程式用的資料庫帳號只有 EXECUTE 權限，不能直接讀寫資料表。測試裡含 SQL 語法的使用者 ID，會被當成一般文字。 |
| XSS | 前端只用 Vue 的文字插值，整個專案沒有用 `v-html`，使用者輸入的內容不會被當成 HTML 執行。Nginx 另外加了 Content-Security-Policy，只允許載入同源的腳本與樣式，就算有內容被塞進頁面，行內腳本也跑不起來。後端刻意不在收到資料時跳脫 HTML，因為跳脫只對 HTML 有意義，太早做會改掉使用者的資料，例如 `A&B` 會被改成跳脫後的樣子，換到匯出檔之類不是 HTML 的地方，就會原樣顯示出跳脫碼。所以資料照原樣存，顯示時才跳脫。測試有確認含 `<script>` 的名稱與備註會原樣存回。 |
| CSRF | 用「Cookie 加標頭」的做法：伺服器發一個 `XSRF-TOKEN` Cookie，前端把同樣的值放進 `X-XSRF-TOKEN` 標頭，兩者對不上就回 403。 |
| 傳輸竊聽 | 只透過 HTTPS（TLS 1.2 與 1.3）提供服務，HTTP 一律轉到 HTTPS，並送出 HSTS。經由 HTTPS 時，Session 和 `XSRF-TOKEN` Cookie 都會帶 `Secure`。 |
| 暴力猜密碼 | Nginx 限制同一個來源位址每分鐘登入 10 次，另外容許 5 次突發，超過回 429。 |
| 點擊劫持與內容嗅探 | 回應帶 `X-Frame-Options: DENY` 和 CSP 的 `frame-ancestors 'none'`，頁面不能被嵌進別人的框架。`X-Content-Type-Options: nosniff` 不讓瀏覽器自己猜檔案類型。另外還設了 `Referrer-Policy: no-referrer` 與 `Permissions-Policy`。 |
| 繞過 Web Server | 後端只聽 `127.0.0.1`，其他主機只能經過 Nginx，限流和安全標頭就繞不過去。`X-Forwarded-*` 標頭也只有本機的 Nginx 送得進來。 |
| 密碼保存 | 密碼用 BCrypt（強度 12）存雜湊，不存明文。 |
| 使用者 ID 列舉 | 密碼錯誤和使用者 ID 不存在，回應一模一樣。使用者 ID 不存在時，Spring 會用同強度的假雜湊比對，所以兩種情況花的時間也差不多。 |
| Session | Session Cookie 設成 HttpOnly 與 SameSite=Lax，閒置 30 分鐘就過期，登入時換發新的 Session，降低 Session Fixation 的風險。沒登入的請求不會建立 Session。 |
| 越權存取 | 使用者 ID 只從登入狀態取得，不接受請求帶入。請求出現沒定義的欄位（例如夾帶 `userId`）直接回 400。每支 Stored Procedure 都用 `user_id` 過濾，讀取、修改或刪除別人的資料，都和資料不存在一樣回 404。 |
| 輸入驗證 | 在入口用 Bean Validation 檢查型別、長度與範圍，資料庫再用 CHECK 和外鍵把關，兩邊各自獨立。數量帶小數直接回 400，不會被截成整數。 |
| 資料庫最小權限 | Stored Procedure 以 `SQL SECURITY DEFINER` 執行，definer 這個資料庫帳號被鎖定，不能登入。它對 `app_user` 只能讀，對 `user_account` 只能讀和新增，只有 `product` 與 `like_list` 能改和刪。 |
| 金額正確性 | 手續費和總金額由後端用 `BigDecimal` 計算，四捨五入到分，資料庫用 `DECIMAL` 存，不用浮點數。前端的預估用 big.js，並和後端共用同一份測試案例，確保兩邊結果一致。總手續費和總金額不接受前端傳入，請求帶了就回 400。 |
| 並行修改 | 用版本號做樂觀鎖，修改和刪除要帶 `If-Match`，版本過期回 412。同一版本同時修改，只會有一個成功。 |
| 錯誤訊息 | 未預期的錯誤只回通用訊息，堆疊和細節只寫進伺服器記錄。 |
| 相依套件弱點 | 針對已公開的弱點覆寫套件版本，見[相依套件修補](#相依套件修補)。 |
| 示範密碼 | 專案裡的密碼都是公開的本機示範值，資料庫只存 BCrypt 雜湊。要部署到別人連得到的環境時，請用 `ALTER USER` 換掉 `favorites_app` 的密碼，並把 `FAVORITES_DB_PASSWORD` 設成同一個值。 |

## 相依套件修補

後端把 Spring Boot 管理的 `json-smart.version` 覆寫成 2.6.1，修補測試相依套件的 [CVE-2026-113602](https://github.com/netplex/json-smart-v2/pull/305)。等 Spring Boot 管理的版本包含這個修補後，就可以移除這項設定。

前端用 npm `overrides` 把間接依賴 `minimatch` 固定為 11.0.4，修補 [GHSA-g3hv-75wg-m249](https://github.com/advisories/GHSA-g3hv-75wg-m249)。等上游依賴改用修補後的版本，並確認建置與程式碼檢查都通過，就可以移除這項設定。
