package com.nadoumi.application.domain;

import com.nadoumi.application.domain.enums.WfStageType;

/** Row of {@code nad_wf_stage} — one stage of a {@link WfDefinition}. */
public class WfStage {

    private Long id;
    private Long definitionId;
    private String code;
    private String name;
    private int orderNo;
    private WfStageType stageType;
    private String statusLabel;
    private Integer slaHours;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long definitionId) { this.definitionId = definitionId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getOrderNo() { return orderNo; }
    public void setOrderNo(int orderNo) { this.orderNo = orderNo; }

    public WfStageType getStageType() { return stageType; }
    public void setStageType(WfStageType stageType) { this.stageType = stageType; }

    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }

    public Integer getSlaHours() { return slaHours; }
    public void setSlaHours(Integer slaHours) { this.slaHours = slaHours; }
}
