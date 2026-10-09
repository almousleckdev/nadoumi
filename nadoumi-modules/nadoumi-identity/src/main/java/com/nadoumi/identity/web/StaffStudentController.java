package com.nadoumi.identity.web;

import com.nadoumi.identity.service.StudentRemovalService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/students")
public class StaffStudentController {

    private final StudentRemovalService removal;

    public StaffStudentController(StudentRemovalService removal) {
        this.removal = removal;
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@ss.hasPermi('nad:student:delete')")
    @Log(title = "Student account", businessType = BusinessType.DELETE)
    public void delete(@PathVariable Long userId) {
        removal.remove(userId);
    }
}
