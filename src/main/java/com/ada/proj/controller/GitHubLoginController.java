package com.ada.proj.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@RestController
public class GitHubLoginController {

    @GetMapping("/api/github/login")
    public ResponseEntity<Void> loginRedirect() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, "/oauth2/authorization/github");
        return ResponseEntity.status(302).headers(headers).build();
    }
}
