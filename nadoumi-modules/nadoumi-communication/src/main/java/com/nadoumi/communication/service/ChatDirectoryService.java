package com.nadoumi.communication.service;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.communication.mapper.CommunicationUserMapper;
import com.nadoumi.communication.mapper.StudentHit;
import com.nadoumi.communication.web.response.ChatPerson;
import com.nadoumi.communication.web.response.StudentSearchResult;
import com.nadoumi.identity.access.CurrentCaller;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who a caller may start a chat with. Students see the staff who can chat; staff search students on the server, a
 * handful at a time, by student id, application id or name. Both return a first name and a photo and nothing else.
 */
@Service
public class ChatDirectoryService {

    static final int MAX_STAFF = 100;
    static final int MAX_RESULTS = 20;
    static final int MIN_TEXT_LENGTH = 2;
    private static final String STUDENT_REF_PREFIX = "STU-";
    private static final int MAX_QUERY_LENGTH = 60;

    private final CommunicationUserMapper users;
    private final ConversationResponseAssembler assembler;
    private final CurrentCaller caller;

    public ChatDirectoryService(CommunicationUserMapper users, ConversationResponseAssembler assembler,
            CurrentCaller caller) {
        this.users = users;
        this.assembler = assembler;
        this.caller = caller;
    }

    @Transactional(readOnly = true)
    public List<ChatPerson> staffDirectory() {
        if (caller.isStaff()) {
            throw new AccessDeniedException("students only");
        }
        List<Long> ids = users.listChatStaffIds(MAX_STAFF);
        Map<Long, ChatPerson> people = assembler.people(ids);
        return ids.stream().map(people::get).toList();
    }

    @Transactional(readOnly = true)
    public List<StudentSearchResult> searchStudents(String rawQuery) {
        if (!caller.isStaff()) {
            throw new AccessDeniedException("staff only");
        }
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() > MAX_QUERY_LENGTH) {
            throw new NadBadRequestException("search text is too long");
        }
        Long numericId = parseReference(query);
        if (numericId == null && query.length() < MIN_TEXT_LENGTH) {
            return List.of();
        }
        List<StudentHit> hits = users.searchStudents(numericId, query, MAX_RESULTS);
        Map<Long, ChatPerson> people = assembler.people(hits.stream().map(StudentHit::userId).toList());
        return hits.stream().map(h -> {
            ChatPerson p = people.get(h.userId());
            return new StudentSearchResult(h.userId(), STUDENT_REF_PREFIX + h.userId(), p.name(), p.avatarUrl(),
                    p.online(), h.applicationId());
        }).toList();
    }

    /** "42", "STU-42", "APP-42" and "#42" all mean the number 42; whether it is a student or an application id is decided by what matches. */
    private static Long parseReference(String query) {
        String digits = query.replaceFirst("(?i)^(stu|app|student|application)?[\\s#:-]*", "");
        if (digits.isEmpty() || digits.length() > 18 || !digits.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return Long.parseLong(digits);
    }
}
