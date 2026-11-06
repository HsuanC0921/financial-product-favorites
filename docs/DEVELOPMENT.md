# 開發模式

[專案說明](../README.md) · [架構](ARCHITECTURE.md) · [API](API.md) · [安全性](SECURITY.md) · [測試](TESTING.md) · **開發模式**

開發時不經過 Nginx，前端由 Vite 開發伺服器提供，前端程式碼存檔後畫面立即更新。這個模式走 HTTP，沒有 TLS 與 Nginx 加的安全標頭，只用於開發。

資料庫的建立方式與[快速開始](../README.md#1-建立資料庫)相同。如果以 Nginx 部署的後端還在執行，先停掉它，兩者都使用 8080。

## 啟動後端

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

後端網址是 `http://127.0.0.1:8080`，環境變數見[設定](../README.md#設定)。

## 啟動前端

另開終端機：

```powershell
cd frontend
npm ci
npm run dev
```

開啟 `http://localhost:5173`。開發伺服器會把 `/api` 轉給本機後端，瀏覽器看到的仍是同一個網址。
