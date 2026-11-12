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
