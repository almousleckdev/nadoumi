package com.nadoumi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.application.domain.Application;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.document.domain.DocumentTemplate;
import com.nadoumi.document.domain.enums.RequirementScope;
import com.nadoumi.document.service.DocumentTemplateService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicationTemplateServiceTest {

    private static final long APP = 42L;

    private final ApplicationMapper applications = mock(ApplicationMapper.class);
    private final DocumentTemplateService templates = mock(DocumentTemplateService.class);
    private final ApplicationTemplateService service = new ApplicationTemplateService(applications, templates);

    private static DocumentTemplate template(long id) {
        DocumentTemplate t = new DocumentTemplate();
        t.setId(id);
        return t;
    }

    private void application(Long programId, Long scholarshipId) {
        Application a = new Application();
        a.setId(APP);
        a.setProgramId(programId);
        a.setScholarshipId(scholarshipId);
        when(applications.findById(APP)).thenReturn(a);
    }

    @Test
    void shouldListTheTemplatesOfTheApplicationsOwnProgrammeAndScholarship() {
        application(5L, 8L);
        when(templates.list(RequirementScope.PROGRAM, 5L)).thenReturn(List.of(template(1)));
        when(templates.list(RequirementScope.SCHOLARSHIP, 8L)).thenReturn(List.of(template(2)));

        assertThat(service.forApplication(APP)).extracting(DocumentTemplate::getId).containsExactly(1L, 2L);
    }

    @Test
    void shouldUseOnlyTheScholarship_forAScholarshipOnlyApplication() {
        application(null, 8L);
        when(templates.list(RequirementScope.SCHOLARSHIP, 8L)).thenReturn(List.of(template(2)));

        assertThat(service.forApplication(APP)).extracting(DocumentTemplate::getId).containsExactly(2L);
    }

    @Test
    void shouldRefuseATemplateThatBelongsToAnotherOpportunity() {
        application(5L, null);
        when(templates.list(RequirementScope.PROGRAM, 5L)).thenReturn(List.of(template(1)));

        assertThat(service.find(APP, 1L).getId()).isEqualTo(1L);
        assertThatThrownBy(() -> service.find(APP, 999L)).isInstanceOf(NadNotFoundException.class);
    }
}
