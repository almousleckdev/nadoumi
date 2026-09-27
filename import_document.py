with open('/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/conversations/index.vue', 'r') as f:
    content = f.read()

if "import { Document }" not in content:
    content = content.replace("import { ref, onMounted, nextTick } from 'vue'", "import { ref, onMounted, nextTick } from 'vue'\nimport { Document } from '@element-plus/icons-vue'")

with open('/Users/mac/Desktop/nadoumi/nadoumi-admin/src/views/conversations/index.vue', 'w') as f:
    f.write(content)
