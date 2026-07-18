package ar.edu.itba.paw.mail;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class MailPublicUrlsTest {

    private static final String KEY = "mail.app.base.url";

    @Mock
    private Environment environment;

    private MailPublicUrls mailPublicUrls;

    private void useBase(final String base) {
        Mockito.when(environment.getProperty(Mockito.eq(KEY), Mockito.anyString()))
                .thenAnswer(invocation -> base != null ? base : invocation.getArgument(1));
        mailPublicUrls = new MailPublicUrls(environment);
    }

    @Test
    void testAbsolutePathFallsBackToDefaultBaseWhenPropertyMissing() {
        // 1.Arrange
        useBase(null);

        // 2.Act
        final String url = mailPublicUrls.absolutePath("/login");

        // 3.Assert — default includes context path (same as Jetty/Pampero /paw-2026a-08)
        Assertions.assertEquals("http://localhost:8080/paw-2026a-08/login", url);
    }

    @Test
    void testAbsolutePathStripsTrailingSlashesFromConfiguredBase() {
        // 1.Arrange
        useBase("https://app.example.com/webapp///");

        // 2.Act
        final String url = mailPublicUrls.absolutePath("/login");

        // 3.Assert
        Assertions.assertEquals("https://app.example.com/webapp/login", url);
    }

    @Test
    void testAbsolutePathReturnsBaseAloneWhenPathIsNull() {
        // 1.Arrange
        useBase("https://app.example.com/webapp/");

        // 2.Act
        final String url = mailPublicUrls.absolutePath(null);

        // 3.Assert
        Assertions.assertEquals("https://app.example.com/webapp", url);
    }

    @Test
    void testAbsolutePathReturnsBaseAloneWhenPathIsBlank() {
        // 1.Arrange
        useBase("https://app.example.com/webapp");

        // 2.Act
        final String url = mailPublicUrls.absolutePath("   ");

        // 3.Assert
        Assertions.assertEquals("https://app.example.com/webapp", url);
    }

    @Test
    void testAbsolutePathPrependsLeadingSlashWhenPathLacksOne() {
        // 1.Arrange
        useBase("https://app.example.com/webapp");

        // 2.Act
        final String url = mailPublicUrls.absolutePath("login");

        // 3.Assert
        Assertions.assertEquals("https://app.example.com/webapp/login", url);
    }

    @Test
    void testAbsolutePathDoesNotDoublePrefixSlashWhenPathStartsWithSlash() {
        // 1.Arrange
        useBase("https://app.example.com/webapp");

        // 2.Act
        final String url = mailPublicUrls.absolutePath("/cars/42");

        // 3.Assert
        Assertions.assertEquals("https://app.example.com/webapp/cars/42", url);
    }
}
