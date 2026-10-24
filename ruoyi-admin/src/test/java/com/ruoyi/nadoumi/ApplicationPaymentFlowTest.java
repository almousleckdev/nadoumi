package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.nadoumi.identity.service.mail.LoggingMailSender;
import com.nadoumi.payment.service.PaymentDocumentWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

/**
 * The Nadoumi application fee against the real context and MySQL: a draft stays saved but cannot be submitted
 * until the fee is confirmed; the QR and the 15-minute window; staff verification (once, never twice); and
 * who may see what.
 */
class ApplicationPaymentFlowTest extends AbstractStudentIntegrationTest {

    private static final String FEE_KEY = CacheConstants.SYS_CONFIG_KEY + "nadoumi.application.fee";
    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89
    };

    @Autowired private RedisCache redisCache;
    @Autowired private PaymentDocumentWorker documentsWorker;
    @Autowired private LoggingMailSender mailbox;
    /** Applicants whose profile is complete; ids are never reused, so this is safe across tests. */
    private final java.util.Set<Long> profileDone = new java.util.HashSet<>();

    private String staffAuth;
    private long programId;
    private long intakeId;

    @BeforeEach
    void setUpCatalog() throws Exception {
        redisCache.setCacheObject(FEE_KEY, "300");
        jdbc.update("update nad_wf_definition set status = 'ACTIVE' where code in ('PROGRAM_ONLY_V1', 'SCHOLARSHIP_ONLY_V1')");
        createStaff("pay_ops", "nadoumi_super_admin");
        staffAuth = bearer(staffToken("pay_ops"));
        String uni = mvc.perform(post("/api/staff/universities").header("Authorization", staffAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Pay University\",\"country\":\"CN\",\"type\":\"PUBLIC\",\"status\":\"ACTIVE\",\"publishStatus\":\"PUBLISHED\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long universityId = ((Number) JsonPath.read(uni, "$.id")).longValue();
        String program = mvc.perform(post("/api/staff/programs").header("Authorization", staffAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"universityId\":" + universityId + ",\"name\":\"Pay MBA\",\"programType\":\"DEGREE\","
                                + "\"levels\":[\"MASTER\"],\"status\":\"ACTIVE\",\"publishStatus\":\"PUBLISHED\","
                                + "\"intakes\":[{\"term\":\"AUTUMN_SEPTEMBER\"}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        programId = ((Number) JsonPath.read(program, "$.id")).longValue();
        intakeId = ((Number) JsonPath.read(program, "$.intakes[0].id")).longValue();
    }

    private long draftFor(Student s) throws Exception {
        String res = mvc.perform(post("/api/public/applications").header("Authorization", bearer(s.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicantId\":" + s.applicantId() + ",\"applicationType\":\"PROGRAM_ONLY\","
                                + "\"programId\":" + programId + ",\"intakeId\":" + intakeId + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    private String url(long applicationId, String suffix) {
        return "/api/student/applications/" + applicationId + "/payment" + suffix;
    }

    private ResultActions submitApplication(Student s, long applicationId) throws Exception {
        return mvc.perform(post("/api/student/applications/" + applicationId + "/submit")
                .header("Authorization", bearer(s.token())));
    }

    private ResultActions openSession(Student s, long applicationId) throws Exception {
        return mvc.perform(post(url(applicationId, "/session")).header("Authorization", bearer(s.token())));
    }

    private ResultActions reportPayment(Student s, long applicationId, String txn) throws Exception {
        return mvc.perform(multipart(url(applicationId, "/proof"))
                .file(new MockMultipartFile("file", "proof.png", "image/png", PNG))
                .param("transactionNo", txn).header("Authorization", bearer(s.token())));
    }

    private long paymentId(long applicationId) {
        return jdbc.queryForObject("select id from nad_application_payment where application_id = ? order by id desc limit 1",
                Long.class, applicationId);
    }

    @Test
    void shouldKeepADraftSaved_butRefuseSubmission_untilTheFeeIsConfirmed() throws Exception {
        Student s = register("pay", "draft");
        long app = draftFor(s);

        submitApplication(s, app).andExpect(status().isPaymentRequired());

        mvc.perform(get("/api/student/applications/" + app).header("Authorization", bearer(s.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentStatus").value("DRAFT"));
        mvc.perform(get(url(app, "")).header("Authorization", bearer(s.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("NOT_STARTED"))
                .andExpect(jsonPath("$.required").value(true))
                .andExpect(jsonPath("$.canStart").value(true))
                .andExpect(jsonPath("$.amount").value(300));
    }

    @Test
    void shouldOpenAFifteenMinuteSession_andServeTheQrOnlyWhileItIsOpen() throws Exception {
        Student s = register("pay", "qr");
        long app = draftFor(s);

        mvc.perform(get(url(app, "/qr")).header("Authorization", bearer(s.token())))
                .andExpect(status().isConflict()); // no session yet: the QR is not a public file

        openSession(s, app).andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PENDING"))
                .andExpect(jsonPath("$.reference").value(org.hamcrest.Matchers.startsWith("NAD-")))
                .andExpect(jsonPath("$.secondsLeft").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.greaterThan(890), org.hamcrest.Matchers.lessThanOrEqualTo(900))));

        byte[] qr = mvc.perform(get(url(app, "/qr")).header("Authorization", bearer(s.token())))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(qr).hasSizeGreaterThan(10_000);
        assertThat(qr[1]).isEqualTo((byte) 'P'); // PNG signature

        // opening again returns the same session, not a second one
        openSession(s, app).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from nad_application_payment where application_id = ?", Integer.class, app))
                .isEqualTo(1);
    }

    @Test
    void shouldRefuseTheQrAndTheProof_afterTheFifteenMinuteWindowCloses() throws Exception {
        Student s = register("pay", "late");
        long app = draftFor(s);
        openSession(s, app).andExpect(status().isOk());
        jdbc.update("update nad_application_payment set expires_at = ? where application_id = ?",
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(1), app);

        mvc.perform(get(url(app, "/qr")).header("Authorization", bearer(s.token()))).andExpect(status().isConflict());
        reportPayment(s, app, "BANK1234567").andExpect(status().isConflict());
        mvc.perform(get(url(app, "")).header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.state").value("EXPIRED")).andExpect(jsonPath("$.canStart").value(true));

        openSession(s, app).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("PENDING"));
        submitApplication(s, app).andExpect(status().isPaymentRequired());
    }

    @Test
    void shouldAllowSubmission_onceThePaymentIsReported_andThenLetStaffConfirm() throws Exception {
        Student s = register("pay", "ok");
        long app = draftFor(s);
        openSession(s, app).andExpect(status().isOk());
        sign(s);
        submitApplication(s, app).andExpect(status().isPaymentRequired()); // nothing reported yet

        reportPayment(s, app, "bank1234567").andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PROOF_SUBMITTED")).andExpect(jsonPath("$.settled").value(true));
        long id = paymentId(app);
        // the application is still a draft: staff do not receive the payment yet
        mvc.perform(get("/api/staff/payments").param("q", "BANK1234567").header("Authorization", staffAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));

        submitApplication(s, app).andExpect(status().isOk());

        mvc.perform(get("/api/staff/payments").param("status", "PROOF_SUBMITTED").param("q", "BANK1234567")
                        .header("Authorization", staffAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reference").isNotEmpty())
                .andExpect(jsonPath("$.content[0].studentTxnNo").value("BANK1234567"))
                .andExpect(jsonPath("$.content[0].hasProof").value(true))
                .andExpect(jsonPath("$.content[0].proofMediaId").doesNotExist());
        mvc.perform(get("/api/staff/payments/" + id + "/proof").header("Authorization", staffAuth))
                .andExpect(status().isOk());

        mvc.perform(post("/api/staff/payments/" + id + "/confirm").header("Authorization", staffAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(get(url(app, "")).header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.state").value("CONFIRMED")).andExpect(jsonPath("$.settled").value(true));
        assertThat(jdbc.queryForObject("select count(*) from nad_revenue where source = 'APPLICATION_FEE' and related_id = ?",
                Integer.class, app)).isEqualTo(1);
    }

    @Test
    void shouldHideOpenCancelledAndExpiredPaymentsAndDraftApplications_fromStaff() throws Exception {
        Student s = register("pay", "inbox");
        long app = draftFor(s);
        openSession(s, app).andExpect(status().isOk());
        String ref = jdbc.queryForObject("select reference from nad_application where id = ?", String.class, app);

        mvc.perform(get("/api/staff/payments").param("q", ref).header("Authorization", staffAuth))
                .andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/staff/applications").param("q", ref).header("Authorization", staffAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));

        mvc.perform(post(url(app, "/cancel")).header("Authorization", bearer(s.token()))).andExpect(status().isOk());
        mvc.perform(get("/api/staff/payments").param("q", ref).header("Authorization", staffAuth))
                .andExpect(jsonPath("$.content.length()").value(0));
        jdbc.update("update nad_application set current_status = 'CLOSED_WITHDRAWN' where id = ?", app);
        mvc.perform(get("/api/staff/applications").param("q", ref).header("Authorization", staffAuth))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void shouldStopTheClock_whenTheStudentStartsReportingThePayment() throws Exception {
        Student s = register("pay", "hold");
        long app = draftFor(s);
        openSession(s, app).andExpect(status().isOk());

        mvc.perform(post(url(app, "/hold")).header("Authorization", bearer(s.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("PENDING"))
                .andExpect(jsonPath("$.secondsLeft").value(org.hamcrest.Matchers.greaterThan(20 * 60)));
        reportPayment(s, app, "BANKHOLD123").andExpect(status().isOk());
    }

    @Test
    void shouldLetStaffAssignChangeStatusAndDownloadTheForm_andNeverLetAStudentWithdraw() throws Exception {
        Student s = register("pay", "manage");
        long app = draftFor(s);
        openSession(s, app).andExpect(status().isOk());
        sign(s);
        reportPayment(s, app, "BANKMANAGE1").andExpect(status().isOk());
        submitApplication(s, app).andExpect(status().isOk());

        // the fee is not refundable: a student cannot withdraw a submitted application
        mvc.perform(post("/api/student/applications/" + app + "/transitions/withdraw").header("Authorization", bearer(s.token()))
                        .contentType("application/json").content("{\"reason\":\"changed my mind\"}"))
                .andExpect(status().isForbidden());

        // staff see who applied and what for, with names instead of ids
        mvc.perform(get("/api/staff/applications/" + app).header("Authorization", staffAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicant.name").exists())
                .andExpect(jsonPath("$.opportunity.title").isNotEmpty())
                .andExpect(jsonPath("$.history[0].toStageName").value("Submitted"));

        // assignment: pick from the staff list, never an arbitrary id
        long staffId = jdbc.queryForObject("select user_id from sys_user where user_name = 'pay_ops'", Long.class);
        mvc.perform(get("/api/staff/applications/assignees").header("Authorization", staffAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.userId == " + staffId + ")]").isNotEmpty());
        long studentUserId = jdbc.queryForObject("select user_id from nad_user_applicant_access where applicant_id = ? limit 1", Long.class, s.applicantId());
        mvc.perform(post("/api/staff/applications/" + app + "/assign").header("Authorization", staffAuth)
                        .contentType("application/json").content("{\"assigneeUserId\":" + studentUserId + "}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/staff/applications/" + app + "/assign").header("Authorization", staffAuth)
                        .contentType("application/json").content("{\"assigneeUserId\":" + staffId + "}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/staff/applications/" + app).header("Authorization", staffAuth))
                .andExpect(jsonPath("$.assigneeName").isNotEmpty());

        // changing the status: the available steps, then one step, which e-mails the student
        mvc.perform(get("/api/staff/applications/" + app + "/next-steps").header("Authorization", staffAuth))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].label").isNotEmpty());
        int version = jdbc.queryForObject("select version from nad_application where id = ?", Integer.class, app);
        mvc.perform(post("/api/staff/applications/" + app + "/advance").header("Authorization", staffAuth)
                        .contentType("application/json").content("{\"code\":\"claim\",\"note\":\"Looks complete\",\"version\":" + version + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentStageName").value("Eligibility review"));
        assertThat(jdbc.queryForObject("select count(*) from nad_outbox_event where type = 'ApplicationStatusChanged' and aggregate_id = ?",
                Integer.class, app)).isEqualTo(1);

        // the submitted form is downloadable by staff, with the payment line
        mvc.perform(get("/api/staff/applications/" + app + "/form").header("Authorization", staffAuth))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
    }

    @Test
    void shouldRefuseASecondConfirmation_withoutBookingTheFeeTwice() throws Exception {
        Student s = register("pay", "twice");
        long app = draftFor(s);
        openSession(s, app);
        reportPayment(s, app, "BANK7654321").andExpect(status().isOk());
        long id = paymentId(app);

        mvc.perform(post("/api/staff/payments/" + id + "/confirm").header("Authorization", staffAuth)).andExpect(status().isOk());
        mvc.perform(post("/api/staff/payments/" + id + "/confirm").header("Authorization", staffAuth)).andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("select count(*) from nad_revenue where related_id = ?", Integer.class, app)).isEqualTo(1);
    }

    @Test
    void shouldLetStaffRejectWithAReason_andTheStudentStartAgain() throws Exception {
        Student s = register("pay", "reject");
        long app = draftFor(s);
        openSession(s, app);
        reportPayment(s, app, "BANK1111111").andExpect(status().isOk());
        long id = paymentId(app);

        mvc.perform(post("/api/staff/payments/" + id + "/reject").header("Authorization", staffAuth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Amount does not match\"}"))
                .andExpect(status().isOk());

        mvc.perform(get(url(app, "")).header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.state").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Amount does not match"))
                .andExpect(jsonPath("$.canStart").value(true));
        openSession(s, app).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("PENDING"));
        // the rejected transaction number can be reported again for a corrected payment
        reportPayment(s, app, "BANK1111111").andExpect(status().isOk());
    }

    @Test
    void shouldRefuseTheSameTransactionNumberOnASecondApplication() throws Exception {
        Student a = register("pay", "dup1");
        Student b = register("pay", "dup2");
        long appA = draftFor(a);
        long appB = draftFor(b);
        openSession(a, appA);
        openSession(b, appB);
        reportPayment(a, appA, "BANK2222222").andExpect(status().isOk());

        reportPayment(b, appB, "BANK2222222").andExpect(status().isConflict());
    }

    @Test
    void shouldKeepAnotherStudent_outOfThePayment() throws Exception {
        Student owner = register("pay", "owner");
        Student intruder = register("pay", "intruder");
        long app = draftFor(owner);
        openSession(owner, app);

        mvc.perform(get(url(app, "")).header("Authorization", bearer(intruder.token()))).andExpect(status().isForbidden());
        mvc.perform(get(url(app, "/qr")).header("Authorization", bearer(intruder.token()))).andExpect(status().isForbidden());
        openSession(intruder, app).andExpect(status().isForbidden());
        reportPayment(intruder, app, "BANK3333333").andExpect(status().isForbidden());
    }

    @Test
    void shouldKeepStudentsOutOfTheStaffPaymentEndpoints() throws Exception {
        Student s = register("pay", "nostaff");
        long app = draftFor(s);
        openSession(s, app);

        mvc.perform(get("/api/staff/payments").header("Authorization", bearer(s.token()))).andExpect(status().isForbidden());
        mvc.perform(post("/api/staff/payments/" + paymentId(app) + "/confirm").header("Authorization", bearer(s.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldNotRequireAPayment_whenNoFeeIsConfigured() throws Exception {
        redisCache.setCacheObject(FEE_KEY, "0");
        Student s = register("pay", "free");
        long app = draftFor(s);

        mvc.perform(get(url(app, "")).header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.state").value("NOT_REQUIRED")).andExpect(jsonPath("$.settled").value(true));
        submitApplication(s, app).andExpect(status().isConflict()); // not signed yet
        sign(s);
        submitApplication(s, app).andExpect(status().isOk());
    }

    /** A drawn-looking signature PNG, as the apply screen would upload. */
    static byte[] signaturePng() throws java.io.IOException {
        var image = new java.awt.image.BufferedImage(400, 120, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        g.setColor(java.awt.Color.BLACK);
        g.setStroke(new java.awt.BasicStroke(5f));
        g.drawArc(20, 20, 160, 90, 30, 280);
        g.drawLine(180, 90, 380, 30);
        g.dispose();
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /** Everything the submit gate asks of an applicant besides the fee, the documents and the signature. */
    private void completeProfile(Student s) throws Exception {
        jdbc.update("update nad_applicant set onboarded_at = now() where id = ?", s.applicantId());
        String url = s.applicantUrl();
        String person = "{\"fullName\":\"Ibrahima Diallo\",\"relationship\":\"Father\",\"gender\":\"MALE\",\"phone\":\"+224611111111\"}";
        send(url + "/personal-details", "PUT", s, "{\"maritalStatus\":\"SINGLE\",\"religion\":\"Muslim\","
                + "\"placeOfBirth\":\"Conakry\",\"permanentAddress\":\"12 Rue du Port\"}");
        send(url + "/people/supporter", "PUT", s, person);
        send(url + "/people/china-emergency", "PUT", s, person);
        send(url + "/family", "POST", s, "{\"relation\":\"FATHER\",\"fullName\":\"Ibrahima Diallo\"}");
        send(url + "/family", "POST", s, "{\"relation\":\"MOTHER\",\"fullName\":\"Fatoumata Diallo\"}");
    }

    private void send(String url, String method, Student s, String json) throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                        org.springframework.http.HttpMethod.valueOf(method), url)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(json)
                        .header("Authorization", bearer(s.token())))
                .andExpect(status().is2xxSuccessful());
    }

    private void sign(Student s) throws Exception {
        if (!profileDone.contains(s.applicantId())) {
            completeProfile(s);
            profileDone.add(s.applicantId());
        }
        mvc.perform(multipart(s.applicantUrl() + "/signature")
                        .file(new MockMultipartFile("file", "signature.png", "image/png", signaturePng()))
                        .header("Authorization", bearer(s.token())))
                .andExpect(status().isOk());
    }

    private void payAndConfirm(Student s, long app, String txn) throws Exception {
        sign(s);
        openSession(s, app);
        reportPayment(s, app, txn).andExpect(status().isOk());
        mvc.perform(post("/api/staff/payments/" + paymentId(app) + "/confirm").header("Authorization", staffAuth))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRenderTheReceiptAndForm_andEmailTheReceiptToTheStudent_afterConfirmation() throws Exception {
        Student s = register("pay", "docs");
        long app = draftFor(s);
        mvc.perform(get(url(app, "/documents")).header("Authorization", bearer(s.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());

        payAndConfirm(s, app, "BANK4444444");
        documentsWorker.runOnce();

        mvc.perform(get(url(app, "/documents")).header("Authorization", bearer(s.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.kind=='RECEIPT')].number").value(org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.startsWith("NAD-RCT-"))))
                .andExpect(jsonPath("$[0].mediaId").doesNotExist());
        byte[] receipt = mvc.perform(get(url(app, "/documents/RECEIPT")).header("Authorization", bearer(s.token())))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(receipt, 0, 4)).isEqualTo("%PDF");
        mvc.perform(get(url(app, "/documents/application_form")).header("Authorization", bearer(s.token())))
                .andExpect(status().isOk());

        var mail = mailbox.last(s.email());
        assertThat(mail).isPresent();
        assertThat(mail.get().attachmentNames()).singleElement().asString().startsWith("NAD-RCT-");
        assertThat(mail.get().subject()).startsWith("Your Nadoumi payment receipt");

        documentsWorker.runOnce(); // a second pass sends nothing more
        assertThat(jdbc.queryForObject("select count(*) from nad_payment_document where payment_id = ?", Integer.class,
                paymentId(app))).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from nad_payment_document_delivery", Integer.class)).isEqualTo(1);
    }

    @Test
    void shouldEmailTheSubmittedFormToTheAdministratorsOnly_andNotTheStaff() throws Exception {
        createStaff("pay_case", "case_officer");
        Student s = register("pay", "submit");
        long app = draftFor(s);
        payAndConfirm(s, app, "BANK5555555");
        documentsWorker.runOnce();
        assertThat(mailbox.last("pay_ops@staff.test").filter(m -> m.subject().contains("APP-" + app + ":")))
                .isEmpty(); // nothing about this application reaches the administrators before submit

        submitApplication(s, app).andExpect(status().isOk());
        documentsWorker.runOnce();

        var adminMail = mailbox.last("pay_ops@staff.test");
        assertThat(adminMail).isPresent();
        assertThat(adminMail.get().attachmentNames()).singleElement().asString().startsWith("NAD-APP-");
        String reference = jdbc.queryForObject("select reference from nad_application where id = ?", String.class, app);
        assertThat(adminMail.get().subject()).startsWith("[Nadoumi] New application " + reference);
        assertThat(mailbox.last("pay_case@staff.test").filter(m -> m.subject().contains(reference + ":"))).isEmpty();
        assertThat(jdbc.queryForObject("select max(version) from nad_payment_document where kind = 'APPLICATION_FORM' "
                + "and application_id = ?", Integer.class, app)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from nad_payment_document where delivered_at is null and application_id = ?",
                Integer.class, app)).isZero();
    }

    @Test
    void shouldKeepTheDocumentsPrivate_toTheStudentWhoMayViewTheApplication() throws Exception {
        Student owner = register("pay", "docowner");
        Student intruder = register("pay", "docintruder");
        long app = draftFor(owner);
        mvc.perform(get(url(app, "/documents/RECEIPT")).header("Authorization", bearer(owner.token())))
                .andExpect(status().isNotFound()); // nothing exists before the payment is confirmed
        payAndConfirm(owner, app, "BANK6666666");
        documentsWorker.runOnce();

        mvc.perform(get(url(app, "/documents")).header("Authorization", bearer(intruder.token()))).andExpect(status().isForbidden());
        mvc.perform(get(url(app, "/documents/RECEIPT")).header("Authorization", bearer(intruder.token()))).andExpect(status().isForbidden());
        mvc.perform(get(url(app, "/documents/NOPE")).header("Authorization", bearer(owner.token()))).andExpect(status().isNotFound());
    }

    private long publishedScholarship() throws Exception {
        String created = mvc.perform(post("/api/staff/scholarships").header("Authorization", staffAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Pay Standalone Scholarship\",\"country\":\"cn\",\"fundingModel\":\"FULLY\","
                                + "\"status\":\"ACTIVE\",\"publishStatus\":\"PUBLISHED\",\"levels\":[\"MASTER\"]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(created, "$.view.id")).longValue();
    }

    private ResultActions startScholarshipOnly(Student s, long scholarshipId) throws Exception {
        return mvc.perform(post("/api/public/applications").header("Authorization", bearer(s.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"applicantId\":" + s.applicantId() + ",\"applicationType\":\"SCHOLARSHIP_ONLY\","
                        + "\"scholarshipId\":" + scholarshipId + "}"));
    }

    @Test
    void shouldLetAStudentApplyForAStandaloneScholarship_payForIt_andSubmit_withoutAnyUniversityShowing() throws Exception {
        Student s = register("pay", "schol");
        long scholarship = publishedScholarship();

        String created = startScholarshipOnly(s, scholarship).andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationType").value("SCHOLARSHIP_ONLY"))
                .andExpect(jsonPath("$.programId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long app = ((Number) JsonPath.read(created, "$.id")).longValue();

        submitApplication(s, app).andExpect(status().isPaymentRequired());
        payAndConfirm(s, app, "BANK7777777");
        documentsWorker.runOnce();
        submitApplication(s, app).andExpect(status().isOk());
        documentsWorker.runOnce();

        var adminMail = mailbox.last("pay_ops@staff.test");
        assertThat(adminMail).isPresent();
        assertThat(adminMail.get().subject()).contains("Pay Standalone Scholarship");
        assertThat(adminMail.get().subject() + adminMail.get().body()).doesNotContain("Pay University");
        mvc.perform(get(url(app, "/documents/RECEIPT")).header("Authorization", bearer(s.token())))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRefuseAScholarshipOnlyApplication_thatNamesAProgramme() throws Exception {
        Student s = register("pay", "schol2");
        long scholarship = publishedScholarship();

        mvc.perform(post("/api/public/applications").header("Authorization", bearer(s.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicantId\":" + s.applicantId() + ",\"applicationType\":\"SCHOLARSHIP_ONLY\","
                                + "\"programId\":" + programId + ",\"scholarshipId\":" + scholarship + "}"))
                .andExpect(status().isBadRequest());
        startScholarshipOnly(s, 99999L).andExpect(status().isNotFound());
    }

    @Test
    void shouldLetAStudentStartAnApplicationOnTheStudentEndpoint_butOnlyForAnApplicantTheyMayActFor() throws Exception {
        Student owner = register("pay", "startown");
        Student other = register("pay", "startother");

        mvc.perform(post("/api/student/applications").header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicantId\":" + owner.applicantId() + ",\"applicationType\":\"PROGRAM_ONLY\","
                                + "\"programId\":" + programId + ",\"intakeId\":" + intakeId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicantId").value(owner.applicantId()))
                .andExpect(jsonPath("$.currentStatus").value("DRAFT"));

        mvc.perform(post("/api/student/applications").header("Authorization", bearer(other.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicantId\":" + owner.applicantId() + ",\"applicationType\":\"PROGRAM_ONLY\","
                                + "\"programId\":" + programId + ",\"intakeId\":" + intakeId + "}"))
                .andExpect(status().isForbidden());
    }

    private void requireDocuments(String json) throws Exception {
        mvc.perform(put("/api/staff/document-requirements/programs/" + programId).header("Authorization", staffAuth)
                .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk());
    }

    @Test
    void shouldPreviewTheApplicationFormBeforePaying_forTheStudentOnly() throws Exception {
        Student s = register("pay", "prev");
        Student stranger = register("pay", "prev2");
        long app = draftFor(s);

        byte[] pdf = mvc.perform(get("/api/student/applications/" + app + "/form-preview").header("Authorization", bearer(s.token())))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        mvc.perform(get("/api/student/applications/" + app + "/form-preview").header("Authorization", bearer(stranger.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRequireTheFilledInApplicationForm_whenTheProgrammeHasAFormToFillIn() throws Exception {
        addProgramTemplate("Application form");
        Student s = register("pay", "formreq");
        long app = draftFor(s);

        mvc.perform(get("/api/student/applications/" + app + "/documents/checklist").header("Authorization", bearer(s.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].docType").value("APPLICATION_FORM"))
                .andExpect(jsonPath("$[0].mandatory").value(true))
                .andExpect(jsonPath("$[0].status").value("MISSING"));
    }

    @Test
    void shouldNumberApplicationsNadYearAndTheYearsRunningNumber() throws Exception {
        Student s = register("pay", "ref1");
        long first = draftFor(s);
        String one = JsonPath.read(mvc.perform(get("/api/student/applications/" + first).header("Authorization", bearer(s.token())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.reference");
        assertThat(one).matches("NAD\\d{4}\\d{3,}");

        // a second application in the same year continues the year's numbering
        long second = draftFor(register("pay", "ref2"));
        String two = JsonPath.read(mvc.perform(get("/api/staff/applications/" + second).header("Authorization", staffAuth))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.application.reference");
        assertThat(Long.parseLong(two.substring(3))).isEqualTo(Long.parseLong(one.substring(3)) + 1);
    }

    @Test
    void shouldLetStaffFindAStudentToChatWith_byTheApplicationReference() throws Exception {
        Student s = register("pay", "chatref");
        long app = draftFor(s);
        String reference = jdbc.queryForObject("select reference from nad_application where id = ?", String.class, app);

        mvc.perform(get("/api/staff/chat/students").param("q", reference).header("Authorization", staffAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matchedApplicationId").value(app));
    }

    @Test
    void shouldShowAnonymousVisitorsTheSameDocumentsStudentsAreAskedFor() throws Exception {
        requireDocuments("{\"requirements\":[{\"docType\":\"PASSPORT\",\"mandatory\":true},"
                + "{\"docType\":\"PHOTO\",\"mandatory\":false,\"notes\":\"white background\"}]}");

        mvc.perform(get("/api/public/document-requirements/programs/" + programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.docType=='PASSPORT')].mandatory").value(true))
                .andExpect(jsonPath("$[?(@.docType=='PHOTO')].notes").value("white background"));
    }

    private ResultActions uploadDocument(Student s, long app, String docType) throws Exception {
        return mvc.perform(multipart("/api/student/documents")
                .file(new MockMultipartFile("file", "scan.png", "image/png", PNG))
                .param("applicantId", String.valueOf(s.applicantId())).param("docType", docType)
                .param("applicationId", String.valueOf(app)).header("Authorization", bearer(s.token())));
    }

    @Test
    void shouldHoldBackSubmission_untilEveryMandatoryDocumentIsUploaded() throws Exception {
        requireDocuments("{\"requirements\":[{\"docType\":\"PASSPORT\",\"mandatory\":true,\"notes\":\"Photo page\"},"
                + "{\"docType\":\"CV\",\"mandatory\":false}]}");
        Student s = register("pay", "docs1");
        long app = draftFor(s);
        mvc.perform(get("/api/student/applications/" + app + "/documents/checklist").header("Authorization", bearer(s.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].docType").value("PASSPORT"))
                .andExpect(jsonPath("$[0].mandatory").value(true))
                .andExpect(jsonPath("$[0].note").value("Photo page"))
                .andExpect(jsonPath("$[0].status").value("MISSING"))
                .andExpect(jsonPath("$[1].mandatory").value(false));

        payAndConfirm(s, app, "BANK8888888");
        submitApplication(s, app).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("PASSPORT")));

        uploadDocument(s, app, "PASSPORT").andExpect(status().isCreated());
        mvc.perform(get("/api/student/applications/" + app + "/documents/checklist").header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$[0].status").value("SUBMITTED")).andExpect(jsonPath("$[0].documentId").isNumber());
        submitApplication(s, app).andExpect(status().isOk()); // the optional CV does not hold it back
    }

    @Test
    void shouldUseTheScholarshipsOwnRequirements_forAScholarshipOnlyApplication() throws Exception {
        Student s = register("pay", "docs2");
        String created = mvc.perform(post("/api/staff/scholarships").header("Authorization", staffAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Pay Documents Scholarship\",\"country\":\"cn\",\"fundingModel\":\"FULLY\","
                                + "\"status\":\"ACTIVE\",\"publishStatus\":\"PUBLISHED\",\"levels\":[\"MASTER\"],"
                                + "\"documentRequirements\":[{\"docType\":\"FINANCIAL_PROOF\",\"mandatory\":true}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long scholarship = ((Number) JsonPath.read(created, "$.view.id")).longValue();
        String app = startScholarshipOnly(s, scholarship).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(app, "$.id")).longValue();

        mvc.perform(get("/api/student/applications/" + id + "/documents/checklist").header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].docType").value("FINANCIAL_PROOF"));
    }

    @Test
    void shouldKeepTheChecklistAndTheRequirementsEditor_toTheRightPeople() throws Exception {
        Student owner = register("pay", "docs3");
        Student intruder = register("pay", "docs4");
        long app = draftFor(owner);

        mvc.perform(get("/api/student/applications/" + app + "/documents/checklist").header("Authorization", bearer(intruder.token())))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/staff/document-requirements/programs/" + programId).header("Authorization", bearer(owner.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"requirements\":[]}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/staff/document-requirements/programs/" + programId).header("Authorization", staffAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirements\":[{\"docType\":\"CV\",\"mandatory\":true},{\"docType\":\"CV\",\"mandatory\":false}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/staff/document-requirements/programs/" + programId).header("Authorization", staffAuth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"requirements\":[{\"docType\":\"not valid\",\"mandatory\":true}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldEmailTheReceiptAgain_butNotMoreOftenThanTheCooldownAllows() throws Exception {
        Student s = register("pay", "resend");
        long app = draftFor(s);
        payAndConfirm(s, app, "BANK9999991");
        documentsWorker.runOnce();
        var first = mailbox.last(s.email()).orElseThrow();

        mvc.perform(post(url(app, "/documents/RECEIPT/resend")).header("Authorization", bearer(s.token())))
                .andExpect(status().isTooManyRequests());

        jdbc.update("update nad_payment_document_delivery set sent_at = ? where document_id = "
                + "(select id from nad_payment_document where application_id = ? and kind = 'RECEIPT')",
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(10), app);
        mvc.perform(post(url(app, "/documents/RECEIPT/resend")).header("Authorization", bearer(s.token())))
                .andExpect(status().isAccepted());
        documentsWorker.runOnce();

        assertThat(mailbox.last(s.email()).orElseThrow()).isNotSameAs(first);
        assertThat(jdbc.queryForObject("select count(*) from nad_payment_document_delivery where sent_at is not null "
                + "and document_id = (select id from nad_payment_document where application_id = ? and kind = 'RECEIPT')",
                Integer.class, app)).isEqualTo(1);
    }

    @Test
    void shouldNotLetAnotherStudentResendTheReceipt() throws Exception {
        Student owner = register("pay", "resown");
        Student intruder = register("pay", "resint");
        long app = draftFor(owner);
        payAndConfirm(owner, app, "BANK9999992");
        documentsWorker.runOnce();

        mvc.perform(post(url(app, "/documents/RECEIPT/resend")).header("Authorization", bearer(intruder.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldNotLetAStudentSkipThePaymentGate_throughTheGenericTransitionEndpoint() throws Exception {
        Student s = register("pay", "bypass");
        long app = draftFor(s);

        // the generic transition call is for offer responses only (the fee is not refundable, so there is no withdrawal)
        mvc.perform(post("/api/student/applications/" + app + "/transitions/submit").header("Authorization", bearer(s.token())))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/student/applications/" + app + "/transitions/claim").header("Authorization", bearer(s.token())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/student/applications/" + app).header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.currentStatus").value("DRAFT"));
    }

    @Test
    void shouldStopStaffFromSubmittingAnUnpaidDraft_andAllowItOncePaid() throws Exception {
        Student s = register("pay", "staffsubmit");
        long app = draftFor(s);

        mvc.perform(post("/api/staff/applications/" + app + "/transitions/submit").header("Authorization", staffAuth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isPaymentRequired());

        payAndConfirm(s, app, "BANK9999993");
        mvc.perform(post("/api/staff/applications/" + app + "/transitions/submit").header("Authorization", staffAuth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRefuseATransactionNumberAlreadyUsedOnAnotherApplication_evenAfterRejection() throws Exception {
        Student a = register("pay", "rej1");
        Student b = register("pay", "rej2");
        long appA = draftFor(a);
        long appB = draftFor(b);
        openSession(a, appA);
        reportPayment(a, appA, "BANK9999994").andExpect(status().isOk());
        mvc.perform(post("/api/staff/payments/" + paymentId(appA) + "/reject").header("Authorization", staffAuth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Amount does not match\"}")).andExpect(status().isOk());

        openSession(b, appB);
        reportPayment(b, appB, "BANK9999994").andExpect(status().isConflict());
    }

    @Test
    void shouldStillRenderTheForm_whenTheStudentHasAProfilePhoto() throws Exception {
        Student s = register("pay", "photo");
        var image = new java.awt.image.BufferedImage(200, 260, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var jpeg = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "jpeg", jpeg);
        mvc.perform(multipart(s.applicantUrl() + "/photo")
                        .file(new MockMultipartFile("file", "me.jpg", "image/jpeg", jpeg.toByteArray()))
                        .header("Authorization", bearer(s.token())))
                .andExpect(status().isCreated());
        long app = draftFor(s);
        payAndConfirm(s, app, "BANK9999995");

        documentsWorker.runOnce();

        mvc.perform(get(url(app, "/documents/APPLICATION_FORM")).header("Authorization", bearer(s.token())))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void shouldHoldBackSubmission_untilTheApplicantHasSigned() throws Exception {
        Student s = register("pay", "nosign");
        long app = draftFor(s);
        openSession(s, app);
        reportPayment(s, app, "BANK9999996").andExpect(status().isOk());
        mvc.perform(post("/api/staff/payments/" + paymentId(app) + "/confirm").header("Authorization", staffAuth))
                .andExpect(status().isOk());

        // the profile is checked before the signature: nothing is finished yet
        submitApplication(s, app).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("profile")))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("ONBOARDING")))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("FAMILY")));
        completeProfile(s);
        profileDone.add(s.applicantId());
        submitApplication(s, app).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("sign")));
        sign(s);
        submitApplication(s, app).andExpect(status().isOk());
    }

    @Test
    void shouldStoreTheSignaturePrivately_andRefuseAnEmptyPadOrAnotherStudent() throws Exception {
        Student s = register("pay", "sigown");
        Student intruder = register("pay", "sigint");
        mvc.perform(get(s.applicantUrl() + "/signature").header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.signed").value(false));

        var blank = new java.io.ByteArrayOutputStream();
        var white = new java.awt.image.BufferedImage(300, 100, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        javax.imageio.ImageIO.write(white, "png", blank);
        mvc.perform(multipart(s.applicantUrl() + "/signature")
                        .file(new MockMultipartFile("file", "s.png", "image/png", blank.toByteArray()))
                        .header("Authorization", bearer(s.token())))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart(s.applicantUrl() + "/signature")
                        .file(new MockMultipartFile("file", "s.png", "image/png", "not an image".getBytes()))
                        .header("Authorization", bearer(s.token())))
                .andExpect(status().isBadRequest());

        sign(s);
        mvc.perform(get(s.applicantUrl() + "/signature").header("Authorization", bearer(s.token())))
                .andExpect(jsonPath("$.signed").value(true)).andExpect(jsonPath("$.signedAt").isNotEmpty());
        mvc.perform(get(s.applicantUrl() + "/signature/image").header("Authorization", bearer(s.token())))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG));

        mvc.perform(get(s.applicantUrl() + "/signature").header("Authorization", bearer(intruder.token()))).andExpect(status().isForbidden());
        mvc.perform(get(s.applicantUrl() + "/signature/image").header("Authorization", bearer(intruder.token()))).andExpect(status().isForbidden());
        mvc.perform(multipart(s.applicantUrl() + "/signature")
                        .file(new MockMultipartFile("file", "s.png", "image/png", signaturePng()))
                        .header("Authorization", bearer(intruder.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReplaceTheSignature_withoutLeavingTheOldOneReachable() throws Exception {
        Student s = register("pay", "sigtwice");
        sign(s);
        sign(s);

        assertThat(jdbc.queryForObject("select count(*) from nad_media_asset where category = 'APPLICANT_SIGNATURE' "
                + "and deleted_at is null and owner_id = ?", Integer.class, s.applicantId())).isEqualTo(1);
    }

    @Test
    void shouldLeaveAConditionalDocumentOffTheChecklist_whenItDoesNotApplyToTheStudent() throws Exception {
        requireDocuments("{\"requirements\":[{\"docType\":\"PASSPORT\",\"mandatory\":true},"
                + "{\"docType\":\"POLICE_CLEARANCE\",\"mandatory\":true,\"appliesWhen\":\"MINOR\"},"
                + "{\"docType\":\"STUDY_PLAN\",\"mandatory\":true,\"appliesWhen\":\"DOCTORAL\"}]}");
        Student s = register("pay", "cond");
        long app = draftFor(s);
        // an adult (born 2001) applying at master level
        mvc.perform(put(s.applicantUrl() + "/interests").header("Authorization", bearer(s.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"desiredLevel\":\"MASTER\",\"fields\":[\"MEDICINE\"],\"cities\":[\"Beijing\",\"Shanghai\",\"Hangzhou\"]}"));

        mvc.perform(get("/api/student/applications/" + app + "/documents/checklist").header("Authorization", bearer(s.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(3)))
                .andExpect(jsonPath("$[?(@.docType=='PASSPORT')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.docType=='STUDY_PLAN')]").isEmpty()); // doctorate only
    }

    private static final byte[] TEMPLATE_PDF = ("%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF\n").getBytes();

    private long addProgramTemplate(String title) throws Exception {
        String created = mvc.perform(multipart("/api/staff/document-templates/programs/" + programId)
                        .file(new MockMultipartFile("file", "form-template.pdf", "application/pdf", TEMPLATE_PDF))
                        .param("title", title).header("Authorization", staffAuth))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.mediaId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(created, "$.id")).longValue();
    }

    @Test
    void shouldLetAStudentDownloadTheTemplatesOfTheirOwnProgramme() throws Exception {
        long templateId = addProgramTemplate("Application form template");
        Student s = register("pay", "tpl1");
        long app = draftFor(s);

        mvc.perform(get("/api/student/applications/" + app + "/templates").header("Authorization", bearer(s.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Application form template"))
                .andExpect(jsonPath("$[0].fileName").isNotEmpty());
        byte[] bytes = mvc.perform(get("/api/student/applications/" + app + "/templates/" + templateId)
                        .header("Authorization", bearer(s.token())))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(bytes, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void shouldKeepTemplatesAwayFromAnotherStudentAndFromAnotherOpportunity() throws Exception {
        long templateId = addProgramTemplate("Template");
        Student owner = register("pay", "tpl2");
        Student intruder = register("pay", "tpl3");
        long app = draftFor(owner);

        mvc.perform(get("/api/student/applications/" + app + "/templates").header("Authorization", bearer(intruder.token())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/student/applications/" + app + "/templates/" + templateId).header("Authorization", bearer(intruder.token())))
                .andExpect(status().isForbidden());
        // a scholarship-only application does not get a programme's template
        String scholarship = mvc.perform(post("/api/staff/scholarships").header("Authorization", staffAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Template Scholarship\",\"country\":\"cn\",\"fundingModel\":\"FULLY\","
                                + "\"status\":\"ACTIVE\",\"publishStatus\":\"PUBLISHED\",\"levels\":[\"MASTER\"]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long sid = ((Number) JsonPath.read(scholarship, "$.view.id")).longValue();
        String other = startScholarshipOnly(owner, sid).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long otherApp = ((Number) JsonPath.read(other, "$.id")).longValue();
        mvc.perform(get("/api/student/applications/" + otherApp + "/templates/" + templateId).header("Authorization", bearer(owner.token())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRefuseABadTemplateUpload_andKeepTheEditorForStaff() throws Exception {
        Student s = register("pay", "tpl4");
        mvc.perform(multipart("/api/staff/document-templates/programs/" + programId)
                        .file(new MockMultipartFile("file", "tool.exe", "application/x-msdownload", "MZ".getBytes()))
                        .param("title", "Bad").header("Authorization", staffAuth))
                .andExpect(status().is4xxClientError());
        mvc.perform(multipart("/api/staff/document-templates/programs/" + programId)
                        .file(new MockMultipartFile("file", "t.pdf", "application/pdf", TEMPLATE_PDF))
                        .param("title", "Student upload").header("Authorization", bearer(s.token())))
                .andExpect(status().isForbidden());

        long id = addProgramTemplate("Removable");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/staff/document-templates/" + id)
                .header("Authorization", bearer(s.token()))).andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/staff/document-templates/" + id)
                .header("Authorization", staffAuth)).andExpect(status().isNoContent());
        mvc.perform(get("/api/staff/document-templates/programs/" + programId).header("Authorization", staffAuth))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldLetAnyoneVerifyAPrintedDocument_withoutLearningWhoItIsFor() throws Exception {
        Student s = register("pay", "verify");
        long app = draftFor(s);
        payAndConfirm(s, app, "BANK9999997");
        documentsWorker.runOnce();
        var row = jdbc.queryForMap("select doc_number, verify_code from nad_payment_document "
                + "where application_id = ? and kind = 'RECEIPT'", app);
        String number = (String) row.get("doc_number");
        String code = (String) row.get("verify_code");

        mvc.perform(get("/api/public/documents/verify").param("number", number).param("code", code))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.kind").value("RECEIPT")).andExpect(jsonPath("$.number").value(number))
                .andExpect(jsonPath("$.issuedOn").isNotEmpty())
                .andExpect(jsonPath("$.details.applicantName").exists())
                .andExpect(jsonPath("$.details.applicationReference").isNotEmpty())
                .andExpect(jsonPath("$.details.feeStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.details.feeAmount").isNotEmpty())
                // contact and passport details never leave through the public page
                .andExpect(jsonPath("$.details.email").doesNotExist()).andExpect(jsonPath("$.details.passportNo").doesNotExist());
        mvc.perform(get("/api/public/documents/verify").param("number", number).param("code", "AAAAAAAAAAAA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(false)).andExpect(jsonPath("$.kind").doesNotExist());
        mvc.perform(get("/api/public/documents/verify").param("number", "NAD-RCT-2099-999999").param("code", code))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(false));
    }
}
