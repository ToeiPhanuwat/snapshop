import { formatDateTime } from "@/lib/format";
import type { ImportResult } from "@/types/api";

export function ImportHistory({ imports }: { imports: ImportResult[] }) {
  if (imports.length === 0) {
    return <p className="text-sm text-zinc-500">ยังไม่เคยนำเข้า</p>;
  }
  return (
    <div className="overflow-x-auto rounded-xl bg-white ring-1 ring-zinc-200">
      <table className="w-full text-left text-sm">
        <thead className="text-xs text-zinc-500">
          <tr>
            <th className="p-3">เวลา</th>
            <th className="p-3">ไฟล์</th>
            <th className="p-3 text-right">แถว</th>
            <th className="p-3 text-right">ใหม่</th>
            <th className="p-3 text-right">ราคาเปลี่ยน</th>
            <th className="p-3 text-right">ซ่อน</th>
            <th className="p-3 text-right">กลุ่มใหม่</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-zinc-100">
          {imports.map((i) => (
            <tr key={i.batchId}>
              <td className="whitespace-nowrap p-3">{formatDateTime(i.importedAt)}</td>
              <td className="p-3">{i.sourceFiles}</td>
              <td className="p-3 text-right">{i.rowCount}</td>
              <td className="p-3 text-right">{i.insertedCount}</td>
              <td className="p-3 text-right">{i.priceChangedCount}</td>
              <td className="p-3 text-right">{i.notInLatestCount}</td>
              <td className="p-3 text-right">{i.newTypeCount}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
