/**
 * Pan, zoom and rotate state for an image inside a frame, with the pointer handlers that drag it.
 * Shared by the photo cropper and the document preview so neither re-implements it.
 */
export function usePanZoom(limits: { min?: number, max?: number } = {}) {
  const { min = 1, max = 4 } = limits
  const zoom = ref(1)
  const rotation = ref(0)
  const offset = reactive({ x: 0, y: 0 })
  const dragging = ref(false)
  let grab = { x: 0, y: 0 }

  const transform = computed(() =>
    `translate(${offset.x}px, ${offset.y}px) rotate(${rotation.value}deg) scale(${zoom.value})`)

  function onPointerDown(e: PointerEvent) {
    dragging.value = true
    grab = { x: e.clientX - offset.x, y: e.clientY - offset.y }
    ;(e.target as HTMLElement).setPointerCapture(e.pointerId)
  }

  function onPointerMove(e: PointerEvent) {
    if (!dragging.value) return
    offset.x = e.clientX - grab.x
    offset.y = e.clientY - grab.y
  }

  function onPointerUp() {
    dragging.value = false
  }

  /** Zoom by `delta`, kept within the limits. */
  function zoomBy(delta: number) {
    zoom.value = Math.min(max, Math.max(min, +(zoom.value + delta).toFixed(2)))
  }

  function rotate() {
    rotation.value = (rotation.value + 90) % 360
  }

  function reset() {
    zoom.value = 1
    rotation.value = 0
    offset.x = 0
    offset.y = 0
  }

  return { zoom, rotation, offset, transform, onPointerDown, onPointerMove, onPointerUp, zoomBy, rotate, reset }
}
