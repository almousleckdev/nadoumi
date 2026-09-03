package com.nadoumi.program.web;

import com.nadoumi.common.web.PageResponse;
import com.nadoumi.program.domain.enums.ProgramTeachingLanguage;
import com.nadoumi.program.domain.enums.ProgramType;
import com.nadoumi.program.service.ProgramService;
import com.nadoumi.program.web.response.PublicProgramResponse;
import com.ruoyi.common.annotation.Anonymous;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public programme catalog -- anonymous. Only PUBLISHED + ACTIVE programmes of a
 * PUBLISHED + ACTIVE university are visible, and only through
 * {@link PublicProgramResponse} (no operational status, audit or notes). The
 * owning university name is resolved via {@code UniversityService}.
 */
@RestController
@RequestMapping("/api/public")
public class PublicProgramController {

    private final ProgramService service;

    public PublicProgramController(ProgramService service) {
        this.service = service;
    }

    @Anonymous
    @GetMapping("/programs")
    public PageResponse<PublicProgramResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long universityId,
            @RequestParam(required = false) ProgramType type,
            @RequestParam(required = false) ProgramTeachingLanguage language,
            @RequestParam(required = false) String field,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean hot,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return service.publicList(q, universityId, type, language, field, featured, hot, page, size);
    }

    @Anonymous
    @GetMapping("/programs/{idOrSlug}")
    public PublicProgramResponse get(@PathVariable String idOrSlug) {
        return service.publicGet(idOrSlug);
    }

    @Anonymous
    @GetMapping("/universities/{idOrSlug}/programs")
    public List<PublicProgramResponse> forUniversity(@PathVariable String idOrSlug) {
        return service.publicListForUniversity(idOrSlug);
    }
}
