<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { University } from '@/api/university'
import { assetUrl } from '@/utils/asset'

defineProps<{ gallery: University['gallery'] }>()

const { t } = useI18n()
</script>

<template>
  <div class="nad-card sec">
    <h3 class="sec__title">
      {{ t('university.secGallery') }}
    </h3>
    <div class="gal">
      <figure
        v-for="g in gallery"
        :key="g.id"
        class="gal__item"
      >
        <img
          :src="assetUrl(g.url ?? g.imageUrl)"
          :alt="g.caption ?? ''"
        >
        <figcaption v-if="g.caption">
          {{ g.caption }}
        </figcaption>
      </figure>
    </div>
  </div>
</template>

<style scoped src="./detail.css" />
<style scoped>
.gal { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px; }
.gal__item { margin: 0; }
.gal__item img { width: 100%; height: 120px; object-fit: cover; border-radius: 8px; }
.gal__item figcaption { margin-top: 4px; font-size: 12px; color: var(--nad-ink-soft); }
</style>
