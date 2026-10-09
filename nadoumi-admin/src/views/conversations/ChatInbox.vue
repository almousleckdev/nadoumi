<template>
  <aside
    class="cx"
    :aria-label="t('conversations.title')"
  >
    <div class="cx__search">
      <el-input
        v-model="search"
        :placeholder="t('conversations.searchPlaceholder')"
        :aria-label="t('conversations.searchAria')"
        clearable
        size="large"
        data-test="inbox-search"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <p class="cx__hint">
        {{ t('conversations.searchHint') }}
      </p>
    </div>

    <div class="cx__scroll">
      <section
        v-if="searching"
        class="cx__section"
        :aria-label="t('conversations.startChatWith')"
        data-test="student-results"
      >
        <h3 class="cx__heading">
          {{ t('conversations.startChatWith') }}
        </h3>
        <ul
          v-if="studentsStatus === 'loading'"
          class="cx__skeletons"
          aria-busy="true"
        >
          <li
            v-for="n in 3"
            :key="n"
          >
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item
                  variant="circle"
                  style="width: 40px; height: 40px"
                />
                <el-skeleton-item
                  variant="text"
                  style="width: 50%; margin-inline-start: 12px"
                />
              </template>
            </el-skeleton>
          </li>
        </ul>
        <p
          v-else-if="studentsStatus === 'error'"
          class="cx__note"
        >
          {{ t('conversations.loadError') }}
        </p>
        <p
          v-else-if="students.length === 0"
          class="cx__note"
          data-test="no-students"
        >
          {{ t('conversations.noStudents') }}
        </p>
        <ul
          v-else
          class="cx__list"
        >
          <li
            v-for="s in students"
            :key="s.userId"
          >
            <button
              type="button"
              class="cs"
              data-test="student-option"
              @click="emit('startWith', s.userId)"
            >
              <ChatAvatar
                :name="s.name"
                :src="s.avatarUrl"
                :size="40"
                :online="s.online"
              />
              <span class="cs__main">
                <span class="cs__name">{{ s.name }}</span>
                <span class="cs__meta">
                  {{ s.studentRef }}<template v-if="s.matchedApplicationId"> · {{ t('conversations.applicationTag', { id: s.matchedApplicationId }) }}</template>
                </span>
              </span>
            </button>
          </li>
        </ul>
      </section>

      <section :aria-label="t('conversations.title')">
        <h3
          v-if="searching"
          class="cx__heading"
        >
          {{ t('conversations.title') }}
        </h3>

        <ul
          v-if="status === 'loading'"
          class="cx__skeletons"
          aria-busy="true"
          data-test="inbox-loading"
        >
          <li
            v-for="n in 6"
            :key="n"
          >
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item
                  variant="circle"
                  style="width: 44px; height: 44px"
                />
                <div style="flex: 1; margin-inline-start: 12px">
                  <el-skeleton-item
                    variant="text"
                    style="width: 45%"
                  />
                  <el-skeleton-item
                    variant="text"
                    style="width: 80%"
                  />
                </div>
              </template>
            </el-skeleton>
          </li>
        </ul>

        <div
          v-else-if="status === 'error'"
          class="cx__state"
          data-test="inbox-error"
        >
          <p>{{ t('conversations.loadError') }}</p>
          <el-button
            size="small"
            @click="emit('retry')"
          >
            {{ t('conversations.retry') }}
          </el-button>
        </div>

        <div
          v-else-if="items.length === 0"
          class="cx__state"
          :data-test="searching ? 'inbox-no-results' : 'inbox-empty'"
        >
          <h4 v-if="!searching">
            {{ t('conversations.emptyTitle') }}
          </h4>
          <p>{{ searching ? t('conversations.noResults') : t('conversations.emptyDesc') }}</p>
        </div>

        <template v-else>
          <ul class="cx__list">
            <ChatInboxItem
              v-for="c in items"
              :key="c.id"
              :conversation="c"
              :active="c.id === activeId"
              :my-id="myId"
              @select="emit('select', $event)"
            />
          </ul>
          <div
            v-if="hasMore"
            class="cx__more"
          >
            <el-button
              size="small"
              text
              :loading="loadingMore"
              @click="emit('loadMore')"
            >
              {{ t('conversations.loadMore') }}
            </el-button>
          </div>
        </template>
      </section>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search } from '@element-plus/icons-vue'
import type { ChatConversation, StudentResult } from '@/api/conversation'
import type { LoadStatus } from '@/composables/useStaffChat'
import type { SearchStatus } from '@/composables/useStudentSearch'
import { isSearchable } from '@/composables/useStudentSearch'
import ChatAvatar from './ChatAvatar.vue'
import ChatInboxItem from './ChatInboxItem.vue'

/**
 * The left column: one search box that narrows the staff member's own conversations and, at the same time, finds
 * students on the server (by name, student id such as STU-12, or application id) to start a new private chat with.
 */
defineProps<{
  items: ChatConversation[]
  status: LoadStatus
  activeId: number | null
  myId: number
  hasMore: boolean
  loadingMore: boolean
  students: StudentResult[]
  studentsStatus: SearchStatus
}>()
const search = defineModel<string>('search', { required: true })
const emit = defineEmits<{ select: [id: number], startWith: [studentUserId: number], retry: [], loadMore: [] }>()
const { t } = useI18n()

const searching = computed(() => isSearchable(search.value))
</script>

<style scoped>
.cx { display: flex; flex-direction: column; min-height: 0; width: 360px; flex-shrink: 0; border-inline-end: 1px solid var(--nad-line); background: #fff; }
.cx__search { padding: 16px 16px 8px; }
.cx__hint { margin: 6px 4px 0; font-size: 12px; color: var(--nad-ink-soft); }
.cx__scroll { flex: 1; min-height: 0; overflow-x: hidden; overflow-y: auto; }
.cx__section { border-bottom: 1px solid var(--nad-line); padding-bottom: 8px; }
.cx__heading { margin: 8px 16px 4px; font-size: 11px; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase; color: var(--nad-ink-soft); }
.cx__list { margin: 0; padding: 0; list-style: none; }
.cx__skeletons { margin: 0; padding: 8px 16px; list-style: none; }
.cx__skeletons li { display: flex; align-items: center; margin-bottom: 14px; }
.cx__note { margin: 4px 16px 8px; font-size: 13px; color: var(--nad-ink-soft); }
.cx__state { display: grid; justify-items: center; gap: 10px; padding: 40px 24px; text-align: center; color: var(--nad-ink-soft); font-size: 14px; }
.cx__state h4 { margin: 0; font-size: 16px; color: var(--nad-ink); }
.cx__state p { margin: 0; }
.cx__more { padding: 8px; text-align: center; }
.cs { display: flex; align-items: center; gap: 12px; width: 100%; padding: 8px 16px; border: 0; background: transparent; text-align: start; cursor: pointer; }
.cs:hover, .cs:focus-visible { background: var(--nad-brand-50); outline: none; }
.cs__main { display: grid; min-width: 0; }
.cs__name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; font-weight: 600; color: var(--nad-ink); }
.cs__meta { font-size: 12px; color: var(--nad-ink-soft); }
@media (max-width: 900px) {
  .cx { width: 100%; border-inline-end: 0; }
}
</style>
