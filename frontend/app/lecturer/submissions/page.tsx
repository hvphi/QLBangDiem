"use client";

import { useEffect, useState } from "react";
import { AppShell } from "@/components/shell/app-shell";
import { api, apiBlob } from "@/lib/api";

type Transcript = { id: string; courseClassCode: string; subjectName: string; fileName: string; status: string; createdAt: string; rejectionReason: string | null; signatureCount: number; sha256: string };
const status = (value: string) => value === "LECTURER_SIGNED" ? ["Chờ duyệt", "waiting"] : value === "ARCHIVED" ? ["Đã lưu trữ", "archived"] : ["Bị từ chối", "rejected"];

export default function LecturerSubmissionsPage() {
  const [items, setItems] = useState<Transcript[]>([]); const [error, setError] = useState("");
  useEffect(() => { api<Transcript[]>("/api/submissions").then(setItems).catch((e) => setError(e.message)); }, []);
  async function download(id: string, name: string) { try { const blob = await apiBlob(`/api/submissions/${id}/file`); const url = URL.createObjectURL(blob); const a = document.createElement("a"); a.href = url; a.download = name; a.click(); URL.revokeObjectURL(url); } catch (e) { setError(e instanceof Error ? e.message : "Không thể tải tệp."); } }
  return <AppShell eyebrow="Hồ sơ cá nhân" title="Bảng điểm đã nộp" description="Các phiên bản đã gửi được giữ nguyên để đối chiếu và kiểm toán.">
    {error && <div className="alert error" role="alert" style={{ marginBottom: 15 }}>{error}</div>}
    <div className="panel"><div className="panel-header"><div><h2 className="panel-title">Danh sách hồ sơ</h2><div className="panel-subtitle">{items.length} phiên bản</div></div></div><div className="table-wrap"><table className="table"><thead><tr><th>Lớp học phần</th><th>Tệp</th><th>Ngày nộp</th><th>Hash SHA-256</th><th>Trạng thái</th><th></th></tr></thead><tbody>{items.map((item) => { const [label, cls] = status(item.status); return <tr key={item.id}><td><div className="table-primary">{item.courseClassCode}</div><div className="table-secondary">{item.subjectName}</div></td><td>{item.fileName}</td><td>{new Date(item.createdAt).toLocaleString("vi-VN")}</td><td><span className="token-badge">{item.sha256.slice(0, 14)}…</span></td><td><span className={`status ${cls}`}>{label}</span>{item.rejectionReason && <div className="table-secondary">{item.rejectionReason}</div>}</td><td><button className="button secondary small" onClick={() => download(item.id, item.fileName)}>Tải PDF</button></td></tr>; })}</tbody></table>{items.length === 0 && <div className="empty-state"><div className="empty-icon">▤</div>Chưa có hồ sơ. Hãy gửi bản PDF đã ký từ trang Tổng quan.</div>}</div></div>
  </AppShell>;
}
