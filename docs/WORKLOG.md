# Work log

บันทึกทุกขั้นและเหตุผล (ตาม SKILL.md "Keep a Log")

## 2026-11-12 — หน่วย 1: โครงสร้างและ schema
- แตก branch `feat/initial-system` จาก `main` — CLAUDE.md ห้าม commit บน main
- สร้าง backend ด้วย Spring Initializr (Boot 4.1.2 รุ่นเสถียรล่าสุด, Java 25, Maven) — ได้ dependency ชุดที่ Boot 4 ต้องการ (starter-webmvc, starter-flyway, Testcontainers 2) ไม่ต้องเดา
- เขียน Flyway V1–V3 ก่อน logic — "Get the data shape right before you write the logic"
  - รหัสกลุ่ม `PT000001` สร้างจาก sequence ใน DB — ระบบเป็นผู้สร้างรหัสตามที่สั่ง
  - CHECK `name_prefix || name_core = name_raw`, partial unique index ของแถว 'Y', CHECK คู่ `active_flag`/`deactivated_at` — ทำให้สถานะผิดเขียนลง DB ไม่ได้
  - view `public_product` — กฎ "แสดงฝั่ง user" อยู่ที่เดียว
  - เพิ่ม `deactivation_reason` (PRICE_CHANGED / DISCONTINUED) — แยกประวัติราคาออกจากการเลิกขายที่ admin สั่ง
- เทสต์: แอปขึ้น, migration ผ่านบน PostgreSQL 17 (Testcontainers), DB ปฏิเสธแถว 'Y' ซ้ำและชื่อที่ประกอบไม่ได้
- พบระหว่างทาง: เทสต์ตรวจ `PT000001` ตรง ๆ ล้ม เพราะ `TRUNCATE ... RESTART IDENTITY` ไม่รีเซ็ต sequence ที่ไม่ได้เป็นของคอลัมน์ — เปลี่ยนเทสต์ให้ตรวจรูปแบบ `PT\d{6}` ซึ่งเป็นพฤติกรรมที่ผู้ใช้เห็นจริง
- ข้อมูลจริง: ไฟล์ merged ยังมีราคาย่อ "1.0พัน"/"1.2พัน" 3 แถว (item 26261939392, 26505445953, 28484198010) — จะได้สถานะ REVIEW ตามกฎ
