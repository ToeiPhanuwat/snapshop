-- ไฟล์นำเข้ารูปแบบใหม่ (2026-11-17): ชื่อสินค้าแยก namePrefix มาในไฟล์, ไม่มีวิธีจัดกลุ่ม
-- ข้อมูลรูปแบบเก่าล้างทิ้งก่อน (docker compose down -v) migration นี้จึงไม่แปลงข้อมูลเดิม

-- view ขยาย p.* ตอนสร้าง ต้องลบก่อนลบคอลัมน์ แล้วสร้างใหม่
DROP VIEW public_product;

ALTER TABLE marketplace_product DROP CONSTRAINT ck_name_parts;
ALTER TABLE marketplace_product DROP COLUMN name_raw;
ALTER TABLE product_type DROP COLUMN grouping_method;

-- สินค้าทุกรายการมี 2 กลุ่ม: กลุ่ม E (ตามชื่ออังกฤษ, product_type_id) และกลุ่ม R (ตามกฎคำระบุสูตร, rule_type_id ว่างได้)
-- ทั้งสองแบบเก็บใน product_type แยกด้วย kind ชื่อซ้ำกันข้าม kind ได้
ALTER TABLE product_type ADD COLUMN kind VARCHAR(8) NOT NULL DEFAULT 'NAME' CHECK (kind IN ('NAME', 'RULE'));
ALTER TABLE product_type ALTER COLUMN kind DROP DEFAULT;
ALTER TABLE product_type DROP CONSTRAINT uq_product_type_category_name;
ALTER TABLE product_type ADD CONSTRAINT uq_product_type_category_kind_name UNIQUE (category_code, kind, type_name_norm);

ALTER TABLE marketplace_product ADD COLUMN rule_type_id BIGINT REFERENCES product_type (product_type_id);
CREATE INDEX ix_marketplace_product_rule_type ON marketplace_product (rule_type_id);

CREATE VIEW public_product AS
SELECT p.*, t.product_type_code, t.type_name, t.category_code
FROM (
    SELECT mp.*, count(*) OVER (PARTITION BY mp.product_type_id) AS type_visible_count
    FROM marketplace_product mp
    WHERE mp.active_flag = 'Y' AND mp.status = 'OK'
) p
JOIN product_type t ON t.product_type_id = p.product_type_id
WHERE p.type_visible_count >= 2;
