<template>
  <div class="nad-page">
    <button
      class="back"
      type="button"
      @click="router.push('/applicants')"
    >
      <el-icon><ArrowLeft /></el-icon>{{ t('applicant.backToList') }}
    </button>

    <LoadingState
      v-if="loading"
      :rows="5"
    />
    <ErrorState
      v-else-if="error"
      :message="error"
      @retry="load"
    />

    <template v-else-if="applicant">
      <PageHeader :title="`${applicant.givenName} ${applicant.familyName}`">
        <template #subtitle>
          {{ t('applicant.idLabel', { id: applicant.id }) }} ·
          {{ t('applicant.registered') }} {{ fmtDate(applicant.createdAt) }}
        </template>
        <template #actions>
          <StatusBadge :status="applicant.status" />
        </template>
      </PageHeader>

      <AppTabs
        v-model="tab"
        :tabs="tabs"
      >
        <section v-if="tab === 'overview'">
          <el-alert
            v-if="piiMasked"
            :title="t('applicant.piiMasked')"
            type="info"
            :closable="false"
            show-icon
            class="detail__notice"
          />
          <DescriptionList :items="overview" />
        </section>

        <section v-else-if="tab === 'education'">
          <SubList
            :loading="edu.loading"
            :error="edu.error"
            :empty="!education || education.length === 0"
            :empty-title="t('applicant.noEducation')"
            @retry="() => runLoad('education')"
          >
            <el-table :data="education || []">
              <el-table-column
                :label="t('applicant.institution')"
                prop="institution"
                min-width="200"
              />
              <el-table-column
                :label="t('applicant.level')"
                prop="level"
                width="150"
              />
              <el-table-column
                :label="t('applicant.field')"
                prop="field"
                min-width="160"
              />
              <el-table-column
                :label="t('applicant.gpa')"
                width="120"
              >
                <template #default="{ row }">
                  {{ row.gpa != null ? `${row.gpa}${row.gpaScale ? ' / ' + row.gpaScale : ''}` : '—' }}
                </template>
              </el-table-column>
              <el-table-column
                :label="t('applicant.period')"
                width="200"
              >
                <template #default="{ row }">
                  {{ period(row.startDate, row.endDate) }}
                </template>
              </el-table-column>
            </el-table>
          </SubList>
        </section>

        <section v-else-if="tab === 'scores'">
          <SubList
            :loading="scores.loading"
            :error="scores.error"
            :empty="!testScores || testScores.length === 0"
            :empty-title="t('applicant.noScores')"
            @retry="() => runLoad('scores')"
          >
            <el-table :data="testScores || []">
              <el-table-column
                :label="t('applicant.testType')"
                prop="testType"
                width="150"
              />
              <el-table-column
                :label="t('applicant.score')"
                prop="score"
                width="120"
              />
              <el-table-column
                :label="t('applicant.takenOn')"
                width="150"
              >
                <template #default="{ row }">
                  {{ fmtDate(row.takenOn) }}
                </template>
              </el-table-column>
              <el-table-column
                :label="t('applicant.expiresOn')"
                width="150"
              >
                <template #default="{ row }">
                  {{ fmtDate(row.expiresOn) }}
                </template>
              </el-table-column>
            </el-table>
          </SubList>
        </section>

        <section v-else-if="tab === 'contacts'">
          <SubList
            :loading="cts.loading"
            :error="cts.error"
            :empty="!contacts || contacts.length === 0"
            :empty-title="t('applicant.noContacts')"
            @retry="() => runLoad('contacts')"
          >
            <el-table :data="contacts || []">
              <el-table-column
                :label="t('applicant.relation')"
                width="150"
              >
                <template #default="{ row }">
                  {{ titleCase(row.relation) }}
                </template>
              </el-table-column>
              <el-table-column
                :label="t('applicant.contactName')"
                prop="name"
                min-width="180"
              />
              <el-table-column
                :label="t('applicant.email')"
                prop="email"
                min-width="200"
              />
              <el-table-column
                :label="t('applicant.phone')"
                prop="phone"
                width="160"
              />
            </el-table>
          </SubList>
        </section>
      </AppTabs>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useApplicant } from '@/composables/useApplicant'
import PageHeader from '@/components/PageHeader.vue'
import AppTabs from '@/components/ui/AppTabs.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import DescriptionList from '@/components/ui/DescriptionList.vue'
import type { Tab, DescriptionItem } from '@/components/ui/types'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import SubList from './SubList.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const id = route.params.id as string
const {
  applicant, loading, error, load,
  education, testScores, contacts,
  loadEducation, loadTestScores, loadContacts,
} = useApplicant(id)

const tab = ref<'overview' | 'education' | 'scores' | 'contacts'>('overview')
const edu = reactive({ loading: false, error: null as string | null })
const scores = reactive({ loading: false, error: null as string | null })
const cts = reactive({ loading: false, error: null as string | null })

const tabs = computed<Tab[]>(() => [
  { key: 'overview', label: t('applicant.tabOverview') },
  { key: 'education', label: t('applicant.tabEducation'), count: education.value?.length },
  { key: 'scores', label: t('applicant.tabScores'), count: testScores.value?.length },
  { key: 'contacts', label: t('applicant.tabContacts'), count: contacts.value?.length },
])

const MASK = '••••'
const piiMasked = computed(() =>
  applicant.value?.dob === MASK || applicant.value?.passportNo === MASK)

const overview = computed<DescriptionItem[]>(() => {
  const a = applicant.value
  if (!a) return []
  return [
    { label: t('applicant.given'), value: a.givenName },
    { label: t('applicant.family'), value: a.familyName },
    { label: t('applicant.dob'), value: a.dob },
    { label: t('applicant.nationality'), value: a.nationality },
    { label: t('applicant.passport'), value: a.passportNo },
    { label: t('applicant.email'), value: a.email },
    { label: t('applicant.phone'), value: a.phone },
    { label: t('applicant.status'), value: titleCase(a.status) },
    { label: t('applicant.registered'), value: fmtDate(a.createdAt) },
  ]
})

function titleCase(s: string) {
  return s ? s.charAt(0) + s.slice(1).toLowerCase().replace(/_/g, ' ') : s
}
function fmtDate(v: string | null): string {
  if (!v) return '—'
  const d = new Date(v.replace(' ', 'T'))
  return Number.isNaN(d.getTime()) ? v : d.toLocaleDateString()
}
function period(a: string | null, b: string | null): string {
  if (!a && !b) return '—'
  return `${a ? fmtDate(a) : '…'} – ${b ? fmtDate(b) : t('applicant.present')}`
}

const LOADERS = {
  education: { state: edu, run: loadEducation },
  scores: { state: scores, run: loadTestScores },
  contacts: { state: cts, run: loadContacts },
} as const

async function runLoad(which: keyof typeof LOADERS, force = false) {
  const { state, run } = LOADERS[which]
  state.loading = true
  state.error = null
  try {
    await run(force)
  }
  catch (e) {
    state.error = (e as Error)?.message || t('state.errorTitle')
  }
  finally {
    state.loading = false
  }
}

watch(tab, (t) => {
  if (t === 'education') runLoad('education')
  else if (t === 'scores') runLoad('scores')
  else if (t === 'contacts') runLoad('contacts')
})

onMounted(load)
</script>

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
.detail__notice {
  margin-bottom: 16px;
}
</style>
