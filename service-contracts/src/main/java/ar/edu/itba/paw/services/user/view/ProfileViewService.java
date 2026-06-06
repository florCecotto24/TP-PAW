package ar.edu.itba.paw.services.user.view;


import java.util.Locale;
import java.util.Optional;

import ar.edu.itba.paw.models.dto.profile.ProfileBaseModel;

import ar.edu.itba.paw.services.file.StoredFileService;
import ar.edu.itba.paw.services.user.UserService;
/**
 * Read-only API for the signed-in user's own profile page (and any other view that needs the same
 * "user basics" panel: email, display name, picture, member-since, document validation flags and
 * file names). Pure orchestrator over {@link UserService} and {@link StoredFileService}; no DAOs
 * are touched directly.
 */
public interface ProfileViewService {

    /**
     * Loads the page model used by the {@code profile/*} JSPs (member-since localized,
     * picture id, both document validation flags and file names). The {@code locale}
     * parameter controls the formatting of {@code profileMemberSinceDisplay}; the rest of
     * the values are language-agnostic.
     *
     * @param userId id of the signed-in user
     * @param locale request locale used to format the member-since label
     * @return populated model when the user exists; {@link Optional#empty()} otherwise
     */
    Optional<ProfileBaseModel> loadProfileBaseModel(long userId, Locale locale);
}
