# 金融商品喜好紀錄系統

使用者登入後，可以新增、查詢、修改、刪除自己喜好的金融商品，記錄預計購買數量與扣款帳號。系統依價格、數量與手續費率計算預計扣款的總手續費與總金額。

本專案是面試作業，依題目要求設計為三層式架構：Web Server 與 Application Server 分開，資料存放在關聯式資料庫，後端的資料庫存取一律透過 Stored Procedure。

## 功能

- 登入與登出，每位使用者只看得到自己的資料。
- 查詢喜好清單：產品名稱、扣款帳號、總手續費、總金額，並顯示使用者的 Email。清單超過 100 筆時用「載入更多」接續。
- 新增、修改、刪除喜好。表單即時預估手續費與總金額。
- 新增自己的扣款帳號。
- 兩個人同時修改同一筆資料時，後送出的人會收到衝突提示，而不是悄悄覆蓋。

## 設計重點

下列幾項是設計時做過取捨的地方，原因寫在各份文件：

- 資料庫存取只用 Stored Procedure，應用程式的資料庫帳號只有 EXECUTE：[為什麼不用 JPA](docs/ARCHITECTURE.md#為什麼用-stored-procedure-加-jdbcclient不用-jpa)
- 清單用 `afterSn` 接續，不用頁碼：[原因](docs/ARCHITECTURE.md#為什麼清單用-aftersn-接續不用頁碼)
- 防範 XSS 是在顯示時跳脫，不在收到資料時過濾：[原因](docs/SECURITY.md)
- 兩人同時修改以版本號偵測，後送出的人會收到衝突提示：[並行更新與版本衝突測試](docs/TESTING.md#寫入交易與安全性)
- 沒做的功能與原因，例如註冊與軟刪除：[評估後沒有做的改進](#評估後沒有做的改進)

## 題目規格對照

題目的每項要求都已完成。

| 題目要求 | 實作與位置 |
| --- | --- |
| 新增、查詢、刪除、修改喜好 | 介面在 `FavoritesView`，後端由 `FavoriteController` 提供 API，規格見 [docs/API.md](docs/API.md) |
| 查詢顯示產品名稱清單、扣款帳號、總金額、總手續費、使用者 Email | 清單每筆列出產品名稱、扣款帳號、總手續費與預計扣款總金額，Email 顯示在頁首 |
| Web Server、Application Server 與關聯式資料庫的三層式架構 | Nginx（設定在 `deploy/nginx.conf`）、Spring Boot、MySQL。後端只監聽本機，對外只開 Nginx |
| 後端分為 Presentation、Business、Data、Shared 4 層 | `presentation`、`service`、`repository`、`common` 4 個套件，說明見 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| 前端使用 Vue.js | Vue 3、TypeScript |
| 後端使用 Spring Boot，API 採 RESTful | Spring Boot 4.1.2，以 `/api/favorites/{sn}` 這類資源路徑搭配 GET、POST、PUT、DELETE，結果以狀態碼表示 |
| 以 Maven 或 Gradle 建置 | Maven，使用專案內的 Maven Wrapper |
| 資料庫存取一律透過 Stored Procedure | 9 支 Stored Procedure，位於 `DB/procedures/`。後端只執行 `CALL`，應用程式的資料庫帳號也只有 EXECUTE 權限 |
| 異動多張資料表時使用 Transaction | 新增、修改、刪除都同時異動 Product 與 Like List，`FavoriteService` 以 `@Transactional` 包成同一個交易，由 [FavoriteTransactionTests](docs/TESTING.md#寫入交易與安全性) 確認新增失敗時觸發回滾 |
| DDL 與 DML 放在專案的 `DB` 資料夾 | `DB/ddl/` 與 `DB/dml/`，另有 `DB/procedures/` |
| 防範 SQL Injection | 參數一律以 `?` 綁定，不拼接 SQL 字串，見 [docs/SECURITY.md](docs/SECURITY.md) |
| 防範 XSS | 前端只用 Vue 的文字插值、不用 `v-html`，Nginx 再加上 Content-Security-Policy，見 [docs/SECURITY.md](docs/SECURITY.md) |
| 資料表至少包含 User、Product、Like List | `app_user`、`product`、`like_list`，另加 `user_account` 保存使用者可用的扣款帳號，欄位見 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#資料模型) |

## 文件

| 文件 | 內容 |
| --- | --- |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 系統架構、後端分層、資料模型、設計決策 |
| [資料庫設計](docs/ARCHITECTURE.md#資料模型) | 四張資料表的 ER 圖與欄位限制，GitHub 上直接顯示圖 |
| [docs/API.md](docs/API.md) | API 清單與狀態碼 |
| [docs/SECURITY.md](docs/SECURITY.md) | 已處理的安全威脅與做法、相依套件修補 |
| [docs/TESTING.md](docs/TESTING.md) | 測試範圍與執行方式 |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | 不經 Nginx 的開發模式 |
| [DB/README.md](DB/README.md) | 資料庫腳本的執行順序與重置方式 |

## 技術

| 項目 | 技術 |
| --- | --- |
| 後端 | Java 21、Spring Boot 4.1.2（Spring Framework 7.0、Spring Security 7.1）、Spring JDBC（`JdbcClient`）、MySQL Connector/J 9.7、Bean Validation（Hibernate Validator 9.1） |
| 資料庫 | MySQL 8.4，Stored Procedure 存取資料 |
| Web Server | Nginx 1.31（Docker 版用 `nginx:1.31-alpine` 映像） |
| 前端 | Vue 3.5、TypeScript 6.0、Vite 8、big.js 7（金額預估用十進位小數計算） |
| 建置 | Maven 3.10（Maven Wrapper）、npm 10（Node.js 22） |
| 測試 | 後端用 JUnit 6.0、Mockito 5.23、MockMvc，前端用 Vitest 5.0 |
| 程式碼檢查 | 前端：ESLint 10、Oxlint 1.82、Stylelint 17、Prettier 3.9、vue-tsc 3.3，後端：Prettier 3.9 搭配 prettier-plugin-java 2.11 |

## 專案結構

```text
financial-product-favorites/
├─ backend/    Spring Boot REST API
├─ frontend/   Vue 前端
├─ DB/         資料庫腳本，分成 ddl、procedures、dml 3 個資料夾
├─ deploy/     Nginx 設定，docker/ 放容器版的建置檔
├─ docs/       架構、API、安全性、測試與開發說明
└─ compose.yaml  Docker Compose 的啟動設定
```

## 快速開始

### 用 Docker 啟動

只需要安裝 Docker，不必另外裝 JDK、Node.js、MySQL 與 Nginx。在專案根目錄執行：

```powershell
docker compose up
```

第一次會下載映像檔並建置，所需時間依網路速度而定。看到 3 個容器都啟動後，開啟 `https://localhost`，帳號密碼見[示範使用者](#示範使用者)。憑證是自簽的，瀏覽器會警告不受信任，選擇繼續前往即可。停止並清除資料庫：

```powershell
docker compose down -v
```

容器版沿用同一份 `deploy/nginx.conf` 與 `DB/` 的腳本，差別有 3 處：

- Nginx 的自簽憑證在建置映像檔時產生，不需要手動安裝的步驟 3。
- 建置後端時不跑測試，因為那時資料庫還沒有起來。
- 後端與資料庫在不同的容器，所以多建一個連線來源為 `%` 的 `favorites_app`，權限同樣只有 EXECUTE，見 `DB/ddl/docker_app_user.sql`。

主機只開放 Nginx 的 80 與 443。容器內的後端監聽所有介面，Nginx 才連得到，但它與資料庫的埠都沒有對主機開放，只有 Compose 內部的網路可連。80 或 443 已被占用時，例如本機已有別的網頁伺服器，要先停掉。容器版的資料庫密碼同樣是公開的示範值，對外開放前要同時改 `DB/ddl/docker_app_user.sql` 與 `FAVORITES_DB_PASSWORD`。

### 手動安裝

不使用 Docker 時，以下步驟以 Nginx、Spring Boot、MySQL 三層啟動，指令都在專案根目錄執行。

#### 環境需求

- JDK 21，並設定好 `JAVA_HOME`
- Node.js 22.18 以上的 22.x，或 24.12 以上
- MySQL 8.4，以及 `mysql` 命令列用戶端
- Nginx 1.25.1 以上
- OpenSSL，Git for Windows 已內附

Maven 由專案內的 Maven Wrapper 提供，不需要另外安裝。

#### 1. 建立資料庫

依 [DB/README.md](DB/README.md) 的順序，以管理員帳號執行 5 支腳本。腳本會建立資料庫、資料表、2 個資料庫帳號、Stored Procedure 與示範資料。

#### 2. 建置

後端建置時會跑[測試](docs/TESTING.md#範圍)，整合測試連的是步驟 1 建好的資料庫。

```powershell
cd backend
.\mvnw.cmd package
cd ..\frontend
npm ci
npm run build
cd ..
```

#### 3. 產生本機憑證

`deploy/certs/` 不納入版本控制，每台機器各自產生：

```powershell
New-Item -ItemType Directory -Force deploy\certs, deploy\logs, deploy\temp
& 'C:\Program Files\Git\usr\bin\openssl.exe' req -x509 -newkey rsa:2048 -nodes -days 365 -keyout deploy/certs/key.pem -out deploy/certs/cert.pem -subj '/CN=localhost' -addext 'subjectAltName=DNS:localhost,IP:127.0.0.1'
```

#### 4. 啟動

各開一個終端機，分別啟動後端與 Nginx：

```powershell
java -jar backend\target\financial-product-favorites-0.0.1-SNAPSHOT.jar
```

```powershell
nginx -p deploy -c nginx.conf
```

開啟 `https://localhost`。憑證是自簽的，瀏覽器會警告不受信任，選擇繼續前往即可。`http://localhost` 會自動轉到 HTTPS。

停止 Nginx：

```powershell
nginx -p deploy -c nginx.conf -s stop
```

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
cd ..\frontend
npm run test:unit -- --run
```

後端有 [47 個測試](docs/TESTING.md#範圍)，其中整合測試連真實的 MySQL，執行前要先完成[建立資料庫](#1-建立資料庫)。前端有 [28 個金額與輸入檢查案例](docs/TESTING.md#金額與輸入邊界)，另有[瀏覽器流程驗證](docs/TESTING.md#瀏覽器流程驗證)。

壓測的操作情境、通過門檻、修正前後數據與資料庫查核，見[壓力測試結果](docs/TESTING.md#壓力測試)。尚未自動化與未涵蓋的部分，見[驗證限制](docs/TESTING.md#未自動化的部分)。

## 已知限制

- 憑證是本機自簽的，HSTS 也只設 300 秒，僅供本機展示。正式環境要換成受信任的憑證，並把 HSTS 改成一年。
- Nginx 與後端都只監聽 `127.0.0.1`。要開放給其他主機時，換掉憑證與示範密碼後，只改 `deploy/nginx.conf` 的 `listen`。後端維持只監聽本機，否則限流與安全標頭會被繞過。
- 登入限流以來源位址計算，擋不住從多個位址輪流猜同一個使用者 ID 的密碼，也沒有連續失敗就鎖定使用者的機制。
- 扣款帳號全系統唯一，新增時已被登錄就回 409。登入的使用者因此能試出某個扣款帳號是否已被登錄，但看不出屬於誰。
- 不提供註冊、忘記密碼與會員資料修改。題目沒有要求，使用者視為由既有系統建立。開放註冊的話，扣款帳號屬於誰沒辦法驗證，「使用者 ID 已被使用」的回應也會洩漏登入端刻意隱藏的資訊。
- 刪除是實體刪除，連同專屬的 Product 一起刪，無法復原。介面的確認對話框有寫明。
- 名稱篩選只搜尋已載入的資料，清單超過 100 筆時，要先按「載入更多」才找得到後面的。

## 評估後沒有做的改進

下列項目評估過，但各自要先決定一些事，單機展示又用不到，所以只記錄在這裡。

| 項目 | 現況 | 沒做的原因 |
| --- | --- | --- |
| Session 跨重啟與多台後端共用 | Session 存在後端記憶體，後端重啟就全部登出 | 要改用 Spring Session 搭配 Redis 或資料庫，多一個要部署與維運的元件 |
| 異動稽核紀錄 | 不記錄誰在何時改了什麼 | 每支寫入的 Stored Procedure 都要多寫一張稽核表，還要先決定保存多久、備註裡的個資怎麼處理 |
| 軟刪除 | 實體刪除，無法復原 | 喜好相關的 Stored Procedure 都要加上過濾條件，也要決定已刪除的資料保留多久 |
| 伺服器端的名稱搜尋 | 只在已載入的資料裡篩選 | 搜尋條件要和 `afterSn` 分頁一起設計。`LIKE '%字%'` 用不到索引，改用 FULLTEXT 又要設定中文分詞（ngram） |
| 即時報價 | 價格由使用者自己輸入 | 串接報價來源要處理授權、延遲，以及來源失效時退回手動輸入 |
| 持續的弱點掃描 | [弱點掃描](docs/SECURITY.md#弱點掃描)只做了一次，之後新公布的弱點不會自動發現 | 要定期排程執行，得先導入 CI，是否導入另行決定 |

## 第三方素材

- 頁面圖示使用 [Tabler Icons](https://github.com/tabler/tabler-icons) 的 `mountain`，採 MIT 授權，授權聲明保留在 `frontend/public/favicon.svg` 內。
- 清單空狀態的圖示是 Tabler Icons 的 `inbox`，同樣採 MIT 授權，檔案是 `frontend/public/empty-list.svg`，只把線條顏色改成專案的灰色。
- 登入頁背景是 [Hero Patterns](https://heropatterns.com) 的 Topography 圖樣，作者 Steve Schoger，依該網站標示採 [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) 授權，檔案是 `frontend/public/topography.svg`，只調整了填色與透明度。

## 授權聲明

本專案僅供面試作業審閱與評估。除法律另有規定或第三方元件原授權允許之範圍外，未經權利人事前書面同意，不得重製、散布、公開傳輸、改作、商業使用，或作為他人之面試作業提交。
