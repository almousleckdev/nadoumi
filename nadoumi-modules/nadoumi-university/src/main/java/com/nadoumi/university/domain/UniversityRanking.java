package com.nadoumi.university.domain;

/** Row of {@code nad_university_ranking}. */
public class UniversityRanking {

    private Long id;
    private Long universityId;
    private String source;
    private Integer rankPosition;
    private Integer rankYear;
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUniversityId() { return universityId; }
    public void setUniversityId(Long universityId) { this.universityId = universityId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Integer getRankPosition() { return rankPosition; }
    public void setRankPosition(Integer rankPosition) { this.rankPosition = rankPosition; }

    public Integer getRankYear() { return rankYear; }
    public void setRankYear(Integer rankYear) { this.rankYear = rankYear; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
