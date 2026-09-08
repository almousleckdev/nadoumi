package com.nadoumi.finance.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.common.web.PageSupport;
import com.nadoumi.finance.domain.Revenue;
import com.nadoumi.finance.mapper.RevenueMapper;
import com.nadoumi.finance.web.request.RevenueRequest;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.ruoyi.common.utils.SecurityUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Revenue records — plain CRUD; the finance summary derives net earnings from these. */
@Service
public class RevenueService {

    private final RevenueMapper mapper;

    public RevenueService(RevenueMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<Revenue> list(String q, String source, LocalDate from, LocalDate to, int page, int size) {
        page = PageSupport.clampPage(page);
        size = PageSupport.clampSize(size);
        PageHelper.startPage(page + 1, size);
        List<Revenue> rows = mapper.search(nz(q), nz(source), from, to);
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows, page, size, total);
    }

    @Transactional(readOnly = true)
    public Revenue get(long id) {
        Revenue r = mapper.findById(id);
        if (r == null) {
            throw new NadNotFoundException("revenue record not found");
        }
        return r;
    }

    @Transactional
    public Revenue create(RevenueRequest req) {
        Revenue r = new Revenue();
        apply(r, req);
        r.setRecordedBy(currentUserId());
        r.setCreateBy(currentUser());
        mapper.insert(r);
        return get(r.getId());
    }

    @Transactional
    public Revenue update(long id, RevenueRequest req) {
        Revenue r = get(id);
        apply(r, req);
        r.setUpdateBy(currentUser());
        mapper.update(r);
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        if (mapper.deleteById(id) == 0) {
            throw new NadNotFoundException("revenue record not found");
        }
    }

    private void apply(Revenue r, RevenueRequest req) {
        r.setSource(req.source().trim().toUpperCase(Locale.ROOT));
        r.setTitle(req.title().trim());
        r.setDescription(nz(req.description()));
        r.setAmount(req.amount());
        r.setCurrency(req.currency().toUpperCase(Locale.ROOT));
        r.setReceivedOn(req.receivedOn());
        r.setReference(nz(req.reference()));
        r.setRelatedType(nz(req.relatedType()));
        r.setRelatedId(req.relatedId());
        r.setNotes(nz(req.notes()));
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
