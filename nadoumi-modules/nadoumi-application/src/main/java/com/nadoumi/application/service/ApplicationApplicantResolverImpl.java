package com.nadoumi.application.service;

import com.nadoumi.common.access.ApplicationApplicantResolver;
import com.nadoumi.application.mapper.ApplicationMapper;
import org.springframework.stereotype.Component;

/**
 * Registers as the {@code Optional<ApplicationApplicantResolver>} bean
 * {@code NadoumiAccessServiceImpl} has been waiting for — the moment this module is
 * on the classpath, application-scoped authorization for external users activates
 * automatically. No change to {@code nadoumi-identity} was needed.
 */
@Component
public class ApplicationApplicantResolverImpl implements ApplicationApplicantResolver {

    private final ApplicationMapper mapper;

    public ApplicationApplicantResolverImpl(ApplicationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Long applicantIdOf(Long applicationId) {
        return applicationId == null ? null : mapper.findApplicantId(applicationId);
    }
}
