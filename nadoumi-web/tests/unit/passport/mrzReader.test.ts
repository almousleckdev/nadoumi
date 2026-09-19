import { describe, it, expect, vi } from 'vitest'
import { MRZ_PASSES } from '~/constants/passport'
import { createMrzReader } from '~/services/passport/mrzReader'

const MRZ = 'P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<\nL898902C36UTO7408122F1204159ZE184226B<<<<<10'
const image = new Blob(['x'], { type: 'image/png' })

describe('createMrzReader', () => {
  it('returns the reading from the first crop that reads cleanly', async () => {
    const recognize = vi.fn().mockResolvedValue(MRZ)

    const reading = await createMrzReader({ recognize }).read(image)

    expect(reading?.surname).toBe('ERIKSSON')
    expect(recognize).toHaveBeenCalledTimes(1)
    expect(recognize).toHaveBeenCalledWith(image, MRZ_PASSES[0])
  })

  it('moves on to the next pass when one does not read', async () => {
    const recognize = vi.fn().mockResolvedValueOnce('garbage').mockResolvedValueOnce(MRZ)

    const reading = await createMrzReader({ recognize }).read(image)

    expect(reading?.givenNames).toBe('ANNA MARIA')
    expect(recognize).toHaveBeenNthCalledWith(2, image, MRZ_PASSES[1])
  })

  it('returns null, never a guess, when no pass validates', async () => {
    const recognize = vi.fn().mockResolvedValue('P<UTOERIKSSON<<ANNA\nL898902C37UTO7408122F1204159ZE184226B<<<<<10')

    expect(await createMrzReader({ recognize }).read(image)).toBeNull()
    expect(recognize).toHaveBeenCalledTimes(MRZ_PASSES.length)
  })
})
