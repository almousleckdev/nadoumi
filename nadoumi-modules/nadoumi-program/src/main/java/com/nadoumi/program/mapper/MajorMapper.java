package com.nadoumi.program.mapper;

import com.nadoumi.program.domain.Major;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface MajorMapper {

    Major findById(@Param("id") Long id);

    /** The major with this name; the column collation makes the match case-insensitive. */
    Major findByName(@Param("name") String name);

    /** PageHelper-paged. A blank {@code q} and a null {@code discipline} are not applied. */
    List<Major> search(@Param("q") String q, @Param("discipline") String discipline);

    int insert(Major major);

    int update(Major major);

    int delete(@Param("id") Long id);

    int countLinks(@Param("id") Long id);

    /** Re-points every programme link of {@code fromId} to {@code toId}. */
    int moveLinks(@Param("fromId") Long fromId, @Param("toId") Long toId);
}
