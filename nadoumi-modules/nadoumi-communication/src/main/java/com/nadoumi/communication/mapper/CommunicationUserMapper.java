package com.nadoumi.communication.mapper;

import com.nadoumi.communication.web.response.AdminContactResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** The one {@code sys_user} read this module needs: a display name for a sender. */
public interface CommunicationUserMapper {

    /** {@code nick_name}, falling back to {@code user_name}; null when the user doesn't exist. */
    String findDisplayName(@Param("userId") long userId);

    /** True for an enabled, non-deleted staff account (the only kind that may be on the staff side of a chat). */
    boolean isActiveStaff(@Param("userId") long userId);

    /** True for an enabled, non-deleted student account (the only kind that may be on the student side of a chat). */
    boolean isActiveStudent(@Param("userId") long userId);

    /** Chat-eligible staff for the student-facing directory, by display name. */
    List<Long> listChatStaffIds(@Param("limit") int limit);

    /** Active students matching an id, an application id ({@code numericId}) or a name fragment ({@code text}). */
    List<StudentHit> searchStudents(@Param("numericId") Long numericId, @Param("text") String text,
            @Param("limit") int limit);

    List<AdminContactResponse> listStaffAdmins();
}
