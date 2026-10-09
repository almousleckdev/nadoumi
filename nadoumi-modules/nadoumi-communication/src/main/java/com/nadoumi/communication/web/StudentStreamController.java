package com.nadoumi.communication.web;

import com.nadoumi.communication.stream.ChatStreamService;
import com.nadoumi.identity.access.CurrentCaller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** {@code text/event-stream} of chat events (messages, receipts, presence) for the signed-in student. */
@RestController
@RequestMapping("/api/student")
public class StudentStreamController {

    private final ChatStreamService streams;
    private final CurrentCaller caller;

    public StudentStreamController(ChatStreamService streams, CurrentCaller caller) {
        this.streams = streams;
        this.caller = caller;
    }

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter stream() {
        return streams.open(caller.requireUserId());
    }
}
