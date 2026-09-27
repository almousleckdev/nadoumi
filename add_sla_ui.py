import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-web/app/pages/dashboard/support/[id].vue"
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

text = text.replace("const formatTime = (iso: string) =>", script_add + "\nconst formatTime = (iso: string) =>")

template_add = """
            <p v-if="detail.ticket.resolvedAt || detail.ticket.closedAt" class="mt-1 text-xs font-semibold text-slate-500">
              ⏱️ Time to Resolution: {{ computeSLA(detail.ticket) }}
            </p>
"""

text = text.replace("{{ t(`dashboard.support.category.${detail.ticket.category}`) }} · {{ formatTime(detail.ticket.createTime) }}\n            </p>", "{{ t(`dashboard.support.category.${detail.ticket.category}`) }} · {{ formatTime(detail.ticket.createTime) }}\n            </p>" + template_add)

with open(path, "w") as f:
    f.write(text)

