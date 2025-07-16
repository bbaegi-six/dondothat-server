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
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@PropertySource({"classpath:/application.properties"})
// @MapperScan(basePackages = {})
public class RootConfig {
	private static final Logger log = LogManager.getLogger(RootConfig.class);
	@Value("${jdbc.driver}")
	String driver;
	@Value("${jdbc.url}")
	String url;
	@Value("${jdbc.username}")
	String username;
	@Value("${jdbc.password}")
	String password;

	@Bean
	public DataSource dataSource() {
		HikariConfig config = new HikariConfig();

		config.setDriverClassName(driver);
		config.setJdbcUrl(url);
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

		log.info("DB Connection: {}", maskDbUrl(url));

		return dataSource;
	}

	private String maskDbUrl(String url) {
		// jdbc:log4jdbc:mysql://localhost:3306/dondothat -> jdbc:log4jdbc:mysql://lo...st:3306/dondothat
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

		return (SqlSessionFactory)sqlSessionFactory.getObject();
	}

	@Bean
	public DataSourceTransactionManager transactionManager() {
		DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource());

		return manager;
	}
}