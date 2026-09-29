package com.nadoumi.application.service;

import com.alibaba.fastjson2.JSONObject;
import com.nadoumi.application.domain.Application;
import com.nadoumi.common.access.AccessGrantStatus;
import com.nadoumi.common.outbox.OutboxEventTypes;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.identity.domain.UserApplicantAccess;
import com.nadoumi.identity.service.UserApplicantAccessService;
import com.nadoumi.program.service.ProgramService;
import com.nadoumi.program.web.response.ProgramResponse;
import com.nadoumi.scholarship.service.ScholarshipAdminService;
import com.nadoumi.scholarship.web.response.ScholarshipResponse;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ApplicationOutboxEmitter {

    private static final Logger log = LoggerFactory.getLogger(ApplicationOutboxEmitter.class);
    private static final String FALLBACK_TITLE = "your application";

    private final OutboxWriter outboxWriter;
    private final UserApplicantAccessService accessService;
    private final ProgramService programService;
    private final ScholarshipAdminService scholarshipAdminService;

    public ApplicationOutboxEmitter(OutboxWriter outboxWriter, UserApplicantAccessService accessService,
            ProgramService programService, ScholarshipAdminService scholarshipAdminService) {
        this.outboxWriter = outboxWriter;
        this.accessService = accessService;
        this.programService = programService;
        this.scholarshipAdminService = scholarshipAdminService;
    }

    public void submitted(Application application) {
        emit(application, OutboxEventTypes.APPLICATION_SUBMITTED, null);
    }

    public void statusChanged(Application application, String status) {
        emit(application, OutboxEventTypes.APPLICATION_STATUS_CHANGED, status);
    }

    /** Payload keys match the templates already seeded in V55 — do not rename without updating them too. */
    private void emit(Application application, String outboxType, String status) {
        List<Long> recipients = accessService.listForApplicant(application.getApplicantId()).stream()
                .filter(g -> g.getStatus() == AccessGrantStatus.ACTIVE)
                .map(UserApplicantAccess::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream().toList();
        if (recipients.isEmpty()) {
            return;
        }
        JSONObject payload = new JSONObject();
        payload.put("applicationRef", "APP-" + application.getId());
        payload.put("opportunityTitle", opportunityTitle(application));
        if (status != null) {
            payload.put("status", status);
        }
        payload.put("recipientUserIds", recipients);
        outboxWriter.write("application", application.getId(), outboxType, payload.toJSONString());
    }

    private String opportunityTitle(Application application) {
        try {
            ProgramResponse program = programService.get(application.getProgramId());
            if (application.getScholarshipId() != null) {
                ScholarshipResponse scholarship = scholarshipAdminService.get(application.getScholarshipId());
                return program.name() + " + " + scholarship.view().title();
            }
            return program.name();
        }
        catch (RuntimeException e) {
            log.warn("could not resolve the opportunity title for application {}; using a generic label",
                    application.getId(), e);
            return FALLBACK_TITLE;
        }
    }
}
