package com.nadoumi.applicant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.applicant.domain.ApplicantContact;
import com.nadoumi.applicant.domain.ApplicantTestScore;
import com.nadoumi.applicant.domain.enums.ContactRelation;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.applicant.web.request.ContactRequest;
import com.nadoumi.applicant.web.request.TestScoreRequest;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ApplicantRecordsServiceTest {

    private static final long APPLICANT = 5L;
    private static final long OTHER_APPLICANT = 6L;

    private final ApplicantMapper mapper = mock(ApplicantMapper.class);
    private final NadoumiAccessService access = mock(NadoumiAccessService.class);
    private final ApplicantRecordsService service = new ApplicantRecordsService(mapper, new ApplicantAccessGuard(access));

    private final TestScoreRequest scoreRequest = new TestScoreRequest("HSK", "5", null, null, null);
    private final ContactRequest contactRequest = new ContactRequest(ContactRelation.GUARDIAN, "Omar", "o@x.com", "+2210000");

    private void allow(String capability) {
        when(access.canAccessApplicant(APPLICANT, capability)).thenReturn(true);
    }

    private static ApplicantTestScore scoreOf(long applicantId) {
        ApplicantTestScore s = new ApplicantTestScore();
        s.setId(9L);
        s.setApplicantId(applicantId);
        return s;
    }

    private static ApplicantContact contactOf(long applicantId) {
        ApplicantContact c = new ApplicantContact();
        c.setId(9L);
        c.setApplicantId(applicantId);
        return c;
    }

    @Test
    void shouldRejectListingScores_whenCallerCannotViewTheApplicant() {
        assertThatThrownBy(() -> service.testScores(APPLICANT)).isInstanceOf(AccessDeniedException.class);
        verify(mapper, never()).findTestScores(APPLICANT);
    }

    @Test
    void shouldInsertScoreForApplicant_whenCallerMayEdit() {
        allow("EDIT_PROFILE");
        var created = service.addTestScore(APPLICANT, scoreRequest);
        assertThat(created.testType()).isEqualTo("HSK");
        verify(mapper).insertTestScore(any(ApplicantTestScore.class));
    }

    @Test
    void shouldRejectAddingScore_whenCallerCannotEdit() {
        assertThatThrownBy(() -> service.addTestScore(APPLICANT, scoreRequest)).isInstanceOf(AccessDeniedException.class);
        verify(mapper, never()).insertTestScore(any());
    }

    @Test
    void shouldNotFindScore_whenItBelongsToAnotherApplicant() {
        allow("EDIT_PROFILE");
        when(mapper.findTestScoreById(9L)).thenReturn(scoreOf(OTHER_APPLICANT));
        assertThatThrownBy(() -> service.updateTestScore(APPLICANT, 9L, scoreRequest)).isInstanceOf(NadNotFoundException.class);
        verify(mapper, never()).updateTestScore(any());
    }

    @Test
    void shouldUpdateScore_whenItBelongsToTheApplicant() {
        allow("EDIT_PROFILE");
        when(mapper.findTestScoreById(9L)).thenReturn(scoreOf(APPLICANT));
        service.updateTestScore(APPLICANT, 9L, scoreRequest);
        verify(mapper).updateTestScore(any(ApplicantTestScore.class));
    }

    @Test
    void shouldReportNotFound_whenDeletingAScoreThatIsNotTheApplicants() {
        allow("EDIT_PROFILE");
        when(mapper.deleteTestScore(9L, APPLICANT)).thenReturn(0);
        assertThatThrownBy(() -> service.deleteTestScore(APPLICANT, 9L)).isInstanceOf(NadNotFoundException.class);
    }

    @Test
    void shouldRejectListingContacts_whenCallerCannotViewTheApplicant() {
        assertThatThrownBy(() -> service.contacts(APPLICANT)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldInsertContactForApplicant_whenCallerMayEdit() {
        allow("EDIT_PROFILE");
        var created = service.addContact(APPLICANT, contactRequest);
        assertThat(created.name()).isEqualTo("Omar");
        verify(mapper).insertContact(any(ApplicantContact.class));
    }

    @Test
    void shouldNotFindContact_whenItBelongsToAnotherApplicant() {
        allow("EDIT_PROFILE");
        when(mapper.findContactById(9L)).thenReturn(contactOf(OTHER_APPLICANT));
        assertThatThrownBy(() -> service.updateContact(APPLICANT, 9L, contactRequest)).isInstanceOf(NadNotFoundException.class);
        verify(mapper, never()).updateContact(any());
    }

    @Test
    void shouldReportNotFound_whenDeletingAContactThatIsNotTheApplicants() {
        allow("EDIT_PROFILE");
        when(mapper.deleteContact(9L, APPLICANT)).thenReturn(0);
        assertThatThrownBy(() -> service.deleteContact(APPLICANT, 9L)).isInstanceOf(NadNotFoundException.class);
    }
}
