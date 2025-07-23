package org.bbagisix.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;

import javax.sql.DataSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@PropertySource({"classpath:/application.properties"})
@MapperScan(basePackages = {"org.bbagisix.**.mapper"})
@ComponentScan(
	basePackages = "org.bbagisix", // 스캔 범위는 동일
	excludeFilters = {            // 제외할 필터를 지정
		@ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ANNOTATION, classes = {
			Controller.class, ControllerAdvice.class})
	}
)
public class RootConfig {
	private static final Logger log = LogManager.getLogger(RootConfig.class);
	@Value("${jdbc.driver}")
	String driver;

	// docker-compose.yml로부터 환경 변수 주입
	@Value("${DB_HOST:localhost}")
	String dbHost;
	@Value("${DB_PORT:3306}")
	String dbPort;
	@Value("${DB_NAME:dondothat}")
	String dbName;
	@Value("${DB_USER:root}")
	String username;
	@Value("${DB_PASSWORD:1234}")
	String password;

	@Bean
	public DataSource dataSource() {
		HikariConfig config = new HikariConfig();

		// 환경 변수를 사용하여 JDBC URL 구성
		String jdbcUrl = String.format("jdbc:log4jdbc:mysql://%s:%s/%s", dbHost, dbPort, dbName);

		config.setDriverClassName(driver);
		config.setJdbcUrl(jdbcUrl);
		config.setUsername(username);
		config.setPassword(password);

		// MySQL 최적화 설정
		config.setMaximumPoolSize(10);
		config.setMinimumIdle(5);
		config.setConnectionTimeout(30000);
		config.setIdleTimeout(600000);
		config.setMaxLifetime(1800000);
		config.setLeakDetectionThreshold(60000);

		// MySQL 연결 검증 설정
		config.setConnectionTestQuery("SELECT 1");
		config.setValidationTimeout(3000);

		HikariDataSource dataSource = new HikariDataSource(config);

		log.info("DB Connection: {}", maskDbUrl(jdbcUrl));

		return dataSource;
	}

	private String maskDbUrl(String url) {
		// jdbc:log4jdbc:mysql://mysql-server:3306/dondothat -> jdbc:log4jdbc:mysql://my...er:3306/dondothat
		Pattern pattern = Pattern.compile("(?<=//)([^:/]+)");
		Matcher matcher = pattern.matcher(url);
		if (matcher.find()) {
			String host = matcher.group(1);
			// localhost is not sensitive, no need to mask
			if (host.length() > 4 && !"localhost".equalsIgnoreCase(host)) {
				String maskedHost = host.substring(0, 2) + "..." + host.substring(host.length() - 2);
				return matcher.replaceFirst(maskedHost);
			}
		}
		return url;
	}

	@Autowired
	ApplicationContext applicationContext;

	@Bean
	public SqlSessionFactory sqlSessionFactory() throws Exception {
		SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
		sqlSessionFactory.setConfigLocation(applicationContext.getResource("classpath:/mybatis-config.xml"));
		sqlSessionFactory.setDataSource(dataSource());

		// resources/mappers/ 하위의 모든 xml 파일을 매퍼로 인식하도록 경로를 설정
		sqlSessionFactory.setMapperLocations(applicationContext.getResources("classpath:/mappers/**/*.xml"));

		return (SqlSessionFactory)sqlSessionFactory.getObject();
	}

	@Bean
	public DataSourceTransactionManager transactionManager() {
		DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource());

		return manager;
	}
}
