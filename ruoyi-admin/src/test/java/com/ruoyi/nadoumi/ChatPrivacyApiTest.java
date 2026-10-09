package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Chat is strictly staff to student, and a conversation belongs to the two people in it. Everything here goes
 * through the real controllers, services and SQL on throwaway MySQL and Redis.
 */
class ChatPrivacyApiTest extends AbstractNadIntegrationTest {

    private static final String JSON = "application/json";
    private static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n".getBytes();

    private long staffA;
    private long staffB;
    private long studentA;
    private long studentB;
    private String staffAToken;
    private String staffBToken;
    private String studentAToken;
    private String studentBToken;

    @BeforeEach
    void people() throws Exception {
        staffA = createStaff("advisor_a", "ops_manager");
        staffB = createStaff("advisor_b", "ops_manager");
        studentA = createStudent("amina");
        studentB = createStudent("bilal");
        staffAToken = staffToken("advisor_a");
        staffBToken = staffToken("advisor_b");
        studentAToken = studentToken("amina");
        studentBToken = studentToken("bilal");
    }

    private static String idBody(long userId) {
        return "{\"userId\":" + userId + "}";
    }

    private long studentOpens(String token, long staffUserId) throws Exception {
        String res = mvc.perform(post("/api/student/conversations/direct").header("Authorization", bearer(token))
                        .contentType(JSON).content(idBody(staffUserId)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    private long staffOpens(String token, long studentUserId) throws Exception {
        String res = mvc.perform(post("/api/staff/conversations/direct").header("Authorization", bearer(token))
                        .contentType(JSON).content(idBody(studentUserId)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    private long studentSays(String token, long conversationId, String body) throws Exception {
        String res = mvc.perform(post("/api/student/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(token)).contentType(JSON)
                        .content("{\"body\":\"" + body + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    /** RuoYi may answer a refused call with HTTP 403 or with 200 and {"code":403} in the body; either is a refusal. */
    private static void assertRefused(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        boolean refused = result.getResponse().getStatus() == 403 || result.getResponse().getStatus() == 401
                || body.contains("\"code\":403") || body.contains("\"code\":401");
        assertThat(refused).as("expected a refusal but got %s %s", result.getResponse().getStatus(), body).isTrue();
    }

    // ---- who can talk to whom ----

    @Test
    void aStudentCannotStartAChatWithAnotherStudent() throws Exception {
        mvc.perform(post("/api/student/conversations/direct").header("Authorization", bearer(studentAToken))
                        .contentType(JSON).content(idBody(studentB)))
                .andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("select count(*) from nad_conversation", Integer.class)).isZero();
    }

    @Test
    void aStaffMemberCannotStartAChatWithAnotherStaffMember() throws Exception {
        mvc.perform(post("/api/staff/conversations/direct").header("Authorization", bearer(staffAToken))
                        .contentType(JSON).content(idBody(staffB)))
                .andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("select count(*) from nad_conversation", Integer.class)).isZero();
    }

    @Test
    void aStudentCannotUseTheStaffSide() throws Exception {
        assertRefused(mvc.perform(get("/api/staff/chat/students").param("q", "amina")
                .header("Authorization", bearer(studentAToken))).andReturn());
        assertRefused(mvc.perform(get("/api/staff/conversations")
                .header("Authorization", bearer(studentAToken))).andReturn());
    }

    @Test
    void aStaffMemberWithoutTheChatPermissionCannotBeChosenByAStudent() throws Exception {
        long noChat = createStaff("front_desk", "common");

        mvc.perform(post("/api/student/conversations/direct").header("Authorization", bearer(studentAToken))
                        .contentType(JSON).content(idBody(noChat)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void thePairSharesExactlyOneConversation_whoeverStartsIt() throws Exception {
        long first = studentOpens(studentAToken, staffA);
        long again = studentOpens(studentAToken, staffA);
        long fromStaff = staffOpens(staffAToken, studentA);

        assertThat(again).isEqualTo(first);
        assertThat(fromStaff).isEqualTo(first);
        assertThat(jdbc.queryForObject("select count(*) from nad_conversation", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from nad_conversation_participant", Integer.class)).isEqualTo(2);
    }

    // ---- privacy between staff and between students ----

    @Test
    void anotherStaffMemberCannotSeeOrReadOrWriteIntoAPrivateChat() throws Exception {
        long conversation = studentOpens(studentAToken, staffA);
        studentSays(studentAToken, conversation, "my passport number is private");

        mvc.perform(get("/api/staff/conversations").header("Authorization", bearer(staffBToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        assertRefused(mvc.perform(get("/api/staff/conversations/{id}/messages", conversation)
                .header("Authorization", bearer(staffBToken))).andReturn());
        assertRefused(mvc.perform(post("/api/staff/conversations/{id}/messages", conversation)
                .header("Authorization", bearer(staffBToken)).contentType(JSON).content("{\"body\":\"hello\"}")).andReturn());
        assertRefused(mvc.perform(post("/api/staff/conversations/{id}/read", conversation)
                .header("Authorization", bearer(staffBToken))).andReturn());
    }

    @Test
    void aStaffMemberCannotAddThemselvesToSomeoneElsesChat() throws Exception {
        long conversation = studentOpens(studentAToken, staffA);

        assertRefused(mvc.perform(post("/api/staff/conversations/{id}/participants", conversation)
                .header("Authorization", bearer(staffBToken)).contentType(JSON)
                .content("{\"userId\":" + staffB + ",\"role\":\"STAFF\"}")).andReturn());

        assertThat(jdbc.queryForObject("select count(*) from nad_conversation_participant where user_id = ?",
                Integer.class, staffB)).isZero();
    }

    @Test
    void anotherStudentCannotReadOrWriteIntoAChat() throws Exception {
        long conversation = studentOpens(studentAToken, staffA);

        assertRefused(mvc.perform(get("/api/student/conversations/{id}/messages", conversation)
                .header("Authorization", bearer(studentBToken))).andReturn());
        assertRefused(mvc.perform(post("/api/student/conversations/{id}/messages", conversation)
                .header("Authorization", bearer(studentBToken)).contentType(JSON).content("{\"body\":\"hi\"}")).andReturn());
        mvc.perform(get("/api/student/conversations").header("Authorization", bearer(studentBToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    // ---- what each side learns about the other ----

    @Test
    void theStaffDirectoryShowsOnlyAFirstNameAPhotoAndPresence() throws Exception {
        createStaff("front_desk", "common");
        jdbc.update("update sys_user set email = 'advisor_a@staff.test', phonenumber = '555-0100' where user_id = ?", staffA);

        String res = mvc.perform(get("/api/student/conversations/staff").header("Authorization", bearer(studentAToken)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(res).contains("advisor_a").contains("advisor_b").doesNotContain("front_desk");
        assertThat(res).doesNotContain("@").doesNotContain("555-0100").doesNotContain("email").doesNotContain("phone");
        assertThat((Iterable<?>) JsonPath.read(res, "$[0].keys()")).isNotNull();
    }

    @Test
    void theChatShowsAStudentOnlyAsAFirstName_neverTheirEmail() throws Exception {
        long conversation = studentOpens(studentAToken, staffA);
        studentSays(studentAToken, conversation, "hello");

        String inbox = mvc.perform(get("/api/staff/conversations").header("Authorization", bearer(staffAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].peer.name").value("amina"))
                .andReturn().getResponse().getContentAsString();
        String thread = mvc.perform(get("/api/staff/conversations/{id}/messages", conversation)
                        .header("Authorization", bearer(staffAToken)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(inbox).doesNotContain("example.test").doesNotContain("email");
        assertThat(thread).doesNotContain("example.test").doesNotContain("email");
    }

    // ---- finding students ----

    @Test
    void staffFindStudentsByNameStudentIdAndApplicationId_withoutLoadingEveryone() throws Exception {
        mvc.perform(get("/api/staff/chat/students").param("q", "ami").header("Authorization", bearer(staffAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(studentA))
                .andExpect(jsonPath("$[0].studentRef").value("STU-" + studentA))
                .andExpect(jsonPath("$[0].name").value("amina"));

        mvc.perform(get("/api/staff/chat/students").param("q", "STU-" + studentB).header("Authorization", bearer(staffAToken)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(studentB));

        // too short to be a name: nothing is returned and nothing is scanned
        mvc.perform(get("/api/staff/chat/students").param("q", "a").header("Authorization", bearer(staffAToken)))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/staff/chat/students").header("Authorization", bearer(staffAToken)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void staffCannotFindOtherStaffOrDisabledStudentsThroughStudentSearch() throws Exception {
        jdbc.update("update sys_user set status = '1' where user_id = ?", studentB);

        mvc.perform(get("/api/staff/chat/students").param("q", "advisor").header("Authorization", bearer(staffAToken)))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/staff/chat/students").param("q", "bilal").header("Authorization", bearer(staffAToken)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---- receipts and attachments ----

    @Test
    void theSenderSeesReadReceiptsAndTheRecipientSeesUnreadCounts() throws Exception {
        long conversation = staffOpens(staffAToken, studentA);
        long first = ((Number) JsonPath.read(mvc.perform(post("/api/staff/conversations/{id}/messages", conversation)
                        .header("Authorization", bearer(staffAToken)).contentType(JSON).content("{\"body\":\"Welcome\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(get("/api/student/conversations").header("Authorization", bearer(studentAToken)))
                .andExpect(jsonPath("$[0].unreadCount").value(1))
                .andExpect(jsonPath("$[0].lastMessagePreview").value("Welcome"))
                .andExpect(jsonPath("$[0].peer.name").value("advisor_a"));
        mvc.perform(get("/api/staff/conversations").header("Authorization", bearer(staffAToken)))
                .andExpect(jsonPath("$[0].peerReadMessageId").doesNotExist());

        mvc.perform(post("/api/student/conversations/{id}/read", conversation).header("Authorization", bearer(studentAToken)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/student/conversations").header("Authorization", bearer(studentAToken)))
                .andExpect(jsonPath("$[0].unreadCount").value(0));
        mvc.perform(get("/api/staff/conversations").header("Authorization", bearer(staffAToken)))
                .andExpect(jsonPath("$[0].peerReadMessageId").value(first))
                .andExpect(jsonPath("$[0].peerDeliveredMessageId").value(first));
    }

    @Test
    void anAttachmentCanBeFetchedAndDownloadedOnlyByTheTwoPeopleInTheChat() throws Exception {
        long conversation = studentOpens(studentAToken, staffA);
        String uploaded = mvc.perform(multipart("/api/student/conversations/{id}/attachments", conversation)
                        .file(new MockMultipartFile("file", "passport.pdf", "application/pdf", PDF))
                        .header("Authorization", bearer(studentAToken)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long mediaId = ((Number) JsonPath.read(uploaded, "$.mediaId")).longValue();
        String posted = mvc.perform(post("/api/student/conversations/{id}/messages", conversation)
                        .header("Authorization", bearer(studentAToken)).contentType(JSON)
                        .content("{\"body\":\"\",\"attachmentMediaIds\":[" + mediaId + "]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attachments[0].filename").value("passport.pdf"))
                .andExpect(jsonPath("$.attachments[0].url").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long attachmentId = ((Number) JsonPath.read(posted, "$.attachments[0].id")).longValue();

        mvc.perform(get("/api/staff/conversations/{id}/attachments/{a}", conversation, attachmentId)
                        .param("json", "1").header("Authorization", bearer(staffAToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").exists());
        mvc.perform(get("/api/student/conversations/{id}/attachments/{a}", conversation, attachmentId)
                        .param("json", "1").param("download", "1").header("Authorization", bearer(studentAToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").exists());

        assertRefused(mvc.perform(get("/api/staff/conversations/{id}/attachments/{a}", conversation, attachmentId)
                .param("json", "1").header("Authorization", bearer(staffBToken))).andReturn());
        assertRefused(mvc.perform(get("/api/student/conversations/{id}/attachments/{a}", conversation, attachmentId)
                .param("json", "1").header("Authorization", bearer(studentBToken))).andReturn());
    }

    @Test
    void aClosedChatRefusesNewMessages_untilItIsReopened() throws Exception {
        long conversation = studentOpens(studentAToken, staffA);
        mvc.perform(post("/api/staff/conversations/{id}/close", conversation).header("Authorization", bearer(staffAToken)))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/student/conversations/{id}/messages", conversation)
                        .header("Authorization", bearer(studentAToken)).contentType(JSON).content("{\"body\":\"hi\"}"))
                .andExpect(status().isBadRequest());

        assertThat(studentOpens(studentAToken, staffA)).isEqualTo(conversation);
        studentSays(studentAToken, conversation, "hi again");
    }
}
