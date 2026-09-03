package com.nadoumi.scholarship.mapper;

/** One facet bucket: a filter value and how many published scholarships match it. */
public record ScholarshipFacetRow(String value, long count) {
}
