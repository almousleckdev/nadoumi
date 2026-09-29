<script setup lang="ts">
import type { ScholarshipDetail } from '~/types/catalog'

const { t } = useI18n()
const localePath = useLocalePath()
const { status } = useSession()
const { data: s } = await usePublicDetail<ScholarshipDetail>('scholarships')

useSeo(
  s.value?.title ?? t('catalog.scholarshipsTitle'),
  s.value?.summary?.slice(0, 155) ?? t('catalog.scholarshipsSubtitle'),
)

const applyTo = computed(() => localePath(status.value === 'authed' ? '/dashboard' : '/register'))
</script>

<template>
  <div v-if="s">
    <ScholarshipHero :s="s" :apply-to="applyTo" />

    <NContainer>
      <div class="grid gap-10 py-10 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <ScholarshipSections :s="s" />
        <ScholarshipSidebar :s="s" />
      </div>
    </NContainer>
  </div>

  <div v-else>
    <PageHero :title="t('catalog.scholarshipsTitle')" />
    <NContainer>
      <p class="py-10 text-slate-600">{{ t('scholarships.notFound') }}</p>
      <NuxtLink :to="localePath('/scholarships')" class="text-sm font-medium text-brand-700">← {{ t('catalog.backToList') }}</NuxtLink>
    </NContainer>
  </div>
</template>
