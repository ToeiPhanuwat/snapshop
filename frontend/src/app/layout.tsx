import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: { default: "Snapshop เปรียบเทียบราคา", template: "%s · Snapshop" },
  description: "เปรียบเทียบราคาสินค้าชนิดเดียวกันจากหลายร้านบน Shopee",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="th" className="h-full antialiased">
      <body className="flex min-h-full flex-col">{children}</body>
    </html>
  );
}
