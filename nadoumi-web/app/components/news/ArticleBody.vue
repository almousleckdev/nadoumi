<script setup lang="ts">
const props = defineProps<{ markdown: string }>()
const html = computed(() => renderMarkdown(props.markdown))
</script>

<template>
  <!-- eslint-disable-next-line vue/no-v-html -- markdown-it escapes raw HTML; see useMarkdown -->
  <div class="nad-article-body" v-html="html" />
</template>

<style scoped>
.nad-article-body {
  font-family: 'Source Serif 4', Georgia, Cambria, serif;
  font-size: 1.25rem;
  line-height: 1.8;
  color: rgb(30 41 59);
  overflow-wrap: anywhere;
}
.nad-article-body :deep(p) { margin: 1.5rem 0 0; }
.nad-article-body :deep(h2),
.nad-article-body :deep(h3) {
  font-family: 'Plus Jakarta Sans', Inter, sans-serif;
  font-weight: 700;
  color: rgb(15 23 42);
  letter-spacing: -0.01em;
  margin: 2.5rem 0 0;
  line-height: 1.3;
}
.nad-article-body :deep(h2) { font-size: 1.75rem; }
.nad-article-body :deep(h3) { font-size: 1.4rem; }
.nad-article-body :deep(a) { color: inherit; text-decoration: underline; text-underline-offset: 3px; }
.nad-article-body :deep(a:hover) { color: rgb(194 65 12); }
.nad-article-body :deep(blockquote) {
  margin: 2rem 0 0;
  padding-inline-start: 1.25rem;
  border-inline-start: 3px solid rgb(15 23 42);
  font-style: italic;
  color: rgb(51 65 85);
}
.nad-article-body :deep(ul),
.nad-article-body :deep(ol) { margin: 1.5rem 0 0; padding-inline-start: 1.5rem; }
.nad-article-body :deep(ul) { list-style: disc; }
.nad-article-body :deep(ol) { list-style: decimal; }
.nad-article-body :deep(li) { margin-top: 0.5rem; }
.nad-article-body :deep(img) {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 2rem auto 0;
  border-radius: 6px;
}
.nad-article-body :deep(hr) { margin: 3rem auto; width: 4rem; border-color: rgb(148 163 184); }
.nad-article-body :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 0.9em;
  background: rgb(241 245 249);
  padding: 0.1em 0.35em;
  border-radius: 4px;
}
.nad-article-body :deep(pre) {
  margin: 1.5rem 0 0;
  padding: 1rem;
  overflow-x: auto;
  background: rgb(241 245 249);
  border-radius: 6px;
  font-size: 0.95rem;
  line-height: 1.6;
}
.nad-article-body :deep(pre code) { background: none; padding: 0; }
.nad-article-body > :deep(:first-child) { margin-top: 0; }
</style>
