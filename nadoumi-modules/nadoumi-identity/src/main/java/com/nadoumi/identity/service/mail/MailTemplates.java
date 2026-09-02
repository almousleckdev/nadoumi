package com.nadoumi.identity.service.mail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * Loads {@code classpath:/mail/<name>.txt} and substitutes every {@code ${key}}
 * placeholder. English-only for now; per-locale templates arrive with the
 * Notification slice (docs/COMMUNICATION_AND_NOTIFICATIONS.md §4).
 */
@Component
public class MailTemplates {

    private static final Pattern VAR = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}");

    public String render(String template, Map<String, String> vars) {
        String raw = load(template);
        Matcher matcher = VAR.matcher(raw);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = vars.get(key);
            if (value == null) {
                throw new IllegalArgumentException(
                        "mail template '" + template + "' has an unbound placeholder: " + key);
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String load(String template) {
        String path = "/mail/" + template + ".txt";
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("mail template not found: " + path);
            }
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new IllegalArgumentException("mail template unreadable: " + path, e);
        }
    }
}
