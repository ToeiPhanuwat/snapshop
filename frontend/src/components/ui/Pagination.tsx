import Link from "next/link";

/** Previous / next links that keep the other query parameters of the page. */
export function Pagination({
  path,
  params,
  page,
  size,
  total,
}: {
  path: string;
  params: Record<string, string | undefined>;
  page: number;
  size: number;
  total: number;
}) {
  const last = Math.max(0, Math.ceil(total / size) - 1);
  if (last === 0) {
    return null;
  }
  const href = (p: number) => {
    const search = new URLSearchParams();
    for (const [key, value] of Object.entries({ ...params, page: p > 0 ? String(p) : undefined })) {
      if (value) search.set(key, value);
    }
    const text = search.toString();
    return text ? `${path}?${text}` : path;
  };
  const linkClass = "rounded-md px-3 py-1.5 ring-1 ring-zinc-200 hover:bg-zinc-50";
  return (
    <nav className="mt-6 flex items-center justify-center gap-3 text-sm" aria-label="เปลี่ยนหน้า">
      {page > 0 ? <Link className={linkClass} href={href(page - 1)}>ก่อนหน้า</Link> : null}
      <span className="text-zinc-500">
        หน้า {page + 1} / {last + 1}
      </span>
      {page < last ? <Link className={linkClass} href={href(page + 1)}>ถัดไป</Link> : null}
    </nav>
  );
}
