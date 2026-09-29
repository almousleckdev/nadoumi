<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps<{ logoSrc: string, coverSrc: string }>()

const { t } = useI18n()
</script>

<template>
  <div class="nad-card sec">
    <h3 class="sec__title">
      {{ t('university.secImages') }}
    </h3>
    <div class="imgs">
      <figure v-if="logoSrc">
        <img
          :src="logoSrc"
          class="img-logo"
          alt=""
        >
        <figcaption>{{ t('university.logoImage') }}</figcaption>
      </figure>
      <figure v-if="coverSrc">
        <img
          :src="coverSrc"
          class="img-cover"
          alt=""
        >
        <figcaption>{{ t('university.coverImage') }}</figcaption>
      </figure>
    </div>
  </div>
</template>

<style scoped src="./detail.css" />
<style scoped>
.imgs { display: flex; gap: 20px; flex-wrap: wrap; align-items: flex-start; }
.imgs figure { margin: 0; }
.imgs figcaption { margin-top: 4px; font-size: 12px; color: var(--nad-ink-soft); }
.img-logo { width: 96px; height: 96px; object-fit: contain; border-radius: 8px; background: #f4f4f5; border: 1px solid var(--nad-border, #e5e7eb); }
.img-cover { width: 280px; height: 140px; object-fit: cover; border-radius: 8px; border: 1px solid var(--nad-border, #e5e7eb); }
</style>
