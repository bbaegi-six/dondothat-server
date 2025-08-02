package org.bbagisix.user.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbagisix.user.dto.CustomOAuth2User;
import org.bbagisix.user.dto.SignUpResponse;
import org.bbagisix.user.util.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws
		ServletException, IOException {

		String authorizationHeader = request.getHeader("Authorization");

		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer")) {
			filterChain.doFilter(request, response);

			return;
		}

		String token = authorizationHeader.split(" ")[1];

		if (jwtUtil.isExpired(token)) {
			System.out.println("token expired");
			filterChain.doFilter(request, response);
			return;
		}

		String name = jwtUtil.getName(token);
		String role = jwtUtil.getRole(token);
		String email = jwtUtil.getEmail(token);
		String nickname = jwtUtil.getNickname(token);

		SignUpResponse signUpResponse = SignUpResponse.builder()
			.name(name)
			.nickname(nickname)
			.email(email)
			.role(role)
			.assetConnected(false)
			.build();

		CustomOAuth2User customOAuth2User = new CustomOAuth2User(signUpResponse);

		Authentication authToken = new UsernamePasswordAuthenticationToken(customOAuth2User, null,
			customOAuth2User.getAuthorities());

		SecurityContextHolder.getContext().setAuthentication(authToken);
		filterChain.doFilter(request, response);
	}
}
