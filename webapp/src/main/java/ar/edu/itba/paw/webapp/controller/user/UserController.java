package ar.edu.itba.paw.webapp.controller.user;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.UserPatchCommand;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.services.user.PasswordResetService;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.api.common.PaginationLinks;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.dto.rest.UserPrivateDto;
import ar.edu.itba.paw.webapp.form.admin.CreateAdminUserForm;
import ar.edu.itba.paw.webapp.form.user.ProfilePasswordChangeForm;
import ar.edu.itba.paw.webapp.form.user.RegistrationAccountForm;
import ar.edu.itba.paw.webapp.form.user.UserPatchForm;
import ar.edu.itba.paw.webapp.security.auth.AuthenticationAuthorities;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.FormValidationSupport;
import ar.edu.itba.paw.webapp.support.PaginationParams;
import ar.edu.itba.paw.webapp.support.PaginationSupport;
import ar.edu.itba.paw.webapp.support.UserRepresentationSupport;
import ar.edu.itba.paw.webapp.support.UserResourceAccess;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.user.ValidUserRole;

/**
 * Users resource ({@code /users}, {@code /users/{id}}).
 *
 * Public vs private representations are selected via {@code Accept} (two fixed schemas),
 * not by trimming fields for the same MIME type.
 *
 * {@code PATCH /users/{id}} uses a single JSON partial body ({@link UserPatchForm}) rather than
 * vendor MIME types per operation (contrast MenuMate). One resource URI, field presence routes to
 * profile / password / admin updates — defendible under LINEAMIENTOS (resource identity in the path).
 */
@Path("/users")
@Component
public class UserController {

    private final UserService userService;
    private final AdminService adminService;
    private final PasswordResetService passwordResetService;
    private final FormValidationSupport formValidationSupport;
    private final CurrentUserResolver currentUserResolver;
    private final UserRepresentationSupport userRepresentationSupport;
    private final UserResourceAccess userResourceAccess;
    private final PaginationSupport paginationSupport;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpHeaders httpHeaders;

    @Autowired
    public UserController(
            final UserService userService,
            final AdminService adminService,
            final PasswordResetService passwordResetService,
            final FormValidationSupport formValidationSupport,
            final CurrentUserResolver currentUserResolver,
            final UserRepresentationSupport userRepresentationSupport,
            final UserResourceAccess userResourceAccess,
            final PaginationSupport paginationSupport) {
        this.userService = userService;
        this.adminService = adminService;
        this.passwordResetService = passwordResetService;
        this.formValidationSupport = formValidationSupport;
        this.currentUserResolver = currentUserResolver;
        this.userRepresentationSupport = userRepresentationSupport;
        this.userResourceAccess = userResourceAccess;
        this.paginationSupport = paginationSupport;
    }

    @GET
    @Produces(VndMediaType.USER_PRIVATE_V1_JSON)
    @PreAuthorize("@userResourceAccess.isAdmin()")
    public Response listUsers(
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("pageSize") final Integer pageSizeParam,
            @QueryParam("blocked") final Boolean blocked,
            @QueryParam("role") @ValidUserRole final String role,
            @QueryParam("q") final String query) {
        final PaginationParams paging = paginationSupport.forDefaultCollection(page, pageSizeParam);
        final Page<User> usersPage = adminService.listUsers(
                paging.getZeroBasedPage(), paging.getPageSize(), blocked, role, query);
        if (usersPage.getTotalItems() == 0L) {
            return Response.noContent().build();
        }
        final List<UserPrivateDto> dtos = userRepresentationSupport.toPrivateDtos(usersPage.getContent(), uriInfo);
        final Response.ResponseBuilder builder = Response.ok(new GenericEntity<List<UserPrivateDto>>(dtos) {})
                .type(VndMediaType.USER_PRIVATE_V1_JSON)
                .header("X-Total-Count", usersPage.getTotalItems());
        PaginationLinks.add(
                builder, uriInfo, paging.getPage(), paging.getPageSize(), (int) usersPage.getTotalItems());
        return builder.build();
    }

    @POST
    @Consumes(VndMediaType.USER_V1_JSON)
    @Produces(VndMediaType.USER_V1_JSON)
    public Response register(final RegistrationAccountForm form) {
        formValidationSupport.validate(form, ValidationGroups.OnRegistration.class);
        final User created = userService.registerUserRequiringAccountConfirmation(
                form.getEmail(),
                form.getForename(),
                form.getSurname(),
                form.getPassword(),
                form.getPasswordConfirm(),
                LocaleContextHolder.getLocale());

        final URI location = uriInfo.getBaseUriBuilder()
                .path("users")
                .path(String.valueOf(created.getId()))
                .build();
        final Response body = userRepresentationSupport.buildUserResponse(
                created, uriInfo, null, httpHeaders);
        return Response.fromResponse(body)
                .status(Response.Status.CREATED)
                .location(location)
                .build();
    }

    /**
     * Admin-only variant of user creation, discriminated from {@link #register} by
     * {@code Content-Type}: provisions a pre-verified admin account and emails an invitation
     * plus a password-reset OTP so the invitee sets their own password (never mailed in clear).
     */
    @POST
    @Consumes(VndMediaType.ADMIN_CREATE_USER_V1_JSON)
    @Produces(VndMediaType.USER_PRIVATE_V1_JSON)
    @PreAuthorize("@userResourceAccess.isAdmin()")
    public Response createAdminUser(final CreateAdminUserForm form) {
        formValidationSupport.validate(form, ValidationGroups.OnCreateAdminUser.class);
        final long actingAdminId = currentUserResolver.requireUserId();
        final User created = adminService.createAdminUser(
                form.getEmail(),
                form.getForename(),
                form.getSurname(),
                actingAdminId,
                LocaleContextHolder.getLocale());

        final URI location = uriInfo.getBaseUriBuilder()
                .path("users")
                .path(String.valueOf(created.getId()))
                .build();
        return Response.status(Response.Status.CREATED)
                .location(location)
                .type(VndMediaType.USER_PRIVATE_V1_JSON)
                .entity(userRepresentationSupport.toPrivateDto(created, uriInfo))
                .build();
    }

    @GET
    @Path("/{id}")
    @Produces({VndMediaType.USER_V1_JSON, VndMediaType.USER_PRIVATE_V1_JSON})
    public Response getUser(@PathParam("id") final long id) {
        final User user = userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        return userRepresentationSupport.buildUserResponse(
                user, uriInfo, currentUserResolver.currentPrincipalOrNull(), httpHeaders);
    }

    @DELETE
    @Path("/{id}")
    @PreAuthorize("@userResourceAccess.isSelfOrAdmin(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response deleteUser(@P("id") @PathParam("id") final long id) {
        userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();
        final long actingUserId = viewer.getUserId();
        if (actingUserId == id) {
            userService.deleteUser(id);
        } else {
            adminService.deleteUser(id, actingUserId);
        }
        return Response.noContent().build();
    }

    // Not a @PreAuthorize candidate: password reset uses OTP credentials in the security
    // context; profile/admin field authorization is enforced in UserService#patchUser.
    @PATCH
    @Path("/{id}")
    @Consumes(VndMediaType.USER_V1_JSON)
    @Produces({VndMediaType.USER_V1_JSON, VndMediaType.USER_PRIVATE_V1_JSON})
    public Response patchUser(@PathParam("id") final long id, @Valid final UserPatchForm patch) {
        final User user = userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final RydenUserDetails viewer = currentUserResolver.currentPrincipalOrNull();
        final UserPatchCommand command = toPatchCommand(patch);

        // Authorize profile/admin fields before any mutation so a password+admin body cannot
        // commit the password and then 403 on the admin facet.
        if (command.hasProfileFields() || command.hasAdminFields()) {
            final long actingUserId = viewer != null
                    ? viewer.getUserId()
                    : currentUserResolver.requireUserId();
            userService.assertCanPatchUser(id, command, actingUserId);
        }

        if (patch.getPassword() != null) {
            applyPasswordPatch(user, patch, viewer);
        }
        if (command.hasProfileFields() || command.hasAdminFields()) {
            final long actingUserId = viewer != null
                    ? viewer.getUserId()
                    : currentUserResolver.requireUserId();
            userService.patchUser(id, command, actingUserId);
        }

        final User updated = userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        return userRepresentationSupport.buildUserResponse(updated, uriInfo, viewer, httpHeaders);
    }

    private static UserPatchCommand toPatchCommand(final UserPatchForm patch) {
        final UserPatchCommand.Builder builder = UserPatchCommand.builder();
        if (patch.getForename() != null) {
            builder.forename(patch.getForename());
        }
        if (patch.getSurname() != null) {
            builder.surname(patch.getSurname());
        }
        if (patch.getPhoneNumber() != null) {
            builder.phoneNumber(patch.getPhoneNumber());
        }
        if (patch.getBirthDate() != null) {
            builder.birthDate(toBirthDate(patch.getBirthDate()));
        }
        if (patch.getAbout() != null) {
            builder.about(patch.getAbout());
        }
        if (patch.getCbu() != null) {
            builder.cbu(patch.getCbu());
        }
        if (patch.getLatestLocale() != null) {
            builder.latestLocale(patch.getLatestLocale());
        }
        if (patch.getRole() != null) {
            builder.role(patch.getRole());
        }
        if (patch.getBlocked() != null) {
            builder.blocked(patch.getBlocked());
        }
        if (patch.getIdentityValidated() != null) {
            builder.identityValidated(patch.getIdentityValidated());
        }
        if (patch.getLicenseValidated() != null) {
            builder.licenseValidated(patch.getLicenseValidated());
        }
        return builder.build();
    }

    private void applyPasswordPatch(
            final User user,
            final UserPatchForm patch,
            final RydenUserDetails viewer) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (AuthenticationAuthorities.hasPasswordResetOtp(authentication)) {
            if (!(authentication.getPrincipal() instanceof RydenUserDetails principal)
                    || principal.getUserId() != user.getId()) {
                throw new AccessDeniedException("You do not have permission to perform this action.");
            }
            final String otp = authentication.getCredentials() != null
                    ? authentication.getCredentials().toString()
                    : "";
            passwordResetService.completePasswordReset(
                    user.getEmail(),
                    otp,
                    patch.getPassword(),
                    patch.getPasswordConfirm());
            return;
        }

        userResourceAccess.requireSelf(user.getId(), viewer);
        final ProfilePasswordChangeForm passwordForm = new ProfilePasswordChangeForm();
        passwordForm.setCurrentPassword(patch.getCurrentPassword());
        passwordForm.setPassword(patch.getPassword());
        passwordForm.setPasswordConfirm(patch.getPasswordConfirm());
        formValidationSupport.validate(passwordForm, ValidationGroups.OnProfilePassword.class);
        userService.changePassword(
                user.getId(),
                passwordForm.getCurrentPassword(),
                passwordForm.getPassword(),
                passwordForm.getPasswordConfirm());
    }

    private static LocalDate toBirthDate(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return LocalDate.parse(raw.trim());
    }
}
