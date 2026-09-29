<script setup lang="ts">
import type { NotificationView } from '~/types/catalog'

defineProps<{ items: NotificationView[], pending: boolean, error: unknown }>()

const { t } = useI18n()
const localePath = useLocalePath()
</script>

<template>
  <SectionCard :title="t('dashboard.home.activity')">
    <template #actions>
      <NuxtLink :to="localePath('/dashboard/notifications')" class="text-sm font-medium text-brand-700 hover:underline">
        {{ t('dashboard.home.viewAll') }}
      </NuxtLink>
    </template>
    <div v-if="pending" class="grid gap-2">
      <NSkeleton class="h-5 w-full rounded" />
      <NSkeleton class="h-5 w-full rounded" />
    </div>
    <NAlert v-else-if="error" tone="danger">{{ t('errors.loadSection') }}</NAlert>
    <p v-else-if="!items.length" class="py-2 text-sm text-slate-500">{{ t('dashboard.home.activityEmpty') }}</p>
    <ul v-else class="grid gap-3">
      <li v-for="n in items" :key="n.id" class="flex items-start gap-2 text-sm">
        <span class="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full" :class="n.read ? 'bg-transparent' : 'bg-brand-600'" aria-hidden="true" />
        <span class="min-w-0 flex-1">
          <span class="block" :class="n.read ? 'text-slate-600' : 'font-medium text-slate-900'">{{ n.title }}</span>
        </span>
      </li>
    </ul>
  </SectionCard>
</template>
