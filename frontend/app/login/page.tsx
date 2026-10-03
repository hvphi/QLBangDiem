"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { setDemoRole, DemoRole, roleHome } from "@/lib/auth/session";
import { AppRole } from "@/lib/auth/session";
import { api } from "@/lib/api";

const roles: { role: DemoRole; icon: string; name: string; hint: string }[] = [
  { role: "LECTURER", icon: "▤", name: "Giảng viên", hint: "Nộp bảng điểm đã ký số, theo dõi tiến độ" },
  { role: "DEPT_HEAD", icon: "✓", name: "Trưởng khoa", hint: "Kiểm tra hồ sơ và xác nhận bảng điểm" },
  { role: "EXAMINATION", icon: "⌕", name: "Khảo thí", hint: "Tra cứu hồ sơ và theo dõi dấu vết kiểm toán" },
];

export default function LoginPage() {
  const router = useRouter();
  const localDemo = process.env.NEXT_PUBLIC_LOCAL_DEMO === "true";
  useEffect(() => {
    if (localDemo) return;
    api<{ role: AppRole }>("/api/auth/me").then((actor) => router.replace(roleHome(actor.role))).catch(() => {});
  }, [localDemo, router]);
  return <main className="login-wrap">
    <section className="login-brand-panel">
      <div className="brand"><span className="brand-mark">BĐ</span><span><span className="brand-name">Bảng điểm số</span><span className="brand-sub">QUẢN LÝ HỌC VỤ</span></span></div>
      <div className="login-art"><div className="eyebrow" style={{ color: "#8ed3c4" }}>Quy trình số hóa</div><h1 style={{ fontSize: 34, lineHeight: 1.25, letterSpacing: "-.04em", margin: 0 }}>Từ ký số đến lưu trữ, trong một luồng rõ ràng.</h1><p style={{ color: "#b5d0c9", lineHeight: 1.8, fontSize: 12 }}>Nộp bảng điểm, xác minh chữ ký và theo dõi phê duyệt với lịch sử thao tác đầy đủ.</p><div className="login-art-card"><div style={{ fontSize: 10, color: "#d2ebe2", fontWeight: 700 }}>LUỒNG XỬ LÝ BẢNG ĐIỂM</div><div className="login-art-line" /><div className="login-art-line short" /><div className="login-art-row"><span>Giảng viên ký số</span><span>→</span><span>Trưởng khoa duyệt</span><span>→</span><span>Lưu trữ</span></div></div></div>
      <div className="login-copy">Hệ thống quản lý bảng điểm điện tử · Môi trường staging</div>
    </section>
    <section className="login-form-panel"><div className="login-card"><div className="eyebrow">Chào mừng trở lại</div><h2 style={{ margin: "0 0 8px", fontSize: 23, letterSpacing: "-.04em" }}>{localDemo ? "Chọn vai trò để vào hệ thống" : "Đăng nhập hệ thống"}</h2><p className="page-description" style={{ marginBottom: 22 }}>{localDemo ? "Tài khoản mẫu chỉ dùng trên staging cục bộ." : "Sử dụng tài khoản định danh của trường để tiếp tục."}</p>
      {localDemo ? <div className="role-choice">{roles.map(({ role, icon, name, hint }) => <button key={role} className="role-button" onClick={() => { setDemoRole(role); router.push(roleHome(role)); }}><span aria-hidden="true">{icon}</span><strong>{name}</strong><div className="role-hint">{hint}</div></button>)}</div> : <a className="button" href="/oauth2/authorization/school" style={{ display: "block", textAlign: "center" }}>Đăng nhập bằng tài khoản trường</a>}
      <p className="footer-note">Không có khóa riêng, mã PIN hoặc dữ liệu chứng thư được lưu trên máy chủ. Chữ ký được kiểm tra lại phía hệ thống trước khi đổi trạng thái hồ sơ.</p></div></section>
  </main>;
}
