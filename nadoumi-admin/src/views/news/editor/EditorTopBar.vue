<template>
  <header class="bar">
    <div class="bar__inner">
      <div class="bar__left">
        <button
          type="button"
          class="bar__back"
          @click="emit('back')"
        >
          <el-icon :size="18">
            <ArrowLeft />
          </el-icon>
          <span class="bar__back-label">{{ t('news.editor.menu.back') }}</span>
        </button>
        <span
          class="bar__sep"
          aria-hidden="true"
        />
        <span class="bar__draft">{{ label }}</span>
        <span
          class="bar__status"
          :class="{ 'bar__status--error': statusTone === 'error' }"
          role="status"
          aria-live="polite"
        >{{ status }}</span>
      </div>

      <div class="bar__right">
        <button
          type="button"
          class="bar__preview"
          @click="emit('preview')"
        >
          <el-icon :size="16">
            <View />
          </el-icon>
          {{ t('news.editor.preview.action') }}
        </button>

        <button
          v-if="primaryVisible"
          type="button"
          class="bar__primary"
          :class="{ 'bar__primary--idle': primaryIdle }"
          :aria-disabled="primaryIdle"
          :disabled="primaryBusy"
          @click="emit('primary')"
        >
          {{ primaryLabel }}
        </button>

        <el-dropdown
          trigger="click"
          placement="bottom-end"
          popper-class="ne-menu"
          @command="(key: string) => emit('command', key)"
        >
          <button
            type="button"
            class="bar__icon"
            :aria-label="t('news.editor.menu.more')"
          >
            <el-icon :size="20">
              <MoreFilled />
            </el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <template
                v-for="item in menu"
                :key="item.key"
              >
                <el-dropdown-item
                  :command="item.key"
                  :divided="item.divided"
                  :class="{ 'ne-menu__danger': item.danger }"
                >
                  {{ item.label }}
                </el-dropdown-item>
              </template>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <Avatar
          :name="userName"
          :src="userAvatar"
          :size="32"
        />
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { ArrowLeft, MoreFilled, View } from '@element-plus/icons-vue'
import Avatar from '@/components/ui/Avatar.vue'

export interface TopBarMenuItem {
  key: string
  label: string
  divided?: boolean
  danger?: boolean
}

defineProps<{
  label: string
  status: string
  statusTone: 'normal' | 'error'
  userName: string
  userAvatar?: string
  primaryVisible: boolean
  primaryLabel: string
  /** Looks inactive (nothing to publish yet) but still explains itself when pressed. */
  primaryIdle: boolean
  primaryBusy: boolean
  menu: TopBarMenuItem[]
}>()
const emit = defineEmits<{ back: [], preview: [], primary: [], command: [key: string] }>()

const { t } = useI18n()
</script>

<style scoped>
.bar {
  position: sticky;
  inset-block-start: 0;
  z-index: 20;
  background: var(--ne-paper);
}
.bar__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  max-width: 1032px;
  height: 65px;
  margin: 0 auto;
  padding: 0 16px;
}
.bar__left,
.bar__right {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}
.bar__back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px 6px 6px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--ne-ink);
  font: 400 14px/20px var(--ne-sans);
  cursor: pointer;
}
.bar__back:hover {
  background: #f2f2f2;
}
.bar__sep {
  width: 1px;
  height: 20px;
  background: #e3e3e3;
}
.bar__preview {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px 6px;
  border: 1px solid #d6d6d6;
  border-radius: 999px;
  background: transparent;
  color: var(--ne-ink);
  font: 400 14px/20px var(--ne-sans);
  cursor: pointer;
}
.bar__preview:hover {
  border-color: var(--ne-ink);
}
.bar__draft {
  overflow: hidden;
  color: var(--ne-ink);
  font: 400 14px/20px var(--ne-sans);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bar__status {
  flex: none;
  color: var(--ne-muted);
  font: 400 14px/20px var(--ne-sans);
}
.bar__status--error {
  color: #c94a4a;
}
.bar__primary {
  padding: 5px 14px 6px;
  border: 0;
  border-radius: 999px;
  background: var(--ne-accent);
  color: #fff;
  font: 400 14px/20px var(--ne-sans);
  cursor: pointer;
  transition: background-color 150ms ease;
}
.bar__primary:hover:not(:disabled) {
  background: var(--ne-accent-strong);
}
.bar__primary--idle {
  background: var(--ne-accent-soft);
}
.bar__primary--idle:hover:not(:disabled) {
  background: var(--ne-accent-soft);
}
.bar__primary:disabled {
  cursor: progress;
  opacity: 0.7;
}
.bar__icon {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--ne-muted);
  cursor: pointer;
}
.bar__icon:hover,
.bar__icon:focus-visible {
  color: var(--ne-ink);
}
.bar__primary:focus-visible,
.bar__back:focus-visible,
.bar__preview:focus-visible,
.bar__icon:focus-visible {
  outline: 2px solid var(--ne-accent);
  outline-offset: 2px;
}
</style>

<style>
/* The dropdown is teleported to <body>, so its Medium-style skin cannot be scoped. */
.ne-menu.el-popper {
  min-width: 232px;
  border: 1px solid rgb(230 230 230);
  border-radius: 4px;
  box-shadow: 0 1px 4px rgb(0 0 0 / 16%);
}
.ne-menu .el-dropdown-menu {
  padding: 8px 0;
}
.ne-menu .el-dropdown-menu__item {
  padding: 8px 24px;
  color: #242424;
  font: 400 14px/20px var(--ne-sans, system-ui);
}
.ne-menu .el-dropdown-menu__item:hover,
.ne-menu .el-dropdown-menu__item:focus {
  background: #f2f2f2;
  color: #242424;
}
.ne-menu .el-dropdown-menu__item--divided {
  margin-block-start: 8px;
}
.ne-menu .ne-menu__danger {
  color: #c94a4a;
}
</style>
