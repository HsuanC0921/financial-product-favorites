# 測試

[專案說明](../README.md) · [架構](ARCHITECTURE.md) · [API](API.md) · [安全性](SECURITY.md) · **測試** · [開發模式](DEVELOPMENT.md)

後端的整合測試連真實的 MySQL。Stored Procedure 沒辦法用記憶體資料庫模擬，所以執行前要先照 [DB/README.md](../DB/README.md) 建好資料庫與示範資料。寫入測試只用 seed 裡沒有喜好紀錄的使用者 C，建立的紀錄由測試自己刪掉。

## 執行

```powershell
cd backend
.\mvnw.cmd verify
npm ci
npm run format:check

cd ..\frontend
npm ci
npm run test:unit -- --run
npm run build
npm exec -- prettier --check src/
npm exec -- eslint .
npm exec -- oxlint .
npm exec -- stylelint "src/**/*.{css,vue}"
```

前端的 `npm run build` 包含 TypeScript 型別檢查與正式環境建置。後端的 Java 用 Prettier 排版，`npm run format:check` 檢查，`npm run format` 自動整理。

## 範圍

| 測試 | 數量 | 涵蓋 |
| --- | --- | --- |
| [FavoriteServiceTests](../backend/src/test/java/io/github/hsuanc0921/favorites/service/FavoriteServiceTests.java) | 12 | 手續費與總金額的半分進位、費率 0 與 100%、價格與數量的上限，以及清單是否還有下一頁 |
| [amount.spec.ts](../frontend/src/__tests__/amount.spec.ts)（前端） | 28 | 表單的金額預估、輸入檢查、百分比換算與千分位 |
| [FavoriteValidationTests](../backend/src/test/java/io/github/hsuanc0921/favorites/FavoriteValidationTests.java) | 16 | 每個欄位超出範圍都回 400，包含數量帶小數、價格 3 位小數、費率超過 100%、名稱與備註超長、扣款帳號格式 |
| [FavoriteWriteTests](../backend/src/test/java/io/github/hsuanc0921/favorites/FavoriteWriteTests.java) | 5 | 新增、修改、刪除、版本衝突，同版本同時修改只有 1 個成功，各欄位上限與 HTML 都原樣存回，改用別人的扣款帳號時資料不變 |
| [FavoriteTransactionTests](../backend/src/test/java/io/github/hsuanc0921/favorites/FavoriteTransactionTests.java) | 1 | 新增時 Like List 寫入失敗，確認 Spring 交易觸發回滾 |
| [ApiIntegrationTests](../backend/src/test/java/io/github/hsuanc0921/favorites/ApiIntegrationTests.java) | 9 | 登入失敗不洩漏使用者 ID 是否存在、SQL Injection、未登入、缺少 CSRF Token、夾帶別人的使用者 ID、讀改刪別人的資料、重複登錄扣款帳號、扣款帳號格式 |
| [CookieAndSessionTests](../backend/src/test/java/io/github/hsuanc0921/favorites/CookieAndSessionTests.java) | 3 | Cookie 的 HttpOnly、SameSite 與 HTTPS 下的 `Secure`，登入時換發 Session，登出後舊 Session 失效 |
| [JsonSmartSecurityTests](../backend/src/test/java/io/github/hsuanc0921/favorites/JsonSmartSecurityTests.java) | 1 | json-smart 弱點修補 |

數量以 JUnit 與 Vitest 實際執行的案例計算，後端共 47 個，前端共 28 個。前端目前測的是金額與輸入檢查函式，不包含元件操作。

同一個 `@Test` 或 `it` 裡即使有多個斷言，仍只算一項。參數化測試會用不同資料分別執行，每組各算一項。例如：

| 檔案 | 計算方式 |
| --- | --- |
| `FavoriteServiceTests` | 9 組 CSV 金額案例 + 3 個分頁測試 = 12 項 |
| `FavoriteValidationTests` | 同一個參數化方法分別驗證 16 組非法輸入 = 16 項 |
| `amount.spec.ts` | 1 項案例載入檢查 + 9 組金額案例 + 5 組非法預估值 + 3 個輸入檢查 + 5 組百分比換算 + 5 組千分位格式 = 28 項 |

例如前端價格檢查的一個 `it` 包含 6 次 `expect`，仍只算一項。因此，一個檔案可以有十多項測試，但測試數量不等於功能數量或程式碼覆蓋率。

### 金額與輸入邊界

前後端共用 [`fee-cases.csv`](../backend/src/test/resources/fee-cases.csv) 的 9 組金額案例。後端驗證 `BigDecimal` 計算，前端驗證 big.js 預估，兩邊都比對完整結果與兩位小數。

| 情境 | 預期結果 |
| --- | --- |
| 單價 45.60、數量 1,000、費率 0.1425% | 手續費 64.98，含手續費的總金額 45,664.98 |
| 手續費剛好 0.005，或略低於半分的 0.00499999 | 分別進位成 0.01、捨去成 0.00 |
| 費率 0%、100%，以及最低價格 0.01 | 依規則計算，結果保留兩位小數 |
| 單價 9,999,999,999.99、數量 1,000,000、費率 100% | 總金額完整保存為 19,999,999,999,980,000.00，不因浮點數而失真 |
| 價格 0、超額或超過兩位小數，數量帶小數，費率超界，名稱與備註過長，帳號格式錯誤 | 後端回 400，前端對已涵蓋的非法數值不顯示預估 |
| 百分比與顯示格式 | `0.1425%` 轉為 API 的 `0.001425`，千分位不改變小數或大額數值 |

### 寫入交易與安全性

下列情境由後端測試驗證，其中資料庫整合測試會呼叫真實的 Stored Procedure。

| 情境 | 預期結果 |
| --- | --- |
| 新增後查詢、修改再刪除 | 欄位與金額正確，修改後版本加一，刪除後查詢回 404 |
| 使用舊版本修改或刪除，同版本同時修改 | 舊版本回 412，同時修改只有一個成功，另一個回 412。缺少 `If-Match` 回 428，格式錯誤回 400 |
| 使用別人的扣款帳號寫入 | 回 422，既有紀錄的名稱、帳號與版本不變。新增失敗時，交易測試確認 Spring 觸發回滾 |
| 未登入、缺少 CSRF Token、夾帶 `userId`，或讀改刪別人的紀錄 | 分別回 401、403、400、404，不能藉由請求指定其他使用者 |
| 帳號不存在與密碼錯誤，或查詢不存在與他人的紀錄 | 對應情境的回應內容相同，避免由回應判斷資料是否存在 |
| 名稱與備註含 HTML，登入欄位含 SQL 語法 | HTML 原樣儲存與回傳，交由前端當純文字顯示。SQL 語法不能繞過登入，正常帳號仍能登入 |
| 重複新增扣款帳號 | 回 409，自己的帳號與他人的帳號使用相同回應 |
| 登入、再次登入與登出 | Session 帶 HttpOnly、SameSite=Lax，再次登入換發 Session，舊 Session 與登出後的 Session 均失效。模擬 HTTPS 轉送時，Cookie 帶 Secure |
| json-smart 嚴格解析 | 合法 JSON 通過，後方插入 `0x1A` 再接其他內容時必須拒絕 |

HTML 原樣回傳的測試只驗證資料保存，不等於已在瀏覽器驗證 XSS。Cookie 測試使用實際 HTTP 連線，但 HTTPS 情境是模擬代理標頭，不會經過 Nginx。

## 瀏覽器流程驗證

以下已在本機經 `https://localhost` 操作確認，目前尚未納入專案的自動化回歸測試。

| 情境 | 確認結果 |
| --- | --- |
| 儲存與刪除請求尚未完成 | 取消按鈕停用，Esc 不關閉對話框，請求成功後才返回清單 |
| 帳號新增成功，但重新查詢清單失敗 | 顯示已建立但尚未更新的訊息，重試只送 GET，不重複新增帳號，成功後選取新帳號 |
| 編輯時另一個請求先完成修改 | 收到 412，重新取得的摘要包含最新帳號與備註，使用者輸入仍保留，確認後可再次儲存 |
| 價格超過上限 | 顯示實際上限，沒有送出新增請求 |
| 桌面與窄螢幕 | 清單在 320～1280px、表單在 320、390、414、768px 寬度下沒有頁面橫向溢出，金額不以省略號截斷 |

## 壓力測試

用 k6 對整套 Docker 服務（Nginx、Spring Boot、MySQL）施壓，腳本是 [`deploy/loadtest/favorites.js`](../deploy/loadtest/favorites.js)。5 位示範使用者各登入一次，所有虛擬使用者共用這 5 個 Session，因為 Nginx 限制登入每分鐘 10 次。每輪隨機選擇查清單 70%、查使用者資料 10%、新增修改刪除一輪 20%。這是操作輪次的比例，寫入一輪會送出 3 次請求。成功完成的寫入資料當場刪除。虛擬使用者在 15 秒內升到 10 人，接著用 30 秒升到 50 人，維持 30 秒，再用 15 秒降回 0，全程約 90 秒。

先用 `docker compose up -d --build` 啟動服務，再執行：

```powershell
docker run --rm --network financial-product-favorites_default -v "${PWD}/deploy/loadtest:/scripts:ro" -e BASE_URL=https://web grafana/k6 run /scripts/favorites.js
```

`--network` 的名稱是 Compose 專案名稱加 `_default`。加上 `-e THINK_TIME=0` 會讓每個虛擬使用者不停頓地連續送請求，觀察持續施壓下的吞吐量，預設是每次操作之間停 0.5 秒。

腳本檢查清單與使用者資料回 200、新增回 201、修改回 200、刪除回 204。通過門檻為請求錯誤率低於 1%、p95 延遲低於 500 ms、狀態碼檢查成功率高於 99%。取得 CSRF Cookie 時預期的 401 不計為請求失敗。

測試環境是一台 Windows 電腦上的 Docker Desktop，三個容器與 k6 都在同一台機器，所以數字只能當展示用的基準，不代表正式環境的容量。

| 停頓 | Nginx 對後端 | 請求數 | 每秒請求數 | p95 延遲 | 錯誤率 | 門檻 |
| --- | --- | --- | --- | --- | --- | --- |
| 0.5 秒 | 持久連線 | 8,040 | 88 | 3.54 ms | 0% | 通過 |
| 0 秒 | 每個請求新開連線 | 63,487 | 698 | 131.81 ms | 20.13% | 未通過 |
| 0 秒 | 持久連線 | 298,875 | 3,285 | 23.26 ms | 0% | 通過 |
| 0 秒（2026-11-06 複驗） | 持久連線 | 330,969 | 3,617 | 21.98 ms | 0% | 通過 |

第一次不停頓時有 20% 的請求回 502。Nginx 的錯誤記錄是 `connect() to 後端 failed (99: Address not available)`，原因是 Nginx 對後端的每個請求都新開一條 TCP 連線，每秒約 700 個請求時，本機可用的暫時埠被用光。後端本身沒有錯誤記錄，也沒有重啟。

改用 `upstream` 搭配 `keepalive 32` 重複使用連線後，同樣的不停頓測試錯誤率降為 0%，每秒請求數約是原本的 4.7 倍。一般停頓的結果沒有變化。

2026-11-06 複驗時，330,954 次狀態碼檢查全數成功，三個服務均未重啟。壓測結束後以管理者唯讀查詢確認，Product 與 Like List 各保留原本的 19 筆資料，壓測商品、孤兒商品與金額不一致的紀錄均為 0。這項資料庫查核另行執行，不是 k6 腳本的斷言。

另以使用者 C 新增喜好，刻意帶入使用者 A 的扣款帳號，讓 Product 寫入後的 Like List 外鍵檢查失敗。API 回 422，前後兩張表的筆數不變且沒有孤兒商品，直接確認了資料庫回滾結果。這項查核目前也未納入 Maven 測試。

## 未自動化的部分

Nginx 的 HTTPS 轉址、安全標頭與登入限流目前以 `curl` 手動確認，安全標頭另以 ZAP 被動掃描過，見[弱點掃描](SECURITY.md#弱點掃描)。Docker Compose 啟動、前端元件與 `api.ts` 錯誤處理尚無納入專案指令的自動化回歸測試，瀏覽器與資料庫查核的範圍如上。

目前沒有長時間穩定性測試，也沒有大量獨立 Session 的壓測。90 秒混合讀寫通過，只能證明這組測試情境達標，不能推算正式環境可承受的使用者人數。
