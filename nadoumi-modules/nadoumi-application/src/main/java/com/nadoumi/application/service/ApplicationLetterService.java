package com.nadoumi.application.service;

import com.nadoumi.application.domain.Application;
import com.nadoumi.application.domain.ApplicationEvent;
import com.nadoumi.application.domain.ApplicationLetter;
import com.nadoumi.application.domain.ApplicationTask;
import com.nadoumi.application.domain.enums.ApplicationTaskStatus;
import com.nadoumi.application.domain.enums.LetterKind;
import com.nadoumi.application.exception.PaymentRequiredException;
import com.nadoumi.application.mapper.ApplicationEventMapper;
import com.nadoumi.application.mapper.ApplicationLetterMapper;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.application.mapper.ApplicationTaskMapper;
import com.nadoumi.application.web.response.LetterResponse;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.ProxyStream;
import com.ruoyi.common.utils.AuditActor;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * The admission letter and the JW202 form of an application. Staff upload them once admission is confirmed; a student
 * can open them only after the Nadoumi service fee is confirmed. The check is made here, on every read, so no
 * endpoint or client can reach a locked file. Files are streamed by the backend and access-logged, never linked.
 */
@Service
public class ApplicationLetterService {

    private static final String ADMISSION_CONFIRMED = "ADMISSION_CONFIRMED";
    private static final String ENROLLED = "CLOSED_SUCCESS";
    /** Titles of the checklist items the uploads complete (workflow V140). */
    private static final Map<LetterKind, String> TASK_TITLES = Map.of(
            LetterKind.ADMISSION_LETTER, "Upload the admission letter",
            LetterKind.JW202, "Upload the JW202 form");

    private final ApplicationLetterMapper letters;
    private final ApplicationMapper applications;
    private final ApplicationTaskMapper tasks;
    private final ApplicationEventMapper events;
    private final PaymentGuardProvider payments;
    private final MediaGateway media;

    public ApplicationLetterService(ApplicationLetterMapper letters, ApplicationMapper applications,
            ApplicationTaskMapper tasks, ApplicationEventMapper events, PaymentGuardProvider payments, MediaGateway media) {
        this.letters = letters;
        this.applications = applications;
        this.tasks = tasks;
        this.events = events;
        this.payments = payments;
        this.media = media;
    }

    @Transactional(readOnly = true)
    public List<LetterResponse> listForStaff(long applicationId) {
        load(applicationId);
        return letters.findCurrent(applicationId).stream().map(l -> LetterResponse.of(l, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<LetterResponse> listForStudent(long applicationId) {
        load(applicationId);
        boolean locked = !payments.isSettled(applicationId, PaymentGuardProvider.SERVICE_FEE);
        return letters.findCurrent(applicationId).stream().map(l -> LetterResponse.of(l, locked)).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public LetterResponse upload(long applicationId, LetterKind kind, MultipartFile file, long actorUserId) {
        Application application = load(applicationId);
        String status = application.getCurrentStatus();
        if (!ADMISSION_CONFIRMED.equals(status) && !ENROLLED.equals(status)) {
            throw new NadBadRequestException("the admission letter and JW202 can be uploaded once the admission is confirmed");
        }
        if (file == null || file.isEmpty()) {
            throw new NadBadRequestException("choose a file to upload");
        }
        long mediaId = media.upload(file::getInputStream, file.getOriginalFilename(), file.getContentType(), file.getSize(),
                kind.mediaCategory(), null, new MediaOwnerRef(MediaOwnerKind.APPLICATION, applicationId), actorUserId)
                .mediaId();
        ApplicationLetter row = new ApplicationLetter();
        row.setApplicationId(applicationId);
        row.setKind(kind.name());
        row.setMediaId(mediaId);
        row.setOriginalFilename(fileName(file, kind));
        row.setUploadedBy(actorUserId);
        row.setCreateBy(AuditActor.username());
        letters.insert(row);
        completeTask(applicationId, kind);
        appendEvent(applicationId, "LETTER_UPLOADED", actorUserId, kind.name());
        return LetterResponse.of(row, false);
    }

    /** Staff read any current letter. */
    @Transactional(readOnly = true)
    public ProxyStream openForStaff(long applicationId, LetterKind kind, MediaAccessLogContext context) {
        return open(applicationId, kind, context);
    }

    /** A student reads a letter only once the service fee is confirmed; until then this is a 402. */
    @Transactional(readOnly = true)
    public ProxyStream openForStudent(long applicationId, LetterKind kind, MediaAccessLogContext context) {
        load(applicationId);
        if (!payments.isSettled(applicationId, PaymentGuardProvider.SERVICE_FEE)) {
            throw new PaymentRequiredException("pay the Nadoumi service fee to receive this document");
        }
        return open(applicationId, kind, context);
    }

    private ProxyStream open(long applicationId, LetterKind kind, MediaAccessLogContext context) {
        ApplicationLetter letter = letters.findCurrentOfKind(applicationId, kind.name());
        if (letter == null) {
            throw new NadNotFoundException("this document has not been uploaded yet");
        }
        return media.openProxyStream(letter.getMediaId(), context);
    }

    private void completeTask(long applicationId, LetterKind kind) {
        for (ApplicationTask task : tasks.findByApplication(applicationId)) {
            if (TASK_TITLES.get(kind).equals(task.getTitle()) && task.getStatus() == ApplicationTaskStatus.OPEN) {
                tasks.updateStatus(task.getId(), ApplicationTaskStatus.DONE.name(), null, AuditActor.username());
            }
        }
    }

    private void appendEvent(long applicationId, String type, long actorUserId, String detail) {
        ApplicationEvent event = new ApplicationEvent();
        event.setApplicationId(applicationId);
        event.setEventType(type);
        event.setActorUserId(actorUserId);
        event.setDetailJson(com.alibaba.fastjson2.JSONObject.of("note", detail).toJSONString());
        events.insert(event);
    }

    private Application load(long applicationId) {
        Application application = applications.findById(applicationId);
        if (application == null) {
            throw new NadNotFoundException("application not found");
        }
        return application;
    }

    private static String fileName(MultipartFile file, LetterKind kind) {
        String name = file.getOriginalFilename();
        return name == null || name.isBlank() ? kind.name().toLowerCase(java.util.Locale.ROOT) : name.strip();
    }
}
