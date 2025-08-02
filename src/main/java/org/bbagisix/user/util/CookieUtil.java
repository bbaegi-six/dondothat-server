package org.bbagisix.user.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class CookieUtil {
    
    private static final String JWT_COOKIE_NAME = "accessToken";
    private static final int COOKIE_MAX_AGE = 24 * 60 * 60; // 24시간 (초 단위)

    public static void addJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, token);
        cookie.setHttpOnly(true); // XSS 공격 방지
        cookie.setSecure(false); // 개발환경에서는 false, 운영환경에서는 true로 변경
        cookie.setPath("/"); // 모든 경로에서 쿠키 전송
        cookie.setMaxAge(COOKIE_MAX_AGE); // 24시간
        
        // SameSite 속성 설정 (CSRF 공격 방지)
        response.setHeader("Set-Cookie", 
            cookie.getName() + "=" + cookie.getValue() + 
            "; Path=" + cookie.getPath() + 
            "; Max-Age=" + cookie.getMaxAge() + 
            "; HttpOnly" +
            (cookie.getSecure() ? "; Secure" : "") +
            "; SameSite=Lax");
    }

    public static void deleteJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 즉시 만료
        
        response.setHeader("Set-Cookie", 
            cookie.getName() + "=" + 
            "; Path=" + cookie.getPath() + 
            "; Max-Age=0" + 
            "; HttpOnly" +
            (cookie.getSecure() ? "; Secure" : "") +
            "; SameSite=Lax");
    }
}
