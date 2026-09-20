package com.nadoumi.support.web;

import com.nadoumi.communication.web.request.PostMessageRequest;
import com.nadoumi.communication.web.response.MessageResponse;
import com.nadoumi.support.domain.enums.TicketStatus;
import com.nadoumi.support.service.SupportTicketService;
import com.nadoumi.support.web.request.CreateTicketRequest;
import com.nadoumi.support.web.response.StudentTicketDetail;
import com.nadoumi.support.web.response.StudentTicketSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The caller's own support tickets. Ownership is enforced in the service (404 for anyone else's). */
@RestController
@RequestMapping("/api/student/support/tickets")
public class StudentSupportTicketController {

    private final SupportTicketService tickets;

    public StudentSupportTicketController(SupportTicketService tickets) {
        this.tickets = tickets;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentTicketDetail create(@Valid @RequestBody CreateTicketRequest req) {
        return tickets.create(req);
    }

    @GetMapping
    public List<StudentTicketSummary> mine(@RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return tickets.listMine(status, page, size);
    }

    @GetMapping("/{id}")
    public StudentTicketDetail detail(@PathVariable Long id) {
        return tickets.get(id);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse reply(@PathVariable Long id, @Valid @RequestBody PostMessageRequest req) {
        return tickets.reply(id, req);
    }
}
