<script setup lang="ts">
import type { UniversityDetail } from '~/types/catalog'

const props = defineProps<{ university: UniversityDetail }>()

const { t } = useI18n()

const monogram = computed(() => props.university.name.trim().charAt(0).toUpperCase() || 'N')
const place = computed(() => [props.university.city, props.university.province, props.university.country].filter(Boolean).join(', '))
const typeLabel = computed(() =>
  props.university.type === 'PUBLIC' ? t('catalog.typePublic') : props.university.type === 'PRIVATE' ? t('catalog.typePrivate') : '',
)
</script>

<template>
  <section class="relative isolate overflow-hidden bg-slate-900 text-white">
    <img
      v-if="university.bannerUrl ?? university.coverImageUrl"
      :src="mediaUrl(university.bannerUrl ?? university.coverImageUrl)"
      alt=""
      class="absolute inset-0 -z-20 h-full w-full object-cover opacity-40"
    >
    <div class="absolute inset-0 -z-10 bg-gradient-to-br from-slate-900 via-slate-900/90 to-brand-900/60" aria-hidden="true" />

    <!-- university logo, pinned bottom-right of the cover -->
    <img
      v-if="university.logoUrl ?? university.logoImageUrl"
      :src="mediaUrl(university.logoUrl ?? university.logoImageUrl)"
      alt=""
      class="absolute bottom-4 right-4 z-10 h-16 w-16 rounded-xl bg-white/95 object-contain p-1.5 shadow-lg ring-1 ring-white/25 sm:bottom-6 sm:right-6 sm:h-20 sm:w-20"
    >
    <span
      v-else
      class="absolute bottom-4 right-4 z-10 flex h-16 w-16 items-center justify-center rounded-xl bg-white/10 font-display text-2xl font-bold ring-1 ring-white/20 sm:bottom-6 sm:right-6 sm:h-20 sm:w-20 sm:text-3xl"
    >
      {{ monogram }}
    </span>

    <NContainer>
      <div class="py-12 pe-20 sm:py-16 sm:pe-28">
        <h1 class="font-display text-3xl font-bold tracking-tight sm:text-4xl">{{ university.name }}</h1>
        <p v-if="university.nameCn" class="mt-1 text-lg text-white/70">{{ university.nameCn }}</p>
        <p class="mt-2 text-white/80">{{ place }}</p>
        <div class="mt-3 flex flex-wrap items-center gap-2">
          <span v-if="typeLabel" class="rounded-full bg-white/10 px-2.5 py-0.5 text-xs font-medium ring-1 ring-white/20">{{ typeLabel }}</span>
          <span v-if="university.recommended" class="rounded-full bg-brand-500/90 px-2.5 py-0.5 text-xs font-semibold">{{ t('catalog.recommendedShort') }}</span>
          <span v-if="university.featured" class="rounded-full bg-white/90 px-2.5 py-0.5 text-xs font-semibold text-brand-800">{{ t('catalog.featured') }}</span>
        </div>
      </div>
    </NContainer>
  </section>
</template>
