package com.ada.proj.service;

import com.ada.proj.dto.StudyGroupCreateRequest;
import com.ada.proj.dto.StudyJoinRequestResponse;
import com.ada.proj.entity.Role;
import com.ada.proj.entity.User;
import com.ada.proj.enums.GroupVisibility;
import com.ada.proj.repository.StudyGroupMemberRepository;
import com.ada.proj.repository.StudyGroupJoinRequestRepository;
import com.ada.proj.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class StudyGroupServiceIntegrationTests {

    @Autowired
    StudyGroupService studyGroupService;
    @Autowired
    StudyGroupMemberRepository memberRepository;
    @Autowired
    StudyGroupJoinRequestRepository joinRequestRepository;
    @Autowired
    UserRepository userRepository;

    private User createUser(String roleName) {
        Role role = Role.valueOf(roleName);
        User u = User.builder()
                .uuid(UUID.randomUUID().toString())
                .adminId("admin_" + System.nanoTime())
                .customId("custom_" + System.nanoTime())
                .password("$2a$10$dummyhashdummyhashdummyhashdummyha")
                .userRealname("실명")
                // 닉네임 길이 제한을 고려해 짧고 유니크한 값 사용 (최대 16자 내)
                .userNickname(generateShortNickname())
                .role(role)
                .useNickname(true)
                .build();
        return userRepository.save(Objects.requireNonNull(u));
    }

    private void setAuth(String userUuid, String role) {
        org.springframework.security.core.Authentication auth
                = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userUuid,
                        "",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                );
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void clearAuth() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private String generateShortNickname() {
        String hex = Long.toHexString(System.nanoTime());
        int target = 7;
        if (hex.length() < target) {
            hex = hex + "0".repeat(target - hex.length());
        } else if (hex.length() > target) {
            hex = hex.substring(0, target);
        }
        return "n" + hex;
    }

    @Test
    @DisplayName("참가요청 생성→목록→승인→멤버 추가 흐름")
    void joinRequestApproveFlow() {
        // 준비: 방장, 신청자 생성
        User owner = createUser("STUDENT");
        User applicant = createUser("STUDENT");

        // 그룹 생성
        StudyGroupCreateRequest req = new StudyGroupCreateRequest();
        req.setName("테스트 스터디");
        req.setDescription("설명");
        req.setTechTags("java,spring");
        req.setVisibility(GroupVisibility.PUBLIC);
        req.setCapacity(5);
        String groupUuid = studyGroupService.create(req, owner.getUuid());
        assertThat(groupUuid).isNotBlank();

        // 참가요청 생성 (신청자)
        studyGroupService.join(groupUuid, applicant.getUuid());
        assertThat(joinRequestRepository.findByGroup_GroupUuidAndUserUuid(groupUuid, applicant.getUuid())).isPresent();

        // 요청 목록 확인 (방장 권한)
        setAuth(owner.getUuid(), owner.getRole().name());
        List<StudyJoinRequestResponse> pending = studyGroupService.listPendingRequests(groupUuid);
        assertThat(pending).extracting(StudyJoinRequestResponse::getUserUuid).contains(applicant.getUuid());

        // 승인 처리 (방장 권한)
        studyGroupService.approveRequest(groupUuid, applicant.getUuid());
        clearAuth();

        // 멤버 추가 확인
        assertThat(memberRepository.existsByGroup_GroupUuidAndUserUuid(groupUuid, applicant.getUuid())).isTrue();
        // 요청 상태가 더 이상 PENDING 아님
        var jr = joinRequestRepository.findByGroup_GroupUuidAndUserUuid(groupUuid, applicant.getUuid()).orElseThrow();
        assertThat(jr.getStatus().name()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("참가요청 취소 흐름")
    void joinRequestCancelFlow() {
        User owner = createUser("STUDENT");
        User applicant = createUser("STUDENT");

        StudyGroupCreateRequest req = new StudyGroupCreateRequest();
        req.setName("취소 플로우");
        req.setVisibility(GroupVisibility.PUBLIC);
        req.setCapacity(5);
        String groupUuid = studyGroupService.create(req, owner.getUuid());

        // 참가요청 생성
        studyGroupService.join(groupUuid, applicant.getUuid());
        assertThat(joinRequestRepository.findByGroup_GroupUuidAndUserUuid(groupUuid, applicant.getUuid())).isPresent();

        // 본인 취소
        setAuth(applicant.getUuid(), applicant.getRole().name());
        studyGroupService.cancelMyRequest(groupUuid, applicant.getUuid());
        clearAuth();
        var jr = joinRequestRepository.findByGroup_GroupUuidAndUserUuid(groupUuid, applicant.getUuid()).orElseThrow();
        assertThat(jr.getStatus().name()).isEqualTo("CANCELED");
    }
} 
