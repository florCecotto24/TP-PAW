package ar.edu.itba.paw.webapp.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.scheduling.TaskScheduler;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.itba.paw.services.ReservationMessageService;
import ar.edu.itba.paw.webapp.security.websocket.ReservationChatChannelInterceptor;

class WebSocketConfigTest {

    @SuppressWarnings("unchecked")
    private static WebSocketConfig newConfig(final TaskScheduler taskScheduler) {
        final ReservationMessageService messageService = mock(ReservationMessageService.class);
        final ReservationChatChannelInterceptor interceptor = new ReservationChatChannelInterceptor(messageService);
        final ObjectProvider<TaskScheduler> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(taskScheduler);
        return new WebSocketConfig(interceptor, new ObjectMapper(), provider);
    }

    @Test
    void testConfigureMessageBrokerSetsHeartbeat() {
        final MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
        final SimpleBrokerRegistration brokerRegistration = mock(SimpleBrokerRegistration.class);
        when(registry.enableSimpleBroker("/topic")).thenReturn(brokerRegistration);
        when(brokerRegistration.setTaskScheduler(any(TaskScheduler.class))).thenReturn(brokerRegistration);

        final long[][] capturedHeartbeat = new long[1][];
        when(brokerRegistration.setHeartbeatValue(any(long[].class))).thenAnswer(invocation -> {
            capturedHeartbeat[0] = invocation.getArgument(0);
            return brokerRegistration;
        });

        newConfig(mock(TaskScheduler.class)).configureMessageBroker(registry);

        assertArrayEquals(WebSocketConfig.STOMP_HEARTBEAT_MS, capturedHeartbeat[0]);
    }

    @Test
    void testConfigureMessageBrokerEnablesTopicBroker() {
        final MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
        final SimpleBrokerRegistration brokerRegistration = mock(SimpleBrokerRegistration.class);
        final AtomicReference<Object> capturedPrefix = new AtomicReference<>();
        when(registry.enableSimpleBroker(any())).thenAnswer(invocation -> {
            capturedPrefix.set(invocation.getArgument(0));
            return brokerRegistration;
        });
        when(brokerRegistration.setHeartbeatValue(any(long[].class))).thenReturn(brokerRegistration);
        when(brokerRegistration.setTaskScheduler(any(TaskScheduler.class))).thenReturn(brokerRegistration);

        newConfig(mock(TaskScheduler.class)).configureMessageBroker(registry);

        assertTrue(
                "/topic".equals(capturedPrefix.get())
                        || (capturedPrefix.get() instanceof String[] prefixes
                                && prefixes.length == 1
                                && "/topic".equals(prefixes[0])));
    }

    @Test
    void testStompHeartbeatConstantIsTenSeconds() {
        assertArrayEquals(new long[] {10_000L, 10_000L}, WebSocketConfig.STOMP_HEARTBEAT_MS);
        assertTrue(WebSocketConfig.STOMP_HEARTBEAT_MS[0] > 0);
    }
}
