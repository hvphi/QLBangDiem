import { signPdf } from "qlbangdiem-signing-bridge";

export function signLocally(file: File) {
  return signPdf(file, {
    confirm: ({ fileName }) => window.confirm(`Cho phép signing agent ký duyệt tệp “${fileName}”?\n\nPIN được nhập trong cửa sổ của thiết bị ký.`),
  });
}
