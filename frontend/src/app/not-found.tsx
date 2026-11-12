import Link from "next/link";

export default function NotFound() {
  return (
    <main className="mx-auto max-w-5xl space-y-3 px-4 py-16 text-center">
      <h1 className="text-xl font-semibold">ไม่พบหน้านี้</h1>
      <p className="text-sm text-zinc-500">สินค้าอาจไม่มีขายแล้ว หรือมีร้านขายน้อยกว่า 2 ร้าน</p>
      <Link href="/" className="text-sm text-orange-600 hover:underline">
        กลับหน้าแรก
      </Link>
    </main>
  );
}
