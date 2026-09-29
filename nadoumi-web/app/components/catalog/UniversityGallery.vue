<script setup lang="ts">
import type { UniversityDetail } from '~/types/catalog'

defineProps<{ images: UniversityDetail['gallery'] }>()

const { t } = useI18n()
const lightboxIndex = ref<number | null>(null)
</script>

<template>
  <div>
    <section v-if="images.length">
      <h2 class="font-display text-xl font-semibold text-slate-900">{{ t('university.gallery') }}</h2>
      <div class="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-3">
        <figure
          v-for="(g, i) in images"
          :key="g.id"
          class="overflow-hidden rounded-xl bg-slate-100"
        >
          <button
            type="button"
            class="group block w-full cursor-zoom-in rounded focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-900"
            :aria-label="g.caption || t('university.gallery')"
            @click="lightboxIndex = i"
          >
            <img
              :src="mediaUrl(g.url ?? g.imageUrl)"
              :alt="g.caption ?? ''"
              loading="lazy"
              decoding="async"
              class="aspect-[4/3] w-full object-cover transition duration-300 group-hover:scale-[1.03]"
            >
          </button>
          <figcaption v-if="g.caption" class="px-2 py-1.5 text-xs text-slate-500">{{ g.caption }}</figcaption>
        </figure>
      </div>
    </section>
    <GalleryLightbox v-model="lightboxIndex" :images="images" />
  </div>
</template>
