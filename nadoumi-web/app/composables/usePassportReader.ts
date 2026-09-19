import { createMrzReader } from '~/services/passport/mrzReader'
import { createTesseractSession } from '~/services/passport/tesseractOcr'
import type { PassportReader } from '~/services/passport/types'

/**
 * The passport reader used by onboarding: the in-browser MRZ reader. A server-side reader can
 * replace it here without touching the components. Each read owns one OCR worker, ended when done.
 */
export function usePassportReader(): PassportReader {
  return {
    async read(image) {
      const ocr = createTesseractSession()
      try {
        return await createMrzReader({ recognize: ocr.recognize }).read(image)
      }
      finally {
        await ocr.dispose()
      }
    },
  }
}
