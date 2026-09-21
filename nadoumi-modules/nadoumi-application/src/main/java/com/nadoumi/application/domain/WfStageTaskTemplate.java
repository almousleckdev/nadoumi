package com.nadoumi.application.domain;

/** Row of {@code nad_wf_stage_task_template} — materialised into a task on stage entry. */
public class WfStageTaskTemplate {

    private Long id;
    private Long stageId;
    private String title;
    private String roleRequired;
    private boolean mandatory;
    private boolean blocksExit;
    private int orderNo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStageId() { return stageId; }
    public void setStageId(Long stageId) { this.stageId = stageId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRoleRequired() { return roleRequired; }
    public void setRoleRequired(String roleRequired) { this.roleRequired = roleRequired; }

    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }

    public boolean isBlocksExit() { return blocksExit; }
    public void setBlocksExit(boolean blocksExit) { this.blocksExit = blocksExit; }

    public int getOrderNo() { return orderNo; }
    public void setOrderNo(int orderNo) { this.orderNo = orderNo; }
}
