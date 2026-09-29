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

      <ImagesCard
        v-if="logoSrc || coverSrc"
        :logo-src="logoSrc"
        :cover-src="coverSrc"
      />


      <div class="nad-card sec">
        <h3 class="sec__title">
          {{ t('university.secProfile') }}
        </h3>
        <DescriptionList :items="profile">
          <template #website="{ value }">
            <a
              v-if="safeHref(value)"
              :href="safeHref(value)!"
              target="_blank"
              rel="noopener"
              class="link"
            >{{ value }}</a>
            <span
              v-else-if="value"
              class="muted"
            >{{ value }}</span>
            <span
              v-else
              class="muted"
            >{{ t('common.notSet') }}</span>
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

      <HighlightsCard
        v-if="u.highlights.length"
        :highlights="u.highlights"
      />
      <RankingsCard
        v-if="u.rankings.length"
        :rankings="u.rankings"
      />
      <GalleryCard
        v-if="u.gallery.length"
        :gallery="u.gallery"
      />

      <DepartmentSection
        v-if="userStore.hasPerm('nad:department:list') || userStore.hasPerm('nad:university:edit')"
        :university-id="u.id"
      />

      <ProgramsSection
        ref="programsSection"
        :university="u"
      />

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
import { getUniversity, type University } from '@/api/university'
import { useUserStore } from '@/stores/user'
import PageHeader from '@/components/PageHeader.vue'
import DescriptionList from '@/components/ui/DescriptionList.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import type { DescriptionItem } from '@/components/ui/types'
import UniversityDrawer from './UniversityDrawer.vue'
import DepartmentSection from './DepartmentSection.vue'
import ImagesCard from './detail/ImagesCard.vue'
import HighlightsCard from './detail/HighlightsCard.vue'
import RankingsCard from './detail/RankingsCard.vue'
import GalleryCard from './detail/GalleryCard.vue'
import ProgramsSection from './detail/ProgramsSection.vue'
import { assetUrl } from '@/utils/asset'
import { safeHref } from '@/utils/url'
import { formatDateTime } from '@/utils/date'
import { titleCase } from '@/utils/text'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const id = route.params.id as string
const u = ref<University | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const drawerOpen = ref(false)
const programsSection = ref<InstanceType<typeof ProgramsSection>>()

const logoSrc = computed(() => assetUrl(u.value?.logoUrl ?? u.value?.logoImageUrl))
const coverSrc = computed(() => assetUrl(u.value?.bannerUrl ?? u.value?.coverImageUrl))

const profile = computed<DescriptionItem[]>(() => {
  const x = u.value
  if (!x) return []
  return [
    { label: t('university.slug'), value: x.slug },
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
    { label: t('university.created'), value: formatDateTime(x.createdAt) },
    { label: t('university.updated'), value: formatDateTime(x.updatedAt) },
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
    await programsSection.value?.reload()
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

<style scoped src="./detail/detail.css" />
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
.prose {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--nad-ink);
  white-space: pre-wrap;
}
.link {
  color: var(--nad-brand-700);
}
</style>
