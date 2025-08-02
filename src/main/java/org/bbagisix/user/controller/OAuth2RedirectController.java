package org.bbagisix.user.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class OAuth2RedirectController {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2RedirectController.class);

    @GetMapping("/oauth2/authorization/google")
    public void googleLogin(@RequestParam(required = false) String redirectUrl, 
                           HttpServletRequest request, 
                           HttpServletResponse response) throws IOException {
        
        // 리다이렉트 URL 저장
        if (redirectUrl != null) {
            request.getSession().setAttribute("OAUTH2_ORIGINAL_URL", redirectUrl);
            logger.info("OAuth2 세션에 원본 URL 저장: {}", redirectUrl);
        } else {
            // Referer에서 추출
            String referer = request.getHeader("Referer");
            if (referer != null) {
                request.getSession().setAttribute("OAUTH2_ORIGINAL_URL", referer);
                logger.info("OAuth2 세션에 Referer URL 저장: {}", referer);
            }
        }
        
        // Spring Security OAuth2 엔드포인트로 리다이렉트
        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/oauth2/authorization/naver")
    public void naverLogin(@RequestParam(required = false) String redirectUrl, 
                          HttpServletRequest request, 
                          HttpServletResponse response) throws IOException {
        
        // 리다이렉트 URL 저장
        if (redirectUrl != null) {
            request.getSession().setAttribute("OAUTH2_ORIGINAL_URL", redirectUrl);
            logger.info("OAuth2 세션에 원본 URL 저장: {}", redirectUrl);
        } else {
            // Referer에서 추출
            String referer = request.getHeader("Referer");
            if (referer != null) {
                request.getSession().setAttribute("OAUTH2_ORIGINAL_URL", referer);
                logger.info("OAuth2 세션에 Referer URL 저장: {}", referer);
            }
        }
        
        // Spring Security OAuth2 엔드포인트로 리다이렉트
        response.sendRedirect("/oauth2/authorization/naver");
    }
}
