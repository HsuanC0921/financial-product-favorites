# 金融商品喜好紀錄系統

使用者登入後，可以新增、查詢、修改、刪除自己喜好的金融商品，記錄預計購買數量與扣款帳號。系統依價格、數量與手續費率計算預計扣款的總手續費與總金額。

本專案是面試作業，依題目要求設計為三層式架構：Web Server 與 Application Server 分開，資料存放在關聯式資料庫，後端的資料庫存取一律透過 Stored Procedure。

## 功能

- 登入與登出，每位使用者只看得到自己的資料。
- 查詢喜好清單：產品名稱、扣款帳號、總手續費、總金額，並回傳使用者的 Email。清單超過 100 筆時用 `afterSn` 接續。

## 題目規格對照

| 題目要求 | 實作與位置 |
| --- | --- |
| 後端分為 Presentation、Business、Data、Shared 4 層 | `presentation`、`service`、`repository`、`common` 4 個套件，說明見 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| 以 Maven 或 Gradle 建置 | Maven，使用專案內的 Maven Wrapper |
| 資料庫存取一律透過 Stored Procedure | 9 支 Stored Procedure，位於 `DB/procedures/`。後端只執行 `CALL`，應用程式的資料庫帳號也只有 EXECUTE 權限 |
| DDL 與 DML 放在專案的 `DB` 資料夾 | `DB/ddl/` 與 `DB/dml/`，另有 `DB/procedures/` |
| 資料表至少包含 User、Product、Like List | `app_user`、`product`、`like_list`，另加 `user_account` 保存使用者可用的扣款帳號，欄位見 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#資料模型) |

## 文件

| 文件 | 內容 |
| --- | --- |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 系統架構、後端分層、資料模型、設計決策 |
| [資料庫設計](docs/ARCHITECTURE.md#資料模型) | 四張資料表的 ER 圖與欄位限制，GitHub 上直接顯示圖 |
| [docs/API.md](docs/API.md) | API 清單與狀態碼 |
| [DB/README.md](DB/README.md) | 資料庫腳本的執行順序與重置方式 |

## 技術

| 項目 | 技術 |
| --- | --- |
| 後端 | Java 21、Spring Boot 4.1.2（Spring Framework 7.0、Spring Security 7.1）、Spring JDBC（`JdbcClient`）、MySQL Connector/J 9.7、Bean Validation（Hibernate Validator 9.1） |
| 資料庫 | MySQL 8.4，Stored Procedure 存取資料 |
| 前端 | Vue 3.5、TypeScript 6.0、Vite 8 |
| 建置 | Maven 3.10（Maven Wrapper）、npm 10（Node.js 22） |
| 測試 | 後端用 JUnit 6.0、Mockito 5.23、MockMvc |
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

### 環境需求

- JDK 21，並設定好 `JAVA_HOME`
- MySQL 8.4，以及 `mysql` 命令列用戶端

Maven 由專案內的 Maven Wrapper 提供，不需要另外安裝。

### 1. 建立資料庫

依 [DB/README.md](DB/README.md) 的順序，以管理員帳號執行 5 支腳本。腳本會建立資料庫、資料表、2 個資料庫帳號、Stored Procedure 與示範資料。

### 2. 建置並啟動後端

後端建置時會跑測試，測試連的是步驟 1 建好的資料庫。

```powershell
cd backend
.\mvnw.cmd package
java -jar target\financial-product-favorites-0.0.1-SNAPSHOT.jar
```

後端監聽 `http://127.0.0.1:8080`，API 規格見 [docs/API.md](docs/API.md)。

### 設定

後端的連線設定可用環境變數覆寫，預設值對應 DB 腳本建立的本機環境：

| 環境變數 | 預設值 | 用途 |
| --- | --- | --- |
| `FAVORITES_DB_URL` | `jdbc:mysql://localhost:3306/favorites` | 資料庫連線 |
| `FAVORITES_DB_USERNAME` | `favorites_app` | 只有 Stored Procedure 的 EXECUTE 權限 |
| `FAVORITES_DB_PASSWORD` | 公開的本機示範值 | 部署到他人可連線的環境前，必須更換 |
| `FAVORITES_BIND_ADDRESS` | `127.0.0.1` | 後端監聽位址 |

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

## 測試

```powershell
cd backend
.\mvnw.cmd verify
```

後端的整合測試連真實的 MySQL，執行前要先完成[建立資料庫](#1-建立資料庫)。

## 授權聲明

本專案僅供面試作業審閱與評估。除法律另有規定或第三方元件原授權允許之範圍外，未經權利人事前書面同意，不得重製、散布、公開傳輸、改作、商業使用，或作為他人之面試作業提交。
