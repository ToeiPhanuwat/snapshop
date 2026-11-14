# Snapshop — เปรียบเทียบราคาสินค้าจาก Shopee Affiliate

ระบบรับไฟล์ CSV ที่ merge และจัดกลุ่มแล้ว (ขั้น F1–F4 ทำนอกระบบ) → เก็บใน PostgreSQL → API → เว็บ
- **ฝั่ง user** (`/`) แสดงกลุ่มสินค้าที่มีร้านขาย ≥ 2 ร้าน พร้อมราคาแต่ละร้าน และลิงก์ affiliate (ไม่มีให้ใช้ลิงก์สินค้า)
- **ฝั่ง admin** (`/admin`, ล็อกอินแบบ HTTP Basic) นำเข้าไฟล์, ดูทุกรายการ, ตรวจ REVIEW, ย้ายกลุ่ม, บันทึกเลิกขาย

ศัพท์ที่ใช้: [docs/GLOSSARY.md](docs/GLOSSARY.md) · บันทึกการทำงาน: [docs/WORKLOG.md](docs/WORKLOG.md)

## โครงสร้าง

```
backend/    Spring Boot 4.1 · Java 25 · Maven · Flyway
  src/main/java/com/snapshop/
    controller/       รับ/ส่ง DTO เท่านั้น
    service/          logic + transaction
      importing/      อ่าน CSV (ขอบระบบ) + ImportPlanner (กฎนำเข้า, ฟังก์ชันบริสุทธิ์)
    repository/       Spring Data JPA (เขียน) + JdbcClient (อ่าน)
    model/entity|enums
    dto/request|response
    exception/  config/
  src/main/resources/db/migration/   V1–V3 (+ view public_product = กฎแสดงฝั่ง user)
frontend/   Next.js 16 · Tailwind 4.3 · App Router
  src/app/(public)/   หน้า user
  src/app/admin/      หน้า admin + Server Actions (actions.ts)
  src/components/     ui / product / admin
  src/lib/api/        เรียก backend จากฝั่ง server เท่านั้น
  src/proxy.ts        บังคับล็อกอินหน้า /admin
```

## รันบนเครื่อง (dev)

ต้องมี JDK 25, Node 24, Docker

1. ตั้งค่า: คัดลอก `.env.example` เป็น `.env` และ `frontend/.env.example` เป็น `frontend/.env.local` แล้วตั้งรหัสผ่าน (admin ต้องตรงกันทั้งสองไฟล์)
2. ฐานข้อมูล:
   ```bash
   docker compose up -d db
   ```
3. Backend (Flyway สร้างตารางให้เอง):
   ```bash
   cd backend && set -a && . ../.env && set +a && ./mvnw spring-boot:run
   ```
4. Frontend:
   ```bash
   cd frontend && npm install && npm run dev
   ```
5. วางไฟล์ CSV ใน `inbox/` แล้วเปิด http://localhost:3000/admin/inbox (หรืออัปโหลดที่ `/admin/import`)

หรือรันทั้งหมดใน Docker:
```bash
docker compose --profile app up -d --build
```

## นำเข้าผ่าน inbox (ไม่ต้องใช้รหัสผ่าน admin)

1. คนหรือ AI เตรียมไฟล์ CSV (merge + แยกชนิดสินค้า) แล้ววางในโฟลเดอร์ `inbox/` ที่ root ของโปรเจกต์
2. admin เปิด `/admin/inbox` → "ดูพรีวิว" เห็นว่าจะเพิ่ม/ราคาเปลี่ยน/ซ่อน/รอตรวจ/กลุ่มใหม่อะไรบ้าง (ยังไม่บันทึก)
3. กด "นำเข้าไฟล์นี้" (ไฟล์ย้ายไป `inbox/imported/`) หรือ "ทิ้งไฟล์" (ย้ายไป `inbox/rejected/`)

ไฟล์ที่มีแถวผิดจะแสดงรายการปัญหาตามเลขแถวในหน้าพรีวิว และกดนำเข้าไม่ได้ ยังอัปโหลดไฟล์ตรงที่ `/admin/import` ได้เหมือนเดิม
ตำแหน่งโฟลเดอร์ตั้งได้ด้วย `IMPORT_INBOX_DIR` (ค่าเริ่ม `./inbox` ของโปรเซส backend; ใน Docker คือ `./inbox` ของโปรเจกต์)

## ไฟล์นำเข้า

CSV (UTF-8) คอลัมน์: `รหัสสินค้า, ชื่อสินค้า, ราคา, ขาย, ชื่อร้านค้า, ประเภทร้านค้า, อัตราค่าคอมมิชชัน, คอมมิชชัน, ลิงก์สินค้า, ลิงก์ข้อเสนอ, ลิงก์รูปภาพ, รหัสกลุ่ม, ชื่อกลุ่ม, วิธีจัดกลุ่ม`
- **ชื่อสินค้าต้องเป็นชื่อเต็ม** (ระบบแยกส่วนหน้า "Dr." เอง)
- `รหัสกลุ่ม` ในไฟล์ใช้บอกว่าแถวไหนอยู่ด้วยกันเท่านั้น รหัสกลุ่มจริง (`PT000001`) ระบบสร้างเอง
- แถวใดผิด ทั้งไฟล์ถูกปฏิเสธ พร้อมรายการปัญหาตามเลขแถว

## กฎการนำเข้า (สรุป)

| สถานการณ์ | ผล |
|---|---|
| รายการใหม่ | เพิ่มแถว `active_flag='Y'` |
| ราคาเท่าเดิม | อัปเดตเวลาเห็นล่าสุดเท่านั้น (ชื่อ/รูป/ร้านเปลี่ยนก็ไม่แตะ) |
| ราคาเปลี่ยน | แถวเดิม → `'N'` (ประวัติราคา), เพิ่มแถวใหม่ |
| ราคาย่อ เช่น "1.0พัน" | ไม่เทียบ สถานะ REVIEW |
| ไม่อยู่ในไฟล์รอบนี้ | สถานะ NOT_IN_LATEST (ซ่อนจาก user, `active_flag` ยัง `'Y'`) — admin ตรวจแล้วกด "เลิกขาย" ได้ |
| รายการเดิม | อยู่กลุ่มเดิมเสมอ แม้ไฟล์รอบใหม่จัดกลุ่มต่างไป |
| รายการใหม่ | ตามกลุ่มของเพื่อนร่วมกลุ่มในไฟล์ → ชื่อกลุ่มตรงกัน → สร้างกลุ่มใหม่ |
| กลุ่มในไฟล์มีสมาชิกเดิมจาก ≥ 2 กลุ่ม | ไม่รวมกลุ่ม รายการใหม่ได้ REVIEW ให้ admin ตัดสิน |

## ตรวจคุณภาพ

```bash
cd backend && ./mvnw verify
```
เทสต์ทั้งหมด (ใช้ Testcontainers PostgreSQL 17 ต้องเปิด Docker), JaCoCo, PMD CPD

```bash
cd backend && python tools/crap.py target/site/jacoco/jacoco.xml
```
CRAP ต่อ method (เกณฑ์ < 10)

```bash
cd backend && ./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
```
Mutation test กฎนำเข้า (เกณฑ์ 90%)

```bash
cd frontend && npm run lint && npm run build
```
