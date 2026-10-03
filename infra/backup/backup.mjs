import { createCipheriv, createHash, randomBytes } from "node:crypto";
import { mkdir, readFile, readdir, rename, writeFile } from "node:fs/promises";
import { spawnSync } from "node:child_process";
import { basename, dirname, join, relative, resolve, sep } from "node:path";
import { gzip } from "node:zlib";
import { promisify } from "node:util";
import { fileURLToPath } from "node:url";

const gzipAsync = promisify(gzip);
const magic = Buffer.from("QLBDSTG1", "ascii");

function backupKey() {
  const key = Buffer.from(process.env.QLBD_BACKUP_KEY ?? "", "base64");
  if (key.length !== 32) throw new Error("QLBD_BACKUP_KEY must be a base64 encoded 32-byte AES key.");
  return key;
}

async function walk(root, current = root) {
  const entries = await readdir(current, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    if (entry.name === "backups" || entry.name.startsWith(".")) continue;
    const full = join(current, entry.name);
    if (entry.isDirectory()) files.push(...await walk(root, full));
    else if (entry.isFile()) files.push(full);
  }
  return files;
}

export async function createBackup({ source, output, key = backupKey(), remote = process.env.QLBD_BACKUP_REMOTE } = {}) {
  const root = resolve(source ?? ".local");
  const destination = resolve(output ?? join(root, "backups", `qlbd-${new Date().toISOString().replaceAll(":", "-")}.qlbackup`));
  const paths = await walk(root);
  if (!paths.length) throw new Error("No local data files were found to back up.");
  const files = [];
  for (const filePath of paths) {
    const bytes = await readFile(filePath);
    files.push({ path: relative(root, filePath).split(sep).join("/"), size: bytes.length,
      sha256: createHash("sha256").update(bytes).digest("hex"), data: bytes.toString("base64") });
  }
  const payload = await gzipAsync(Buffer.from(JSON.stringify({ format: 1, createdAt: new Date().toISOString(), files })));
  const iv = randomBytes(12);
  const cipher = createCipheriv("aes-256-gcm", key, iv);
  const encrypted = Buffer.concat([cipher.update(payload), cipher.final()]);
  const packed = Buffer.concat([magic, iv, cipher.getAuthTag(), encrypted]);
  await mkdir(dirname(destination), { recursive: true });
  const temp = `${destination}.${process.pid}.tmp`;
  await writeFile(temp, packed, { flag: "wx" });
  await rename(temp, destination);
  if (remote) {
    const target = `${remote.replace(/\/$/, "")}/${basename(destination)}`;
    const copied = spawnSync("rclone", ["copyto", destination, target], { stdio: "ignore", shell: false });
    if (copied.error || copied.status !== 0) throw new Error("Encrypted backup was created locally, but the configured rclone copy failed.");
  }
  return { destination, files: files.length, encryptedBytes: packed.length, remote: Boolean(remote) };
}

if (process.argv[1] && resolve(process.argv[1]) === resolve(fileURLToPath(import.meta.url))) {
  try { process.stdout.write(`${JSON.stringify(await createBackup())}\n`); }
  catch (error) { process.stderr.write(`${error.message}\n`); process.exitCode = 1; }
}
