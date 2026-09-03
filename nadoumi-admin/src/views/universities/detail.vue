<template>
  <div class="nad-page">
    <button
      class="back"
      type="button"
      @click="router.push('/universities')"
    >
      <el-icon><ArrowLeft /></el-icon>{{ t('university.backToList') }}
    </button>

    <LoadingState
      v-if="loading"
      :rows="6"
    />
    <ErrorState
      v-else-if="error"
      :message="error"
      @retry="load"
    />

    <template v-else-if="u">
      <PageHeader :title="u.nameCn ? `${u.name} · ${u.nameCn}` : u.name">
        <template #subtitle>
          {{ [u.city, u.province, u.country].filter(Boolean).join(' · ') }}
          <span v-if="u.type"> · {{ titleCase(u.type) }}</span>
        </template>
        <template #actions>
          <StatusBadge :status="u.status" />
          <StatusBadge
            :status="u.publishStatus"
            :map="{ PUBLISHED: 'success', DRAFT: 'neutral' }"
          />
          <el-tag
            v-if="u.featured"
            type="warning"
            effect="plain"
            disable-transitions
          >
            {{ t('university.featured') }}
          </el-tag>
          <el-button
            v-if="userStore.hasPerm('nad:university:edit')"
            size="small"
            :icon="Edit"
            @click="drawerOpen = true"
          >
            {{ t('common.edit') }}
          </el-button>
        </template>
      </PageHeader>

      <div class="nad-card sec">
        <h3 class="sec__title">
          {{ t('university.secProfile') }}
        </h3>
        <DescriptionList :items="profile">
          <template #website="{ value }">
            <a
              v-if="value"
              :href="String(value)"
              target="_blank"
              rel="noopener"
              class="link"
            >{{ value }}</a>
            <span
              v-else
              class="muted"
            >—</span>
          </template>
        </DescriptionList>
      </div>

      <div
        v-for="block in prose"
        :key="block.label"
        class="nad-card sec"
      >
        <h3 class="sec__title">
          {{ block.label }}
        </h3>
        <p class="prose">
          {{ block.value }}
        </p>
      </div>

      <div
        v-if="u.highlights.length"
        class="nad-card sec"
      >
        <h3 class="sec__title">
          {{ t('university.secHighlights') }}
        </h3>
        <div class="hl">
          <div v-if="highlightsOf('HIGHLIGHT').length">
            <p class="hl__cap">
              {{ t('university.kindHighlight') }}
            </p>
            <ul>
              <li
                v-for="h in highlightsOf('HIGHLIGHT')"
                :key="h.id"
              >
                {{ h.text }}
              </li>
            </ul>
          </div>
          <div v-if="highlightsOf('ADVANTAGE').length">
            <p class="hl__cap">
              {{ t('university.kindAdvantage') }}
            </p>
            <ul>
              <li
                v-for="h in highlightsOf('ADVANTAGE')"
                :key="h.id"
              >
                {{ h.text }}
              </li>
            </ul>
          </div>
        </div>
      </div>

      <div
        v-if="u.rankings.length"
        class="nad-card sec"
      >
        <h3 class="sec__title">
          {{ t('university.secRankings') }}
        </h3>
        <el-table :data="u.rankings">
          <el-table-column
            :label="t('university.rankSource')"
            prop="source"
            width="140"
          />
          <el-table-column
            :label="t('university.rankPosition')"
            width="120"
          >
            <template #default="{ row }">
              #{{ row.rankPosition }}
            </template>
          </el-table-column>
          <el-table-column
            :label="t('university.rankYear')"
            prop="rankYear"
            width="100"
          />
          <el-table-column
            :label="t('common.actions')"
            prop="note"
          />
        </el-table>
      </div>

      <div
        v-if="u.gallery.length"
        class="nad-card sec"
      >
        <h3 class="sec__title">
          {{ t('university.secGallery') }}
        </h3>
        <div class="gal">
          <figure
            v-for="g in u.gallery"
            :key="g.id"
            class="gal__item"
          >
            <img
              :src="g.imageUrl"
              :alt="g.caption ?? ''"
            >
            <figcaption v-if="g.caption">
              {{ g.caption }}
            </figcaption>
          </figure>
        </div>
      </div>

      <UniversityDrawer
        v-model="drawerOpen"
        :university="u"
        @saved="load"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Edit } from '@element-plus/icons-vue'
import { getUniversity, type University, type HighlightKind } from '@/api/university'
import { useUserStore } from '@/stores/user'
import PageHeader from '@/components/PageHeader.vue'
import DescriptionList from '@/components/ui/DescriptionList.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import type { DescriptionItem } from '@/components/ui/types'
import UniversityDrawer from './UniversityDrawer.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const id = route.params.id as string
const u = ref<University | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const drawerOpen = ref(false)

function titleCase(s: string) {
  return s ? s.charAt(0) + s.slice(1).toLowerCase() : s
}
function fmtDate(v: string | null): string {
  if (!v) return '—'
  const d = new Date(v.replace(' ', 'T'))
  return Number.isNaN(d.getTime()) ? v : d.toLocaleString()
}
function highlightsOf(kind: HighlightKind) {
  return (u.value?.highlights ?? []).filter(h => h.kind === kind)
}

const profile = computed<DescriptionItem[]>(() => {
  const x = u.value
  if (!x) return []
  return [
    { label: t('university.country'), value: x.country },
    { label: t('university.city'), value: x.city },
    { label: t('university.province'), value: x.province },
    { label: t('university.type'), value: x.type ? titleCase(x.type) : null },
    { label: t('university.foundedYear'), value: x.foundedYear },
    { label: t('university.totalStudents'), value: x.totalStudents?.toLocaleString() },
    { label: t('university.intlStudents'), value: x.internationalStudents?.toLocaleString() },
    { label: t('university.facultyCount'), value: x.facultyCount?.toLocaleString() },
    { label: t('university.rankingTier'), value: x.rankingTier },
    { label: t('university.website'), value: x.website, slot: 'website' },
    { label: t('university.admissionsEmail'), value: x.admissionsEmail },
    { label: t('university.officePhone'), value: x.officePhone },
    { label: t('university.created'), value: fmtDate(x.createdAt) },
    { label: t('university.updated'), value: fmtDate(x.updatedAt) },
  ]
})

const prose = computed(() => {
  const x = u.value
  if (!x) return []
  return [
    { label: t('university.introduction'), value: x.introduction },
    { label: t('university.history'), value: x.history },
    { label: t('university.campusInfo'), value: x.campusInfo },
    { label: t('university.accommodationInfo'), value: x.accommodationInfo },
    { label: t('university.nearbyInfo'), value: x.nearbyInfo },
  ].filter(b => b.value)
})

async function load() {
  loading.value = true
  error.value = null
  try {
    u.value = await getUniversity(id)
  }
  catch (e) {
    error.value = (e as Error)?.message || t('state.errorTitle')
  }
  finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.gal { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px; }
.gal__item { margin: 0; }
.gal__item img { width: 100%; height: 120px; object-fit: cover; border-radius: 8px; }
.gal__item figcaption { margin-top: 4px; font-size: 12px; color: var(--nad-ink-soft); }
</style>
<style scoped>
.back {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: none;
  background: transparent;
  color: var(--nad-ink-soft);
  font-size: 13px;
  font-weight: 550;
  cursor: pointer;
  padding: 0;
  margin-bottom: 14px;
}
.back:hover {
  color: var(--nad-brand-700);
}
.sec {
  padding: 16px 20px;
  margin-bottom: 16px;
}
.sec__title {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--nad-ink-faint);
}
.prose {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--nad-ink);
  white-space: pre-wrap;
}
.hl {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
}
.hl__cap {
  margin: 0 0 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--nad-ink-faint);
}
.hl ul {
  margin: 0;
  padding-left: 18px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--nad-ink);
}
.link {
  color: var(--nad-brand-700);
}
.muted {
  color: var(--nad-ink-faint);
}
</style>
