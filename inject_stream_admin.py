import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/conversations/index.vue"
with open(path, "r") as f:
    text = f.read()

# I need to find the setup script and add the import, then replace useStaffStream with useConversationStream

text = text.replace("import { useStaffStream } from '@/composables/useStaffStream'", "import { useConversationStream } from '@/composables/useConversationStream'")

stream_init = """
const { reconnecting } = useConversationStream(
  (refId) => {
    if (selectedId.value === refId) {
      loadThread(false)
    }
    loadInbox(true)
  },
  () => {
    if (selectedId.value !== null) {
      loadThread(false)
    }
    loadInbox(true)
  }
)
"""

text = re.sub(r'const \{ reconnecting \} = useStaffStream[^)]+\)', stream_init, text)

# ensure loadThread and loadInbox can accept boolean if they don't
text = text.replace("async function loadInbox() {", "async function loadInbox(silent = false) {\n  if (!silent) pendingInbox.value = true\n")
text = text.replace("pendingInbox.value = true", "") # Remove original to avoid double, wait it's easier to just pass silent

with open(path, "w") as f:
    f.write(text)

