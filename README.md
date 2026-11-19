# Snapshop — เปรียบเทียบราคาสินค้าจาก Shopee Affiliate

ระบบรับไฟล์ CSV จาก F1 ที่จัดกลุ่มแล้ว (ขั้น F1 และแยกชนิดสินค้าทำนอกระบบ — ดู [docs/DATA-FLOW.md](docs/DATA-FLOW.md)) → เก็บใน PostgreSQL → ดึงราคาที่ขาด ประเภทร้าน และรูปจาก feed ของ Shopee อัตโนมัติ → API → เว็บ
- **ฝั่ง user** (`/`) แสดงกลุ่มสินค้าที่มีรายการแสดงได้ ≥ 2 รายการ พร้อมรูป (ไม่มีรูปแสดง "ยังไม่มีรูป") ราคาแต่ละร้าน และลิงก์ affiliate (ไม่มีให้ใช้ลิงก์สินค้า)
- **ฝั่ง admin** (`/admin`, ล็อกอินแบบ HTTP Basic) นำเข้าไฟล์, ดูทุกรายการ, ตรวจ REVIEW, REVIEW กลุ่ม R (รวมกลุ่ม E เข้ากลุ่ม R), ดูรายการรอราคาจาก feed และผลการดึง feed, ย้ายกลุ่ม, บันทึกเลิกขาย

Flow ข้อมูลตั้งแต่ F1 ถึงเว็บ: [docs/DATA-FLOW.md](docs/DATA-FLOW.md) · ศัพท์ที่ใช้: [docs/GLOSSARY.md](docs/GLOSSARY.md) · บันทึกการทำงาน: [docs/WORKLOG.md](docs/WORKLOG.md)

## โครงสร้าง

```
backend/    Spring Boot 4.1 · Java 25 · Maven · Flyway
  src/main/java/com/snapshop/
    controller/       รับ/ส่ง DTO เท่านั้น
    service/          logic + transaction
      importing/      อ่าน CSV (ขอบระบบ) + ImportPlanner / RulePlanner (กฎนำเข้า, ฟังก์ชันบริสุทธิ์)
      feed/           ดึง feed หลังนำเข้า: FeedClient (ขอบระบบ), FeedPlanner (บริสุทธิ์), FeedSyncService
    repository/       Spring Data JPA (เขียน) + JdbcClient (อ่าน)
    model/entity|enums
    dto/request|response
    exception/  config/
  src/main/resources/db/migration/   V1–V5 (+ view public_product = กฎแสดงฝั่ง user)
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

CSV (UTF-8) 14 คอลัมน์ (รูปแบบตั้งแต่ 2026-11-17 ตัวอย่าง `research/drpong_new_grouped_20261117.csv`):
`รหัสสินค้า, namePrefix, ชื่อสินค้า, ราคา, ขาย, ชื่อร้านค้า, อัตราค่าคอมมิชชัน, คอมมิชชัน, ลิงก์สินค้า, ลิงก์ข้อเสนอ, รหัสกลุ่ม, ชื่อกลุ่ม, รหัสกลุ่มตามกฎ, ชื่อกลุ่มตามกฎ`
- `namePrefix` = ข้อความก่อน "Dr." (ว่างได้), `ชื่อสินค้า` = ตั้งแต่ "Dr." ไป — ระบบเก็บตามที่ไฟล์ให้ ชื่อเต็มที่แสดง = `namePrefix` + ช่องว่าง + `ชื่อสินค้า`
- `รหัสกลุ่ม` / `ชื่อกลุ่ม` = **กลุ่ม E** (ตามชื่ออังกฤษ) ทุกแถวต้องมี
- `รหัสกลุ่มตามกฎ` / `ชื่อกลุ่มตามกฎ` = **กลุ่ม R** (ตามกฎคำระบุสูตร) ว่างทั้งคู่หรือมีทั้งคู่
- รหัสกลุ่มในไฟล์ (E001, R001) ใช้บอกว่าแถวไหนอยู่ด้วยกันเท่านั้น รหัสกลุ่มจริง (`PT000001`) ระบบสร้างเอง
- ไฟล์รูปแบบเก่า (มี `วิธีจัดกลุ่ม`, ไม่มี `namePrefix`) ถูกปฏิเสธ
- แถวใดผิด ทั้งไฟล์ถูกปฏิเสธ พร้อมรายการปัญหาตามเลขแถว

## กฎการนำเข้า (สรุป)

| สถานการณ์ | ผล |
|---|---|
| รายการใหม่ | เพิ่มแถว `active_flag='Y'` |
| ราคาเท่าเดิม | อัปเดตเวลาเห็นล่าสุดเท่านั้น (ชื่อ/รูป/ร้านเปลี่ยนก็ไม่แตะ) |
| ราคาเปลี่ยน | แถวเดิม → `'N'` (ประวัติราคา), เพิ่มแถวใหม่ |
| ราคาย่อ เช่น "1.0พัน" | ไม่เทียบ สถานะ `PRICE_PENDING` (ซ่อนจาก user) รอ `sale_price` จาก feed |
| ไม่อยู่ในไฟล์รอบนี้ | สถานะ NOT_IN_LATEST (ซ่อนจาก user, `active_flag` ยัง `'Y'`) — admin ตรวจแล้วกด "เลิกขาย" ได้ |
| รายการเดิม | อยู่กลุ่มเดิมเสมอ แม้ไฟล์รอบใหม่จัดกลุ่มต่างไป |
| รายการใหม่ | ตามกลุ่มของเพื่อนร่วมกลุ่มในไฟล์ → ชื่อกลุ่มตรงกัน → สร้างกลุ่มใหม่ |
| กลุ่มในไฟล์มีสมาชิกเดิมจาก ≥ 2 กลุ่ม | ไม่รวมกลุ่ม รายการใหม่ได้ REVIEW ให้ admin ตัดสิน |
| รายการใหม่ที่มีกลุ่ม R และกลุ่ม E ของมันในไฟล์มี ≥ 2 แถว | ผ่านอัตโนมัติ (กลุ่ม E ขึ้นเว็บเองได้) ขึ้นเว็บเหมือนรายการทั่วไป |
| รายการใหม่ที่มีกลุ่ม R และกลุ่ม E ของมันในไฟล์มีแถวเดียว | REVIEW (ซ่อนจาก user) จนกว่า admin ยืนยันที่ `/admin/rule-groups` |
| รายการใหม่เข้ากลุ่ม R ที่ admin ยืนยันแล้ว | กลุ่ม R นั้นกลับมาให้ admin ดูอีกรอบ (`merge_reviewed_at` ล้างเป็นว่าง) |
| รายการเดิมกับกลุ่ม R | คงกลุ่ม R เดิมเสมอ และไม่ต้อง REVIEW ซ้ำ |
| กลุ่ม R จับคู่ข้ามรอบ | ชื่อกลุ่มตามกฎที่ normalize แล้วตรงกับกลุ่ม R เดิม → ใช้กลุ่มเดิม ไม่เช่นนั้นสร้างใหม่ |

## REVIEW กลุ่ม R

หน้า `/admin/rule-groups` มี 2 ส่วน แต่ละกลุ่ม R แสดงกลุ่ม E ที่สมาชิกอยู่ พร้อมรายการทั้งหมดของกลุ่ม E นั้น (ทำเครื่องหมายรายการที่อยู่ในกลุ่ม R)
- **รอ REVIEW**: กลุ่ม R ที่มีรายการรอ REVIEW (รายการในกลุ่ม E ที่มีรายการเดียว ยังไม่ขึ้นเว็บ)
- **ยุบรวมได้**: กลุ่ม R ที่ยังไม่เคยยืนยัน ไม่มีรายการรอ แต่สมาชิกอยู่ในกลุ่ม E ≥ 2 กลุ่ม (สินค้าขึ้นเว็บแล้วในกลุ่ม E ของตัวเอง)
- กลุ่ม R ที่สมาชิกอยู่ในกลุ่ม E เดียวและไม่มีรายการรอ ไม่แสดง

- กลุ่ม E ติ๊กไว้ทุกกลุ่มเป็นค่าเริ่ม กด "ยืนยัน" (ปุ่ม disable จนเสร็จ):
  - กลุ่ม E ที่ติ๊กถูก**รวมเข้ากลุ่ม R ทั้งกลุ่ม** เว็บแสดงรายการเหล่านั้นใต้ชื่อกลุ่ม R
  - กลุ่ม E ที่ไม่ติ๊กแสดงในกลุ่ม E เดิม
  - รายการกลุ่ม R ทุกรายการผ่าน REVIEW → `OK` (ถ้ายังไม่มีราคา → `PRICE_PENDING` รอ feed)
  - กลุ่ม R ถูกบันทึกว่ายืนยันแล้ว (`product_type.merge_reviewed_at`) จึงหายจากหน้านี้ จนกว่าการนำเข้าจะเพิ่มรายการใหม่เข้ากลุ่ม R นั้น
- ลิงก์หน้ากลุ่มของกลุ่ม E ที่ถูกรวมแล้ว (`/types/<รหัสกลุ่ม E>`) พาไปหน้ากลุ่ม R (redirect 307)
- กลุ่ม E ที่รวมเข้ากลุ่ม R อื่นแล้วติ๊กไม่ได้; ย้ายรายการเข้ากลุ่ม R ด้วยปุ่ม "ย้ายกลุ่ม" ไม่ได้
- ยืนยันซ้ำได้ผลเดิม และใช้ล็อกเดียวกับการนำเข้า

## ดึง feed อัตโนมัติ

หลังนำเข้าไฟล์สำเร็จ ระบบดึงข้อมูลจาก feed ของ Shopee ให้รายการใหม่ / ราคาเปลี่ยน / ราคาตัวย่อ / รอราคา (ราคาขายที่ขาด, ประเภทร้าน, รูป, stock, rating) ผลแต่ละรอบดูที่ `/admin` และมีปุ่มดึงใหม่เมื่อค้างหรือล้มเหลว

- ตั้งลิงก์ feed ในตัวแปร `FEED_URL` ของ `.env` (เป็นความลับ ห้าม commit) ว่าง = ปิดการดึง
- ใน Docker ส่งต่อให้ backend อัตโนมัติผ่าน `docker-compose.yml`
- feed ใหญ่ราว 4 GB ระบบอ่านแบบ stream และหยุดเมื่อเจอครบ ถ้าโดน 429 ระบบไม่ลองซ้ำเอง (บันทึกผลไว้ให้ admin กดดึงใหม่ภายหลัง)
- รายละเอียดและกฎราคา: [docs/DATA-FLOW.md](docs/DATA-FLOW.md#feed-ของ-shopee-แทน-f2)

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
Mutation test กฎนำเข้าและแกน feed (`FeedPlanner`, `FeedReader`, `FeedClient`) (เกณฑ์ 90%)

```bash
cd frontend && npm run lint && npm run build
```

### เทสต์ frontend

ไฟล์เทสต์วางคู่กับไฟล์ต้นทาง (`X.tsx` อยู่ข้าง `X.test.tsx`) แบ่งเป็น 4 ชั้น:

| ชั้น | สิ่งที่เทสต์ | คำสั่ง |
|---|---|---|
| 1. ฟังก์ชันล้วน | `src/lib/**`, `src/proxy.ts` | `npm test` |
| 2. คอมโพเนนต์ | `src/components/**` (React Testing Library บน jsdom) | `npm test` |
| 3. Server Actions และ client ที่เรียก backend | `src/app/admin/actions.ts`, `src/lib/api/**` | `npm test` |
| 4. End to end | `e2e/*.spec.ts` (Playwright กับระบบจริงใน Docker) | `npm run e2e` |

```bash
cd frontend && npm test
```
ชั้น 1–3 (Vitest) ไม่ต้องใช้ Docker หรือ backend ส่วน `npm run test:watch` จะรันซ้ำทุกครั้งที่แก้ไฟล์

```bash
cd frontend && npm run mutation
```
Mutation test ด้วย Stryker (เทียบได้กับ PIT ของ backend) ใช้เวลาราว 10–20 นาที รายงานอยู่ที่ `frontend/reports/mutation/index.html`

**E2E** รันบน compose project แยกชื่อ `snapshop-e2e` ซึ่งมีฐานข้อมูลของตัวเอง
1. คัดลอก `.env.test.example` เป็น `.env.test` ที่ root แล้วเปลี่ยนรหัสผ่าน ห้ามใช้รหัส admin จริงหรือลิงก์ feed จริง และ `.env.test` ไม่ถูก commit
2. ปิด stack dev ก่อน เพราะใช้พอร์ต 3000/8080/5432 เหมือนกัน (`docker compose stop`, `next dev` และ backend ที่รันนอก Docker)
3. รันตามลำดับ:
   ```bash
   cd frontend && npm run e2e:down && npm run e2e:up && npm run e2e
   ```
   `e2e:down` ลบฐานข้อมูลของ stack ทดสอบเพื่อให้เริ่มจากข้อมูลว่างทุกครั้ง จากนั้นเทสต์จะนำเข้าไฟล์ `e2e/fixtures/e2e-import.csv` เอง
   - โฟลเดอร์ inbox ของ stack ทดสอบมาจาก `INBOX_DIR` (`frontend/e2e/.inbox`) จึงไม่แตะ `inbox/` จริง
   - ถ้าครั้งก่อนเหลือไฟล์ค้างอยู่ ให้ลบ `frontend/e2e/.inbox` ทิ้งก่อนรัน
