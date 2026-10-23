package com.nadoumi.application.mapper;

import java.time.LocalDate;
import org.apache.ibatis.annotations.Param;

public interface ApplicationReferenceMapper {

    /** Takes the day's next number. Must be followed by {@link #lastTaken()} on the same connection. */
    int take(@Param("day") LocalDate day);

    /** The number the last {@link #take} produced (MySQL {@code last_insert_id}). */
    int lastTaken();
}
