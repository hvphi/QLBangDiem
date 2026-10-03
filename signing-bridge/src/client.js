const DEFAULT_AGENT = "http://127.0.0.1:49152";

export class SigningAgentError extends Error {
  constructor(code, message) { super(message); this.name = "SigningAgentError"; this.code = code; }
}

export async function probeSigningAgent({ baseUrl = DEFAULT_AGENT, fetchImpl = fetch, timeoutMs = 1200 } = {}) {
  try {
    const response = await fetchImpl(`${baseUrl}/v1/health`, { signal: AbortSignal.timeout(timeoutMs) });
    if (!response.ok) return { available: false, reason: "AGENT_UNAVAILABLE" };
    const health = await response.json();
    return health?.ready === true
      ? { available: true, version: health.version ?? "unknown", tokenPresent: health.tokenPresent === true }
      : { available: false, reason: "NO_COMPATIBLE_TOKEN" };
  } catch {
    return { available: false, reason: "AGENT_UNAVAILABLE" };
  }
}

export async function signPdf(file, { confirm, baseUrl = DEFAULT_AGENT, fetchImpl = fetch } = {}) {
  if (!(file instanceof Blob) || file.type !== "application/pdf") {
    throw new SigningAgentError("PDF_REQUIRED", "Chỉ hỗ trợ tệp PDF.");
  }
  if (typeof confirm !== "function" || !(await confirm({ fileName: file.name ?? "transcript.pdf" }))) {
    throw new SigningAgentError("CONSENT_REQUIRED", "Người dùng đã hủy thao tác ký.");
  }
  const status = await probeSigningAgent({ baseUrl, fetchImpl });
  if (!status.available) {
    throw new SigningAgentError(status.reason, "Không tìm thấy signing agent tương thích. Hãy ký bằng vSignPDF rồi tải PDF lên hệ thống.");
  }
  const data = new FormData();
  data.set("file", file, file.name ?? "transcript.pdf");
  let response;
  try { response = await fetchImpl(`${baseUrl}/v1/sign`, { method: "POST", body: data, signal: AbortSignal.timeout(120000) }); }
  catch { throw new SigningAgentError("AGENT_DISCONNECTED", "Signing agent đã mất kết nối. Hãy dùng vSignPDF để hoàn tất thao tác."); }
  if (!response.ok) {
    const code = response.headers.get("X-Agent-Error") ?? "SIGNING_FAILED";
    throw new SigningAgentError(code, "Không thể ký bằng token. Kiểm tra token/PIN trong cửa sổ của agent hoặc dùng vSignPDF.");
  }
  const signed = await response.blob();
  if (signed.type !== "application/pdf" || signed.size === 0) {
    throw new SigningAgentError("INVALID_AGENT_RESPONSE", "Agent không trả về PDF đã ký hợp lệ.");
  }
  return new File([signed], file.name ?? "transcript-signed.pdf", { type: "application/pdf" });
}
