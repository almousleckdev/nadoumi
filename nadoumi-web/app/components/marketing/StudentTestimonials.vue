<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { testimonials, type Testimonial } from '~/data/testimonials'

/**
 * Student stories: one featured story at a time (portrait and quote), with previous/next and photo thumbnails to
 * jump between them. The quotes stay in the language the student wrote them in. Long ones are clamped with a
 * "read more" toggle so the section keeps a steady height.
 */
const { t, locale } = useI18n()

const index = ref(0)
const expanded = ref(false)
const current = computed<Testimonial>(() => testimonials[index.value]!)
const paragraphs = computed(() => current.value.quote.split('\n\n'))
const LONG_QUOTE_CHARS = 420
const isLong = computed(() => current.value.quote.length > LONG_QUOTE_CHARS)

watch(index, () => { expanded.value = false })

function go(to: number) {
  index.value = (to + testimonials.length) % testimonials.length
}

function flag(region: string): string {
  return String.fromCodePoint(...[...region.toUpperCase()].map(c => 0x1F1E6 + c.charCodeAt(0) - 65))
}
function country(region: string): string {
  try {
    return new Intl.DisplayNames([locale.value], { type: 'region' }).of(region) ?? region
  }
  catch {
    return region
  }
}
const route = computed(() => `${flag(current.value.from)} ${country(current.value.from)} → ${flag(current.value.to)} ${country(current.value.to)}`)

function onKey(e: KeyboardEvent) {
  const rtl = document.documentElement.dir === 'rtl'
  if (e.key === 'ArrowRight') go(index.value + (rtl ? -1 : 1))
  else if (e.key === 'ArrowLeft') go(index.value + (rtl ? 1 : -1))
}
</script>

<template>
  <section class="bg-white" :aria-label="t('home.testimonials.title')" data-test="testimonials">
    <NContainer>
      <div class="py-16 sm:py-20">
        <SectionHeading
          :eyebrow="t('home.testimonials.eyebrow')"
          :title="t('home.testimonials.title')"
          :description="t('home.testimonials.description')"
        />

        <div
          class="mt-10 grid items-center gap-8 lg:grid-cols-[minmax(0,22rem)_minmax(0,1fr)] lg:gap-14"
          role="group"
          aria-roledescription="carousel"
          tabindex="0"
          @keydown="onKey"
        >
          <figure class="relative mx-auto w-full max-w-xs lg:max-w-none">
            <span class="absolute -bottom-3 -end-3 h-full w-full rounded-3xl bg-brand-100" aria-hidden="true" />
            <img
              :key="current.id"
              :src="current.image"
              :alt="t('home.testimonials.photoAlt', { name: current.name ?? t('home.testimonials.anonymous') })"
              width="675"
              height="900"
              loading="lazy"
              decoding="async"
              class="relative aspect-[4/5] w-full rounded-3xl object-cover object-[50%_35%] shadow-lg"
              data-test="testimonial-photo"
            >
          </figure>

          <div :key="current.id" aria-live="polite">
            <svg viewBox="0 0 48 48" class="h-10 w-10 text-brand-500" fill="currentColor" aria-hidden="true">
              <path d="M19.5 10C12 13 7 19.5 7 28v10h14V26h-7c0-5 2.5-8.5 7-10.5L19.5 10Zm21 0C33 13 28 19.5 28 28v10h14V26h-7c0-5 2.5-8.5 7-10.5L40.5 10Z" />
            </svg>
            <blockquote
              class="mt-4 grid gap-4 text-lg leading-relaxed text-slate-700"
              :class="isLong && !expanded ? 'max-h-72 overflow-hidden [mask-image:linear-gradient(to_bottom,black_75%,transparent)]' : ''"
              :lang="current.lang"
              dir="auto"
              data-test="testimonial-quote"
            >
              <p v-for="(p, i) in paragraphs" :key="i">{{ p }}</p>
            </blockquote>
            <button
              v-if="isLong"
              type="button"
              class="mt-3 text-sm font-semibold text-brand-700 hover:underline"
              :aria-expanded="expanded"
              data-test="testimonial-toggle"
              @click="expanded = !expanded"
            >
              {{ expanded ? t('home.testimonials.readLess') : t('home.testimonials.readMore') }}
            </button>

            <figcaption class="mt-6 border-t border-slate-200 pt-5">
              <p class="font-display text-lg font-semibold text-slate-900" data-test="testimonial-name">
                {{ current.name ?? t('home.testimonials.anonymous') }}
              </p>
              <p class="mt-0.5 text-sm text-slate-600">{{ route }}</p>
            </figcaption>
          </div>
        </div>

        <div class="mt-10 flex items-center justify-between gap-4 lg:justify-start lg:gap-8">
          <div class="flex items-center gap-3" role="tablist" :aria-label="t('home.testimonials.title')">
            <button
              v-for="(s, i) in testimonials"
              :key="s.id"
              type="button"
              role="tab"
              :aria-selected="i === index"
              :aria-label="t('home.testimonials.show', { n: i + 1 })"
              class="h-12 w-12 overflow-hidden rounded-full ring-2 ring-offset-2 transition focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-500"
              :class="i === index ? 'ring-brand-500' : 'opacity-60 ring-transparent hover:opacity-100'"
              data-test="testimonial-thumb"
              @click="go(i)"
            >
              <img :src="s.image" alt="" class="h-full w-full origin-[52%_33%] scale-[2.2] object-cover object-[52%_33%]" loading="lazy" decoding="async">
            </button>
          </div>
          <div class="flex items-center gap-2">
            <button
              type="button"
              class="grid h-11 w-11 place-items-center rounded-full border border-slate-200 text-slate-700 hover:bg-slate-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-500"
              :aria-label="t('home.testimonials.previous')"
              data-test="testimonial-prev"
              @click="go(index - 1)"
            >
              <svg viewBox="0 0 24 24" class="h-5 w-5 rtl:-scale-x-100" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="m15 6-6 6 6 6" stroke-linecap="round" stroke-linejoin="round" /></svg>
            </button>
            <button
              type="button"
              class="grid h-11 w-11 place-items-center rounded-full bg-brand-600 text-white hover:bg-brand-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
              :aria-label="t('home.testimonials.next')"
              data-test="testimonial-next"
              @click="go(index + 1)"
            >
              <svg viewBox="0 0 24 24" class="h-5 w-5 rtl:-scale-x-100" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="m9 6 6 6-6 6" stroke-linecap="round" stroke-linejoin="round" /></svg>
            </button>
          </div>
        </div>
      </div>
    </NContainer>
  </section>
</template>
