# 金融商品喜好紀錄系統

使用者登入後，可以新增、查詢、修改、刪除自己喜好的金融商品，記錄預計購買數量與扣款帳號。系統依價格、數量與手續費率計算預計扣款的總手續費與總金額。

本專案是面試作業，依題目要求設計為三層式架構：Web Server 與 Application Server 分開，資料存放在關聯式資料庫，後端的資料庫存取一律透過 Stored Procedure。

## 題目規格對照

| 題目要求 | 實作與位置 |
| --- | --- |
| DDL 與 DML 放在專案的 `DB` 資料夾 | `DB/ddl/` 與 `DB/dml/`，另有 `DB/procedures/` |
| 資料表至少包含 User、Product、Like List | `app_user`、`product`、`like_list`，另加 `user_account` 保存使用者可用的扣款帳號，欄位見 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#資料模型) |

## 文件

| 文件 | 內容 |
| --- | --- |
| [資料庫設計](docs/ARCHITECTURE.md#資料模型) | 四張資料表的 ER 圖與欄位限制，GitHub 上直接顯示圖 |
| [DB/README.md](DB/README.md) | 資料庫腳本的執行順序與重置方式 |

## 技術

| 項目 | 技術 |
| --- | --- |
| 後端 | Java 21、Spring Boot 4.1.2 |
| 資料庫 | MySQL 8.4，Stored Procedure 存取資料 |
| 前端 | Vue 3.5、TypeScript 6.0、Vite 8 |
| 建置 | Maven 3.10（Maven Wrapper）、npm 10（Node.js 22） |
| 程式碼檢查 | 後端：Prettier 3.9 搭配 prettier-plugin-java 2.11 |

## 專案結構

```text
financial-product-favorites/
├─ backend/    Spring Boot REST API
├─ frontend/   Vue 前端
├─ DB/         資料庫腳本，分成 ddl、procedures、dml 3 個資料夾
└─ docs/       架構與資料模型說明
```

## 快速開始

### 1. 建立資料庫

依 [DB/README.md](DB/README.md) 的順序，以管理員帳號執行 5 支腳本。腳本會建立資料庫、資料表、2 個資料庫帳號、Stored Procedure 與示範資料。

### 示範使用者

示範資料全是虛構的，只供本機展示。

| 使用者 ID | 姓名 | 密碼 |
| --- | --- | --- |
| `A1236456789` | 王小明 | `DemoUserA#2026` |
| `B2345678901` | 李小華 | `DemoUserB#2026` |
| `C3456789012` | 陳小美 | `DemoUserC#2026` |
| `D4567890123` | 林佳怡 | `DemoUserD#2026` |
| `E5678901234` | 黃志豪 | `DemoUserE#2026` |

想操作「載入更多」，可再執行選用的 `DB/dml/pagination_sample.sql`，它會新增一位有 120 筆喜好的使用者 `F6789012345`，密碼是 `DemoUserF#2026`。

## 授權聲明

本專案僅供面試作業審閱與評估。除法律另有規定或第三方元件原授權允許之範圍外，未經權利人事前書面同意，不得重製、散布、公開傳輸、改作、商業使用，或作為他人之面試作業提交。
