package org.bbagisix.user.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OAuth2RedirectService {
    
    private static final String OAUTH2_ORIGINAL_URL_KEY = "OAUTH2_ORIGINAL_URL";
    private static final String OAUTH2_BASE_PATH = "/oauth2/authorization/";

    public String prepareOAuth2Login(HttpServletRequest request, String provider, String redirectUrl) {
        log.info("OAuth2 로그인 준비 시작 - provider: {}", provider);
        
        // 원본 URL 저장
        saveOriginalUrl(request, redirectUrl);
        
        // OAuth2 리다이렉트 URL 생성
        String oauth2RedirectUrl = OAUTH2_BASE_PATH + provider;
        
        log.info("OAuth2 리다이렉트 URL 생성: {}", oauth2RedirectUrl);
        return oauth2RedirectUrl;
    }

    private void saveOriginalUrl(HttpServletRequest request, String redirectUrl) {
        HttpSession session = request.getSession();
        
        if (redirectUrl != null && !redirectUrl.trim().isEmpty()) {
            session.setAttribute(OAUTH2_ORIGINAL_URL_KEY, redirectUrl);
            log.info("OAuth2 세션에 요청 URL 저장: {}", redirectUrl);
        } else {
            // Referer에서 추출
            String referer = request.getHeader("Referer");
            if (referer != null && !referer.trim().isEmpty()) {
                session.setAttribute(OAUTH2_ORIGINAL_URL_KEY, referer);
                log.info("OAuth2 세션에 Referer URL 저장: {}", referer);
            } else {
                log.info("OAuth2 원본 URL이 없음 - 기본 페이지로 이동");
            }
        }
    }

    public String getOriginalUrl(HttpSession session) {
        String originalUrl = (String) session.getAttribute(OAUTH2_ORIGINAL_URL_KEY);
        log.info("OAuth2 세션에서 원본 URL 조회: {}", originalUrl);
        return originalUrl;
    }

    public void clearOriginalUrl(HttpSession session) {
        session.removeAttribute(OAUTH2_ORIGINAL_URL_KEY);
        log.info("OAuth2 세션에서 원본 URL 제거");
    }
}
