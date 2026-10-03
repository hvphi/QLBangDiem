"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { AppRole, clearDemoRole, DemoRole, getDemoRole, roleLabel } from "@/lib/auth/session";

type Props = { children: React.ReactNode; eyebrow: string; title: string; description: string; };
type Actor = { displayName: string; role: AppRole };
type Notice = { id: string; message: string; createdAt: string; readAt: string | null };

const navigation: Record<AppRole, { href: string; label: string; icon: string }[]> = {
  LECTURER: [{ href: "/lecturer", label: "Tổng quan", icon: "⌂" }, { href: "/lecturer/submissions", label: "Bảng điểm đã nộp", icon: "▤" }],
  DEPT_HEAD: [{ href: "/department", label: "Hồ sơ chờ duyệt", icon: "✓" }],
  EXAMINATION: [{ href: "/examination", label: "Tra cứu & kiểm toán", icon: "⌕" }],
  ADMIN: [{ href: "/examination", label: "Tra cứu & kiểm toán", icon: "⌕" }],
};

export function AppShell({ children, eyebrow, title, description }: Props) {
  const pathname = usePathname();
  const router = useRouter();
  const [role, setRole] = useState<AppRole | null>(null);
  const [actor, setActor] = useState<Actor | null>(null);
  const [notices, setNotices] = useState<Notice[]>([]);
  const [unread, setUnread] = useState(0);
  const [noticesOpen, setNoticesOpen] = useState(false);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const current = getDemoRole();
    const localDemo = process.env.NEXT_PUBLIC_LOCAL_DEMO === "true";
    if (localDemo && !current) { router.replace("/login"); return; }
    Promise.all([api<Actor>("/api/auth/me"), api<Notice[]>("/api/notifications")])
      .then(([who, items]) => { setRole(who.role); setActor(who); setNotices(items); setUnread(items.filter((item) => !item.readAt).length); })
      .catch(() => { clearDemoRole(); router.replace("/login"); })
      .finally(() => setReady(true));
  }, [router]);

  if (!ready || !role || !actor) return <div className="loading">Đang kiểm tra phiên làm việc…</div>;
  const links = navigation[role];
  const initials = actor.displayName.split(" ").slice(-2).map((part) => part[0]).join("").toUpperCase();
  async function markRead(notice: Notice) {
    if (notice.readAt) return;
    try {
      await api<void>(`/api/notifications/${notice.id}/read`, { method: "PATCH" });
      setNotices((items) => items.map((item) => item.id === notice.id ? { ...item, readAt: new Date().toISOString() } : item));
      setUnread((count) => Math.max(0, count - 1));
    } catch { /* the notification remains unread until the server confirms */ }
  }
  return <div className="app-frame">
    <aside className="sidebar">
      <Link href={links[0].href} className="brand">
        <span className="brand-mark">BĐ</span><span><span className="brand-name">Bảng điểm số</span><span className="brand-sub">QUẢN LÝ HỌC VỤ</span></span>
      </Link>
      <div className="side-label">Không gian làm việc</div>
      <nav className="nav-list" aria-label="Điều hướng chính">
        {links.map((item) => <Link key={item.href} href={item.href} className={`nav-link ${pathname === item.href ? "active" : ""}`}>
          <span className="nav-icon" aria-hidden="true">{item.icon}</span>{item.label}
        </Link>)}
      </nav>
      <div className="sidebar-bottom">Hồ sơ được lưu theo năm học, học kỳ, khoa và mã lớp học phần.<br />Mọi thao tác được ghi nhận trong nhật ký.</div>
    </aside>
    <div className="main-column">
      <header className="topbar">
        <div className="crumb">Cổng nghiệp vụ <span aria-hidden="true"> / </span> {roleLabel(role)}</div>
        <div className="top-actions">
          <span className="demo-chip">STAGING · LOCAL</span>
          <div className="notification-anchor"><button className="notification-button" aria-label={`${unread} thông báo chưa đọc`} title={`${unread} thông báo chưa đọc`} aria-expanded={noticesOpen} onClick={() => setNoticesOpen((open) => !open)}>♧{unread > 0 && <span className="notification-dot" />}</button>
            {noticesOpen && <div className="notification-menu"><div className="notification-menu-head"><strong>Thông báo</strong><span>{unread} chưa đọc</span></div>{notices.length ? notices.slice(0, 8).map((notice) => <button key={notice.id} className={`notice-row ${notice.readAt ? "read" : ""}`} onClick={() => markRead(notice)}><span className="notice-dot" /><span><span>{notice.message}</span><small>{new Date(notice.createdAt).toLocaleString("vi-VN")}</small></span></button>) : <div className="notice-empty">Chưa có thông báo mới.</div>}</div>}
          </div>
          <div className="user-pill"><span className="avatar">{initials}</span><span><span className="user-name">{actor.displayName}</span><span className="user-role">{roleLabel(role)}</span></span></div>
          <button className="button secondary small" onClick={async () => { if (process.env.NEXT_PUBLIC_LOCAL_DEMO !== "true") await fetch("/logout", { method: "POST", credentials: "same-origin" }); clearDemoRole(); router.push("/login"); }}>Đăng xuất</button>
        </div>
      </header>
      <main className="content">
        <div className="page-heading"><div><div className="eyebrow">{eyebrow}</div><h1 className="page-title">{title}</h1><p className="page-description">{description}</p></div><div className="heading-meta">Quản lý theo học kỳ và phân công</div></div>
        {children}
      </main>
    </div>
  </div>;
}
