package com.nadoumi.document.service;

import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.exception.NadNotFoundException;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaCategory;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaOwnerKind;
import com.nadoumi.common.media.MediaOwnerRef;
import com.nadoumi.common.media.ProxyStream;
import com.nadoumi.document.domain.DocumentTemplate;
import com.nadoumi.document.domain.enums.RequirementScope;
import com.nadoumi.document.mapper.DocumentTemplateMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * The files a programme or scholarship gives applicants to fill in. Staff manage them; which applicant may download
 * which template is decided by the caller (the application module checks the application's own opportunity).
 */
@Service
public class DocumentTemplateService {

    private static final int MAX_PER_OPPORTUNITY = 10;

    private final DocumentTemplateMapper mapper;
    private final MediaGateway media;

    public DocumentTemplateService(DocumentTemplateMapper mapper, MediaGateway media) {
        this.mapper = mapper;
        this.media = media;
    }

    @Transactional(readOnly = true)
    public List<DocumentTemplate> list(RequirementScope scope, long refId) {
        return mapper.findByScope(scope, refId);
    }

    @Transactional(rollbackFor = Exception.class)
    public DocumentTemplate add(RequirementScope scope, long refId, String title, MultipartFile file, long userId) {
        if (title == null || title.isBlank() || title.length() > 120) {
            throw new NadBadRequestException("a title of up to 120 characters is required");
        }
        if (file == null || file.isEmpty()) {
            throw new NadBadRequestException("a file is required");
        }
        if (mapper.findByScope(scope, refId).size() >= MAX_PER_OPPORTUNITY) {
            throw new NadBadRequestException("at most " + MAX_PER_OPPORTUNITY + " templates per opportunity");
        }
        var uploaded = media.upload(file::getInputStream, file.getOriginalFilename(), file.getContentType(),
                file.getSize(), MediaCategory.DOCUMENT_TEMPLATE, null,
                new MediaOwnerRef(scope == RequirementScope.PROGRAM ? MediaOwnerKind.PROGRAM : MediaOwnerKind.SCHOLARSHIP, refId),
                userId);
        DocumentTemplate template = new DocumentTemplate();
        template.setScope(scope);
        template.setRefId(refId);
        template.setTitle(title.strip());
        template.setFileName(media.find(uploaded.mediaId()).map(a -> a.originalFilename()).orElse("template"));
        template.setMediaId(uploaded.mediaId());
        template.setCreateBy(String.valueOf(userId));
        mapper.insert(template);
        return template;
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(long id, long userId) {
        DocumentTemplate template = mapper.findById(id);
        if (template == null) {
            throw new NadNotFoundException("template not found");
        }
        mapper.delete(id);
        media.softDelete(template.getMediaId(), userId);
    }

    @Transactional(readOnly = true)
    public DocumentTemplate require(long id) {
        DocumentTemplate template = mapper.findById(id);
        if (template == null) {
            throw new NadNotFoundException("template not found");
        }
        return template;
    }

    /** The template's bytes through the backend, access-logged. The caller has already authorised the download. */
    @Transactional(readOnly = true)
    public ProxyStream open(DocumentTemplate template, MediaAccessLogContext context) {
        return media.openProxyStream(template.getMediaId(), context);
    }
}
