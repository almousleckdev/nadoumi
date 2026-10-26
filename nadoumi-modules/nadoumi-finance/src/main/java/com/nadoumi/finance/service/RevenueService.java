package com.nadoumi.finance.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.text.Texts;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.common.web.PageSupport;
import com.nadoumi.finance.domain.MoneyTotal;
import com.nadoumi.finance.domain.Revenue;
import com.nadoumi.finance.mapper.RevenueMapper;
import com.nadoumi.finance.web.request.RevenueRequest;
import com.nadoumi.finance.web.response.RevenueResponse;
import com.nadoumi.common.exception.NadNotFoundException;
import com.ruoyi.common.utils.AuditActor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Revenue records — plain CRUD; the finance summary derives net earnings from these. */
@Service
public class RevenueService {

    public static final String SOURCE_APPLICATION_FEE = "APPLICATION_FEE";
    public static final String SOURCE_SERVICE_FEE = "SERVICE_FEE";
    private static final String RELATED_APPLICATION = "application";

    private final RevenueMapper mapper;

    public RevenueService(RevenueMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<RevenueResponse> list(String q, String source, LocalDate from, LocalDate to, int page,
            int size) {
        page = PageSupport.clampPage(page);
        size = PageSupport.clampSize(size);
        PageHelper.startPage(page + 1, size);
        List<Revenue> rows = mapper.search(Texts.blankToNull(q), Texts.blankToNull(source), from, to);
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(RevenueResponse::from).toList(), page, size, total);
    }

    /** The records {@link #list} would return for these filters, summed per currency. */
    @Transactional(readOnly = true)
    public List<MoneyTotal> totals(String q, String source, LocalDate from, LocalDate to) {
        return mapper.totalsMatching(Texts.blankToNull(q), Texts.blankToNull(source), from, to);
    }

    @Transactional(readOnly = true)
    public RevenueResponse get(long id) {
        return RevenueResponse.from(findEntity(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public RevenueResponse create(RevenueRequest req) {
        Revenue r = new Revenue();
        apply(r, req);
        r.setRecordedBy(AuditActor.userId());
        r.setCreateBy(AuditActor.username());
        mapper.insert(r);
        return get(r.getId());
    }

    /**
     * Books a confirmed application-fee payment as revenue. Called by the payment module inside its own
     * transaction when staff confirm a payment, so bookkeeping needs no second entry. Idempotent per
     * (application, payment reference): a repeated call does nothing.
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordApplicationFee(BigDecimal amount, String currency, LocalDate receivedOn, String reference,
            long applicationId, long recordedBy) {
        recordPayment(SOURCE_APPLICATION_FEE, amount, currency, receivedOn, reference, applicationId, recordedBy);
    }

    /** Books a confirmed student payment as revenue under its source ({@link #SOURCE_APPLICATION_FEE} or {@link #SOURCE_SERVICE_FEE}). */
    @Transactional(rollbackFor = Exception.class)
    public void recordPayment(String source, BigDecimal amount, String currency, LocalDate receivedOn, String reference,
            long applicationId, long recordedBy) {
        if (mapper.countByRelated(RELATED_APPLICATION, applicationId, reference) > 0) {
            return;
        }
        Revenue r = new Revenue();
        r.setSource(source);
        r.setTitle((SOURCE_SERVICE_FEE.equals(source) ? "Service fee " : "Application fee ") + reference);
        r.setAmount(amount);
        r.setCurrency(currency.toUpperCase(Locale.ROOT));
        r.setReceivedOn(receivedOn);
        r.setReference(reference);
        r.setRelatedType(RELATED_APPLICATION);
        r.setRelatedId(applicationId);
        r.setRecordedBy(recordedBy);
        r.setCreateBy(String.valueOf(recordedBy));
        mapper.insert(r);
    }

    @Transactional(rollbackFor = Exception.class)
    public RevenueResponse update(long id, RevenueRequest req) {
        Revenue r = findEntity(id);
        apply(r, req);
        r.setUpdateBy(AuditActor.username());
        mapper.update(r);
        return get(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(long id) {
        if (mapper.deleteById(id) == 0) {
            throw new NadNotFoundException("revenue record not found");
        }
    }

    private Revenue findEntity(long id) {
        Revenue r = mapper.findById(id);
        if (r == null) {
            throw new NadNotFoundException("revenue record not found");
        }
        return r;
    }

    private void apply(Revenue r, RevenueRequest req) {
        r.setSource(req.source().trim().toUpperCase(Locale.ROOT));
        r.setTitle(req.title().trim());
        r.setDescription(Texts.blankToNull(req.description()));
        r.setAmount(req.amount());
        r.setCurrency(req.currency().toUpperCase(Locale.ROOT));
        r.setReceivedOn(req.receivedOn());
        r.setReference(Texts.blankToNull(req.reference()));
        r.setRelatedType(Texts.blankToNull(req.relatedType()));
        r.setRelatedId(req.relatedId());
        r.setNotes(Texts.blankToNull(req.notes()));
    }
}
