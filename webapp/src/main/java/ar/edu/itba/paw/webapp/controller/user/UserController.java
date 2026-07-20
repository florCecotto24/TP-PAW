package ar.edu.itba.paw.webapp.controller.user;

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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.form.admin.CreateAdminUserForm;
import ar.edu.itba.paw.webapp.form.user.RegistrationAccountForm;
import ar.edu.itba.paw.webapp.form.user.UserPatchForm;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;
import ar.edu.itba.paw.webapp.support.PaginationSupport;
import ar.edu.itba.paw.webapp.support.UserCollectionSupport;
import ar.edu.itba.paw.webapp.support.UserPatchSupport;
import ar.edu.itba.paw.webapp.support.UserRepresentationSupport;
import ar.edu.itba.paw.webapp.validation.constraint.user.ValidUserRole;

/**
 * Users resource ({@code /users}, {@code /users/{id}}).
 * HTTP routing only; collection / PATCH binding lives in {@code *Support} helpers.
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

    private final CurrentUserResolver currentUserResolver;
    private final UserRepresentationSupport userRepresentationSupport;
    private final UserCollectionSupport userCollectionSupport;
    private final UserPatchSupport userPatchSupport;
    private final PaginationSupport paginationSupport;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpHeaders httpHeaders;

    @Autowired
    public UserController(
            final CurrentUserResolver currentUserResolver,
            final UserRepresentationSupport userRepresentationSupport,
            final UserCollectionSupport userCollectionSupport,
            final UserPatchSupport userPatchSupport,
            final PaginationSupport paginationSupport) {
        this.currentUserResolver = currentUserResolver;
        this.userRepresentationSupport = userRepresentationSupport;
        this.userCollectionSupport = userCollectionSupport;
        this.userPatchSupport = userPatchSupport;
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
        return userCollectionSupport.list(
                paginationSupport.forDefaultCollection(page, pageSizeParam),
                blocked,
                role,
                query,
                uriInfo);
    }

    @POST
    @Consumes(VndMediaType.USER_V1_JSON)
    @Produces(VndMediaType.USER_V1_JSON)
    public Response register(final RegistrationAccountForm form) {
        return userCollectionSupport.register(
                form, LocaleContextHolder.getLocale(), uriInfo, httpHeaders);
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
        return userCollectionSupport.createAdminUser(
                form,
                currentUserResolver.requireUserId(),
                LocaleContextHolder.getLocale(),
                uriInfo);
    }

    @GET
    @Path("/{id}")
    @Produces({VndMediaType.USER_V1_JSON, VndMediaType.USER_PRIVATE_V1_JSON})
    public Response getUser(@PathParam("id") final long id) {
        return userRepresentationSupport.getUserResponse(
                id, uriInfo, currentUserResolver.currentPrincipalOrNull(), httpHeaders);
    }

    @DELETE
    @Path("/{id}")
    @PreAuthorize("@userResourceAccess.isSelfOrAdmin(#id, @currentUserResolver.currentPrincipalOrNull())")
    public Response deleteUser(@P("id") @PathParam("id") final long id) {
        return userCollectionSupport.delete(id, currentUserResolver.currentPrincipalOrNull());
    }

    // Not a @PreAuthorize candidate: password reset uses OTP credentials in the security
    // context; profile/admin field authorization is enforced in UserService#patchUser.
    @PATCH
    @Path("/{id}")
    @Consumes(VndMediaType.USER_V1_JSON)
    @Produces({VndMediaType.USER_V1_JSON, VndMediaType.USER_PRIVATE_V1_JSON})
    public Response patchUser(@PathParam("id") final long id, @Valid final UserPatchForm patch) {
        return userPatchSupport.apply(
                id, patch, currentUserResolver.currentPrincipalOrNull(), uriInfo, httpHeaders);
    }
}
