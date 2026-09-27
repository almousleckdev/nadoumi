import re

path1 = "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-support/src/main/java/com/nadoumi/support/mapper/SupportTicketMapper.java"
with open(path1, "r") as f:
    text1 = f.read()

text1 = text1.replace("int updateCategory(@Param(\"id\") long id, @Param(\"category\") String category, @Param(\"updateBy\") String updateBy);", "int updateCategory(@Param(\"id\") long id, @Param(\"category\") String category, @Param(\"updateBy\") String updateBy);\n\n    int updateResolvedAt(@Param(\"id\") long id, @Param(\"resolvedAt\") java.time.LocalDateTime resolvedAt, @Param(\"updateBy\") String updateBy);\n\n    int updateClosedAt(@Param(\"id\") long id, @Param(\"closedAt\") java.time.LocalDateTime closedAt, @Param(\"updateBy\") String updateBy);")

with open(path1, "w") as f:
    f.write(text1)

path2 = "/Users/mac/Desktop/nadoumi/nadoumi-modules/nadoumi-support/src/main/resources/mapper/support/SupportTicketMapper.xml"
with open(path2, "r") as f:
    text2 = f.read()

script_add = """
    <update id="updateResolvedAt">
        update nad_support_ticket set resolved_at = #{resolvedAt}, update_by = #{updateBy}, update_time = now()
        where id = #{id}
    </update>

    <update id="updateClosedAt">
        update nad_support_ticket set closed_at = #{closedAt}, update_by = #{updateBy}, update_time = now()
        where id = #{id}
    </update>
"""

text2 = text2.replace("</mapper>", script_add + "</mapper>")

with open(path2, "w") as f:
    f.write(text2)

