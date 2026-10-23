package com.nadoumi.program.web.response;

import com.nadoumi.program.domain.Major;

public record MajorResponse(Long id, String name, String nameCn, String discipline, int programCount) {

    public static MajorResponse of(Major m) {
        return new MajorResponse(m.getId(), m.getName(), m.getNameCn(), m.getDiscipline(), m.getProgramCount());
    }
}
