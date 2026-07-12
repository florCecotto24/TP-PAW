package ar.edu.itba.paw.webapp.config;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

import ar.edu.itba.paw.policy.CarValidationPolicy;
import ar.edu.itba.paw.policy.ListingFormValidationPolicy;
import ar.edu.itba.paw.policy.MoneyFormatPolicy;
import ar.edu.itba.paw.policy.ReservationChatPolicy;
import ar.edu.itba.paw.policy.ReservationMessageValidationPolicy;
import ar.edu.itba.paw.policy.ReviewValidationPolicy;
import ar.edu.itba.paw.policy.UserValidationPolicy;
import ar.edu.itba.paw.policy.VerificationCodePolicy;
import ar.edu.itba.paw.webapp.config.properties.AppMoneyProperties;
import ar.edu.itba.paw.webapp.config.properties.AppReservationChatProperties;
import ar.edu.itba.paw.webapp.config.properties.AppSecurityJwtProperties;
import ar.edu.itba.paw.webapp.config.properties.AppValidationProperties;

/**
 * Root Spring configuration for the SPA + REST stack: JPA, validation, async mail, security, and services.
 *
 * REST endpoints live in {@code controller/} as JAX-RS resources ({@code @Path} + {@code @Component}).
 * Bean Validation is enabled via {@link SpringValidatorBinder} and {@code @Valid} / {@link FormValidationSupport}.
 */
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@Configuration
@Import({
    JacksonConfig.class,
    LocalApplicationEnvironmentConfig.class,
    DeployedApplicationEnvironmentConfig.class,
    SpringMailConfig.class,
    WebAuthConfig.class
})
@PropertySource("classpath:application/application.properties")
@ComponentScan(
        basePackages = {
                "ar.edu.itba.paw.webapp.controller",
                "ar.edu.itba.paw.webapp.exception.mapper",
                "ar.edu.itba.paw.webapp.util",
                "ar.edu.itba.paw.webapp.support",
                "ar.edu.itba.paw.webapp.security",
                "ar.edu.itba.paw.webapp.validation",
                "ar.edu.itba.paw.webapp.config.properties",
                "ar.edu.itba.paw.services",
                "ar.edu.itba.paw.persistence",
                "ar.edu.itba.paw.mail",
                "ar.edu.itba.paw.policy",
                "ar.edu.itba.paw.scheduling",
                "ar.edu.itba.paw.util"
        },
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Controller.class)
)
public class WebConfig {

    @Bean
    public MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource bundle = new ReloadableResourceBundleMessageSource();
        bundle.setBasenames("classpath:messages/messages", "classpath:messages/exception/exception-messages");
        bundle.setDefaultEncoding(StandardCharsets.UTF_8.name());
        bundle.setFallbackToSystemLocale(false);
        return bundle;
    }

    @Bean
    public AppSecurityJwtProperties appSecurityJwtProperties(final Environment environment) {
        return AppSecurityJwtProperties.fromEnvironment(environment);
    }

    @Bean
    public AppValidationProperties appValidationProperties(final Environment environment) {
        return AppValidationProperties.fromEnvironment(environment);
    }

    @Bean
    public AppMoneyProperties appMoneyProperties(final Environment environment) {
        return AppMoneyProperties.fromEnvironment(environment);
    }

    @Bean
    public MoneyFormatPolicy moneyFormatPolicy(final AppMoneyProperties appMoneyProperties) {
        return appMoneyProperties.toMoneyFormatPolicy();
    }

    @Bean
    public UserValidationPolicy userValidationPolicy(final AppValidationProperties appValidationProperties) {
        return appValidationProperties.toUserValidationPolicy();
    }

    @Bean
    public ReviewValidationPolicy reviewValidationPolicy(final AppValidationProperties appValidationProperties) {
        return appValidationProperties.toReviewValidationPolicy();
    }

    @Bean
    public AppReservationChatProperties appReservationChatProperties(final Environment environment) {
        return AppReservationChatProperties.fromEnvironment(environment);
    }

    @Bean
    public ReservationMessageValidationPolicy reservationMessageValidationPolicy(
            final AppValidationProperties appValidationProperties) {
        return appValidationProperties.toReservationMessageValidationPolicy();
    }

    @Bean
    public ReservationChatPolicy reservationChatPolicy(
            final AppReservationChatProperties appReservationChatProperties) {
        return appReservationChatProperties.toReservationChatPolicy();
    }

    @Bean
    public CarValidationPolicy carValidationPolicy(final AppValidationProperties appValidationProperties) {
        return appValidationProperties.toCarValidationPolicy();
    }

    @Bean
    public ListingFormValidationPolicy listingFormValidationPolicy(
            final AppValidationProperties appValidationProperties) {
        return appValidationProperties.toListingFormValidationPolicy();
    }

    @Bean
    public VerificationCodePolicy verificationCodePolicy(final AppValidationProperties appValidationProperties) {
        return appValidationProperties.toVerificationCodePolicy();
    }

    @Bean
    public LocalValidatorFactoryBean localValidatorFactoryBean(
            final MessageSource messageSource,
            final AutowireCapableBeanFactory beanFactory) {
        final LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        bean.setConstraintValidatorFactory(new SpringConstraintValidatorFactory(beanFactory));
        return bean;
    }

    @Bean
    public org.springframework.validation.beanvalidation.MethodValidationPostProcessor methodValidationPostProcessor(
            final LocalValidatorFactoryBean localValidatorFactoryBean) {
        final org.springframework.validation.beanvalidation.MethodValidationPostProcessor processor =
                new org.springframework.validation.beanvalidation.MethodValidationPostProcessor();
        processor.setValidator(localValidatorFactoryBean);
        return processor;
    }

    /**
     * Hibernate JPA properties are read from the {@link Environment} (application.properties + active profile
     * override). Verbose SQL logging is off by default; enable it per environment via {@code hibernate.show_sql} /
     * {@code hibernate.format_sql} in {@code application-local.properties}.
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            final DataSource dataSource, final Environment environment) {
        final LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setPackagesToScan("ar.edu.itba.paw.models");
        factoryBean.setDataSource(dataSource);
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factoryBean.setJpaProperties(resolveHibernateProperties(environment));
        return factoryBean;
    }

    private static Properties resolveHibernateProperties(final Environment environment) {
        final Properties properties = new Properties();
        copyIfPresent(environment, properties, "hibernate.hbm2ddl.auto");
        copyIfPresent(environment, properties, "hibernate.dialect");
        copyIfPresent(environment, properties, "hibernate.show_sql");
        copyIfPresent(environment, properties, "hibernate.format_sql");
        copyIfPresent(environment, properties, "hibernate.use_sql_comments");
        return properties;
    }

    private static void copyIfPresent(
            final Environment environment, final Properties properties, final String key) {
        final String value = environment.getProperty(key);
        if (value != null && !value.isBlank()) {
            properties.setProperty(key, value.trim());
        }
    }

    @Bean
    public PlatformTransactionManager transactionManager(final EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor(final Environment environment) {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(environment.getProperty("app.mail.pool.core-size", Integer.class, 1));
        executor.setMaxPoolSize(environment.getProperty("app.mail.pool.max-size", Integer.class, 4));
        executor.setQueueCapacity(environment.getProperty("app.mail.pool.queue-capacity", Integer.class, 100));
        executor.setThreadNamePrefix("mail-async-");
        final ClassLoader webAppClassLoader = WebConfig.class.getClassLoader();
        executor.setTaskDecorator(runnable -> () -> {
            final ClassLoader previous = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(webAppClassLoader);
            try {
                runnable.run();
            } finally {
                Thread.currentThread().setContextClassLoader(previous);
            }
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(
                environment.getProperty("app.mail.pool.await-termination-seconds", Integer.class, 30));
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Shared upload limit helper (legacy MVC multipart bean removed). Used by tests and future Jersey multipart wiring.
     */
    static long resolveMaxMultipartRequestBytes(final Environment environment) {
        final String bytesProperty = environment.getProperty("app.upload.max-multipart-request-bytes");
        if (bytesProperty != null && !bytesProperty.isBlank()) {
            return Long.parseLong(bytesProperty.trim());
        }
        final Long megabytes = environment.getProperty("app.upload.max-multipart-request-megabytes", Long.class);
        final int bytesPerMegabyte =
                environment.getProperty("app.upload.bytes-per-binary-megabyte", Integer.class, 1048576);
        if (megabytes != null && megabytes > 0) {
            return megabytes * bytesPerMegabyte;
        }
        return 188743680L;
    }

}
