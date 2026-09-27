import re

with open('/Users/mac/Desktop/nadoumi/nadoumi-web/app/pages/dashboard/messages/index.vue', 'r') as f:
    content = f.read()

# Add useConversationStream
if 'import { useConversationStream }' not in content:
    content = content.replace(
        "import { formatMessageTime, mergeMessages } from '~/utils/messages'",
        "import { formatMessageTime, mergeMessages } from '~/utils/messages'\nimport { useConversationStream } from '~/composables/useConversationStream'"
    )

stream_code = """
// --- Real-time Stream ---
const { reconnecting } = useConversationStream(
  (refId) => {
    void loadInbox(true)
    if (selectedId.value !== null && refId === selectedId.value) {
      void syncThreadLatest(selectedId.value)
    }
  },
  () => {
    void loadInbox(true)
    if (selectedId.value !== null) {
      void syncThreadLatest(selectedId.value)
    }
  }
)
"""

if 'useConversationStream(' not in content:
    content = content.replace(
        "const isClosed = computed(() => selectedConversation.value?.status === 'CLOSED')",
        "const isClosed = computed(() => selectedConversation.value?.status === 'CLOSED')\n\n" + stream_code
    )

with open('/Users/mac/Desktop/nadoumi/nadoumi-web/app/pages/dashboard/messages/index.vue', 'w') as f:
    f.write(content)
