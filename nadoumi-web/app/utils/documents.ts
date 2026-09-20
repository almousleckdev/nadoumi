const KB = 1024
const MB = KB * 1024

/** "850 KB" / "1.2 MB": a file size for a version summary. */
export function formatFileSize(bytes: number): string {
  if (bytes >= MB) return `${(bytes / MB).toFixed(1)} MB`
  return `${Math.max(1, Math.round(bytes / KB))} KB`
}

/** Hands a downloaded blob to the browser as a saved file. */
export function saveBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}
