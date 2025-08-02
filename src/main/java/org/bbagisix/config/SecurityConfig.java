package org.bbagisix.config;

import java.util.Arrays;

import org.bbagisix.user.filter.JWTFilter;
import org.bbagisix.user.handler.CustomOAuth2SuccessHandler;
import org.bbagisix.user.service.CustomOAuth2UserService;
import org.bbagisix.user.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@PropertySource("classpath:application.properties")
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	private final CustomOAuth2UserService customOAuth2UserService;
	private final Environment environment;
	private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
	private final JwtUtil jwtUtil;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		
		// CORS 설정 적용
		http.cors();
		
		// csrf disable
		http.csrf().disable();

		// Form 로그인 방식 disable
		http.formLogin().disable();

		// HTTP basic 인증 방식 disable
		http.httpBasic().disable();

		// 세션 설정 (OAuth2에서는 세션 필요)
		http.sessionManagement()
			.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);

		// 경로별 인가 작업
		http.authorizeRequests()
			// 정적 리소스 허용
			.antMatchers("/", "/error", "/resources/**", "/static/**", "/css/**", "/js/**", "/images/**").permitAll()
			// OAuth2 관련 경로 허용
			.antMatchers("/oauth2-login", "/oauth2-success", "/oauth2/**", "/login/oauth2/**").permitAll()
			// API 회원가입/로그인 경로 허용
			.antMatchers("/api/user/signup", "/api/user/login", "/api/user/send-verification", 
						"/api/user/check-email", "/api/user/check-nickname").permitAll()
			// 디버그 경로 허용 (개발용)
			.antMatchers("/debug/**").permitAll()
			// 나머지는 인증 필요
			.anyRequest().authenticated();

		http.addFilterBefore(new JWTFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

		// OAuth2 로그인 설정
		http.oauth2Login()
			.clientRegistrationRepository(clientRegistrationRepository(environment))
			.userInfoEndpoint()
				.userService(customOAuth2UserService)
			.and()
			.successHandler(customOAuth2SuccessHandler)
			.failureUrl("/oauth2-login?error");
	}

	@Bean
	public static ClientRegistrationRepository clientRegistrationRepository(Environment environment) {
		return new InMemoryClientRegistrationRepository(
			googleClientRegistration(environment),
			naverClientRegistration(environment)
		);
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "https://dondothat.netlify.app", "http://dondothat.duckdns.org:8080"));
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList("*"));
		configuration.setAllowCredentials(true); // 쿠키 전송 허용

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 위 설정 적용
		return source;
	}

	@Bean
	public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
		return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
	}

	private static ClientRegistration googleClientRegistration(Environment environment) {
		return ClientRegistration.withRegistrationId("google")
			.clientId(environment.getProperty("GOOGLE_CLIENT_ID"))
			.clientSecret(environment.getProperty("GOOGLE_CLIENT_SECRET"))
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
			.scope("profile", "email")
			.authorizationUri("https://accounts.google.com/o/oauth2/auth")
			.tokenUri("https://oauth2.googleapis.com/token")
			.userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
			.userNameAttributeName("email")
			.clientName("Google")
			.build();
	}

	private static ClientRegistration naverClientRegistration(Environment environment) {
		return ClientRegistration.withRegistrationId("naver")
			.clientId(environment.getProperty("NAVER_CLIENT_ID"))
			.clientSecret(environment.getProperty("NAVER_CLIENT_SECRET"))
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
			.scope("name", "email")
			.authorizationUri("https://nid.naver.com/oauth2.0/authorize")
			.tokenUri("https://nid.naver.com/oauth2.0/token")
			.userInfoUri("https://openapi.naver.com/v1/nid/me")
			.userNameAttributeName("response")
			.clientName("Naver")
			.build();
	}
}
