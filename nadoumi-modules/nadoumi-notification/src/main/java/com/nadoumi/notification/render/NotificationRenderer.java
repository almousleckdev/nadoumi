package com.nadoumi.notification.render;

import com.nadoumi.common.notification.NotificationChannelKind;
import com.nadoumi.notification.domain.NotificationTemplate;
import com.nadoumi.notification.domain.NotificationType;
import com.nadoumi.notification.mapper.NotificationTemplateMapper;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves a {@code nad_notification_template} for a {@code (type, channel, locale)}
 * — falling back to {@code locale = 'en'} — and substitutes its {@code {{var}}}
 * placeholders from a context map. An unbound placeholder or a missing template is
 * a hard error, so a mis-seeded template fails loudly rather than shipping a
 * half-rendered message.
 */
public class NotificationRenderer {

    /** Default locale; every {@link NotificationType} must have an {@code en} template. */
    public static final String DEFAULT_LOCALE = "en";

    private static final Pattern VAR = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    private final NotificationTemplateMapper templateMapper;

    public NotificationRenderer(NotificationTemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    public record Rendered(String subject, String body) {
    }

    public Rendered render(NotificationType type, NotificationChannelKind channel, String locale,
            Map<String, Object> context) {
        String effectiveLocale = locale == null || locale.isBlank() ? DEFAULT_LOCALE : locale;
        NotificationTemplate template = templateMapper.find(type.name(), channel.name(), effectiveLocale);
        if (template == null && !DEFAULT_LOCALE.equals(effectiveLocale)) {
            template = templateMapper.find(type.name(), channel.name(), DEFAULT_LOCALE);
        }
        if (template == null) {
            throw new IllegalStateException("no notification template for " + type + "/" + channel
                    + " (locale " + effectiveLocale + " or " + DEFAULT_LOCALE + ")");
        }
        String subject = template.getSubjectTpl() == null ? null : substitute(template.getSubjectTpl(), context, type);
        String body = substitute(template.getBodyTpl(), context, type);
        return new Rendered(subject, body);
    }

    private static String substitute(String raw, Map<String, Object> context, NotificationType type) {
        Matcher matcher = VAR.matcher(raw);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = context == null ? null : context.get(key);
            if (value == null) {
                throw new IllegalStateException(
                        "notification template for " + type + " has an unbound placeholder: " + key);
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
