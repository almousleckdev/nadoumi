package com.nadoumi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.common.access.ApplicationApplicantResolver;
import org.junit.jupiter.api.Test;

/**
 * The bean {@code NadoumiAccessServiceImpl} has been waiting for (its
 * {@code Optional<ApplicationApplicantResolver>} is empty until this module exists).
 * This is the one integration point the 2026-09-20 addendum called out explicitly.
 */
class ApplicationApplicantResolverImplTest {

    @Test
    void resolves_the_owning_applicant_for_an_existing_application() {
        ApplicationMapper mapper = mock(ApplicationMapper.class);
        when(mapper.findApplicantId(123L)).thenReturn(42L);
        ApplicationApplicantResolver resolver = new ApplicationApplicantResolverImpl(mapper);

        assertThat(resolver.applicantIdOf(123L)).isEqualTo(42L);
    }

    @Test
    void returns_null_for_an_application_that_does_not_exist_denying_access_by_default() {
        ApplicationMapper mapper = mock(ApplicationMapper.class);
        when(mapper.findApplicantId(999L)).thenReturn(null);
        ApplicationApplicantResolver resolver = new ApplicationApplicantResolverImpl(mapper);

        assertThat(resolver.applicantIdOf(999L)).isNull();
    }

    @Test
    void returns_null_for_a_null_applicationId_rather_than_querying_the_mapper() {
        ApplicationMapper mapper = mock(ApplicationMapper.class);
        ApplicationApplicantResolver resolver = new ApplicationApplicantResolverImpl(mapper);

        assertThat(resolver.applicantIdOf(null)).isNull();
        org.mockito.Mockito.verifyNoInteractions(mapper);
    }
}
