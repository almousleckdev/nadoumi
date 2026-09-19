import { describe, it, expect } from 'vitest'
import { validateFile } from '~/utils/files'

const RULES = { mime: /^(image\/(jpeg|png)|application\/pdf)$/, maxMb: 10 }
const MB = 1024 * 1024

describe('validateFile', () => {
  it('accepts an allowed type within the size cap', () => {
    expect(validateFile({ type: 'image/png', size: 2 * MB }, RULES)).toBeNull()
    expect(validateFile({ type: 'application/pdf', size: 10 * MB }, RULES)).toBeNull()
  })

  it('rejects a type that is not allowed', () => {
    expect(validateFile({ type: 'image/gif', size: 1 }, RULES)).toBe('badType')
    expect(validateFile({ type: '', size: 1 }, RULES)).toBe('badType')
  })

  it('rejects a file over the cap', () => {
    expect(validateFile({ type: 'image/png', size: 10 * MB + 1 }, RULES)).toBe('tooBig')
  })
})
