package ar.edu.itba.paw.webapp.support;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.UserPrivateDto;
import ar.edu.itba.paw.webapp.form.admin.CreateAdminUserForm;
import ar.edu.itba.paw.webapp.form.user.RegistrationAccountForm;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;

/**
 * Collection and lifecycle HTTP binding for {@code /users} (list, register, admin-create, delete).
 */
@Component
public final class UserCollectionSupport {

    private final UserService userService;
    private final AdminService adminService;
    private final FormValidationSupport formValidationSupport;
    private final UserRepresentationSupport userRepresentationSupport;

    public UserCollectionSupport(
            final UserService userService,
            final AdminService adminService,
            final FormValidationSupport formValidationSupport,
            final UserRepresentationSupport userRepresentationSupport) {
        this.userService = userService;
        this.adminService = adminService;
        this.formValidationSupport = formValidationSupport;
        this.userRepresentationSupport = userRepresentationSupport;
    }

    public Response list(
            final PaginationParams paging,
            final Boolean blocked,
            final String role,
            final String query,
            final UriInfo uriInfo) {
        final Page<User> usersPage = adminService.listUsers(
                paging.getZeroBasedPage(), paging.getPageSize(), blocked, role, query);
        if (usersPage.getTotalItems() == 0L) {
            return Response.noContent().build();
        }
        final List<UserPrivateDto> dtos =
                userRepresentationSupport.toPrivateDtos(usersPage.getContent(), uriInfo);
        final Response.ResponseBuilder builder = Response.ok(new GenericEntity<List<UserPrivateDto>>(dtos) {})
                .type(VndMediaType.USER_PRIVATE_V1_JSON)
                .header("X-Total-Count", usersPage.getTotalItems());
        PaginationLinks.add(
                builder, uriInfo, paging.getPage(), paging.getPageSize(), (int) usersPage.getTotalItems());
        return builder.build();
    }

    public Response register(
            final RegistrationAccountForm form,
            final Locale locale,
            final UriInfo uriInfo,
            final HttpHeaders httpHeaders) {
        formValidationSupport.validate(form, ValidationGroups.OnRegistration.class);
        final User created = userService.registerUserRequiringAccountConfirmation(
                form.getEmail(),
                form.getForename(),
                form.getSurname(),
                form.getPassword(),
                form.getPasswordConfirm(),
                locale);
        final URI location = userLocation(uriInfo, created.getId());
        final Response body = userRepresentationSupport.buildUserResponse(
                created, uriInfo, null, httpHeaders);
        return Response.fromResponse(body)
                .status(Response.Status.CREATED)
                .location(location)
                .build();
    }

    public Response createAdminUser(
            final CreateAdminUserForm form,
            final long actingAdminId,
            final Locale locale,
            final UriInfo uriInfo) {
        formValidationSupport.validate(form, ValidationGroups.OnCreateAdminUser.class);
        final User created = adminService.createAdminUser(
                form.getEmail(),
                form.getForename(),
                form.getSurname(),
                actingAdminId,
                locale);
        return Response.status(Response.Status.CREATED)
                .location(userLocation(uriInfo, created.getId()))
                .type(VndMediaType.USER_PRIVATE_V1_JSON)
                .entity(userRepresentationSupport.toPrivateDto(created, uriInfo))
                .build();
    }

    public Response delete(final long userId, final RydenUserDetails viewer) {
        userService.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final long actingUserId = viewer.getUserId();
        if (actingUserId == userId) {
            userService.deleteUser(userId);
        } else {
            adminService.deleteUser(userId, actingUserId);
        }
        return Response.noContent().build();
    }

    private static URI userLocation(final UriInfo uriInfo, final long userId) {
        return uriInfo.getBaseUriBuilder()
                .path("users")
                .path(String.valueOf(userId))
                .build();
    }
}
