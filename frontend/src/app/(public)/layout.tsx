import Link from "next/link";

export default function PublicLayout({ children }: LayoutProps<"/">) {
  return (
    <>
      <header className="border-b border-zinc-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <Link href="/" className="text-lg font-semibold text-orange-600">
            Snapshop
          </Link>
          <span className="text-xs text-zinc-500">เปรียบเทียบราคาสินค้าชนิดเดียวกันจากหลายร้าน</span>
        </div>
      </header>
      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-6">{children}</main>
      <footer className="border-t border-zinc-200 py-4 text-center text-xs text-zinc-500">
        ลิงก์ไปยังร้านค้าบางลิงก์เป็นลิงก์ Affiliate
      </footer>
    </>
  );
}
