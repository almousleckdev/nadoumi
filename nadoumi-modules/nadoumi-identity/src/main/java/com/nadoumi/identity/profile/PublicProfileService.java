package com.nadoumi.identity.profile;

import com.nadoumi.identity.mapper.PublicProfileMapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves how users appear publicly (comments, likers), in one query for any number of users. */
@Service
public class PublicProfileService {

    private final PublicProfileMapper mapper;

    public PublicProfileService(PublicProfileMapper mapper) {
        this.mapper = mapper;
    }

    /** A profile per user that still exists; ids with no matching user are simply absent from the map. */
    @Transactional(readOnly = true)
    public Map<Long, PublicProfile> resolve(Collection<Long> userIds) {
        Set<Long> ids = userIds.stream().filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, PublicProfile> profiles = new HashMap<>();
        for (PublicProfileRow row : mapper.findByUserIds(ids)) {
            profiles.put(row.getUserId(), new PublicProfile(
                    PublicProfileRules.displayName(row.getUserType(), row.getNickName(), row.getUserName(),
                            row.getOwnerGivenName()),
                    PublicProfileRules.avatarUrl(row.getUserType(), row.getAvatar())));
        }
        return profiles;
    }
}
