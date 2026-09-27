import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/support/SupportTicketDrawer.vue"
with open(path, "r") as f:
    text = f.read()

script_add = """
function computeSLA(t: any) {
  const end = new Date(t.resolvedAt || t.closedAt || t.updateTime).getTime()
  const start = new Date(t.createTime).getTime()
  const diff = end - start
  if (diff < 0) return '0h'
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  return `${hours}h ${minutes}m`
}
"""

text = text.replace("function eventText(e: TicketEvent): string {", script_add + "\nfunction eventText(e: TicketEvent): string {")

template_add = """
          <div>
            <dt>{{ t('support.resolutionTime', 'SLA (Time to Resolution)') }}</dt>
            <dd>{{ detail.ticket.resolvedAt || detail.ticket.closedAt ? computeSLA(detail.ticket) : 'Ongoing' }}</dd>
          </div>
"""

text = text.replace("<div>\n            <dt>{{ t('support.assigned') }}</dt>", template_add + "\n          <div>\n            <dt>{{ t('support.assigned') }}</dt>")

with open(path, "w") as f:
    f.write(text)

