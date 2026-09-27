import re

path = "/Users/mac/Desktop/nadoumi/nadoumi-web/app/pages/dashboard/messages/index.vue"
with open(path, "r") as f:
    text = f.read()

# 1. Remove sender name from message bubble
text = re.sub(r'<p v-if="isFirstInGroup\(index\) && !isMine\(m\)" class="text-xs font-medium text-slate-500 mb-1 px-1">\s*\{\{ m\.senderName || \'Staff\' \}\}\s*</p>', '', text)

# 2. Add emoji picker to composer
# Wait, I need to know where the composer is.
# I'll just remove the name first.

with open(path, "w") as f:
    f.write(text)

