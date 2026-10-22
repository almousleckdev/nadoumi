package com.nadoumi.application.service;

import com.nadoumi.application.domain.Application;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.program.service.ProgramService;
import com.nadoumi.program.web.response.ProgramResponse;
import com.nadoumi.scholarship.service.ScholarshipAdminService;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What an application is for, worded for a document the student also sees (receipt, application form).
 *
 * <p>Confidentiality lives here and nowhere else: the university is named only for a plain programme
 * application. Whenever a scholarship is involved the university stays out of the description, so a document
 * can never reveal a partner relationship. Callers get a value with no way to ask for the hidden name.</p>
 */
@Service
public class ApplicationOpportunityService {

    public enum Kind { PROGRAM, SCHOLARSHIP }

    /** @param universityName {@code null} for any application that involves a scholarship; {@code intake} may be null */
    public record Description(Kind kind, String title, String universityName, String intake) {
    }

    private final ApplicationMapper applications;
    private final ProgramService programs;
    private final ScholarshipAdminService scholarships;

    public ApplicationOpportunityService(ApplicationMapper applications, ProgramService programs,
            ScholarshipAdminService scholarships) {
        this.applications = applications;
        this.programs = programs;
        this.scholarships = scholarships;
    }

    @Transactional(readOnly = true)
    public Description describe(long applicationId) {
        Application application = applications.findById(applicationId);
        if (application == null) {
            throw new NadNotFoundException("application not found");
        }
        boolean withScholarship = application.getScholarshipId() != null;
        if (application.getProgramId() == null) {
            return new Description(Kind.SCHOLARSHIP, scholarshipTitle(application.getScholarshipId()), null, null);
        }
        ProgramResponse program = programs.get(application.getProgramId());
        String title = withScholarship
                ? program.name() + " + " + scholarshipTitle(application.getScholarshipId())
                : program.name();
        return new Description(withScholarship ? Kind.SCHOLARSHIP : Kind.PROGRAM, title,
                withScholarship ? null : program.universityName(), intakeOf(program, application.getIntakeId()));
    }

    private String scholarshipTitle(Long scholarshipId) {
        return scholarships.get(scholarshipId).view().title();
    }

    private static String intakeOf(ProgramResponse program, Long intakeId) {
        return Optional.ofNullable(intakeId)
                .flatMap(id -> program.intakes().stream().filter(i -> id.equals(i.id())).findFirst())
                .map(ProgramResponse.Intake::term)
                .orElse(null);
    }
}
