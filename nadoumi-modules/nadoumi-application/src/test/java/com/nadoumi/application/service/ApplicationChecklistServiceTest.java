package com.nadoumi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.applicant.service.ApplicantDossier;
import com.nadoumi.applicant.service.ApplicantDossierService;
import com.nadoumi.application.domain.Application;
import com.nadoumi.program.service.ProgramService;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.application.service.ApplicationChecklistService.Item;
import com.nadoumi.application.service.ApplicationChecklistService.ItemStatus;
import com.nadoumi.document.domain.Document;
import com.nadoumi.document.domain.DocumentRequirement;
import com.nadoumi.document.domain.enums.DocumentStatus;
import com.nadoumi.document.domain.enums.RequirementScope;
import com.nadoumi.document.service.DocumentRequirementService;
import com.nadoumi.document.service.DocumentService;
import com.nadoumi.scholarship.service.ScholarshipAdminService;
import com.nadoumi.scholarship.web.response.PublicScholarshipResponse;
import com.nadoumi.scholarship.web.response.ScholarshipResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationChecklistServiceTest {

    private static final long APP = 42L;
    private static final long APPLICANT = 9L;

    private final ApplicationMapper applications = mock(ApplicationMapper.class);
    private final DocumentRequirementService requirements = mock(DocumentRequirementService.class);
    private final ScholarshipAdminService scholarships = mock(ScholarshipAdminService.class);
    private final DocumentService documents = mock(DocumentService.class);
    private final ApplicantDossierService dossiers = mock(ApplicantDossierService.class);
    private final ProgramService programs = mock(ProgramService.class);
    private final ApplicationChecklistService service =
            new ApplicationChecklistService(applications, requirements, scholarships, documents, dossiers, programs);

    @BeforeEach
    void application() {
        com.nadoumi.program.web.response.ProgramResponse program = mock(com.nadoumi.program.web.response.ProgramResponse.class);
        when(program.levels()).thenReturn(List.of("BACHELOR"));
        when(programs.get(5L)).thenReturn(program);
        profile("2001-04-05", null, null);
        Application a = new Application();
        a.setId(APP);
        a.setApplicantId(APPLICANT);
        a.setProgramId(5L);
        when(applications.findById(APP)).thenReturn(a);
    }

    private void profile(String dob, com.nadoumi.applicant.web.response.ResidenceResponse residence,
            com.nadoumi.applicant.web.response.InterestResponse interests) {
        ApplicantDossier built = dossier(dob, residence, interests);
        when(dossiers.read(APPLICANT)).thenReturn(built);
    }

    private static ApplicantDossier dossier(String dob, com.nadoumi.applicant.web.response.ResidenceResponse residence,
            com.nadoumi.applicant.web.response.InterestResponse interests) {
        var profile = mock(com.nadoumi.applicant.web.response.ApplicantResponse.class);
        when(profile.dob()).thenReturn(dob);
        return new ApplicantDossier(profile, null, residence, interests, List.of(), List.of(), List.of(), null, List.of(), null, null);
    }

    private static DocumentRequirement requirement(String type, boolean mandatory, String notes) {
        DocumentRequirement r = new DocumentRequirement();
        r.setDocType(type);
        r.setMandatory(mandatory);
        r.setNotes(notes);
        return r;
    }

    private static Document document(long id, String type, DocumentStatus status, Long applicationId) {
        Document d = new Document();
        d.setId(id);
        d.setDocType(type);
        d.setStatus(status);
        d.setApplicationId(applicationId);
        d.setRejectionReason(status == DocumentStatus.REJECTED ? "Blurry scan" : null);
        return d;
    }

    private void programWants(DocumentRequirement... wanted) {
        when(requirements.checklistFor(RequirementScope.PROGRAM, 5L)).thenReturn(List.of(wanted));
    }

    private void studentHas(Document... docs) {
        when(documents.search(APPLICANT, null)).thenReturn(List.of(docs));
    }

    @Test
    void shouldMarkEverythingMissing_whenTheStudentHasUploadedNothing() {
        programWants(requirement("PASSPORT", true, null), requirement("CV", false, "One page"));
        studentHas();

        List<Item> items = service.checklist(APP);

        assertThat(items).extracting(Item::docType).containsExactly("PASSPORT", "CV");
        assertThat(items).extracting(Item::status).containsOnly(ItemStatus.MISSING);
        assertThat(service.missingMandatory(APP)).containsExactly("PASSPORT");
    }

    @Test
    void shouldCountAnUploadedDocumentAsAttached_butNotARejectedOne() {
        programWants(requirement("PASSPORT", true, null), requirement("TRANSCRIPT", true, null));
        studentHas(document(1, "PASSPORT", DocumentStatus.SUBMITTED, null), document(2, "TRANSCRIPT", DocumentStatus.REJECTED, APP));

        assertThat(service.missingMandatory(APP)).containsExactly("TRANSCRIPT");
        Item rejected = service.checklist(APP).get(1);
        assertThat(rejected.status()).isEqualTo(ItemStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("Blurry scan");
    }

    @Test
    void shouldPreferTheBestDocumentOfAType() {
        programWants(requirement("TRANSCRIPT", true, null));
        studentHas(document(1, "TRANSCRIPT", DocumentStatus.REJECTED, APP), document(2, "TRANSCRIPT", DocumentStatus.VERIFIED, APP));

        assertThat(service.checklist(APP).get(0).documentId()).isEqualTo(2L);
    }

    @Test
    void shouldNeverUseADocumentOfAnotherApplication() {
        programWants(requirement("TRANSCRIPT", true, null));
        studentHas(document(1, "TRANSCRIPT", DocumentStatus.VERIFIED, 777L));

        assertThat(service.checklist(APP).get(0).status()).isEqualTo(ItemStatus.MISSING);
    }

    @Test
    void shouldMergeProgrammeAndScholarshipRequirements_mandatoryIfEitherSaysSo() {
        Application a = new Application();
        a.setId(APP);
        a.setApplicantId(APPLICANT);
        a.setProgramId(5L);
        a.setScholarshipId(8L);
        when(applications.findById(APP)).thenReturn(a);
        programWants(requirement("CV", false, null), requirement("PASSPORT", true, null));
        PublicScholarshipResponse view = mock(PublicScholarshipResponse.class);
        when(view.documentRequirements()).thenReturn(List.of(
                new PublicScholarshipResponse.DocumentRequirement("CV", true, "Max 2 pages", null),
                new PublicScholarshipResponse.DocumentRequirement("STUDY_PLAN", false, null, null)));
        ScholarshipResponse scholarship = mock(ScholarshipResponse.class);
        when(scholarship.view()).thenReturn(view);
        when(scholarships.get(8L)).thenReturn(scholarship);
        studentHas();

        List<Item> items = service.checklist(APP);

        assertThat(items).extracting(Item::docType).containsExactly("CV", "PASSPORT", "STUDY_PLAN");
        assertThat(items.get(0).mandatory()).isTrue();
        assertThat(items.get(0).note()).isEqualTo("Max 2 pages");
    }

    @Test
    void shouldHaveNothingRequired_whenNoRequirementsAreConfigured() {
        programWants();
        studentHas();

        assertThat(service.checklist(APP)).isEmpty();
        assertThat(service.missingMandatory(APP)).isEmpty();
    }

    private static DocumentRequirement conditional(String type, String condition) {
        DocumentRequirement r = requirement(type, true, null);
        r.setAppliesWhen(condition);
        return r;
    }

    @Test
    void shouldLeaveOutARequirementThatDoesNotApplyToThisStudent() {
        programWants(requirement("PASSPORT", true, null), conditional("GUARANTEE_LETTER", "MINOR"),
                conditional("TRANSFER_CERTIFICATE", "STUDIED_IN_CHINA"));
        studentHas();
        // an adult (born 2001) who has never been in China
        profile("2001-04-05",
                new com.nadoumi.applicant.web.response.ResidenceResponse(false, "GN", "Conakry", null, null, null, null, null), null);

        assertThat(service.checklist(APP)).extracting(Item::docType).containsExactly("PASSPORT");
    }

    @Test
    void shouldIncludeAConditionalRequirement_whenItAppliesOrTheFactIsUnknown() {
        programWants(conditional("GUARANTEE_LETTER", "MINOR"), conditional("STUDY_PLAN", "MASTER_OR_DOCTORAL"));
        studentHas();
        var master = new com.nadoumi.applicant.web.response.InterestResponse("MASTER", List.of(), List.of(), null, null, null, null, null);
        var minor = java.time.LocalDate.now().minusYears(15).toString();
        profile(minor, null, master);

        assertThat(service.checklist(APP)).extracting(Item::docType).containsExactly("GUARANTEE_LETTER", "STUDY_PLAN");

        profile(null, null, null); // nothing known about the student ...
        when(programs.get(5L).levels()).thenReturn(List.of()); // ... nor about the programme: ask for everything
        assertThat(service.checklist(APP)).hasSize(2);
    }
}
