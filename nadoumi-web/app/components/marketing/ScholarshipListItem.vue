<script setup lang="ts">
import type { ScholarshipCard } from '~/types/catalog'

/** A scholarship as one compact row: thumbnail, title, a meta line and the deadline. For lists inside a card. */
const props = defineProps<{ scholarship: ScholarshipCard }>()

const { t } = useI18n()
const localePath = useLocalePath()

const meta = computed(() => [
  t(`scholarships.funding.${props.scholarship.fundingModel}`),
  ...props.scholarship.levels.map(l => t(`scholarships.level.${l}`)),
].join(' · '))
</script>

<template>
  <NuxtLink
    :to="localePath(`/scholarships/${scholarship.slug}`)"
    class="group flex items-center gap-4 rounded-xl border border-slate-200 bg-white p-3 no-underline transition-colors hover:bg-slate-50"
  >
    <CardMedia
      :src="scholarship.coverUrl ?? scholarship.coverImageUrl ?? scholarship.heroUrl ?? scholarship.heroImageUrl"
      :label="scholarship.title"
      square
      class="h-16 w-16 shrink-0 rounded-lg"
    />
    <div class="min-w-0 flex-1">
      <p class="line-clamp-2 font-semibold text-slate-900 group-hover:underline">{{ scholarship.title }}</p>
      <p class="mt-0.5 truncate text-sm text-slate-500">{{ meta }}</p>
      <p class="mt-0.5 text-xs text-slate-500">
        {{ scholarship.deadline ? t('catalog.deadline', { date: scholarship.deadline }) : t('catalog.rollingDeadline') }}
      </p>
    </div>
    <span class="shrink-0 text-slate-400 group-hover:text-slate-900" aria-hidden="true">→</span>
  </NuxtLink>
</template>
