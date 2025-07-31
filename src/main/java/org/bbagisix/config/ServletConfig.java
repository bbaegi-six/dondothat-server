package org.bbagisix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

@EnableWebMvc // FrontController (DispatcherServlet)
@Configuration
@ComponentScan(basePackages = "org.bbagisix", // 스캔 범위를 최상위로 넓힘
	useDefaultFilters = false,     // 기본 필터는 끄고
	includeFilters = {            // 포함할 필터만 지정
		@ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ANNOTATION, classes = {
			Controller.class, ControllerAdvice.class})})
public class ServletConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/resources/**") // url이 /resources/로 시작하는 모든 경로 (정작 파일 등록)
			.addResourceLocations("/resources/"); // webapp/resources/ 경로로 매핑

	}

	// jsp view resolver 설정
	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		InternalResourceViewResolver bean = new InternalResourceViewResolver();

		bean.setViewClass(JstlView.class);
		bean.setPrefix("/WEB-INF/views/");
		bean.setSuffix(".jsp");

		registry.viewResolver(bean);
	}

	@Bean
	public MultipartResolver multipartResolver() {
		StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
		return resolver;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**").allowedOrigins("http://localhost:5173", // 로컬에서 테스트 할 Vue 개발 서버 주소
			"https://dondothat.netlify.app" // 실제 배포된 프론트엔드 주소
		).allowedMethods("GET", "POST", "PUT", "DELETE").allowCredentials(true).allowedHeaders("*");
	}
}
