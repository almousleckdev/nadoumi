package com.nadoumi.applicant.mapper;

import com.nadoumi.applicant.domain.ApplicantFamilyMember;
import com.nadoumi.applicant.domain.ApplicantPerson;
import com.nadoumi.applicant.domain.ApplicantPersonal;
import com.nadoumi.applicant.domain.enums.PersonRole;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** MyBatis mapper for the personal details, family members and the two related people of an applicant. */
public interface FamilyMapper {

    ApplicantPersonal findPersonal(@Param("applicantId") long applicantId);

    int insertPersonal(ApplicantPersonal personal);

    int updatePersonal(ApplicantPersonal personal);

    List<ApplicantFamilyMember> findMembers(@Param("applicantId") long applicantId);

    ApplicantFamilyMember findMember(@Param("id") long id);

    int insertMember(ApplicantFamilyMember member);

    int updateMember(ApplicantFamilyMember member);

    int deleteMember(@Param("id") long id, @Param("applicantId") long applicantId);

    ApplicantPerson findPerson(@Param("applicantId") long applicantId, @Param("role") PersonRole role);

    int insertPerson(ApplicantPerson person);

    int updatePerson(ApplicantPerson person);
}
