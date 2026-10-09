package com.nadoumi.applicant.service;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.mapper.ApplicantErasureMapper;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.common.erasure.ApplicantErasureParticipant;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.identity.access.CurrentCaller;
import com.nadoumi.identity.service.AccountRetirement;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Permanently deletes an applicant and everything that hangs off it, for any applicant (with or without a
 * student account, with or without applications). The order matters because of the foreign keys: grants first
 * (they can point at applications), then every other module's data through {@link ApplicantErasureParticipant},
 * then this module's own rows and files, and last the student accounts left with no applicant.
 */
@Service
public class ApplicantErasureService {

    private static final Logger log = LoggerFactory.getLogger(ApplicantErasureService.class);

    private final ApplicantMapper applicants;
    private final ApplicantErasureMapper erasure;
    private final AccountRetirement accounts;
    private final ObjectProvider<ApplicantErasureParticipant> participants;
    private final MediaGateway media;
    private final CurrentCaller caller;

    public ApplicantErasureService(ApplicantMapper applicants, ApplicantErasureMapper erasure,
            AccountRetirement accounts, ObjectProvider<ApplicantErasureParticipant> participants,
            MediaGateway media, CurrentCaller caller) {
        this.applicants = applicants;
        this.erasure = erasure;
        this.accounts = accounts;
        this.participants = participants;
        this.media = media;
        this.caller = caller;
    }

    @Transactional(rollbackFor = Exception.class)
    public void erase(long applicantId) {
        if (!caller.isStaff()) {
            throw new AccessDeniedException("deleting an applicant is a staff action");
        }
        long staffId = caller.requireUserId();
        Applicant applicant = applicants.findById(applicantId);
        if (applicant == null) {
            throw new NadNotFoundException("applicant not found");
        }
        List<Long> users = accounts.usersOf(applicantId);

        accounts.eraseGrants(applicantId);
        participants.orderedStream().forEach(p -> p.erase(applicantId));
        erasure.deleteContacts(applicantId);
        erasure.deleteEducation(applicantId);
        erasure.deleteInterestChoices(applicantId);
        erasure.deleteInterests(applicantId);
        erasure.deleteResidence(applicantId);
        erasure.deleteTestScores(applicantId);
        erasure.deleteWork(applicantId);
        erasure.deleteApplicant(applicantId);
        Stream.of(applicant.getPhotoMediaId(), applicant.getPassportMediaId())
                .filter(Objects::nonNull)
                .forEach(id -> media.softDelete(id, staffId));
        accounts.retireOrphans(users, staffId);
        log.info("applicant {} permanently deleted by staff {}", applicantId, staffId);
    }
}
