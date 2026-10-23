package com.nadoumi.program.web.response;

import java.util.List;

/** What a pasted list did: the catalogue rows it matched or created (in input order) and how many were new. */
public record MajorBulkResponse(List<MajorResponse> majors, int created) {
}
