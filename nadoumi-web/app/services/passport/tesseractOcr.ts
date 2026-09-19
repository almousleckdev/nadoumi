import { MRZ_CHARSET, OCR_ASSET_BASE, type MrzPass } from '~/constants/passport'

/**
 * Browser OCR for crops of a passport image. Browser-only: it needs canvas and web workers.
 * tesseract.js is imported lazily so it costs nothing until a passport is read, and its worker,
 * core and language data come from our own origin (`OCR_ASSET_BASE`), never a public CDN.
 * The image never leaves the browser. One worker serves every pass of a read; `dispose` ends it.
 */
export function createTesseractSession() {
  let workerPromise: ReturnType<typeof startWorker> | null = null

  return {
    async recognize(image: Blob, pass: MrzPass): Promise<string> {
      workerPromise ??= startWorker()
      const worker = await workerPromise
      const { data } = await worker.recognize(await cropBand(image, pass))
      return data.text
    },
    async dispose(): Promise<void> {
      if (workerPromise) await (await workerPromise).terminate()
      workerPromise = null
    },
  }
}

async function startWorker() {
  const { createWorker, OEM, PSM } = await import('tesseract.js')
  const worker = await createWorker('eng', OEM.LSTM_ONLY, {
    workerPath: `${OCR_ASSET_BASE}/worker.min.js`,
    corePath: OCR_ASSET_BASE,
    langPath: OCR_ASSET_BASE,
    workerBlobURL: false,
    gzip: true,
  })
  await worker.setParameters({ tessedit_char_whitelist: MRZ_CHARSET, tessedit_pageseg_mode: PSM.SINGLE_BLOCK })
  return worker
}

/** The bottom `band` of the image, greyscaled with more contrast and scaled to `width`. */
async function cropBand(image: Blob, { band, width }: MrzPass): Promise<HTMLCanvasElement> {
  const bitmap = await createImageBitmap(image)
  const sourceHeight = Math.round(bitmap.height * band)
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = Math.round(sourceHeight * (width / bitmap.width))
  const context = canvas.getContext('2d')
  if (!context) throw new Error('canvas is not available')
  context.filter = 'grayscale(1) contrast(1.4)'
  context.drawImage(bitmap, 0, bitmap.height - sourceHeight, bitmap.width, sourceHeight, 0, 0, canvas.width, canvas.height)
  bitmap.close()
  return canvas
}
