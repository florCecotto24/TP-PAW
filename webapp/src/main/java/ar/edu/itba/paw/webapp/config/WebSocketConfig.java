package ar.edu.itba.paw.webapp.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.itba.paw.webapp.security.websocket.ReservationChatChannelInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@Import({JacksonConfig.class, WebSocketSecurityConfig.class})
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ReservationChatChannelInterceptor reservationChatChannelInterceptor;
    private final ObjectMapper objectMapper;

    @Autowired
    public WebSocketConfig(
            final ReservationChatChannelInterceptor reservationChatChannelInterceptor,
            final ObjectMapper objectMapper) {
        this.reservationChatChannelInterceptor = reservationChatChannelInterceptor;
        this.objectMapper = objectMapper;
    }

    @Override
    public void configureMessageBroker(final MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(final StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(final ChannelRegistration registration) {
        registration.interceptors(reservationChatChannelInterceptor);
    }

    /**
     * STOMP uses dedicated message converters; use the shared {@link ObjectMapper} so
     * {@link ar.edu.itba.paw.models.dto.reservation.ReservationMessageDto#createdAt} is ISO-8601 in topic payloads.
     */
    @Override
    public boolean configureMessageConverters(final List<MessageConverter> messageConverters) {
        final MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        messageConverters.add(converter);
        return true;
    }
}
