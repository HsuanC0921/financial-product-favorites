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
| `FavoriteServiceTests` | 12 | 手續費與總金額的半分進位、費率 0 與 100%、價格與數量的上限，以及清單是否還有下一頁 |
| `amount.spec.ts`（前端） | 28 | 表單的金額預估、輸入檢查、百分比換算與千分位 |
| `FavoriteValidationTests` | 16 | 每個欄位超出範圍都回 400，包含數量帶小數、價格 3 位小數、費率超過 100%、名稱與備註超長、扣款帳號格式 |
| `FavoriteWriteTests` | 5 | 新增、修改、刪除、版本衝突，同版本同時修改只有 1 個成功，各欄位上限與 HTML 都原樣存回，改用別人的扣款帳號時資料不變 |
| `FavoriteTransactionTests` | 1 | 新增時 Like List 寫入失敗，Product 跟著回復 |
| `ApiIntegrationTests` | 9 | 登入失敗不洩漏使用者 ID 是否存在、SQL Injection、未登入、缺少 CSRF Token、夾帶別人的使用者 ID、讀改刪別人的資料、重複登錄扣款帳號、扣款帳號格式 |
| `CookieAndSessionTests` | 3 | Cookie 的 HttpOnly、SameSite 與 HTTPS 下的 `Secure`，登入時換發 Session，登出後舊 Session 失效 |
| `JsonSmartSecurityTests` | 1 | json-smart 弱點修補 |

金額案例只維護一份，放在 `backend/src/test/resources/fee-cases.csv`，前後端的單元測試都讀它。任何一邊的計算規則改了，測試就會失敗，表單的預估金額不會跟實際存進去的對不上。

## 壓力測試

用 k6 對整套 Docker 服務（Nginx、Spring Boot、MySQL）施壓，腳本是 [`deploy/loadtest/favorites.js`](../deploy/loadtest/favorites.js)。5 位示範使用者各登入一次，所有虛擬使用者共用這 5 個 Session，因為 Nginx 限制登入每分鐘 10 次。操作比例是查清單 70%、查使用者資料 10%、新增修改刪除一輪 20%，新增的資料當場刪除，不留紀錄。虛擬使用者在 15 秒內升到 10 人，再升到 50 人並維持 30 秒，最後降回 0，全程約 90 秒。

先用 `docker compose up -d --build` 啟動服務，再執行：

```powershell
docker run --rm --network financial-product-favorites_default -v "${PWD}/deploy/loadtest:/scripts:ro" -e BASE_URL=https://web grafana/k6 run /scripts/favorites.js
```

`--network` 的名稱是 Compose 專案名稱加 `_default`。加上 `-e THINK_TIME=0` 會讓每個虛擬使用者不停頓地連續送請求，用來找出吞吐量的上限，預設是每次操作之間停 0.5 秒。門檻是錯誤率低於 1%、p95 延遲低於 500 ms。

測試環境是一台 Windows 電腦上的 Docker Desktop，三個容器與 k6 都在同一台機器，所以數字只能當展示用的基準，不代表正式環境的容量。

| 停頓 | Nginx 對後端 | 請求數 | 每秒請求數 | p95 延遲 | 錯誤率 | 門檻 |
| --- | --- | --- | --- | --- | --- | --- |
| 0.5 秒 | 持久連線 | 8,040 | 88 | 3.54 ms | 0% | 通過 |
| 0 秒 | 每個請求新開連線 | 63,487 | 698 | 131.81 ms | 20.13% | 未通過 |
| 0 秒 | 持久連線 | 298,875 | 3,285 | 23.26 ms | 0% | 通過 |

第一次不停頓時有 20% 的請求回 502。Nginx 的錯誤記錄是 `connect() to 後端 failed (99: Address not available)`，原因是 Nginx 對後端的每個請求都新開一條 TCP 連線，每秒約 700 個請求時，本機可用的暫時埠被用光。後端本身沒有錯誤記錄，也沒有重啟。

改用 `upstream` 搭配 `keepalive 32` 重複使用連線後，同樣的不停頓測試錯誤率降為 0%，每秒請求數約是原本的 4.7 倍。一般停頓的結果沒有變化。

## 未自動化的部分

Nginx 的 HTTPS 轉址、安全標頭與登入限流沒有自動化測試，目前以 `curl` 手動確認。安全標頭另以 ZAP 被動掃描過，見[弱點掃描](SECURITY.md#弱點掃描)。Docker Compose 的啟動也沒有自動化，以手動啟動後登入、新增、查詢與刪除確認過。前端元件與 `api.ts` 的錯誤處理也還沒有測試。
