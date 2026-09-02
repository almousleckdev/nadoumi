package com.nadoumi.identity.access;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nadoumi.common.access.ApplicantCapability;
import java.util.EnumSet;
import java.util.Set;

/**
 * Parsed {@code nad_user_applicant_access.capability_overrides_json}. Shape:
 * {@code {"add":["SUBMIT_APPLICATION"],"remove":["EDIT_PROFILE"]}}. Unknown capability
 * names are dropped so an old row never breaks a newer deployment.
 */
public record CapabilityOverrides(Set<ApplicantCapability> add, Set<ApplicantCapability> remove) {

    private static final CapabilityOverrides EMPTY = new CapabilityOverrides(Set.of(), Set.of());

    public static CapabilityOverrides empty() {
        return EMPTY;
    }

    public static CapabilityOverrides parse(String json) {
        if (json == null || json.isBlank()) {
            return EMPTY;
        }
        JSONObject root = JSONObject.parseObject(json);
        return new CapabilityOverrides(read(root.getJSONArray("add")), read(root.getJSONArray("remove")));
    }

    private static Set<ApplicantCapability> read(JSONArray names) {
        Set<ApplicantCapability> out = EnumSet.noneOf(ApplicantCapability.class);
        if (names == null) {
            return out;
        }
        for (Object name : names) {
            try {
                out.add(ApplicantCapability.valueOf(String.valueOf(name)));
            } catch (IllegalArgumentException ignored) {
                // forward-compatible: skip a capability this build does not know
            }
        }
        return out;
    }

    public boolean isEmpty() {
        return add.isEmpty() && remove.isEmpty();
    }

    public String toJson() {
        JSONObject root = new JSONObject();
        root.put("add", add.stream().map(Enum::name).toList());
        root.put("remove", remove.stream().map(Enum::name).toList());
        return root.toJSONString();
    }
}
