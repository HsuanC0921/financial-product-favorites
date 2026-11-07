# 本機開發與手動部署

[專案說明](../README.md) · [架構](ARCHITECTURE.md) · [API](API.md) · [安全性](SECURITY.md) · [測試](TESTING.md) · **開發與部署**

## 手動部署

不使用 Docker 時，以下步驟以 Nginx、Spring Boot、MySQL 三層啟動，指令都在專案根目錄執行。

### 環境需求

- JDK 21，並設定好 `JAVA_HOME`
- Node.js 22.18 以上的 22.x，或 24.12 以上
- MySQL 8.4，以及 `mysql` 命令列用戶端
- Nginx 1.25.1 以上
- OpenSSL，Git for Windows 已內附

Maven 由專案內的 Maven Wrapper 提供，不需要另外安裝。

### 1. 建立資料庫

依 [DB/README.md](../DB/README.md) 的順序，以管理員帳號執行 5 支腳本。腳本會建立資料庫、資料表、2 個資料庫帳號、Stored Procedure 與示範資料。

### 2. 建置

後端建置時會跑[測試](TESTING.md#範圍)，整合測試連的是步驟 1 建好的資料庫。

```powershell
cd backend
.\mvnw.cmd package
cd ..\frontend
npm ci
npm run build
cd ..
```

### 3. 產生本機憑證

`deploy/certs/` 不納入版本控制，每台機器各自產生：

```powershell
New-Item -ItemType Directory -Force deploy\certs, deploy\logs, deploy\temp
& 'C:\Program Files\Git\usr\bin\openssl.exe' req -x509 -newkey rsa:2048 -nodes -days 365 -keyout deploy/certs/key.pem -out deploy/certs/cert.pem -subj '/CN=localhost' -addext 'subjectAltName=DNS:localhost,IP:127.0.0.1'
```

### 4. 啟動

各開一個終端機，分別啟動後端與 Nginx：

```powershell
java -jar backend\target\financial-product-favorites-1.0.1.jar
```

```powershell
nginx -p deploy -c nginx.conf
```

開啟 `https://localhost`，帳號密碼見[示範使用者](../README.md#示範使用者)。憑證是自簽的，瀏覽器會警告不受信任，選擇繼續前往即可。`http://localhost` 會自動轉到 HTTPS。

停止 Nginx：

```powershell
nginx -p deploy -c nginx.conf -s stop
```

## 設定

後端的連線設定可用環境變數覆寫，預設值對應 DB 腳本建立的本機環境：

| 環境變數 | 預設值 | 用途 |
| --- | --- | --- |
| `FAVORITES_DB_URL` | `jdbc:mysql://localhost:3306/favorites` | 資料庫連線 |
| `FAVORITES_DB_USERNAME` | `favorites_app` | 只有 Stored Procedure 的 EXECUTE 權限 |
| `FAVORITES_DB_PASSWORD` | 公開的本機示範值 | 部署到他人可連線的環境前，必須更換 |
| `FAVORITES_BIND_ADDRESS` | `127.0.0.1` | 後端監聽位址 |

## Docker 設定

容器版沿用同一份 `deploy/nginx.conf` 與 `DB/` 的腳本，主要差別如下：

- Nginx 的自簽憑證由各容器首次啟動時產生，重啟沿用，不放進映像檔。不需要手動執行[產生本機憑證](#3-產生本機憑證)的步驟。
- 建置後端時不跑測試，因為那時資料庫還沒有起來。
- 後端與資料庫在不同的容器，所以多建一個連線來源為 `%` 的 `favorites_app`，權限同樣只有 EXECUTE，見 `DB/ddl/docker_app_user.sql`。

主機只開放 Nginx 的 80 與 443。容器內的後端監聽所有介面，Nginx 才連得到，但它與資料庫的埠都沒有對主機開放，只有 Compose 內部的網路可連。80 或 443 已被占用時，例如本機已有別的網頁伺服器，要先停掉。容器版的資料庫密碼同樣是公開的示範值，對外開放前要同時改 `DB/ddl/docker_app_user.sql` 與 `FAVORITES_DB_PASSWORD`。

PowerShell 的環境變數要設在執行 Compose 的同一個終端機，也可在專案根目錄的 `.env` 設定同名變數。`.env` 不納入版本控制：

```powershell
$env:FAVORITES_DB_PASSWORD = '你的資料庫密碼'
docker compose up --build
```

`DB/` 的初始化腳本只在資料卷為空時執行。已有資料卷時，修改 SQL 檔案不會自動套用，包含帳號密碼。需要保留資料時，請以資料庫管理員執行對應 SQL 或修改帳號密碼。確定要重置時才使用 `down -v`。

## 開發模式

開發時不經過 Nginx，前端由 Vite 開發伺服器提供，前端程式碼存檔後畫面立即更新。這個模式走 HTTP，沒有 TLS 與 Nginx 加的安全標頭，只用於開發。

先完成[建立資料庫](#1-建立資料庫)的步驟。如果以 Nginx 部署的後端還在執行，先停掉它，兩者都使用 8080。

### 啟動後端

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

後端網址是 `http://127.0.0.1:8080`，環境變數見[設定](#設定)。

### 啟動前端

另開終端機：

```powershell
cd frontend
npm ci
npm run dev
```

開啟 `http://localhost:5173`。開發伺服器會把 `/api` 轉給本機後端，瀏覽器看到的仍是同一個網址。
