package com.ada.proj.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.Cacheable;

import com.ada.proj.dto.CreateCustomLoginRequest;
import com.ada.proj.dto.CreateUserRequest;
import com.ada.proj.dto.UpdatePasswordRequest;
import com.ada.proj.dto.UpdateProfileRequest;
import com.ada.proj.dto.UserProfileResponse;
import com.ada.proj.enums.Role;
import com.ada.proj.entity.User;
import com.ada.proj.entity.UserData;
import com.ada.proj.exception.ForbiddenException;
import com.ada.proj.exception.UnauthenticatedException;
import com.ada.proj.exception.UserNotFoundException;
import com.ada.proj.repository.UserDataRepository;
import com.ada.proj.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserDataRepository userDataRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public UserService(UserRepository userRepository,
            UserDataRepository userDataRepository,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.userDataRepository = userDataRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    public List<User> listUsers(Role role, String query) {
        return userRepository.search(role, query);
    }

    @Cacheable(cacheNames = "users", key = "#uuid")
    public UserProfileResponse getUserProfile(String uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        Optional<UserData> userDataOpt = userDataRepository.findByUuid(uuid);
        UserData ud = userDataOpt.orElse(null);

        List<String> techList = null;
        if (ud != null && ud.getTechStack() != null) {
            techList = parseTechStack(ud.getTechStack());
        }

        return UserProfileResponse.builder()
                .uuid(user.getUuid())
                .adminId(user.getAdminId())
                .customId(user.getCustomId())
                .userRealname(user.getUserRealname())
                .userNickname(user.getUserNickname())
                .useNickname(user.isUseNickname())
                .profileImage(user.getProfileImage())
                .profileBanner(user.getProfileBanner())
                .role(user.getRole())
                .intro(ud == null ? null : ud.getIntro())
                .techStack(techList)
                .badge(ud == null ? null : ud.getBadge())
                .activityScore(ud == null ? null : ud.getActivityScore())
                .contributionData(ud == null ? null : ud.getContributionData())
                .build();
    }

    public void updateRole(String uuid, Role role) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setRole(role);
    }

    @CacheEvict(cacheNames = "users", key = "#uuid")
    public void toggleUseNickname(String uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setUseNickname(!user.isUseNickname());
    }

    @CacheEvict(cacheNames = "users", key = "#uuid")
    public void updateProfile(String uuid, UpdateProfileRequest req) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (req.getNickname() != null) {
            user.setUserNickname(req.getNickname());
        }
        if (req.getProfileImage() != null) {
            user.setProfileImage(req.getProfileImage());
        }
        if (req.getProfileBanner() != null) {
            user.setProfileBanner(req.getProfileBanner());
        }

        // upsert user_data
        UserData ud = userDataRepository.findByUuid(uuid).orElseGet(() -> {
            UserData created = new UserData();
            created.setUuid(uuid);
            return created;
        });

        if (req.getIntro() != null) {
            ud.setIntro(req.getIntro());
        }
        if (req.getTechStack() != null) {
            ud.setTechStack(serializeTechStack(req.getTechStack()));
        }

        // persist if new or changed
        if (ud.getSeq() == null) {
            userDataRepository.save(ud);
        }
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "users", key = "#uuid"),
        @CacheEvict(cacheNames = "users", key = "'custom:' + #req.customId")
    })
    public void createCustomLogin(String uuid, CreateCustomLoginRequest req, Authentication auth) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        // 자신 또는 관리자만 허용
        ensureSelfOrAdmin(auth, uuid);
        if (user.getCustomId() != null) {
            throw new IllegalStateException("Custom ID already set");
        }
        if (userRepository.existsByCustomId(req.getCustomId())) {
            throw new IllegalArgumentException("Custom ID already exists");
        }
        user.setCustomId(req.getCustomId());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "users", key = "#result.uuid", condition = "#result != null"),
        @CacheEvict(cacheNames = "users", key = "'admin:' + #req.adminId"),
        @CacheEvict(cacheNames = "users", key = "'custom:' + #req.customId", condition = "#req.customId != null")
    })
    public User createUserByAdmin(CreateUserRequest req, Authentication auth) {
        ensureAdmin(auth);

        if (userRepository.findByAdminId(req.getAdminId()).isPresent()) {
            throw new IllegalArgumentException("adminId already exists");
        }

        if (req.getCustomId() != null && userRepository.existsByCustomId(req.getCustomId())) {
            throw new IllegalArgumentException("customId already exists");
        }

        User user = User.builder()
                .uuid(java.util.UUID.randomUUID().toString())
                .adminId(req.getAdminId())
                .customId(req.getCustomId())
                .password(req.getPassword() == null ? null : passwordEncoder.encode(req.getPassword()))
                .userRealname(req.getUserRealname())
                .userNickname(req.getUserNickname())
                .role(req.getRole() == null ? Role.STUDENT : req.getRole())
                .profileImage(req.getProfileImage())
                .profileBanner(req.getProfileBanner())
                .build();

        forceApplyIdenticonProfileImage(user);
        user = userRepository.save(Objects.requireNonNull(user));

        if (req.getIntro() != null || req.getTechStack() != null) {
            UserData ud = new UserData();
            ud.setUuid(user.getUuid());
            if (req.getIntro() != null) {
                ud.setIntro(req.getIntro());
            }
            if (req.getTechStack() != null) {
                ud.setTechStack(serializeTechStack(req.getTechStack()));
            }
            userDataRepository.save(ud);
        }

        return user;
    }

    /**
     * Create the very first ADMIN account when none exists. This can be called
     * without authentication but will fail if any ADMIN already exists.
     */
    @Caching(evict = {
        @CacheEvict(cacheNames = "users", key = "#result.uuid", condition = "#result != null"),
        @CacheEvict(cacheNames = "users", key = "'admin:' + #req.adminId"),
        @CacheEvict(cacheNames = "users", key = "'custom:' + #req.customId", condition = "#req.customId != null")
    })
    public User createInitialAdmin(CreateUserRequest req) {
        // if any ADMIN exists, disallow unauthenticated init
        if (userRepository.existsByRole(Role.ADMIN)) {
            throw new ForbiddenException("Admin already exists");
        }

        if (userRepository.findByAdminId(req.getAdminId()).isPresent()) {
            throw new IllegalArgumentException("adminId already exists");
        }

        User user = User.builder()
                .uuid(java.util.UUID.randomUUID().toString())
                .adminId(req.getAdminId())
                .customId(req.getCustomId())
                .password(req.getPassword() == null ? null : passwordEncoder.encode(req.getPassword()))
                .userRealname(req.getUserRealname())
                .userNickname(req.getUserNickname())
                .role(Role.ADMIN)
                .profileImage(req.getProfileImage())
                .profileBanner(req.getProfileBanner())
                .build();

        forceApplyIdenticonProfileImage(user);
        user = userRepository.save(Objects.requireNonNull(user));

        if (req.getIntro() != null || req.getTechStack() != null) {
            UserData ud = new UserData();
            ud.setUuid(user.getUuid());
            if (req.getIntro() != null) {
                ud.setIntro(req.getIntro());
            }
            if (req.getTechStack() != null) {
                ud.setTechStack(serializeTechStack(req.getTechStack()));
            }
            userDataRepository.save(ud);
        }

        return user;
    }

    private void applyDefaultIdenticonProfileImageIfMissing(User user) {
        applyIdenticonProfileImage(user, false);
    }

    private void forceApplyIdenticonProfileImage(User user) {
        applyIdenticonProfileImage(user, true);
    }

    private void applyIdenticonProfileImage(User user, boolean forceApply) {
        if (user == null) {
            return;
        }
        String profileImage = user.getProfileImage();
        if (!forceApply && profileImage != null && !profileImage.isBlank()) {
            return;
        }
        String uuid = user.getUuid();
        String seed = uuid == null
                ? java.util.UUID.randomUUID().toString()
                : uuid.replace("-", "");
        String encodedSeed = URLEncoder.encode(seed, StandardCharsets.UTF_8);
        String url = "https://api.dicebear.com/9.x/identicon/svg?seed=" + encodedSeed;
        user.setProfileImage(url);
    }

    private void ensureAdmin(Authentication auth) {
        if (auth == null) {
            throw new UnauthenticatedException("Unauthenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new ForbiddenException("Forbidden");
        }
    }

    @CacheEvict(cacheNames = "users", key = "#uuid")
    public void changeCustomPassword(String uuid, UpdatePasswordRequest req, Authentication auth) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        ensureSelfOrAdmin(auth, uuid);
        if (user.getPassword() == null || !passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password does not match");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
    }

    private void ensureSelfOrAdmin(Authentication auth, String uuid) {
        if (auth == null) {
            throw new UnauthenticatedException("Unauthenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        String principal = auth.getName();
        if (!isAdmin && !uuid.equals(principal)) {
            throw new ForbiddenException("Forbidden");
        }
    }

    private List<String> parseTechStack(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        try {
            String s = stored.trim();
            if (s.startsWith("[")) {
                // JSON array
                return objectMapper.readValue(s, new TypeReference<List<String>>() {
                });
            }
            // CSV fallback
            List<String> list = new ArrayList<>();
            for (String it : s.split(",")) {
                String v = it.trim();
                if (!v.isEmpty()) {
                    list.add(v);
                }
            }
            return list.isEmpty() ? null : list;
        } catch (IOException e) {
            return null;
        }
    }

    private String serializeTechStack(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public boolean isStudent(String uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return user.getRole() == Role.STUDENT;
    }
}
