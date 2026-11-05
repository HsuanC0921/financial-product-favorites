-- 資料庫與四張資料表：使用者、可用帳號、產品、喜好清單
-- 以管理員帳號執行，順序見 DB\README.md，要重來時先 DROP DATABASE favorites

CREATE DATABASE IF NOT EXISTS favorites
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE favorites;

CREATE TABLE app_user (
    user_id       VARCHAR(64)  NOT NULL,
    user_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(254) NOT NULL,
    account       VARCHAR(32)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    CONSTRAINT pk_app_user PRIMARY KEY (user_id),
    CONSTRAINT ck_app_user_user_id CHECK (CHAR_LENGTH(TRIM(user_id)) > 0),
    CONSTRAINT ck_app_user_user_name CHECK (CHAR_LENGTH(TRIM(user_name)) > 0),
    CONSTRAINT ck_app_user_account CHECK (REGEXP_LIKE(account, '^[0-9]+$'))
) ENGINE = InnoDB;

-- 一個帳號只能屬於一位使用者
CREATE TABLE user_account (
    user_id      VARCHAR(64) NOT NULL,
    account      VARCHAR(32) NOT NULL,
    account_name VARCHAR(50) NOT NULL,
    CONSTRAINT pk_user_account PRIMARY KEY (user_id, account),
    CONSTRAINT uk_user_account_account UNIQUE (account),
    CONSTRAINT fk_user_account_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    CONSTRAINT ck_user_account_account CHECK (REGEXP_LIKE(account, '^[0-9]+$')),
    CONSTRAINT ck_user_account_name CHECK (CHAR_LENGTH(TRIM(account_name)) > 0)
) ENGINE = InnoDB;

CREATE TABLE product (
    no           BIGINT        NOT NULL AUTO_INCREMENT,
    product_name VARCHAR(100)  NOT NULL,
    price        DECIMAL(12,2) NOT NULL,
    fee_rate     DECIMAL(9,8)  NOT NULL,
    CONSTRAINT pk_product PRIMARY KEY (no),
    CONSTRAINT ck_product_name CHECK (CHAR_LENGTH(TRIM(product_name)) > 0),
    CONSTRAINT ck_product_price CHECK (price > 0),
    CONSTRAINT ck_product_fee_rate CHECK (fee_rate BETWEEN 0 AND 1)
) ENGINE = InnoDB;

-- created_at 以 UTC 保存，由前端轉成本地時間
CREATE TABLE like_list (
    sn                BIGINT        NOT NULL AUTO_INCREMENT,
    user_id           VARCHAR(64)   NOT NULL,
    product_no        BIGINT        NOT NULL,
    purchase_quantity INT           NOT NULL,
    account           VARCHAR(32)   NOT NULL,
    total_fee         DECIMAL(20,2) NOT NULL,
    total_amount      DECIMAL(20,2) NOT NULL,
    note              VARCHAR(200)  NOT NULL DEFAULT '',
    created_at        DATETIME      NOT NULL DEFAULT (UTC_TIMESTAMP()),
    version           BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_like_list PRIMARY KEY (sn),
    CONSTRAINT uk_like_list_product UNIQUE (product_no),
    CONSTRAINT fk_like_list_product FOREIGN KEY (product_no) REFERENCES product (no),
    CONSTRAINT fk_like_list_user_account FOREIGN KEY (user_id, account)
        REFERENCES user_account (user_id, account),
    CONSTRAINT ck_like_list_quantity CHECK (purchase_quantity BETWEEN 1 AND 1000000),
    CONSTRAINT ck_like_list_total_fee CHECK (total_fee >= 0),
    CONSTRAINT ck_like_list_total_amount CHECK (total_amount > 0),
    CONSTRAINT ck_like_list_version CHECK (version >= 0),
    INDEX ix_like_list_user_sn (user_id, sn)
) ENGINE = InnoDB;
