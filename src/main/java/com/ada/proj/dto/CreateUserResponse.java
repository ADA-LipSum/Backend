package com.ada.proj.dto;

import com.ada.proj.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateUserResponse {
    private String uuid;
    private String adminId;
    private String customId;
    private Role role;
}
