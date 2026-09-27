import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-web/app/pages/dashboard/support/[id].vue"
with open(path, "r") as f:
    text = f.read()

script_add = """
const { studentFetch } = useApi()
async function closeTicket() {
  const ok = await run(() => studentFetch(`/api/student/support/tickets/${id}/close`, { method: 'POST' }))
  if (ok) await load(true)
}
"""

text = text.replace("async function onSend(body: string) {", script_add + "\nasync function onSend(body: string) {")

template_add = """
          <NBadge :tone="ticketStatusTone(detail.ticket.status)" data-test="status">
            {{ t(`dashboard.support.status.${detail.ticket.status}`) }}
          </NBadge>
          <NButton v-if="detail.ticket.status !== 'CLOSED' && detail.ticket.status !== 'RESOLVED'" variant="secondary" size="sm" class="ml-auto" @click="closeTicket">{{ t('dashboard.support.action.CLOSED') || 'Close Ticket' }}</NButton>
"""

text = re.sub(r'<NBadge :tone="ticketStatusTone\(detail\.ticket\.status\)" data-test="status">\s*\{\{ t\(`dashboard\.support\.status\.\$\{detail\.ticket\.status\}`\) \}\}\s*<\/NBadge>', template_add, text)

with open(path, "w") as f:
    f.write(text)

