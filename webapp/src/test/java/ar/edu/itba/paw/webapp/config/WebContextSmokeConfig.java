package ar.edu.itba.paw.webapp.config;

import java.util.Properties;
import java.util.concurrent.Executor;

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
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

import ar.edu.itba.paw.policy.CarValidationPolicy;
import ar.edu.itba.paw.policy.ListingFormValidationPolicy;
import ar.edu.itba.paw.policy.MoneyFormatPolicy;
import ar.edu.itba.paw.policy.ReservationChatPolicy;
import ar.edu.itba.paw.policy.ReservationMessageValidationPolicy;
import ar.edu.itba.paw.policy.ReviewValidationPolicy;
import ar.edu.itba.paw.policy.UserValidationPolicy;
import ar.edu.itba.paw.policy.VerificationCodePolicy;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.config.properties.AppMoneyProperties;
import ar.edu.itba.paw.webapp.config.properties.AppReservationChatProperties;
import ar.edu.itba.paw.webapp.config.properties.AppSecurityJwtProperties;
import ar.edu.itba.paw.webapp.config.properties.AppValidationProperties;
import ar.edu.itba.paw.webapp.filter.RydenLocaleFilter;
import ar.edu.itba.paw.webapp.i18n.RydenLocaleResolver;

/**
 * TEST-ONLY root context that mirrors {@link WebConfig}'s DI wiring but backs JPA with embedded
 * HSQLDB (no Postgres, no Flyway), so {@link WebApplicationContextSmokeTest} can prove the entire
 * Jersey/REST + security graph instantiates. Deliberately does NOT import
 * {@code Local/DeployedApplicationEnvironmentConfig}.
 */
@EnableAsync
@EnableTransactionManagement
@Configuration
@Import({JacksonConfig.class, SpringMailConfig.class, WebAuthConfig.class})
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
public class WebContextSmokeConfig {

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.HSQL)
                .addScript("classpath:schema-hsqldb.sql")
                .addScript("classpath:seed-neighborhoods.sql")
                .build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(final DataSource dataSource) {
        final LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setPackagesToScan("ar.edu.itba.paw.models");
        factoryBean.setDataSource(dataSource);
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        final Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "none");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        factoryBean.setJpaProperties(properties);
        return factoryBean;
    }

    @Bean
    public PlatformTransactionManager transactionManager(final EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor() {
        return new SyncTaskExecutor();
    }

    @Bean
    public MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource bundle = new ReloadableResourceBundleMessageSource();
        bundle.setBasenames("classpath:messages/messages", "classpath:messages/exception/exception-messages");
        bundle.setDefaultEncoding("UTF-8");
        bundle.setFallbackToSystemLocale(false);
        return bundle;
    }

    @Bean
    public RydenLocaleResolver rydenLocaleResolver(final UserService userService) {
        return new RydenLocaleResolver(userService);
    }

    @Bean
    public RydenLocaleFilter rydenLocaleFilter(final RydenLocaleResolver rydenLocaleResolver) {
        return new RydenLocaleFilter(rydenLocaleResolver);
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
    public MethodValidationPostProcessor methodValidationPostProcessor(
            final LocalValidatorFactoryBean localValidatorFactoryBean) {
        final MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(localValidatorFactoryBean);
        return processor;
    }
}
