package com.nadoumi.university.domain;

import com.nadoumi.university.domain.enums.HighlightKind;

/** Row of {@code nad_university_highlight}. */
public class UniversityHighlight {

    private Long id;
    private Long universityId;
    private HighlightKind kind;
    private Integer sortOrder;
    private String text;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUniversityId() { return universityId; }
    public void setUniversityId(Long universityId) { this.universityId = universityId; }

    public HighlightKind getKind() { return kind; }
    public void setKind(HighlightKind kind) { this.kind = kind; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
