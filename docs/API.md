# API 說明

除登入外，所有 API 都需要先登入。登入以表單格式送出，其餘請求與回應都是 JSON。

所有 POST、PUT、DELETE，包含登入與登出，都要帶 `X-XSRF-TOKEN` 標頭，值取自 `XSRF-TOKEN` Cookie。還沒有這個 Cookie 時，先送一個 `/api/` 底下的 GET，例如 `GET /api/me`，未登入也會拿到。登入成功的回應會清掉這個 Cookie，要再送一次 `/api/` 的 GET 取得新的值，才能繼續送寫入請求。

## 端點

| 方法 | 路徑 | 說明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 以表單格式送出 `userId`、`password`，成功回 204 |
| POST | `/api/auth/logout` | 登出，回 204 |
| GET | `/api/me` | 目前使用者的資料、預設扣款帳號與可用的扣款帳號 |
| GET | `/api/favorites` | 喜好清單，每次最多 100 筆，用 `afterSn` 接續下一頁 |
| GET | `/api/favorites/{sn}` | 單筆喜好，`ETag` 標頭帶版本號 |

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

錯誤一律回 `{"code": "...", "message": "..."}`，`code` 供程式判斷，`message` 是給使用者看的訊息。

| 狀態碼 | `code` | 意思 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | 欄位不合法，或 `If-Match` 格式不對 |
| 401 | `LOGIN_FAILED` | 使用者 ID 或密碼錯誤 |
| 401 | `UNAUTHENTICATED` | 未登入或 Session 已失效 |
| 403 | `FORBIDDEN` | CSRF 驗證失敗 |
| 404 | `NOT_FOUND` | 資料不存在，或屬於別的使用者 |
| 500 | `INTERNAL_ERROR` | 未預期的錯誤，細節只寫進伺服器記錄 |

Spring MVC 內建的其他錯誤，例如 405 不支援的方法、415 的 `Content-Type` 不是 `application/json`，也用相同的格式回應。
