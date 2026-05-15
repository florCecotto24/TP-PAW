package ar.edu.itba.paw.services.mail;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

        // 2.Exercise
        final String url = mailPublicUrls.absolutePath("/login");

        // 3.Assert
        Assertions.assertEquals("http://localhost:8080/login", url);
    }

    @Test
    void testAbsolutePathStripsTrailingSlashesFromConfiguredBase() {
        // 1.Arrange
        useBase("https://app.example.com/webapp///");

        // 2.Exercise
        final String url = mailPublicUrls.absolutePath("/login");

        // 3.Assert
        Assertions.assertEquals("https://app.example.com/webapp/login", url);
    }

    @Test
    void testAbsolutePathReturnsBaseAloneWhenPathIsNull() {
        // 1.Arrange
        useBase("https://app.example.com/webapp/");

        // 2.Exercise
        final String url = mailPublicUrls.absolutePath(null);

        // 3.Assert
        Assertions.assertEquals("https://app.example.com/webapp", url);
    }

    @Test
    void testAbsolutePathReturnsBaseAloneWhenPathIsBlank() {
        // 1.Arrange
        useBase("https://app.example.com/webapp");

        // 2.Exercise
        final String url = mailPublicUrls.absolutePath("   ");

        // 3.Assert
        Assertions.assertEquals("https://app.example.com/webapp", url);
    }

    @Test
    void testAbsolutePathPrependsLeadingSlashWhenPathLacksOne() {
        // 1.Arrange
        useBase("https://app.example.com/webapp");

        // 2.Exercise
        final String url = mailPublicUrls.absolutePath("login");

        // 3.Assert
        Assertions.assertEquals("https://app.example.com/webapp/login", url);
    }

    @Test
    void testAbsolutePathDoesNotDoublePrefixSlashWhenPathStartsWithSlash() {
        // 1.Arrange
        useBase("https://app.example.com/webapp");

        // 2.Exercise
        final String url = mailPublicUrls.absolutePath("/cars/42");

        // 3.Assert
        Assertions.assertEquals("https://app.example.com/webapp/cars/42", url);
    }
}
