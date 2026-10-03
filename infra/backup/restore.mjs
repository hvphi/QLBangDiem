import { createDecipheriv, createHash } from "node:crypto";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, isAbsolute, relative, resolve, sep } from "node:path";
import { gunzip } from "node:zlib";
import { promisify } from "node:util";
import { fileURLToPath } from "node:url";

const gunzipAsync = promisify(gunzip);
const magic = Buffer.from("QLBDSTG1", "ascii");

function backupKey() {
  const key = Buffer.from(process.env.QLBD_BACKUP_KEY ?? "", "base64");
  if (key.length !== 32) throw new Error("QLBD_BACKUP_KEY must be a base64 encoded 32-byte AES key.");
  return key;
}

export async function restoreBackup({ archive, destination, key = backupKey() } = {}) {
  if (!archive) throw new Error("Pass the encrypted archive path as the first argument.");
  const bytes = await readFile(resolve(archive));
  if (bytes.length < 36 || !bytes.subarray(0, 8).equals(magic)) throw new Error("Backup format is invalid.");
  const iv = bytes.subarray(8, 20); const tag = bytes.subarray(20, 36);
  const decipher = createDecipheriv("aes-256-gcm", key, iv); decipher.setAuthTag(tag);
  const plain = Buffer.concat([decipher.update(bytes.subarray(36)), decipher.final()]);
  const payload = JSON.parse((await gunzipAsync(plain)).toString("utf8"));
  if (payload.format !== 1 || !Array.isArray(payload.files)) throw new Error("Backup manifest is invalid.");
  const target = resolve(destination ?? `.local/restore-${new Date().toISOString().replaceAll(":", "-")}`);
  for (const entry of payload.files) {
    if (typeof entry.path !== "string" || isAbsolute(entry.path) || entry.path.split(/[\\/]/).some((part) => part === ".." || part === "")) {
      throw new Error("Backup contains an unsafe path.");
    }
    const filePath = resolve(target, entry.path);
    const rel = relative(target, filePath);
    if (rel.startsWith(`..${sep}`) || rel === ".." || isAbsolute(rel)) throw new Error("Backup path escapes restore folder.");
    const data = Buffer.from(entry.data, "base64");
    const actual = createHash("sha256").update(data).digest("hex");
    if (data.length !== entry.size || actual !== entry.sha256) throw new Error(`Checksum mismatch: ${entry.path}`);
  }
  await mkdir(target, { recursive: true });
  for (const entry of payload.files) {
    const filePath = resolve(target, entry.path);
    await mkdir(dirname(filePath), { recursive: true });
    await writeFile(filePath, Buffer.from(entry.data, "base64"), { flag: "wx" });
  }
  return { destination: target, files: payload.files.length, sourceCreatedAt: payload.createdAt };
}

if (process.argv[1] && resolve(process.argv[1]) === resolve(fileURLToPath(import.meta.url))) {
  try { process.stdout.write(`${JSON.stringify(await restoreBackup({ archive: process.argv[2], destination: process.argv[3] }))}\n`); }
  catch (error) { process.stderr.write(`${error.message}\n`); process.exitCode = 1; }
}
