package com.ruoyi.nadoumi;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** /login is staff-only; /api/student/login is external-only (API_DESIGN §4.1). */
class LoginBoundaryTest extends AbstractNadIntegrationTest {

    @Test
    void staffCanUseStaffLoginAndStudentCannot() throws Exception {
        createStaff("boundary_staff", "ops_manager");

        staffToken("boundary_staff"); // 200 + token asserted inside helper

        createStudent("boundary_student");
        mvc.perform(post("/login").contentType("application/json")
                        .content("{\"username\":\"boundary_student\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void studentCanUseStudentLoginAndStaffCannot() throws Exception {
        createStudent("student_ok");
        studentToken("student_ok"); // 200 + token asserted inside helper

        createStaff("staff_blocked", "ops_manager");
        // A staff email is not a user_type='10' row, so it never resolves as a student.
        // The response is the same generic "email or password is incorrect" (400) a
        // wrong password gets — the boundary holds without disclosing the account type.
        mvc.perform(post("/api/student/login").contentType("application/json")
                        .content("{\"email\":\"staff_blocked@staff.test\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
