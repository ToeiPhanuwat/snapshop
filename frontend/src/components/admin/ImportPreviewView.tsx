import { formatPrice } from "@/lib/format";
import type { ImportPreview, ItemLine } from "@/types/api";

function Section({ title, count, children }: { title: string; count: number; children: React.ReactNode }) {
  return (
    <details className="rounded-xl bg-white p-4 ring-1 ring-zinc-200" open={count > 0 && count <= 20}>
      <summary className="cursor-pointer text-sm font-medium">
        {title} ({count})
      </summary>
      <div className="mt-3 text-sm">{count === 0 ? <p className="text-zinc-500">ไม่มี</p> : children}</div>
    </details>
  );
}

function Items({ items }: { items: ItemLine[] }) {
  return (
    <ul className="space-y-1">
      {items.map((i) => (
        <li key={i.itemId}>
          <span className="text-zinc-500">{i.itemId}</span> {i.name}
        </li>
      ))}
    </ul>
  );
}

/** What importing the file would change; nothing has been written yet. */
export function ImportPreviewView({ preview }: { preview: ImportPreview }) {
  const counts = [
    ["แถวในไฟล์", preview.rowCount],
    ["รายการใหม่", preview.insertedCount],
    ["ราคาเปลี่ยน", preview.priceChangedCount],
    ["ราคาเท่าเดิม", preview.unchangedCount],
    ["จะถูกซ่อน", preview.notInLatestCount],
    ["กลุ่มใหม่", preview.newTypeCount],
    ["ดึงจากฟีด", preview.feedItemCount],
    ["รอ REVIEW กลุ่ม R", preview.ruleReviewCount],
    ["กลุ่ม R ผ่านอัตโนมัติ", preview.autoPassedCount],
  ] as const;
  return (
    <div className="space-y-3">
      <dl className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
        {counts.map(([label, value]) => (
          <div key={label} className="rounded-xl bg-white p-3 ring-1 ring-zinc-200">
            <dt className="text-xs text-zinc-500">{label}</dt>
            <dd className="text-lg font-semibold">{value}</dd>
          </div>
        ))}
      </dl>
      <Section title="ราคาเปลี่ยน" count={preview.priceChanges.length}>
        <ul className="space-y-1">
          {preview.priceChanges.map((c) => (
            <li key={c.itemId}>
              <span className="text-zinc-500">{c.itemId}</span> {c.name}:{" "}
              <span className="line-through">{formatPrice(c.oldPrice)}</span> → <strong>{formatPrice(c.newPrice)}</strong>
            </li>
          ))}
        </ul>
      </Section>
      <Section title="จะถูกซ่อน (ไม่อยู่ในไฟล์นี้)" count={preview.toHide.length}>
        <Items items={preview.toHide} />
      </Section>
      <Section title="จะรอตรวจ (REVIEW)" count={preview.toReview.length}>
        <Items items={preview.toReview} />
      </Section>
      <Section title="กลุ่มใหม่" count={preview.newTypes.length}>
        <ul className="list-disc space-y-1 pl-5">
          {preview.newTypes.map((name) => (
            <li key={name}>{name}</li>
          ))}
        </ul>
      </Section>
      <Section title="กลุ่ม R ใหม่" count={preview.newRuleTypes.length}>
        <ul className="list-disc space-y-1 pl-5">
          {preview.newRuleTypes.map((name) => (
            <li key={name}>{name}</li>
          ))}
        </ul>
      </Section>
    </div>
  );
}
