package com.nadoumi.support.mapper;

import com.nadoumi.support.domain.SupportMeeting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SupportMeetingMapper {
    int insert(SupportMeeting meeting);

    SupportMeeting findById(long id);

    List<SupportMeeting> findByTicketId(long ticketId);

    int countOverlappingStaffMeetings(@Param("staffId") long staffId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    int countOverlappingStudentMeetings(@Param("studentId") long studentId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);
    
    int updateStatus(@Param("id") long id, @Param("status") String status, @Param("updateBy") String updateBy);
}
