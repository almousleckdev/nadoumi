package com.nadoumi.hr.domain;

/** How many people sit behind one role, department or post. */
public class OrgCount {
    private long id;
    private long members;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getMembers() { return members; }
    public void setMembers(long members) { this.members = members; }
}
