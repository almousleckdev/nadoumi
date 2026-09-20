import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import ImageCropper from '~/components/onboarding/ImageCropper.vue'

const { imageSize } = vi.hoisted(() => ({ imageSize: vi.fn() }))
vi.mock('~/utils/files', async (original) => ({
  ...(await original<typeof import('~/utils/files')>()),
  imageSize,
}))

/** A fake 2D context so apply() has something to call scale/translate/drawImage on. */
function stubCanvasContext() {
  const ctx = { fillStyle: '', fillRect: vi.fn(), translate: vi.fn(), rotate: vi.fn(), scale: vi.fn(), drawImage: vi.fn() }
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(ctx as unknown as CanvasRenderingContext2D)
  vi.spyOn(HTMLCanvasElement.prototype, 'toDataURL').mockReturnValue('data:image/jpeg;base64,CROPPED')
  return ctx
}

beforeEach(() => {
  imageSize.mockReset()
  vi.restoreAllMocks()
})

describe('ImageCropper', () => {
  it('starts zoomed to fill the frame, not letterboxed and not at native pixel size', async () => {
    // A portrait phone photo, much larger than the square 240x240 crop frame.
    imageSize.mockResolvedValue({ width: 3000, height: 4000 })
    const w = await mountSuspended(ImageCropper, { props: { src: 'data:image/png;base64,AAAA', aspect: 1, size: 240 } })
    await flushPromises()

    // Cover-fit: the larger of the two frame/image ratios wins, so the image fills
    // (and slightly overflows on one axis) the 240x240 frame instead of leaving a gap.
    const expectedZoom = 240 / 3000
    const slider = w.find('input[type="range"]').element as HTMLInputElement
    expect(Number(slider.value)).toBeCloseTo(expectedZoom, 5)
    expect(Number(slider.min)).toBeCloseTo(expectedZoom / 2, 5)
    expect(Number(slider.max)).toBeCloseTo(expectedZoom * 4, 5)
  })

  it('exports the crop at the same scale the preview showed, not the raw pixel size', async () => {
    imageSize.mockResolvedValue({ width: 3000, height: 4000 })
    const ctx = stubCanvasContext()
    const w = await mountSuspended(ImageCropper, { props: { src: 'data:image/png;base64,AAAA', aspect: 1, size: 240 } })
    await flushPromises()

    await w.findAll('button').find(b => b.text() === 'Use this crop')!.trigger('click')
    await flushPromises()

    const expectedZoom = 240 / 3000
    expect(ctx.scale).toHaveBeenCalledWith(expectedZoom, expectedZoom)
    expect(w.emitted('crop')?.[0]?.[0]).toBe('data:image/jpeg;base64,CROPPED')
  })

  it('resets back to the fill-frame zoom, not to 1', async () => {
    imageSize.mockResolvedValue({ width: 3000, height: 4000 })
    const w = await mountSuspended(ImageCropper, { props: { src: 'data:image/png;base64,AAAA', aspect: 1, size: 240 } })
    await flushPromises()

    const slider = w.find('input[type="range"]')
    await slider.setValue(0.5)
    await w.findAll('button').find(b => b.text() === 'Reset')!.trigger('click')

    const expectedZoom = 240 / 3000
    expect(Number((slider.element as HTMLInputElement).value)).toBeCloseTo(expectedZoom, 5)
  })
})
