package com.nadoumi.application.service;

import com.alibaba.fastjson2.JSONObject;
import com.nadoumi.applicant.service.ApplicantService;
import com.nadoumi.application.domain.Application;
import com.nadoumi.application.domain.ApplicationSnapshot;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.application.mapper.ApplicationSnapshotMapper;
import org.springframework.stereotype.Component;

@Component
public class ApplicationSnapshotWriter {

    private static final String PROFILE = "PROFILE";
    private static final String REQUIREMENTS = "REQUIREMENTS";

    private final ApplicationSnapshotMapper snapshotMapper;
    private final ApplicationMapper applicationMapper;
    private final ApplicantService applicantService;

    public ApplicationSnapshotWriter(ApplicationSnapshotMapper snapshotMapper, ApplicationMapper applicationMapper,
            ApplicantService applicantService) {
        this.snapshotMapper = snapshotMapper;
        this.applicationMapper = applicationMapper;
        this.applicantService = applicantService;
    }

    /** DA4: PROFILE + REQUIREMENTS, written once at submit, never touched again. */
    public void writeSubmitSnapshots(long applicationId, long applicantId) {
        JSONObject profile = new JSONObject();
        profile.put("applicant", applicantService.get(applicantId));
        insert(applicationId, PROFILE, profile);

        Application application = applicationMapper.findById(applicationId);
        JSONObject requirements = new JSONObject();
        requirements.put("applicationType", application.getApplicationType());
        requirements.put("programId", application.getProgramId());
        requirements.put("scholarshipId", application.getScholarshipId());
        requirements.put("intakeId", application.getIntakeId());
        insert(applicationId, REQUIREMENTS, requirements);
    }

    private void insert(long applicationId, String kind, JSONObject payload) {
        ApplicationSnapshot snapshot = new ApplicationSnapshot();
        snapshot.setApplicationId(applicationId);
        snapshot.setKind(kind);
        snapshot.setPayloadJson(payload.toJSONString());
        snapshotMapper.insert(snapshot);
    }
}
