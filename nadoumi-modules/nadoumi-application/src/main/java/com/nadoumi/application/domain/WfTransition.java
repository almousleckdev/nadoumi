package com.nadoumi.application.domain;

/**
 * Row of {@code nad_wf_transition}. {@code code} is a transition "family" name —
 * the same code may appear on several rows (one per eligible {@code fromStageId}),
 * e.g. {@code withdraw} from nine different stages. The engine resolves a
 * transition by {@code (definitionId, code, fromStageId)}.
 */
public class WfTransition {

    private Long id;
    private Long definitionId;
    private String code;
    private Long fromStageId;
    private Long toStageId;
    private String guardJson;
    private String roleRequired;
    private boolean auto;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long definitionId) { this.definitionId = definitionId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Long getFromStageId() { return fromStageId; }
    public void setFromStageId(Long fromStageId) { this.fromStageId = fromStageId; }

    public Long getToStageId() { return toStageId; }
    public void setToStageId(Long toStageId) { this.toStageId = toStageId; }

    public String getGuardJson() { return guardJson; }
    public void setGuardJson(String guardJson) { this.guardJson = guardJson; }

    public String getRoleRequired() { return roleRequired; }
    public void setRoleRequired(String roleRequired) { this.roleRequired = roleRequired; }

    public boolean isAuto() { return auto; }
    public void setAuto(boolean auto) { this.auto = auto; }
}
