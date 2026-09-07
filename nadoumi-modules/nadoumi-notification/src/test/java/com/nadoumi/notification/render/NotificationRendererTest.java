package com.nadoumi.notification.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nadoumi.common.notification.NotificationChannelKind;
import com.nadoumi.notification.domain.NotificationTemplate;
import com.nadoumi.notification.domain.NotificationType;
import com.nadoumi.notification.mapper.NotificationTemplateMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationRendererTest {

    private final NotificationTemplateMapper templateMapper = mock(NotificationTemplateMapper.class);
    private final NotificationRenderer renderer = new NotificationRenderer(templateMapper);

    private static NotificationTemplate template(String subject, String body) {
        NotificationTemplate t = new NotificationTemplate();
        t.setSubjectTpl(subject);
        t.setBodyTpl(body);
        return t;
    }

    @Test
    void substitutesDoubleBracePlaceholders() {
        when(templateMapper.find("SCHOLARSHIP_PUBLISHED", "EMAIL", "en"))
                .thenReturn(template("Published: {{scholarshipTitle}}",
                        "Ref {{scholarshipReference}} is live."));

        NotificationRenderer.Rendered r = renderer.render(NotificationType.SCHOLARSHIP_PUBLISHED,
                NotificationChannelKind.EMAIL, "en",
                Map.of("scholarshipTitle", "CSC Full", "scholarshipReference", "NAC-2026-0007"));

        assertThat(r.subject()).isEqualTo("Published: CSC Full");
        assertThat(r.body()).isEqualTo("Ref NAC-2026-0007 is live.");
    }

    @Test
    void fallsBackToEnglish_whenTheRequestedLocaleHasNoTemplate() {
        when(templateMapper.find("SCHOLARSHIP_PUBLISHED", "IN_APP", "fr")).thenReturn(null);
        when(templateMapper.find("SCHOLARSHIP_PUBLISHED", "IN_APP", "en"))
                .thenReturn(template(null, "{{scholarshipTitle}} published"));

        NotificationRenderer.Rendered r = renderer.render(NotificationType.SCHOLARSHIP_PUBLISHED,
                NotificationChannelKind.IN_APP, "fr", Map.of("scholarshipTitle", "X"));

        assertThat(r.subject()).isNull();
        assertThat(r.body()).isEqualTo("X published");
    }

    @Test
    void throws_whenNoTemplateAtAll() {
        when(templateMapper.find("SCHOLARSHIP_PUBLISHED", "EMAIL", "en")).thenReturn(null);

        assertThatThrownBy(() -> renderer.render(NotificationType.SCHOLARSHIP_PUBLISHED,
                NotificationChannelKind.EMAIL, "en", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no notification template");
    }

    @Test
    void throws_whenAPlaceholderIsUnbound() {
        when(templateMapper.find("SCHOLARSHIP_PUBLISHED", "EMAIL", "en"))
                .thenReturn(template("s", "needs {{missing}}"));

        assertThatThrownBy(() -> renderer.render(NotificationType.SCHOLARSHIP_PUBLISHED,
                NotificationChannelKind.EMAIL, "en", Map.of("present", "1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unbound placeholder: missing");
    }
}
