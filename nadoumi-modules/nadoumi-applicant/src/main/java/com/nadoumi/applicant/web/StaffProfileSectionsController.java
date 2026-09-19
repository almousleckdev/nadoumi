package com.nadoumi.applicant.web;

import com.nadoumi.applicant.service.InterestService;
import com.nadoumi.applicant.service.ResidenceService;
import com.nadoumi.applicant.service.WorkService;
import com.nadoumi.applicant.web.response.InterestResponse;
import com.nadoumi.applicant.web.response.ResidenceResponse;
import com.nadoumi.applicant.web.response.WorkResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Staff read access to an applicant's interests, location and work experience. */
@RestController
@RequestMapping("/api/staff/applicants/{id}")
public class StaffProfileSectionsController {

    private final InterestService interests;
    private final ResidenceService residence;
    private final WorkService work;

    public StaffProfileSectionsController(InterestService interests, ResidenceService residence, WorkService work) {
        this.interests = interests;
        this.residence = residence;
        this.work = work;
    }

    @GetMapping("/interests")
    @PreAuthorize("@ss.hasPermi('nad:applicant:view')")
    public ResponseEntity<InterestResponse> interests(@PathVariable Long id) {
        return OptionalResponses.okOrNoContent(interests.get(id));
    }

    @GetMapping("/residence")
    @PreAuthorize("@ss.hasPermi('nad:applicant:view')")
    public ResponseEntity<ResidenceResponse> residence(@PathVariable Long id) {
        return OptionalResponses.okOrNoContent(residence.get(id));
    }

    @GetMapping("/work")
    @PreAuthorize("@ss.hasPermi('nad:applicant:view')")
    public List<WorkResponse> work(@PathVariable Long id) {
        return work.list(id);
    }
}
