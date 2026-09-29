package com.nadoumi.application.service;

import com.nadoumi.application.domain.WfDefinition;
import com.nadoumi.application.domain.WfStage;
import com.nadoumi.application.domain.WfTransition;
import com.nadoumi.application.domain.enums.WfDefinitionStatus;
import com.nadoumi.application.domain.enums.WfStageType;
import com.nadoumi.application.mapper.WfDefinitionMapper;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowDefinitionService {

    private static final String DECISION_GUARD = "DECISION_RECORDED";
    private static final int MIN_DECISION_BRANCHES = 2;

    private final WfDefinitionMapper definitionMapper;
    private final GuardEvaluator guards;

    public WorkflowDefinitionService(WfDefinitionMapper definitionMapper, GuardEvaluator guards) {
        this.definitionMapper = definitionMapper;
        this.guards = guards;
    }

    /** The six §II.4 checks. Flips DRAFT to ACTIVE and retires any prior ACTIVE version of the same code. */
    @Transactional(rollbackFor = Exception.class)
    public List<String> activate(long definitionId) {
        WfDefinition definition = definitionMapper.findById(definitionId);
        if (definition == null) {
            throw new NadNotFoundException("workflow definition not found");
        }
        List<WfStage> stages = definitionMapper.findStages(definitionId);
        List<WfTransition> transitions = definitionMapper.findTransitions(definitionId);

        List<String> problems = validate(stages, transitions);
        if (!problems.isEmpty()) {
            throw new NadBadRequestException("UNBACKED_GUARD_PREDICATE or structural validity failure: " + problems);
        }

        definitionMapper.findAllVersions(definition.getCode()).stream()
                .filter(d -> d.getStatus() == WfDefinitionStatus.ACTIVE)
                .forEach(d -> definitionMapper.updateStatus(d.getId(), WfDefinitionStatus.RETIRED.name()));
        definitionMapper.updateStatus(definitionId, WfDefinitionStatus.ACTIVE.name());
        return problems;
    }

    private List<String> validate(List<WfStage> stages, List<WfTransition> transitions) {
        List<String> problems = new ArrayList<>();
        List<WfStage> starts = stages.stream().filter(s -> s.getStageType() == WfStageType.START).toList();
        checkEntryAndExit(stages, starts, problems);
        checkOutgoing(stages, transitions, problems);
        checkReachability(stages, starts, transitions, problems);
        checkDecisionBranches(stages, transitions, problems);
        checkGuardPredicates(transitions, problems);
        return problems;
    }

    private void checkEntryAndExit(List<WfStage> stages, List<WfStage> starts, List<String> problems) {
        if (starts.size() != 1) {
            problems.add("exactly one START stage is required, found " + starts.size());
        }
        long terminalCount = stages.stream().filter(s -> s.getStageType() == WfStageType.TERMINAL).count();
        if (terminalCount < 1) {
            problems.add("at least one TERMINAL stage is required");
        }
    }

    private void checkOutgoing(List<WfStage> stages, List<WfTransition> transitions, List<String> problems) {
        Set<Long> withOutgoing = transitions.stream().map(WfTransition::getFromStageId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        for (WfStage stage : stages) {
            if (stage.getStageType() != WfStageType.TERMINAL && !withOutgoing.contains(stage.getId())) {
                problems.add("stage '" + stage.getCode() + "' is non-terminal with no outgoing transition");
            }
        }
    }

    private void checkReachability(List<WfStage> stages, List<WfStage> starts, List<WfTransition> transitions,
            List<String> problems) {
        if (starts.isEmpty()) {
            return;
        }
        Set<Long> reachable = reachableFrom(starts.get(0).getId(), transitions);
        for (WfStage stage : stages) {
            if (!reachable.contains(stage.getId())) {
                problems.add("stage '" + stage.getCode() + "' is unreachable from START");
            }
        }
    }

    private void checkDecisionBranches(List<WfStage> stages, List<WfTransition> transitions, List<String> problems) {
        for (WfStage stage : stages) {
            if (stage.getStageType() != WfStageType.DECISION) {
                continue;
            }
            long decisionGuarded = transitions.stream()
                    .filter(t -> stage.getId().equals(t.getFromStageId()))
                    .filter(t -> t.getGuardJson() != null && t.getGuardJson().contains(DECISION_GUARD))
                    .count();
            if (decisionGuarded < MIN_DECISION_BRANCHES) {
                problems.add("DECISION stage '" + stage.getCode() + "' needs >=2 outgoing DECISION_RECORDED-guarded transitions");
            }
        }
    }

    private void checkGuardPredicates(List<WfTransition> transitions, List<String> problems) {
        for (WfTransition transition : transitions) {
            for (String unbacked : guards.unbackedPredicates(transition.getGuardJson())) {
                problems.add("transition '" + transition.getCode() + "' references an unbacked predicate: " + unbacked);
            }
        }
    }

    private static Set<Long> reachableFrom(long startId, List<WfTransition> transitions) {
        Map<Long, List<Long>> edges = transitions.stream()
                .filter(t -> t.getFromStageId() != null)
                .collect(Collectors.groupingBy(WfTransition::getFromStageId,
                        Collectors.mapping(WfTransition::getToStageId, Collectors.toList())));
        Set<Long> visited = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(startId);
        visited.add(startId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            for (Long next : edges.getOrDefault(current, List.of())) {
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return visited;
    }
}
