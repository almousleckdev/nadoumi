type Rgb = [number, number, number]

const CORNER_TOLERANCE = 40
const HARD_THRESHOLD = 45
const SOFT_THRESHOLD = 110

const distance = (a: Rgb, b: Rgb) => Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2])

export function knockOutFlatBackground(pixels: Uint8ClampedArray, width: number, height: number): boolean {
  if (!width || !height || pixels.length < width * height * 4) return false

  const rgbAt = (x: number, y: number): Rgb => {
    const i = (y * width + x) * 4
    return [pixels[i] ?? 0, pixels[i + 1] ?? 0, pixels[i + 2] ?? 0]
  }
  const corners = [rgbAt(0, 0), rgbAt(width - 1, 0), rgbAt(0, height - 1), rgbAt(width - 1, height - 1)]
  const key = corners[0]!
  if (!corners.every(corner => distance(corner, key) < CORNER_TOLERANCE)) return false

  for (let i = 0; i < pixels.length; i += 4) {
    const d = distance([pixels[i] ?? 0, pixels[i + 1] ?? 0, pixels[i + 2] ?? 0], key)
    if (d <= HARD_THRESHOLD) pixels[i + 3] = 0
    else if (d < SOFT_THRESHOLD) pixels[i + 3] = Math.round(((pixels[i + 3] ?? 255) * (d - HARD_THRESHOLD)) / (SOFT_THRESHOLD - HARD_THRESHOLD))
  }
  return true
}
