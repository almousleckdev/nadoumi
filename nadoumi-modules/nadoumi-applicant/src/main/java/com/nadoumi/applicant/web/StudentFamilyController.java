package com.nadoumi.applicant.web;

import com.nadoumi.applicant.domain.enums.PersonRole;
import com.nadoumi.applicant.service.FamilyService;
import com.nadoumi.applicant.service.FormReadinessService;
import com.nadoumi.applicant.web.request.FamilyMemberRequest;
import com.nadoumi.applicant.web.request.PersonRequest;
import com.nadoumi.applicant.web.request.PersonalDetailsRequest;
import com.nadoumi.applicant.web.response.FamilyMemberResponse;
import com.nadoumi.applicant.web.response.FormReadinessResponse;
import com.nadoumi.applicant.web.response.PersonResponse;
import com.nadoumi.applicant.web.response.PersonalDetailsResponse;
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

/** The student's personal details, family, financial supporter and emergency contact in China. */
@RestController
@RequestMapping("/api/student/applicants/{id}")
public class StudentFamilyController {

    private final FamilyService family;
    private final FormReadinessService readiness;

    public StudentFamilyController(FamilyService family, FormReadinessService readiness) {
        this.family = family;
        this.readiness = readiness;
    }

    @GetMapping("/personal-details")
    @PreAuthorize("@na.canAccessApplicant(#id, 'VIEW_PROFILE')")
    public ResponseEntity<PersonalDetailsResponse> personal(@PathVariable Long id) {
        return OptionalResponses.okOrNoContent(family.personal(id));
    }

    @PutMapping("/personal-details")
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public PersonalDetailsResponse savePersonal(@PathVariable Long id, @Valid @RequestBody PersonalDetailsRequest req) {
        return family.savePersonal(id, req);
    }

    @GetMapping("/family")
    @PreAuthorize("@na.canAccessApplicant(#id, 'VIEW_PROFILE')")
    public List<FamilyMemberResponse> members(@PathVariable Long id) {
        return family.members(id);
    }

    @PostMapping("/family")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public FamilyMemberResponse addMember(@PathVariable Long id, @Valid @RequestBody FamilyMemberRequest req) {
        return family.addMember(id, req);
    }

    @PutMapping("/family/{memberId}")
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public FamilyMemberResponse updateMember(@PathVariable Long id, @PathVariable Long memberId,
            @Valid @RequestBody FamilyMemberRequest req) {
        return family.updateMember(id, memberId, req);
    }

    @DeleteMapping("/family/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public void deleteMember(@PathVariable Long id, @PathVariable Long memberId) {
        family.deleteMember(id, memberId);
    }

    /** {@code role} is {@code supporter} or {@code china-emergency}. */
    @GetMapping("/people/{role}")
    @PreAuthorize("@na.canAccessApplicant(#id, 'VIEW_PROFILE')")
    public ResponseEntity<PersonResponse> person(@PathVariable Long id, @PathVariable String role) {
        return OptionalResponses.okOrNoContent(family.person(id, roleOf(role)));
    }

    @PutMapping("/people/{role}")
    @PreAuthorize("@na.canAccessApplicant(#id, 'EDIT_PROFILE')")
    public PersonResponse savePerson(@PathVariable Long id, @PathVariable String role,
            @Valid @RequestBody PersonRequest req) {
        return family.savePerson(id, roleOf(role), req);
    }

    @GetMapping("/form-readiness")
    @PreAuthorize("@na.canAccessApplicant(#id, 'VIEW_PROFILE')")
    public FormReadinessResponse formReadiness(@PathVariable Long id) {
        return readiness.of(id);
    }

    private static PersonRole roleOf(String role) {
        return switch (role) {
            case "supporter" -> PersonRole.SUPPORTER;
            case "china-emergency" -> PersonRole.CHINA_EMERGENCY;
            default -> throw new com.nadoumi.common.exception.NadNotFoundException("unknown role");
        };
    }
}
