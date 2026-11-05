# 系統架構與設計決策

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
- 價格大於 0，手續費率介於 0 到 1，購買數量介於 1 到 1,000,000，由資料庫的 CHECK 把關。
- 手續費率存成比例，`0.1` 就是 10%，`0.01` 就是 1%，和題目的例子一致，畫面上則用百分比輸入與顯示。
- 金額一律是新台幣，用 `DECIMAL` 存到小數 2 位，不用浮點數。
- 時間以 UTC 存放，由前端轉成本地時間。

建表與 Stored Procedure 的腳本見 [DB/README.md](../DB/README.md)。
