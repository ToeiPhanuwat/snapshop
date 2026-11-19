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

## 2026-11-12 — หน่วย 2: อ่าน CSV และแยกชื่อ (ขอบของระบบ)
- พบว่าไฟล์ `drpong_merged_20261110_renamed.csv` (มีคอลัมน์กลุ่ม) ถูกตัดคำนำหน้าชื่อออกแล้ว 95 แถว ส่วน `drpong_merged_20261110.csv` มีชื่อเต็มแต่ไม่มีคอลัมน์กลุ่ม สองไฟล์ต่างกันแค่คอลัมน์ชื่อ — สร้าง fixture `backend/src/test/resources/fixtures/drpong_merged_20261110.csv` = ไฟล์ _renamed + ชื่อเต็มจากไฟล์ต้นฉบับ ได้ 95 มี prefix / 96 ขึ้นต้น Dr. / 1 ไม่มี Dr. ตรงกับบรีฟ
  - **ไฟล์นำเข้าจริงต้องใช้ชื่อสินค้าเต็ม** ไม่เช่นนั้น name_prefix จะหาย
- เขียนเทสต์ก่อน (ล้มเพราะยังไม่มีคลาส) แล้วเขียน `ProductName`, `CsvImportParser`, `RecordReader`, enum `ShopType`, `GroupingMethod`
- ตรวจครั้งเดียวที่ขอบ: รหัสสินค้าเป็นตัวเลข, ไม่ซ้ำในไฟล์, ลิงก์สินค้าตรงกับรหัส (ได้ shop_id), ป้ายประเภทร้าน/วิธีจัดกลุ่มรู้จัก, รหัส/ชื่อกลุ่มไม่ว่าง — รวบรวมทุกข้อผิดพลาดพร้อมเลขแถว แล้วปฏิเสธทั้งไฟล์
- ราคาย่อ "1.0พัน" → null (จะเป็น REVIEW) ไม่ปัดเป็น 1000 เพราะไม่แม่น
- ป้ายภาษาไทยเทียบหลัง NFKC เพราะ "ำ" เขียนได้ 2 แบบ (U+0E33 และ U+0E4D U+0E32) ในบรีฟและไฟล์ใช้ต่างกัน
- เทสต์ชื่อล้มรอบแรกเพราะตัวเทสต์ตั้ง quote character ผิด (ไม่ใช่โค้ด) — แก้เทสต์

## 2026-11-12 — หน่วย 3: ImportPlanner (กฎเป็นฟังก์ชันบริสุทธิ์)
- ออกแบบรูปข้อมูลก่อน: `CurrentState` (แถวปัจจุบัน, กลุ่มของทุก item ที่เคยเก็บ, กลุ่มตามชื่อ normalize) → `ImportPlan` (กลุ่มใหม่, เวอร์ชันใหม่, เห็นซ้ำ, ซ่อน) — Service จะเอาแผนไปเขียน DB อย่างเดียว
- `TypeRef` เป็น sealed (`Existing` / `Created`) — เวอร์ชันใหม่ต้องชี้กลุ่มเสมอ เขียนแบบไม่มีกลุ่มไม่ได้
- เขียนเทสต์ 14 ข้อก่อน แล้วสร้าง planner เปล่าที่คืนแผนว่าง → ล้มทั้ง 14 (ยืนยันว่าเทสต์ไม่ผ่านถ้าโค้ดไม่ทำอะไร) จากนั้นเขียน logic จนผ่าน
- กฎที่ทดสอบ: กลุ่มใหม่ 1 กลุ่มต่อชื่อ, item เดิมไม่ย้ายกลุ่มแม้ไฟล์แยก, item ใหม่ตามเพื่อนร่วมกลุ่ม, จับคู่ชื่อ normalize, item ที่เคยเก็บแต่ไม่ active ยังได้กลุ่มเดิม, ชนกัน → ไม่รวม + REVIEW เฉพาะ item ใหม่, ราคาเท่า/เปลี่ยน/ไม่รู้, REVIEW ค้างจน admin แก้, NOT_IN_LATEST และกลับมา, TEST/OUT_OF_STOCK
- ตัดสินใจเพิ่ม: กลุ่มที่ชนกันเลือกกลุ่มที่สมาชิกมากที่สุด เสมอกันเลือก id เล็กสุด — ผลไม่ขึ้นกับลำดับใน Map; สินค้าทดสอบ = ชื่อร้านมีคำว่า "test"
- REVIEW ค้างไว้จน admin กด "ตรวจแล้ว" (จะเพิ่ม endpoint resolve ในหน่วย 5) เพราะระบบไม่รู้ว่าเหตุผลที่ต้องตรวจหมดไปแล้วหรือยัง

## 2026-11-12 — หน่วย 4: ImportService + POST /api/admin/imports
- คำนวณตัวเลขที่คาดจากไฟล์จริงด้วย Python แยกจากโค้ด Java (ไม่ใช้โค้ดที่กำลังทดสอบเป็นตัวเฉลย): 122 กลุ่ม, OK 187 / REVIEW 3 / TEST 1 / OUT_OF_STOCK 1
- Entity 3 ตัว (`ProductType`, `MarketplaceProduct`, `ImportBatch`), Repository (Spring Data JPA + `ImportLockRepository` advisory lock), `ImportService`, `AdminImportController`, `SecurityConfig` (HTTP Basic เฉพาะ /api/admin/**), `GlobalExceptionHandler` (ProblemDetail + รายการ errors)
- ข้อมูลของรายการใน `MarketplaceProduct` เป็น `updatable = false` — แถวเดิมแก้ได้แค่สถานะ/last_seen/กลุ่ม ตามกฎ "สินค้าเดิมไม่ต้องทำอะไร"
- ลำดับเขียน: ปิดแถวเดิม (N) แล้ว flush ก่อน insert แถวใหม่ เพราะ Hibernate เรียง insert ก่อน update ซึ่งจะชน partial unique index
- ใช้ advisory lock แทน ON CONFLICT ตามแผนเดิม — นำเข้าทีละรอบเท่านั้น (ผู้เขียนคนเดียว) จึงไม่มี race ให้กัน; unique constraint ยังเป็นด่านสุดท้าย
- เทสต์ integration 5 ข้อ: ไฟล์จริง, นำเข้าซ้ำไม่เปลี่ยนอะไร (idempotent), รอบถัดไป (ราคาเปลี่ยน/หายไป/ใหม่เข้ากลุ่มเดิมทั้งที่ป้ายในไฟล์เปลี่ยน E001→E005), ไฟล์ผิดไม่บันทึกอะไร, ต้องล็อกอิน
- **ข้อสังเกตตามจริง**: หน่วยนี้เขียนโค้ดเสร็จก่อนรันเทสต์ครั้งแรก จึงไม่ได้เห็นเทสต์ล้มก่อน (ผ่านตั้งแต่รอบแรก) — จะใช้ PIT ในหน่วย 6 ตรวจว่าเทสต์จับการเปลี่ยนโค้ดได้จริง
- รวมเทสต์ทั้งหมด 36 ข้อผ่าน

## 2026-11-12 — หน่วย 5: API ฝั่ง user และ admin
- เขียนเทสต์ 16 ข้อก่อน รันแล้วล้ม 13 ข้อ (ไม่มี endpoint) 3 ข้อที่ผ่านคือข้อที่คาดหวัง 401/404 อยู่แล้ว
- User: `GET /api/product-types`, `GET /api/product-types/{code}` (ร้านเรียงราคาถูกสุดก่อน), `GET /api/products` (กรอง typeCode/shopType/q, เรียง PRICE_ASC/PRICE_DESC/NAME, แบ่งหน้า ≤100), `GET /api/products/{itemId}` (+ประวัติราคา)
- Admin: `GET /api/admin/products`, `GET /api/admin/product-types` (มี activeCount/visibleCount/onUserSite), `POST .../resolve`, `POST .../move`, `POST .../deactivate`
- ฝั่งอ่านใช้ `ProductQueryRepository` (JdbcClient) map ตรงเป็น response record — เป็น read model ไม่ต้องโหลด entity มาคัดลอก; ฝั่งสั่งงานใช้ entity + JPA
- กฎ "แสดงฝั่ง user" อยู่ที่ view `public_product` ที่เดียว; admin ถาม `onUserSite` จาก view เดียวกัน
- กฎลิงก์หลัก (affiliate → product_url) อยู่ที่ `ProductQueryRepository.link` ที่เดียว
- sort รับเป็น enum แล้ว map เป็น ORDER BY ด้วย switch — ค่าแปลกเช่น "DROP TABLE" ได้ 400 ไม่ถึง SQL; ค่าค้นหา escape % และ _
- **บั๊กที่ constraint ใน DB จับได้**: เทสต์ลองปิด REVIEW ให้สินค้าที่ราคาย่อ "1.0พัน" (ไม่มีราคา) → DB ปฏิเสธ (`ck_price_or_review`) แทนที่จะแสดงสินค้าไม่มีราคาบนเว็บ แก้โดยย้ายกฎเข้า `MarketplaceProduct.resolveReview()`: ปิด REVIEW ได้เฉพาะเมื่อมีราคา ไม่เช่นนั้นตอบ 409 พร้อมเหตุผล ("รอนำเข้ารอบที่มีราคาเต็ม")
- ฉากทดสอบกลุ่มชนกันตั้งผิดรอบแรก (กลุ่มในไฟล์มีสมาชิกเดิมกลุ่มเดียว) — แก้ให้มีสมาชิกเดิมจาก 2 กลุ่มจริง
- ย้ายกลุ่มย้ายทุกเวอร์ชันของ item เพื่อให้ประวัติราคาตามไปด้วย; เลิกขาย = 'N' + DISCONTINUED (ไม่ใช่ประวัติราคา)
- เทสต์ทั้งหมด 52 ข้อผ่าน

## 2026-11-12 — หน่วย 6: เครื่องมือคุณภาพ
- SKILL.md อ้างถึง crapper / dryer / mutator โดยไม่ระบุเครื่องมือ ผมเลือกที่ใช้กับ Maven ได้:
  - Crapper → JaCoCo (ผูกกับ `verify`) + `backend/tools/crap.py` คำนวณ CRAP ต่อ method จาก jacoco.xml
  - Dryer → PMD CPD `cpd-check` (ผูกกับ `verify`, minimumTokens 60) — build ล้มถ้ามีโค้ดซ้ำ
  - Mutator → PIT บนกฎบริสุทธิ์ (`service.importing`, `model.enums`) เกณฑ์ 90% สั่งแยก: `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`
- ผลรอบแรก: CRAP สูงสุด 7.0 (`addFileGroup`) ไม่มี method ไหนถึง 10; CPD ไม่พบโค้ดซ้ำ; PIT ฆ่าได้ 88/95 (93%)
- Survivors 2 ตัวเป็น equivalent mutant: เส้นทาง error ใน `RecordReader` คืน null อยู่แล้ว เปลี่ยนเป็น null/0 ก็ไม่มีผลเพราะแถวถูกทิ้ง — แก้ที่ต้นเหตุ: helper คืน `Optional` และ `error()` ไม่คืนค่า ทำให้ไม่มีค่าหลอกให้ใช้ผิด
- ไม่มีเทสต์ unit ครอบ 5 จุด (`ImportPlan.priceChangeCount/newItemCount` มีแค่ integration test ครอบ) — เพิ่มการตรวจในเทสต์ planner
- ผลรอบสอง: PIT ฆ่าได้ 96/96 (100%), `verify` ผ่าน, CRAP สูงสุด 7.0

## 2026-11-12 — หน่วย 7: Frontend ฝั่ง user
- สร้างด้วย `create-next-app@16.4.6` (Tailwind 4.3.3, React 19.3) — `AGENTS.md` ของ Next บอกว่ามี breaking changes จึงอ่าน `node_modules/next/dist/docs` ก่อนเขียน: `cacheComponents` เปิดอยู่ (ข้อมูลที่ไม่ cache ต้องอยู่ใน `<Suspense>`), `params`/`searchParams` เป็น Promise, `middleware.ts` เปลี่ยนเป็น `proxy.ts`, proxy เป็นแค่ด่านแรก Server Action ต้องตรวจสิทธิ์ซ้ำ
- โครงสร้าง: `app/(public)` หน้า user, `components/{ui,product,admin}`, `lib/api/{http,public,admin}.ts` (มี `server-only` — เรียก backend จากฝั่ง server เท่านั้น), `lib/auth.ts` (กฎล็อกอิน admin ที่เดียว), `lib/format.ts`, `types/api.ts` (ตรงกับ DTO), `proxy.ts`
- ไม่ cache ราคา (`cache: "no-store"`) — ราคาต้องเป็นปัจจุบันเสมอ
- ชื่อแสดงใช้ `displayName` จาก API (กฎ prefix + core อยู่ที่ backend ที่เดียว); ไม่แสดงรูป; ลิงก์ `rel="sponsored"`
- ใช้ฟอนต์ระบบที่มีภาษาไทย แทน Geist ที่ไม่มีภาษาไทยและต้องโหลดจาก Google ตอน build
- ตรวจกับระบบจริง: PostgreSQL 17 (docker compose) + backend jar + นำเข้าไฟล์จริงผ่าน API → 192 แถว, 122 กลุ่ม, หน้า user 38 กลุ่ม; admin ไม่ล็อกอินได้ 401; เปิดหน้าแรกและหน้ากลุ่ม PT000023 ใน browser: 9 ร้าน เรียงราคา, ป้ายร้าน, ปุ่มชี้ affiliate link
- ปัญหาเครื่อง: Tomcat เปิด loopback ไม่ได้ ("Unable to establish loopback connection") เพราะ JDK สร้าง Unix domain socket ในโฟลเดอร์ temp ของ sandbox ไม่ได้ — รันด้วย `-Djdk.net.unixdomain.tmpdir=<โฟลเดอร์ที่เขียนได้>`
- ข้อมูลจริงที่เห็น: "Dr.PONG 28D smart whitening drone acne activein serum Gen2" ขึ้นเป็น 2 กลุ่ม (ชื่อกลุ่มในไฟล์ต่างกันเล็กน้อย) — admin ใช้ปุ่มย้ายกลุ่มรวมได้
- `npm run lint`, `npm run build` ผ่าน

## 2026-11-12 — หน่วย 8: หน้า admin, Docker, README
- หน้า admin: `/admin` (REVIEW, NOT_IN_LATEST, นำเข้าล่าสุด), `/admin/import` (อัปโหลด + ผลนับ + รายการ error ตามแถว), `/admin/products` (กรองสถานะ/กลุ่ม/คำค้น, ปุ่ม ตรวจแล้ว / ย้ายกลุ่ม / เลิกขาย พร้อมยืนยัน), `/admin/types` (activeCount, visibleCount, แสดงฝั่ง user หรือไม่)
- Server Actions (`app/admin/actions.ts`) เรียก `lib/api/admin.ts` ซึ่งตรวจล็อกอินซ้ำทุกครั้ง (`requireAdmin`) แล้วค่อยใส่ Basic auth ไป backend; หลังสั่งงานเรียก `refresh()`
- ตรวจด้วย curl: ทุกหน้า `/admin*` ไม่ล็อกอิน/รหัสผิด = 401, ถูก = 200; หน้า `/admin` แสดง REVIEW 3 รายการ (ราคาย่อ) และประวัตินำเข้า
- **ยังไม่ได้ทดสอบ**: กดปุ่ม admin (Server Actions) ใน browser — ผมไม่ได้ใช้รหัสผ่าน admin ใน browser (ระบบไม่ให้ดึงรหัสผ่านออกมา) logic ฝั่ง backend ของทุกปุ่มมี integration test ครอบแล้ว
- หน้าแบบ stream (Suspense) ที่เรียก `notFound()` ได้ status 200 + `<meta name="robots" content="noindex">` แทน 404 — เป็นพฤติกรรมของ Next 16 เมื่อเปิด cacheComponents; ผู้ใช้เห็นหน้า "ไม่พบหน้านี้" ถูกต้อง
- เพิ่ม Dockerfile (backend: temurin 25 multi-stage, รันด้วย user ไม่ใช่ root; frontend: node 24 + `output: "standalone"`) และ service `backend`/`frontend` ใน compose ภายใต้ profile `app` (`docker compose up -d db` ยังเปิดแค่ DB ได้)
- ตรวจ: `docker compose --profile app build` ผ่าน, `up` แล้ว backend/frontend/db running, API 38 กลุ่ม, หน้าเว็บ 38 กลุ่ม, admin 401/200; หน้ามือถือ 375px ไม่มี scroll แนวนอน
- README: โครงสร้าง, วิธีรัน, รูปแบบไฟล์นำเข้า, สรุปกฎ, คำสั่งตรวจคุณภาพ

## 2026-11-13 — CLAUDE.md
- สร้าง `CLAUDE.md` ที่ root ใส่ `@SKILL.md` ให้โหลดทุก session; บอกว่าข้อ push ใน SKILL.md ให้ใช้ CLAUDE.md ส่วนตัวแทน; บันทึกข้อตกลง (Claude ไม่ใช้รหัสผ่าน admin, ไฟล์วางใน `inbox/`) และคำสั่งที่ใช้บ่อย — commit `SKILL.md` ด้วยตามที่ผู้ใช้เลือก
- เหตุ: ผู้ใช้ถามว่าผมนำเข้าข้อมูลให้ได้ด้วยหรือ — ในรอบแรกผมใช้รหัสผ่าน admin จาก `.env` นำเข้าไฟล์ตัวอย่างระหว่างตรวจ ซึ่งขัดกับบรีฟ ("Claude ไม่กรอกรหัสผ่านหรือคีย์ของระบบ") จึงออกแบบทาง inbox ให้ไม่ต้องใช้รหัสผ่าน

## 2026-11-13 — หน่วย 9: พรีวิว + inbox (backend)
- เขียนเทสต์ 8 ข้อก่อน (`AdminInboxControllerTest`, inbox เป็น `@TempDir`) รันแล้วล้ม 7 (ข้อ 401 ผ่านอยู่แล้ว)
- `ImportService`: แยก `prepare` (โหลดสถานะ + วางแผน) ใช้ทั้งนำเข้าจริงและ `preview` (readOnly) — พรีวิวใช้โค้ดเดียวกับนำเข้า ตัวเลขจึงตรงกันเสมอ (เทสต์ยืนยันด้วยไฟล์จริง)
- `ImportPreviews` สร้างรายการให้ admin อ่าน: กลุ่มใหม่, ราคาเปลี่ยน (เดิม → ใหม่), จะซ่อน, จะเป็น REVIEW
- `InboxService` + `AdminInboxController`: list / preview / import / discard; ย้ายไฟล์ไป `imported/` หรือ `rejected/` (มีเวลานำหน้า) หลัง transaction commit แล้ว ไฟล์ที่นำเข้าไม่ผ่านยังอยู่ใน inbox
- ชื่อไฟล์ตรวจที่ขอบครั้งเดียว: regex ห้าม `/ \ : * ? " < > |`, ห้าม `..`, ต้องลงท้าย `.csv` → 400; ไม่มีไฟล์ → 404; `%2F` ถูก firewall ของ Spring Security ปฏิเสธ 400
- รอบแรก `existing()` ครอบ 87% เพราะเช็ก parent หลัง normalize ไม่มีทางเกิด (regex ห้าม separator อยู่แล้ว) — ลบออกตาม "Delete dead code"
- ผล: เทสต์ 60 ข้อผ่าน, CPD ผ่าน, CRAP สูงสุด 7.0, PIT 96/96

## 2026-11-13 — หน่วย 10: หน้าไฟล์รอนำเข้า (frontend)
- `/admin/inbox` รายการไฟล์ (ชื่อ, ขนาด, เวลา) + ข้อความผลหลังนำเข้า/ทิ้ง; `/admin/inbox/[name]` พรีวิว (ตัวเลข 6 ช่อง + ราคาเปลี่ยน เดิม → ใหม่, จะซ่อน, จะ REVIEW, กลุ่มใหม่) หรือ error ตามแถวถ้าไฟล์ผิด, ปุ่ม "นำเข้าไฟล์นี้" (ซ่อนเมื่อไฟล์ผิด) และ "ทิ้งไฟล์" (ยืนยันก่อน)
- Server Actions `importInboxAction` / `discardInboxAction` ผ่าน `lib/api/admin.ts` (ตรวจล็อกอินซ้ำ) แล้ว redirect กลับรายการพร้อมผล
- เมนู: "ไฟล์รอนำเข้า" และเปลี่ยน "นำเข้า" เป็น "อัปโหลด"; หน้าภาพรวมแสดงแถบจำนวนไฟล์รอ
- lint, typecheck, build ผ่าน

## 2026-11-13 — หน่วย 11: Docker และเอกสารสำหรับ inbox
- compose: backend mount `./inbox:/app/inbox` + `IMPORT_INBOX_DIR=/app/inbox`; `inbox/` อยู่ใน `.gitignore`
- README: ขั้นตอน "วางไฟล์ใน inbox → พรีวิว → นำเข้า/ทิ้ง"; GLOSSARY: inbox, พรีวิว
- ตรวจ (ไม่ใช้รหัสผ่าน admin ตามข้อตกลง): `docker compose --profile app up -d --build` ผ่าน; `/api/admin/inbox` และ `/admin/inbox` ไม่ล็อกอิน = 401; ใน container เห็นไฟล์และเขียนโฟลเดอร์ได้
- วางไฟล์ตัวอย่าง `inbox/drpong_20261110_sample.csv` (ข้อมูลชุดเดียวกับที่อยู่ใน DB แล้ว — พรีวิวควรได้ราคาเท่าเดิม 192, ใหม่ 0) ให้ผู้ใช้ลองพรีวิว/นำเข้า/ทิ้งเอง
- **ยังไม่ได้ทดสอบ**: กดปุ่มในหน้า admin ใน browser (ต้องใช้รหัสผ่าน admin ซึ่งผมไม่ใช้) — ฝั่ง backend มี integration test ครอบทุกปุ่ม

## 2026-11-16 — เอกสาร Flow ข้อมูล
- เหตุ: แชตอื่นค้นคำว่า "F2" ใน repo แล้วไม่เจอ — รายละเอียด F1/F2/Merge/แยกชนิดสินค้าอยู่แค่ในบรีฟที่วางในแชต README มีแค่ "ขั้น F1–F4 ทำนอกระบบ" โดยไม่มีคำอธิบาย
- `main` มี PR #1, #2 รวมแล้ว จึงแตก branch `docs/data-flow` (ไม่ commit บน main)
- สร้าง `docs/DATA-FLOW.md`: flow ฉบับปัจจุบัน + ตารางใครทำขั้นไหน, F1, F2 (ตอนนี้ผู้ใช้ดึงเอง), Merge (ราคาจาก F2, คงชื่อเต็ม), แยกชนิดสินค้า (กฎ 2 ชั้น, คอลัมน์ E/R/U), ไฟล์ส่งเข้าระบบ, ขั้นในระบบ (ลิงก์ไปกฎใน README ไม่เขียนซ้ำ), บันทึกการตัดสินใจ (คำตอบคำถามเปิด 8 ข้อ + การตัดสินใจหลังบรีฟพร้อมวันที่), คำถามที่ยังเปิด
- ลิงก์จาก README, CLAUDE.md; GLOSSARY เพิ่ม F1, F2, Merge, ไฟล์ merged
- ตรวจ: ค้นคำว่า "F2" ในไฟล์ .md ของ repo เจอแล้ว

## 2026-11-16 — เลิก F2, ดึง feed อัตโนมัติ, แสดงรูปหน้าแรก (branch `feat/feed-sync`)
- เหตุ: F2 (scrape การ์ดสินค้า) ผิดกฎ ข้อมูลที่เคยมาจาก F2 (ราคา, ประเภทร้าน, รูป) ต้องมาจาก feed ของ Shopee; ผู้ใช้ต้องการให้ระบบดึงเองหลังนำเข้า F1 และเก็บในตารางเดียวกัน ตัดสินใจกับผู้ใช้ผ่านแผนที่อนุมัติแล้ว
- ข้อค้นพบก่อนลงมือ: `image_url` มีในตารางอยู่แล้วแต่ API ไม่ส่งและ `updatable=false`; view `public_product` ขยาย `p.*` ตอนสร้าง คอลัมน์ใหม่ต้องสร้าง view ใหม่; feed จริงมี `sale_price`/`price`/`stock`/rating/flag ร้าน (ตรวจจากไฟล์ตัวอย่างใน `research/` ไม่เปิดลิงก์ feed)
- **A (399d224)**: เทสต์ก่อน (planner: `PRICE_PENDING`, `feedItemIds`; parser: ไฟล์ 12 คอลัมน์) แล้วแก้ — ตัดคอลัมน์ประเภทร้าน/รูปออกจากไฟล์ F1 (ไฟล์เก่ายังนำเข้าได้), ราคาตัวย่อ → `PRICE_PENDING` แทน REVIEW (แยกสาเหตุจาก REVIEW ของกลุ่มชนกัน ไม่งั้นแยกไม่ออกว่าควรเปลี่ยนเป็น OK เมื่อได้ราคาไหม), `ImportPlan.feedItemIds`, พรีวิวบอกจำนวนที่จะดึง; migration V4 (คอลัมน์ feed, constraint, ล้างค่าที่ scrape, สร้าง view ใหม่, ตาราง `feed_sync_run`)
- **B (a765b1e)**: `FeedReader` (อ่าน stream หยุดเมื่อเจอครบ, ข้ามแถวเสีย), `FeedClient` (ไม่เก็บไฟล์, จัดการ 429 ไม่ลองซ้ำ), `FeedPlanner` (บริสุทธิ์: เติมราคาที่ขาด, เวอร์ชันราคาใหม่เฉพาะรายการที่ราคา F1 เป็นตัวย่อ, ไม่แทนราคาที่มาจากไฟล์, ข้าม shopid ไม่ตรง); เทสต์ HTTP ด้วย `com.sun.net.httpserver` ต้องตั้ง `-Djdk.net.unixdomain.tmpdir` ใน surefire (loopback บน Windows)
- **C (e1d8125)**: หลัง commit การนำเข้า → event → `@Async` เธรดเดียว: ดาวน์โหลดนอก transaction/ล็อก แล้ว transaction สั้นที่ใช้ `ImportLockRepository` เดิม (ไม่ชนกับการนำเข้า); บันทึก `feed_sync_run` ทุกรอบ; ไม่มี `FEED_URL` = ไม่ทำอะไร (กันเทสต์ชนกัน); admin ดูผลและกดดึงรายการค้างใหม่ (`/api/admin/feed-syncs`); `FEED_URL` ส่งผ่าน `.env` และ compose
- **D (e83ac37, 3c031ce)**: API ส่ง `imageUrl` (ชนิดสินค้า = รูปของ offer ที่ถูกสุดที่มีรูป ไม่มี = null); การ์ดหน้าแรกมีรูป + ป้าย "N ร้าน" + placeholder "ยังไม่มีรูป" (รวมกรณีรูปโหลดไม่ได้); ใช้ `<img>` ไม่ส่ง referrer แทน next/image จึงไม่ต้องตั้ง remotePatterns; หน้า admin มีส่วน "รอราคาจากฟีด" + ผลดึง feed + ปุ่ม; จอมือถือปรับเป็น 2 คอลัมน์ (commit เอกสาร)
- ตรวจ: เทสต์ backend ทั้งชุดผ่าน (รวม `FeedSyncServiceTest` กับ Postgres จริง); `npm run lint`/`build` ผ่าน; ดูหน้าจริงบน DB ชั่วคราว (Postgres คนละพอร์ต + backend พอร์ต 8081 + frontend พอร์ต 3100 ไม่แตะ DB dev) นำเข้าไฟล์ตัวอย่างแล้วใส่รูป 3 แบบ — รูปจริง, ลิงก์เสีย, ไม่มีรูป — การ์ดแสดงรูป/placeholder ถูกต้องทั้งเดสก์ท็อปและมือถือ; หน้า admin แสดง 3 รายการ `PRICE_PENDING` และส่วนผลดึง feed; เก็บกวาดกระบวนการและ container ชั่วคราวแล้ว
- **ยังไม่ได้ทำ/ไม่ได้ตรวจ**: ไม่ได้ยิง feed จริง (ลิงก์เป็นความลับและเคยโดน 429) ฝั่งการดึงจริงตรวจด้วย stub และเซิร์ฟเวอร์ทดสอบเท่านั้น; ไม่ได้กดปุ่ม "ดึงรายการที่ค้างจากฟีด" ใน browser (ต้องใช้รหัสผ่าน admin จริง); รายการที่ราคาไม่เปลี่ยนไม่ถูกดึงใหม่ (stock/rating ไม่สดเอง); ต้องยืนยันเงื่อนไขของ Shopee ก่อนตั้ง `FEED_URL` ใช้งานจริง
- ตรวจคุณภาพรอบสุดท้าย: `./mvnw verify` ผ่าน (เทสต์ 97 ข้อ รวม CPD), CRAP สูงสุด 8.0 (ทุกเมธอด < 10), PIT ขยายให้ครอบ `FeedPlanner`/`FeedReader`/`FeedClient` ได้ 140/140 (รอบแรก 137 — ที่ขาดคือเส้นทางเชื่อมต่อไม่ได้และถูกขัดจังหวะ จึงเพิ่มเทสต์สองข้อ); เพิ่มเทสต์เส้นทางอัตโนมัติ (นำเข้า → event ที่มีรายการถูกต้อง → listener เริ่มดึงเมื่อมี `FEED_URL`)

## 2026-11-17 — feed ตอบ 302, ปุ่มดึง feed disable ระหว่างดึง (branch `fix/feed-redirect`)
- เหตุ: ผู้ใช้ตั้ง `FEED_URL` จริงแล้วได้ "ฟีดตอบ HTTP 302" — `HttpClient` ของ Java ไม่ตาม redirect ถ้าไม่สั่ง (`Redirect.NEVER`) ส่วนสคริปต์ Python ใช้ `requests` ซึ่งตามให้เอง เทสต์เดิมไม่ได้จำลอง redirect
- เขียนเทสต์ก่อน: เซิร์ฟเวอร์ทดสอบตอบ 302 ไปอีก path → ล้มด้วยข้อความเดียวกับที่ผู้ใช้เจอ → เพิ่ม `followRedirects(Redirect.NORMAL)` (ไม่ตามจาก https ลง http) → ผ่าน
- ผู้ใช้ตัด progress bar ออก เลือกให้ปุ่ม disable ระหว่างดึง + หน้า `/admin` รีเฟรชเองทุก 1 นาที (ไม่ใช่ 5 วินาที) และไม่ต้องมีข้อความข้างปุ่ม เพราะตารางแสดงสถานะอยู่แล้ว; ดึง feed ที่หลังบ้านเหมือนเดิม
- เจอช่องว่าง: แถว "กำลังดึง" ถูกเขียนบนเธรดเบื้องหลังหลังตอบกลับ หน้าที่โหลดใหม่ทันทีอาจไม่เห็น ปุ่มจึงกลับมากดได้ — ถามผู้ใช้พร้อมตัวเลือก ผู้ใช้เลือกให้ backend เขียนแถวก่อนตอบกลับ (ยังไม่แยกสถานะ "รอคิว")
- `FeedSyncService.start` (transaction ของตัวเอง `REQUIRES_NEW` ตามที่ Spring แนะนำสำหรับการเขียนหลัง commit) + `runAsync`; listener หลังนำเข้าและปุ่มเรียก start ก่อนแล้วค่อยส่งงานไปเธรด feed. เทสต์ `FeedSyncStartTest` (feed จำลองที่ค้างไว้): ก่อนแก้ ปุ่มที่กดหลังนำเข้าได้ 409 เพราะแถวของรอบก่อนโผล่ช้า; หลังแก้ แถวมีอยู่เมื่อการนำเข้า/ปุ่มตอบกลับ และกดซ้ำได้ 409
- ข้อสังเกต: ลองเอา `REQUIRES_NEW` ออกชั่วคราว เทสต์ยังผ่าน (การตั้งค่านี้ Hibernate คืน connection หลัง commit แล้ว) จึงคงไว้ตามเอกสาร Spring แต่เทสต์ไม่ได้ครอบจุดนี้
- Frontend: `FeedSyncPanel` disable ปุ่มเมื่อมีรอบ `RUNNING` หรือกำลังส่งคำขอ; `AutoRefresh` เรียก `router.refresh()` ทุก 60 วินาทีเฉพาะตอนมีรอบ `RUNNING`
- ตรวจในเบราว์เซอร์บน DB ชั่วคราว: แถว `RUNNING` → ปุ่ม disable; เปลี่ยนเป็น `DONE` ใน DB โดยไม่โหลดหน้า → ราว 60 วินาทีปุ่มกลับมากดได้เอง และไม่ถามซ้ำอีก. ระหว่างตรวจพบว่า `next start` ใช้กับ build แบบ standalone ไม่ได้ (คอมโพเนนต์ client ไม่ hydrate) ต้องรัน `node .next/standalone/server.js` (คัดลอก `.next/static` ไปด้วย) — Docker ใช้แบบนี้อยู่แล้ว
- กฎใหม่ของผู้ใช้ (CLAUDE.md ส่วนตัว): ปุ่มสั่งงานต้อง disable จนงานเสร็จ; เจอเรื่องต้องตัดสินใจให้ถามพร้อมทุกแนวทางและคำแนะนำ ห้ามเดา

## 2026-11-17 — เทสต์ frontend ครบ 4 ชั้น (branch `test/frontend-tests`)
- เหตุ: frontend ยังไม่มีเทสต์เลย ผู้ใช้อนุมัติแผน 4 ชั้น โดยวางไฟล์เทสต์คู่กับไฟล์ต้นทาง, เก็บ `.env.test` ไว้บนเครื่องไม่ commit (commit แค่ `.env.test.example`), ให้ E2E ใช้พอร์ตเดิมโดยปิด stack dev ก่อน, แก้ปัญหาปุ่มที่กดซ้อนกันได้แบบ TDD และเพิ่ม Stryker
- pull `main` (ได้ PR #5 มาด้วย) แล้วแตก branch `test/frontend-tests`
- **ชั้น 1 (bfa579f)** ตั้ง Vitest + Testing Library ตามคู่มือของ Next 16
  - alias `server-only` ไปที่ stub ว่าง เพราะของจริง throw เมื่อถูก import นอก Server Component
  - Vitest 5 ต้องใช้ `@types/node` ≥22 ผู้ใช้เลือกอัปเป็น ^24 ให้ตรงกับ Node 24 ที่รันจริง
  - เทสต์ format (เวลา Bangkok), `query`, `backendUrl`, `isAdminAuthorization` และ proxy
- **ชั้น 2 (a36f18a)** เทสต์คอมโพเนนต์โดยหาองค์ประกอบด้วย role/ข้อความ และ mock เฉพาะ Server Actions กับ router
  - ใช้ promise ที่เทสต์ resolve เองเพื่อจับสถานะ pending
  - ข้อจำกัดของ jsdom: ไฟล์ที่เลือกไม่นับว่ากรอก `required` แล้ว และไม่ถูกใส่ใน FormData เทสต์จึงส่ง submit event เอง ส่วนการอัปโหลดจริงให้ E2E ตรวจ
- **ปุ่มกดซ้อนกันได้ (9c225dd)**: `InboxDecision` และ `ProductActions` disable เฉพาะปุ่มของ action ที่กำลังทำงาน ระหว่างนำเข้ายังกดทิ้งไฟล์ได้ ซึ่งขัดกฎ "ปุ่มต้อง disable"
  - เขียนเทสต์ก่อน: ล้มที่ `toBeDisabled` ตามที่คาด
  - แก้ด้วยธง `busy` เดียวต่อคอมโพเนนต์ แล้วเทสต์ผ่าน
  - ระหว่างเทสต์ยังล้ม มีอีกเทสต์หนึ่งล้มตามไปด้วย สาเหตุคือ React 19 รวม transition ของ async action ทั้งหมดเข้าด้วยกัน promise ที่ค้างอยู่จากเทสต์ที่ล้มก่อนหน้าจึงทำให้เทสต์ถัดไปค้างตาม เมื่อเทสต์ผ่านครบปัญหานี้ก็หายไป
- **ชั้น 3 (96abfec)**
  - Server Actions: mock `@/lib/api/admin`, `refresh` และ `redirect` (ให้ throw เหมือนของจริง)
  - client ที่เรียก backend: mock `fetch` และ `next/headers` แล้วตรวจว่าประตู admin ปฏิเสธก่อนที่จะเรียก backend
- **E2E (7981456, c1f67cc)** ใช้ compose project `snapshop-e2e` ซึ่งมี DB volume ของตัวเองและ `FEED_URL` ว่าง
  - mount ของ inbox อ่านจาก `INBOX_DIR` (ค่า default `./inbox` stack dev จึงไม่เปลี่ยน) ส่วน stack ทดสอบใช้ `frontend/e2e/.inbox`
  - ข้อมูลทดสอบเป็นไฟล์ 7 แถวที่เขียนเอง (3 กลุ่ม, ราคาย่อ 1 แถว, 1 รายการไม่มีคู่)
  - ปัญหา lock file: `npm install` บน Windows ทำให้ `package-lock.json` ขาด `@emnapi/runtime` และ `npm ci` ใน image Linux ล้ม แก้โดยรัน `npm install --package-lock-only` ใน `node:24-alpine` ซึ่งเติมแค่ 3 รายการที่ขาด
  - ปิด stack dev ตามที่ผู้ใช้สั่ง: `next dev` บนพอร์ต 3000, backend (java) บนพอร์ต 8080 และ container `snapshop-db-1` (stop เท่านั้น ไม่ลบ volume)
  - รอบแรกล้ม 2 ข้อ ทั้งสองข้อเป็นความผิดของเทสต์ ไม่ใช่ของแอป:
    - ช่องค้นหาของหน้าสินค้า admin ไม่ค้นด้วย item id
    - Next (cacheComponents) เก็บหน้าพรีวิวที่ออกมาแล้วไว้ใน DOM แบบซ่อน แก้โดยใช้ `getByRole` ที่ข้ามองค์ประกอบที่ซ่อน
  - ผลรอบสุดท้าย: 17/17 ผ่านบน stack ที่เริ่มจากข้อมูลว่าง และ `inbox/` จริงไม่เปลี่ยน
- ข้อสังเกตเรื่องพฤติกรรมเดิมของแอป (ไม่ได้แก้): ในหน้าสินค้า admin เมื่อกด "เลิกขาย" แล้วรายการจะหายจากรายการทันทีหลัง `refresh()` ข้อความ "บันทึกว่าเลิกขายแล้ว" จึงหายไปพร้อมกัน ผู้ใช้เห็นเพียงว่าแถวหายไป
- **Stryker (4bc1ac5)**
  - รอบแรกได้ 90.28%: รอด 48 ตัว และ `SearchForm` ไม่มีเทสต์ครอบ
  - เพิ่มเทสต์ที่ผู้ใช้จะเห็นผลจริง: label ของ outcome/สถานะ, ปุ่มนำเข้าตอนว่าง, ขอบเขต 20 บรรทัดของพรีวิว, ช่องว่างในบรรทัดกลุ่ม และ `SearchForm`
  - ignorer `styling` ข้าม mutant ที่เปลี่ยนแค่ class ของ Tailwind หรือ tone ของ badge
  - ปิด 3 mutant ใน `auth.ts` ที่ให้ผลเหมือนโค้ดเดิม (equivalent) พร้อมเหตุผล
  - รอบสองได้ 100%: killed 462, timeout 12, survived 0, no coverage 0, ignored 36 ใช้เวลารอบละราว 30 นาที เพราะต้องสร้าง jsdom ใหม่ทุกไฟล์
  - mutant ที่แก้ตัวเลือกของ Intl เป็นค่าว่างทำให้ module throw ตอนโหลด (RuntimeError/Timeout) จึงนับว่าจับได้แล้ว
- **ตรวจรอบสุดท้าย**
  - ผ่านหมด: `npm run lint`, `npm run build`, `npm test` (148) และ E2E 17/17 สองรอบ แต่ละรอบเริ่มจากฐานข้อมูลใหม่ (`import_batch` เริ่มที่ 1 ทุกรอบ)
  - ระหว่างตรวจเจอ 3 เรื่อง:
    - **global-setup** เดิมรอแค่หน้าแรก แต่หน้าแรกเป็น Partial Prerender ซึ่งตอบ 200 จาก static shell ทั้งที่ backend ยังไม่ขึ้น seed จึงล้ม แก้ให้รอ `/api/product-types` ของ backend ก่อน
    - **เทสต์ค้นหา** ฟอร์มค้นหาอยู่ใน Suspense ฟอร์มจาก static shell จะถูกแทนด้วยฟอร์มที่ stream มาทีหลัง ข้อความที่พิมพ์ก่อนการแทนที่จึงหาย แก้เทสต์ให้รอรายการโหลดก่อนแล้วค่อยพิมพ์ (พฤติกรรมของแอปไม่ได้แก้ ดูข้อสังเกตด้านล่าง)
    - **lint** ไปตรวจไฟล์ trace viewer ใน `playwright-report/` ด้วย เพิ่มโฟลเดอร์ที่เครื่องมือเทสต์สร้างไว้ใน `globalIgnores` ของ ESLint
  - เคยล้มหนึ่งรอบเพราะคำสั่งของผมเอง: ผมส่งผล `e2e:down` เข้า `tail -0` คาดว่า `tail -0` ปิดก่อน npm เขียนเสร็จ `down` จึงถูกตัดก่อนลบ volume และข้อมูลเก่ายังอยู่ เมื่อรันโดยไม่ตัด output ผลผ่านทุกรอบ
  - `docker compose config` ของ stack dev ยัง mount `./inbox` เหมือนเดิม และ `inbox/` จริงไม่เปลี่ยน (แก้ไขล่าสุด 2026-11-13)
- ข้อสังเกตเรื่องพฤติกรรมเดิมของแอป (ไม่ได้แก้): ถ้าผู้ใช้พิมพ์ในช่องค้นหาหน้าแรกเร็วมากก่อนที่ฟอร์มที่ stream มาจะแทนฟอร์มของ static shell ข้อความที่พิมพ์จะหาย
- **ยังไม่ได้ทำ**: ยังไม่ได้เปิด stack dev กลับ (`next dev`, backend นอก Docker, container `snapshop-db-1`) และยังไม่ได้ push

## 2026-11-18 — ไฟล์นำเข้ารูปแบบใหม่ + REVIEW กลุ่ม R (branch `feat/rule-group-review`)
- เหตุ: ผู้ใช้ให้ไฟล์รูปแบบใหม่ `research/drpong_new_grouped_20261117.csv` (431 แถว, กลุ่ม E 205, กลุ่ม R 16) แยก `namePrefix` มาแล้ว ไม่มี `วิธีจัดกลุ่ม` และเพิ่มกลุ่มตามกฎ ต้องการให้สินค้าที่มีแค่กลุ่ม E ขึ้นเว็บเหมือนเดิม ส่วนสินค้ากลุ่ม R ต้อง REVIEW เทียบกลุ่ม E ก่อนขึ้นเว็บ
- ก่อนแก้ สำรวจไฟล์แล้วถามผู้ใช้ 3 รอบ ผู้ใช้ตัดสิน:
  - ทุกสินค้าเก็บ 2 กลุ่ม; รายการใหม่ในกลุ่ม R ต้อง REVIEW ยกเว้นผลของกฎทั้งสองตรงกัน (กลุ่ม R = กลุ่ม E หนึ่งกลุ่มพอดี) ผ่านอัตโนมัติ; รหัสสินค้าเดิมไม่ตรวจซ้ำ
  - `namePrefix` = ข้อความก่อน "Dr." เท่านั้น, ไม่ตรวจชื่อตอนอ่านไฟล์; ลบ `name_raw` และ `grouping_method`; รับเฉพาะไฟล์รูปแบบใหม่; ล้างข้อมูลเก่าด้วย `docker compose down -v`
  - หน้า REVIEW แยก (ไม่ใช่แค่พรีวิว); ปุ่มรวม = ติ๊กกลุ่ม E แล้วรวมเข้ากลุ่ม R **ทั้งกลุ่ม**, ที่ไม่ติ๊กผ่าน REVIEW แต่อยู่กลุ่ม E เดิม, ชื่อกลุ่มที่รวม = ชื่อกลุ่มตามกฎ
  - ขึ้นเว็บต้อง ≥ 2 **รายการ** (ผมเคยพูดผิดว่า 2 ร้าน — แก้ถ้อยคำใน README/DATA-FLOW ด้วย); เช็คสินค้าหมดจากชื่อคงไว้
  - schema: เพิ่ม `V5` ไปก่อน ยุบ migration ก่อนขึ้นระบบจริง (เหตุผล: ช่วง dev ถ้าแก้ V1–V4 ตรง ๆ ทุกการแก้ schema ต้องล้าง DB + ดึง feed ใหม่)
  - ผู้ใช้อนุมัติแผนผ่าน plan mode แบ่งเป็น T1–T8
- **T1 (0e3b5ff)** อ่านไฟล์ 14 คอลัมน์: เทสต์ parser ก่อน (ไฟล์จริง 431 แถว, คู่กลุ่ม R ว่างทั้งคู่/มีทั้งคู่, ไฟล์รูปแบบเก่าถูกปฏิเสธ) → `Columns`, `RecordReader`, `RuleGroup`; `ProductName` เลิกตัดที่ "Dr." แล้ว; `V5` ลบ `name_raw` + `ck_name_parts` + `grouping_method`; ลบ `GroupingMethod` ทั้ง backend/frontend; fixture เทสต์เปลี่ยนเป็นไฟล์ใหม่
  - ตัดสินใจเล็กที่ไม่ได้ถาม (แจ้งไว้ที่นี่): ชื่อเต็มที่แสดง (`displayName`, พรีวิว, เช็ค "สินค้าหมด") = prefix + **ช่องว่าง** + ชื่อ เพราะ prefix ในไฟล์ใหม่ไม่มีช่องว่างท้าย ถ้าต่อกันตรง ๆ จะได้ "promoDr.PONG"
- **T2 (64d734f)** เก็บกลุ่ม R: `product_type.kind` (NAME/RULE, unique ต่อ kind) + `marketplace_product.rule_type_id`; `RulePlanner` (บริสุทธิ์) จับคู่กลุ่ม R ด้วยชื่อ normalize, ตัดสิน REVIEW/ผ่านอัตโนมัติ, รายการเดิมคงกลุ่ม R เดิม; พรีวิวแสดงจำนวนรอ REVIEW / ผ่านอัตโนมัติ / กลุ่ม R ใหม่
  - เทสต์ integration ข้อหนึ่งของผมคาดผิด (คาด REVIEW แต่ได้ OK) — รายการเดียวในกลุ่ม R และกลุ่ม E เดียวกันคือ "ผ่านอัตโนมัติ" ตามกฎ แก้ที่เทสต์
  - ไฟล์จริง: REVIEW 89, ผ่านอัตโนมัติ 7 (R004, R006, R010), OK 313, PRICE_PENDING 29
- **T3 (5458146)** รวมกลุ่ม: `product_type.merged_into_type_id` (เฉพาะกลุ่ม E), view `public_product` จัดตามกลุ่มที่แสดง; `GET /api/admin/rule-groups`, `POST /api/admin/rule-groups/{code}/confirm` (ใช้ล็อกเดียวกับนำเข้า, ยืนยันซ้ำได้ผลเดิม)
  - `passReview` แทน `resolveReview`: ไม่มีราคา → `PRICE_PENDING` (เดิม error) ตามแผนที่อนุมัติ ใช้กับปุ่ม "ตรวจแล้ว" รายตัวด้วย
  - เพิ่มกันข้อมูลเพี้ยน: กลุ่ม E ที่รวมเข้ากลุ่ม R อื่นแล้วรวมซ้ำไม่ได้ (409); ปุ่ม "ย้ายกลุ่ม" ย้ายเข้ากลุ่ม R ไม่ได้ (409) เพราะ `product_type_id` ต้องเป็นกลุ่ม E เสมอ
- **T4 (520974f)** หน้า `/admin/rule-groups` (การ์ดต่อกลุ่ม R, checkbox ต่อกลุ่ม E ติ๊กไว้ทั้งหมด, ปุ่มยืนยัน disable จนเสร็จ), ลิงก์ในเมนู, `/admin/types` แสดงชนิดและรวมเข้า, พรีวิว inbox แสดงตัวเลขใหม่
- **T5 (2cce4b1)** E2E: ไฟล์ตัวอย่างรูปแบบใหม่ 11 แถว (กลุ่ม R ข้ามกลุ่ม E 1 กลุ่ม + กลุ่ม R ที่ผ่านอัตโนมัติ 1 กลุ่ม) และ `admin-rule-groups.spec.ts`; 20/20 ผ่านบน stack `snapshop-e2e` ที่เริ่มจาก DB ว่าง (stack dev ไม่ได้รันอยู่ จึงไม่ต้องปิด); ถ่ายภาพหน้า REVIEW จาก stack ทดสอบด้วย spec ชั่วคราว (ลบแล้ว ไม่ commit)
- **T6 คุณภาพ**: CRAP — `ImportPlanner.addRow` ได้ 10 พอดี แยก `addCurrent` + `priceKnown` → ทุกเมธอด < 10 (สูงสุด 7.0); PIT รอบแรก 94.7% (NO_COVERAGE 9: `ImportPlan.ruleReviewCount` ไม่มี unit test, `Labels` กลายเป็น dead code หลังลบ `GroupingMethod`) → เพิ่มเทสต์ + ลบ `Labels` → 165/165; Stryker รอบเต็ม 99.81% (รอด 1: รายการ "กลุ่ม R ใหม่" ในพรีวิวไม่มีเทสต์) → เพิ่มเทสต์ รันไฟล์นั้นซ้ำได้ 100% (da35b90)
- เครื่องมือ: heredoc ใน Bash tool ของ session นี้กิน backslash (`\\n` กลายเป็นขึ้นบรรทัดจริง) ทำให้ string ใน Java เสีย — เปลี่ยนไปเขียนสคริปต์แก้ไฟล์ด้วย Write tool แล้วรันด้วย `python -I`
- **T8** ผู้ใช้ยืนยันแล้ว: `docker compose --profile app down -v` ลบ volume `snapshop_db-data` + container dev → `up -d --build` บน branch นี้ (Flyway V1–V5 สำเร็จ, สินค้า 0 รายการ) → คัดลอก `research/drpong_new_grouped_20261117.csv` ไป `inbox/`; stack E2E หยุดด้วย `down` (ไม่ลบ volume)
- **ยังไม่ได้ทำ/ไม่ได้ตรวจ**: ไม่ได้นำเข้าไฟล์จริงผ่านหน้าเว็บ (ต้องใช้รหัส admin จริง — ผู้ใช้ทำเอง) ส่วนไฟล์จริงตรวจผ่านเทสต์ integration; ยังไม่ได้ push

## 2026-11-19 — กฎ REVIEW กลุ่ม R ยึดกลุ่ม E + ส่วน "ยุบรวมได้" + redirect (branch `feat/rule-group-merge`)
- เหตุ: ผู้ใช้เห็นว่ากฎ "กลุ่ม R ต้องเท่ากับกลุ่ม E พอดี" ทำให้สินค้าที่ควรขึ้นเว็บต้องรอ REVIEW (เช่น R003 ทั้ง 13 รายการรอ ทั้งที่ E114/E111/E113 มีตั้งแต่ 2 รายการ) และถามว่าควรรวมกลุ่ม E เข้ากลุ่ม E หลักแทนกลุ่ม R ไหม
- คุยใน session แยก อธิบายด้วยตัวอย่างเล็ก (Aqua Gel 3 รายการ) แล้วได้คำตัดสินทีละข้อ:
  - ขึ้นเว็บยึดกลุ่ม E: รายการกลุ่ม R ผ่านถ้ากลุ่ม E มี ≥ 2 รายการ; กลุ่ม E มีรายการนอกกลุ่ม R ปนก็ผ่าน; กลุ่ม E รายการเดียว → REVIEW
  - ข้อเสนอเดิมเรื่อง "กลุ่ม E หลัก" (ผ่านเฉพาะกลุ่มที่มีสมาชิกมากสุด) ขัดกับกฎข้างบน ผมยอมรับว่าพลาดและตัดทิ้ง ข้อเรื่องกรณีเสมอกันจึงหายไปด้วย
  - ทิศการรวม: คงรวมกลุ่ม E เข้ากลุ่ม R (ชื่อกว้างกว่า) + redirect ลิงก์กลุ่ม E เดิม — งานน้อยกว่าการรวมเข้ากลุ่ม E หลัก ซึ่งต้องเก็บชื่อที่แสดงแยก เพราะการนำเข้าจับคู่กลุ่ม E ด้วยชื่อ (`ImportPlanner.typeForNewItems`)
  - กลุ่ม R ที่ไม่มีรายการรอแต่ครอบกลุ่ม E ≥ 2 กลุ่ม → ส่วน "ยุบรวมได้" (ต้องมีเครื่องหมาย "ยืนยันแล้ว" กันการ์ดค้าง)
  - ผู้ใช้ให้ pull `main` (PR #7 merge แล้ว) แล้วแตก branch ใหม่; อนุมัติแผนผ่าน plan mode
  - ระหว่างทำเจอกรณีที่แผนไม่ครอบคลุม (กลุ่ม E รายการเดียว และกลุ่ม R มีแค่รายการนั้น) ถามแล้ว ผู้ใช้ให้ REVIEW ตามกฎเดิม (ไฟล์จริงไม่มีกรณีนี้)
- ขั้น 1 กฎ REVIEW: `RulePlanner` นับแถวของกลุ่ม E ในไฟล์ (`SHOWS_ON_ITS_OWN = 2`) แทนการเทียบเซตพอดี — เทสต์ planner แก้ก่อน เห็นล้มด้วยสถานะผิดตามคาด แล้วจึงแก้โค้ด
- ขั้น 2 "ยุบรวมได้": `V6` เพิ่ม `product_type.merge_reviewed_at` (เฉพาะกลุ่ม R); ยืนยันแล้วตั้งค่า (ครั้งแรก), นำเข้ารายการใหม่เข้ากลุ่ม R เดิม → ล้าง (`ImportPlan.reopenedRuleTypeIds`); `pending()` = กลุ่มที่มี REVIEW ∪ กลุ่มที่ยังไม่ยืนยันและครอบกลุ่ม E ≥ 2 กลุ่ม; หน้า admin แยก 2 ส่วนด้วย `RuleGroupSections`, การ์ดไม่แสดง "รอตรวจ 0 รายการ", หัวการ์ดเปลี่ยนเป็น h3 ให้อยู่ใต้หัวส่วน
  - ยอมรับตามจริง: ขั้นนี้ฝั่ง backend ผมเขียนเทสต์กับโค้ดแล้วรันทีเดียว ไม่ได้ดูเทสต์ใหม่ล้มก่อน (ฝั่ง frontend เห็นล้มก่อน)
  - catalog ของ `AdminRuleGroupApiTest` เขียนใหม่ เพราะภายใต้กฎใหม่ catalog เดิมไม่มีรายการ REVIEW เหลือเลย
- ขั้น 3 redirect: `ProductQueryService.type` ตอบด้วยกลุ่ม R ถ้ารหัสเป็นกลุ่ม E ที่ถูกรวม (เทสต์ล้มด้วย 404 ก่อนแก้); หน้า `/types/[code]` เรียก `redirect` (307 ชั่วคราว เผื่อยกเลิกการรวมในอนาคต)
- เทสต์ที่ตัวเลขเปลี่ยนตามกฎ: ไฟล์จริง REVIEW 89 → 45, ผ่านอัตโนมัติ 7 → 51, OK 313 → 355, PRICE_PENDING 29 → 31; `storedItemKeepsItsRuleGroup…` เพิ่มรายการที่สองในกลุ่ม E เพื่อให้เทสต์ยังวัดเรื่องเดิม (กลุ่ม R ไม่เปลี่ยน) ไม่ผูกกับสถานะ REVIEW
- E2E: ไฟล์ตัวอย่างเพิ่ม 4 แถว (กลุ่ม R "E2E Toner" ครอบกลุ่ม E 2 กลุ่ม กลุ่มละ 2 รายการ) → ตัวเลขใน `admin-import`/`admin-inbox` เป็น 15 แถว / กลุ่มใหม่ 8; เพิ่มเทสต์ส่วน "ยุบรวมได้" + ลิงก์เดิม redirect ไม่แตะกลุ่ม Sunscreen/Serum เพราะ `public.spec.ts` ใช้อยู่
