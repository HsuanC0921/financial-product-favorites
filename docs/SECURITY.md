# 安全性

[專案說明](../README.md) · [架構](ARCHITECTURE.md) · [API](API.md) · **安全性** · [測試](TESTING.md) · [開發與部署](DEVELOPMENT.md)

下表列出已處理的威脅和做法。本機部署才有的限制，見[已知限制](../README.md#已知限制)。

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
| 相依套件弱點 | 已公開的弱點，用覆寫套件版本的方式處理，見[相依套件修補](#相依套件修補)。全部套件的掃描結果見[弱點掃描](#弱點掃描)。 |
| 示範密碼 | 專案裡的密碼都是公開的本機示範值，資料庫只存 BCrypt 雜湊。要部署到別人連得到的環境時，請用 `ALTER USER` 換掉 `favorites_app` 的密碼，並把 `FAVORITES_DB_PASSWORD` 設成同一個值。 |

## 弱點掃描

2026-11-06 執行相依套件、程式碼、容器映像與網站掃描，結果如下。

| 掃描 | 工具 | 範圍 | 結果 |
| --- | --- | --- | --- |
| 相依套件 | OSV-Scanner 2.7.0 | 後端 97 個套件，其中 60 個屬於 compile/runtime 相依套件、37 個只用於測試。前端 414 個套件，其中 24 個是 `vue` 與它的依賴，1 個是金額計算用的 `big.js`，其餘是建置與檢查工具。後端的 Java 排版工具另有 3 個 npm 套件 | 沒有已知弱點，前端另以 `npm audit` 確認 |
| 執行中的網站 | OWASP ZAP 2.17.0，只做爬蟲與被動掃描 | 經 Nginx 的 `https://localhost` 與 `http://localhost`，未登入 | 沒有高、中風險，1 項低風險、4 項資訊提示 |
| Java 程式碼 | SpotBugs 4.10.5 與 FindSecBugs 1.14.0 | 編譯後的後端程式 | 20 項提醒已逐項核對，沒有可重現的弱點 |
| 容器映像 | Trivy 0.76.2 | 建置後的後端、Nginx 與 MySQL 映像 | 修補後 MySQL 從 99 項降至 0 項，後端從 59 項降至 49 項，Nginx 為 0 項。後端剩餘項目皆為中、低風險，發行版尚未提供修補版本 |

Trivy 的項目以套件與弱點編號的組合計算，同一個弱點影響多個套件會重複列出。

後端的版本由 Spring Boot 統一管理，OSV-Scanner 直接讀 `pom.xml` 只看得到 8 個沒有版本號的套件，所以改用 `mvnw dependency:list` 列出實際用到的版本再掃。

未登入掃描的低風險項目是 `XSRF-TOKEN` 沒有 HttpOnly。前端必須讀出這個值放進標頭，CSRF 防護才能運作，所以是刻意這樣設。Session Cookie 有設 HttpOnly。資訊提示中，「疑似洩漏資訊的註解」是壓縮後的 JS 裡出現 `select`、favicon 的授權註解裡出現 `from`，並不是 SQL。其餘 3 項只是說明：認出這是單頁應用、找到和 Session 有關的回應，以及提醒檢查 Cache-Control，首頁回 `no-cache` 是為了每次都拿到最新版本。

另以獨立資料庫完成登入後的 ZAP 主動掃描，共記錄 4,667 筆 HTTP 訊息，沒有高、中風險警告。JSON 原樣回傳 HTML 被列為低風險，已在實際瀏覽器確認名稱與備註以純文字顯示。掃描紀錄中的畸形查詢 `/api/favorites?=` 原本回 500，已修正為 400，並在既有真實 HTTP 測試補上斷言。登入後的 Cookie 屬性仍由 `CookieAndSessionTests` 檢查。

映像修補包含系統套件更新，以及移除未使用的 MySQL Shell 與 gosu。三個服務均以非 root 帳號執行。自簽 TLS 私鑰由各容器首次啟動時產生，不放進映像檔或版本控制。系統更新有建置快取，需要重新取得修補時使用 `docker compose build --pull --no-cache` 再啟動。

## 相依套件修補

後端把 Spring Boot 管理的 `json-smart.version` 覆寫成 2.6.1，修補測試相依套件的 [CVE-2026-113602](https://github.com/netplex/json-smart-v2/pull/305)。等 Spring Boot 管理的版本包含這個修補後，就可以移除這項設定。

前端用 npm `overrides` 把間接依賴 `minimatch` 固定為 11.0.4，修補 [GHSA-g3hv-75wg-m249](https://github.com/advisories/GHSA-g3hv-75wg-m249)。等上游依賴改用修補後的版本，並確認建置與程式碼檢查都通過，就可以移除這項設定。
