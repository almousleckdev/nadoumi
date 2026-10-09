package com.nadoumi.applicant.web;

import com.nadoumi.applicant.mapper.ApplicantMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lets URLs identify an applicant by an unguessable UUID. {@code /api/staff/applicants/{uuid}/...} and
 * {@code /api/student/applicants/{uuid}/...} are rewritten to the internal numeric id before the controllers see
 * them, so no controller changes and authorization still runs on the real id.
 *
 * <p>The staff API accepts <b>only</b> the UUID: a numeric id there answers 404, so applicants cannot be walked
 * by counting. The student API keeps accepting the numeric id, because a student can reach nothing but their own
 * applicants (the access guard decides), and the web app still passes the id it received at sign-in.</p>
 *
 * <p>This runs after Spring Security, so only authenticated requests cost a lookup.</p>
 */
@Component
public class ApplicantPublicIdFilter extends OncePerRequestFilter {

    private static final Pattern PATH = Pattern.compile("^/api/(staff|student)/applicants/([^/]+)(/.*)?$");
    private static final Pattern UUID_FORM =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern DIGITS = Pattern.compile("^\\d+$");
    private static final String STAFF = "staff";

    private final ApplicantMapper applicants;

    public ApplicantPublicIdFilter(ApplicantMapper applicants) {
        this.applicants = applicants;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        Matcher m = PATH.matcher(path);
        if (!m.matches()) {
            chain.doFilter(request, response);
            return;
        }
        String audience = m.group(1);
        String segment = m.group(2);

        if (UUID_FORM.matcher(segment).matches()) {
            Long id = applicants.findIdByPublicId(segment.toLowerCase(Locale.ROOT));
            if (id == null) {
                notFound(response);
                return;
            }
            chain.doFilter(new Rewritten(request, segment, id), response);
            return;
        }
        if (STAFF.equals(audience) && DIGITS.matcher(segment).matches()) {
            notFound(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private static void notFound(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("{\"status\":404,\"detail\":\"applicant not found\"}");
    }

    /**
     * The same request with the applicant segment replaced wherever the servlet container reports the path, so MVC
     * maps and binds the numeric id whatever way the dispatcher servlet is mapped.
     */
    private static final class Rewritten extends HttpServletRequestWrapper {

        private final String from;
        private final String to;

        Rewritten(HttpServletRequest request, String segment, long id) {
            super(request);
            this.from = "/applicants/" + segment;
            this.to = "/applicants/" + id;
        }

        private String swap(String value) {
            return value == null ? null : value.replaceFirst(Pattern.quote(from), java.util.regex.Matcher.quoteReplacement(to));
        }

        @Override
        public String getRequestURI() {
            return swap(super.getRequestURI());
        }

        @Override
        public StringBuffer getRequestURL() {
            return new StringBuffer(swap(super.getRequestURL().toString()));
        }

        @Override
        public String getServletPath() {
            return swap(super.getServletPath());
        }

        @Override
        public String getPathInfo() {
            return swap(super.getPathInfo());
        }
    }
}
