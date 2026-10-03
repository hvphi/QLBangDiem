import test from "node:test";
import assert from "node:assert/strict";
import { probeSigningAgent, signPdf, SigningAgentError } from "./client.js";

test("missing local agent yields a clear vSignPDF fallback", async () => {
  const result = await probeSigningAgent({ fetchImpl: async () => { throw new Error("offline"); } });
  assert.deepEqual(result, { available: false, reason: "AGENT_UNAVAILABLE" });
});

test("cancelled consent sends no PDF to the agent", async () => {
  let calls = 0;
  const fakeFetch = async () => { calls += 1; return new Response(JSON.stringify({ ready: true }), { headers: { "Content-Type": "application/json" } }); };
  await assert.rejects(() => signPdf(new File(["%PDF-1.7"], "grades.pdf", { type: "application/pdf" }), { confirm: async () => false, fetchImpl: fakeFetch }),
    (error) => error instanceof SigningAgentError && error.code === "CONSENT_REQUIRED");
  assert.equal(calls, 0);
});
