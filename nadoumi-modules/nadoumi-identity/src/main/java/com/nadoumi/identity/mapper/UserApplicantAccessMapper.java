package com.nadoumi.identity.mapper;

import com.nadoumi.identity.domain.UserApplicantAccess;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserApplicantAccessMapper {

    UserApplicantAccess findById(Long id);

    /** ACTIVE, non-expired, applicant-wide (application_id IS NULL) grant for this user+applicant. */
    UserApplicantAccess findActiveApplicantGrant(@Param("userId") Long userId,
                                                 @Param("applicantId") Long applicantId);

    /** ACTIVE, non-expired grant that authorizes this application: applicant-wide OR scoped to it. */
    UserApplicantAccess findActiveApplicationGrant(@Param("userId") Long userId,
                                                   @Param("applicantId") Long applicantId,
                                                   @Param("applicationId") Long applicationId);

    List<UserApplicantAccess> findActiveGrantsForUser(@Param("userId") Long userId);

    List<UserApplicantAccess> findByApplicant(@Param("applicantId") Long applicantId);

    UserApplicantAccess findActiveOwner(@Param("applicantId") Long applicantId);

    /** Same as {@link #findActiveOwner} but takes a row lock for the transfer / invariant path. */
    UserApplicantAccess lockActiveOwner(@Param("applicantId") Long applicantId);

    UserApplicantAccess findPendingInvite(@Param("applicantId") Long applicantId,
                                          @Param("invitedEmail") String invitedEmail);

    List<UserApplicantAccess> findPendingInvitesByEmail(@Param("invitedEmail") String invitedEmail);

    List<Long> accessibleApplicantIds(@Param("userId") Long userId);

    /** Every user that ever held a grant on the applicant, any status. */
    List<Long> userIdsForApplicant(@Param("applicantId") Long applicantId);

    /** Grants (any status) a user holds across all applicants; 0 means the user is left with no applicant. */
    int countForUser(@Param("userId") Long userId);

    /** Permanent erasure of an applicant removes its grants; they are the only rows exempt from "never deleted". */
    int deleteByApplicantId(@Param("applicantId") Long applicantId);

    int insert(UserApplicantAccess grant);

    int update(UserApplicantAccess grant);
}
