package com.nadoumi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.application.domain.Application;
import com.nadoumi.application.domain.ApplicationLetter;
import com.nadoumi.application.domain.enums.LetterKind;
import com.nadoumi.application.exception.PaymentRequiredException;
import com.nadoumi.application.mapper.ApplicationEventMapper;
import com.nadoumi.application.mapper.ApplicationLetterMapper;
import com.nadoumi.application.mapper.ApplicationMapper;
import com.nadoumi.application.mapper.ApplicationTaskMapper;
import com.nadoumi.common.exception.NadBadRequestException;
import com.nadoumi.common.media.MediaAccessLogContext;
import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaUploadResult;
import com.nadoumi.common.media.ProxyStream;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/** The admission letter and JW202 stay locked for the student until the service fee is confirmed, on the server. */
class ApplicationLetterServiceTest {

    private static final long APPLICATION_ID = 5L;
    private static final long STAFF_ID = 3L;

    private final ApplicationLetterMapper letters = mock(ApplicationLetterMapper.class);
    private final ApplicationMapper applications = mock(ApplicationMapper.class);
    private final PaymentGuardProvider payments = mock(PaymentGuardProvider.class);
    private final MediaGateway media = mock(MediaGateway.class);
    private final ApplicationLetterService service = new ApplicationLetterService(letters, applications,
            mock(ApplicationTaskMapper.class), mock(ApplicationEventMapper.class), payments, media);
    private final MediaAccessLogContext context = new MediaAccessLogContext(1L, null, APPLICATION_ID, null, "10.0.0.1", "junit");

    private void application(String status) {
        Application a = new Application();
        a.setId(APPLICATION_ID);
        a.setCurrentStatus(status);
        when(applications.findById(APPLICATION_ID)).thenReturn(a);
    }

    private ApplicationLetter letter() {
        ApplicationLetter l = new ApplicationLetter();
        l.setKind("ADMISSION_LETTER");
        l.setMediaId(77L);
        l.setOriginalFilename("letter.pdf");
        when(letters.findCurrentOfKind(APPLICATION_ID, "ADMISSION_LETTER")).thenReturn(l);
        when(letters.findCurrent(APPLICATION_ID)).thenReturn(List.of(l));
        return l;
    }

    @Test
    void shouldRefuseTheStudent_andNeverTouchTheFile_untilTheServiceFeeIsConfirmed() {
        application("ADMISSION_CONFIRMED");
        letter();
        when(payments.isSettled(APPLICATION_ID, "SERVICE_FEE")).thenReturn(false);

        assertThatThrownBy(() -> service.openForStudent(APPLICATION_ID, LetterKind.ADMISSION_LETTER, context))
                .isInstanceOf(PaymentRequiredException.class);
        verify(media, never()).openProxyStream(anyLong(), any());
    }

    @Test
    void shouldHideTheFileName_whileLocked_andShowItOnceTheFeeIsConfirmed() {
        application("ADMISSION_CONFIRMED");
        letter();
        when(payments.isSettled(APPLICATION_ID, "SERVICE_FEE")).thenReturn(false);

        var locked = service.listForStudent(APPLICATION_ID).get(0);
        assertThat(locked.locked()).isTrue();
        assertThat(locked.fileName()).isNull();

        when(payments.isSettled(APPLICATION_ID, "SERVICE_FEE")).thenReturn(true);
        var open = service.listForStudent(APPLICATION_ID).get(0);
        assertThat(open.locked()).isFalse();
        assertThat(open.fileName()).isEqualTo("letter.pdf");
    }

    @Test
    void shouldStreamTheFileToTheStudent_whenTheFeeIsConfirmed() {
        application("ADMISSION_CONFIRMED");
        letter();
        when(payments.isSettled(APPLICATION_ID, "SERVICE_FEE")).thenReturn(true);
        ProxyStream stream = new ProxyStream(new ByteArrayInputStream(new byte[0]), "application/pdf", 0, "letter.pdf");
        when(media.openProxyStream(77L, context)).thenReturn(stream);

        assertThat(service.openForStudent(APPLICATION_ID, LetterKind.ADMISSION_LETTER, context)).isSameAs(stream);
    }

    @Test
    void shouldLetStaffOpenALetter_whateverTheFeeState() {
        application("ADMISSION_CONFIRMED");
        letter();
        ProxyStream stream = new ProxyStream(new ByteArrayInputStream(new byte[0]), "application/pdf", 0, "letter.pdf");
        when(media.openProxyStream(77L, context)).thenReturn(stream);

        assertThat(service.openForStaff(APPLICATION_ID, LetterKind.ADMISSION_LETTER, context)).isSameAs(stream);
        verify(payments, never()).isSettled(anyLong(), any());
    }

    @Test
    void shouldAcceptAnUpload_onlyOnceTheAdmissionIsConfirmed() {
        MockMultipartFile file = new MockMultipartFile("file", "letter.pdf", "application/pdf", new byte[] {1});
        application("UNDER_UNIVERSITY_REVIEW");

        assertThatThrownBy(() -> service.upload(APPLICATION_ID, LetterKind.ADMISSION_LETTER, file, STAFF_ID))
                .isInstanceOf(NadBadRequestException.class);
        verify(media, never()).upload(any(com.nadoumi.common.media.MediaGateway.UploadSource.class), any(), any(), anyLong(),
                any(), any(), any(), anyLong());

        application("ADMISSION_CONFIRMED");
        when(media.upload(any(com.nadoumi.common.media.MediaGateway.UploadSource.class), any(), any(), anyLong(), any(),
                any(), any(), anyLong())).thenReturn(new MediaUploadResult(77L, null));

        var saved = service.upload(APPLICATION_ID, LetterKind.ADMISSION_LETTER, file, STAFF_ID);

        assertThat(saved.kind()).isEqualTo("ADMISSION_LETTER");
        assertThat(saved.fileName()).isEqualTo("letter.pdf");
        verify(letters).insert(any());
    }
}
