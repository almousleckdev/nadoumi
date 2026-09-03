import { describe, it, expect, vi } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import Carousel from '~/components/ui/Carousel.vue'

describe('Carousel', () => {
  it('is a keyboard-reachable labelled group and scrolls on arrow keys', async () => {
    const w = await mountSuspended(Carousel, {
      props: { label: 'Featured universities' },
      slots: { default: () => '<div style="width:300px">a</div><div style="width:300px">b</div>' },
    })
    const track = w.get('[role="group"]')
    expect(track.attributes('aria-label')).toBe('Featured universities')
    expect(track.attributes('tabindex')).toBe('0')

    const el = track.element as HTMLElement
    const spy = vi.spyOn(el, 'scrollBy').mockImplementation(() => {})
    await track.trigger('keydown', { key: 'ArrowRight' })
    expect(spy).toHaveBeenCalledOnce()
    await track.trigger('keydown', { key: 'ArrowLeft' })
    expect(spy).toHaveBeenCalledTimes(2)
  })

  it('exposes scrollPrev / scrollNext / edge state', async () => {
    const w = await mountSuspended(Carousel, {
      props: { label: 'x' },
      slots: { default: () => '<div>a</div>' },
    })
    const vm = w.vm as unknown as { scrollPrev: unknown, scrollNext: unknown, atStart: boolean }
    expect(typeof vm.scrollPrev).toBe('function')
    expect(typeof vm.scrollNext).toBe('function')
    expect(vm.atStart).toBe(true)
  })
})
