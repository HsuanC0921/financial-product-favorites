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

## 未自動化的部分

Nginx 的 HTTPS 轉址、安全標頭與登入限流沒有自動化測試，目前以 `curl` 手動確認。安全標頭另以 ZAP 被動掃描過，見[弱點掃描](SECURITY.md#弱點掃描)。前端元件與 `api.ts` 的錯誤處理也還沒有測試。
