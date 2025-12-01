package com.ada.proj.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.entity.Role;
import com.ada.proj.entity.User;
import com.ada.proj.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping
@Tag(name = "권한 관리", description = "역할 목록 및 역할별 사용자 조회 API")
public class RoleController {

    private final UserService userService;

    public RoleController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/roles")
    @Operation(summary = "역할 목록 조회")
    public ResponseEntity<ApiResponse<List<Role>>> listRoles() {
        List<Role> roles = Arrays.asList(Role.values());
        return ResponseEntity.ok(ApiResponse.ok(roles));
    }

    @GetMapping("/roles/{role}")
    @Operation(summary = "역할별 유저 조회")
    public ResponseEntity<ApiResponse<List<User>>> listUsersByRole(@PathVariable("role") Role role) {
        List<User> users = userService.listUsers(role, null);
        return ResponseEntity.ok(ApiResponse.ok(users));
    }
}
