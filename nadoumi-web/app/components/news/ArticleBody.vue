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
.nad-article-body :deep(hr) {
  height: 1.25rem;
  margin: 3rem 0 0;
  border: 0;
  text-align: center;
}
.nad-article-body :deep(hr)::before {
  content: '\00b7\00b7\00b7';
  padding-inline-start: 0.6em;
  font-size: 1.9rem;
  line-height: 1.25rem;
  letter-spacing: 0.6em;
  color: rgb(15 23 42);
}
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

/* Figures: inline sits in the column, wide and full extend past it, centred in either text direction. */
.nad-article-body :deep(.nad-fig) {
  --nad-fig-width: 100%;
  width: var(--nad-fig-width);
  margin: 2.5rem calc(50% - var(--nad-fig-width) / 2) 0;
}
.nad-article-body :deep(.nad-fig--wide) { --nad-fig-width: min(1032px, calc(100vw - 2rem)); }
.nad-article-body :deep(.nad-fig--full) { --nad-fig-width: 100vw; }
/* With the sidebar beside the article (lg and up) a figure can only extend a little past the text column. */
@media (min-width: 1024px) {
  .nad-article-body :deep(.nad-fig--wide),
  .nad-article-body :deep(.nad-fig--full) { --nad-fig-width: calc(100% + 4rem); }
}
.nad-article-body :deep(.nad-fig img) { margin: 0; width: 100%; border-radius: 0; }
.nad-article-body :deep(.nad-fig--inline img) { border-radius: 2px; }
.nad-article-body :deep(figcaption) {
  margin-top: 0.625rem;
  padding: 0 1rem;
  text-align: center;
  font-family: 'Plus Jakarta Sans', Inter, sans-serif;
  font-size: 0.875rem;
  line-height: 1.4;
  color: rgb(100 116 139);
}
.nad-article-body :deep(.nad-embed) {
  position: relative;
  margin: 2.5rem 0 0;
  aspect-ratio: 16 / 9;
  background: rgb(0 0 0);
}
.nad-article-body :deep(.nad-embed iframe) {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border: 0;
}
</style>
