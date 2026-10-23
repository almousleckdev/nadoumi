package com.nadoumi.program.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.text.Texts;
import com.nadoumi.common.web.PageResponse;
import com.nadoumi.common.web.PageSupport;
import com.nadoumi.program.domain.Major;
import com.nadoumi.program.mapper.MajorMapper;
import com.nadoumi.program.web.request.MajorBulkRequest;
import com.nadoumi.program.web.request.MajorRequest;
import com.nadoumi.program.web.response.MajorBulkResponse;
import com.nadoumi.program.web.response.MajorResponse;
import com.ruoyi.common.utils.AuditActor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The shared catalogue of majors. A programme links its majors to these rows, so one subject is entered once and
 * reused by every university; the level and department stay on the programme's link.
 */
@Service
public class MajorService {

    private static final int MAX_BULK_LINES = 500;
    private static final Pattern CELL_SEPARATOR = Pattern.compile("\t|\\||｜");
    private static final Pattern LIST_MARKER = Pattern.compile("^\\s*(?:[-*•·]|\\d+[.)])\\s+");
    /** "Computer Science 计算机科学": a Latin name directly followed by a Chinese one. */
    private static final Pattern LATIN_THEN_CJK = Pattern.compile("^(.*?[A-Za-z0-9.)])\\s*([㐀-鿿].*)$");

    private final MajorMapper mapper;

    public MajorService(MajorMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<MajorResponse> list(String q, String discipline, int page, int size) {
        int clampedPage = PageSupport.clampPage(page);
        int clampedSize = PageSupport.clampSize(size);
        PageHelper.startPage(clampedPage + 1, clampedSize);
        List<Major> rows = mapper.search(Texts.blankToNull(q), Texts.blankToNull(discipline));
        long total = new PageInfo<>(rows).getTotal();
        return PageResponse.of(rows.stream().map(MajorResponse::of).toList(), clampedPage, clampedSize, total);
    }

    @Transactional(rollbackFor = Exception.class)
    public MajorResponse create(MajorRequest req) {
        String name = normalise(req.name());
        if (mapper.findByName(name) != null) {
            throw new NadBadRequestException("a major named \"" + name + "\" already exists");
        }
        return MajorResponse.of(insert(name, Texts.blankToNull(req.nameCn()), discipline(req.discipline())));
    }

    @Transactional(rollbackFor = Exception.class)
    public MajorResponse update(Long id, MajorRequest req) {
        Major major = require(id);
        String name = normalise(req.name());
        Major sameName = mapper.findByName(name);
        if (sameName != null && !sameName.getId().equals(id)) {
            throw new NadBadRequestException("a major named \"" + name + "\" already exists; merge them instead");
        }
        major.setName(name);
        major.setNameCn(Texts.blankToNull(req.nameCn()));
        major.setDiscipline(discipline(req.discipline()));
        major.setUpdateBy(AuditActor.username());
        mapper.update(major);
        return MajorResponse.of(major);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        require(id);
        if (mapper.countLinks(id) > 0) {
            throw new NadBadRequestException("this major is used by programmes; merge it into another major instead");
        }
        mapper.delete(id);
    }

    /** Moves every programme link of {@code id} to {@code intoId} and deletes {@code id}. */
    @Transactional(rollbackFor = Exception.class)
    public MajorResponse merge(Long id, Long intoId) {
        if (id.equals(intoId)) {
            throw new NadBadRequestException("choose a different major to merge into");
        }
        require(id);
        Major target = require(intoId);
        mapper.moveLinks(id, intoId);
        mapper.delete(id);
        return MajorResponse.of(target);
    }

    /** Each pasted line is matched to an existing major by name, or created. Blank and repeated lines are skipped. */
    @Transactional(rollbackFor = Exception.class)
    public MajorBulkResponse bulk(MajorBulkRequest req) {
        String discipline = discipline(req.discipline());
        Map<String, String[]> wanted = new LinkedHashMap<>();
        for (String raw : req.text().split("\\R")) {
            String[] cells = parseLine(raw);
            if (cells != null) {
                wanted.putIfAbsent(cells[0].toLowerCase(), cells);
            }
        }
        if (wanted.size() > MAX_BULK_LINES) {
            throw new NadBadRequestException("paste at most " + MAX_BULK_LINES + " majors at a time");
        }
        List<MajorResponse> result = new ArrayList<>();
        int created = 0;
        for (String[] cells : wanted.values()) {
            Major existing = mapper.findByName(cells[0]);
            if (existing == null) {
                existing = insert(cells[0], cells[1], discipline);
                created++;
            }
            result.add(MajorResponse.of(existing));
        }
        return new MajorBulkResponse(result, created);
    }

    /**
     * The catalogue major for a typed name: the existing one when the name matches, otherwise a new one. This keeps every
     * programme major linked to the catalogue even when it was typed rather than picked.
     */
    @Transactional(rollbackFor = Exception.class)
    public Major findOrCreate(String name, String nameCn) {
        String trimmed = normalise(name);
        Major existing = mapper.findByName(trimmed);
        return existing != null ? existing : insert(trimmed, Texts.blankToNull(nameCn), null);
    }

    @Transactional(readOnly = true)
    public Major require(Long id) {
        Major major = mapper.findById(id);
        if (major == null) {
            throw new NadNotFoundException("major not found");
        }
        return major;
    }

    private Major insert(String name, String nameCn, String discipline) {
        Major major = new Major();
        major.setName(name);
        major.setNameCn(nameCn);
        major.setDiscipline(discipline);
        major.setCreateBy(AuditActor.username());
        mapper.insert(major);
        return major;
    }

    /** Trimmed with inner runs of whitespace collapsed, so "Computer  Science " and "Computer Science" are one major. */
    static String normalise(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private static String discipline(String value) {
        String trimmed = Texts.blankToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase().replaceAll("[^A-Z0-9_]", "_");
    }

    /** {name, nameCn} for a line, or null when it is empty. */
    static String[] parseLine(String raw) {
        String line = LIST_MARKER.matcher(raw).replaceFirst("").trim();
        if (line.isEmpty()) {
            return null;
        }
        String[] cells = CELL_SEPARATOR.split(line, 2);
        if (cells.length == 2) {
            return new String[] {normalise(cells[0]), Texts.blankToNull(cells[1].trim())};
        }
        var split = LATIN_THEN_CJK.matcher(line);
        return split.matches()
                ? new String[] {normalise(split.group(1)), split.group(2).trim()}
                : new String[] {normalise(line), null};
    }
}
