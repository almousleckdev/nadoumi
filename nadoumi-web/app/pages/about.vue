<script setup lang="ts">
const { t } = useI18n()
useSeo(t('about.title'), t('about.subtitle'))

const lifecycle = computed(() => [
  t('about.lc1'), t('about.lc2'), t('about.lc3'), t('about.lc4'), t('about.lc5'),
])
</script>

<template>
  <div>
    <PageHero :title="t('about.title')" :subtitle="t('about.subtitle')" />
    <NContainer>
      <div class="max-w-2xl space-y-12 py-12">
        <p class="text-lg text-slate-700">{{ t('about.body') }}</p>

        <section>
          <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('about.whoTitle') }}</h2>
          <p class="mt-2 text-slate-700">{{ t('about.whoBody') }}</p>
        </section>

        <section>
          <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('about.destinationsTitle') }}</h2>
          <p class="mt-2 text-slate-700">{{ t('about.destinationsBody') }}</p>
        </section>

        <section>
          <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('about.lifecycleTitle') }}</h2>
          <ol class="mt-3 space-y-2">
            <li v-for="(step, i) in lifecycle" :key="step" class="flex gap-3 text-slate-700">
              <span class="font-display text-sm font-bold text-brand-600">{{ i + 1 }}</span>
              <span>{{ step }}</span>
            </li>
          </ol>
        </section>

        <section>
          <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('about.approachTitle') }}</h2>
          <p class="mt-2 text-slate-700">{{ t('about.approachBody') }}</p>
        </section>
      </div>
    </NContainer>
  </div>
</template>
