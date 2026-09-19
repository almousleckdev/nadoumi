import { describe, it, expect } from 'vitest'
import { usePanZoom } from '~/composables/usePanZoom'

const pointer = (x: number, y: number) => ({
  clientX: x, clientY: y, pointerId: 1, target: { setPointerCapture: () => undefined },
}) as unknown as PointerEvent

describe('usePanZoom', () => {
  it('starts at rest', () => {
    const { zoom, rotation, offset, transform } = usePanZoom()

    expect(zoom.value).toBe(1)
    expect(rotation.value).toBe(0)
    expect(offset).toEqual({ x: 0, y: 0 })
    expect(transform.value).toBe('translate(0px, 0px) rotate(0deg) scale(1)')
  })

  it('drags the image with the pointer', () => {
    const { offset, onPointerDown, onPointerMove, onPointerUp } = usePanZoom()

    onPointerDown(pointer(10, 10))
    onPointerMove(pointer(40, 25))
    onPointerUp()
    onPointerMove(pointer(200, 200)) // ignored: not dragging any more

    expect(offset).toEqual({ x: 30, y: 15 })
  })

  it('keeps zoom within the limits', () => {
    const { zoom, zoomBy } = usePanZoom({ min: 1, max: 3 })

    zoomBy(5)
    expect(zoom.value).toBe(3)
    zoomBy(-10)
    expect(zoom.value).toBe(1)
  })

  it('rotates in quarter turns and wraps', () => {
    const { rotation, rotate } = usePanZoom()

    rotate(); rotate(); rotate(); rotate()

    expect(rotation.value).toBe(0)
    rotate()
    expect(rotation.value).toBe(90)
  })

  it('reset returns to rest', () => {
    const { zoom, rotation, offset, zoomBy, rotate, onPointerDown, onPointerMove, reset } = usePanZoom()
    zoomBy(1); rotate(); onPointerDown(pointer(0, 0)); onPointerMove(pointer(9, 9))

    reset()

    expect([zoom.value, rotation.value, offset.x, offset.y]).toEqual([1, 0, 0, 0])
  })
})
