-- ไฟล์นำเข้ารูปแบบใหม่ (2026-11-17): ชื่อสินค้าแยก namePrefix มาในไฟล์, ไม่มีวิธีจัดกลุ่ม
-- ข้อมูลรูปแบบเก่าล้างทิ้งก่อน (docker compose down -v) migration นี้จึงไม่แปลงข้อมูลเดิม

-- view ขยาย p.* ตอนสร้าง ต้องลบก่อนลบคอลัมน์ แล้วสร้างใหม่
DROP VIEW public_product;

ALTER TABLE marketplace_product DROP CONSTRAINT ck_name_parts;
ALTER TABLE marketplace_product DROP COLUMN name_raw;
ALTER TABLE product_type DROP COLUMN grouping_method;

CREATE VIEW public_product AS
SELECT p.*, t.product_type_code, t.type_name, t.category_code
FROM (
    SELECT mp.*, count(*) OVER (PARTITION BY mp.product_type_id) AS type_visible_count
    FROM marketplace_product mp
    WHERE mp.active_flag = 'Y' AND mp.status = 'OK'
) p
JOIN product_type t ON t.product_type_id = p.product_type_id
WHERE p.type_visible_count >= 2;
