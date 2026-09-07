package com.nadoumi.program.mapper;

import com.nadoumi.program.domain.Program;
import com.nadoumi.program.domain.ProgramIntake;
import com.nadoumi.program.domain.ProgramMajor;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProgramMapper {

    Program findById(@Param("id") Long id);

    /**
     * PageHelper-paged list. Every {@link ProgramSearch} field is optional; a null
     * field is not applied. The service sets {@code status} / {@code publishStatus}
     * (public reads force ACTIVE + PUBLISHED).
     */
    List<Program> search(ProgramSearch filter);

    int insert(Program program);

    int update(Program program);

    int delete(@Param("id") Long id);

    int updateImageMediaId(@Param("id") long id, @Param("mediaId") Long mediaId);

    Long findIdByUniversityAndName(@Param("universityId") Long universityId, @Param("name") String name);

    Long findIdBySlug(@Param("slug") String slug);

    // ---- children ----

    List<String> findLevels(@Param("programId") Long programId);

    int deleteLevels(@Param("programId") Long programId);

    int insertLevel(@Param("programId") Long programId, @Param("level") String level,
            @Param("sortOrder") int sortOrder);

    List<ProgramMajor> findMajors(@Param("programId") Long programId);

    int deleteMajors(@Param("programId") Long programId);

    int insertMajor(ProgramMajor major);

    List<ProgramIntake> findIntakes(@Param("programId") Long programId);

    int deleteIntakes(@Param("programId") Long programId);

    int insertIntake(ProgramIntake intake);
}
