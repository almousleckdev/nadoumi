package com.nadoumi.applicant.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.applicant.domain.Applicant;
import com.nadoumi.applicant.mapper.ApplicantMapper;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadBadRequestException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class OnboardingServiceTest {

    private static final long ID = 7L;

    private final ApplicantMapper mapper = mock(ApplicantMapper.class);
    private final NadoumiAccessService access = mock(NadoumiAccessService.class);
    private final OnboardingService service = new OnboardingService(mapper, access);

    private static Applicant filled() {
        Applicant a = new Applicant();
        a.setId(ID);
        a.setGivenName("AHMED");
        a.setFamilyName("HASSAN");
        a.setDob(LocalDate.now().minusYears(20));
        a.setNationality("EG");
        a.setGender("MALE");
        a.setCountryOfOrigin("EG");
        a.setCountryOfResidence("CN");
        a.setNativeLanguage("ar");
        a.setPhone("+8613800000000");
        a.setWhatsapp("+8613800000000");
        a.setEmailVerifiedAt(LocalDateTime.now());
        a.setPhotoMediaId(11L);
        a.setPassportMediaId(12L);
        a.setPassportNo("P1234567");
        a.setPassportGivenName("AHMED");
        a.setPassportFamilyName("HASSAN");
        a.setPassportDob(a.getDob());
        a.setPassportIssueDate(LocalDate.now().minusYears(2));
        a.setPassportExpiryDate(LocalDate.now().plusYears(8));
        return a;
    }

    @Test
    void shouldMarkOnboarded_whenEverySectionIsSatisfied() {
        when(access.canAccessApplicant(ID, "EDIT_PROFILE")).thenReturn(true);
        Applicant before = filled();
        Applicant after = filled();
        after.setOnboardedAt(LocalDateTime.now());
        when(mapper.findById(ID)).thenReturn(before, after);

        OnboardingStatus status = service.complete(ID);

        verify(mapper).markOnboarded(ID);
        assertThat(status.complete()).isTrue();
    }

    @Test
    void shouldRejectCompletion_whenASectionIsIncomplete() {
        when(access.canAccessApplicant(ID, "EDIT_PROFILE")).thenReturn(true);
        Applicant incomplete = filled();
        incomplete.setGender(null);
        when(mapper.findById(ID)).thenReturn(incomplete);

        assertThatThrownBy(() -> service.complete(ID))
                .isInstanceOf(NadBadRequestException.class)
                .hasMessageContaining("PROFILE");
        verify(mapper, never()).markOnboarded(ID);
    }

    @Test
    void shouldRefuseCompletion_whenCallerCannotEditThisApplicant() {
        when(access.canAccessApplicant(ID, "EDIT_PROFILE")).thenReturn(false);

        assertThatThrownBy(() -> service.complete(ID)).isInstanceOf(AccessDeniedException.class);
        verify(mapper, never()).markOnboarded(ID);
    }

    @Test
    void shouldBeIdempotent_whenAlreadyOnboarded() {
        when(access.canAccessApplicant(ID, "EDIT_PROFILE")).thenReturn(true);
        Applicant done = filled();
        done.setOnboardedAt(LocalDateTime.now().minusDays(1));
        when(mapper.findById(ID)).thenReturn(done);

        assertThat(service.complete(ID).complete()).isTrue();
        verify(mapper, never()).markOnboarded(ID);
    }

    @Test
    void shouldReportStatus_withoutChangingAnything() {
        when(access.canAccessApplicant(ID, "VIEW_PROFILE")).thenReturn(true);
        Applicant incomplete = filled();
        incomplete.setPhone(null);
        when(mapper.findById(ID)).thenReturn(incomplete);

        OnboardingStatus status = service.status(ID);

        assertThat(status.complete()).isFalse();
        assertThat(status.ready()).isFalse();
        assertThat(status.sections().get(0).missing()).containsExactly("phone");
    }
}
