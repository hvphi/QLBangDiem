import test from "node:test";
import assert from "node:assert/strict";
import { mkdtemp, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { randomBytes } from "node:crypto";
import { createBackup } from "../../infra/backup/backup.mjs";
import { restoreBackup } from "../../infra/backup/restore.mjs";

test("encrypted backup restores the original file after checksum validation", async () => {
  const root = await mkdtemp(join(tmpdir(), "qlbd-backup-"));
  const source = join(root, "source"); const restore = join(root, "restore"); const backup = join(root, "stage.qlbackup");
  const key = randomBytes(32); const original = Buffer.from("immutable local staging fixture");
  try {
    await mkdir(join(source, "2026-2027", "HK1"), { recursive: true });
    await writeFile(join(source, "2026-2027", "HK1", "sample.pdf"), original);
    const created = await createBackup({ source, output: backup, key, remote: "" });
    assert.equal(created.files, 1);
    const result = await restoreBackup({ archive: backup, destination: restore, key });
    assert.equal(result.files, 1);
    assert.deepEqual(await readFile(join(restore, "2026-2027", "HK1", "sample.pdf")), original);
  } finally { await rm(root, { recursive: true, force: true }); }
});

test("encrypted backup rejects a wrong key", async () => {
  const root = await mkdtemp(join(tmpdir(), "qlbd-backup-key-"));
  const source = join(root, "source"); const archive = join(root, "stage.qlbackup");
  try {
    await mkdir(source); await writeFile(join(source, "one.txt"), "data");
    await createBackup({ source, output: archive, key: randomBytes(32), remote: "" });
    await assert.rejects(() => restoreBackup({ archive, destination: join(root, "restore"), key: randomBytes(32) }));
  } finally { await rm(root, { recursive: true, force: true }); }
});
