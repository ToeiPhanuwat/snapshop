"use client";

export default function ErrorPage({ reset }: { error: Error; reset: () => void }) {
  return (
    <main className="mx-auto max-w-5xl space-y-3 px-4 py-16 text-center">
      <h1 className="text-xl font-semibold">โหลดข้อมูลไม่สำเร็จ</h1>
      <p className="text-sm text-zinc-500">ระบบหลังบ้านอาจยังไม่พร้อม ลองใหม่อีกครั้ง</p>
      <button onClick={reset} className="rounded-lg bg-zinc-900 px-4 py-2 text-sm text-white">
        ลองใหม่
      </button>
    </main>
  );
}
