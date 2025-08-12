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
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@PropertySource({"classpath:/application.properties"})
@MapperScan(basePackages = {"org.bbagisix.**.mapper"})
@ComponentScan(
    basePackages = "org.bbagisix",
    excludeFilters = {
        @ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ANNOTATION, classes = {
            Controller.class, ControllerAdvice.class})
    }
)
@EnableScheduling
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

    @Value("${REDIS_HOST:localhost}")
    private String redisHost;
    @Value("${REDIS_PORT:6379}")
    private int redisPort;

    @Value("${SPRING_MAIL_HOST}")
    private String mailHost;
    @Value("${SPRING_MAIL_PORT}")
    private int mailPort;
    @Value("${SPRING_MAIL_USERNAME}")
    private String mailUsername;
    @Value("${SPRING_MAIL_PASSWORD}")
    private String mailPassword;

    // OAuth 환경 변수
    @Value("${GOOGLE_CLIENT_ID:}")
    private String googleClientId;
    @Value("${GOOGLE_CLIENT_SECRET:}")
    private String googleClientSecret;
    @Value("${NAVER_CLIENT_ID:}")
    private String naverClientId;
    @Value("${NAVER_CLIENT_SECRET:}")
    private String naverClientSecret;
    @Value("${BASE_URL:}")
    private String baseUrl;
    @Value("${JWT_SECRET}")
    private String jwtSecret;

    // CODEF 환경 변수
    @Value("${CODEF_CLIENT_ID:}")
    private String codefClientId;
    @Value("${CODEF_CLIENT_SECRET:}")
    private String codefClientSecret;
    @Value("${CODEF_PUBLIC_KEY:}")
    private String codefPublicKey;
    
    // FSS API 환경 변수
    @Value("${fss.api.key}")
    private String fssApiKey;
    @Value("${fss.api.url}")
    private String fssApiUrl;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
        stringRedisTemplate.setConnectionFactory(redisConnectionFactory());
        return stringRedisTemplate;
    }

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);
        mailSender.setDefaultEncoding("UTF-8");
        mailSender.setJavaMailProperties(getMailProperties());
        return mailSender;
    }

    private Properties getMailProperties() {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        return properties;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        // 환경 변수를 사용하여 JDBC URL 구성 (SSL 및 공개키 검색 옵션 추가)
        String jdbcUrl = String.format("jdbc:log4jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true", dbHost, dbPort, dbName);

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
        Pattern pattern = Pattern.compile("(?<=//)([^:/]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String host = matcher.group(1);
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
        sqlSessionFactory.setMapperLocations(applicationContext.getResources("classpath:/mappers/**/*.xml"));
        return (SqlSessionFactory) sqlSessionFactory.getObject();
    }

    @Bean
    public DataSourceTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(20);
        scheduler.initialize();
        return scheduler;
    }
}
