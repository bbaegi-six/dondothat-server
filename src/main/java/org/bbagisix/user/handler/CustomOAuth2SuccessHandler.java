package org.bbagisix.user.handler;

import lombok.RequiredArgsConstructor;

import org.bbagisix.user.dto.CustomOAuth2User;
import org.bbagisix.user.util.JwtUtil;
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

    private final JwtUtil jwtUtil;

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
            logger.info("CustomOAuth2SuccessHandler: 추출된 사용자 정보 - Email: {}, Role: {}, Name: {}, Nickname: {}", email,
                role, name, nickname);
            if (email == null || role == null || name == null || nickname == null) {
                logger.error("CustomOAuth2SuccessHandler: 필수 사용자 정보(email, role, name, nickname) 중 일부가 null입니다.");
            }
            String token = jwtUtil.createToken(email, role, name, nickname, 60 * 60 * 1000L); // 1시간 유효
            logger.info("CustomOAuth2SuccessHandler: JWT 토큰 생성 완료. 토큰 길이: {}", token.length());
            String targetUrl = UriComponentsBuilder.fromUriString(
                    "https://dondothat.netlify.app/oauth-redirect")
                .queryParam("token", token)
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
}
