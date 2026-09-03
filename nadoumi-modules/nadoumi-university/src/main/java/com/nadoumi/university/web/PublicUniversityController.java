package com.nadoumi.university.web;

import com.nadoumi.common.web.PageResponse;
import com.nadoumi.university.domain.enums.UniversityType;
import com.nadoumi.university.service.UniversityService;
import com.nadoumi.university.web.response.PublicUniversityResponse;
import com.ruoyi.common.annotation.Anonymous;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public university catalog — anonymous. Only {@code status = ACTIVE} +
 * {@code publish_status = PUBLISHED} rows are visible, and only through
 * {@link PublicUniversityResponse} (no operational status, audit or notes).
 */
@RestController
@RequestMapping("/api/public/universities")
public class PublicUniversityController {

    private final UniversityService service;

    public PublicUniversityController(UniversityService service) {
        this.service = service;
    }

    @Anonymous
    @GetMapping
    public PageResponse<PublicUniversityResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) UniversityType type,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean recommended,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return service.publicList(q, country, province, city, type, featured, recommended, page, size);
    }

    @Anonymous
    @GetMapping("/{idOrSlug}")
    public PublicUniversityResponse get(@PathVariable String idOrSlug) {
        return service.publicGet(idOrSlug);
    }
}
