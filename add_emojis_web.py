import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-web/app/components/messages/MessageComposer.vue"
with open(path, "r") as f:
    text = f.read()

if "EMOJIS" not in text:
    text = text.replace("const pickError = ref('')", "const pickError = ref('')\nconst EMOJIS = ['👍', '😂', '🔥', '❤️', '👏', '🎉', '😊', '🙌', '👀', '🤔', '✅', '🙏', '💯', '✨']\nconst showEmoji = ref(false)")
    
emoji_html = """
      <div class="relative">
        <button type="button" @click="showEmoji = !showEmoji" class="inline-flex cursor-pointer items-center justify-center rounded-md px-2 py-1.5 text-lg hover:bg-slate-100" :title="t('dashboard.messages.emoji')">😀</button>
        <div v-if="showEmoji" class="absolute bottom-full left-0 mb-2 w-48 rounded-lg border border-slate-200 bg-white p-2 shadow-lg grid grid-cols-5 gap-2 z-10">
          <button v-for="e in EMOJIS" :key="e" type="button" class="text-xl hover:bg-slate-100 rounded" @click="draft += e; showEmoji = false">{{ e }}</button>
        </div>
      </div>
"""

if "showEmoji" not in text: # It should be there now
    pass

text = re.sub(r'(<label[^>]*for="inputId"[^>]*>[\s\S]*?</label>)', r'\1\n' + emoji_html, text)

with open(path, "w") as f:
    f.write(text)

