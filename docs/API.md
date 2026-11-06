# API 說明

[專案說明](../README.md) · [架構](ARCHITECTURE.md) · **API** · [安全性](SECURITY.md) · [測試](TESTING.md) · [開發與部署](DEVELOPMENT.md)

使用者資料、扣款帳號與喜好相關 API 都需要先登入。登入以表單格式送出，其餘有本文的請求與回應使用 JSON。204 回應不含本文。

所有 POST、PUT、DELETE，包含登入與登出，都要帶 `X-XSRF-TOKEN` 標頭，值取自 `XSRF-TOKEN` Cookie。還沒有這個 Cookie 時，先送一個 `/api/` 底下的 GET，例如 `GET /api/me`，未登入也會拿到。首頁與靜態檔由 Nginx 直接回應，不會發這個 Cookie。登入成功的回應會清掉這個 Cookie，要再送一次 `/api/` 的 GET 取得新的值，才能繼續送寫入請求。原理見[安全性](SECURITY.md)。

## 端點

| 方法 | 路徑 | 說明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 以表單格式送出 `userId`、`password`，成功回 204 |
| POST | `/api/auth/logout` | 登出，回 204 |
| GET | `/api/me` | 目前使用者的資料、預設扣款帳號與可用的扣款帳號 |
| POST | `/api/me/accounts` | 新增扣款帳號，回 201 |
| GET | `/api/favorites` | 喜好清單，每次最多 100 筆，用 `afterSn` 接續下一頁 |
| GET | `/api/favorites/{sn}` | 單筆喜好，`ETag` 標頭帶版本號 |
| POST | `/api/favorites` | 新增喜好，回 201，`Location` 指向新的資料 |
| PUT | `/api/favorites/{sn}` | 修改喜好，`If-Match` 帶讀取時的 `ETag`，回 200 與新的 `ETag` |
| DELETE | `/api/favorites/{sn}` | 刪除喜好，`If-Match` 帶讀取時的 `ETag`，回 204 |

## 請求內容

新增與修改喜好使用相同的內容。總手續費與總金額由後端計算，不接受前端傳入。

```json
{
  "productName": "心悅台灣成長 ETF",
  "price": "45.60",
  "feeRate": "0.001425",
  "purchaseQuantity": 1000,
  "account": "1111999666",
  "note": "長期持有"
}
```

| 欄位 | 型別 | 規則 |
| --- | --- | --- |
| `productName` | 字串 | 必填，最多 100 字，前後空白會去掉 |
| `price` | 十進位字串 | 必填，大於 0，最多 10 位整數、2 位小數 |
| `feeRate` | 十進位字串 | 必填，0 到 1，最多 8 位小數。`0.001425` 代表 0.1425% |
| `purchaseQuantity` | 整數 | 必填，1 到 1,000,000，帶小數回 400 |
| `account` | 字串 | 必填，1 到 32 位數字，必須是自己的扣款帳號，否則回 422 |
| `note` | 字串 | 選填，最多 200 字 |

出現表格以外的欄位（例如 `userId`）回 400。

新增扣款帳號的內容是 `{"account": "1111999888", "accountName": "薪轉戶"}`：`account` 為 1 到 32 位數字，`accountName` 必填、最多 50 字。帳號已被任何人登錄過，一律回 409。

## 回應範例

單筆喜好，`GET /api/favorites/1`：

```http
HTTP/1.1 200 OK
ETag: "0"
Content-Type: application/json
```

```json
{
  "sn": 1,
  "productName": "心悅台灣成長 ETF",
  "price": "45.60",
  "feeRate": "0.001425",
  "purchaseQuantity": 1000,
  "account": "1111999666",
  "totalFee": "64.98",
  "totalAmount": "45664.98",
  "note": "長期持有",
  "createdAt": "2026-09-01T09:30:00Z",
  "version": 0
}
```

價格、費率與金額都以十進位字串回傳，避免被當成浮點數。`createdAt` 是 UTC。

清單，`GET /api/favorites`：

```jsonc
{
  "user": { "userId": "A1236456789", "userName": "王小明", "email": "ming.wang@example.com" },
  "items": [
    // 每筆的欄位與上面的單筆喜好相同
  ],
  "nextAfterSn": null
}
```

`nextAfterSn` 不是 `null` 時，以 `GET /api/favorites?afterSn={nextAfterSn}` 取下一頁。不用頁碼的原因見[設計決策](ARCHITECTURE.md#為什麼清單用-aftersn-接續不用頁碼)。

## 狀態碼

後端 API 的錯誤回應為 `{"code": "...", "message": "..."}`，`code` 供程式判斷，`message` 是給使用者看的訊息。Nginx 產生的錯誤不一定是 JSON，例如超過 64 KB 的請求會回 413 與 HTML 錯誤頁，前端會顯示通用錯誤訊息。

| 狀態碼 | `code` | 意思 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | 欄位不合法，或 `If-Match` 格式不對 |
| 401 | `LOGIN_FAILED` | 使用者 ID 或密碼錯誤 |
| 401 | `UNAUTHENTICATED` | 未登入或 Session 已失效 |
| 403 | `FORBIDDEN` | CSRF 驗證失敗 |
| 404 | `NOT_FOUND` | 資料不存在，或屬於別的使用者 |
| 409 | `ACCOUNT_TAKEN` | 扣款帳號已被登錄 |
| 412 | `VERSION_CONFLICT` | 資料在讀取後已被修改，`If-Match` 的版本過期 |
| 422 | `UNKNOWN_ACCOUNT` | 扣款帳號不屬於此使用者 |
| 428 | `PRECONDITION_REQUIRED` | 修改或刪除時缺少 `If-Match` |
| 429 | `TOO_MANY_REQUESTS` | 登入嘗試過於頻繁，由 Nginx 回應 |
| 500 | `INTERNAL_ERROR` | 未預期的錯誤，細節只寫進伺服器記錄 |

Spring MVC 內建的其他錯誤，例如 405 不支援的方法、415 的 `Content-Type` 不是 `application/json`，也用相同的格式回應。
