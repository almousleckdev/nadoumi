package com.nadoumi.university.domain;

/** One image in a university's gallery. URL-based until the Document slice. */
public record UniversityGalleryImage(Long id, String imageUrl, String caption) {
}
