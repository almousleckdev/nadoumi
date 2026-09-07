package com.nadoumi.program.web.response;

import com.nadoumi.program.domain.Program;
import com.nadoumi.program.domain.ProgramIntake;
import com.nadoumi.program.domain.ProgramMajor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Full staff-side programme view. Public catalog fields only -- no commercial data. */
public record ProgramResponse(
        Long id,
        Long universityId,
        String universityName,
        String universitySlug,
        String name,
        String slug,
        String nameCn,
        String programType,
        List<String> levels,
        String field,
        String termLength,
        String teachingLanguage,
        Integer durationMonths,
        BigDecimal tuitionAmount,
        String tuitionCurrency,
        BigDecimal tuitionAmountUsd,
        String summary,
        Long imageMediaId,
        String imageUrl,
        boolean featured,
        boolean hot,
        String status,
        String publishStatus,
        String remark,
        String createdAt,
        String updatedAt,
        List<Major> majors,
        List<Intake> intakes) {

    public record Major(Long id, String name, String nameCn, Long departmentId, String departmentName, String level) {
        static Major of(ProgramMajor m) {
            return new Major(m.getId(), m.getName(), m.getNameCn(), m.getDepartmentId(), m.getDepartmentName(), m.getLevel());
        }
    }

    public record Intake(Long id, String term, LocalDate applicationOpen, LocalDate applicationClose) {
        static Intake of(ProgramIntake i) {
            return new Intake(i.getId(), i.getTerm(), i.getApplicationOpen(), i.getApplicationClose());
        }
    }

    public static ProgramResponse of(Program p) {
        return of(p, null, null);
    }

    public static ProgramResponse of(Program p, String imageUrl) {
        return of(p, imageUrl, null);
    }

    /** {@code tuitionAmountUsd} is computed by the service from the editable FX rate. */
    public static ProgramResponse of(Program p, String imageUrl, BigDecimal tuitionAmountUsd) {
        return new ProgramResponse(
                p.getId(), p.getUniversityId(), p.getUniversityName(), p.getUniversitySlug(),
                p.getName(), p.getSlug(), p.getNameCn(),
                p.getProgramType() == null ? null : p.getProgramType().name(),
                p.getLevels() == null ? java.util.List.of() : java.util.List.copyOf(p.getLevels()),
                p.getField(),
                p.getTermLength(),
                p.getTeachingLanguage() == null ? null : p.getTeachingLanguage().name(),
                p.getDurationMonths(), p.getTuitionAmount(), p.getTuitionCurrency(), tuitionAmountUsd,
                p.getSummary(), p.getImageMediaId(), imageUrl, p.isFeatured(), p.isHot(),
                p.getStatus() == null ? null : p.getStatus().name(),
                p.getPublishStatus() == null ? null : p.getPublishStatus().name(),
                p.getRemark(), str(p.getCreateTime()), str(p.getUpdateTime()),
                p.getMajors().stream().map(Major::of).toList(),
                p.getIntakes().stream().map(Intake::of).toList());
    }

    private static String str(Object v) {
        return v == null ? null : v.toString();
    }
}
