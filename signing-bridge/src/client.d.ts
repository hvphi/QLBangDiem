export type SignConsent = (context: { fileName: string }) => boolean | Promise<boolean>;
export type SigningAgentErrorCode = "PDF_REQUIRED" | "CONSENT_REQUIRED" | "AGENT_UNAVAILABLE" | "NO_COMPATIBLE_TOKEN" | "AGENT_DISCONNECTED" | "SIGNING_FAILED" | "INVALID_AGENT_RESPONSE";

export class SigningAgentError extends Error {
  code: SigningAgentErrorCode;
}

export function probeSigningAgent(options?: { baseUrl?: string; fetchImpl?: typeof fetch; timeoutMs?: number }): Promise<{
  available: boolean; reason?: string; version?: string; tokenPresent?: boolean;
}>;

export function signPdf(file: File, options: { confirm: SignConsent; baseUrl?: string; fetchImpl?: typeof fetch }): Promise<File>;
