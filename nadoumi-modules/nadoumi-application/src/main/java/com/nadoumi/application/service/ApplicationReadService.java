package com.nadoumi.application.service;

import com.nadoumi.application.domain.Application;
import com.nadoumi.application.mapper.ApplicationMapper;
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
    public record ApplicationFacts(long id, long applicantId, String status, boolean draft) {
    }

    private static final String DRAFT_STATUS = "DRAFT";

    private final ApplicationMapper mapper;

    public ApplicationReadService(ApplicationMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Optional<ApplicationFacts> find(long applicationId) {
        Application application = mapper.findById(applicationId);
        if (application == null) {
            return Optional.empty();
        }
        return Optional.of(new ApplicationFacts(application.getId(), application.getApplicantId(),
                application.getCurrentStatus(), DRAFT_STATUS.equals(application.getCurrentStatus())));
    }
}
