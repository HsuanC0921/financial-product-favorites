# 本機開發與手動部署

[專案說明](../README.md) · [架構](ARCHITECTURE.md) · [API](API.md) · [安全性](SECURITY.md) · [測試](TESTING.md) · **開發與部署**

## 手動部署

以下使用 Windows PowerShell。除另有說明外，從專案根目錄執行。

### 環境需求

- JDK 21，並設定好 `JAVA_HOME`
- Node.js 22.18 以上的 22.x，或 24.12 以上
- MySQL 8.4 與 `mysql` 用戶端，服務須已監聽 `localhost:3306`
- Nginx 1.25.1 以上，`nginx` 須在 PATH 內
- OpenSSL，Git for Windows 已內附

使用專案內的 Maven Wrapper。80、443、8080 埠須可用，若本專案的 Docker 服務仍在執行，先用 `docker compose down` 停止。

### 1. 建立資料庫

在 `DB` 資料夾依 [DB/README.md](../DB/README.md) 執行 5 支腳本，完成後回到專案根目錄。

### 2. 建置

建置會跑[測試](TESTING.md#範圍)，須先完成步驟 1。

```powershell
cd backend
.\mvnw.cmd package
cd ..\frontend
npm ci
npm run build
cd ..
```

### 3. 產生本機憑證

`deploy/certs/` 不納入版本控制。下方使用 Git for Windows 的預設 OpenSSL 路徑，安裝位置不同時請調整：

```powershell
New-Item -ItemType Directory -Force deploy\certs, deploy\logs, deploy\temp
& 'C:\Program Files\Git\usr\bin\openssl.exe' req -x509 -newkey rsa:2048 -nodes -days 365 -keyout deploy/certs/key.pem -out deploy/certs/cert.pem -subj '/CN=localhost' -addext 'subjectAltName=DNS:localhost,IP:127.0.0.1'
```

### 4. 啟動

兩個終端機都從專案根目錄執行。自訂[連線設定](#設定)時，建置與啟動後端的終端機須設相同環境變數。

```powershell
java -jar backend\target\financial-product-favorites-1.0.1.jar
```

看到 `Started FinancialProductFavoritesApplication` 後，在另一個終端機啟動 Nginx：

```powershell
nginx -p deploy -c nginx.conf
```

開啟 `https://localhost`，以[示範使用者](../README.md#示範使用者)登入。自簽憑證警告選擇繼續即可，HTTP 會轉到 HTTPS。

停止 Nginx：

```powershell
nginx -p deploy -c nginx.conf -s stop
```

## 設定

後端連線可用環境變數覆寫，預設值對應 DB 腳本：

| 環境變數 | 預設值 | 用途 |
| --- | --- | --- |
| `FAVORITES_DB_URL` | `jdbc:mysql://localhost:3306/favorites` | 資料庫連線 |
| `FAVORITES_DB_USERNAME` | `favorites_app` | 只有 Stored Procedure 的 EXECUTE 權限 |
| `FAVORITES_DB_PASSWORD` | 公開的本機示範值 | 部署到他人可連線的環境前，必須更換 |
| `FAVORITES_BIND_ADDRESS` | `127.0.0.1` | 後端監聽位址 |

## Docker 設定

容器版沿用 `deploy/nginx.conf` 與 `DB/` 腳本，差異如下：

- Nginx 首次啟動時自動產生自簽憑證，重啟沿用，私鑰不放進映像檔。
- 建置後端時不跑測試，該階段尚無資料庫。
- 後端以 `favorites_app@'%'` 連線，權限只有 EXECUTE，見 `DB/ddl/docker_app_user.sql`。

主機只開放 Nginx 的 80 與 443。容器內的後端監聽所有介面，Nginx 才連得到，但它與資料庫的埠都沒有對主機開放，只有 Compose 內部的網路可連。80 或 443 已被占用時，例如本機已有別的網頁伺服器，要先停掉。容器版的資料庫密碼同樣是公開的示範值，對外開放前要同時改 `DB/ddl/docker_app_user.sql` 與 `FAVORITES_DB_PASSWORD`。

PowerShell 環境變數須設在執行 Compose 的終端機，也可寫入專案根目錄的 `.env`（不納入版本控制）：

```powershell
$env:FAVORITES_DB_PASSWORD = '你的資料庫密碼'
docker compose up --build
```

`DB/` 的初始化腳本只在資料卷為空時執行。已有資料卷時，修改 SQL 檔案不會自動套用，包含帳號密碼。需要保留資料時，請以資料庫管理員執行對應 SQL 或修改帳號密碼。確定要重置時才使用 `down -v`。

### Windows 容器模式

啟動 [Docker Desktop](https://docs.docker.com/desktop/setup/install/windows-install/)，等引擎就緒後，在 Windows 工作列通知區的 Docker 圖示按右鍵：

- 「Switch to Linux containers」：選取後等待切換完成。
- 「Switch to Windows containers」：目前已在 Linux 容器模式，不必切換。

在 PowerShell 確認模式，應輸出 `linux`。此指令僅查詢，不會切換：

```powershell
docker info --format '{{.OSType}}'
```

確認後依[用 Docker 啟動](../README.md#用-docker-啟動)或預建套件的 README 操作。

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
