package com.nadoumi.finance.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.finance.domain.Expense;
import com.nadoumi.finance.domain.ExpenseCategory;
import com.nadoumi.finance.domain.ExpenseStatus;
import com.nadoumi.finance.mapper.ExpenseMapper;
import com.nadoumi.finance.web.request.ExpenseRequest;
import com.nadoumi.identity.exception.NadBadRequestException;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.ruoyi.common.utils.SecurityUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Expense records. Lifecycle {@link ExpenseStatus}; the human receipt number
 * ({@code NAD-EXP-<year>-NNNN}) is stamped on the first move to {@code APPROVED}.
 * Approving / paying / rejecting is gated by {@code nad:expense:approve} at the
 * controller and passed in as {@code canApprove}.
 */
@Service
public class ExpenseService {

    private static final Set<String> EDITABLE = Set.of("DRAFT", "SUBMITTED", "REJECTED");
    private static final Set<ExpenseStatus> APPROVAL_MOVES =
            Set.of(ExpenseStatus.APPROVED, ExpenseStatus.PAID, ExpenseStatus.REJECTED);

    private final ExpenseMapper mapper;

    public ExpenseService(ExpenseMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<Expense> list(String q, String status, Long categoryId, LocalDate from, LocalDate to,
            int page, int size) {
        PageHelper.startPage(page + 1, size);
        List<Expense> rows = mapper.search(nz(q), nz(status), categoryId, from, to);
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows, page, size, total);
    }

    @Transactional(readOnly = true)
    public Expense get(long id) {
        Expense e = mapper.findById(id);
        if (e == null) {
            throw new NadNotFoundException("expense not found");
        }
        return e;
    }

    @Transactional(readOnly = true)
    public List<ExpenseCategory> categories() {
        return mapper.listCategories();
    }

    @Transactional
    public Expense create(ExpenseRequest req) {
        Expense e = new Expense();
        apply(e, req);
        e.setStatus(ExpenseStatus.DRAFT.name());
        e.setSubmittedBy(currentUserId());
        e.setCreateBy(currentUser());
        mapper.insert(e);
        return get(e.getId());
    }

    @Transactional
    public Expense update(long id, ExpenseRequest req) {
        Expense e = get(id);
        if (!EDITABLE.contains(e.getStatus())) {
            throw new NadBadRequestException("a " + e.getStatus() + " expense cannot be edited");
        }
        apply(e, req);
        e.setUpdateBy(currentUser());
        mapper.update(e);
        return get(id);
    }

    @Transactional
    public Expense changeStatus(long id, String targetRaw, boolean canApprove) {
        Expense e = get(id);
        ExpenseStatus from = ExpenseStatus.valueOf(e.getStatus());
        ExpenseStatus target = parse(targetRaw);
        if (from == target) {
            return e;
        }
        if (!from.canMoveTo(target)) {
            throw new NadBadRequestException("cannot move an expense from " + from + " to " + target);
        }
        if (APPROVAL_MOVES.contains(target) && !canApprove) {
            throw new NadBadRequestException("nad:expense:approve is required to " + target.name().toLowerCase(Locale.ROOT)
                    + " an expense");
        }
        LocalDateTime now = LocalDateTime.now();
        e.setStatus(target.name());
        if (target == ExpenseStatus.APPROVED) {
            if (!StringUtils.hasText(e.getReceiptNo())) {
                e.setReceiptNo(nextReceiptNo());
            }
            e.setApprovedBy(currentUserId());
            e.setApprovedAt(now);
        }
        if (target == ExpenseStatus.PAID) {
            e.setPaidAt(now);
        }
        e.setUpdateBy(currentUser());
        mapper.update(e);
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        Expense e = get(id);
        if (!Set.of("DRAFT", "REJECTED").contains(e.getStatus())) {
            throw new NadBadRequestException("only a DRAFT or REJECTED expense can be deleted");
        }
        mapper.deleteById(id);
    }

    @Transactional(readOnly = true)
    public String receiptHtml(long id, ReceiptWriter writer) {
        Expense e = get(id);
        if (!Set.of("APPROVED", "PAID").contains(e.getStatus())) {
            throw new NadBadRequestException("a receipt is only available once the expense is APPROVED or PAID");
        }
        return writer.renderHtml(e);
    }

    private void apply(Expense e, ExpenseRequest req) {
        e.setCategoryId(resolveCategoryId(req));
        e.setTitle(req.title().trim());
        e.setDescription(nz(req.description()));
        e.setAmount(req.amount());
        e.setCurrency(req.currency().toUpperCase(Locale.ROOT));
        e.setSpentOn(req.spentOn());
        e.setVendor(nz(req.vendor()));
        e.setPaymentMethod(nz(req.paymentMethod()));
        e.setNotes(nz(req.notes()));
    }

    /**
     * An explicit {@code categoryId} wins; otherwise a typed {@code categoryName}
     * is matched case-insensitively and created on first use.
     */
    private Long resolveCategoryId(ExpenseRequest req) {
        if (req.categoryId() != null) {
            return req.categoryId();
        }
        String name = nz(req.categoryName());
        if (name == null) {
            return null;
        }
        Long existing = mapper.findCategoryIdByName(name);
        if (existing != null) {
            return existing;
        }
        ExpenseCategory c = new ExpenseCategory();
        c.setName(name);
        c.setCode(slugCode(name));
        c.setActive(true);
        mapper.insertCategory(c);
        return c.getId();
    }

    private static String slugCode(String name) {
        String base = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_|_$", "");
        return base.isEmpty() ? "CUSTOM" : base.substring(0, Math.min(32, base.length()));
    }

    private String nextReceiptNo() {
        String prefix = "NAD-EXP-" + Year.now().getValue() + "-";
        Integer max = mapper.maxReceiptSeq(prefix);
        return prefix + String.format(Locale.ROOT, "%04d", (max == null ? 0 : max) + 1);
    }

    private static ExpenseStatus parse(String raw) {
        try {
            return ExpenseStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw new NadBadRequestException("unknown expense status: " + raw);
        }
    }

    private static long currentUserId() {
        try {
            return SecurityUtils.getUserId();
        } catch (RuntimeException e) {
            return 0L;
        }
    }

    private static String currentUser() {
        try {
            return SecurityUtils.getUsername();
        } catch (RuntimeException e) {
            return "system";
        }
    }

    private static String nz(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
