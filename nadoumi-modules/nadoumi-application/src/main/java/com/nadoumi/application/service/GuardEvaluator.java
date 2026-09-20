package com.nadoumi.application.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nadoumi.application.domain.Application;
import com.nadoumi.application.domain.ApplicationDecision;
import com.nadoumi.application.domain.ApplicationTask;
import com.nadoumi.application.domain.enums.ApplicationTaskStatus;
import com.nadoumi.application.mapper.ApplicationDecisionMapper;
import com.nadoumi.application.mapper.ApplicationTaskMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Evaluates {@code guard_json} — the fixed predicate set from D4 / APPLICATION_WORKFLOW.md
 * §2. {@code {"all":["PRED", "PRED:ARG", "PRED:ARG:OUTCOME"]}}, AND only; OR is
 * modelled as two transitions. No expression language, no SpEL.
 */
@Component
public class GuardEvaluator {

    /** {@code nad_application} columns a {@code FIELD_SET(name)} predicate may test. */
    private static final Set<String> FIELD_WHITELIST =
            Set.of("program_id", "scholarship_id", "intake_id", "submitted_at");

    /** Predicate names that require a provider bean to evaluate. */
    private static final Set<String> DOCUMENT_PREDICATES =
            Set.of("ALL_REQUIRED_DOCUMENTS_ATTACHED", "ALL_REQUIRED_DOCUMENTS_VERIFIED");
    private static final String PAYMENT_PREDICATE = "PAYMENT_SETTLED";

    private final ApplicationTaskMapper taskMapper;
    private final ApplicationDecisionMapper decisionMapper;
    private final Optional<DocumentGuardProvider> documentGuardProvider;
    private final Optional<PaymentGuardProvider> paymentGuardProvider;

    public GuardEvaluator(ApplicationTaskMapper taskMapper, ApplicationDecisionMapper decisionMapper,
            Optional<DocumentGuardProvider> documentGuardProvider, Optional<PaymentGuardProvider> paymentGuardProvider) {
        this.taskMapper = taskMapper;
        this.decisionMapper = decisionMapper;
        this.documentGuardProvider = documentGuardProvider;
        this.paymentGuardProvider = paymentGuardProvider;
    }

    /** {@code true} when every predicate in {@code guardJson} holds (or the guard is empty). */
    public boolean evaluate(String guardJson, Application application) {
        for (String predicate : predicatesIn(guardJson)) {
            if (!evaluateOne(predicate, application)) {
                return false;
            }
        }
        return true;
    }

    /** Every current-stage task with {@code mandatory=1} is DONE or SKIPPED. */
    public boolean allMandatoryTasksDone(long applicationId) {
        return taskMapper.findByApplication(applicationId).stream()
                .filter(ApplicationTask::isMandatory)
                .allMatch(GuardEvaluator::isCleared);
    }

    /** Every current-stage task with {@code blocks_exit=1} is DONE or SKIPPED — always enforced, guard or not. */
    public boolean tasksBlockingExitDone(long applicationId) {
        return taskMapper.findByApplication(applicationId).stream()
                .filter(ApplicationTask::isBlocksExit)
                .allMatch(GuardEvaluator::isCleared);
    }

    /**
     * Predicate names referenced by {@code guardJson} that have no registered
     * provider bean — {@code activate()} rejects a definition whose seed data
     * references any of these (DA3, fail-closed).
     */
    public Set<String> unbackedPredicates(String guardJson) {
        return predicatesIn(guardJson).stream()
                .map(p -> p.split(":")[0])
                .filter(name -> (DOCUMENT_PREDICATES.contains(name) && documentGuardProvider.isEmpty())
                        || (name.equals(PAYMENT_PREDICATE) && paymentGuardProvider.isEmpty()))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static List<String> predicatesIn(String guardJson) {
        if (guardJson == null || guardJson.isBlank()) {
            return List.of();
        }
        JSONObject obj = JSON.parseObject(guardJson);
        JSONArray all = obj.getJSONArray("all");
        if (all == null) {
            return List.of();
        }
        return all.stream().map(String::valueOf).toList();
    }

    private boolean evaluateOne(String predicate, Application application) {
        String[] parts = predicate.split(":");
        String name = parts[0];
        return switch (name) {
            case "ALL_MANDATORY_TASKS_DONE" -> allMandatoryTasksDone(application.getId());
            case "TASKS_BLOCKING_EXIT_DONE" -> tasksBlockingExitDone(application.getId());
            case "ALL_REQUIRED_DOCUMENTS_ATTACHED" -> documentGuardProvider
                    .map(p -> p.allRequiredDocumentsAttached(application.getId()))
                    .orElseThrow(() -> unbacked(predicate));
            case "ALL_REQUIRED_DOCUMENTS_VERIFIED" -> documentGuardProvider
                    .map(p -> p.allRequiredDocumentsVerified(application.getId()))
                    .orElseThrow(() -> unbacked(predicate));
            case "PAYMENT_SETTLED" -> paymentGuardProvider
                    .map(p -> p.isSettled(application.getId(), parts.length > 1 ? parts[1] : null))
                    .orElseThrow(() -> unbacked(predicate));
            case "DECISION_RECORDED" -> decisionRecorded(application.getId(),
                    parts.length > 1 ? parts[1] : null, parts.length > 2 ? parts[2] : null);
            case "FIELD_SET" -> fieldSet(application, parts.length > 1 ? parts[1] : null);
            default -> throw new IllegalStateException("unknown guard predicate: " + name);
        };
    }

    private boolean decisionRecorded(long applicationId, String decisionType, String outcome) {
        List<ApplicationDecision> decisions = decisionMapper.findByApplicationAndType(applicationId, decisionType);
        if (outcome == null) {
            return !decisions.isEmpty();
        }
        return decisions.stream().anyMatch(d -> outcome.equals(d.getOutcome()));
    }

    private boolean fieldSet(Application application, String fieldName) {
        if (!FIELD_WHITELIST.contains(fieldName)) {
            throw new IllegalStateException("FIELD_SET references a non-whitelisted column: " + fieldName);
        }
        return switch (fieldName) {
            case "program_id" -> application.getProgramId() != null;
            case "scholarship_id" -> application.getScholarshipId() != null;
            case "intake_id" -> application.getIntakeId() != null;
            case "submitted_at" -> application.getSubmittedAt() != null;
            default -> false;
        };
    }

    private static boolean isCleared(ApplicationTask task) {
        return task.getStatus() == ApplicationTaskStatus.DONE || task.getStatus() == ApplicationTaskStatus.SKIPPED;
    }

    private static IllegalStateException unbacked(String predicate) {
        return new IllegalStateException("UNBACKED_GUARD_PREDICATE: " + predicate);
    }
}
