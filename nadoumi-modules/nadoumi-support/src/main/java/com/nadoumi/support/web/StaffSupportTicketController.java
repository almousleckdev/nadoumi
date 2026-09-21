package com.nadoumi.support.web;

import com.nadoumi.communication.web.request.PostMessageRequest;
import com.nadoumi.communication.web.response.MessageResponse;
import com.nadoumi.support.domain.enums.TicketCategory;
import com.nadoumi.support.domain.enums.TicketPriority;
import com.nadoumi.support.domain.enums.TicketStatus;
import com.nadoumi.support.service.StaffSupportTicketService;
import com.nadoumi.support.web.request.AssignTicketRequest;
import com.nadoumi.support.web.request.ChangeCategoryRequest;
import com.nadoumi.support.web.request.ChangePriorityRequest;
import com.nadoumi.support.web.request.ChangeStatusRequest;
import com.nadoumi.support.web.response.StaffTicketDetail;
import com.nadoumi.support.web.response.StaffTicketSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The staff ticket queue. Each endpoint carries its own permission; the service re-checks (defense in depth). */
@RestController
@RequestMapping("/api/staff/support/tickets")
public class StaffSupportTicketController {

    private final StaffSupportTicketService tickets;

    public StaffSupportTicketController(StaffSupportTicketService tickets) {
        this.tickets = tickets;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:support:ticket:view')")
    public List<StaffTicketSummary> queue(@RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return tickets.list(status, priority, category, assigneeId, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:support:ticket:view')")
    public StaffTicketDetail detail(@PathVariable Long id) {
        return tickets.get(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@ss.hasPermi('nad:support:ticket:manage')")
    public StaffTicketSummary changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest req) {
        return tickets.changeStatus(id, req.status());
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("@ss.hasPermi('nad:support:ticket:assign')")
    public StaffTicketSummary assign(@PathVariable Long id, @Valid @RequestBody AssignTicketRequest req) {
        return tickets.assign(id, req.assigneeUserId());
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize("@ss.hasPermi('nad:support:ticket:manage')")
    public StaffTicketSummary changePriority(@PathVariable Long id, @Valid @RequestBody ChangePriorityRequest req) {
        return tickets.changePriority(id, req.priority());
    }

    @PatchMapping("/{id}/category")
    @PreAuthorize("@ss.hasPermi('nad:support:ticket:manage')")
    public StaffTicketSummary changeCategory(@PathVariable Long id, @Valid @RequestBody ChangeCategoryRequest req) {
        return tickets.changeCategory(id, req.category());
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@ss.hasPermi('nad:support:ticket:manage')")
    public MessageResponse reply(@PathVariable Long id, @Valid @RequestBody PostMessageRequest req) {
        return tickets.reply(id, req);
    }
}
