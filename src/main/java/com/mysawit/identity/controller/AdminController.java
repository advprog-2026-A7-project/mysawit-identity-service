package com.mysawit.identity.controller;

import com.mysawit.identity.dto.AssignBuruhRequest;
import com.mysawit.identity.dto.MessageResponse;
import com.mysawit.identity.dto.UserDetailResponse;
import com.mysawit.identity.enums.Role;
import com.mysawit.identity.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<List<UserDetailResponse>> listUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Role role
    ) {
        List<UserDetailResponse> users = adminService.listUsers(name, email, role);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailResponse> getUser(@PathVariable String userId) {
        UserDetailResponse user = adminService.getUser(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{buruhId}/assign-mandor")
    public ResponseEntity<MessageResponse> assignBuruhToMandor(
            @PathVariable String buruhId,
            @Valid @RequestBody AssignBuruhRequest request
    ) {
        adminService.assignBuruhToMandor(buruhId, request.getMandorId());
        return ResponseEntity.ok(new MessageResponse("Buruh assigned to Mandor successfully"));
    }

    @PutMapping("/{buruhId}/unassign-mandor")
    public ResponseEntity<MessageResponse> unassignBuruhFromMandor(@PathVariable String buruhId) {
        adminService.unassignBuruhFromMandor(buruhId);
        return ResponseEntity.ok(new MessageResponse("Buruh unassigned from Mandor successfully"));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<MessageResponse> deleteUser(
            Authentication authentication,
            @PathVariable String userId
    ) {
        String adminId = authentication.getName();
        adminService.deleteUser(adminId, userId);
        return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
    }
}
