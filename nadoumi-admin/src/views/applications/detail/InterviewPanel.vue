<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { cancelInterview, listInterviews, scheduleInterview, type Interview } from '@/api/application'
import {
  beijingWallTimeToIso, formatBeijing, formatLocal, isFutureInBeijing, toBeijingLocal,
} from '@/utils/beijingTime'
import StatusBadge from '@/components/ui/StatusBadge.vue'

const props = defineProps<{ applicationId: number, canSchedule: boolean }>()
const emit = defineEmits<{ changed: [] }>()
const { t } = useI18n()

const DURATIONS = [15, 30, 45, 60, 90]
const DEFAULT_DURATION = 30

const interviews = ref<Interview[]>([])
const loading = ref(true)
const saving = ref(false)
const form = reactive({ wall: '', durationMinutes: DEFAULT_DURATION, platform: '', meetingUrl: '', instructions: '' })
const error = ref('')

const scheduled = computed(() => interviews.value.find(i => i.status === 'SCHEDULED') ?? null)
const history = computed(() => interviews.value.filter(i => i.status !== 'SCHEDULED'))
const nowInBeijing = computed(() => toBeijingLocal(new Date()))

async function load() {
  loading.value = true
  try {
    interviews.value = await listInterviews(props.applicationId)
  }
  finally {
    loading.value = false
  }
}

function validate(): string {
  if (!form.wall) return t('applications.interview.timeRequired')
  if (!isFutureInBeijing(form.wall)) return t('applications.interview.inThePast')
  if (!/^https:\/\/\S+$/i.test(form.meetingUrl.trim())) return t('applications.interview.linkInvalid')
  return ''
}

async function submit() {
  error.value = validate()
  if (error.value) return
  saving.value = true
  try {
    await scheduleInterview(props.applicationId, {
      startsAt: beijingWallTimeToIso(form.wall),
      durationMinutes: form.durationMinutes,
      platform: form.platform.trim() || undefined,
      meetingUrl: form.meetingUrl.trim(),
      instructions: form.instructions.trim() || undefined,
    })
    ElMessage.success(t('applications.interview.sent'))
    Object.assign(form, { wall: '', platform: '', meetingUrl: '', instructions: '', durationMinutes: DEFAULT_DURATION })
    await load()
    emit('changed')
  }
  catch {
    // the shared request client already toasts the server's message
  }
  finally {
    saving.value = false
  }
}

async function cancel(i: Interview) {
  saving.value = true
  try {
    await cancelInterview(props.applicationId, i.id)
    ElMessage.success(t('applications.interview.cancelled'))
    await load()
    emit('changed')
  }
  catch {
    // the shared request client already toasts the server's message
  }
  finally {
    saving.value = false
  }
}

const beijingDisabledDate = (date: Date) => {
  // the picker hands over the browser's midnight of each day, which names the same calendar day
  const day = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  return day < nowInBeijing.value.slice(0, 10)
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <p class="ip-help">
      {{ t('applications.interview.help') }}
    </p>

    <section
      v-if="scheduled"
      class="ip-card"
      data-test="interview-scheduled"
    >
      <div class="ip-head">
        <StatusBadge
          status="ACTIVE"
          :label="t('applications.interview.scheduled')"
        />
        <el-button
          v-if="canSchedule"
          size="small"
          type="danger"
          plain
          :loading="saving"
          data-test="interview-cancel"
          @click="cancel(scheduled)"
        >
          {{ t('applications.interview.cancel') }}
        </el-button>
      </div>
      <p class="ip-time">
        {{ formatBeijing(scheduled.startsAt) }} {{ t('applications.interview.beijing') }}
      </p>
      <p class="ip-sub">
        {{ t('applications.interview.yourClock', { time: formatLocal(scheduled.startsAt) }) }}
      </p>
      <p class="ip-sub">
        {{ t('applications.interview.length', { minutes: scheduled.durationMinutes }) }}
        <template v-if="scheduled.platform">
          · {{ scheduled.platform }}
        </template>
      </p>
      <el-link
        :href="scheduled.meetingUrl"
        target="_blank"
        rel="noopener"
        type="primary"
      >
        {{ scheduled.meetingUrl }}
      </el-link>
      <p
        v-if="scheduled.instructions"
        class="ip-notes"
      >
        {{ scheduled.instructions }}
      </p>
    </section>

    <el-form
      v-if="canSchedule"
      label-position="top"
      class="ip-form"
      @submit.prevent="submit"
    >
      <h4 class="ip-h">
        {{ scheduled ? t('applications.interview.reschedule') : t('applications.interview.schedule') }}
      </h4>
      <el-form-item :label="t('applications.interview.when')">
        <el-date-picker
          v-model="form.wall"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm"
          format="YYYY-MM-DD HH:mm"
          :disabled-date="beijingDisabledDate"
          :placeholder="t('applications.interview.whenHint')"
          data-test="interview-when"
        />
        <span class="ip-zone">{{ t('applications.interview.beijingNow', { time: nowInBeijing.replace('T', ' ') }) }}</span>
      </el-form-item>
      <el-form-item :label="t('applications.interview.duration')">
        <el-select v-model="form.durationMinutes">
          <el-option
            v-for="m in DURATIONS"
            :key="m"
            :value="m"
            :label="t('applications.interview.length', { minutes: m })"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('applications.interview.platform')">
        <el-input
          v-model="form.platform"
          maxlength="60"
          :placeholder="t('applications.interview.platformHint')"
        />
      </el-form-item>
      <el-form-item :label="t('applications.interview.link')">
        <el-input
          v-model="form.meetingUrl"
          maxlength="500"
          placeholder="https://"
          data-test="interview-link"
        />
      </el-form-item>
      <el-form-item :label="t('applications.interview.instructions')">
        <el-input
          v-model="form.instructions"
          type="textarea"
          :rows="3"
          maxlength="2000"
          :placeholder="t('applications.interview.instructionsHint')"
        />
      </el-form-item>
      <p
        v-if="error"
        class="ip-error"
        data-test="interview-error"
      >
        {{ error }}
      </p>
      <el-button
        type="primary"
        native-type="submit"
        :loading="saving"
        data-test="interview-submit"
      >
        {{ t('applications.interview.send') }}
      </el-button>
    </el-form>

    <template v-if="history.length">
      <h4 class="ip-h">
        {{ t('applications.interview.history') }}
      </h4>
      <ul class="ip-history">
        <li
          v-for="i in history"
          :key="i.id"
        >
          {{ formatBeijing(i.startsAt) }} {{ t('applications.interview.beijing') }} · {{ t('applications.interview.cancelledLabel') }}
        </li>
      </ul>
    </template>
  </div>
</template>

<style scoped>
.ip-help { margin: 0 0 14px; font-size: 13px; color: var(--nad-ink-soft, #64748b); }
.ip-card { display: grid; gap: 4px; padding: 14px; margin-bottom: 18px; border: 1px solid var(--nad-line, #e2e8f0); border-radius: 8px; }
.ip-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.ip-time { margin: 0; font-size: 16px; font-weight: 600; }
.ip-sub { margin: 0; font-size: 13px; color: var(--nad-ink-soft, #64748b); }
.ip-notes { margin: 6px 0 0; white-space: pre-wrap; font-size: 13px; }
.ip-form { max-width: 520px; }
.ip-h { margin: 18px 0 8px; font-size: 14px; }
.ip-zone { margin-inline-start: 10px; font-size: 12px; color: var(--nad-ink-soft, #64748b); }
.ip-error { margin: 0 0 10px; font-size: 13px; color: var(--el-color-danger); }
.ip-history { margin: 0; padding-inline-start: 18px; font-size: 13px; color: var(--nad-ink-soft, #64748b); }
</style>
