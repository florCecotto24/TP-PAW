package ar.edu.itba.paw.webapp.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

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
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import ar.edu.itba.paw.services.policy.ReviewValidationPolicy;
import ar.edu.itba.paw.services.policy.UserValidationPolicy;
import ar.edu.itba.paw.webapp.config.properties.AppValidationProperties;
import ar.edu.itba.paw.webapp.interceptor.LatestLocaleSaveInterceptor;
import ar.edu.itba.paw.webapp.support.CurrentUserArgumentResolver;

/**
 * Central Spring MVC setup: view resolver, i18n, multipart, async mail executor, Flyway-ready property sources,
 * and component scan for controllers, advice, exception handlers, util, security, validation, interceptor, services, and persistence.
 */
@EnableWebMvc
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@Configuration
@Import({
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
        "ar.edu.itba.paw.webapp.security",
        "ar.edu.itba.paw.webapp.validation",
        "ar.edu.itba.paw.webapp.interceptor",
        "ar.edu.itba.paw.services",
        "ar.edu.itba.paw.persistence"
})
public class WebConfig implements WebMvcConfigurer {

    private final ObjectProvider<LatestLocaleSaveInterceptor> latestLocaleSaveInterceptor;

    /**
     * Defer resolution until {@link #addInterceptors}: avoids a context cycle and avoids {@code @Lazy} (CGLIB cannot
     * proxy {@link LatestLocaleSaveInterceptor} because it is final).
     */
    public WebConfig(
            final ObjectProvider<LatestLocaleSaveInterceptor> latestLocaleSaveInterceptor) {
        this.latestLocaleSaveInterceptor = latestLocaleSaveInterceptor;
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

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(latestLocaleSaveInterceptor.getObject()).addPathPatterns("/**");
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
