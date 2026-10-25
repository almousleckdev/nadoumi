package com.nadoumi.hr.mapper;

import com.nadoumi.hr.domain.OrgCount;
import java.util.List;

/** Read-only head counts for the people screens (roles, departments, posts). */
public interface OrgOverviewMapper {

    /** Active staff accounts (not students) holding each role. */
    List<OrgCount> roleMembers();

    /** Employees in each department who are still employed. */
    List<OrgCount> departmentMembers();

    /** Employees holding each post who are still employed. */
    List<OrgCount> postMembers();
}
