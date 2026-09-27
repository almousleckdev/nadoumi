import re

path1 = "/Users/mac/Desktop/nadoumi/nadoumi-web/app/types/support.ts"
with open(path1, "r") as f:
    text1 = f.read()

if "resolvedAt?: string" not in text1:
    text1 = text1.replace("updateTime: string", "updateTime: string\n  resolvedAt?: string\n  closedAt?: string")

with open(path1, "w") as f:
    f.write(text1)

path2 = "/Users/mac/Desktop/nadoumi/nadoumi-admin/src/api/support.ts"
with open(path2, "r") as f:
    text2 = f.read()

if "resolvedAt?: string" not in text2:
    text2 = text2.replace("updateTime: string", "updateTime: string\n  resolvedAt?: string\n  closedAt?: string")

with open(path2, "w") as f:
    f.write(text2)

