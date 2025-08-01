package org.bbagisix.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OAuth2Controller {

    @GetMapping("/oauth2-success")
    public String oauth2Success() {
        return "OAuth2 login successful. JWT token is set in cookie.";
    }
}