package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "ProfileLinksRequest", description = "프로필 외부 링크/연동 정보")
public class ProfileLinksRequest {
    @Schema(description = "GitHub 프로필 URL", example = "https://github.com/username")
    private String github;

    @Schema(description = "Discord 프로필 URL", example = "https://discord.com/users/12345/user")
    private String discord;

    @Schema(description = "linked_in 프로필 URL", example = "https://www.solved_ac.com/in/username/")
    private String linked_in;

    @Schema(description = "GitHub 연동 여부", example = "true")
    private Boolean githubConnected;

    @Schema(description = "StackOverflow 연동 여부", example = "false")
    private Boolean discordConnected;

    @Schema(description = "LinkedIn 연동 여부", example = "true")
    private Boolean linkedinConnected;
}
