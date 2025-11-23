package com.ada.proj.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.ada.proj.dto.UpdateProfileRequest;
import com.ada.proj.entity.User;
import com.ada.proj.entity.UserData;
import com.ada.proj.repository.UserDataRepository;
import com.ada.proj.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;

@SuppressWarnings("removal")
@SpringBootTest
public class UserServiceCacheTest {

    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserDataRepository userDataRepository;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void getUserProfile_is_cached_and_updateProfile_eviction_works() {
        String uuid = "user-1";
        User u = new User();
        u.setUuid(uuid);
        u.setAdminId("adm1");
        u.setCustomId(null);
        u.setUserRealname("Real");
        u.setUserNickname("Nick");
        u.setProfileImage("img.png");

        UserData ud = new UserData();
        ud.setUuid(uuid);
        ud.setIntro("hi");

        when(userRepository.findByUuid(uuid)).thenReturn(Optional.of(u));
        when(userDataRepository.findByUuid(uuid)).thenReturn(Optional.of(ud));

        // ensure cache empty
        var cache = cacheManager.getCache("users");
        if (cache != null) cache.clear();

        // first call -> hits repository
        userService.getUserProfile(uuid);
        // second call -> served from cache (no additional repository call)
        userService.getUserProfile(uuid);

        verify(userRepository, times(1)).findByUuid(uuid);

        // now update profile -> should evict (updateProfile itself loads user)
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setNickname("NewNick");
        userService.updateProfile(uuid, req);
        verify(userRepository, times(2)).findByUuid(uuid);

        // next call -> repository should be called again due to eviction
        userService.getUserProfile(uuid);
        verify(userRepository, times(3)).findByUuid(uuid);
    }
}
