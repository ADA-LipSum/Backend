package com.ada.proj.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.ada.proj.config.CookieProperties;
import com.ada.proj.dto.LoginResponse;
import com.ada.proj.dto.TokenReissueRequest;
import com.ada.proj.entity.RefreshToken;
import com.ada.proj.repository.RefreshTokenRepository;
import com.ada.proj.service.AuthService;
import com.ada.proj.service.UserService;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerReissueTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    @MockBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockBean
    private CookieProperties cookieProperties;

    @Test
    void reissue_usesCookieRefreshToken_whenBodyMissing() throws Exception {
        when(cookieProperties.isHttpOnly()).thenReturn(true);
        when(cookieProperties.isSecure()).thenReturn(false);
        when(cookieProperties.getSameSite()).thenReturn("Lax");
        when(cookieProperties.getMaxAge()).thenReturn(60L);

        when(authService.reissue(any(TokenReissueRequest.class)))
                .thenReturn(LoginResponse.builder().uuid("u1").tokenType("Bearer").accessToken("newAccess").expiresIn(900_000).build());

        when(refreshTokenRepository.findByUuid("u1"))
                .thenReturn(Optional.of(RefreshToken.builder()
                        .uuid("u1")
                        .token("newRefresh")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .build()));

        mockMvc.perform(post("/auth/reissue")
                .cookie(new Cookie("refreshToken", "cookieRefresh")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refreshToken=newRefresh")));

        ArgumentCaptor<TokenReissueRequest> captor = ArgumentCaptor.forClass(TokenReissueRequest.class);
        verify(authService).reissue(captor.capture());
        assertThat(captor.getValue().getRefreshToken()).isEqualTo("cookieRefresh");
    }

    @Test
    void reissue_usesCookieRefreshToken_whenBodyIsEmptyJson() throws Exception {
        when(cookieProperties.isHttpOnly()).thenReturn(true);
        when(cookieProperties.isSecure()).thenReturn(false);
        when(cookieProperties.getSameSite()).thenReturn("Lax");
        when(cookieProperties.getMaxAge()).thenReturn(60L);

        when(authService.reissue(any(TokenReissueRequest.class)))
                .thenReturn(LoginResponse.builder().uuid("u1").tokenType("Bearer").accessToken("newAccess").expiresIn(900_000).build());

        when(refreshTokenRepository.findByUuid("u1"))
                .thenReturn(Optional.of(RefreshToken.builder()
                        .uuid("u1")
                        .token("newRefresh")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .build()));

        mockMvc.perform(post("/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .cookie(new Cookie("refreshToken", "cookieRefresh")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refreshToken=newRefresh")));

        ArgumentCaptor<TokenReissueRequest> captor = ArgumentCaptor.forClass(TokenReissueRequest.class);
        verify(authService).reissue(captor.capture());
        assertThat(captor.getValue().getRefreshToken()).isEqualTo("cookieRefresh");
    }

    @Test
    void reissue_usesAuthorizationBearer_whenCookieMissingAndBodyMissing() throws Exception {
        when(cookieProperties.isHttpOnly()).thenReturn(true);
        when(cookieProperties.isSecure()).thenReturn(false);
        when(cookieProperties.getSameSite()).thenReturn("Lax");
        when(cookieProperties.getMaxAge()).thenReturn(60L);

        when(authService.reissue(any(TokenReissueRequest.class)))
                .thenReturn(LoginResponse.builder().uuid("u1").tokenType("Bearer").accessToken("newAccess").expiresIn(900_000).build());

        when(refreshTokenRepository.findByUuid("u1"))
                .thenReturn(Optional.of(RefreshToken.builder()
                        .uuid("u1")
                        .token("newRefresh")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .build()));

        mockMvc.perform(post("/auth/reissue")
                .header("Authorization", "Bearer headerRefresh"))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refreshToken=newRefresh")));

        ArgumentCaptor<TokenReissueRequest> captor = ArgumentCaptor.forClass(TokenReissueRequest.class);
        verify(authService).reissue(captor.capture());
        assertThat(captor.getValue().getRefreshToken()).isEqualTo("headerRefresh");
    }

    @Test
    void reissue_returns400_whenNoCookieHeaderOrBody() throws Exception {
        mockMvc.perform(post("/auth/reissue"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MISSING_REFRESH")));

        verify(authService, never()).reissue(any(TokenReissueRequest.class));
    }
}
