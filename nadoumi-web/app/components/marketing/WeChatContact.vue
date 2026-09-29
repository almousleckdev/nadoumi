<script setup lang="ts">
import wechatQrUrl from '~/assets/images/wechat.png'

const { t } = useI18n()
</script>

<template>
  <div class="group relative inline-block">
    <button
      type="button"
      :aria-label="t('footer.wechat')"
      class="inline-flex h-9 cursor-pointer items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-1 text-xs font-medium text-slate-700 shadow-sm transition hover:border-[#07C160] hover:text-[#07C160] hover:shadow"
    >
      <span class="flex h-5 w-5 items-center justify-center rounded-full bg-[#07C160] text-white">
        <WeChatIcon />
      </span>
      <span>WeChat</span>
    </button>

    <div
      class="pointer-events-none absolute bottom-full left-0 z-50 mb-3 w-64 rounded-2xl border border-slate-200/90 bg-white p-4 shadow-xl opacity-0 translate-y-2 transition-all duration-200 ease-out group-hover:pointer-events-auto group-hover:opacity-100 group-hover:translate-y-0 group-focus-within:pointer-events-auto group-focus-within:opacity-100 group-focus-within:translate-y-0"
      data-test="wechat-popover"
    >
      <div class="absolute -bottom-3 left-0 right-0 h-3" />

      <div class="mb-2.5 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <span class="flex h-5 w-5 items-center justify-center rounded-full bg-[#07C160] text-white">
            <WeChatIcon />
          </span>
          <span class="text-xs font-semibold text-slate-900">WeChat</span>
        </div>
        <span class="rounded bg-emerald-50 px-1.5 py-0.5 text-[10px] font-medium text-emerald-700">Official</span>
      </div>

      <div class="overflow-hidden rounded-xl border border-slate-200/80 bg-white p-2 text-center shadow-inner">
        <img
          :src="wechatQrUrl"
          :alt="t('footer.wechat')"
          class="mx-auto h-56 w-auto rounded-lg object-contain"
          width="200"
          height="280"
        >
      </div>

      <p class="mt-2 text-center text-[11px] leading-tight text-slate-500">
        {{ t('footer.wechatScanHint') }}
      </p>

      <div class="absolute -bottom-1.5 left-6 h-3 w-3 rotate-45 border-b border-r border-slate-200/90 bg-white" />
    </div>
  </div>
</template>
