package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Listing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class ReservationPricingServiceImpl implements ReservationPricingService {

	private static final long MINUTES_PER_DAY = 24L * 60L;

	private final ListingService listingService;

	@Autowired
	public ReservationPricingServiceImpl(final ListingService listingService) {
		this.listingService = listingService;
	}

	@Override
	public Optional<BigDecimal> calculateTotal(
			final long listingId,
			final OffsetDateTime startDate,
			final OffsetDateTime endDate) {
		final long billableDays = calculateBillableDays(startDate, endDate);
		if (billableDays <= 0) {
			return Optional.empty();
		}

		return listingService.getListingById(listingId)
				.map(Listing::getDayPrice)
				.map(dayPrice -> dayPrice.multiply(BigDecimal.valueOf(billableDays)));
	}

	@Override
	public long calculateBillableDays(final OffsetDateTime startDate, final OffsetDateTime endDate) {
		if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
			return 0;
		}

		final long totalMinutes = Duration.between(startDate, endDate).toMinutes();
		return (totalMinutes + MINUTES_PER_DAY - 1L) / MINUTES_PER_DAY;
	}
}

