package com.nadoumi.applicant.web;

import com.nadoumi.applicant.service.InterestService;
import com.nadoumi.applicant.service.ResidenceService;
import com.nadoumi.applicant.service.WorkService;
import com.nadoumi.applicant.web.request.InterestRequest;
import com.nadoumi.applicant.web.request.ResidenceRequest;
import com.nadoumi.applicant.web.request.WorkRequest;
import com.nadoumi.applicant.web.response.InterestResponse;
import com.nadoumi.applicant.web.response.ResidenceResponse;
import com.nadoumi.applicant.web.response.WorkResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The student's own study interests, current location and work experience. */
@RestController
@RequestMapping("/api/student/applicants/{id}")
public class StudentProfileSectionsController {

    private final InterestService interests;
    private final ResidenceService residence;
    private final WorkService work;

    public StudentProfileSectionsController(InterestService interests, ResidenceService residence, WorkService work) {
        this.interests = interests;
        this.residence = residence;
        this.work = work;
    }

    @GetMapping("/interests")
    @PreAuthorize("@na.canAccessApplicant(#id, 'VIEW_PROFILE')")
    public ResponseEntity<InterestResponse> interests(@PathVariable Long id) {
        return OptionalResponses.okOrNoContent(interests.get(id));
    }

    @PutMapping("/interests")
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public InterestResponse saveInterests(@PathVariable Long id, @Valid @RequestBody InterestRequest req) {
        return interests.save(id, req);
    }

    @GetMapping("/residence")
    @PreAuthorize("@na.canAccessApplicant(#id, 'VIEW_PROFILE')")
    public ResponseEntity<ResidenceResponse> residence(@PathVariable Long id) {
        return OptionalResponses.okOrNoContent(residence.get(id));
    }

    @PutMapping("/residence")
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public ResidenceResponse saveResidence(@PathVariable Long id, @Valid @RequestBody ResidenceRequest req) {
        return residence.save(id, req);
    }

    @GetMapping("/work")
    @PreAuthorize("@na.canAccessApplicant(#id, 'VIEW_PROFILE')")
    public List<WorkResponse> work(@PathVariable Long id) {
        return work.list(id);
    }

    @PostMapping("/work")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public WorkResponse addWork(@PathVariable Long id, @Valid @RequestBody WorkRequest req) {
        return work.add(id, req);
    }

    @PutMapping("/work/{workId}")
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public WorkResponse updateWork(@PathVariable Long id, @PathVariable Long workId, @Valid @RequestBody WorkRequest req) {
        return work.update(id, workId, req);
    }

    @DeleteMapping("/work/{workId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public void deleteWork(@PathVariable Long id, @PathVariable Long workId) {
        work.delete(id, workId);
    }
}
