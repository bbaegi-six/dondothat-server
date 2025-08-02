package org.bbagisix.user.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbagisix.user.service.OAuth2RedirectService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class OAuth2RedirectController {

    private final OAuth2RedirectService oauth2RedirectService;

    @GetMapping("/oauth2/authorization/google")
    public void googleLogin(@RequestParam(required = false) String redirectUrl, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(oauth2RedirectService.prepareOAuth2Login(request, "google", redirectUrl));
    }

    @GetMapping("/oauth2/authorization/naver")
    public void naverLogin(@RequestParam(required = false) String redirectUrl, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(oauth2RedirectService.prepareOAuth2Login(request, "naver", redirectUrl));
    }
}
