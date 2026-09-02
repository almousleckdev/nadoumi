package com.nadoumi.common.access;

import static com.nadoumi.common.access.ApplicantCapability.EDIT_PROFILE;
import static com.nadoumi.common.access.ApplicantCapability.MANAGE_ACCESS;
import static com.nadoumi.common.access.ApplicantCapability.MESSAGE_STAFF;
import static com.nadoumi.common.access.ApplicantCapability.UPLOAD_DOCUMENT;
import static com.nadoumi.common.access.ApplicantCapability.VIEW_APPLICATION;
import static com.nadoumi.common.access.ApplicantCapability.VIEW_DOCUMENT;
import static com.nadoumi.common.access.ApplicantCapability.VIEW_PROFILE;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Default role &rarr; capability mapping from {@code docs/DOMAIN_MODEL.md} §4.2, plus the
 * rule for applying a grant's per-row overrides. Pure functions, no state.
 */
public final class AccessCapabilityMatrix {

    private static final Map<AccessRole, Set<ApplicantCapability>> DEFAULTS = Map.of(
            AccessRole.OWNER, EnumSet.allOf(ApplicantCapability.class),
            AccessRole.AGENT, EnumSet.complementOf(EnumSet.of(MANAGE_ACCESS)),
            AccessRole.GUARDIAN, EnumSet.of(VIEW_PROFILE, EDIT_PROFILE, VIEW_APPLICATION,
                    VIEW_DOCUMENT, UPLOAD_DOCUMENT, MESSAGE_STAFF),
            AccessRole.VIEWER, EnumSet.of(VIEW_PROFILE, VIEW_APPLICATION, VIEW_DOCUMENT));

    private AccessCapabilityMatrix() {
    }

    public static Set<ApplicantCapability> defaultCapabilities(AccessRole role) {
        return Set.copyOf(DEFAULTS.get(role));
    }

    /** Role default, then {@code add} unioned in, then {@code remove} subtracted (remove wins). Nulls are empty. */
    public static Set<ApplicantCapability> effectiveCapabilities(AccessRole role,
            Set<ApplicantCapability> add, Set<ApplicantCapability> remove) {
        var effective = EnumSet.copyOf(DEFAULTS.get(role));
        if (add != null) {
            effective.addAll(add);
        }
        if (remove != null) {
            effective.removeAll(remove);
        }
        return effective;
    }

    public static boolean hasCapability(AccessRole role, ApplicantCapability capability,
            Set<ApplicantCapability> add, Set<ApplicantCapability> remove) {
        return effectiveCapabilities(role, add, remove).contains(capability);
    }
}
