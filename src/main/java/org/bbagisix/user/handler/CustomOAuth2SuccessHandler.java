package org.bbagisix.user.handler;

import lombok.RequiredArgsConstructor;

import org.bbagisix.user.dto.CustomOAuth2User;
import org.bbagisix.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2SuccessHandler.class);

    private final UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        try {
            logger.info("CustomOAuth2SuccessHandler: onAuthenticationSuccess 호출됨.");
            CustomOAuth2User oAuth2User = (CustomOAuth2User)authentication.getPrincipal();
            logger.info("CustomOAuth2SuccessHandler: OAuth2User Principal: {}", oAuth2User);
            
            String email = oAuth2User.getEmail();
            String role = oAuth2User.getRole();
            String name = oAuth2User.getName();
            String nickname = oAuth2User.getNickname();
            Long userId = oAuth2User.getUserId();
            
            logger.info("CustomOAuth2SuccessHandler: 추출된 사용자 정보 - Email: {}, Role: {}, Name: {}, Nickname: {}, UserId: {}", 
                email, role, name, nickname, userId);
            
            if (email == null || role == null || name == null || nickname == null || userId == null) {
                logger.error("CustomOAuth2SuccessHandler: 필수 사용자 정보 중 일부가 null입니다.");
                response.sendRedirect("/error?message=missing_user_info");
                return;
            }
            
            // 비즈니스 로직을 Service로 위임
            userService.processOAuth2Login(email, role, name, nickname, userId, response);
            
            // 동적 리다이렉트 URL 결정
            String redirectUrl = determineRedirectUrl(request);
            
            // 쿠키로 토큰을 설정했으므로 URL 파라미터에서는 제거
            String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
            logger.info("CustomOAuth2SuccessHandler: 리다이렉션할 URL: {}", targetUrl);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            logger.info("CustomOAuth2SuccessHandler: 리다이렉션 요청 완료.");
        } catch (Exception e) {
            logger.error("CustomOAuth2SuccessHandler: 인증 성공 처리 중 예외 발생", e);
            response.sendRedirect("/error?message=authentication_success_error");
        }
    }
    
    private String determineRedirectUrl(HttpServletRequest request) {
        // 1. URL 파라미터에서 직접 확인
        String redirectUri = request.getParameter("redirect_uri");
        if (redirectUri != null) {
            logger.info("URL 파라미터에서 리다이렉트 URI 발견: {}", redirectUri);
            return redirectUri;
        }
        
        // 2. 세션에서 원본 URL 확인 (가장 정확)
        String originalUrl = (String) request.getSession().getAttribute("OAUTH2_ORIGINAL_URL");
        if (originalUrl != null) {
            logger.info("세션에서 원본 URL 발견: {}", originalUrl);
            return getRedirectUrlFromOrigin(originalUrl);
        } else {
            logger.info("세션에 OAUTH2_ORIGINAL_URL이 없습니다.");
        }
        
        // 3. Referer 헤더에서 확인
        String referer = request.getHeader("Referer");
        if (referer != null) {
            logger.info("Referer 헤더에서 URL 확인: {}", referer);
            return getRedirectUrlFromOrigin(referer);
        } else {
            logger.info("Referer 헤더가 없습니다.");
        }
        
        // 4. 기본값 (로컬 개발)
        logger.info("기본 리다이렉트 URL 사용");
        return "http://dondothat.netlify.app/oauth-redirect";
    }
    
    private String getRedirectUrlFromOrigin(String originUrl) {
        if (originUrl.contains("localhost:5173") || originUrl.contains("127.0.0.1:5173")) {
            return "http://localhost:5173/oauth-redirect";
        } else if (originUrl.contains("netlify.app")) {
            return "https://dondothat.netlify.app/oauth-redirect";
        } else {
            // 기본값 (로컬 개발 우선)
            return "http://localhost:5173/oauth-redirect";
        }
    }
}
