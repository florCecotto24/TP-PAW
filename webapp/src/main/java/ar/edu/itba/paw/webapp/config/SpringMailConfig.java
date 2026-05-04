package ar.edu.itba.paw.webapp.config;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * JavaMail sender plus Thymeleaf for HTML mail. SMTP wiring lives under {@code mail/config/} on the webapp
 * classpath; templates and {@code MailMessages} bundles come from {@code classpath:mail/} (services JAR).
 */
@Configuration
@PropertySource(value = "classpath:mail/config/emailconfig.properties", encoding = "UTF-8")
public class SpringMailConfig implements ApplicationContextAware, EnvironmentAware {

    private ApplicationContext applicationContext;
    private Environment environment;

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }

    @Bean
    public JavaMailSender mailSender() throws IOException {
        final JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(requiredProperty("mail.server.host"));
        mailSender.setPort(Integer.parseInt(requiredProperty("mail.server.port")));
        mailSender.setProtocol(requiredProperty("mail.server.protocol"));
        mailSender.setUsername(requiredProperty("mail.server.username"));
        mailSender.setPassword(requiredProperty("mail.server.password"));

        final Properties javaMailProperties = new Properties();
        javaMailProperties.load(
                this.applicationContext.getResource(requiredProperty("mail.javamail.config.location")).getInputStream());
        mailSender.setJavaMailProperties(javaMailProperties);

        return mailSender;
    }

    @Bean
    public ResourceBundleMessageSource emailMessageSource() {
        final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename(requiredProperty("mail.messages.basename"));
        messageSource.setDefaultEncoding(templateEncoding());
        messageSource.setDefaultLocale(Locale.ENGLISH);
        return messageSource;
    }

    @Bean
    public TemplateEngine emailTemplateEngine() {
        final SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.addTemplateResolver(textTemplateResolver());
        templateEngine.addTemplateResolver(htmlTemplateResolver());
        templateEngine.addTemplateResolver(stringTemplateResolver());
        templateEngine.setTemplateEngineMessageSource(emailMessageSource());
        return templateEngine;
    }

    private ITemplateResolver textTemplateResolver() {
        final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setOrder(1);
        templateResolver.setResolvablePatterns(
                Collections.singleton(requiredProperty("mail.templates.pattern.text")));
        templateResolver.setPrefix(requiredProperty("mail.templates.prefix"));
        templateResolver.setSuffix(requiredProperty("mail.templates.suffix.text"));
        templateResolver.setTemplateMode(TemplateMode.TEXT);
        templateResolver.setCharacterEncoding(templateEncoding());
        templateResolver.setCacheable(false);
        return templateResolver;
    }

    private ITemplateResolver htmlTemplateResolver() {
        final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setOrder(2);
        templateResolver.setResolvablePatterns(
                Collections.singleton(requiredProperty("mail.templates.pattern.html")));
        templateResolver.setPrefix(requiredProperty("mail.templates.prefix"));
        templateResolver.setSuffix(requiredProperty("mail.templates.suffix.html"));
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding(templateEncoding());
        templateResolver.setCacheable(false);
        return templateResolver;
    }

    private ITemplateResolver stringTemplateResolver() {
        final StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setOrder(3);
        templateResolver.setTemplateMode(
                TemplateMode.parse(requiredProperty("mail.templates.string.mode")));
        templateResolver.setCacheable(false);
        return templateResolver;
    }

    private String templateEncoding() {
        return requiredProperty("mail.template.encoding");
    }

    private String requiredProperty(final String key) {
        final String value = this.environment.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing or empty mail configuration property: " + key);
        }
        return value;
    }
}
