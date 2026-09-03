<script setup lang="ts">
import type { UniversitySummary } from '~/types/catalog'

const props = withDefaults(defineProps<{
  university: UniversitySummary
  /** carousel item width vs. full-width grid cell */
  variant?: 'carousel' | 'grid'
}>(), { variant: 'grid' })

const localePath = useLocalePath()
const { t } = useI18n()

const monogram = computed(() => props.university.name.trim().charAt(0).toUpperCase() || 'N')
const place = computed(() =>
  [props.university.city, props.university.province, props.university.country].filter(Boolean).join(', '),
)
const typeLabel = computed(() => {
  const type = props.university.type
  return type === 'PUBLIC' ? t('catalog.typePublic') : type === 'PRIVATE' ? t('catalog.typePrivate') : ''
})
</script>

<template>
  <NuxtLink
    :to="localePath(`/universities/${university.slug}`)"
    class="group flex flex-col overflow-hidden rounded-xl border border-slate-200 bg-white no-underline shadow-xs transition-shadow hover:shadow-md"
    :class="variant === 'carousel' ? 'w-[17rem] sm:w-[19rem]' : 'w-full'"
  >
    <div class="relative flex h-28 items-center justify-center overflow-hidden bg-gradient-to-br from-brand-500 to-brand-700">
      <img
        v-if="university.coverImageUrl"
        :src="mediaUrl(university.coverImageUrl)"
        alt=""
        loading="lazy"
        decoding="async"
        class="absolute inset-0 h-full w-full object-cover"
      >
      <img
        v-if="university.logoImageUrl"
        :src="mediaUrl(university.logoImageUrl)"
        alt=""
        loading="lazy"
        decoding="async"
        class="relative h-14 w-14 rounded-lg bg-white/90 object-contain p-1.5 ring-1 ring-white/40"
      >
      <span v-else class="relative font-display text-3xl font-bold text-white/95">{{ monogram }}</span>
      <span
        v-if="university.featured"
        class="absolute right-3 top-3 rounded-full bg-white/90 px-2 py-0.5 text-[0.7rem] font-semibold text-brand-800"
      >{{ t('catalog.featured') }}</span>
    </div>

    <div class="flex flex-1 flex-col gap-1 p-4">
      <h3 class="font-display text-base font-semibold text-slate-900 group-hover:text-brand-800">
        {{ university.name }}
      </h3>
      <p v-if="university.nameCn" class="text-sm text-slate-500">{{ university.nameCn }}</p>
      <p class="mt-1 text-sm text-slate-600">{{ place }}</p>
      <div class="mt-3 flex items-center gap-2">
        <NBadge v-if="typeLabel" tone="neutral">{{ typeLabel }}</NBadge>
      </div>
      <span class="mt-3 inline-flex items-center gap-1 text-sm font-medium text-brand-700 group-hover:gap-2">
        {{ t('catalog.viewUniversity') }}<span aria-hidden="true">→</span>
      </span>
    </div>
  </NuxtLink>
</template>
