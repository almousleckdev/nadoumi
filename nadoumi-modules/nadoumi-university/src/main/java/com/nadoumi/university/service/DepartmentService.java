package com.nadoumi.university.service;

import com.nadoumi.identity.exception.NadBadRequestException;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.nadoumi.university.domain.Department;
import com.nadoumi.university.mapper.DepartmentMapper;
import com.nadoumi.university.mapper.UniversityMapper;
import com.nadoumi.university.web.request.DepartmentRequest;
import com.nadoumi.university.web.response.DepartmentResponse;
import com.ruoyi.common.utils.SecurityUtils;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Academic departments of a university. Names are unique per university. A
 * department cannot be deleted while a programme major still points at it.
 */
@Service
public class DepartmentService {

    private final DepartmentMapper mapper;
    private final UniversityMapper universityMapper;

    public DepartmentService(DepartmentMapper mapper, UniversityMapper universityMapper) {
        this.mapper = mapper;
        this.universityMapper = universityMapper;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> list(long universityId) {
        requireUniversity(universityId);
        return mapper.findByUniversity(universityId).stream().map(DepartmentResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse get(long universityId, long id) {
        return DepartmentResponse.of(load(universityId, id));
    }

    /** Used by {@code ProgramService} to validate a major's department on write. */
    @Transactional(readOnly = true)
    public boolean belongsToUniversity(long departmentId, long universityId) {
        Department d = mapper.findById(departmentId);
        return d != null && d.getUniversityId() == universityId;
    }

    @Transactional
    public DepartmentResponse create(long universityId, DepartmentRequest req) {
        requireUniversity(universityId);
        requireUniqueName(universityId, req.name().trim(), null);
        Department d = new Department();
        d.setUniversityId(universityId);
        apply(d, req);
        d.setCreateBy(currentUser());
        mapper.insert(d);
        return get(universityId, d.getId());
    }

    @Transactional
    public DepartmentResponse update(long universityId, long id, DepartmentRequest req) {
        Department d = load(universityId, id);
        requireUniqueName(universityId, req.name().trim(), id);
        apply(d, req);
        d.setUpdateBy(currentUser());
        mapper.update(d);
        return get(universityId, id);
    }

    @Transactional
    public void delete(long universityId, long id) {
        load(universityId, id);
        if (mapper.countMajorsUsing(id) > 0) {
            throw new NadBadRequestException("this department still has programme majors — move them first");
        }
        mapper.deleteById(id);
    }

    // ---- internals ----

    private void apply(Department d, DepartmentRequest req) {
        d.setName(req.name().trim());
        d.setNameCn(blankToNull(req.nameCn()));
        d.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    }

    private Department load(long universityId, long id) {
        Department d = mapper.findById(id);
        if (d == null || d.getUniversityId() != universityId) {
            throw new NadNotFoundException("department not found");
        }
        return d;
    }

    private void requireUniversity(long universityId) {
        if (universityMapper.findById(universityId) == null) {
            throw new NadNotFoundException("university not found");
        }
    }

    private void requireUniqueName(long universityId, String name, Long selfId) {
        Long existing = mapper.findIdByUniversityAndName(universityId, name);
        if (existing != null && !existing.equals(selfId)) {
            throw new NadBadRequestException("a department with this name already exists for that university");
        }
    }

    private static String currentUser() {
        try {
            return SecurityUtils.getUsername();
        }
        catch (RuntimeException e) {
            return "system";
        }
    }

    private static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
