package ar.edu.itba.paw.services.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.util.rules.CbuRules;

@Service
public class UserReadinessServiceImpl implements UserReadinessService {

    @Override
    @Transactional(readOnly = true)
    public boolean hasValidCbu(final User user) {
        if (user == null) {
            return false;
        }
        return user.getCbu().filter(CbuRules::isValidFormat).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUploadedLicenseAndIdentity(final User user) {
        if (user == null) {
            return false;
        }
        return user.getLicenseFileId().isPresent() && user.getIdentityFileId().isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean meetsPublishingPrerequisites(final User user) {
        if (user == null) {
            return false;
        }
        return hasValidCbu(user) && user.getIdentityFileId().isPresent();
    }

    @Override
    public boolean isValidCbuFormat(final String cbuRaw) {
        return CbuRules.isValidFormat(cbuRaw);
    }
}
