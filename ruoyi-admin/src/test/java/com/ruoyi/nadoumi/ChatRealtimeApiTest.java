package com.ruoyi.nadoumi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

/**
 * Real-time delivery over genuine SSE connections to a running server: messages arrive without any request,
 * receipts flow back to the sender, and presence changes reach the people who share a conversation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "nadoumi.chat.heartbeat-seconds=1")
class ChatRealtimeApiTest extends AbstractNadIntegrationTest {

    private static final String JSON = "application/json";
    private static final long WAIT_SECONDS = 10;

    @LocalServerPort
    private int port;

    private record Event(String name, String data) {
    }

    /** One open SSE connection: a reader thread parsing the stream into a queue of events. */
    private final class Stream {
        final BlockingQueue<Event> events = new LinkedBlockingQueue<>();
        final CompletableFuture<HttpResponse<java.util.stream.Stream<String>>> response;

        Stream(String path, String token) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .header("Authorization", bearer(token)).header("Accept", "text/event-stream").GET().build();
            response = HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofLines());
            response.thenAcceptAsync(r -> {
                String name = null;
                List<String> data = new ArrayList<>();
                try (java.util.stream.Stream<String> lines = r.body()) {
                    for (var it = lines.iterator(); it.hasNext(); ) {
                        String line = it.next();
                        if (line.startsWith("event:")) {
                            name = line.substring(6).trim();
                        }
                        else if (line.startsWith("data:")) {
                            data.add(line.substring(5).trim());
                        }
                        else if (line.isEmpty() && name != null) {
                            events.add(new Event(name, String.join("\n", data)));
                            name = null;
                            data.clear();
                        }
                    }
                }
                catch (RuntimeException closedByTest) {
                    // the test cancelled the stream
                }
            });
        }

        /** The next event with this name, skipping heartbeats and anything else. */
        Event next(String name) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
            while (System.nanoTime() < deadline) {
                Event e = events.poll(200, TimeUnit.MILLISECONDS);
                if (e != null && e.name().equals(name)) {
                    return e;
                }
            }
            throw new AssertionError("no '" + name + "' event within " + WAIT_SECONDS + "s");
        }

        void close() {
            response.cancel(true);
            response.thenAccept(r -> r.body().close());
        }
    }

    private long staffA;
    private long studentA;
    private String staffAToken;
    private String studentAToken;
    private final List<Stream> open = new ArrayList<>();

    @BeforeEach
    void people() throws Exception {
        staffA = createStaff("rt_advisor", "ops_manager");
        studentA = createStudent("rt_student");
        staffAToken = staffToken("rt_advisor");
        studentAToken = studentToken("rt_student");
    }

    @AfterEach
    void closeStreams() {
        open.forEach(Stream::close);
    }

    private Stream connect(String path, String token) {
        Stream stream = new Stream(path, token);
        open.add(stream);
        return stream;
    }

    private long openChat() throws Exception {
        String res = mvc.perform(post("/api/staff/conversations/direct").header("Authorization", bearer(staffAToken))
                        .contentType(JSON).content("{\"userId\":" + studentA + "}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    private long staffSays(long conversation, String body) throws Exception {
        String res = mvc.perform(post("/api/staff/conversations/{id}/messages", conversation)
                        .header("Authorization", bearer(staffAToken)).contentType(JSON).content("{\"body\":\"" + body + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    @Test
    void aMessageReachesTheRecipientInstantly_andTheSenderLearnsItWasDelivered() throws Exception {
        long conversation = openChat();
        Stream student = connect("/api/student/stream", studentAToken);
        Stream staff = connect("/api/staff/stream", staffAToken);
        student.next("ready");
        staff.next("ready");

        long id = staffSays(conversation, "Welcome aboard");

        Event arrived = student.next("message");
        assertThat((Number) JsonPath.read(arrived.data(), "$.id")).isEqualTo((int) id);
        assertThat((String) JsonPath.read(arrived.data(), "$.body")).isEqualTo("Welcome aboard");
        assertThat((Number) JsonPath.read(arrived.data(), "$.conversationId")).isEqualTo((int) conversation);

        Event delivered = staff.next("delivered");
        assertThat((Number) JsonPath.read(delivered.data(), "$.messageId")).isEqualTo((int) id);
        assertThat((Number) JsonPath.read(delivered.data(), "$.userId")).isEqualTo((int) studentA);
        assertThat(jdbc.queryForObject("select last_delivered_message_id from nad_conversation_participant "
                + "where conversation_id = ? and user_id = ?", Long.class, conversation, studentA)).isEqualTo(id);
    }

    @Test
    void readingAMessageTellsTheSenderInstantly() throws Exception {
        long conversation = openChat();
        Stream staff = connect("/api/staff/stream", staffAToken);
        staff.next("ready");
        long id = staffSays(conversation, "Please read this");

        mvc.perform(post("/api/student/conversations/{id}/read", conversation).header("Authorization", bearer(studentAToken)))
                .andExpect(status().isNoContent());

        Event read = staff.next("read");
        assertThat((Number) JsonPath.read(read.data(), "$.messageId")).isEqualTo((int) id);
        assertThat((Number) JsonPath.read(read.data(), "$.userId")).isEqualTo((int) studentA);
    }

    @Test
    void messagesSentWhileOfflineAreMarkedDelivered_whenTheRecipientComesBack() throws Exception {
        long conversation = openChat();
        Stream staff = connect("/api/staff/stream", staffAToken);
        staff.next("ready");
        long id = staffSays(conversation, "Sent while you were away");

        connect("/api/student/stream", studentAToken);

        Event delivered = staff.next("delivered");
        assertThat((Number) JsonPath.read(delivered.data(), "$.messageId")).isEqualTo((int) id);
    }

    @Test
    void peersSeeWhenSomeoneComesOnlineAndWhenTheyLeave_withTheirLastSeenTime() throws Exception {
        openChat();
        Stream staff = connect("/api/staff/stream", staffAToken);
        staff.next("ready");

        Stream student = connect("/api/student/stream", studentAToken);
        Event online = staff.next("presence");
        assertThat((Number) JsonPath.read(online.data(), "$.userId")).isEqualTo((int) studentA);
        assertThat((Boolean) JsonPath.read(online.data(), "$.online")).isTrue();

        student.close();
        Event offline = staff.next("presence");
        assertThat((Boolean) JsonPath.read(offline.data(), "$.online")).isFalse();
        assertThat((String) JsonPath.read(offline.data(), "$.lastSeenAt")).isNotBlank();
        assertThat(jdbc.queryForObject("select count(*) from nad_user_presence where user_id = ?", Integer.class, studentA))
                .isEqualTo(1);
    }

    @Test
    void anotherStudentNeverReceivesTheMessagesOfAChat() throws Exception {
        long other = createStudent("rt_other");
        String otherToken = studentToken("rt_other");
        long conversation = openChat();
        Stream outsider = connect("/api/student/stream", otherToken);
        Stream student = connect("/api/student/stream", studentAToken);
        outsider.next("ready");
        student.next("ready");

        staffSays(conversation, "private to the pair");

        student.next("message");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            Event e = outsider.events.poll(200, TimeUnit.MILLISECONDS);
            assertThat(e == null || !e.name().equals("message")).as("outsider %s got %s", other, e).isTrue();
        }
    }
}
