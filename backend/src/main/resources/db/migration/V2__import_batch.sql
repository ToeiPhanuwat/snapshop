-- หนึ่งรอบนำเข้า = หนึ่งไฟล์ CSV ที่ merge และจัดกลุ่มแล้ว
CREATE TABLE import_batch (
    batch_id              BIGSERIAL    PRIMARY KEY,
    category_code         VARCHAR(32)  NOT NULL,
    source_files          TEXT         NOT NULL,
    row_count             INT          NOT NULL,
    inserted_count        INT          NOT NULL,
    price_changed_count   INT          NOT NULL,
    unchanged_count       INT          NOT NULL,
    not_in_latest_count   INT          NOT NULL,
    new_type_count        INT          NOT NULL,
    imported_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);
