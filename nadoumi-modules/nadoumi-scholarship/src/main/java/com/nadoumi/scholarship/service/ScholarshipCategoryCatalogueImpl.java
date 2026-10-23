package com.nadoumi.scholarship.service;

import com.nadoumi.common.scholarship.ScholarshipCategoryCatalogue;
import com.nadoumi.scholarship.domain.ScholarshipCategory;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Exposes the scholarship categories to other modules through the shared interface. */
@Component
public class ScholarshipCategoryCatalogueImpl implements ScholarshipCategoryCatalogue {

    private final ScholarshipService scholarships;

    public ScholarshipCategoryCatalogueImpl(ScholarshipService scholarships) {
        this.scholarships = scholarships;
    }

    @Override
    public Set<String> codes() {
        return scholarships.categories().stream().map(ScholarshipCategory::code).collect(Collectors.toUnmodifiableSet());
    }
}
