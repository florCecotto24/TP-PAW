package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface ReservationPricingService {

    Optional<BigDecimal> calculateTotal(long listingId, OffsetDateTime startDate, OffsetDateTime endDate);

    long calculateBillableDays(OffsetDateTime startDate, OffsetDateTime endDate);
}

