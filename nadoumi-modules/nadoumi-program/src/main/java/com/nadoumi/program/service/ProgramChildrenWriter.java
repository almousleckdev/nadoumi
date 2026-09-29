package com.nadoumi.program.service;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.text.Texts;
import com.nadoumi.program.domain.Program;
import com.nadoumi.program.domain.ProgramIntake;
import com.nadoumi.program.domain.ProgramMajor;
import com.nadoumi.program.mapper.ProgramMapper;
import com.nadoumi.program.web.request.ProgramRequest;
import com.nadoumi.university.service.DepartmentService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/** Replaces a programme's levels, majors and intakes with the request's, inside the caller's transaction. */
@Component
public class ProgramChildrenWriter {

    private static final List<String> DEGREE_LEVELS = List.of("DIPLOMA", "BACHELOR", "MASTER", "PHD");

    private final ProgramMapper mapper;
    private final DepartmentService departmentService;

    public ProgramChildrenWriter(ProgramMapper mapper, DepartmentService departmentService) {
        this.mapper = mapper;
        this.departmentService = departmentService;
    }

    public void replace(Program program, ProgramRequest req) {
        boolean degree = program.getProgramType() != null && program.getProgramType().isDegree();
        List<String> levels = replaceLevels(program, req, degree);
        replaceMajors(program, req, degree, levels);
        replaceIntakes(program.getId(), req.intakes());
    }

    private List<String> replaceLevels(Program program, ProgramRequest req, boolean degree) {
        Long programId = program.getId();
        mapper.deleteLevels(programId);
        List<String> levels = degree ? normalizedLevels(req.levels()) : new ArrayList<>();
        if (degree && levels.isEmpty()) {
            throw new NadBadRequestException("a degree programme needs at least one level");
        }
        levels.sort(Comparator.comparingInt(DEGREE_LEVELS::indexOf));
        int order = 0;
        for (String level : levels) {
            mapper.insertLevel(programId, level, order++);
        }
        program.setLevels(levels);
        return levels;
    }

    private static List<String> normalizedLevels(List<String> requested) {
        List<String> levels = new ArrayList<>();
        if (requested == null) {
            return levels;
        }
        for (String raw : requested) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String level = raw.trim().toUpperCase();
            if (!DEGREE_LEVELS.contains(level)) {
                throw new NadBadRequestException("unknown programme level: " + raw);
            }
            if (!levels.contains(level)) {
                levels.add(level);
            }
        }
        return levels;
    }

    /** Only degree programmes carry majors; LANGUAGE / NON_DEGREE use term_length. */
    private void replaceMajors(Program program, ProgramRequest req, boolean degree, List<String> levels) {
        Long programId = program.getId();
        mapper.deleteMajors(programId);
        if (!degree || req.majors() == null) {
            return;
        }
        int order = 0;
        for (ProgramRequest.MajorInput in : req.majors()) {
            if (in.name() == null || in.name().isBlank()) {
                continue;
            }
            requireDepartmentOfUniversity(in.departmentId(), program.getUniversityId());
            String majorLevel = in.level() == null || in.level().isBlank() ? null : in.level().trim().toUpperCase();
            if (majorLevel != null && !levels.contains(majorLevel)) {
                throw new NadBadRequestException("major level " + majorLevel + " is not one of the programme's levels");
            }
            ProgramMajor major = new ProgramMajor();
            major.setProgramId(programId);
            major.setDepartmentId(in.departmentId());
            major.setLevel(majorLevel);
            major.setName(in.name().trim());
            major.setNameCn(Texts.blankToNull(in.nameCn()));
            major.setSortOrder(order++);
            mapper.insertMajor(major);
        }
    }

    private void requireDepartmentOfUniversity(Long departmentId, Long universityId) {
        if (departmentId != null && !departmentService.belongsToUniversity(departmentId, universityId)) {
            throw new NadBadRequestException("department " + departmentId + " is not part of this university");
        }
    }

    private void replaceIntakes(Long programId, List<ProgramRequest.IntakeInput> intakes) {
        mapper.deleteIntakes(programId);
        if (intakes == null) {
            return;
        }
        int order = 0;
        for (ProgramRequest.IntakeInput in : intakes) {
            if (in.term() == null || in.term().isBlank()) {
                continue;
            }
            ProgramIntake intake = new ProgramIntake();
            intake.setProgramId(programId);
            intake.setTerm(in.term().trim());
            intake.setApplicationOpen(in.applicationOpen());
            intake.setApplicationClose(in.applicationClose());
            intake.setSortOrder(order++);
            mapper.insertIntake(intake);
        }
    }
}
