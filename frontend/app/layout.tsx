import type { Metadata } from "next";
import "@/styles/globals.css";

export const metadata: Metadata = {
  title: "Quản lý bảng điểm điện tử",
  description: "Tiếp nhận, duyệt và tra cứu bảng điểm ký số",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="vi"><body>{children}</body></html>;
}
