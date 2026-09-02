package com.nadoumi.identity.web.request;

import com.nadoumi.common.access.AccessRole;
import jakarta.validation.constraints.NotNull;

/** {@code previousOwnerNewRole} null / OWNER means the outgoing owner is revoked. */
public record TransferOwnershipRequest(@NotNull Long toUserId, AccessRole previousOwnerNewRole) {
}
