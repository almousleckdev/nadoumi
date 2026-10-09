package com.nadoumi.communication.mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserPresenceMapper {

    int upsert(@Param("userId") long userId, @Param("lastSeenAt") LocalDateTime lastSeenAt);

    List<LastSeenRow> listLastSeen(@Param("userIds") Collection<Long> userIds);
}
