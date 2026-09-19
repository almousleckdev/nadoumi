package com.nadoumi.applicant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.applicant.domain.ApplicantInterest;
import com.nadoumi.applicant.domain.ApplicantResidence;
import com.nadoumi.applicant.domain.ApplicantWork;
import com.nadoumi.applicant.domain.enums.ChinaVisaType;
import com.nadoumi.applicant.domain.enums.EducationLevel;
import com.nadoumi.applicant.domain.enums.InterestKind;
import com.nadoumi.applicant.domain.enums.StudyLevel;
import com.nadoumi.applicant.mapper.InterestMapper;
import com.nadoumi.applicant.mapper.ResidenceMapper;
import com.nadoumi.applicant.mapper.WorkMapper;
import com.nadoumi.applicant.web.request.InterestRequest;
import com.nadoumi.applicant.web.request.ResidenceRequest;
import com.nadoumi.applicant.web.request.WorkRequest;
import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

/** Interests, residence and work: each guards access, applies its rules, and persists what the rules allow. */
class ProfileSectionServicesTest {

    private static final long ID = 5L;
    private static final LocalDate TODAY = LocalDate.now();

    private final ApplicantAccessGuard guard = mock(ApplicantAccessGuard.class);

    // ---- interests ----

    private final InterestMapper interestMapper = mock(InterestMapper.class);
    private final InterestService interests = new InterestService(interestMapper, guard);

    private static InterestRequest interestRequest(List<String> fields, List<String> cities) {
        return new InterestRequest(StudyLevel.MASTER, fields, cities, null, null, null, null, null);
    }

    @Test
    void shouldInsertTheFirstInterestAndReplaceTheChoicesWithoutDuplicates() {
        when(interestMapper.find(ID)).thenReturn(null);

        interests.save(ID, interestRequest(List.of("BUSINESS", " BUSINESS ", "LAW"), List.of("Beijing", "Beijing")));

        verify(interestMapper).insert(any(ApplicantInterest.class));
        var order = inOrder(interestMapper);
        order.verify(interestMapper).deleteChoices(ID);
        order.verify(interestMapper).insertChoices(ID, InterestKind.FIELD, List.of("BUSINESS", "LAW"));
        order.verify(interestMapper).insertChoices(ID, InterestKind.CITY, List.of("Beijing"));
    }

    @Test
    void shouldUpdateAnExistingInterest() {
        when(interestMapper.find(ID)).thenReturn(new ApplicantInterest());

        interests.save(ID, interestRequest(List.of("LAW"), List.of("Shanghai")));

        verify(interestMapper).update(any(ApplicantInterest.class));
        verify(interestMapper, never()).insert(any());
    }

    @Test
    void shouldReturnNothingWhenNoInterestHasBeenSaved() {
        when(interestMapper.find(ID)).thenReturn(null);

        assertThat(interests.get(ID)).isEmpty();
    }

    @Test
    void shouldNotSaveInterestsWithoutTheEditCapability() {
        doThrow(new AccessDeniedException("no")).when(guard).require(ID, ApplicantCapability.EDIT_PROFILE);

        assertThatThrownBy(() -> interests.save(ID, interestRequest(List.of("LAW"), List.of("Beijing"))))
                .isInstanceOf(AccessDeniedException.class);
        verify(interestMapper, never()).insert(any());
    }

    // ---- residence ----

    private final ResidenceMapper residenceMapper = mock(ResidenceMapper.class);
    private final ResidenceService residence = new ResidenceService(residenceMapper, guard);

    @Test
    void shouldKeepTheChinaDetailsOnlyForAStudentInChina() {
        ResidenceRequest inChina = new ResidenceRequest(true, "cn", " Beijing ", null, EducationLevel.BACHELOR,
                "Peking University", ChinaVisaType.X1, TODAY.plusMonths(8));
        ResidenceRequest abroad = new ResidenceRequest(false, "eg", "Cairo", null, EducationLevel.BACHELOR,
                "ignored", ChinaVisaType.X1, TODAY.plusMonths(8));

        residence.save(ID, inChina);
        residence.save(ID, abroad);

        ArgumentCaptor<ApplicantResidence> stored = ArgumentCaptor.forClass(ApplicantResidence.class);
        verify(residenceMapper, org.mockito.Mockito.times(2)).insert(stored.capture());
        assertThat(stored.getAllValues().get(0).getVisaType()).isEqualTo(ChinaVisaType.X1);
        assertThat(stored.getAllValues().get(0).getCity()).isEqualTo("Beijing");
        assertThat(stored.getAllValues().get(0).getCountry()).isEqualTo("CN");
        assertThat(stored.getAllValues().get(1).getVisaType()).isNull();
        assertThat(stored.getAllValues().get(1).getChinaSchool()).isNull();
    }

    @Test
    void shouldRejectAnExpiredVisaAndStoreNothing() {
        ResidenceRequest expired = new ResidenceRequest(true, "CN", "Beijing", null, EducationLevel.BACHELOR, null,
                ChinaVisaType.X1, TODAY.minusDays(1));

        assertThatThrownBy(() -> residence.save(ID, expired)).isInstanceOf(NadBadRequestException.class);
        verify(residenceMapper, never()).insert(any());
    }

    // ---- work ----

    private final WorkMapper workMapper = mock(WorkMapper.class);
    private final WorkService work = new WorkService(workMapper, guard);

    private static WorkRequest workRequest(String country, ChinaVisaType visa) {
        return new WorkRequest(" Acme ", "Teacher", null, country, null, LocalDate.of(2023, 1, 1), null, true, null,
                visa, TODAY.plusMonths(6));
    }

    @Test
    void shouldStoreWorkAndKeepTheVisaOnlyForWorkInChina() {
        work.add(ID, workRequest("cn", ChinaVisaType.Z));
        work.add(ID, workRequest("eg", ChinaVisaType.Z));

        ArgumentCaptor<ApplicantWork> stored = ArgumentCaptor.forClass(ApplicantWork.class);
        verify(workMapper, org.mockito.Mockito.times(2)).insert(stored.capture());
        assertThat(stored.getAllValues().get(0).getEmployer()).isEqualTo("Acme");
        assertThat(stored.getAllValues().get(0).getWorkVisaType()).isEqualTo(ChinaVisaType.Z);
        assertThat(stored.getAllValues().get(1).getWorkVisaType()).isNull();
        assertThat(stored.getAllValues().get(1).getWorkVisaExpiry()).isNull();
    }

    @Test
    void shouldRequireTheWorkVisaForWorkInChina() {
        assertThatThrownBy(() -> work.add(ID, workRequest("CN", null))).isInstanceOf(NadBadRequestException.class);
        verify(workMapper, never()).insert(any());
    }

    @Test
    void shouldNotTouchAnotherApplicantsWork() {
        ApplicantWork other = new ApplicantWork();
        other.setApplicantId(99L);
        when(workMapper.findById(9L)).thenReturn(other);
        when(workMapper.delete(9L, ID)).thenReturn(0);

        assertThatThrownBy(() -> work.update(ID, 9L, workRequest("EG", null))).isInstanceOf(NadNotFoundException.class);
        assertThatThrownBy(() -> work.delete(ID, 9L)).isInstanceOf(NadNotFoundException.class);
        verify(workMapper, never()).update(any());
    }
}
