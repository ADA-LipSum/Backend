package com.ada.proj.dto;

import com.ada.proj.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "UpdateRoleRequest", description = "권한 변경 요청 바디")
public class UpdateRoleRequest {
    @NotNull
    @Schema(description = "변경할 권한", example = "TEACHER")
    private Role role;
}
