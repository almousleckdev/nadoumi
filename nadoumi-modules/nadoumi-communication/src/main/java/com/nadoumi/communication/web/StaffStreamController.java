package com.nadoumi.communication.web;

import com.nadoumi.communication.stream.SseConnectionRegistry;
import com.nadoumi.identity.access.CurrentCaller;
import java.util.concurrent.TimeUnit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** {@code text/event-stream} of ping-only realtime events for the signed-in staff member. */
@RestController
@RequestMapping("/api/staff")
public class StaffStreamController {

    private static final long TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(30);

    private final SseConnectionRegistry registry;
    private final CurrentCaller caller;

    public StaffStreamController(SseConnectionRegistry registry, CurrentCaller caller) {
        this.registry = registry;
        this.caller = caller;
    }

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter stream() {
        return registry.register(caller.requireUserId(), TIMEOUT_MILLIS);
    }
}
