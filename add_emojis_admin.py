import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/conversations/index.vue"
with open(path, "r") as f:
    text = f.read()

# Add EMOJIS array
if "const EMOJIS" not in text:
    text = text.replace("const MASK = '••••'", "const MASK = '••••'\nconst EMOJIS = ['👍', '😂', '🔥', '❤️', '👏', '🎉', '😊', '🙌', '👀', '🤔', '✅', '🙏', '💯', '✨']")
    if "const EMOJIS" not in text: # fallback if MASK is not there
        text = text.replace("const searchStudent = ref('')", "const searchStudent = ref('')\nconst EMOJIS = ['👍', '😂', '🔥', '❤️', '👏', '🎉', '😊', '🙌', '👀', '🤔', '✅', '🙏', '💯', '✨']")

# Add emoji button and popover
emoji_html = """
                    <el-popover placement="top-start" :width="200" trigger="click">
                      <template #reference>
                        <el-button size="small" text style="font-size: 16px; padding: 4px;">😀</el-button>
                      </template>
                      <div style="display: flex; flex-wrap: wrap; gap: 8px;">
                        <span v-for="e in EMOJIS" :key="e" style="cursor: pointer; font-size: 20px;" @click="draft += e">{{ e }}</span>
                      </div>
                    </el-popover>
"""

# Inject next to the file input button
if "triggerFilePick" in text and "el-popover" not in text:
    text = re.sub(r'(<el-button[^>]*@click="triggerFilePick"[^>]*>[\s\S]*?</el-button>)', r'\1\n' + emoji_html, text)

# Remove sender name above messages
text = re.sub(r'<p v-if="index === 0 \|\| messages\[index - 1\]\.senderUserId !== m\.senderUserId" class="conv__sender">\s*\{\{ isMine\(m\) \? t\(\'conversations\.you\'\) : \(m\.senderName \|\| `\#\$\{m\.senderUserId\}`\) \}\}\s*</p>', '', text)

with open(path, "w") as f:
    f.write(text)

