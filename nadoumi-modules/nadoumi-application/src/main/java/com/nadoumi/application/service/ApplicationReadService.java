package com.nadoumi.application.service;

import com.nadoumi.application.domain.Application;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.program.service.ProgramService;
import com.nadoumi.scholarship.service.ScholarshipAdminService;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only facts about an application for other modules (payment, documents). Modules reach an application
 * through this service, never through its mapper or entity.
 */
@Service
public class ApplicationReadService {

    /** The few facts another module needs about an application. */
    public record ApplicationFacts(long id, String reference, long applicantId, String status, boolean draft) {
    }

    private static final String DRAFT_STATUS = "DRAFT";

    private final ApplicationMapper mapper;
    private final ProgramService programs;
    private final ScholarshipAdminService scholarships;

    public ApplicationReadService(ApplicationMapper mapper, ProgramService programs, ScholarshipAdminService scholarships) {
        this.mapper = mapper;
        this.programs = programs;
        this.scholarships = scholarships;
    }

    /**
     * The Nadoumi service fee (RMB) of the application's programme, else of its scholarship; zero when neither sets one.
     * It is read live, so a payment session keeps the amount it was opened with while a new one follows the catalogue.
     */
    @Transactional(readOnly = true)
    public BigDecimal serviceFee(long applicationId) {
        Application application = mapper.findById(applicationId);
        if (application == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal fee = application.getProgramId() == null ? null : programs.get(application.getProgramId()).serviceFeeAmount();
        if ((fee == null || fee.signum() <= 0) && application.getScholarshipId() != null) {
            var money = scholarships.get(application.getScholarshipId()).view().serviceFee();
            fee = money == null ? null : money.amountRmb();
        }
        return fee == null || fee.signum() <= 0 ? BigDecimal.ZERO : fee;
    }

    @Transactional(readOnly = true)
    public Optional<ApplicationFacts> find(long applicationId) {
        Application application = mapper.findById(applicationId);
        if (application == null) {
            return Optional.empty();
        }
        return Optional.of(new ApplicationFacts(application.getId(), application.getReference(), application.getApplicantId(),
                application.getCurrentStatus(), DRAFT_STATUS.equals(application.getCurrentStatus())));
    }
}
