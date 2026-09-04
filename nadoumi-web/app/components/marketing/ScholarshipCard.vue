<script setup lang="ts">
import type { ScholarshipCard } from '~/types/catalog'

const props = withDefaults(defineProps<{
  scholarship: ScholarshipCard
  variant?: 'carousel' | 'grid'
}>(), { variant: 'grid' })

const { t } = useI18n()
const localePath = useLocalePath()

const place = computed(() =>
  [props.scholarship.city, props.scholarship.province, props.scholarship.country].filter(Boolean).join(', '),
)
</script>

<template>
  <NuxtLink
    :to="localePath(`/scholarships/${scholarship.slug}`)"
    class="group flex flex-col overflow-hidden rounded-xl border border-slate-200 bg-white no-underline shadow-xs transition-shadow hover:shadow-md"
    :class="variant === 'carousel' ? 'w-[19rem] sm:w-[21rem]' : 'w-full'"
  >
    <img
      v-if="scholarship.coverUrl ?? scholarship.coverImageUrl"
      :src="mediaUrl(scholarship.coverUrl ?? scholarship.coverImageUrl)"
      alt=""
      loading="lazy"
      decoding="async"
      class="h-32 w-full object-cover"
    >
    <div class="flex flex-1 flex-col p-5">
    <div class="flex items-center gap-2">
      <span class="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700">
        {{ t(`scholarships.funding.${scholarship.fundingModel}`) }}
      </span>
      <span v-if="scholarship.hot" class="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-800">{{ t('scholarships.hot') }}</span>
      <span v-else-if="scholarship.featured" class="rounded-full bg-brand-100 px-2 py-0.5 text-xs font-semibold text-brand-800">{{ t('catalog.featured') }}</span>
    </div>

    <h3 class="mt-3 font-display text-base font-semibold text-slate-900 group-hover:text-brand-800">
      {{ scholarship.title }}
    </h3>
    <p v-if="scholarship.levels.length" class="mt-1 text-sm text-slate-500">
      {{ scholarship.levels.map(l => t(`scholarships.level.${l}`)).join(' · ') }}
    </p>
    <p v-if="place" class="mt-1 text-sm text-slate-600">{{ place }}</p>

    <div class="mt-auto flex items-center justify-between pt-4 text-sm">
      <span class="text-slate-500">
        {{ scholarship.deadline ? t('catalog.deadline', { date: scholarship.deadline }) : t('catalog.rollingDeadline') }}
      </span>
      <span class="inline-flex items-center gap-1 font-medium text-brand-700 group-hover:gap-2">
        {{ t('scholarships.view') }}<span aria-hidden="true">→</span>
      </span>
    </div>
    </div>
  </NuxtLink>
</template>
