package com.nadoumi.finance.web;

import com.nadoumi.common.web.PageResponse;
import com.nadoumi.finance.domain.Expense;
import com.nadoumi.finance.domain.ExpenseCategory;
import com.nadoumi.finance.service.ExpenseService;
import com.nadoumi.finance.service.ReceiptWriter;
import com.nadoumi.finance.web.request.ExpenseRequest;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Expense records + receipt PDF. Approve / pay / reject need {@code nad:expense:approve}. */
@RestController
@RequestMapping("/api/staff/expenses")
public class StaffExpenseController {

    private final ExpenseService service;
    private final ReceiptWriter receipts;

    public StaffExpenseController(ExpenseService service, ReceiptWriter receipts) {
        this.service = service;
        this.receipts = receipts;
    }

    private boolean canApprove() {
        Set<String> perms = SecurityUtils.getLoginUser().getPermissions();
        return perms.contains("*:*:*") || perms.contains("nad:expense:approve");
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('nad:expense:list')")
    public PageResponse<Expense> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(q, status, categoryId, from, to, page, size);
    }

    @GetMapping("/categories")
    @PreAuthorize("@ss.hasPermi('nad:expense:list')")
    public List<ExpenseCategory> categories() {
        return service.categories();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:expense:query')")
    public Expense get(@PathVariable long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/receipt")
    @PreAuthorize("@ss.hasPermi('nad:expense:query')")
    public ResponseEntity<String> receipt(@PathVariable long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(service.receiptHtml(id, receipts));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('nad:expense:add')")
    @Log(title = "Expense", businessType = BusinessType.INSERT)
    @ResponseStatus(HttpStatus.CREATED)
    public Expense create(@Valid @RequestBody ExpenseRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:expense:edit')")
    @Log(title = "Expense", businessType = BusinessType.UPDATE)
    public Expense update(@PathVariable long id, @Valid @RequestBody ExpenseRequest req) {
        return service.update(id, req);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPermi('nad:expense:edit')")
    @Log(title = "Expense status", businessType = BusinessType.UPDATE)
    public Expense changeStatus(@PathVariable long id, @RequestBody Map<String, String> body) {
        return service.changeStatus(id, body.getOrDefault("status", ""), canApprove());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('nad:expense:remove')")
    @Log(title = "Expense", businessType = BusinessType.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
