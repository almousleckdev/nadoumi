package com.nadoumi.common.workflow;

/**
 * The fixed set of workflow transition guard predicates (D4).
 *
 * <p>Transition {@code guard_json} is only {@code {"all":[<predicate>, ...]}} where
 * each entry names one of these. There is deliberately <b>no</b> expression
 * language / SpEL in v1 (removes an eval attack surface and keeps guards
 * reviewable). OR is modelled as two transitions. See
 * {@code docs/APPLICATION_WORKFLOW.md} §3.1 and
 * {@code docs/WORKFLOW_PROGRAM_WITH_SCHOLARSHIP.md} §2.</p>
 *
 * <p>Some predicates take a single argument encoded in the guard entry, e.g.
 * {@code "DECISION_RECORDED:UNIVERSITY_OFFER"} or {@code "FIELD_SET:program_id"};
 * the argument grammar is defined by the WorkflowService in the workflow slice.</p>
 *
 * <p>Phase 2: enum only.</p>
 */
public enum GuardPredicate {
    /** Every current-stage task with {@code mandatory = 1} is DONE or SKIPPED. */
    ALL_MANDATORY_TASKS_DONE,

    /** Every current-stage task with {@code blocks_exit = 1} is DONE or SKIPPED (always enforced by the engine). */
    TASKS_BLOCKING_EXIT_DONE,

    /** Every mandatory {@code nad_document_requirement} in scope has a linked document. */
    ALL_REQUIRED_DOCUMENTS_ATTACHED,

    /** …and each linked document's current version is VERIFIED and not expired. */
    ALL_REQUIRED_DOCUMENTS_VERIFIED,

    /** A {@code nad_application_decision} exists for the given {@code decision_type} (and optional outcome set). */
    DECISION_RECORDED,

    /** A {@code nad_payment} of the given kind for this application is SETTLED. */
    PAYMENT_SETTLED,

    /** The named {@code nad_application} column is non-null. */
    FIELD_SET
}
