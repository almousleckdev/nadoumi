package com.nadoumi.document.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.document.domain.Document;
import com.nadoumi.document.domain.enums.DocumentStatus;
import com.nadoumi.document.service.DocumentService;
import com.nadoumi.document.web.response.StudentDocumentResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class StudentDocumentControllerTest {

    private final DocumentService service = mock(DocumentService.class);
    private final StudentDocumentController controller = new StudentDocumentController(service);

    private static Document doc() {
        Document d = new Document();
        d.setId(1L);
        d.setApplicantId(42L);
        d.setDocType("TRANSCRIPT");
        d.setStatus(DocumentStatus.DRAFT);
        return d;
    }

    @Test
    void mine_delegatesTheApplicantAndApplicationFilter() {
        when(service.listMine(42L, null)).thenReturn(List.of(doc()));

        List<StudentDocumentResponse> result = controller.mine(42L, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).docType()).isEqualTo("TRANSCRIPT");
    }

    @Test
    void create_delegatesToCreateAndUploadFirstVersion() {
        MockMultipartFile file = new MockMultipartFile("file", "t.pdf", "application/pdf", new byte[] { 1 });
        when(service.createAndUploadFirstVersion(42L, null, "TRANSCRIPT", file)).thenReturn(doc());

        controller.create(42L, "TRANSCRIPT", null, file);

        verify(service).createAndUploadFirstVersion(42L, null, "TRANSCRIPT", file);
    }

    @Test
    void delete_delegatesToDeleteDraft() {
        controller.delete(1L);
        verify(service).deleteDraft(1L);
    }

    /** DOCUMENT_MANAGEMENT.md §3.1: a student never sees reviewer identity or internal event detail. */
    @Test
    void studentResponse_neverExposesReviewerOrInternalFields() {
        List<String> fields = java.util.Arrays.stream(StudentDocumentResponse.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName).toList();

        assertThat(fields).doesNotContain("reviewerUserId", "events");
    }
}
