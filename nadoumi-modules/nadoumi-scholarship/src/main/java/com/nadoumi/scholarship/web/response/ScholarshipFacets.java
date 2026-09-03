package com.nadoumi.scholarship.web.response;

import com.nadoumi.scholarship.mapper.ScholarshipFacetRow;
import java.util.List;

/**
 * Live facet counts for the current scholarship filter set — how many published
 * scholarships match each value, so the UI can show counts next to each filter.
 */
public record ScholarshipFacets(
        List<Bucket> levels,
        List<Bucket> categories,
        List<Bucket> fundingModels,
        List<Bucket> teachingLanguages) {

    public record Bucket(String value, long count) {
        static Bucket of(ScholarshipFacetRow row) {
            return new Bucket(row.value(), row.count());
        }
    }

    public static ScholarshipFacets of(List<ScholarshipFacetRow> levels, List<ScholarshipFacetRow> categories,
            List<ScholarshipFacetRow> fundingModels, List<ScholarshipFacetRow> teachingLanguages) {
        return new ScholarshipFacets(
                levels.stream().map(Bucket::of).toList(),
                categories.stream().map(Bucket::of).toList(),
                fundingModels.stream().map(Bucket::of).toList(),
                teachingLanguages.stream().map(Bucket::of).toList());
    }
}
