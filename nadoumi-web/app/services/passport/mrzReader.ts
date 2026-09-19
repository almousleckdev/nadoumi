import { MRZ_PASSES, type MrzPass } from '~/constants/passport'
import { parsePassportMrz } from './mrz'
import type { PassportReader } from './types'

export interface MrzReaderDeps {
  /** OCR one crop of the image and return its text. */
  recognize: (image: Blob, pass: MrzPass) => Promise<string>
  /** Passes to try, in order. */
  passes?: readonly MrzPass[]
}

/**
 * A passport reader that OCRs the machine-readable zone and accepts a result only when its
 * check digits validate. It tries the passes in order and returns `null` (never a guess) when
 * none reads cleanly. The OCR engine is injected so it is swappable and testable.
 */
export function createMrzReader({ recognize, passes = MRZ_PASSES }: MrzReaderDeps): PassportReader {
  return {
    async read(image) {
      for (const pass of passes) {
        const reading = parsePassportMrz(await recognize(image, pass))
        if (reading) return reading
      }
      return null
    },
  }
}
