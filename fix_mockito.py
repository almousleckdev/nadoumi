import re

with open('/Users/mac/Desktop/nadoumi/pom.xml', 'r') as f:
    content = f.read()

# Add to maven-surefire-plugin argLine
if '<argLine>-Xmx1024m</argLine>' in content:
    content = content.replace('<argLine>-Xmx1024m</argLine>', '<argLine>-Xmx1024m -XX:+EnableDynamicAgentLoading</argLine>')
elif '<plugin>\n                <groupId>org.apache.maven.plugins</groupId>\n                <artifactId>maven-surefire-plugin</artifactId>' in content:
    # try inserting argLine if not present
    pass
else:
    # Just let's append it where surefire plugin is
    content = re.sub(r'(<artifactId>maven-surefire-plugin</artifactId>\s*<version>[^<]+</version>\s*<configuration>)', 
                     r'\1\n                    <argLine>-XX:+EnableDynamicAgentLoading</argLine>', content)

with open('/Users/mac/Desktop/nadoumi/pom.xml', 'w') as f:
    f.write(content)

