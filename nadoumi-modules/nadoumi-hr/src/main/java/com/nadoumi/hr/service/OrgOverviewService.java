package com.nadoumi.hr.service;

import com.nadoumi.hr.domain.OrgCount;
import com.nadoumi.hr.mapper.OrgOverviewMapper;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Head counts for roles, departments and posts, keyed by id; an id with nobody behind it is simply absent. */
@Service
public class OrgOverviewService {

    /** Members per id for each kind of structure; the screens read {@code counts.get(id)} and treat a miss as zero. */
    public record Overview(Map<Long, Long> roles, Map<Long, Long> departments, Map<Long, Long> posts) {
    }

    private final OrgOverviewMapper mapper;

    public OrgOverviewService(OrgOverviewMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Overview overview() {
        return new Overview(byId(mapper.roleMembers()), byId(mapper.departmentMembers()), byId(mapper.postMembers()));
    }

    private static Map<Long, Long> byId(List<OrgCount> rows) {
        return rows.stream().collect(Collectors.toMap(OrgCount::getId, OrgCount::getMembers, Long::sum, java.util.LinkedHashMap::new));
    }
}
