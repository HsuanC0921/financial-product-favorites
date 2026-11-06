# 系統架構與設計決策

[專案說明](../README.md) · **架構** · [API](API.md) · [安全性](SECURITY.md) · [測試](TESTING.md) · [開發模式](DEVELOPMENT.md)

## 系統架構

```text
瀏覽器 ──► Web Server（Nginx） ──► Application Server（Spring Boot） ──► MySQL
           靜態檔案、TLS、反向代理    REST API                           資料表與 Stored Procedure
```

瀏覽器只會連到 Nginx。Nginx 負責提供 `frontend/dist` 裡的靜態檔，把 `/api/` 開頭的請求轉給 Spring Boot，並把 HTTP 一律轉到 HTTPS，設定在 [`deploy/nginx.conf`](../deploy/nginx.conf)。Spring Boot 只聽 `127.0.0.1:8080`，從 Nginx 帶來的 `X-Forwarded-Proto` 判斷原本是不是 HTTPS。Nginx 加的安全標頭和登入次數限制，寫在[安全性](SECURITY.md)。

後端分成 4 層，每層一個套件，都在 `io.github.hsuanc0921.favorites` 底下：

| 層 | 套件 | 責任 |
| --- | --- | --- |
| Presentation | `presentation` | 接收請求、檢查輸入、決定回應格式，並把錯誤換成 HTTP 狀態碼 |
| Business | `service` | 計算手續費與總金額，決定哪些操作要放在同一個交易（`@Transactional`） |
| Data | `repository` | 用 `CALL` 呼叫 Stored Procedure，並把資料庫的錯誤換成業務例外 |
| Shared | `common` | 安全設定、例外與共用的資料類別 |

## 資料模型

```mermaid
erDiagram
    app_user ||--o{ user_account : "擁有"
    user_account ||--o{ like_list : "扣款"
    product ||--o| like_list : "對應"

    app_user {
        varchar user_id PK
        varchar user_name
        varchar email
        varchar account "預設扣款帳號"
        varchar password_hash
    }
    user_account {
        varchar user_id PK
        varchar account PK "全系統唯一"
        varchar account_name
    }
    product {
        bigint no PK
        varchar product_name
        decimal price
        decimal fee_rate
    }
    like_list {
        bigint sn PK
        varchar user_id
        bigint product_no
        int purchase_quantity
        varchar account
        decimal total_fee
        decimal total_amount
        varchar note
        datetime created_at "UTC"
        bigint version "樂觀鎖"
    }
```

- 一個扣款帳號只屬於一位使用者，喜好用到的扣款帳號必須是自己的。
- 每筆喜好各有一筆自己的 Product，新增、修改、刪除時兩邊一起動。
- 價格要大於 0、手續費率在 0 到 1 之間、購買數量在 1 到 1,000,000 之間。API 用 Bean Validation 檢查，資料庫用 CHECK 再擋一次，兩邊各自獨立。
- 手續費率存成比例，`0.1` 就是 10%，`0.01` 就是 1%，和題目的例子一致，畫面上則用百分比輸入與顯示。
- 金額一律是新台幣，用 `DECIMAL` 存到小數 2 位，不用浮點數。後端用 `BigDecimal` 計算，前端的預估用 big.js，兩邊規則相同，都是四捨五入到分。
- 時間以 UTC 存放，由前端轉成本地時間。

資料表與 Stored Procedure 的建立腳本見 [DB/README.md](../DB/README.md)，API 各欄位的規則見[請求內容](API.md#請求內容)。

## 設計決策

### 為什麼用 Stored Procedure 加 `JdbcClient`，不用 JPA？

題目要求資料存取要走 Stored Procedure。`JdbcClient` 可以直接下 `CALL`，不必再經過 ORM。後端寫出來的 SQL 只有固定的 `CALL` 字串，參數一律用 `?` 綁定，要檢查有沒有 SQL Injection，只要確認這一點。

### 為什麼每筆喜好各自擁有一筆 Product？

產品有價格和手續費率，同一檔商品在不同日子的價格不一樣。所以每筆喜好各自存一份當時的產品資料，改或刪其中一筆時，不會影響到別筆。刪除喜好時，它專屬的 Product 一起刪掉。

### 為什麼清單用 `afterSn` 接續，不用頁碼？

翻頁的時候資料可能新增或被刪掉，用頁碼的話，同一筆可能重複出現，也可能被漏掉。改用上一頁最後一筆的 `sn` 往後取，結果就穩定。查詢走 `(user_id, sn)` 索引，直接從上次的位置往後讀，不像 `OFFSET` 要先掃過前面所有資料，翻到很後面也不會變慢。用法見[回應範例](API.md#回應範例)。
