package com.nadoumi.application.domain;

import com.nadoumi.application.domain.enums.WfDefinitionStatus;
import java.time.LocalDateTime;

/** Row of {@code nad_wf_definition} — one versioned workflow config (D4). */
public class WfDefinition {

    private Long id;
    private String code;
    private String name;
    private int version;
    private WfDefinitionStatus status;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public WfDefinitionStatus getStatus() { return status; }
    public void setStatus(WfDefinitionStatus status) { this.status = status; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
