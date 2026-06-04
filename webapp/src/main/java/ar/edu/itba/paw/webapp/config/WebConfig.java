package ar.edu.itba.paw.webapp.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.i18n.RydenLocaleResolver;
import ar.edu.itba.paw.webapp.support.converter.StringToReservationViewerRoleConverter;
import ar.edu.itba.paw.webapp.support.converter.StringToSupportedLocaleConverter;
import ar.edu.itba.paw.webapp.support.converter.StringToUserDocumentTypeConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.itba.paw.services.policy.ReservationChatPolicy;
import ar.edu.itba.paw.services.policy.ReservationMessageValidationPolicy;
import ar.edu.itba.paw.services.policy.ReviewValidationPolicy;
import ar.edu.itba.paw.services.policy.UserValidationPolicy;
import ar.edu.itba.paw.webapp.config.properties.AppReservationChatProperties;
import ar.edu.itba.paw.webapp.config.properties.AppValidationProperties;
import ar.edu.itba.paw.webapp.interceptor.NoCacheHtmlInterceptor;
import ar.edu.itba.paw.webapp.support.CurrentUserArgumentResolver;

/**
 * Central Spring MVC setup: view resolver, i18n, multipart, async mail executor, Flyway-ready property sources,
 * and component scan for controllers, advice, exception handlers, util, support, security, validation, interceptor, services, and persistence.
 */
@EnableWebMvc
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@Configuration
@Import({
    JacksonConfig.class,
    LocalApplicationEnvironmentConfig.class,
    DeployedApplicationEnvironmentConfig.class,
    SpringMailConfig.class,
    WebAuthConfig.class,
    ValidationWebConfig.class
})
@PropertySource("classpath:application/application.properties")
@ComponentScan({
        "ar.edu.itba.paw.webapp.controller",
        "ar.edu.itba.paw.webapp.advice",
        "ar.edu.itba.paw.webapp.exception",
        "ar.edu.itba.paw.webapp.util",
        "ar.edu.itba.paw.webapp.support",
        "ar.edu.itba.paw.webapp.security",
        "ar.edu.itba.paw.webapp.validation",
        "ar.edu.itba.paw.webapp.interceptor",
        "ar.edu.itba.paw.services",
        "ar.edu.itba.paw.persistence"
})
public class WebConfig implements WebMvcConfigurer {

    private final ObjectProvider<NoCacheHtmlInterceptor> noCacheHtmlInterceptor;
    private final ObjectMapper objectMapper;

    /**
     * Defer resolution until {@link #addInterceptors}: avoids a context cycle and avoids {@code @Lazy} (CGLIB cannot
     * proxy interceptors because they are final).
     */
    public WebConfig(
            final ObjectProvider<NoCacheHtmlInterceptor> noCacheHtmlInterceptor,
            final ObjectMapper objectMapper) {
        this.noCacheHtmlInterceptor = noCacheHtmlInterceptor;
        this.objectMapper = objectMapper;
    }

    /**
     * Needed for {@link org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher} with path variables
     * (e.g. {@code /my-reservations/{reservationId}/**}), distinct from Ant {@code *} patterns.
     */
    @Bean
    public HandlerMappingIntrospector handlerMappingIntrospector(final ApplicationContext applicationContext) {
        return new HandlerMappingIntrospector(applicationContext);
    }

    @Bean
    public MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource bundle = new ReloadableResourceBundleMessageSource();
        bundle.setBasenames("classpath:messages/messages", "classpath:messages/exception/exception-messages");
        bundle.setDefaultEncoding(StandardCharsets.UTF_8.name());
        bundle.setFallbackToSystemLocale(false);
        return bundle;
    }

    @Bean
    public AppValidationProperties appValidationProperties(final Environment environment) {
        return AppValidationProperties.fromEnvironment(environment);
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
    public LocalValidatorFactoryBean localValidatorFactoryBean(
            final MessageSource messageSource,
            final AutowireCapableBeanFactory beanFactory) {
        final LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        bean.setConstraintValidatorFactory(new SpringConstraintValidatorFactory(beanFactory));
        return bean;
    }

    @Bean
    public LocaleResolver localeResolver(final UserService userService) {
        // Custom resolver: signed-in user's stored preference > cookie > default (Spanish).
        // Accept-Language is intentionally ignored; the user picks the language explicitly via the navbar toggle.
        return new RydenLocaleResolver(userService);
    }

    @Bean
    public ViewResolver viewResolver() {
        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");
        return viewResolver;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(final DataSource dataSource) {
        final LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setPackagesToScan("ar.edu.itba.paw.models");
        factoryBean.setDataSource(dataSource);
        final JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        factoryBean.setJpaVendorAdapter(vendorAdapter);
        final Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.show_sql", "true");
        properties.setProperty("format_sql", "true");
        factoryBean.setJpaProperties(properties);
        return factoryBean;
    }

    @Bean
    public PlatformTransactionManager transactionManager(final EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-async-");
        /*
         * JavaMail (MimeMessage / MimeMessageHelper) loads javax.activation.* from the context class loader.
         * Jetty + @Async can leave worker threads with a TCCL that does not see WEB-INF/lib, causing
         * ClassNotFoundException: javax.activation.DataSource even when com.sun.activation:javax.activation is packaged.
         */
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
        executor.initialize();
        return executor;
    }

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations("/css/");
        registry.addResourceHandler("/js/**").addResourceLocations("/js/");
        registry.addResourceHandler("/assets/**").addResourceLocations("/assets/");
    }

    /**
     * Multipart using Apache Commons FileUpload (Spring {@link CommonsMultipartResolver}),
     * with {@code MultipartFilter} in {@code web.xml} before Spring Security.
     */
    @Bean
    public CommonsMultipartResolver multipartResolver(final Environment environment) {
        final long maxRequest = resolveMaxMultipartRequestBytes(environment);
        final CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
        multipartResolver.setMaxUploadSize(maxRequest);
        multipartResolver.setMaxInMemorySize(1048576);
        multipartResolver.setDefaultEncoding(StandardCharsets.UTF_8.name());
        return multipartResolver;
    }

    /**
     * Prefer {@code app.upload.max-multipart-request-megabytes} (aligned with {@code application.properties});
     * fall back to explicit bytes, then the legacy default (~180 MiB).
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

    @Override
    public void configureDefaultServletHandling(final DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    /** Ensures REST chat history returns ISO-8601 {@code createdAt} strings, not numeric epoch seconds. */
    @Override
    public void extendMessageConverters(final List<HttpMessageConverter<?>> converters) {
        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i) instanceof MappingJackson2HttpMessageConverter) {
                converters.set(i, new MappingJackson2HttpMessageConverter(objectMapper));
                return;
            }
        }
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(noCacheHtmlInterceptor.getObject()).addPathPatterns("/**");
    }

    /**
     * Registers explicit {@code String → Enum/Locale} converters for {@code @RequestParam} binding,
     * replacing the manual {@code parseDocumentType(raw)} / {@code if (!"owner".equals(role))} /
     * {@code SupportedLocales.parse(raw)} dance that controllers used to perform. Each converter
     * returns {@code null} for blank or unrecognized inputs so the existing "silently ignore" /
     * "swap to default" semantics of those endpoints is preserved.
     */
    @Override
    public void addFormatters(final FormatterRegistry registry) {
        registry.addConverter(new StringToReservationViewerRoleConverter());
        registry.addConverter(new StringToUserDocumentTypeConverter());
        registry.addConverter(new StringToSupportedLocaleConverter());
    }

    /**
     * {@link WebMvcConfigurer#addArgumentResolvers} appends after built-in resolvers;
     * {@link org.springframework.web.method.annotation.ModelAttributeMethodProcessor}
     * would otherwise claim {@code User} parameters before {@link ar.edu.itba.paw.webapp.support.CurrentUserArgumentResolver}.
     * Registration runs on {@link ContextRefreshedEvent} so {@link RequestMappingHandlerAdapter} is not touched during
     * {@code WebConfig} creation (that would re-enter MVC bootstrap and cycle with {@code messageSource} / {@code localValidatorFactoryBean}).
     */
    @Bean
    public ApplicationListener<ContextRefreshedEvent> prependCurrentUserArgumentResolver() {
        return event -> {
            if (event.getApplicationContext().getParent() != null) {
                return;
            }
            final RequestMappingHandlerAdapter adapter =
                    event.getApplicationContext().getBean(RequestMappingHandlerAdapter.class);
            adapter.setIgnoreDefaultModelOnRedirect(true);
            final List<HandlerMethodArgumentResolver> resolvers =
                    new ArrayList<>(adapter.getArgumentResolvers());
            if (resolvers.stream().anyMatch(r -> r instanceof CurrentUserArgumentResolver)) {
                return;
            }
            resolvers.add(0, new CurrentUserArgumentResolver());
            adapter.setArgumentResolvers(resolvers);
        };
    }

}
