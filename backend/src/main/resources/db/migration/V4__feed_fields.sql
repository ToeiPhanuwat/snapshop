-- ข้อมูลที่มาจาก feed ของ Shopee (แทนข้อมูลที่เคย scrape จากหน้าค้นหา)
ALTER TABLE marketplace_product
    ADD COLUMN list_price     NUMERIC(12,2),
    ADD COLUMN stock          INTEGER,
    ADD COLUMN item_rating    NUMERIC(3,2),
    ADD COLUMN shop_rating    NUMERIC(3,2),
    ADD COLUMN feed_synced_at TIMESTAMPTZ;

-- ราคาใน F1 เป็นตัวย่อ ("1.0พัน") → รอราคาจาก feed (ไม่ใช่ REVIEW)
ALTER TABLE marketplace_product DROP CONSTRAINT marketplace_product_status_check;
ALTER TABLE marketplace_product ADD CONSTRAINT marketplace_product_status_check
    CHECK (status IN ('OK', 'NOT_IN_LATEST', 'TEST', 'OUT_OF_STOCK', 'REVIEW', 'PRICE_PENDING'));
ALTER TABLE marketplace_product DROP CONSTRAINT ck_price_or_review;
ALTER TABLE marketplace_product ADD CONSTRAINT ck_price_or_review
    CHECK (price IS NOT NULL OR status IN ('REVIEW', 'NOT_IN_LATEST', 'TEST', 'OUT_OF_STOCK', 'PRICE_PENDING'));

-- ประเภทร้านและรูปที่ scrape มาใช้ไม่ได้ ล้างทิ้งแล้วรอ feed เติม
UPDATE marketplace_product SET shop_type = 'REGULAR', image_url = NULL;

-- view ขยาย p.* ตอนสร้าง คอลัมน์ใหม่จึงต้องสร้าง view ใหม่ (กฎแสดงฝั่ง user เหมือนเดิม)
DROP VIEW public_product;
CREATE VIEW public_product AS
SELECT p.*, t.product_type_code, t.type_name, t.category_code
FROM (
    SELECT mp.*, count(*) OVER (PARTITION BY mp.product_type_id) AS type_visible_count
    FROM marketplace_product mp
    WHERE mp.active_flag = 'Y' AND mp.status = 'OK'
) p
JOIN product_type t ON t.product_type_id = p.product_type_id
WHERE p.type_visible_count >= 2;

-- ผลการดึง feed แต่ละรอบ ไว้ให้ admin ดูว่าสำเร็จหรือล้มเหลว
CREATE TABLE feed_sync_run (
    run_id          BIGSERIAL    PRIMARY KEY,
    import_batch_id BIGINT       REFERENCES import_batch (batch_id),
    started_at      TIMESTAMPTZ  NOT NULL,
    finished_at     TIMESTAMPTZ,
    requested_count INTEGER      NOT NULL,
    found_count     INTEGER      NOT NULL DEFAULT 0,
    outcome         VARCHAR(16)  NOT NULL CHECK (outcome IN ('RUNNING', 'DONE', 'INCOMPLETE', 'RATE_LIMITED', 'FAILED')),
    message         TEXT
);
