import { getDemoRole } from "@/lib/auth/session";

function apiBase() {
  if (process.env.NEXT_PUBLIC_API_BASE !== undefined) return process.env.NEXT_PUBLIC_API_BASE;
  if (typeof window === "undefined") return "http://127.0.0.1:8080";
  return window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1"
    ? "http://127.0.0.1:8080"
    : window.location.origin;
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const role = getDemoRole();
  if (role) headers.set("X-Demo-Role", role);
  if (init.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const response = await fetch(`${apiBase()}${path}`, { ...init, headers, cache: "no-store", credentials: process.env.NEXT_PUBLIC_LOCAL_DEMO === "true" ? "omit" : "same-origin" });
  if (!response.ok) {
    let message = `Yêu cầu thất bại (${response.status}).`;
    try {
      const body = await response.json();
      message = body.detail ?? body.message ?? message;
    } catch { /* keep the status based message */ }
    throw new Error(message);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export function idempotencyKey() {
  return typeof crypto !== "undefined" && "randomUUID" in crypto ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`;
}

export async function apiBlob(path: string) {
  const headers = new Headers();
  const role = getDemoRole();
  if (role) headers.set("X-Demo-Role", role);
  const response = await fetch(`${apiBase()}${path}`, { headers, cache: "no-store" });
  if (!response.ok) throw new Error(`Không thể tải tệp (${response.status}).`);
  return response.blob();
}
