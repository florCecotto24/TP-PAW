package ar.edu.itba.paw.webapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

/**
 * Propagates the HTTP session principal to STOMP frames and requires authentication for chat
 * destinations. CSRF for the CONNECT frame is disabled because servlet CSRF is off globally.
 */
@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    protected void configureInbound(final MessageSecurityMetadataSourceRegistry messages) {
        messages
                .simpDestMatchers("/app/**").authenticated()
                .simpSubscribeDestMatchers("/topic/**").authenticated()
                .anyMessage().authenticated();
    }

    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
}
