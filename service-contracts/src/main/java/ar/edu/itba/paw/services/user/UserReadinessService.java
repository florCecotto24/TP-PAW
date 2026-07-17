package ar.edu.itba.paw.services.user;

import ar.edu.itba.paw.models.domain.user.User;

/**
 * In-memory readiness gates for riders/owners (CBU, KYC slots, publish prerequisites).
 * No {@code UserDao} — operates on loaded {@link User} rows or static CBU format rules.
 */
public interface UserReadinessService {

    boolean hasValidCbu(User user);

    boolean hasUploadedLicenseAndIdentity(User user);

    boolean meetsPublishingPrerequisites(User user);

    boolean isValidCbuFormat(String cbuRaw);
}
