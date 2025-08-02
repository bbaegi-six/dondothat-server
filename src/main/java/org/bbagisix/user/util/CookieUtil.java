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
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 즉시 만료
        response.addCookie(cookie);
        
        response.addHeader("Set-Cookie",
            JWT_COOKIE_NAME + "=" + 
            "; Path=/" + 
            "; Max-Age=0" + 
            "; HttpOnly" +
            "; SameSite=Lax" +
            "; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
            
        response.addHeader("Set-Cookie",
            JWT_COOKIE_NAME + "=deleted" + 
            "; Path=/" + 
            "; Max-Age=0" + 
            "; HttpOnly" +
            "; SameSite=Lax");
            
        System.out.println("쿠키 삭제 요청 전송됨: " + JWT_COOKIE_NAME);
    }
}
