export type FileProblem = 'badType' | 'tooBig'

const BYTES_PER_MB = 1024 * 1024

/** Checks a picked file against an allowed type pattern and a size cap; `null` means acceptable. */
export function validateFile(file: Pick<File, 'type' | 'size'>, rules: { mime: RegExp, maxMb: number }): FileProblem | null {
  if (!rules.mime.test(file.type)) return 'badType'
  if (file.size > rules.maxMb * BYTES_PER_MB) return 'tooBig'
  return null
}

export function readAsDataUrl(file: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })
}

/** Natural size of an image; `{0, 0}` when it cannot be decoded. */
export function imageSize(src: string): Promise<{ width: number, height: number }> {
  return new Promise((resolve) => {
    const image = new Image()
    image.onload = () => resolve({ width: image.naturalWidth, height: image.naturalHeight })
    image.onerror = () => resolve({ width: 0, height: 0 })
    image.src = src
  })
}

export async function dataUrlToBlob(dataUrl: string): Promise<Blob> {
  return (await fetch(dataUrl)).blob()
}
