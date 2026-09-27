import re

with open('/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/conversations/index.vue', 'r') as f:
    content = f.read()

# Replace the conv__messages div with a much cleaner version
start_marker = '<div\n                v-for="m in messages"'
# We want to replace the whole v-for loop
# We can find where the loop ends.
# It ends right before `</template>` around line 265.
# Let's just find it with regex.

pattern = re.compile(r'(<div\s+v-for="\(m, i\) in messages"|<div\s+v-for="m in messages").*?(?=<!-- Composer -->|<div\s+class="conv__composer)', re.DOTALL)

replacement = """<div
                v-for="(m, index) in messages"
                :key="m.id"
                class="conv__msg"
                :class="{ 'conv__msg--mine': isMine(m), 'conv__msg--chained': index > 0 && messages[index - 1].senderUserId === m.senderUserId }"
                data-test="message"
              >
                <div class="conv__bubble">
                  <p v-if="index === 0 || messages[index - 1].senderUserId !== m.senderUserId" class="conv__sender">
                    {{ isMine(m) ? t('conversations.you') : (m.senderName || `#${m.senderUserId}`) }}
                  </p>
                  <p class="conv__body">
                    {{ m.body }}
                  </p>

                  <div v-if="m.attachments && m.attachments.length" class="conv__attachments">
                    <div v-for="a in m.attachments" :key="a.id" class="conv__attachment-item">
                      <el-image
                        v-if="a.filename && a.filename.match(/\\.(jpeg|jpg|gif|png|webp)$/i)"
                        :src="a.url"
                        class="conv__image-preview"
                        :preview-src-list="[a.url]"
                        fit="cover"
                        lazy
                      />
                      <a v-else :href="a.url" target="_blank" rel="noopener noreferrer" class="conv__file-card">
                        <el-icon class="conv__file-icon"><Document /></el-icon>
                        <span class="conv__file-name" :title="a.filename">{{ a.filename || 'Attachment' }}</span>
                      </a>
                    </div>
                  </div>
                  
                  <p class="conv__time">
                    {{ formatTime(m.createdAt) }}
                  </p>
                </div>
              </div>
              """

new_content = pattern.sub(replacement, content)

with open('/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/conversations/index.vue', 'w') as f:
    f.write(new_content)
