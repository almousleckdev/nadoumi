package com.nadoumi.identity.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nadoumi.identity.service.StudentAuthService;
import com.nadoumi.identity.web.request.ChangePasswordRequest;
import com.nadoumi.identity.web.request.PasswordResetRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class StudentPasswordControllerTest {

    private final StudentAuthService service = mock(StudentAuthService.class);
    private final StudentPasswordController controller = new StudentPasswordController(service);

    @Test
    void reset_delegates_the_ticket_and_new_password() {
        controller.reset(new PasswordResetRequest("tkt_1", "BrandNew1!"));
        verify(service).resetPassword("tkt_1", "BrandNew1!");
    }

    @Test
    void change_delegates_the_current_and_new_password_with_the_request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        controller.change(request, new ChangePasswordRequest("OldPass1!", "NewPass1!"));
        verify(service).changePassword(request, "OldPass1!", "NewPass1!");
    }
}
