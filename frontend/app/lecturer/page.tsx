"use client";

import Link from "next/link";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/shell/app-shell";
import { api, idempotencyKey } from "@/lib/api";
import { signLocally } from "@/features/lecturer/signing";

type Course = { id: string; courseClassCode: string; subjectName: string; departmentName: string; academicYear: string; semester: string; deadlineAt: string; locked: boolean };
type Transcript = { id: string; courseClassCode: string; subjectName: string; fileName: string; status: string; createdAt: string; rejectionReason: string | null; signatureCount: number; sha256: string };
const status = (value: string) => value === "LECTURER_SIGNED" ? ["Chờ trưởng khoa duyệt", "waiting"] : value === "ARCHIVED" ? ["Đã lưu trữ", "archived"] : value === "REJECTED" ? ["Cần xử lý lại", "rejected"] : [value, "unknown"];
const date = (value: string) => new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(value));

export default function LecturerPage() {
  const [classes, setClasses] = useState<Course[]>([]);
  const [submissions, setSubmissions] = useState<Transcript[]>([]);
  const [courseId, setCourseId] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [signing, setSigning] = useState(false);
  const refresh = () => Promise.all([api<Course[]>("/api/catalog/classes"), api<Transcript[]>("/api/submissions")]).then(([c, s]) => { setClasses(c); setSubmissions(s); if (!courseId && c[0]) setCourseId(c[0].id); });
  useEffect(() => { refresh().catch((e) => setError(e.message)); }, []);
  const waiting = useMemo(() => submissions.filter((item) => item.status === "LECTURER_SIGNED").length, [submissions]);
  const archived = useMemo(() => submissions.filter((item) => item.status === "ARCHIVED").length, [submissions]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const formElement = event.currentTarget; setError(""); setMessage("");
    if (!courseId || !file) { setError("Chọn lớp học phần và tệp PDF đã ký số trước khi nộp."); return; }
    const form = new FormData(); form.set("courseClassId", courseId); form.set("file", file);
    setBusy(true);
    try {
      const saved = await api<Transcript>("/api/submissions", { method: "POST", headers: { "X-Idempotency-Key": idempotencyKey() }, body: form });
      setMessage(`Đã tiếp nhận ${saved.courseClassCode}; bảng điểm đang chờ trưởng khoa duyệt.`); setFile(null);
      formElement.reset(); await refresh();
    } catch (e) { setError(e instanceof Error ? e.message : "Không thể nộp bảng điểm."); }
    finally { setBusy(false); }
  }

  async function signSelectedFile() {
    if (!file) { setError("Chọn tệp PDF trước khi yêu cầu ký."); return; }
    setSigning(true); setError(""); setMessage("");
    try { const signed = await signLocally(file); setFile(signed); setMessage("Đã nhận PDF từ signing agent. Máy chủ vẫn sẽ xác minh chữ ký trước khi tiếp nhận."); }
    catch (e) { setError(e instanceof Error ? e.message : "Không kết nối được signing agent. Bạn có thể ký bằng vSignPDF rồi chọn PDF đã ký."); }
    finally { setSigning(false); }
  }

  return <AppShell eyebrow="Không gian giảng viên" title="Bảng điểm của tôi" description="Theo dõi lớp học phần, hạn nộp và trạng thái kiểm tra chữ ký số.">
    <section className="grid stats-grid">
      <div className="stat-card"><div className="stat-top">Lớp học phần <span className="stat-icon">▤</span></div><div className="stat-value">{classes.length}</div><div className="stat-foot">Được phân công trong học kỳ</div></div>
      <div className="stat-card"><div className="stat-top">Chờ duyệt <span className="stat-icon">◷</span></div><div className="stat-value">{waiting}</div><div className="stat-foot">Đang ở bước trưởng khoa</div></div>
      <div className="stat-card"><div className="stat-top">Đã lưu trữ <span className="stat-icon">▣</span></div><div className="stat-value">{archived}</div><div className="stat-foot">Có thể tra cứu lại</div></div>
      <div className="stat-card"><div className="stat-top">Cần xử lý <span className="stat-icon">!</span></div><div className="stat-value">{submissions.filter((item) => item.status === "REJECTED").length}</div><div className="stat-foot">Hồ sơ bị từ chối có lưu lý do</div></div>
    </section>
    <section className="grid two-col">
      <div className="section-stack">
        <div className="panel"><div className="panel-header"><div><h2 className="panel-title">Lớp học phần được giao</h2><div className="panel-subtitle">Danh mục lấy từ phân công học vụ</div></div><span className="token-badge">{classes.length} lớp</span></div>
          <div className="panel-body">{classes.length ? <div className="class-list">{classes.map((item) => <div className="class-row" key={item.id}><div><div className="class-code">{item.courseClassCode} <span className="token-badge">{item.semester} · {item.academicYear}</span></div><div className="class-name">{item.subjectName}</div><div className="class-meta"><span>{item.departmentName}</span><span>Hạn nộp: {date(item.deadlineAt)}</span></div></div><span className="deadline">{item.locked ? "Đã khóa" : "Đang mở nhận"}</span></div>)}</div> : <div className="empty-state"><div className="empty-icon">▤</div>Chưa có lớp học phần trong danh mục.</div>}</div>
        </div>
        <div className="panel"><div className="panel-header"><div><h2 className="panel-title">Hồ sơ gần đây</h2><div className="panel-subtitle">Bảng điểm đã gửi và kết quả xử lý</div></div><Link className="file-link" href="/lecturer/submissions">Xem tất cả →</Link></div>
          <div className="table-wrap"><table className="table"><thead><tr><th>Lớp học phần</th><th>Ngày gửi</th><th>Chữ ký</th><th>Trạng thái</th></tr></thead><tbody>{submissions.slice(0, 5).map((item) => { const [label, cls] = status(item.status); return <tr key={item.id}><td><div className="table-primary">{item.courseClassCode}</div><div className="table-secondary">{item.fileName}</div></td><td>{date(item.createdAt)}</td><td>{item.signatureCount} chữ ký</td><td><span className={`status ${cls}`}>{label}</span>{item.rejectionReason && <div className="table-secondary">{item.rejectionReason}</div>}</td></tr>; })}</tbody></table>{submissions.length === 0 && <div className="empty-state">Chưa có bảng điểm nào được nộp.</div>}</div>
        </div>
      </div>
      <aside className="panel"><div className="panel-header"><div><h2 className="panel-title">Nộp bảng điểm đã ký</h2><div className="panel-subtitle">Tải lên PDF hoàn tất bằng vSignPDF</div></div><span className="stat-icon">↑</span></div><div className="panel-body">
        <div className="alert info" style={{ marginBottom: 15 }}>Tải lên bảng điểm có chữ ký số của giảng viên. Hệ thống kiểm tra tính toàn vẹn và chuyển hồ sơ được tiếp nhận đến trưởng khoa để ký duyệt.</div>
        {error && <div className="alert error" style={{ marginBottom: 12 }} role="alert">{error}</div>}{message && <div className="alert success" style={{ marginBottom: 12 }} role="status">{message}</div>}
        <form className="form-grid" onSubmit={submit}>
          <div className="field"><label htmlFor="course-class">Lớp học phần</label><select className="select" id="course-class" required value={courseId} onChange={(e) => setCourseId(e.target.value)}><option value="">Chọn lớp học phần</option>{classes.map((item) => <option key={item.id} value={item.id} disabled={item.locked}>{item.courseClassCode} — {item.subjectName}</option>)}</select></div>
          <div className="file-drop"><strong>Chọn bảng điểm định dạng PDF</strong><div className="hint" style={{ marginTop: 4 }}>Tối đa 20 MB · ký qua agent trên máy hoặc bằng vSignPDF</div><input aria-label="Chọn tệp PDF" type="file" accept="application/pdf,.pdf" required onChange={(e) => setFile(e.target.files?.[0] ?? null)} /></div>
          {file && <div className="detail-line"><span>Tệp đã chọn</span><strong>{file.name}</strong></div>}
          <button className="button secondary" type="button" onClick={signSelectedFile} disabled={signing || busy || !file}>{signing ? "Đang chờ agent…" : "Ký bằng thiết bị trên máy"}</button>
          <div className="button-row"><button className="button" type="submit" disabled={busy || classes.length === 0}>{busy ? "Đang kiểm tra…" : "Gửi để xác minh"}</button><button className="button secondary" type="reset" onClick={() => { setFile(null); setError(""); setMessage(""); }}>Xóa lựa chọn</button></div>
          <div className="hint">Chưa có thiết bị ký? Hãy xuất PDF và ký bằng vSignPDF theo quy trình của trường, sau đó tải bản đã ký lên đây.</div>
        </form>
      </div></aside>
    </section>
  </AppShell>;
}
