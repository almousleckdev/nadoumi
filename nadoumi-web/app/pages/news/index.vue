<script setup lang="ts">
import type { ArticleSummary } from '~/types/news'

const { t } = useI18n()
useSeo(t('news.title'), t('news.subtitle'))

const {
  filters, page, pageCount, items,
  pending, error, isEmpty,
  setFilter, setPage, refresh,
} = useDiscovery<ArticleSummary>({
  resource: 'news',
  filterKeys: ['q'],
  pageSize: 10,
})

// local search box: committed on submit, not per keystroke
const searchText = ref(typeof filters.q === 'string' ? filters.q : '')
function submitSearch() {
  setFilter('q', searchText.value.trim() || undefined)
}
</script>

<template>
  <div>
    <NContainer>
      <div class="mx-auto max-w-3xl py-12 sm:py-16">
        <header class="border-b border-slate-200 pb-8">
          <h1 class="font-display text-4xl font-bold tracking-tight text-slate-900 sm:text-5xl">{{ t('news.title') }}</h1>
          <p class="mt-3 font-serif text-lg text-slate-600 sm:text-xl">{{ t('news.subtitle') }}</p>
          <div class="mt-6 max-w-md">
            <CatalogSearchForm
              id="news-search"
              v-model="searchText"
              :label="t('news.searchLabel')"
              :placeholder="t('news.searchPlaceholder')"
              @submit="submitSearch"
            />
          </div>
        </header>

        <div class="pt-8">
          <div v-if="pending" class="space-y-8" aria-hidden="true">
            <NSkeleton v-for="i in 4" :key="i" class="h-32 rounded-xl" />
          </div>
          <NAlert v-else-if="error" tone="danger">
            {{ t('errors.loadSection') }}
            <button type="button" class="ms-2 font-medium underline" @click="refresh()">{{ t('common.retry') }}</button>
          </NAlert>
          <div v-else-if="isEmpty" class="rounded-xl border border-dashed border-slate-200 px-6 py-14 text-center">
            <p class="font-display text-lg font-semibold text-slate-900">{{ t('news.emptyTitle') }}</p>
            <p class="mt-1 text-sm text-slate-600">{{ t('news.emptyBody') }}</p>
          </div>
          <template v-else>
            <ArticleCard v-for="a in items" :key="a.slug" :article="a" />
          </template>
        </div>

        <Pagination class="mt-8" :page="page" :page-count="pageCount" @update:page="setPage" />
      </div>
    </NContainer>
  </div>
</template>
