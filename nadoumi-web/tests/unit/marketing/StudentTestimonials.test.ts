import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import StudentTestimonials from '~/components/marketing/StudentTestimonials.vue'
import { testimonials } from '~/data/testimonials'

describe('student testimonials', () => {
  it('has three stories whose photos exist as optimized site assets', () => {
    expect(testimonials).toHaveLength(3)
    for (const s of testimonials) {
      expect(s.image).toMatch(/^\/students\/student-\d\.jpg$/)
      expect(s.quote.length).toBeGreaterThan(100)
    }
  })

  it('shows the first story with its portrait, quote in the student\'s own language and route', async () => {
    const w = await mountSuspended(StudentTestimonials)
    expect(w.find('[data-test="testimonial-photo"]').attributes('src')).toBe('/students/student-1.jpg')
    expect(w.find('[data-test="testimonial-quote"]').text()).toContain('deepest gratitude to NADOUMI')
    expect(w.find('[data-test="testimonial-name"]').text()).toBe('Nadoumi student')
    expect(w.text()).toContain('Chad')
    expect(w.text()).toContain('China')
  })

  it('steps through the stories with next and previous, wrapping around', async () => {
    const w = await mountSuspended(StudentTestimonials)
    await w.find('[data-test="testimonial-next"]').trigger('click')
    expect(w.find('[data-test="testimonial-quote"]').attributes('lang')).toBe('fr')
    expect(w.find('[data-test="testimonial-quote"]').text()).toContain('profonde gratitude')
    await w.find('[data-test="testimonial-next"]').trigger('click')
    expect(w.find('[data-test="testimonial-name"]').text()).toBe('Fru Schinaylla')
    await w.find('[data-test="testimonial-next"]').trigger('click')
    expect(w.find('[data-test="testimonial-photo"]').attributes('src')).toBe('/students/student-1.jpg')
    await w.find('[data-test="testimonial-prev"]').trigger('click')
    expect(w.find('[data-test="testimonial-name"]').text()).toBe('Fru Schinaylla')
  })

  it('jumps straight to a story from its thumbnail', async () => {
    const w = await mountSuspended(StudentTestimonials)
    await w.findAll('[data-test="testimonial-thumb"]')[2]!.trigger('click')
    expect(w.find('[data-test="testimonial-name"]').text()).toBe('Fru Schinaylla')
    expect(w.findAll('[data-test="testimonial-thumb"]')[2]!.attributes('aria-selected')).toBe('true')
  })

  it('clamps a long quote behind a read-more toggle, and collapses it again on the next story', async () => {
    const w = await mountSuspended(StudentTestimonials)
    const toggle = () => w.find('[data-test="testimonial-toggle"]')
    expect(toggle().attributes('aria-expanded')).toBe('false')
    await toggle().trigger('click')
    expect(toggle().attributes('aria-expanded')).toBe('true')
    await w.find('[data-test="testimonial-next"]').trigger('click')
    expect(toggle().attributes('aria-expanded')).toBe('false')
    await w.findAll('[data-test="testimonial-thumb"]')[2]!.trigger('click')
    expect(w.find('[data-test="testimonial-quote"]').text()).toContain('thoughtful gift')
  })

  it('answers the arrow keys', async () => {
    const w = await mountSuspended(StudentTestimonials)
    await w.find('[role="group"]').trigger('keydown', { key: 'ArrowRight' })
    expect(w.find('[data-test="testimonial-quote"]').attributes('lang')).toBe('fr')
  })
})
