package ar.edu.itba.paw.webapp.config;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import ar.edu.itba.paw.models.UserValidationPolicy;

@EnableWebMvc
@EnableAsync
@EnableTransactionManagement
@Configuration
@Import({SpringMailConfig.class, WebAuthConfig.class, ValidationWebConfig.class})
@PropertySources({
    @PropertySource("classpath:application.properties"),
    @PropertySource(value = "classpath:application-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
})
@ComponentScan({
        "ar.edu.itba.paw.webapp.controller",
        "ar.edu.itba.paw.webapp.util",
        "ar.edu.itba.paw.webapp.security",
        "ar.edu.itba.paw.webapp.validation",
        "ar.edu.itba.paw.services",
        "ar.edu.itba.paw.persistence"
})
public class WebConfig implements WebMvcConfigurer, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(@NonNull final Environment environment) {
        this.environment = environment;
    }

    @Bean
    public MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource bundle = new ReloadableResourceBundleMessageSource();
        bundle.setBasenames("classpath:messages", "classpath:exception-messages");
        bundle.setDefaultEncoding(StandardCharsets.UTF_8.name());
        bundle.setFallbackToSystemLocale(false);
        return bundle;
    }

    @Bean
    public UserValidationPolicy userValidationPolicy() {
        final int min = this.environment.getProperty("app.validation.registration-password-min-length", Integer.class, 8);
        final int max = this.environment.getProperty("app.validation.profile-phone-max-length", Integer.class, 20);
        final String pattern = this.environment.getProperty("app.validation.profile-phone-pattern", "^[0-9+]+$");
        if (min < 1) {
            throw new IllegalArgumentException("app.validation.registration-password-min-length must be >= 1, got " + min);
        }
        if (max < 1) {
            throw new IllegalArgumentException("app.validation.profile-phone-max-length must be >= 1, got " + max);
        }
        final Pattern compiled;
        try {
            compiled = Pattern.compile(pattern);
        } catch (final PatternSyntaxException e) {
            throw new IllegalArgumentException("app.validation.profile-phone-pattern is not a valid regex: " + pattern, e);
        }
        return new UserValidationPolicy(min, max, compiled);
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
    public LocaleResolver localeResolver() {
        final AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setSupportedLocales(Arrays.asList(Locale.ENGLISH, Locale.forLanguageTag("es")));
        return resolver;
    }

    @Bean
    public ViewResolver viewResolver() {
        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");
        return viewResolver;
    }

    @Bean
    public PlatformTransactionManager transactionManager(final DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-async-");
        executor.initialize();
        return executor;
    }

    @Bean
    public DataSource dataSource() {
        final SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.postgresql.Driver.class);
        dataSource.setUrl(requiredProperty("spring.datasource.url"));
        dataSource.setUsername(requiredProperty("spring.datasource.username"));
        dataSource.setPassword(requiredProperty("spring.datasource.password"));

        final ResourceDatabasePopulator schemaBootstrap = new ResourceDatabasePopulator();
        schemaBootstrap.addScript(new ClassPathResource("schema.sql"));
        DatabasePopulatorUtils.execute(schemaBootstrap, dataSource);

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .failOnMissingLocations(true)
                .load()
                .migrate();

        return dataSource;
    }

    @Bean
    public DataSourceInitializer dataSourceInitializer(final DataSource dataSource) {
        final DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(databasePopulator());
        return initializer;
    }

    private DatabasePopulator databasePopulator() {
        final ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema.sql"));
        return populator;
    }

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations("/css/");
        registry.addResourceHandler("/js/**").addResourceLocations("/js/");
        registry.addResourceHandler("/assets/**").addResourceLocations("/assets/");
    }

    /**
     * Multipart vía Apache Commons FileUpload (integrated in Spring {@link CommonsMultipartResolver}),
     * With {@code MultipartFilter} declared in {@code web.xml} before Spring Security.
     */
    @Bean
    public CommonsMultipartResolver multipartResolver() {
        final long maxRequest = Long.parseLong(
                environment.getProperty("app.upload.max-multipart-request-bytes", "188743680").trim());
        final CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
        multipartResolver.setMaxUploadSize(maxRequest);
        multipartResolver.setMaxInMemorySize(1048576);
        multipartResolver.setDefaultEncoding(StandardCharsets.UTF_8.name());
        return multipartResolver;
    }

    @Override
    public void configureDefaultServletHandling(final DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    private String requiredProperty(final String key) {
        final String value = this.environment.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing or empty datasource configuration property: " + key);
        }
        return value;
    }
}
