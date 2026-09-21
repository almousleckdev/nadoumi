package com.nadoumi.document.service;

import com.nadoumi.common.media.MediaAccessClass;
import java.util.Set;

/**
 * The five doc types whose category default (PROTECTED) is overridden to SENSITIVE
 * (DOCUMENT_MANAGEMENT.md §3.2a) — their bytes are never handed to the browser as a
 * URL, only backend-proxied.
 */
final class DocumentTypeAccess {

    private static final Set<String> SENSITIVE_OVERRIDE = Set.of(
            "PASSPORT", "VISA", "FINANCIAL_PROOF", "TRANSCRIPT", "POLICE_CLEARANCE");

    private DocumentTypeAccess() {
    }

    /** {@link MediaAccessClass#SENSITIVE} for the five overridden types, else {@code null} (category default). */
    static MediaAccessClass overrideFor(String docType) {
        return SENSITIVE_OVERRIDE.contains(docType) ? MediaAccessClass.SENSITIVE : null;
    }
}
