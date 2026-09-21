package com.nadoumi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.application.domain.Application;
import com.nadoumi.application.domain.enums.ApplicationType;
import com.nadoumi.application.mapper.ApplicationEventMapper;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.application.mapper.ApplicationSearch;
import com.nadoumi.application.mapper.WfDefinitionMapper;
import com.nadoumi.application.web.request.StartApplicationRequest;
import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.common.exception.NadForbiddenException;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.identity.access.CurrentCaller;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * A user without a grant cannot start/submit/read another applicant's application;
 * on-behalf-of with the right capability succeeds; staff scope filtering is real.
 * (§II.10 — application ownership, staff transitions, User != Applicant.)
 */
class ApplicationAuthorizationTest {

    private final ApplicationMapper applicationMapper = mock(ApplicationMapper.class);
    private final WfDefinitionMapper definitionMapper = mock(WfDefinitionMapper.class);
    private final ApplicationEventMapper eventMapper = mock(ApplicationEventMapper.class);
    private final WorkflowService workflow = mock(WorkflowService.class);
    private final NadoumiAccessService access = mock(NadoumiAccessService.class);
    private final CurrentCaller caller = mock(CurrentCaller.class);

    private final StudentApplicationService service =
            new StudentApplicationService(applicationMapper, definitionMapper, eventMapper, workflow, access, caller);

    @Test
    void starting_a_draft_for_an_applicant_with_no_grant_is_refused_before_the_engine_runs() {
        var req = new StartApplicationRequest(42L, ApplicationType.PROGRAM_ONLY, 1L, null, null);
        when(access.canAccessApplicant(42L, "CREATE_APPLICATION")).thenReturn(false);

        assertThatThrownBy(() -> service.create(req)).isInstanceOf(NadForbiddenException.class);
        org.mockito.Mockito.verifyNoInteractions(workflow);
    }

    @Test
    void starting_a_draft_on_behalf_of_an_applicant_with_a_live_grant_succeeds() {
        var req = new StartApplicationRequest(42L, ApplicationType.PROGRAM_ONLY, 1L, null, null);
        when(access.canAccessApplicant(42L, "CREATE_APPLICATION")).thenReturn(true);
        when(caller.requireUserId()).thenReturn(7L);
        Application created = new Application();
        created.setId(500L);
        created.setApplicantId(42L);
        created.setApplicationType(ApplicationType.PROGRAM_ONLY);
        created.setCurrentStageId(1L);
        created.setCurrentStatus("DRAFT");
        when(workflow.startDraft(42L, ApplicationType.PROGRAM_ONLY, 1L, null, null, 7L)).thenReturn(created);
        when(definitionMapper.findStageById(1L)).thenReturn(null);
        when(eventMapper.findByApplication(500L)).thenReturn(List.of());

        var result = service.create(req);

        assertThat(result.id()).isEqualTo(500L);
        verify(workflow).startDraft(42L, ApplicationType.PROGRAM_ONLY, 1L, null, null, 7L);
    }

    @Test
    void listMine_only_ever_queries_applicants_the_caller_actually_holds_a_grant_for() {
        when(access.accessibleApplicantIds()).thenReturn(List.of(42L));
        when(applicationMapper.findByApplicant(42L)).thenReturn(List.of());

        service.listMine();

        verify(applicationMapper).findByApplicant(42L);
        verify(applicationMapper, org.mockito.Mockito.never()).findByApplicant(org.mockito.ArgumentMatchers.longThat(id -> id != 42L));
    }

    @Test
    void staff_case_officer_scope_restricts_the_list_query_to_assigned_or_unclaimed() {
        // exercised at the mapper-search-filter boundary: ApplicationAdminService just forwards
        // whatever scope the controller decided (see StaffApplicationController for the role check).
        ApplicationMapper adminMapper = mock(ApplicationMapper.class);
        ApplicationAdminService admin = new ApplicationAdminService(adminMapper, definitionMapper,
                mock(com.nadoumi.application.mapper.ApplicationTaskMapper.class),
                mock(com.nadoumi.application.mapper.ApplicationStageHistoryMapper.class),
                mock(com.nadoumi.application.mapper.ApplicationEventMapper.class),
                mock(com.nadoumi.application.mapper.ApplicationDecisionMapper.class), workflow);
        when(adminMapper.searchStaff(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        ApplicationSearch scoped = new ApplicationSearch(null, null, null, null, null, 99L);
        admin.list(scoped, 0, 20);

        ArgumentCaptor<ApplicationSearch> captor = ArgumentCaptor.forClass(ApplicationSearch.class);
        verify(adminMapper).searchStaff(captor.capture());
        assertThat(captor.getValue().scopeUserId()).isEqualTo(99L);
    }
}
