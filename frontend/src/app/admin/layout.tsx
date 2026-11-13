import Link from "next/link";

const LINKS = [
  { href: "/admin", label: "ภาพรวม" },
  { href: "/admin/inbox", label: "ไฟล์รอนำเข้า" },
  { href: "/admin/import", label: "อัปโหลด" },
  { href: "/admin/products", label: "สินค้า" },
  { href: "/admin/types", label: "กลุ่ม" },
] as const;

export default function AdminLayout({ children }: LayoutProps<"/admin">) {
  return (
    <>
      <header className="border-b border-zinc-200 bg-zinc-900 text-white">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-3">
          <span className="font-semibold">Snapshop admin</span>
          <nav className="flex flex-wrap gap-4 text-sm text-zinc-300">
            {LINKS.map((link) => (
              <Link key={link.href} href={link.href} className="hover:text-white">
                {link.label}
              </Link>
            ))}
            <Link href="/" className="hover:text-white">
              ดูเว็บ user →
            </Link>
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-6xl flex-1 space-y-6 px-4 py-6">{children}</main>
    </>
  );
}
