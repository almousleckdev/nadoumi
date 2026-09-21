package com.nadoumi.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.common.access.NadoumiAccessService;
import com.nadoumi.document.domain.Document;
import com.nadoumi.document.mapper.DocumentMapper;
import org.junit.jupiter.api.Test;

/** {@code ownsDocument} is the bean {@code @PreAuthorize} calls on every by-document-id student endpoint. */
class DocumentAccessGuardTest {

    private final NadoumiAccessService access = mock(NadoumiAccessService.class);
    private final DocumentMapper documents = mock(DocumentMapper.class);
    private final DocumentAccessGuard guard = new DocumentAccessGuard(access, documents);

    @Test
    void shouldResolveTheOwningApplicant_thenCheckTheCapabilityOnIt() {
        Document doc = new Document();
        doc.setApplicantId(42L);
        when(documents.findById(100L)).thenReturn(doc);
        when(access.canAccessApplicant(42L, "VIEW_DOCUMENT")).thenReturn(true);

        assertThat(guard.ownsDocument(100L, "VIEW_DOCUMENT")).isTrue();
    }

    @Test
    void shouldDeny_whenTheDocumentDoesNotExist() {
        when(documents.findById(999L)).thenReturn(null);

        assertThat(guard.ownsDocument(999L, "VIEW_DOCUMENT")).isFalse();
    }

    @Test
    void shouldDeny_whenTheCallerHasNoGrantOnTheOwningApplicant() {
        Document doc = new Document();
        doc.setApplicantId(42L);
        when(documents.findById(100L)).thenReturn(doc);
        when(access.canAccessApplicant(42L, "VIEW_DOCUMENT")).thenReturn(false);

        assertThat(guard.ownsDocument(100L, "VIEW_DOCUMENT")).isFalse();
    }
}
