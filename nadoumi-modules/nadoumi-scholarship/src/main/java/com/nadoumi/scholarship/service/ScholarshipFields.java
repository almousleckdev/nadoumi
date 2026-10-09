package com.nadoumi.scholarship.service;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.text.Texts;
import com.nadoumi.scholarship.domain.ScholarshipField;
import com.nadoumi.scholarship.web.request.ScholarshipRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The fields of study of a scholarship: one or many, each for every level or for one degree level. Cleans what
 * the form sent (trimmed, no blanks, no duplicates), refuses a field tied to a level the scholarship does not
 * offer, and builds the short summary kept on the scholarship row for text search.
 */
final class ScholarshipFields {

    /** The summary column holds this many characters. */
    static final int SUMMARY_MAX = 120;
    private static final String SEPARATOR = ", ";

    private ScholarshipFields() {
    }

    static List<ScholarshipField> normalise(ScholarshipRequest req) {
        List<ScholarshipRequest.FieldInput> source = requested(req);
        Set<String> offered = new HashSet<>();
        if (req.levels() != null) {
            req.levels().forEach(level -> offered.add(level.name()));
        }
        List<ScholarshipField> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ScholarshipRequest.FieldInput in : source) {
            String name = Texts.blankToNull(in.name());
            if (name == null) {
                continue;
            }
            String level = in.level() == null ? null : in.level().name();
            if (level != null && !offered.contains(level)) {
                throw new NadBadRequestException("field '" + name + "' is set for " + level
                        + ", which this scholarship does not offer");
            }
            if (seen.add((level == null ? "ALL" : level) + "|" + name.trim().toLowerCase(Locale.ROOT))) {
                out.add(new ScholarshipField(level, name.trim()));
            }
        }
        return out;
    }

    /** The distinct names, joined, cut at a whole name so the column never overflows. */
    static String summary(List<ScholarshipField> fields) {
        Set<String> names = new java.util.LinkedHashSet<>();
        Set<String> seen = new HashSet<>();
        for (ScholarshipField f : fields) {
            if (seen.add(f.name().toLowerCase(Locale.ROOT))) {
                names.add(f.name());
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            int next = sb.length() == 0 ? name.length() : sb.length() + SEPARATOR.length() + name.length();
            if (next > SUMMARY_MAX) {
                break;
            }
            if (sb.length() > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(name);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /** {@code fields} when sent; otherwise the older single {@code field} as one every-level entry. */
    private static List<ScholarshipRequest.FieldInput> requested(ScholarshipRequest req) {
        if (req.fields() != null) {
            return req.fields();
        }
        String legacy = Texts.blankToNull(req.field());
        return legacy == null ? List.of() : List.of(new ScholarshipRequest.FieldInput(null, legacy));
    }
}
