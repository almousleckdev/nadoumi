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

    /** Owner ids of the applicants this user owns right now. */
    List<Long> ownedApplicantIds(@Param("userId") Long userId);

    /** Revokes every live grant a user holds (account deletion). Rows are kept for the audit trail. */
    int revokeAllForUser(@Param("userId") Long userId, @Param("revokedBy") Long revokedBy, @Param("updateBy") String updateBy);

    int insert(UserApplicantAccess grant);

    int update(UserApplicantAccess grant);
}
