<template>
  <div class="nad-page">
    <button
      class="back"
      type="button"
      @click="router.push('/scholarships')"
    >
      <el-icon><ArrowLeft /></el-icon>{{ t('scholarship.backToList') }}
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

    <template v-else-if="s">
      <PageHeader :title="s.view.title">
        <template #subtitle>
          {{ [s.view.city, s.view.province, s.view.country].filter(Boolean).join(' · ') }}
          · {{ t(`scholarship.funding.${s.view.fundingModel}`) }}
        </template>
        <template #actions>
          <StatusBadge :status="s.status" />
          <StatusBadge
            :status="s.publishStatus"
            :map="{ PUBLISHED: 'success', DRAFT: 'neutral' }"
          />
          <el-button
            v-if="userStore.hasPerm('nad:scholarship:edit')"
            size="small"
            :icon="Edit"
            @click="drawerOpen = true"
          >
            {{ t('common.edit') }}
          </el-button>
        </template>
      </PageHeader>

      <FormSection
        v-if="s.view.heroImageUrl || s.view.coverImageUrl"
        :title="t('scholarship.secMedia')"
      >
        <div class="imgs">
          <figure v-if="s.view.heroImageUrl">
            <img
              :src="s.view.heroImageUrl"
              alt=""
            >
            <figcaption>{{ t('scholarship.heroImage') }}</figcaption>
          </figure>
          <figure v-if="s.view.coverImageUrl">
            <img
              :src="s.view.coverImageUrl"
              alt=""
            >
            <figcaption>{{ t('scholarship.coverImage') }}</figcaption>
          </figure>
        </div>
      </FormSection>

      <FormSection :title="t('scholarship.secIdentity')">
        <DescriptionList
          :items="[
            { label: t('scholarship.slug'), value: s.view.slug },
            { label: t('scholarship.field'), value: s.view.field || '—' },
            { label: t('scholarship.teachingLanguage'), value: s.view.teachingLanguage ? t(`scholarship.lang.${s.view.teachingLanguage}`) : '—' },
            { label: t('scholarship.deadline'), value: s.view.deadline || t('scholarship.rolling') },
            { label: t('scholarship.levels'), value: s.view.levels.map(l => t(`scholarship.level.${l}`)).join(', ') || '—' },
            { label: t('scholarship.nonDegreeDuration'), value: s.view.nonDegreeDuration ? t(`scholarship.nonDegree.${s.view.nonDegreeDuration}`) : '—' },
            { label: t('scholarship.categories'), value: s.view.categories.join(', ') || '—' },
            { label: t('scholarship.slots'), value: s.view.slots ?? '—' },
          ]"
        />
        <p
          v-if="s.view.summary"
          class="prose"
        >
          {{ s.view.summary }}
        </p>
      </FormSection>

      <FormSection
        v-if="s.view.eligibility"
        :title="t('scholarship.secEligibility')"
      >
        <DescriptionList :items="eligibilityItems" />
        <p
          v-if="s.view.eligibility.notes"
          class="prose"
        >
          {{ s.view.eligibility.notes }}
        </p>
      </FormSection>

      <FormSection
        v-if="s.view.fees.length || s.view.applicationFee || s.view.serviceFee"
        :title="t('scholarship.secFees')"
      >
        <el-table
          :data="feeRows"
          size="small"
        >
          <el-table-column
            prop="label"
            :label="t('scholarship.feeKind')"
          />
          <el-table-column
            prop="amount"
            :label="t('scholarship.amount')"
            align="right"
          />
        </el-table>
      </FormSection>

      <FormSection
        v-if="s.view.stipends.length"
        :title="t('scholarship.secStipend')"
      >
        <el-table
          :data="s.view.stipends"
          size="small"
        >
          <el-table-column
            :label="t('scholarship.level')"
            width="120"
          >
            <template #default="{ row }">
              {{ t(`scholarship.level.${row.level}`) }}
            </template>
          </el-table-column>
          <el-table-column
            :label="t('scholarship.amount')"
            align="right"
          >
            <template #default="{ row }">
              {{ dual(row.amountRmb, row.amountUsd) }} / {{ t(`scholarship.freq.${row.frequency}`) }}
            </template>
          </el-table-column>
          <el-table-column
            :label="t('scholarship.durationMonths')"
            width="120"
            align="right"
          >
            <template #default="{ row }">
              {{ row.durationMonths ?? '—' }}
            </template>
          </el-table-column>
          <el-table-column
            :label="t('scholarship.stipendConditions')"
            prop="conditions"
          />
        </el-table>
      </FormSection>

      <FormSection
        v-if="s.view.accommodation.length"
        :title="t('scholarship.secAccommodation')"
      >
        <el-table
          :data="s.view.accommodation"
          size="small"
        >
          <el-table-column
            :label="t('scholarship.accommodationNote')"
            prop="note"
          >
            <template #default="{ row }">
              <strong>{{ t(`scholarship.room.${row.roomType}`) }}</strong>
              <span
                v-if="row.note"
                class="muted"
              > — {{ row.note }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="t('scholarship.amount')"
            align="right"
            width="200"
          >
            <template #default="{ row }">
              {{ dual(row.amountRmb, row.amountUsd) }}
            </template>
          </el-table-column>
        </el-table>
      </FormSection>

      <FormSection
        v-if="s.view.intakes.length"
        :title="t('scholarship.secIntakes')"
      >
        <ul class="list">
          <li
            v-for="(it, i) in s.view.intakes"
            :key="i"
          >
            {{ t(`scholarship.intake.${it.term}`) }}
            <span
              v-if="it.applicationClose"
              class="muted"
            > · {{ t('scholarship.closes') }} {{ it.applicationClose }}</span>
          </li>
        </ul>
      </FormSection>

      <FormSection
        v-if="s.view.documentRequirements.length"
        :title="t('scholarship.secDocuments')"
      >
        <ul class="list">
          <li
            v-for="(d, i) in s.view.documentRequirements"
            :key="i"
          >
            {{ t(`scholarship.doc.${d.docType}`, d.docType) }}
            <el-tag
              size="small"
              :type="d.mandatory ? 'danger' : 'info'"
              effect="plain"
            >
              {{ d.mandatory ? t('scholarship.mandatory') : t('scholarship.optionalDoc') }}
            </el-tag>
            <span
              v-if="d.note"
              class="muted"
            > — {{ d.note }}</span>
          </li>
        </ul>
      </FormSection>

      <FormSection
        v-for="p in prose"
        :key="p.label"
        :title="p.label"
      >
        <p class="prose">
          {{ p.value }}
        </p>
      </FormSection>

      <FormSection
        v-if="userStore.hasPerm('nad:scholarship:internal:view')"
        :title="t('scholarship.secInternal')"
        :description="t('scholarship.internalHint')"
      >
        <DescriptionList
          :items="[
            { label: t('scholarship.partnerUniversity'), value: internal?.universityName || (internal?.universityId ? `#${internal.universityId}` : '—') },
            { label: t('scholarship.internalStatus'), value: internal?.internalStatus || 'DRAFT' },
            { label: t('scholarship.operationalNotes'), value: internal?.operationalNotes || '—' },
            { label: t('scholarship.confidentialTerms'), value: internal?.confidentialTerms || '—' },
          ]"
        />
        <el-button
          v-if="userStore.hasPerm('nad:scholarship:internal:edit')"
          size="small"
          :icon="Edit"
          style="margin-top: 8px"
          @click="internalOpen = true"
        >
          {{ t('scholarship.editInternal') }}
        </el-button>
      </FormSection>
    </template>

    <ScholarshipDrawer
      v-model="drawerOpen"
      :scholarship="s"
      @saved="onSaved"
    />

    <el-dialog
      v-model="internalOpen"
      :title="t('scholarship.editInternal')"
      width="480"
    >
      <el-form label-position="top">
        <el-form-item :label="t('scholarship.partnerUniversityId')">
          <el-input-number
            v-model="internalForm.universityId"
            :min="1"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item :label="t('scholarship.internalStatus')">
          <el-input
            v-model="internalForm.internalStatus"
            maxlength="24"
          />
        </el-form-item>
        <el-form-item :label="t('scholarship.operationalNotes')">
          <el-input
            v-model="internalForm.operationalNotes"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
        <el-form-item :label="t('scholarship.confidentialTerms')">
          <el-input
            v-model="internalForm.confidentialTerms"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
        <el-form-item :label="t('scholarship.commissionModelJson')">
          <el-input
            v-model="internalForm.commissionModelJson"
            type="textarea"
            :rows="2"
            placeholder="{&quot;rate&quot;:0.15}"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="internalOpen = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button
          type="primary"
          :loading="savingInternal"
          @click="saveInternal"
        >
          {{ t('common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Edit } from '@element-plus/icons-vue'
import {
  getScholarship, getScholarshipInternal, putScholarshipInternal,
  type Scholarship, type ScholarshipInternal,
} from '@/api/scholarship'
import { useUserStore } from '@/stores/user'
import PageHeader from '@/components/PageHeader.vue'
import DescriptionList from '@/components/ui/DescriptionList.vue'
import FormSection from '@/components/ui/FormSection.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import ScholarshipDrawer from './ScholarshipDrawer.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const id = computed(() => String(route.params.id))
const loading = ref(true)
const error = ref<string | null>(null)
const s = ref<Scholarship | null>(null)
const internal = ref<ScholarshipInternal | null>(null)
const drawerOpen = ref(false)
const internalOpen = ref(false)
const savingInternal = ref(false)
const internalForm = reactive({
  universityId: null as number | null, internalStatus: '', operationalNotes: '',
  confidentialTerms: '', commissionModelJson: '',
})

const eligibilityItems = computed(() => {
  const e = s.value?.view.eligibility
  if (!e) return []
  const items: { label: string, value: string | number }[] = []
  if (e.ageMin != null || e.ageMax != null) items.push({ label: t('scholarship.age'), value: `${e.ageMin ?? '—'}–${e.ageMax ?? '—'}` })
  items.push({ label: t('scholarship.nationality'), value: nationalityText(e) })
  if (e.inChina != null) items.push({ label: t('scholarship.inChina'), value: e.inChina ? t('common.yes') : t('common.no') })
  if (e.gpaMin != null) items.push({ label: 'GPA', value: `≥ ${e.gpaMin}` })
  if (e.ieltsMin != null) items.push({ label: 'IELTS', value: `≥ ${e.ieltsMin}` })
  if (e.toeflMin != null) items.push({ label: 'TOEFL', value: `≥ ${e.toeflMin}` })
  if (e.hskMin != null) items.push({ label: 'HSK', value: `≥ ${e.hskMin}` })
  return items
})
function nationalityText(e: NonNullable<Scholarship['view']['eligibility']>) {
  if (e.nationalityScope === 'INCLUDE') return `${t('scholarship.scope.INCLUDE')}: ${e.acceptedCountries || '—'}`
  if (e.nationalityScope === 'EXCLUDE') return `${t('scholarship.scope.EXCLUDE')}: ${e.acceptedCountries || '—'}`
  return t('scholarship.scope.ANY')
}

function dual(rmb: number | null | undefined, usd: number | null | undefined): string {
  if (rmb == null && usd == null) return '—'
  return `¥${(rmb ?? 0).toLocaleString('en')} · $${(usd ?? 0).toLocaleString('en')}`
}
const feeRows = computed(() => {
  const v = s.value?.view
  if (!v) return []
  const rows = v.fees.map(f => ({ label: t(`scholarship.fee.${f.kind}`), amount: dual(f.amountRmb, f.amountUsd) }))
  if (v.applicationFee) rows.unshift({ label: t('scholarship.fee.APPLICATION'), amount: dual(v.applicationFee.amountRmb, v.applicationFee.amountUsd) })
  if (v.serviceFee) rows.push({ label: t('scholarship.fee.NADOUMI_SERVICE'), amount: dual(v.serviceFee.amountRmb, v.serviceFee.amountUsd) })
  return rows
})
const prose = computed(() => {
  const v = s.value?.view
  if (!v) return []
  return [
    { label: t('scholarship.benefits'), value: v.benefits },
    { label: t('scholarship.requirements'), value: v.requirements },
    { label: t('scholarship.policy'), value: v.policy },
  ].filter(p => p.value) as { label: string, value: string }[]
})

async function load() {
  loading.value = true
  error.value = null
  try {
    s.value = await getScholarship(id.value)
    if (userStore.hasPerm('nad:scholarship:internal:view')) {
      internal.value = await getScholarshipInternal(id.value).catch(() => null)
    }
  }
  catch (e) {
    error.value = (e as Error)?.message || t('state.errorTitle')
  }
  finally {
    loading.value = false
  }
}
function onSaved() { load() }

watch(internalOpen, (open) => {
  if (!open) return
  internalForm.universityId = internal.value?.universityId ?? null
  internalForm.internalStatus = internal.value?.internalStatus ?? ''
  internalForm.operationalNotes = internal.value?.operationalNotes ?? ''
  internalForm.confidentialTerms = internal.value?.confidentialTerms ?? ''
  internalForm.commissionModelJson = internal.value?.commissionModelJson ?? ''
})
async function saveInternal() {
  savingInternal.value = true
  try {
    internal.value = await putScholarshipInternal(id.value, {
      universityId: internalForm.universityId,
      internalStatus: internalForm.internalStatus || null,
      operationalNotes: internalForm.operationalNotes || null,
      confidentialTerms: internalForm.confidentialTerms || null,
      commissionModelJson: internalForm.commissionModelJson || null,
    })
    ElMessage.success(t('common.saved'))
    internalOpen.value = false
  }
  finally {
    savingInternal.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.back { display: inline-flex; align-items: center; gap: 4px; margin-bottom: 12px; background: none; border: 0; cursor: pointer; color: var(--nad-ink-soft); font-size: 13px; }
.prose { margin: 8px 0 0; white-space: pre-line; line-height: 1.6; color: var(--nad-ink-soft); }
.list { margin: 4px 0 0; padding-left: 18px; line-height: 1.9; }
.muted { color: var(--nad-ink-faint); }
.imgs { display: flex; gap: 20px; flex-wrap: wrap; align-items: flex-start; }
.imgs figure { margin: 0; }
.imgs img { width: 280px; height: 150px; object-fit: cover; border-radius: 8px; border: 1px solid var(--nad-border, #e5e7eb); }
.imgs figcaption { margin-top: 4px; font-size: 12px; color: var(--nad-ink-soft); }
</style>
