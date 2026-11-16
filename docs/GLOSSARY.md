# Glossary

| คำ | ความหมาย | ในโค้ด |
|---|---|---|
| รายการ (item) | สินค้าหนึ่งชิ้นของร้านหนึ่งบน Shopee ระบุด้วยรหัสสินค้า | `item_id` |
| เวอร์ชันราคา | หนึ่งแถวใน `marketplace_product`; ราคาเปลี่ยนจะได้แถวใหม่ | `marketplace_product` |
| แถวปัจจุบัน | เวอร์ชันราคาล่าสุดของรายการ (มีได้ 1 แถวต่อรายการ) | `active_flag = 'Y'` |
| ประวัติราคา | เวอร์ชันราคาเก่า | `active_flag = 'N'`, `deactivation_reason = PRICE_CHANGED` |
| เลิกขาย | admin ตรวจแล้วว่ารายการเลิกขายจริง | `active_flag = 'N'`, `deactivation_reason = DISCONTINUED` |
| กลุ่ม | รายการสินค้าชนิดเดียวกันจากหลายร้าน ใช้เปรียบเทียบราคา | `product_type` |
| รหัสกลุ่ม | รหัสที่ระบบสร้าง เช่น `PT000001` ไม่เปลี่ยนตลอดอายุกลุ่ม | `product_type_code` |
| ป้ายกลุ่มในไฟล์ | รหัสกลุ่มที่ Claude ใส่ในไฟล์ (E001, R001, U001) ใช้บอกว่าแถวไหนอยู่ด้วยกัน**เฉพาะในไฟล์นั้น** ระบบไม่เก็บ | `ImportRow.fileGroupLabel` |
| ชื่อกลุ่ม normalize | ชื่อกลุ่มที่ตัวพิมพ์เล็ก ตัดเครื่องหมายและช่องว่างซ้ำ ใช้จับคู่กลุ่มเดิม | `type_name_norm` |
| วิธีจัดกลุ่ม | ชื่ออังกฤษตรงกัน / ตามกฎคำระบุสูตร / ไม่มีคู่ | `GroupingMethod` |
| หมวด | ชุดสินค้าจากคำค้นเดียว เช่น `DRPONG` | `category_code` |
| รอบนำเข้า | การนำเข้าไฟล์หนึ่งครั้ง | `import_batch` |
| ชื่อส่วนหน้า / ชื่อหลัก | ข้อความก่อน "Dr." แรก / ตั้งแต่ "Dr." ไป; รวมกันได้ชื่อเดิมเสมอ | `name_prefix`, `name_core`, `name_raw` |
| ประเภทร้าน | Mall / ร้านแนะนำ / ทั่วไป — มาจาก feed: Official shop = Mall, ไม่ใช่แต่ Preferred shop = ร้านแนะนำ, นอกนั้นทั่วไป | `ShopType` = `MALL`, `PREFERRED`, `REGULAR` |
| สถานะ | `OK` แสดงได้, `NOT_IN_LATEST` ไม่อยู่ในรอบล่าสุด, `TEST` สินค้าทดสอบ, `OUT_OF_STOCK` สินค้าหมด, `REVIEW` รอ admin ตัดสิน, `PRICE_PENDING` ราคาใน F1 เป็นตัวย่อ รอ `sale_price` จาก feed (ซ่อนจาก user) | `ProductStatus` |
| แสดงฝั่ง user | แถวปัจจุบัน + สถานะ `OK` + กลุ่มมีรายการแบบนี้ ≥ 2 | view `public_product` |
| ลิงก์หลัก | `affiliate_url` ถ้าว่างใช้ `product_url` | `ProductLink` |
| inbox | โฟลเดอร์ที่วางไฟล์ CSV ที่เตรียมแล้ว รอ admin ตัดสิน; นำเข้าแล้วย้ายไป `imported/`, ทิ้งแล้วย้ายไป `rejected/` | `InboxService`, `IMPORT_INBOX_DIR` |
| พรีวิว | ผลที่การนำเข้าไฟล์จะทำ คำนวณด้วยโค้ดเดียวกับนำเข้าจริงแต่ไม่บันทึก | `ImportService.preview`, `ImportPreviewResponse` |
| F1 | ไฟล์ลิงก์ affiliate จากปุ่ม "รับลิงก์แบบทีเดียวทั้งหมด" ในพอร์ทัล Shopee Affiliate (≤ 100 รายการต่อไฟล์) — [DATA-FLOW.md](DATA-FLOW.md#f1--ลิงก์-affiliate-จากพอร์ทัล) | นอกระบบ |
| F2 | **เลิกใช้แล้ว** (เดิม: ข้อมูลจากการ์ดสินค้าในหน้าค้นหา ซึ่ง scrape ผิดกฎ) แทนด้วย feed — [DATA-FLOW.md](DATA-FLOW.md#feed-ของ-shopee-แทน-f2) | — |
| feed | ไฟล์สินค้าของ Shopee (CSV ~4 GB) ที่ระบบอ่านจาก `FEED_URL` เพื่อเติมราคาขาย ประเภทร้าน รูป stock rating | `FeedClient`, `FeedReader` |
| ดึง feed | การค้นรายการที่ต้องการใน feed หลังนำเข้า บันทึกผลทุกรอบ (เสร็จ / ไม่ครบ / ถูกจำกัด 429 / ล้มเหลว) | `FeedSyncService`, `feed_sync_run` |
| ไฟล์นำเข้า | ไฟล์ F1 ที่แยกชนิดสินค้าแล้ว (12 คอลัมน์) ที่วางใน inbox หรืออัปโหลด | `CsvImportParser` |
