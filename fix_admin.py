import re

with open('/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/conversations/index.vue', 'r') as f:
    content = f.read()

# Remove repeated sender name and fix bubble styles
content = content.replace(
    """<p class="conv__sender">{{ isMine(m) ? t('conversations.you') : (m.senderName || t('conversations.student')) }}</p>""",
    ""
)

# Render attachments nicer
# Wait, I can't do this easily with python replace if the block is huge. I'll just use regex.

attachment_block = """
                  <ul
                    v-if="m.attachments.length"
                    class="conv__attach"
                  >
                    <li
                      v-for="a in m.attachments"
                      :key="a.id"
                    >
                      <a
                        v-if="a.url"
                        :href="a.url"
                        target="_blank"
                        rel="noopener"
                        class="conv__attach-link"
                      >
                        📎 {{ a.filename || t('conversations.attachment') }}
                      </a>
                      <span v-else>
                        📎 {{ a.filename || t('conversations.attachment') }}
                      </span>
                    </li>
                  </ul>
"""

nice_attachment_block = """
                  <div v-if="m.attachments.length" class="conv__attach-nice">
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
                  </div>
"""

# I need to add CSS for this!
css_additions = """
.conv__attach-nice { display: flex; flex-direction: column; gap: 8px; margin-top: 8px; }
.conv__attach-img-link { display: block; max-width: 250px; border-radius: 8px; overflow: hidden; border: 1px solid rgba(0,0,0,0.1); }
.conv__attach-img { display: block; width: 100%; height: auto; object-fit: contain; }
.conv__attach-card { display: flex; align-items: center; gap: 8px; background: rgba(0,0,0,0.05); padding: 8px 12px; border-radius: 8px; text-decoration: none; border: 1px solid rgba(0,0,0,0.1); }
.conv__attach-name { font-size: 13px; color: var(--nad-ink, #1e293b); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv__attach-view { font-size: 12px; color: var(--el-color-primary, #4338ca); text-decoration: underline; }
.conv__attach-missing { font-size: 12px; color: var(--el-color-danger, #ef4444); font-style: italic; }
"""

# replace the block
if "class=\"conv__attach\"" in content:
    # simple replace
    content = re.sub(r'<ul[^>]*v-if="m.attachments.length"[^>]*class="conv__attach"[^>]*>.*?</ul>', nice_attachment_block, content, flags=re.DOTALL)

if ".conv__attach-nice" not in content:
    content = content.replace("</style>", css_additions + "\n</style>")

# Write back
with open('/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/conversations/index.vue', 'w') as f:
    f.write(content)
