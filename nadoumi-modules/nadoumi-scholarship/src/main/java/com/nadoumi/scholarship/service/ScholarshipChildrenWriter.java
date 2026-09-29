package com.nadoumi.scholarship.service;

import com.nadoumi.common.text.Texts;
import com.nadoumi.scholarship.domain.ScholarshipAccommodation;
import com.nadoumi.scholarship.domain.ScholarshipCoverage;
import com.nadoumi.scholarship.domain.ScholarshipDocumentRequirement;
import com.nadoumi.scholarship.domain.ScholarshipEligibility;
import com.nadoumi.scholarship.domain.ScholarshipFee;
import com.nadoumi.scholarship.domain.ScholarshipIntake;
import com.nadoumi.scholarship.domain.ScholarshipLevelStipend;
import com.nadoumi.scholarship.domain.enums.FundingModel;
import com.nadoumi.scholarship.mapper.ScholarshipMapper;
import com.nadoumi.scholarship.web.request.ScholarshipRequest;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Replaces every child collection of a scholarship with the request's, inside the caller's transaction. */
@Component
public class ScholarshipChildrenWriter {

    private static final String DEFAULT_NATIONALITY_SCOPE = "ANY";
    private static final String DEFAULT_ACCOMMODATION_CURRENCY = "CNY";

    /**
     * Category applicability by funding model: a self-funded scholarship has no
     * categories at all, and a partially-funded one cannot be a CSC / CGS /
     * government "Type" scholarship (those are fully-funded schemes).
     */
    private static final Set<String> PARTIAL_EXCLUDED = Set.of("CSC", "CGS", "TYPE_A", "TYPE_B", "TYPE_C", "TYPE_D");

    private final ScholarshipMapper mapper;

    public ScholarshipChildrenWriter(ScholarshipMapper mapper) {
        this.mapper = mapper;
    }

    public void replace(Long id, ScholarshipRequest req) {
        replaceLevels(id, req);
        replaceCategories(id, req);
        replaceIntakes(id, req.intakes());
        replaceEligibility(id, req.eligibility());
        replaceFees(id, req.fees());
        replaceLevelStipends(id, req.levelStipends());
        replaceAccommodations(id, req.accommodations());
        replaceCoverage(id, req.coverage());
        replaceDocumentRequirements(id, req.documentRequirements());
    }

    private void replaceLevels(Long id, ScholarshipRequest req) {
        mapper.deleteLevels(id);
        if (req.levels() != null) {
            req.levels().stream().distinct().forEach(level -> mapper.insertLevel(id, level.name()));
        }
    }

    private void replaceCategories(Long id, ScholarshipRequest req) {
        mapper.deleteCategoryLinks(id);
        if (req.categoryCodes() != null) {
            req.categoryCodes().stream().map(code -> code.trim().toUpperCase(Locale.ROOT)).distinct()
                    .filter(code -> categoryAllowed(code, req.fundingModel()))
                    .forEach(code -> mapper.insertCategoryLink(id, code));
        }
    }

    private void replaceIntakes(Long id, List<ScholarshipRequest.IntakeInput> intakes) {
        mapper.deleteIntakes(id);
        if (intakes == null) {
            return;
        }
        int order = 0;
        for (ScholarshipRequest.IntakeInput in : intakes) {
            mapper.insertIntake(id, new ScholarshipIntake(in.term().trim(), in.applicationOpen(), in.applicationClose()), order++);
        }
    }

    private void replaceEligibility(Long id, ScholarshipRequest.EligibilityInput e) {
        mapper.deleteEligibility(id);
        if (e == null) {
            return;
        }
        mapper.insertEligibility(id, new ScholarshipEligibility(
                e.ageMin(), e.ageMax(),
                e.nationalityScope() == null ? DEFAULT_NATIONALITY_SCOPE : e.nationalityScope(),
                Texts.blankToNull(e.acceptedCountries()), e.inChina(), e.gpaMin(), e.ieltsMin(),
                e.toeflMin(), e.duolingoMin(), e.hskMin(), e.cscaMin(), Texts.blankToNull(e.notes())));
    }

    private void replaceFees(Long id, List<ScholarshipRequest.FeeInput> fees) {
        mapper.deleteFees(id);
        if (fees == null) {
            return;
        }
        int order = 0;
        for (ScholarshipRequest.FeeInput f : fees) {
            mapper.insertFee(id, new ScholarshipFee(f.kind().name(), f.amount(),
                    f.currency().toUpperCase(Locale.ROOT), Texts.blankToNull(f.note())), order++);
        }
    }

    private void replaceLevelStipends(Long id, List<ScholarshipRequest.LevelStipendInput> stipends) {
        mapper.deleteLevelStipends(id);
        if (stipends == null) {
            return;
        }
        for (ScholarshipRequest.LevelStipendInput st : stipends) {
            mapper.insertLevelStipend(id, new ScholarshipLevelStipend(st.level().name(), st.amount(),
                    st.currency().toUpperCase(Locale.ROOT), st.frequency().name(),
                    st.durationMonths(), Texts.blankToNull(st.conditions())));
        }
    }

    private void replaceAccommodations(Long id, List<ScholarshipRequest.AccommodationInput> accommodations) {
        mapper.deleteAccommodations(id);
        if (accommodations == null) {
            return;
        }
        int order = 0;
        for (ScholarshipRequest.AccommodationInput a : accommodations) {
            mapper.insertAccommodation(id, new ScholarshipAccommodation(a.roomType().name(), a.amount(),
                    a.currency() == null ? DEFAULT_ACCOMMODATION_CURRENCY : a.currency().toUpperCase(Locale.ROOT),
                    Texts.blankToNull(a.note())), order++);
        }
    }

    private void replaceCoverage(Long id, List<ScholarshipRequest.CoverageInput> coverage) {
        mapper.deleteCoverage(id);
        if (coverage == null) {
            return;
        }
        int order = 0;
        for (ScholarshipRequest.CoverageInput c : coverage) {
            mapper.insertCoverage(id, new ScholarshipCoverage(c.kind().name(), Texts.blankToNull(c.detail())), order++);
        }
    }

    private void replaceDocumentRequirements(Long id, List<ScholarshipRequest.DocumentRequirementInput> documents) {
        mapper.deleteDocumentRequirements(id);
        if (documents == null) {
            return;
        }
        int order = 0;
        for (ScholarshipRequest.DocumentRequirementInput d : documents) {
            mapper.insertDocumentRequirement(id, new ScholarshipDocumentRequirement(
                    d.docType().trim().toUpperCase(Locale.ROOT), d.mandatoryOrDefault(), Texts.blankToNull(d.note())), order++);
        }
    }

    private static boolean categoryAllowed(String code, FundingModel funding) {
        return switch (funding) {
            case SELF -> false;
            case PARTIAL -> !PARTIAL_EXCLUDED.contains(code);
            case FULLY -> true;
        };
    }
}
