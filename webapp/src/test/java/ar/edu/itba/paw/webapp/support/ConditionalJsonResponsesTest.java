package ar.edu.itba.paw.webapp.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.models.domain.car.Car;

class ConditionalJsonResponsesTest {

    @Test
    void testOkOrNotModifiedReturns200WithEtagWhenPreconditionsFail() {
        // 1.Arrange
        final Request request = mock(Request.class);
        final EntityTag tag = new EntityTag("car-7-detail-1700000000000");
        when(request.evaluatePreconditions(tag)).thenReturn(null);
        final AtomicBoolean supplierCalled = new AtomicBoolean(false);

        // 2.Act
        final Response response = ConditionalJsonResponses.okOrNotModified(
                request,
                tag.getValue(),
                "application/vnd.paw.car.v1+json",
                () -> {
                    supplierCalled.set(true);
                    return "{\"plate\":\"ABC123\"}";
                });

        // 3.Assert
        assertEquals(200, response.getStatus());
        assertEquals(tag, response.getEntityTag());
        assertEquals("application/vnd.paw.car.v1+json", response.getMediaType().toString());
        assertEquals("Accept", response.getHeaderString(HttpHeaders.VARY));
        assertEquals("{\"plate\":\"ABC123\"}", response.getEntity());
        assertEquals(true, supplierCalled.get());
    }

    @Test
    void testOkOrNotModifiedReturns304WithoutBodyWhenPreconditionsMatch() {
        // 1.Arrange
        final Request request = mock(Request.class);
        final EntityTag tag = new EntityTag("car-7-detail-1700000000000");
        when(request.evaluatePreconditions(tag)).thenReturn(Response.notModified());
        final AtomicBoolean supplierCalled = new AtomicBoolean(false);

        // 2.Act
        final Response response = ConditionalJsonResponses.okOrNotModified(
                request,
                tag.getValue(),
                "application/vnd.paw.car.v1+json",
                () -> {
                    supplierCalled.set(true);
                    return "{\"plate\":\"ABC123\"}";
                });

        // 3.Assert
        assertEquals(304, response.getStatus());
        assertNull(response.getEntity());
        assertEquals(false, supplierCalled.get());
        assertEquals("Accept", response.getHeaderString(HttpHeaders.VARY));
    }

    @Test
    void testCarRepresentationVersionsUsesUpdatedAtMillis() {
        // 1.Arrange
        final OffsetDateTime updatedAt = OffsetDateTime.parse("2026-07-12T12:34:56-03:00");
        final Car car = mock(Car.class);
        when(car.getId()).thenReturn(12L);
        when(car.getUpdatedAt()).thenReturn(updatedAt);

        // 2.Act
        final String summaryTag = CarRepresentationVersions.etagValue(car, CarRepresentationVersions.SUMMARY);
        final String detailTag = CarRepresentationVersions.etagValue(car, CarRepresentationVersions.DETAIL);

        // 3.Assert
        assertEquals("car-12-summary-" + updatedAt.toInstant().toEpochMilli(), summaryTag);
        assertEquals("car-12-detail-" + updatedAt.toInstant().toEpochMilli(), detailTag);
    }
}
