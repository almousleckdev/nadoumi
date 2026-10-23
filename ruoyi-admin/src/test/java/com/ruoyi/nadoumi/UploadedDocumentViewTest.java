package com.ruoyi.nadoumi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/** A student can open the document they uploaded, and nobody else can. */
class UploadedDocumentViewTest extends AbstractStudentIntegrationTest {

    private long uploadPassport(Student s) throws Exception {
        String res = mvc.perform(multipart("/api/student/documents")
                        .file(new MockMultipartFile("file", "passport.png", "image/png", ApplicationPaymentFlowTest.signaturePng()))
                        .param("applicantId", String.valueOf(s.applicantId())).param("docType", "PASSPORT")
                        .header("Authorization", bearer(s.token())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    @Test
    void shouldLetTheStudentOpenTheirUploadedDocument_asABrowserWouldToPreviewOrDownloadIt() throws Exception {
        Student s = register("view", "own");
        long doc = uploadPassport(s);

        mvc.perform(get("/api/student/documents/" + doc + "/versions/1/content").param("json", "1")
                        .header("Authorization", bearer(s.token())))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRefuseAnotherStudentTheDocument() throws Exception {
        Student owner = register("view", "owner");
        Student stranger = register("view", "stranger");
        long doc = uploadPassport(owner);

        mvc.perform(get("/api/student/documents/" + doc + "/versions/1/content").param("json", "1")
                        .header("Authorization", bearer(stranger.token())))
                .andExpect(status().isForbidden());
    }
}
