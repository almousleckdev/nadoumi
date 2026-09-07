package com.nadoumi.program.domain.enums;

/**
 * {@code nad_program.program_type} -- the KIND of a programme. A {@link #DEGREE}
 * programme spans one or more levels (see {@code nad_program_level}) and carries
 * majors grouped under departments; {@link #LANGUAGE} / {@link #NON_DEGREE}
 * carry a {@code term_length} instead.
 */
public enum ProgramType {
    DEGREE,
    LANGUAGE,
    NON_DEGREE;

    public boolean isDegree() {
        return this == DEGREE;
    }
}
