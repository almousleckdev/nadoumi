import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-web/app/components/dashboard/DashboardSidebar.vue"
with open(path, "r") as f:
    text = f.read()

# Add badge property to NavItem
text = text.replace("interface NavItem { to: string, key: string, icon: DashboardIconName }", "interface NavItem { to: string, key: string, icon: DashboardIconName, badge?: number }")

# Need to fetch counts.
script_add = """
const { listConversations } = useMessages()
const { unreadCount: getUnreadNotifs } = useNotifications()

const { data: badgeCounts } = useAsyncData('sidebar-badges', async () => {
  const [convos, notifs] = await Promise.all([
    listConversations().catch(() => []),
    getUnreadNotifs().catch(() => ({ count: 0 }))
  ])
  const unreadMessages = convos.reduce((sum, c) => sum + (c.unreadCount || 0), 0)
  return { messages: unreadMessages, notifications: notifs.count }
})
"""

text = text.replace("const { signOut } = useSession()", "const { signOut } = useSession()\n" + script_add)

# In template, find where we render text and add badge
template_replace = """<span v-show="!collapsed" class="truncate flex-1">{{ t(item.key) }}</span>
        <span v-if="!collapsed && (item.to === '/dashboard/messages' ? badgeCounts?.messages : item.to === '/dashboard/notifications' ? badgeCounts?.notifications : 0)" class="ml-auto inline-flex h-5 items-center justify-center rounded-full bg-brand-500 px-2 text-[10px] font-bold text-white shadow-sm ring-1 ring-inset ring-brand-500/20">
          {{ item.to === '/dashboard/messages' ? badgeCounts?.messages : badgeCounts?.notifications }}
        </span>"""

text = re.sub(r'<span v-show="!collapsed" class="truncate">\{\{\s*t\(item\.key\)\s*\}\}</span>', template_replace, text)

with open(path, "w") as f:
    f.write(text)

