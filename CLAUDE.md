# Snapshop — คำสั่งสำหรับ Claude

## หลักการเขียนโค้ด (โหลดทุก session)
@SKILL.md

ถ้า SKILL.md ขัดกับ CLAUDE.md ส่วนตัวของผู้ใช้ ให้ใช้ CLAUDE.md ส่วนตัว — โดยเฉพาะข้อ "Push to the main line":
commit บน feature branch เท่านั้น ไม่ commit/push เข้า `main` ตรง การรวมเข้า main ทำผ่าน PR เมื่อผู้ใช้สั่ง

## ข้อตกลงของโปรเจกต์
- Claude ไม่ใช้รหัสผ่านหรือคีย์ admin ของระบบ (ไม่เรียก `/api/admin/**` ด้วยรหัสผ่านจาก `.env`)
- ข้อมูลที่ Claude เตรียม (merge + แยกชนิดสินค้า) ให้วางเป็นไฟล์ CSV ใน `inbox/` — admin พรีวิวและกดนำเข้าเองที่ `/admin/inbox`
- ไฟล์นำเข้าต้องมีชื่อสินค้าเต็ม (ระบบแยก name_prefix ที่ "Dr." เอง) รูปแบบคอลัมน์อยู่ใน README.md
- ไม่ทำ automation ที่พรางตัวเป็นคนบน Shopee
- ศัพท์ของโดเมน: docs/GLOSSARY.md · บันทึกงานทุกขั้นพร้อมเหตุผล: docs/WORKLOG.md

## คำสั่งที่ใช้บ่อย (Windows, Git Bash)
- Backend ต้องใช้ JDK 25 (JAVA_HOME ของเครื่องชี้ JDK 17):
  `cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.3.9-hotspot" ./mvnw verify`
  (เทสต์ใช้ Testcontainers ต้องเปิด Docker Desktop)
- CRAP: `python -I tools/crap.py target/site/jacoco/jacoco.xml` (ใน backend, หลัง verify)
- PIT: `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` (ใน backend)
- Frontend: `cd frontend && npm run lint && npm run build` — Next.js 16 มี breaking changes อ่าน `frontend/AGENTS.md` และ `node_modules/next/dist/docs` ก่อนเขียน
- รัน jar บนเครื่องนี้นอก Docker ต้องเพิ่ม `-Djdk.net.unixdomain.tmpdir=<โฟลเดอร์ที่เขียนได้>` ไม่เช่นนั้น Tomcat เปิด loopback ไม่ได้
- ทั้งระบบ: `docker compose --profile app up -d --build`
