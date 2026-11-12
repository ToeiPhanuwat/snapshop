-- กลุ่ม = สินค้าชนิดเดียวกันที่ขายหลายร้าน รหัสกลุ่มสร้างโดยระบบ (ไม่ใช่รหัสในไฟล์นำเข้า)
CREATE SEQUENCE product_type_code_seq;

CREATE TABLE product_type (
    product_type_id   BIGSERIAL    PRIMARY KEY,
    product_type_code VARCHAR(16)  NOT NULL UNIQUE
                      DEFAULT ('PT' || lpad(nextval('product_type_code_seq')::text, 6, '0')),
    category_code     VARCHAR(32)  NOT NULL,
    type_name         TEXT         NOT NULL,
    type_name_norm    TEXT         NOT NULL,
    grouping_method   VARCHAR(32)  NOT NULL
                      CHECK (grouping_method IN ('ENGLISH_NAME_MATCH', 'FORMULA_RULE', 'NO_MATCH')),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_product_type_category_name UNIQUE (category_code, type_name_norm)
);
