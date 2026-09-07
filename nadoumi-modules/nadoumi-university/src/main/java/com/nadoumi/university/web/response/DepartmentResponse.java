package com.nadoumi.university.web.response;

import com.nadoumi.university.domain.Department;

/** Staff view of a university's academic department. */
public record DepartmentResponse(
        Long id,
        Long universityId,
        String name,
        String nameCn,
        int sortOrder,
        int programCount) {

    public static DepartmentResponse of(Department d) {
        return new DepartmentResponse(
                d.getId(), d.getUniversityId(), d.getName(), d.getNameCn(),
                d.getSortOrder(), d.getProgramCount());
    }
}
