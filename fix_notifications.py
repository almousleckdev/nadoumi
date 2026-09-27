import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-web/app/pages/dashboard/notifications.vue"
with open(path, "r") as f:
    text = f.read()

# I want to change onOpen to deep link based on Notification type.
# And also visual differentiation.
script_add = """
const router = useRouter()
const localePath = useLocalePath()

async function onOpen(n: NotificationView) {
  if (!n.read) {
    n.read = true
    n.readAt = new Date().toISOString()
    await markRead(n.id).catch(() => { n.read = false; n.readAt = null })
  }
  
  // Deep-linking based on notification type
  const t = n.type || ''
  if (t === 'CONTACT_INQUIRY_RECEIVED' || t.includes('MESSAGE')) {
    router.push(localePath('/dashboard/messages'))
  } else if (t.includes('TICKET') || t.includes('SUPPORT')) {
    router.push(localePath('/dashboard/support'))
  } else if (t.includes('APPLICATION') || t === 'TASK_PROGRESS') {
    router.push(localePath('/dashboard/applications'))
  }
}
"""

text = re.sub(r'async function onOpen\(n: NotificationView\) \{[\s\S]*?\}', script_add, text)

# Differentiate targeted vs platform notifications visually
template_replace = """<span
                class="mt-1.5 h-2 w-2 shrink-0 rounded-full"
                :class="n.read ? 'bg-transparent' : 'bg-brand-600'"
                aria-hidden="true"
              />
              <span v-if="n.type && (n.type.includes('PUBLISHED') || n.type === 'PLATFORM_UPDATE')" class="mt-0.5 shrink-0 text-slate-400" title="System Announcement">
                📢
              </span>
              <span class="min-w-0 flex-1">
                <span class="block text-sm" :class="n.read ? 'font-medium text-slate-700' : 'font-semibold text-slate-900'">
                  <span v-if="n.type && n.type.includes('PUBLISHED')" class="inline-flex items-center rounded-md bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-700 ring-1 ring-inset ring-blue-700/10 mr-2">System</span>
                  {{ n.title }}
                </span>"""

text = re.sub(r'<span\s*class="mt-1\.5 h-2 w-2 shrink-0 rounded-full"[\s\S]*?<span class="block text-sm" [^>]*>\{\{ n\.title \}\}<\/span>', template_replace, text)

with open(path, "w") as f:
    f.write(text)

