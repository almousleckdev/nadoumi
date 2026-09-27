import re

with open('/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/conversations/index.vue', 'r') as f:
    content = f.read()

# 1. Update v-for to include index
content = content.replace('v-for="m in messages"', 'v-for="(m, index) in messages"')

# 2. Add chained class and sender conditionally
content = content.replace(
    """<div
                v-for="(m, index) in messages"
                :key="m.id"
                class="conv__msg"
                :class="{ 'conv__msg--mine': isMine(m) }"
                data-test="message"
              >
                <div class="conv__bubble">
                  <p class="conv__sender">
                    {{ isMine(m) ? t('conversations.you') : (m.senderName || `#${m.senderUserId}`) }}
                  </p>""",
    """<div
                v-for="(m, index) in messages"
                :key="m.id"
                class="conv__msg"
                :class="{ 'conv__msg--mine': isMine(m), 'conv__msg--chained': index > 0 && messages[index - 1].senderUserId === m.senderUserId }"
                data-test="message"
              >
                <div class="conv__bubble">
                  <p v-if="index === 0 || messages[index - 1].senderUserId !== m.senderUserId" class="conv__sender">
                    {{ isMine(m) ? t('conversations.you') : (m.senderName || `#${m.senderUserId}`) }}
                  </p>"""
)

# 3. Replace the attachments block with valid Vue / Element Plus
old_attachments = """<div v-if="m.attachments.length" class="conv__attach-nice">
                    <div v-for="a in m.attachments" :key="a.id" class="conv__attach-item">
                      <a v-if="a.url && (a.filename && (a.filename.toLowerCase().endsWith('.jpg') || a.filename.toLowerCase().endsWith('.jpeg') || a.filename.toLowerCase().endsWith('.png') || a.filename.toLowerCase().endsWith('.webp')))" :href="a.url" target="_blank" rel="noopener" class="conv__attach-img-link">
                        <img :src="a.url" :alt="a.filename || 'attachment'" class="conv__attach-img" />
                      </a>
                      <a v-else-if="a.url" :href="a.url" target="_blank" rel="noopener" class="conv__attach-card">
                        <span class="conv__attach-icon">📎</span>
                        <span class="conv__attach-name">{{ a.filename || t('conversations.attachment') }}</span>
                        <span class="conv__attach-view">{{ t('conversations.viewAttachment') }}</span>
                      </a>
                      <span v-else class="conv__attach-missing">
                        📎 {{ a.filename || t('conversations.attachment') }} ({{ t('conversations.attachmentUnavailable') }})
                      </span>
                    </div>
                  </div>"""

new_attachments = """<div v-if="m.attachments && m.attachments.length" class="conv__attachments">
                    <div v-for="a in m.attachments" :key="a.id" class="conv__attachment-item">
                      <el-image
                        v-if="a.url && a.filename && a.filename.match(/\\.(jpeg|jpg|gif|png|webp)$/i)"
                        :src="a.url"
                        class="conv__image-preview"
                        :preview-src-list="[a.url]"
                        fit="cover"
                        lazy
                      />
                      <a v-else-if="a.url" :href="a.url" target="_blank" rel="noopener noreferrer" class="conv__file-card">
                        <el-icon class="conv__file-icon"><Document /></el-icon>
                        <span class="conv__file-name" :title="a.filename || 'Attachment'">{{ a.filename || 'Attachment' }}</span>
                      </a>
                      <span v-else class="conv__attach-missing">
                        <el-icon><Warning /></el-icon> {{ a.filename || 'Attachment' }} (Unavailable)
                      </span>
                    </div>
                  </div>"""

content = content.replace(old_attachments, new_attachments)

# 4. Import Document and Warning icons
if "import { Document" not in content:
    content = content.replace("import { ref, onMounted, nextTick } from 'vue'", "import { ref, onMounted, nextTick } from 'vue'\nimport { Document, Warning } from '@element-plus/icons-vue'")

# 5. Fix the CSS
css_hack = r'\.conv__attach-nice \{.*?\.conv__attach-missing \{[^\}]*\}'
content = re.sub(css_hack, '', content, flags=re.DOTALL)

new_css = """
.conv__msg--chained { margin-top: -6px; }
.conv__msg--chained .conv__bubble { border-top-left-radius: 4px; border-top-right-radius: 4px; }
.conv__attachments { display: flex; flex-direction: column; gap: 8px; margin-top: 8px; }
.conv__image-preview { border-radius: 8px; border: 1px solid var(--nad-line, #e2e8f0); max-width: 240px; max-height: 240px; display: block; }
.conv__file-card { display: flex; align-items: center; gap: 8px; padding: 10px 14px; background: var(--nad-surface, #ffffff); border: 1px solid var(--nad-line, #e2e8f0); border-radius: 8px; text-decoration: none; transition: background 0.2s; max-width: 280px; }
.conv__file-card:hover { background: var(--nad-surface-2, #f8fafc); }
.conv__file-icon { font-size: 18px; color: var(--el-color-primary, #4338ca); }
.conv__file-name { font-size: 13px; color: var(--nad-ink, #1e293b); font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv__attach-missing { font-size: 12px; color: var(--el-color-danger, #ef4444); font-style: italic; display: flex; align-items: center; gap: 4px; }
"""

content = content.replace("</style>", new_css + "\n</style>")

with open('/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/conversations/index.vue', 'w') as f:
    f.write(content)
