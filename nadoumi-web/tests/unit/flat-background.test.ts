import { describe, it, expect } from 'vitest'
import { knockOutFlatBackground } from '~/utils/flatBackground'

function image(width: number, height: number, fill: [number, number, number]) {
  const px = new Uint8ClampedArray(width * height * 4)
  for (let i = 0; i < px.length; i += 4) {
    px[i] = fill[0]
    px[i + 1] = fill[1]
    px[i + 2] = fill[2]
    px[i + 3] = 255
  }
  return px
}

function setPixel(px: Uint8ClampedArray, width: number, x: number, y: number, rgb: [number, number, number]) {
  const i = (y * width + x) * 4
  px[i] = rgb[0]
  px[i + 1] = rgb[1]
  px[i + 2] = rgb[2]
  px[i + 3] = 255
}

const alphaAt = (px: Uint8ClampedArray, width: number, x: number, y: number) => px[(y * width + x) * 4 + 3]

describe('knockOutFlatBackground', () => {
  it('makes a flat background transparent and keeps the foreground', () => {
    const px = image(5, 5, [255, 255, 255])
    setPixel(px, 5, 2, 2, [200, 0, 0])
    expect(knockOutFlatBackground(px, 5, 5)).toBe(true)
    expect(alphaAt(px, 5, 0, 0)).toBe(0)
    expect(alphaAt(px, 5, 4, 4)).toBe(0)
    expect(alphaAt(px, 5, 2, 2)).toBe(255)
  })

  it('feathers colours that are close to the background', () => {
    const px = image(5, 5, [255, 255, 255])
    setPixel(px, 5, 2, 2, [230, 230, 230])
    expect(knockOutFlatBackground(px, 5, 5)).toBe(true)
    const alpha = alphaAt(px, 5, 2, 2)!
    expect(alpha).toBeGreaterThan(0)
    expect(alpha).toBeLessThan(255)
  })

  it('leaves a photo alone when the corners disagree', () => {
    const px = image(5, 5, [255, 255, 255])
    setPixel(px, 5, 0, 0, [0, 0, 0])
    const before = Uint8ClampedArray.from(px)
    expect(knockOutFlatBackground(px, 5, 5)).toBe(false)
    expect(px).toEqual(before)
  })

  it('does nothing for an empty image', () => {
    expect(knockOutFlatBackground(new Uint8ClampedArray(0), 0, 0)).toBe(false)
  })
})
