import { VueNodeViewRenderer } from '@tiptap/vue-3'
import type { NewsNodeViews } from './extensions'
import ImageBlockView from './ImageBlockView.vue'
import EmbedBlockView from './EmbedBlockView.vue'
import CodeBlockView from './CodeBlockView.vue'

/** The Vue renderers for the news editor's block nodes. Kept apart from `extensions.ts` so the schema stays testable without Vue. */
export const newsNodeViews: NewsNodeViews = {
  image: () => VueNodeViewRenderer(ImageBlockView),
  embed: () => VueNodeViewRenderer(EmbedBlockView),
  codeBlock: () => VueNodeViewRenderer(CodeBlockView),
}
