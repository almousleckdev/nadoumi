package com.nadoumi.program.service;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.text.Texts;
import com.nadoumi.program.domain.Major;
import com.nadoumi.program.domain.Program;
import com.nadoumi.program.domain.ProgramIntake;
import com.nadoumi.program.domain.ProgramMajor;
import com.nadoumi.program.domain.ProgramOffering;
import com.nadoumi.program.mapper.ProgramMapper;
import com.nadoumi.program.web.request.ProgramRequest;
import com.nadoumi.university.service.DepartmentService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Replaces a programme's levels, majors and intakes with the request's, inside the caller's transaction. */
@Component
public class ProgramChildrenWriter {

    private static final List<String> DEGREE_LEVELS = List.of("DIPLOMA", "BACHELOR", "MASTER", "PHD");

    private final ProgramMapper mapper;
    private final DepartmentService departmentService;
    private final MajorService majors;

    public ProgramChildrenWriter(ProgramMapper mapper, DepartmentService departmentService, MajorService majors) {
        this.mapper = mapper;
        this.departmentService = departmentService;
        this.majors = majors;
    }

    public void replace(Program program, ProgramRequest req) {
        boolean degree = program.getProgramType() != null && program.getProgramType().isDegree();
        List<String> levels = replaceLevels(program, req, degree);
        replaceMajors(program, req, degree, levels);
        replaceIntakes(program.getId(), req.intakes());
        replaceOfferings(program, req, levels);
    }

    /** Offerings are optional; a null list leaves the programme with none. */
    private void replaceOfferings(Program program, ProgramRequest req, List<String> levels) {
        Long programId = program.getId();
        ProgramOfferingRules.validate(program.getProgramType(), levels, req.offerings());
        mapper.deleteOfferings(programId);
        if (req.offerings() == null) {
            return;
        }
        int order = 0;
        for (ProgramRequest.OfferingInput in : req.offerings()) {
            ProgramOffering offering = new ProgramOffering();
            offering.setProgramId(programId);
            offering.setLevel(ProgramOfferingRules.normalizedLevel(in.level()));
            offering.setDurationMonths(in.durationMonths());
            offering.setTuitionAmount(in.tuitionAmount());
            offering.setTuitionCurrency(in.tuitionAmount() == null ? null : currencyOrCny(in.tuitionCurrency()));
            offering.setTuitionPeriod(periodOr(in.tuitionPeriod(), "PER_YEAR"));
            offering.setAccommodationAmount(in.accommodationAmount());
            offering.setAccommodationCurrency(
                    in.accommodationAmount() == null ? null : currencyOrCny(in.accommodationCurrency()));
            offering.setAccommodationPeriod(periodOr(in.accommodationPeriod(), "PER_MONTH"));
            offering.setNote(Texts.blankToNull(in.note()));
            offering.setSortOrder(order++);
            mapper.insertOffering(offering);
        }
    }

    private static String currencyOrCny(String raw) {
        return raw == null || raw.isBlank() ? "CNY" : raw.trim().toUpperCase();
    }

    private static String periodOr(String raw, String fallback) {
        return raw == null || raw.isBlank() ? fallback : raw.trim().toUpperCase();
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
        Set<String> listed = new HashSet<>();
        for (ProgramRequest.MajorInput in : req.majors()) {
            Major catalogue = catalogueMajor(in);
            if (catalogue == null) {
                continue;
            }
            requireDepartmentOfUniversity(in.departmentId(), program.getUniversityId());
            String majorLevel = in.level() == null || in.level().isBlank() ? null : in.level().trim().toUpperCase();
            if (majorLevel != null && !levels.contains(majorLevel)) {
                throw new NadBadRequestException("major level " + majorLevel + " is not one of the programme's levels");
            }
            // the same major at the same level is one offering, whatever the department: a repeat is dropped
            if (!listed.add(catalogue.getId() + ":" + majorLevel)) {
                continue;
            }
            ProgramMajor major = new ProgramMajor();
            major.setProgramId(programId);
            major.setDepartmentId(in.departmentId());
            major.setLevel(majorLevel);
            major.setMajorId(catalogue.getId());
            major.setName(Texts.blankToNull(in.name()) == null ? catalogue.getName() : in.name().trim());
            major.setNameCn(Texts.blankToNull(in.nameCn()) == null ? catalogue.getNameCn() : in.nameCn().trim());
            major.setSortOrder(order++);
            mapper.insertMajor(major);
        }
    }

    /** The catalogue major a programme row stands for: the picked one, or the typed name matched to (or added to) it. */
    private Major catalogueMajor(ProgramRequest.MajorInput in) {
        if (in.majorId() != null) {
            return majors.require(in.majorId());
        }
        return Texts.blankToNull(in.name()) == null ? null : majors.findOrCreate(in.name(), in.nameCn());
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
