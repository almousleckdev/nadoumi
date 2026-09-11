package com.nadoumi.notification.config;

import com.nadoumi.common.notification.NotificationChannel;
import com.nadoumi.common.outbox.OutboxWriter;
import com.nadoumi.identity.service.mail.BrandProperties;
import com.nadoumi.identity.service.mail.EmailLayout;
import com.nadoumi.identity.service.mail.MailSender;
import com.nadoumi.notification.channel.EmailNotificationChannel;
import com.nadoumi.notification.dispatch.NotificationDeliveryDispatcher;
import com.nadoumi.notification.job.NotificationDispatchJob;
import com.nadoumi.notification.job.OutboxPollerJob;
import com.nadoumi.notification.mapper.NotificationAudienceMapper;
import com.nadoumi.notification.mapper.NotificationDeliveryMapper;
import com.nadoumi.notification.mapper.NotificationMapper;
import com.nadoumi.notification.mapper.NotificationPreferenceMapper;
import com.nadoumi.notification.mapper.NotificationRecipientMapper;
import com.nadoumi.notification.mapper.NotificationTemplateMapper;
import com.nadoumi.notification.mapper.OutboxEventMapper;
import com.nadoumi.notification.outbox.OutboxDispatcher;
import com.nadoumi.notification.outbox.OutboxToNotificationDispatcher;
import com.nadoumi.notification.outbox.OutboxWriterImpl;
import com.nadoumi.notification.outbox.WelcomeContentComposer;
import com.nadoumi.notification.render.NotificationRenderer;
import com.nadoumi.notification.service.NotificationService;
import com.nadoumi.program.service.ProgramService;
import com.nadoumi.scholarship.service.ScholarshipService;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * The single wiring point for {@code nadoumi-notification}. Registered as a Spring
 * Boot auto-configuration (see
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports})
 * rather than component-scanned, so every bean is {@link ConditionalOnMissingBean}
 * and a test can substitute its own {@link OutboxDispatcher} or
 * {@link OutboxWriter} without the Quartz job ever being built.
 */
@AutoConfiguration
public class NotificationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(OutboxWriter.class)
    OutboxWriter outboxWriter(OutboxEventMapper outboxEventMapper) {
        return new OutboxWriterImpl(outboxEventMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    WelcomeContentComposer welcomeContentComposer(NotificationService notificationService,
            NotificationRenderer notificationRenderer, EmailLayout emailLayout, MailSender mailSender,
            BrandProperties brandProperties, ScholarshipService scholarshipService, ProgramService programService) {
        return new WelcomeContentComposer(notificationService, notificationRenderer, emailLayout, mailSender,
                brandProperties, scholarshipService, programService);
    }

    @Bean
    @ConditionalOnMissingBean(OutboxDispatcher.class)
    OutboxDispatcher outboxDispatcher(NotificationService notificationService, NotificationRenderer notificationRenderer,
            NotificationAudienceMapper notificationAudienceMapper, WelcomeContentComposer welcomeContentComposer) {
        return new OutboxToNotificationDispatcher(notificationService, notificationRenderer, notificationAudienceMapper,
                welcomeContentComposer);
    }

    /**
     * The outbox poller. Registered under the bean name {@code outboxPollerJob} so
     * a {@code sys_job} row can invoke it as {@code outboxPollerJob.run()} (seeded
     * active by V31).
     */
    @Bean
    @ConditionalOnMissingBean
    OutboxPollerJob outboxPollerJob(OutboxEventMapper outboxEventMapper, OutboxDispatcher outboxDispatcher) {
        return new OutboxPollerJob(outboxEventMapper, outboxDispatcher);
    }

    @Bean
    @ConditionalOnMissingBean
    NotificationService notificationService(NotificationMapper notificationMapper,
            NotificationDeliveryMapper notificationDeliveryMapper,
            NotificationPreferenceMapper notificationPreferenceMapper) {
        return new NotificationService(notificationMapper, notificationDeliveryMapper, notificationPreferenceMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    NotificationRenderer notificationRenderer(NotificationTemplateMapper notificationTemplateMapper) {
        return new NotificationRenderer(notificationTemplateMapper);
    }

    @Bean
    @ConditionalOnMissingBean(name = "emailNotificationChannel")
    NotificationChannel emailNotificationChannel(MailSender mailSender, EmailLayout emailLayout,
            BrandProperties brandProperties) {
        return new EmailNotificationChannel(mailSender, emailLayout, brandProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    NotificationDeliveryDispatcher notificationDeliveryDispatcher(NotificationDeliveryMapper notificationDeliveryMapper,
            NotificationMapper notificationMapper, NotificationRecipientMapper notificationRecipientMapper,
            NotificationRenderer notificationRenderer, List<NotificationChannel> notificationChannels) {
        return new NotificationDeliveryDispatcher(notificationDeliveryMapper, notificationMapper,
                notificationRecipientMapper, notificationRenderer, notificationChannels);
    }

    /**
     * The notification channel-dispatch Quartz job. Bean name
     * {@code notificationDispatchJob} so a {@code sys_job} row can invoke it as
     * {@code notificationDispatchJob.run()} (seeded active by V34).
     */
    @Bean
    @ConditionalOnMissingBean
    NotificationDispatchJob notificationDispatchJob(NotificationDeliveryDispatcher notificationDeliveryDispatcher) {
        return new NotificationDispatchJob(notificationDeliveryDispatcher);
    }
}
