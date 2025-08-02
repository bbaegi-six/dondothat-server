package org.bbagisix.user.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbagisix.user.dto.CustomOAuth2User;
import org.bbagisix.user.service.OAuth2RedirectService;
import org.bbagisix.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final OAuth2RedirectService oauth2RedirectService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) 
            throws IOException, ServletException {

        try {
            log.info("OAuth2 로그인 성공 처리 시작");
            
            // 사용자 정보 추출 및 검증
            CustomOAuth2User oAuth2User = extractAndValidateUser(authentication);
            
            // JWT 토큰 생성 및 설정
            userService.processOAuth2Login(
                oAuth2User.getEmail(), 
                oAuth2User.getRole(), 
                oAuth2User.getName(), 
                oAuth2User.getNickname(), 
                oAuth2User.getUserId(), 
                response
            );
            
            // 리다이렉트 URL 결정 및 이동
            String redirectUrl = determineRedirectUrl(request);
            redirectToTarget(request, response, redirectUrl);
            
            log.info("OAuth2 로그인 성공 처리 완료");
            
        } catch (Exception e) {
            log.error("OAuth2 로그인 성공 처리 중 오류 발생", e);
            response.sendRedirect("/error?message=authentication_success_error");
        }
    }
    
    /**
     * 사용자 정보 추출 및 검증
     */
    private CustomOAuth2User extractAndValidateUser(Authentication authentication) {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        
        log.info("OAuth2 사용자 정보 - Email: {}, Name: {}, UserId: {}", 
            oAuth2User.getEmail(), oAuth2User.getName(), oAuth2User.getUserId());
        
        // 필수 정보 검증
        if (oAuth2User.getEmail() == null || oAuth2User.getUserId() == null) {
            throw new IllegalStateException("필수 사용자 정보가 누락되었습니다.");
        }
        
        return oAuth2User;
    }
    
    /**
     * 리다이렉트 URL 결정
     */
    private String determineRedirectUrl(HttpServletRequest request) {
        // 1. URL 파라미터 확인
        String redirectUri = request.getParameter("redirect_uri");
        if (isValidUrl(redirectUri)) {
            log.info("URL 파라미터에서 리다이렉트 URI 발견: {}", redirectUri);
            return redirectUri;
        }
        
        // 2. 세션에서 원본 URL 확인
        String originalUrl = oauth2RedirectService.getOriginalUrl(request.getSession());
        if (isValidUrl(originalUrl)) {
            // 세션 정보 사용 후 제거
            oauth2RedirectService.clearOriginalUrl(request.getSession());
            return getRedirectUrlFromOrigin(originalUrl);
        }
        
        // 3. Referer 헤더 확인
        String referer = request.getHeader("Referer");
        if (isValidUrl(referer)) {
            log.info("Referer 헤더에서 URL 확인: {}", referer);
            return getRedirectUrlFromOrigin(referer);
        }
        
        // 4. 기본값
        log.info("기본 리다이렉트 URL 사용");
        return "https://dondothat.netlify.app/oauth-redirect";
    }
    
    /**
     * URL 유효성 검사
     */
    private boolean isValidUrl(String url) {
        return url != null && !url.trim().isEmpty();
    }
    
    /**
     * 원본 URL에서 적절한 리다이렉트 URL 생성
     */
    private String getRedirectUrlFromOrigin(String originUrl) {
        if (originUrl.contains("localhost:5173") || originUrl.contains("127.0.0.1:5173")) {
            return "http://localhost:5173/oauth-redirect";
        } else if (originUrl.contains("netlify.app")) {
            return "https://dondothat.netlify.app/oauth-redirect";
        } else {
            return "https://dondothat.netlify.app/oauth-redirect";
        }
    }
    
    /**
     * 리다이렉트 실행
     */
    private void redirectToTarget(HttpServletRequest request, HttpServletResponse response, String redirectUrl) 
            throws IOException {
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
            .build()
            .encode(StandardCharsets.UTF_8)
            .toUriString();
            
        log.info("리다이렉트 대상 URL: {}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
