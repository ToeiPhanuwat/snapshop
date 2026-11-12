import { Suspense } from "react";
import { ProductTypeCard } from "@/components/product/ProductTypeCard";
import { SearchForm } from "@/components/product/SearchForm";
import { Pagination } from "@/components/ui/Pagination";
import { getProductTypes } from "@/lib/api/public";

export default function HomePage({ searchParams }: PageProps<"/">) {
  return (
    <div className="space-y-6">
      <div className="space-y-3">
        <h1 className="text-xl font-semibold">สินค้าที่มีหลายร้านขาย</h1>
        <Suspense fallback={<SearchForm action="/" placeholder="ค้นหาชื่อสินค้า" />}>
          {searchParams.then(({ q }) => (
            <SearchForm action="/" defaultValue={typeof q === "string" ? q : undefined} placeholder="ค้นหาชื่อสินค้า" />
          ))}
        </Suspense>
      </div>
      <Suspense fallback={<p className="text-sm text-zinc-500">กำลังโหลด…</p>}>
        <TypeGrid searchParams={searchParams} />
      </Suspense>
    </div>
  );
}

async function TypeGrid({ searchParams }: Pick<PageProps<"/">, "searchParams">) {
  const { q, page } = await searchParams;
  const text = typeof q === "string" ? q : undefined;
  const pageNumber = Math.max(0, Number(typeof page === "string" ? page : 0) || 0);
  const types = await getProductTypes({ q: text, page: pageNumber });

  if (types.items.length === 0) {
    return <p className="text-sm text-zinc-500">{text ? `ไม่พบสินค้าที่ตรงกับ “${text}”` : "ยังไม่มีสินค้า"}</p>;
  }
  return (
    <>
      <p className="text-sm text-zinc-500">{types.total} ชนิดสินค้า</p>
      <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {types.items.map((type) => (
          <li key={type.code}>
            <ProductTypeCard type={type} />
          </li>
        ))}
      </ul>
      <Pagination path="/" params={{ q: text }} page={types.page} size={types.size} total={types.total} />
    </>
  );
}
