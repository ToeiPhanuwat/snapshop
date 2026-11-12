-- 1 แถว = 1 เวอร์ชันราคาของรายการ (item_id) หนึ่ง
-- แถว active_flag='Y' คือเวอร์ชันปัจจุบัน (มีได้ 1 แถวต่อ item_id), แถว 'N' คือประวัติ
CREATE TABLE marketplace_product (
    product_id          BIGSERIAL     PRIMARY KEY,
    item_id             BIGINT        NOT NULL,
    shop_id             BIGINT        NOT NULL,
    shop_name           TEXT          NOT NULL,
    shop_type           VARCHAR(16)   NOT NULL CHECK (shop_type IN ('MALL', 'PREFERRED', 'REGULAR')),
    name_prefix         TEXT          NOT NULL,
    name_core           TEXT          NOT NULL,
    name_raw            TEXT          NOT NULL,
    product_type_id     BIGINT        NOT NULL REFERENCES product_type (product_type_id),
    price               NUMERIC(12,2),
    sold_text           TEXT,
    commission_rate     NUMERIC(5,2),
    commission_amount   NUMERIC(12,2),
    product_url         TEXT          NOT NULL,
    affiliate_url       TEXT,
    image_url           TEXT,
    status              VARCHAR(16)   NOT NULL
                        CHECK (status IN ('OK', 'NOT_IN_LATEST', 'TEST', 'OUT_OF_STOCK', 'REVIEW')),
    active_flag         CHAR(1)       NOT NULL CHECK (active_flag IN ('Y', 'N')),
    import_batch_id     BIGINT        NOT NULL REFERENCES import_batch (batch_id),
    last_seen_batch_id  BIGINT        NOT NULL REFERENCES import_batch (batch_id),
    last_seen_at        TIMESTAMPTZ   NOT NULL,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    deactivated_at      TIMESTAMPTZ,
    deactivation_reason VARCHAR(16)   CHECK (deactivation_reason IN ('PRICE_CHANGED', 'DISCONTINUED')),
    CONSTRAINT ck_name_parts CHECK (name_prefix || name_core = name_raw),
    CONSTRAINT ck_price_or_review CHECK (price IS NOT NULL OR status IN ('REVIEW', 'NOT_IN_LATEST', 'TEST', 'OUT_OF_STOCK')),
    CONSTRAINT ck_deactivation CHECK (
        (active_flag = 'Y' AND deactivated_at IS NULL AND deactivation_reason IS NULL)
        OR (active_flag = 'N' AND deactivated_at IS NOT NULL AND deactivation_reason IS NOT NULL))
);

CREATE UNIQUE INDEX uq_marketplace_product_active_item
    ON marketplace_product (item_id) WHERE active_flag = 'Y';
CREATE INDEX ix_marketplace_product_item ON marketplace_product (item_id);
CREATE INDEX ix_marketplace_product_type ON marketplace_product (product_type_id);

-- กฎ "แสดงฝั่ง user" อยู่ที่เดียว: แถว 'Y', status OK, และกลุ่มมีรายการแบบนี้ >= 2 รายการ
CREATE VIEW public_product AS
SELECT p.*, t.product_type_code, t.type_name, t.category_code
FROM (
    SELECT mp.*, count(*) OVER (PARTITION BY mp.product_type_id) AS type_visible_count
    FROM marketplace_product mp
    WHERE mp.active_flag = 'Y' AND mp.status = 'OK'
) p
JOIN product_type t ON t.product_type_id = p.product_type_id
WHERE p.type_visible_count >= 2;
