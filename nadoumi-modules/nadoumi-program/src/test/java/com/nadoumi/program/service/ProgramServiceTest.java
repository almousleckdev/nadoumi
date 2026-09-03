package com.nadoumi.program.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nadoumi.common.media.MediaGateway;
import com.nadoumi.common.media.MediaUploadResult;
import com.nadoumi.identity.exception.NadBadRequestException;
import com.nadoumi.identity.exception.NadNotFoundException;
import com.nadoumi.program.domain.Program;
import com.nadoumi.program.domain.enums.ProgramStatus;
import com.nadoumi.program.domain.enums.ProgramType;
import com.nadoumi.program.domain.enums.PublishStatus;
import com.nadoumi.program.mapper.ProgramMapper;
import com.nadoumi.program.web.request.ProgramRequest;
import com.nadoumi.university.domain.University;
import com.nadoumi.university.domain.enums.UniversityStatus;
import com.nadoumi.university.service.UniversityService;
import com.nadoumi.university.web.response.PublicUniversityResponse;
import com.nadoumi.university.web.response.UniversityResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProgramServiceTest {

    private final ProgramMapper mapper = mock(ProgramMapper.class);
    private final UniversityService universityService = mock(UniversityService.class);
    private final MediaGateway media = mock(MediaGateway.class);
    private final ProgramService service = new ProgramService(mapper, universityService, media);

    private static ProgramRequest req(Long universityId, String name,
            List<ProgramRequest.MajorInput> majors, List<ProgramRequest.IntakeInput> intakes) {
        return new ProgramRequest(
                universityId, name, "计算机科学", ProgramType.BACHELOR, "Engineering",
                null, 48, null, "usd", "A four-year programme.", null,
                false, false, ProgramStatus.ACTIVE, PublishStatus.DRAFT, "note",
                majors, intakes);
    }

    private static ProgramRequest req(Long universityId, String name) {
        return req(universityId, name, List.of(), List.of());
    }

    private static UniversityResponse namedUniversity(String name) {
        University u = new University();
        u.setId(1L);
        u.setName(name);
        u.setCountry("CN");
        return UniversityResponse.of(u);
    }

    private static PublicUniversityResponse publicUniversity(String name) {
        University u = new University();
        u.setId(1L);
        u.setName(name);
        u.setCountry("CN");
        u.setStatus(UniversityStatus.ACTIVE);
        return PublicUniversityResponse.of(u);
    }

    @Test
    void create_validates_the_university_then_persists_and_replaces_children() {
        when(universityService.get(1L)).thenReturn(namedUniversity("Fudan University"));
        when(mapper.findIdByUniversityAndName(1L, "MBA")).thenReturn(null);
        when(mapper.findById(any())).thenReturn(newProgram(9L, 1L));

        service.create(req(1L, "  MBA  ",
                List.of(new ProgramRequest.MajorInput("Finance", null)),
                List.of(new ProgramRequest.IntakeInput("AUTUMN_SEPTEMBER", null, null))));

        verify(mapper).insert(any(Program.class));
        verify(mapper).deleteMajors(any());
        verify(mapper).insertMajor(any());
        verify(mapper).deleteIntakes(any());
        verify(mapper).insertIntake(any());
    }

    @Test
    void create_rejects_an_unknown_university() {
        when(universityService.get(404L)).thenThrow(new NadNotFoundException("university not found"));

        assertThatThrownBy(() -> service.create(req(404L, "MBA")))
                .isInstanceOf(NadBadRequestException.class);
        verify(mapper, never()).insert(any());
    }

    @Test
    void create_rejects_a_duplicate_name_under_the_same_university() {
        when(universityService.get(1L)).thenReturn(namedUniversity("Fudan University"));
        when(mapper.findIdByUniversityAndName(1L, "MBA")).thenReturn(77L);

        assertThatThrownBy(() -> service.create(req(1L, "MBA")))
                .isInstanceOf(NadBadRequestException.class);
        verify(mapper, never()).insert(any());
    }

    @Test
    void update_replaces_children_even_when_the_lists_are_empty() {
        when(universityService.get(1L)).thenReturn(namedUniversity("Fudan University"));
        when(mapper.findById(7L)).thenReturn(newProgram(7L, 1L));
        when(mapper.findIdByUniversityAndName(1L, "MBA")).thenReturn(7L);

        service.update(7L, req(1L, "MBA"));

        verify(mapper).update(any(Program.class));
        verify(mapper).deleteMajors(7L);
        verify(mapper).deleteIntakes(7L);
        verify(mapper, never()).insertMajor(any());
    }

    @Test
    void get_and_delete_throw_when_the_row_is_missing() {
        when(mapper.findById(1L)).thenReturn(null);
        when(mapper.delete(1L)).thenReturn(0);

        assertThatThrownBy(() -> service.get(1L)).isInstanceOf(NadNotFoundException.class);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NadNotFoundException.class);
    }

    @Test
    void publicGet_hides_a_draft_or_inactive_programme() {
        Program draft = newProgram(3L, 1L);
        draft.setStatus(ProgramStatus.ACTIVE);
        draft.setPublishStatus(PublishStatus.DRAFT);
        when(mapper.findById(3L)).thenReturn(draft);
        when(mapper.findMajors(anyLong())).thenReturn(List.of());
        when(mapper.findIntakes(anyLong())).thenReturn(List.of());
        when(universityService.publicGet("1")).thenReturn(publicUniversity("Fudan University"));

        assertThatThrownBy(() -> service.publicGet("3")).isInstanceOf(NadNotFoundException.class);

        draft.setPublishStatus(PublishStatus.PUBLISHED);
        draft.setStatus(ProgramStatus.INACTIVE);
        assertThatThrownBy(() -> service.publicGet("3")).isInstanceOf(NadNotFoundException.class);

        draft.setStatus(ProgramStatus.ACTIVE);
        assertThat(service.publicGet("3").id()).isEqualTo(3L);
        assertThat(service.publicGet("3").universityName()).isEqualTo("Fudan University");
    }

    @Test
    void publicListForUniversity_requires_a_public_university() {
        when(universityService.publicGet("2")).thenThrow(new NadNotFoundException("university not found"));

        assertThatThrownBy(() -> service.publicListForUniversity("2"))
                .isInstanceOf(NadNotFoundException.class);
    }

    @Test
    void programImageResolvesUrl() {
        Program p = newProgram(4L, 1L);
        p.setImageMediaId(15L);
        p.setStatus(ProgramStatus.ACTIVE);
        p.setPublishStatus(PublishStatus.PUBLISHED);
        when(mapper.findById(4L)).thenReturn(p);
        when(mapper.findMajors(4L)).thenReturn(List.of());
        when(mapper.findIntakes(4L)).thenReturn(List.of());
        when(universityService.get(1L)).thenReturn(namedUniversity("Fudan University"));
        when(media.publicUrl(15L)).thenReturn("https://res.cloudinary.com/x/prog.png");

        var res = service.get(4L);

        assertThat(res.imageMediaId()).isEqualTo(15L);
        assertThat(res.imageUrl()).isEqualTo("https://res.cloudinary.com/x/prog.png");
    }

    @Test
    void uploadImageStoresMediaId() {
        when(mapper.findById(4L)).thenReturn(newProgram(4L, 1L));
        when(media.upload(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong(), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(new MediaUploadResult(88L, "https://res.cloudinary.com/x/p.png"));

        var result = service.uploadImage(4L, new org.springframework.mock.web.MockMultipartFile(
                "file", "p.png", "image/png", new byte[] { 1, 2, 3 }));

        assertThat(result.mediaId()).isEqualTo(88L);
        verify(mapper).updateImageMediaId(4L, 88L);
    }

    private static Program newProgram(Long id, Long universityId) {
        Program p = new Program();
        p.setId(id);
        p.setUniversityId(universityId);
        p.setName("MBA");
        p.setProgramType(ProgramType.MASTER);
        return p;
    }
}
