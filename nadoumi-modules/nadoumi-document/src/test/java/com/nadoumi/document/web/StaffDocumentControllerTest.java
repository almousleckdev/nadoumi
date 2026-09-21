package com.nadoumi.document.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.document.domain.Document;
import com.nadoumi.document.domain.enums.DocumentStatus;
import com.nadoumi.document.service.DocumentService;
import com.nadoumi.document.web.request.RejectDocumentRequest;
import com.nadoumi.document.web.response.StaffDocumentResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class StaffDocumentControllerTest {

    private final DocumentService service = mock(DocumentService.class);
    private final StaffDocumentController controller = new StaffDocumentController(service);

    private static Document doc() {
        Document d = new Document();
        d.setId(1L);
        d.setApplicantId(42L);
        d.setReviewerUserId(7L);
        d.setDocType("TRANSCRIPT");
        d.setStatus(DocumentStatus.SUBMITTED);
        return d;
    }

    @Test
    void get_includesReviewerAndVersionHistory_unlikeTheStudentView() {
        when(service.get(1L)).thenReturn(doc());
        when(service.versionsOf(1L)).thenReturn(List.of());
        when(service.eventsOf(1L)).thenReturn(List.of());

        StaffDocumentResponse response = controller.get(1L);

        assertThat(response.reviewerUserId()).isEqualTo(7L);
        assertThat(response.versions()).isNotNull();
        assertThat(response.events()).isNotNull();
    }

    @Test
    void verify_delegatesToTheService() {
        controller.verify(1L);
        verify(service).verify(1L);
    }

    @Test
    void reject_delegatesTheReason() {
        controller.reject(1L, new RejectDocumentRequest("blurry scan"));
        verify(service).reject(1L, "blurry scan");
    }
}
