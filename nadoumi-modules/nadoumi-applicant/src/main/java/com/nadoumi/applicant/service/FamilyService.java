package com.nadoumi.applicant.service;

import com.nadoumi.applicant.domain.ApplicantFamilyMember;
import com.nadoumi.applicant.domain.ApplicantPerson;
import com.nadoumi.applicant.domain.ApplicantPersonal;
import com.nadoumi.applicant.domain.enums.MailingChoice;
import com.nadoumi.applicant.domain.enums.PersonRole;
import com.nadoumi.applicant.mapper.FamilyMapper;
import com.nadoumi.applicant.rules.FamilyRules;
import com.nadoumi.applicant.web.request.FamilyMemberRequest;
import com.nadoumi.applicant.web.request.PersonRequest;
import com.nadoumi.applicant.web.request.PersonalDetailsRequest;
import com.nadoumi.applicant.web.response.FamilyMemberResponse;
import com.nadoumi.applicant.web.response.PersonResponse;
import com.nadoumi.applicant.web.response.PersonalDetailsResponse;
import com.nadoumi.common.access.ApplicantCapability;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The extra details the Nadoumi application form asks for, kept on the applicant so a student is asked once:
 * personal details, family members, the financial supporter and the emergency contact in China. Every call
 * re-checks the caller's grant on the applicant.
 */
@Service
public class FamilyService {

    private final FamilyMapper mapper;
    private final ApplicantAccessGuard guard;

    public FamilyService(FamilyMapper mapper, ApplicantAccessGuard guard) {
        this.mapper = mapper;
        this.guard = guard;
    }

    // personal details

    @Transactional(readOnly = true)
    public Optional<PersonalDetailsResponse> personal(Long applicantId) {
        guard.require(applicantId, ApplicantCapability.VIEW_PROFILE);
        return Optional.ofNullable(mapper.findPersonal(applicantId)).map(PersonalDetailsResponse::of);
    }

    @Transactional(rollbackFor = Exception.class)
    public PersonalDetailsResponse savePersonal(Long applicantId, PersonalDetailsRequest req) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        ApplicantPersonal personal = new ApplicantPersonal();
        personal.setApplicantId(applicantId);
        personal.setMaritalStatus(req.maritalStatus());
        personal.setReligion(blankToNull(req.religion()));
        personal.setPlaceOfBirth(blankToNull(req.placeOfBirth()));
        personal.setOfficialLanguage(blankToNull(req.officialLanguage()));
        personal.setPermanentAddress(blankToNull(req.permanentAddress()));
        personal.setPermanentPostcode(blankToNull(req.permanentPostcode()));
        personal.setCurrentPostcode(blankToNull(req.currentPostcode()));
        MailingChoice choice = req.mailingChoice() == null ? MailingChoice.CURRENT : req.mailingChoice();
        personal.setMailingChoice(choice);
        personal.setMailingAddress(choice == MailingChoice.OTHER ? blankToNull(req.mailingAddress()) : null);
        personal.setMailingPostcode(choice == MailingChoice.OTHER ? blankToNull(req.mailingPostcode()) : null);
        if (mapper.findPersonal(applicantId) == null) {
            try {
                mapper.insertPersonal(personal);
                return PersonalDetailsResponse.of(personal);
            }
            catch (DuplicateKeyException e) {
                // a concurrent save created the row first: update it instead
            }
        }
        mapper.updatePersonal(personal);
        return PersonalDetailsResponse.of(personal);
    }

    // family members

    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> members(Long applicantId) {
        guard.require(applicantId, ApplicantCapability.VIEW_PROFILE);
        return mapper.findMembers(applicantId).stream().map(FamilyMemberResponse::of).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public FamilyMemberResponse addMember(Long applicantId, FamilyMemberRequest req) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        ApplicantFamilyMember member = new ApplicantFamilyMember();
        member.setApplicantId(applicantId);
        apply(member, req, null);
        mapper.insertMember(member);
        return FamilyMemberResponse.of(member);
    }

    @Transactional(rollbackFor = Exception.class)
    public FamilyMemberResponse updateMember(Long applicantId, Long memberId, FamilyMemberRequest req) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        ApplicantFamilyMember member = mapper.findMember(memberId);
        if (member == null || !member.getApplicantId().equals(applicantId)) {
            throw new NadNotFoundException("family member not found");
        }
        apply(member, req, memberId);
        mapper.updateMember(member);
        return FamilyMemberResponse.of(member);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteMember(Long applicantId, Long memberId) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        if (mapper.deleteMember(memberId, applicantId) == 0) {
            throw new NadNotFoundException("family member not found");
        }
    }

    // supporter and emergency contact in China

    @Transactional(readOnly = true)
    public Optional<PersonResponse> person(Long applicantId, PersonRole role) {
        guard.require(applicantId, ApplicantCapability.VIEW_PROFILE);
        return Optional.ofNullable(mapper.findPerson(applicantId, role)).map(PersonResponse::of);
    }

    @Transactional(rollbackFor = Exception.class)
    public PersonResponse savePerson(Long applicantId, PersonRole role, PersonRequest req) {
        guard.require(applicantId, ApplicantCapability.EDIT_PROFILE);
        ApplicantPerson person = new ApplicantPerson();
        person.setApplicantId(applicantId);
        person.setRole(role);
        person.setFullName(req.fullName().strip());
        person.setRelationship(req.relationship().strip());
        person.setGender(req.gender());
        person.setEmail(blankToNull(req.email()));
        person.setPhone(req.phone().strip());
        person.setPosition(blankToNull(req.position()));
        person.setCompany(blankToNull(req.company()));
        person.setCompanyAddress(blankToNull(req.companyAddress()));
        person.setHomeAddress(blankToNull(req.homeAddress()));
        if (mapper.findPerson(applicantId, role) == null) {
            try {
                mapper.insertPerson(person);
                return PersonResponse.of(person);
            }
            catch (DuplicateKeyException e) {
                // a concurrent save created the row first: update it instead
            }
        }
        mapper.updatePerson(person);
        return PersonResponse.of(person);
    }

    private void apply(ApplicantFamilyMember member, FamilyMemberRequest req, Long ownId) {
        boolean deceased = Boolean.TRUE.equals(req.deceased());
        FamilyRules.validate(deceased, req.note(), req.dateOfBirth(), LocalDate.now(ZoneOffset.UTC));
        if (req.relation().isSingular() && mapper.findMembers(member.getApplicantId()).stream()
                .anyMatch(m -> m.getRelation() == req.relation() && !m.getId().equals(ownId))) {
            throw new NadBadRequestException("a " + req.relation().name().toLowerCase() + " is already recorded");
        }
        member.setRelation(req.relation());
        member.setFullName(req.fullName().strip());
        member.setDateOfBirth(req.dateOfBirth());
        member.setNationality(blankToNull(req.nationality()));
        member.setDeceased(deceased);
        member.setNote(blankToNull(req.note()));
        member.setCompany(blankToNull(req.company()));
        member.setPosition(blankToNull(req.position()));
        member.setCompanyAddress(blankToNull(req.companyAddress()));
        member.setPhone(blankToNull(req.phone()));
        member.setEmail(blankToNull(req.email()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
