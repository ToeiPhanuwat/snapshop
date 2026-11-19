-- admin ยืนยันกลุ่ม R แล้ว (ยุบรวมกลุ่ม E ที่ติ๊ก): กลุ่มนี้ไม่ต้องขึ้นในส่วน "ยุบรวมได้" ของหน้า REVIEW อีก
-- จนกว่าการนำเข้าจะเพิ่มรายการใหม่เข้ากลุ่ม R นี้ (ล้างกลับเป็น NULL)
ALTER TABLE product_type ADD COLUMN merge_reviewed_at TIMESTAMPTZ;
ALTER TABLE product_type ADD CONSTRAINT ck_merge_reviewed_only_rule_groups
    CHECK (merge_reviewed_at IS NULL OR kind = 'RULE');
