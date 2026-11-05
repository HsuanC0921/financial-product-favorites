# 資料庫腳本

[回到專案說明](../README.md)

MySQL 8.4。所有腳本以管理員帳號（root）依下列順序執行。

| 順序 | 檔案 | 內容 |
| --- | --- | --- |
| 1 | `ddl/01_schema.sql` | 資料庫、資料表、主外鍵、唯一鍵、CHECK、索引 |
| 2 | `ddl/02_accounts.sql` | `favorites_definer` 與 `favorites_app` 的建立與授權 |
| 3 | `procedures/sp_user.sql` | 使用者與扣款帳號的 Stored Procedure |
| 4 | `procedures/sp_favorite.sql` | 喜好清單的 Stored Procedure |
| 5 | `dml/seed.sql` | 虛構的示範資料，登入帳密寫在檔案開頭的註解 |
| 選用 | `dml/pagination_sample.sql` | 一位有 120 筆喜好的使用者，用來操作「載入更多」，登入帳密寫在檔案開頭的註解 |

第 2 步必須在建立 Stored Procedure 之前執行，因為 definer 帳號要先存在。資料夾依字母排序是 `ddl`、`dml`、`procedures`，不是執行順序，請照上表。

在 `DB` 資料夾執行，每一行都會詢問 root 密碼。`mysql` 不在 PATH 上時，改用完整路徑，例如 `& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe'`：

```powershell
mysql -u root -p -e "source ddl/01_schema.sql"
mysql -u root -p -e "source ddl/02_accounts.sql"
mysql -u root -p -e "source procedures/sp_user.sql"
mysql -u root -p -e "source procedures/sp_favorite.sql"
mysql -u root -p -e "source dml/seed.sql"
```

PowerShell 不支援 `<` 導向，所以改用 `source`。在 Git Bash 或 cmd 也可以用 `mysql -u root -p < ddl/01_schema.sql` 的寫法。

重置資料庫：執行 `DROP DATABASE favorites;`，再從第 1 步重新來過。`ddl/02_accounts.sql` 可以重複執行，已存在的資料庫帳號，密碼不會被覆寫。

資料表的關聯與欄位限制見[資料模型](../docs/ARCHITECTURE.md#資料模型)。
