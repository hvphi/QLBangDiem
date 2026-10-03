"use client";

import { FormEvent, useEffect, useState } from "react";
import { AppShell } from "@/components/shell/app-shell";
import { api, apiBlob, idempotencyKey } from "@/lib/api";
import { signLocally } from "@/features/department/signing";

type Transcript = { id: string; courseClassId: string; courseClassCode: string; subjectName: string; lecturerName: string; departmentName: string; academicYear: string; semester: string; fileName: string; sha256: string; status: string; createdAt: string; signatureCount: number; rejectionReason: string | null };
type Signature = { signerType: string; signerName: string | null; certificateSerial: string | null; issuer: string | null; signedAt: string | null; validationStatus: string; validationReason: string | null };
const date = (value: string) => new Date(value).toLocaleString("vi-VN", { dateStyle: "medium", timeStyle: "short" });

export default function DepartmentPage() {
  const [items, setItems] = useState<Transcript[]>([]); const [selected, setSelected] = useState<Transcript | null>(null);
  const [signatures, setSignatures] = useState<Signature[]>([]); const [viewer, setViewer] = useState("");
  const [file, setFile] = useState<File | null>(null); const [reason, setReason] = useState("");
  const [error, setError] = useState(""); const [notice, setNotice] = useState(""); const [busy, setBusy] = useState(false);
  const [signing, setSigning] = useState(false);
  const refresh = () => api<Transcript[]>("/api/approvals/queue").then((result) => { setItems(result); if (selected) setSelected(result.find((item) => item.id === selected.id) ?? null); });
  useEffect(() => { refresh().catch((e) => setError(e.message)); }, []);
  useEffect(() => {
    if (!selected) { setViewer(""); setSignatures([]); return; }
    let url = "";
    Promise.all([api<Signature[]>(`/api/submissions/${selected.id}/signatures`), apiBlob(`/api/submissions/${selected.id}/file`)]).then(([sigs, blob]) => {
      setSignatures(sigs); url = URL.createObjectURL(blob); setViewer(url);
    }).catch((e) => setError(e.message));
    return () => { if (url) URL.revokeObjectURL(url); };
  }, [selected?.id]);

  async function approve(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!selected || !file) { setError("Chọn PDF đã được trưởng khoa ký."); return; }
    setBusy(true); setError(""); setNotice("");
    const data = new FormData(); data.set("file", file);
    try { const saved = await api<Transcript>(`/api/approvals/${selected.id}/signed-file`, { method: "POST", headers: { "X-Idempotency-Key": idempotencyKey() }, body: data }); setNotice(`Đã duyệt và lưu trữ ${saved.courseClassCode}.`); setSelected(null); setFile(null); await refresh(); }
    catch (e) { setError(e instanceof Error ? e.message : "Không thể duyệt hồ sơ."); }
    finally { setBusy(false); }
  }

  async function reject() {
    if (!selected || !reason.trim()) { setError("Nhập lý do trước khi từ chối."); return; }
    setBusy(true); setError(""); setNotice("");
    try { await api<Transcript>(`/api/approvals/${selected.id}/reject`, { method: "POST", body: JSON.stringify({ reason }) }); setNotice(`Đã gửi hồ sơ ${selected.courseClassCode} về giảng viên kèm lý do.`); setReason(""); setSelected(null); await refresh(); }
    catch (e) { setError(e instanceof Error ? e.message : "Không thể từ chối hồ sơ."); }
    finally { setBusy(false); }
  }

  async function signSelectedFile() {
    if (!file) { setError("Chọn PDF trước khi ký."); return; }
    setSigning(true); setError(""); setNotice("");
    try { const signed = await signLocally(file); setFile(signed); setNotice("Đã nhận PDF từ signing agent. Hệ thống sẽ xác minh lại cả hai chữ ký."); }
    catch (e) { setError(e instanceof Error ? e.message : "Không kết nối được signing agent. Hãy ký bằng vSignPDF rồi tải bản đã ký lên."); }
    finally { setSigning(false); }
  }

  return <AppShell eyebrow="Không gian trưởng khoa" title="Hồ sơ chờ duyệt" description="Kiểm tra bản PDF đã ký, đối chiếu thông tin và xác minh trước khi lưu trữ.">
    <section className="grid stats-grid"><div className="stat-card"><div className="stat-top">Chờ xử lý <span className="stat-icon">◷</span></div><div className="stat-value">{items.length}</div><div className="stat-foot">Trong phạm vi khoa</div></div><div className="stat-card"><div className="stat-top">Chữ ký GV <span className="stat-icon">✎</span></div><div className="stat-value">{items.filter((i) => i.signatureCount > 0).length}</div><div className="stat-foot">Có chữ ký được ghi nhận</div></div><div className="stat-card"><div className="stat-top">Cần kiểm tra <span className="stat-icon">!</span></div><div className="stat-value">{items.filter((i) => i.signatureCount === 0).length}</div><div className="stat-foot">Thiếu metadata chữ ký</div></div><div className="stat-card"><div className="stat-top">Quy trình <span className="stat-icon">↗</span></div><div className="stat-value" style={{ fontSize: 17, marginTop: 17 }}>Hai cấp ký</div><div className="stat-foot">Tất cả file được kiểm tra lại</div></div></section>
    {error && <div className="alert error" role="alert" style={{ marginBottom: 13 }}>{error}</div>}{notice && <div className="alert success" role="status" style={{ marginBottom: 13 }}>{notice}</div>}
    <section className="grid two-col">
      <div className="panel"><div className="panel-header"><div><h2 className="panel-title">Hàng đợi phê duyệt</h2><div className="panel-subtitle">Sắp xếp theo thời điểm nhận hồ sơ</div></div><span className="token-badge">{items.length} hồ sơ</span></div><div className="table-wrap"><table className="table"><thead><tr><th>Lớp học phần</th><th>Giảng viên</th><th>Đã nhận</th><th></th></tr></thead><tbody>{items.map((item) => <tr key={item.id} style={{ background: selected?.id === item.id ? "#f2f9f6" : undefined }}><td><button className="file-link" style={{ border: 0, background: "none", padding: 0 }} onClick={() => { setSelected(item); setError(""); }}>{item.courseClassCode}</button><div className="table-secondary">{item.subjectName}</div></td><td>{item.lecturerName}</td><td>{date(item.createdAt)}</td><td><span className="token-badge">{item.signatureCount} chữ ký</span></td></tr>)}</tbody></table>{!items.length && <div className="empty-state"><div className="empty-icon">✓</div>Không có hồ sơ nào đang chờ duyệt.</div>}</div></div>
      <div className="panel"><div className="panel-header"><div><h2 className="panel-title">Chi tiết và xác minh</h2><div className="panel-subtitle">{selected ? selected.courseClassCode : "Chọn một hồ sơ trong hàng đợi"}</div></div></div>
        {selected ? <div className="panel-body section-stack"><div className="detail-list"><div className="detail-line"><span>Môn học</span><strong>{selected.subjectName}</strong></div><div className="detail-line"><span>Giảng viên</span><strong>{selected.lecturerName}</strong></div><div className="detail-line"><span>Hash SHA-256</span><strong className="token-badge">{selected.sha256.slice(0, 18)}…</strong></div><div className="detail-line"><span>Tệp</span><strong>{selected.fileName}</strong></div></div>
          <div><div className="panel-title" style={{ marginBottom: 9 }}>Chữ ký được máy chủ ghi nhận</div>{signatures.length ? signatures.map((sig, index) => <div className="detail-line" key={index}><span>{sig.signerType === "LECTURER" ? "Giảng viên" : "Trưởng khoa"}</span><strong>{sig.signerName ?? sig.validationStatus}{sig.validationReason ? ` · ${sig.validationReason}` : ""}</strong></div>) : <div className="alert warning">Chưa có metadata chữ ký được lưu cho hồ sơ này.</div>}</div>
          <div>{viewer ? <iframe title={`Xem trước ${selected.fileName}`} src={viewer} style={{ width: "100%", height: 250, border: "1px solid var(--line)", borderRadius: 10 }} /> : <div className="empty-state">Đang tải bản xem trước…</div>}</div>
          <form className="form-grid" onSubmit={approve}><div className="file-drop"><strong>PDF đã ký bởi trưởng khoa</strong><div className="hint" style={{ marginTop: 4 }}>Ký bằng agent trên máy hoặc bằng vSignPDF; giữ nguyên chữ ký giảng viên</div><input type="file" accept="application/pdf,.pdf" aria-label="Chọn PDF để trưởng khoa ký" onChange={(e) => setFile(e.target.files?.[0] ?? null)} /></div><button className="button secondary" type="button" onClick={signSelectedFile} disabled={busy || signing || !file}>{signing ? "Đang chờ agent…" : "Ký bằng thiết bị trên máy"}</button><button className="button" type="submit" disabled={busy || !file}>{busy ? "Đang kiểm tra…" : "Xác minh và lưu trữ"}</button></form>
          <div className="field"><label htmlFor="reject-reason">Lý do từ chối</label><textarea className="textarea" id="reject-reason" value={reason} onChange={(e) => setReason(e.target.value)} maxLength={2000} placeholder="Ghi rõ nội dung cần giảng viên cập nhật" /><button className="button danger" onClick={reject} disabled={busy || !reason.trim()}>Từ chối và gửi lại</button></div>
        </div> : <div className="empty-state"><div className="empty-icon">▤</div>Chọn một hồ sơ để xem nội dung và nhật ký xác minh.</div>}
      </div>
    </section>
  </AppShell>;
}
