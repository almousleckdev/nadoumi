package com.nadoumi.finance.web;

import com.nadoumi.common.web.PageResponse;
import com.nadoumi.finance.domain.Revenue;
import com.nadoumi.finance.service.FinanceSummaryService;
import com.nadoumi.finance.service.RevenueService;
import com.nadoumi.finance.web.request.RevenueRequest;
import com.nadoumi.finance.web.response.FinanceSummary;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

/** Revenue records + the derived finance summary (net earnings). */
@RestController
@RequestMapping("/api/staff")
public class StaffRevenueController {

    private final RevenueService service;
    private final FinanceSummaryService summaryService;

    public StaffRevenueController(RevenueService service, FinanceSummaryService summaryService) {
        this.service = service;
        this.summaryService = summaryService;
    }

    @GetMapping("/revenue")
    @PreAuthorize("@ss.hasPermi('nad:revenue:list')")
    public PageResponse<Revenue> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(q, source, from, to, page, size);
    }

    @GetMapping("/revenue/{id}")
    @PreAuthorize("@ss.hasPermi('nad:revenue:query')")
    public Revenue get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/revenue")
    @PreAuthorize("@ss.hasPermi('nad:revenue:add')")
    @Log(title = "Revenue", businessType = BusinessType.INSERT)
    @ResponseStatus(HttpStatus.CREATED)
    public Revenue create(@Valid @RequestBody RevenueRequest req) {
        return service.create(req);
    }

    @PutMapping("/revenue/{id}")
    @PreAuthorize("@ss.hasPermi('nad:revenue:edit')")
    @Log(title = "Revenue", businessType = BusinessType.UPDATE)
    public Revenue update(@PathVariable long id, @Valid @RequestBody RevenueRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/revenue/{id}")
    @PreAuthorize("@ss.hasPermi('nad:revenue:remove')")
    @Log(title = "Revenue", businessType = BusinessType.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }

    @GetMapping("/finance/summary")
    @PreAuthorize("@ss.hasPermi('nad:finance:view')")
    public FinanceSummary summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return summaryService.summary(from, to);
    }
}
