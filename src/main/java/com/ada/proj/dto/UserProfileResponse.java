package com.ada.proj.dto;

import com.ada.proj.enums.Role;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UserProfileResponse {

    private String uuid;
    private String adminId;
    private String customId;
    private String userRealname;
    private String userNickname;
    private boolean useNickname;
    private String profileImage;
    private String profileBanner;
    private Role role;

    // user_data
    private String intro;
    private List<String> techStack;
    private String badge;
    private Integer activityScore;
    private String contributionData; // JSON 문자열
}
