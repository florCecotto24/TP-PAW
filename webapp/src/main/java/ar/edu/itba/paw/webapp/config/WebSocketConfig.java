package ar.edu.itba.paw.webapp.config;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
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

    /** Server and client negotiate STOMP heartbeats every 10s to keep proxies from closing idle sockets. */
    static final long[] STOMP_HEARTBEAT_MS = {10_000L, 10_000L};

    private final ReservationChatChannelInterceptor reservationChatChannelInterceptor;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<TaskScheduler> messageBrokerTaskScheduler;

    @Autowired
    public WebSocketConfig(
            final ReservationChatChannelInterceptor reservationChatChannelInterceptor,
            final ObjectMapper objectMapper,
            final ObjectProvider<TaskScheduler> messageBrokerTaskScheduler) {
        this.reservationChatChannelInterceptor = reservationChatChannelInterceptor;
        this.objectMapper = objectMapper;
        this.messageBrokerTaskScheduler = messageBrokerTaskScheduler;
    }

    @Override
    public void configureMessageBroker(final MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic")
                .setHeartbeatValue(STOMP_HEARTBEAT_MS)
                .setTaskScheduler(messageBrokerTaskScheduler.getObject());
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
