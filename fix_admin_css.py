import re

with open('/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/conversations/index.vue', 'r') as f:
    content = f.read()

# Remove the old hacky CSS
css_hack = r'\.conv__attach-nice \{.*?\.conv__attach-missing \{[^\}]*\}'
content = re.sub(css_hack, '', content, flags=re.DOTALL)

# Add new beautiful CSS before </style>
new_css = """
.conv__msg--chained { margin-top: -6px; }
.conv__attachments { display: flex; flex-direction: column; gap: 8px; margin-top: 8px; }
.conv__image-preview { border-radius: 8px; border: 1px solid var(--nad-line, #e2e8f0); max-width: 240px; max-height: 240px; display: block; }
.conv__file-card { display: flex; align-items: center; gap: 8px; padding: 10px 14px; background: var(--nad-surface, #ffffff); border: 1px solid var(--nad-line, #e2e8f0); border-radius: 8px; text-decoration: none; transition: background 0.2s; max-width: 280px; }
.conv__file-card:hover { background: var(--nad-surface-2, #f8fafc); }
.conv__file-icon { font-size: 18px; color: var(--el-color-primary, #4338ca); }
.conv__file-name { font-size: 13px; color: var(--nad-ink, #1e293b); font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
"""

content = content.replace("</style>", new_css + "\n</style>")

with open('/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/conversations/index.vue', 'w') as f:
    f.write(content)
