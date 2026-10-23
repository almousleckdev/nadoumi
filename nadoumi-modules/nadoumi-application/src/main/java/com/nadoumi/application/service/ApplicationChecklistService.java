package com.nadoumi.application.service;

import com.nadoumi.applicant.service.ApplicantDossier;
import com.nadoumi.applicant.service.ApplicantDossierService;
import com.nadoumi.application.domain.Application;
import com.nadoumi.application.service.RequirementApplicability.Facts;
import com.nadoumi.common.document.RequirementCondition;
import com.nadoumi.program.service.ProgramService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.document.domain.Document;
import com.nadoumi.document.domain.DocumentRequirement;
import com.nadoumi.document.domain.enums.DocumentStatus;
import com.nadoumi.document.domain.enums.RequirementScope;
import com.nadoumi.document.service.DocumentRequirementService;
import com.nadoumi.document.service.DocumentService;
import com.nadoumi.scholarship.service.ScholarshipAdminService;
import com.nadoumi.scholarship.web.response.PublicScholarshipResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The documents an application needs, and where the student stands on each. The list comes from the
 * programme's requirements and the scholarship's, merged by document type (mandatory if either says so); each
 * row is matched to the student's best document of that type: one uploaded for this application, or one the
 * student keeps on their profile (a passport). A document of another application is never used.
 */
@Service
public class ApplicationChecklistService {

    /** {@code MISSING} when the student has no usable document of that type yet. */
    public enum ItemStatus { MISSING, SUBMITTED, IN_REVIEW, VERIFIED, REJECTED, EXPIRED, DRAFT }

    public record Item(String docType, boolean mandatory, String note, ItemStatus status, Long documentId,
            String rejectionReason) {

        /** Uploaded and not turned down: staff have it or will review it. */
        public boolean attached() {
            return status == ItemStatus.SUBMITTED || status == ItemStatus.IN_REVIEW || status == ItemStatus.VERIFIED;
        }
    }

    private static final Comparator<Document> BEST_FIRST = Comparator
            .comparingInt((Document d) -> rank(d.getStatus()))
            .thenComparing(Comparator.comparing(Document::getId).reversed());

    private final ApplicationMapper applications;
    private final DocumentRequirementService requirements;
    private final ScholarshipAdminService scholarships;
    private final DocumentService documents;
    private final ApplicantDossierService dossiers;
    private final ProgramService programs;

    public ApplicationChecklistService(ApplicationMapper applications, DocumentRequirementService requirements,
            ScholarshipAdminService scholarships, DocumentService documents,
            ApplicantDossierService dossiers, ProgramService programs) {
        this.applications = applications;
        this.requirements = requirements;
        this.scholarships = scholarships;
        this.documents = documents;
        this.dossiers = dossiers;
        this.programs = programs;
    }

    @Transactional(readOnly = true)
    public List<Item> checklist(long applicationId) {
        Application application = applications.findById(applicationId);
        if (application == null) {
            throw new NadNotFoundException("application not found");
        }
        List<Document> mine = documents.search(application.getApplicantId(), null).stream()
                .filter(d -> d.getApplicationId() == null || d.getApplicationId().equals(applicationId))
                .toList();
        List<Item> items = new ArrayList<>();
        Facts facts = facts(application);
        for (Wanted wanted : wanted(application, facts).values()) {
            Optional<Document> best = mine.stream()
                    .filter(d -> d.getDocType().equals(wanted.docType))
                    .min(BEST_FIRST);
            items.add(new Item(wanted.docType, wanted.mandatory, wanted.note,
                    best.map(d -> ItemStatus.valueOf(d.getStatus().name())).orElse(ItemStatus.MISSING),
                    best.map(Document::getId).orElse(null),
                    best.filter(d -> d.getStatus() == DocumentStatus.REJECTED).map(Document::getRejectionReason).orElse(null)));
        }
        return List.copyOf(items);
    }

    /** The mandatory document types the student has not uploaded yet (or whose upload was turned down). */
    @Transactional(readOnly = true)
    public List<String> missingMandatory(long applicationId) {
        return checklist(applicationId).stream()
                .filter(i -> i.mandatory() && !i.attached())
                .map(Item::docType)
                .toList();
    }

    private Map<String, Wanted> wanted(Application application, Facts facts) {
        Map<String, Wanted> byType = new LinkedHashMap<>();
        if (application.getProgramId() != null) {
            for (DocumentRequirement r : requirements.checklistFor(RequirementScope.PROGRAM, application.getProgramId())) {
                if (applies(r.getAppliesWhen(), facts)) {
                    merge(byType, r.getDocType(), r.isMandatory(), r.getNotes());
                }
            }
        }
        if (application.getScholarshipId() != null) {
            List<PublicScholarshipResponse.DocumentRequirement> list =
                    scholarships.get(application.getScholarshipId()).view().documentRequirements();
            if (list != null) {
                list.stream().filter(r -> applies(r.appliesWhen(), facts))
                        .forEach(r -> merge(byType, r.docType(), r.mandatory(), r.note()));
            }
        }
        return byType;
    }

    private static boolean applies(String condition, Facts facts) {
        return RequirementApplicability.applies(RequirementCondition.ofOrNull(condition), facts, LocalDate.now(ZoneOffset.UTC));
    }

    /** The student's facts from their profile; the level comes from their study interests, else the programme. */
    private Facts facts(Application application) {
        ApplicantDossier dossier = dossiers.read(application.getApplicantId());
        LocalDate dob = parseDate(dossier.profile().dob());
        Boolean inChina = dossier.residence() == null ? null : dossier.residence().inChina();
        Boolean studied = dossier.residence() == null ? null
                : dossier.residence().inChina() || notBlank(dossier.residence().chinaEducationLevel()) || notBlank(dossier.residence().chinaSchool());
        List<String> levels = new ArrayList<>();
        if (dossier.interests() != null) {
            levels.add(dossier.interests().desiredLevel());
        }
        if (levels.stream().allMatch(java.util.Objects::isNull) && application.getProgramId() != null) {
            levels.addAll(programs.get(application.getProgramId()).levels());
        }
        return new Facts(dob, inChina, studied, RequirementApplicability.highestLevel(levels));
    }

    private static LocalDate parseDate(String iso) {
        try {
            return iso == null ? null : LocalDate.parse(iso);
        }
        catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static void merge(Map<String, Wanted> byType, String docType, boolean mandatory, String note) {
        byType.merge(docType, new Wanted(docType, mandatory, note), (a, b) ->
                new Wanted(docType, a.mandatory || b.mandatory, a.note != null ? a.note : b.note));
    }

    private static int rank(DocumentStatus status) {
        return switch (status) {
            case VERIFIED -> 0;
            case IN_REVIEW -> 1;
            case SUBMITTED -> 2;
            case REJECTED -> 3;
            case EXPIRED -> 4;
            case DRAFT -> 5;
        };
    }

    private record Wanted(String docType, boolean mandatory, String note) {
    }
}
