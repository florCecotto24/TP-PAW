package ar.edu.itba.paw.webapp.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.config.JacksonConfig;
import ar.edu.itba.paw.webapp.dto.rest.ApiIndexDto;
import ar.edu.itba.paw.webapp.dto.rest.CarSummaryDto;
import ar.edu.itba.paw.webapp.dto.rest.CatalogApprovalDto;
import ar.edu.itba.paw.webapp.dto.rest.ErrorDto;
import ar.edu.itba.paw.webapp.dto.rest.LinksDto;
import ar.edu.itba.paw.webapp.dto.rest.MessageDto;
import ar.edu.itba.paw.webapp.dto.rest.ReservationDto;
import ar.edu.itba.paw.webapp.dto.rest.ReservationSummaryDto;
import ar.edu.itba.paw.webapp.dto.rest.ValidationErrorDto;
import ar.edu.itba.paw.webapp.form.reservation.ReservationCreateForm;
import ar.edu.itba.paw.webapp.form.reservation.ReservationPatchForm;
import ar.edu.itba.paw.webapp.form.user.UserPatchForm;
import ar.edu.itba.paw.webapp.support.ReservationRestEnums;

/**
 * Contract tests: REST wire format and forms vs {@code openapi.yaml}.
 * No database, no Jersey container, no service mocks.
 */
class OpenApiContractTest {

    private static String openapiYaml;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void loadContract() {
        openapiYaml = OpenApiContractSupport.loadOpenApiYaml();
        objectMapper = new JacksonConfig().objectMapper();
    }

    @Test
    void testReservationStatusEnumMatchesOpenApi() {
        // 1.Arrange
        final Set<String> spec = OpenApiContractSupport.enumValues(openapiYaml, "ReservationStatus");

        // 2.Act
        final Set<String> domain = Arrays.stream(Reservation.Status.values())
                .map(s -> s.name().toLowerCase())
                .collect(Collectors.toSet());

        // 3.Assert
        assertEquals(spec, domain);
    }

    @Test
    void testReservationRestEnumsRoundTripOpenApiValues() {
        // 1.Arrange
        final Set<String> specValues = OpenApiContractSupport.enumValues(openapiYaml, "ReservationStatus");

        // 2.Act / 3.Assert
        for (final String value : specValues) {
            assertEquals(value, ReservationRestEnums.toRestName(ReservationRestEnums.parseStatus(value)));
        }
    }

    @Test
    void testCarSummaryDtoJsonFieldsMatchOpenApi() throws Exception {
        // 1.Arrange
        final CarSummaryDto dto = sampleCarSummaryDto();
        final Set<String> expected = OpenApiContractSupport.schemaProperties(openapiYaml, "CarSummaryDto");

        // 2.Act
        final Set<String> wire = OpenApiContractSupport.jsonFieldNames(
                objectMapper.writeValueAsString(dto), objectMapper);

        // 3.Assert
        assertEquals(expected, wire);
    }

    @Test
    void testReservationSummaryDtoJsonFieldsMatchOpenApi() throws Exception {
        // 1.Arrange
        final ReservationSummaryDto dto = sampleReservationSummaryDto();
        final Set<String> expected = OpenApiContractSupport.schemaProperties(openapiYaml, "ReservationSummaryDto");

        // 2.Act
        final Set<String> wire = OpenApiContractSupport.jsonFieldNames(
                objectMapper.writeValueAsString(dto), objectMapper);

        // 3.Assert
        assertEquals(expected, wire);
    }

    @Test
    void testReservationDtoJsonFieldsMatchOpenApi() throws Exception {
        // 1.Arrange
        final ReservationDto dto = sampleReservationDto();
        final Set<String> expected = OpenApiContractSupport.schemaProperties(openapiYaml, "ReservationDto");

        // 2.Act
        final Set<String> wire = OpenApiContractSupport.jsonFieldNames(
                objectMapper.writeValueAsString(dto), objectMapper);

        // 3.Assert
        assertEquals(expected, wire);
    }

    @Test
    void testMessageDtoJsonFieldsMatchOpenApi() throws Exception {
        // 1.Arrange
        final MessageDto dto = sampleMessageDto();
        final Set<String> expected = OpenApiContractSupport.schemaProperties(openapiYaml, "MessageDto");

        // 2.Act
        final Set<String> wire = OpenApiContractSupport.jsonFieldNames(
                objectMapper.writeValueAsString(dto), objectMapper);

        // 3.Assert
        assertEquals(expected, wire);
    }

    @Test
    void testErrorDtoJsonFieldsMatchOpenApi() throws Exception {
        // 1.Arrange
        final ErrorDto dto = ErrorDto.of(403, "forbidden", "Not allowed");
        final Set<String> expected = OpenApiContractSupport.schemaProperties(openapiYaml, "ErrorDto");

        // 2.Act
        final Set<String> wire = OpenApiContractSupport.jsonFieldNames(
                objectMapper.writeValueAsString(dto), objectMapper);

        // 3.Assert
        assertEquals(expected, wire);
        assertEquals(VndMediaType.ERROR_V1_JSON, ErrorDto.mediaType());
    }

    @Test
    void testValidationErrorDtoJsonFieldsMatchOpenApi() throws Exception {
        // 1.Arrange
        final ValidationErrorDto dto = new ValidationErrorDto();
        dto.setMessage("Validation failed.");
        final ValidationErrorDto.FieldError field = new ValidationErrorDto.FieldError();
        field.setField("email");
        field.setMessage("invalid");
        dto.setErrors(java.util.List.of(field));
        final Set<String> expected = OpenApiContractSupport.schemaProperties(openapiYaml, "ValidationErrorDto");

        // 2.Act
        final Set<String> wire = OpenApiContractSupport.jsonFieldNames(
                objectMapper.writeValueAsString(dto), objectMapper);

        // 3.Assert
        assertEquals(expected, wire);
        assertEquals(VndMediaType.VALIDATION_ERROR_V1_JSON, ValidationErrorDto.mediaType());
    }

    @Test
    void testReservationCreateFormFieldsMatchOpenApi() throws Exception {
        // 1.Arrange
        final Set<String> specProps = OpenApiContractSupport.schemaProperties(openapiYaml, "ReservationCreateDto");
        final Set<String> required = OpenApiContractSupport.schemaRequired(openapiYaml, "ReservationCreateDto");
        final String json = """
                {"carUri":"/cars/1","availabilityUri":"/cars/1/availabilities/2",\
                "startDate":"2026-06-01T10:00:00Z","endDate":"2026-06-05T18:00:00Z"}\
                """;

        // 2.Act
        final Set<String> formProps = beanPropertyNames(ReservationCreateForm.class);
        final ReservationCreateForm parsed = objectMapper.readValue(json, ReservationCreateForm.class);

        // 3.Assert
        assertEquals(specProps, formProps);
        for (final String field : required) {
            assertTrue(beanPropertyPresent(parsed, field), "missing required field: " + field);
        }
    }

    @Test
    void testReservationPatchFormFieldsMatchOpenApi() throws Exception {
        // 1.Arrange
        final Set<String> specProps = OpenApiContractSupport.schemaProperties(openapiYaml, "ReservationPatchDto");

        // 2.Act
        final Set<String> formProps = beanPropertyNames(ReservationPatchForm.class);

        // 3.Assert
        assertEquals(specProps, formProps);
    }

    @Test
    void testUserPatchFormFieldsAreSubsetOfOpenApi() throws Exception {
        // 1.Arrange
        final Set<String> spec = OpenApiContractSupport.schemaProperties(openapiYaml, "UserPatchDto");

        // 2.Act
        final Set<String> form = beanPropertyNames(UserPatchForm.class);
        final Set<String> extra = form.stream().filter(f -> !spec.contains(f)).collect(Collectors.toSet());

        // 3.Assert
        assertTrue(extra.isEmpty(), () -> "form fields not in openapi: " + extra);
    }

    @Test
    void testCredentialEmissionEndpointsDocumentUniform200() {
        // 1.Arrange — openapiYaml from @BeforeAll

        // 2.Act
        final Set<String> passwordReset = OpenApiContractSupport.operationResponseStatusCodes(
                openapiYaml, "/credentials", "post");
        final Set<String> emailVerification = OpenApiContractSupport.operationResponseStatusCodes(
                openapiYaml, "/users/{id}/credentials", "post");

        // 3.Assert
        assertEquals(Set.of("200"), passwordReset);
        assertEquals(Set.of("200"), emailVerification);
    }

    @Test
    void testVndMediaTypesAreDeclaredInOpenApi() {
        // 1.Arrange
        final Set<String> inSpec = OpenApiContractSupport.vendorJsonMediaTypes(openapiYaml);
        final Set<String> inCode = Set.of(
                VndMediaType.API_V1_JSON,
                VndMediaType.USER_V1_JSON,
                VndMediaType.USER_FAVORITES_V1_JSON,
                VndMediaType.USER_PRIVATE_V1_JSON,
                VndMediaType.ADMIN_CREATE_USER_V1_JSON,
                VndMediaType.CAR_SUMMARY_V1_JSON,
                VndMediaType.CAR_SIMILAR_V1_JSON,
                VndMediaType.CAR_V1_JSON,
                VndMediaType.AVAILABILITY_V1_JSON,
                VndMediaType.BRAND_V1_JSON,
                VndMediaType.MODEL_V1_JSON,
                VndMediaType.PRICE_MARKET_INSIGHT_V1_JSON,
                VndMediaType.NEIGHBORHOOD_V1_JSON,
                VndMediaType.RESERVATION_SUMMARY_V1_JSON,
                VndMediaType.RESERVATION_LINKS_V1_JSON,
                VndMediaType.RESERVATION_V1_JSON,
                VndMediaType.COUNTERPARTY_CONTACT_V1_JSON,
                VndMediaType.MESSAGE_V1_JSON,
                VndMediaType.REVIEW_V1_JSON,
                VndMediaType.REVIEW_LINKS_V1_JSON,
                VndMediaType.PICTURE_V1_JSON,
                VndMediaType.BOOKABLE_SEGMENT_V1_JSON,
                VndMediaType.CREDENTIAL_V1_JSON,
                VndMediaType.ERROR_V1_JSON,
                VndMediaType.VALIDATION_ERROR_V1_JSON);

        // 2.Act
        final Set<String> missing = diff(inCode, inSpec);

        // 3.Assert
        assertTrue(missing.isEmpty(), () -> "openapi missing MIME types: " + missing);
    }

    @Test
    void testPageSizeParametersMatchPaginationPolicy() {
        // 1.Arrange — openapiYaml from @BeforeAll

        // 2.Act / 3.Assert — contract values documented in openapi.yaml
        assertEquals(100, OpenApiContractSupport.parameterSchemaInt(openapiYaml, "pageSize", "maximum"));
        assertEquals(8, OpenApiContractSupport.parameterSchemaInt(openapiYaml, "pageSize", "default"));
        assertEquals(8, OpenApiContractSupport.parameterSchemaInt(openapiYaml, "pageSizeBrowse", "default"));
        assertEquals(6, OpenApiContractSupport.parameterSchemaInt(openapiYaml, "pageSizeReviews", "default"));
        assertEquals(4, OpenApiContractSupport.parameterSchemaInt(openapiYaml, "pageSizeAvailabilities", "default"));
        assertEquals(8, OpenApiContractSupport.parameterSchemaInt(openapiYaml, "pageSizePictures", "default"));
        assertEquals(50, OpenApiContractSupport.parameterSchemaInt(openapiYaml, "pageSizeMessages", "default"));
    }

    @Test
    void testUsersIdHasNoPutOperation() {
        // 1.Arrange — openapiYaml from @BeforeAll

        // 2.Act / 3.Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> OpenApiContractSupport.operationResponseStatusCodes(openapiYaml, "/users/{id}", "put"));
    }

    @Test
    void testAvailabilityUpdateDocumentsPatchNotPut() {
        // 1.Arrange — openapiYaml from @BeforeAll

        // 2.Act
        final Set<String> patchStatuses = OpenApiContractSupport.operationResponseStatusCodes(
                openapiYaml, "/cars/{id}/availabilities/{availabilityId}", "patch");

        // 3.Assert
        assertTrue(patchStatuses.contains("200"));
        assertThrows(
                IllegalArgumentException.class,
                () -> OpenApiContractSupport.operationResponseStatusCodes(
                        openapiYaml, "/cars/{id}/availabilities/{availabilityId}", "put"));
    }

    @Test
    void testCatalogApprovalDtoJsonFieldsMatchOpenApi() throws Exception {
        // 1.Arrange
        final CatalogApprovalDto dto = new CatalogApprovalDto();
        dto.setValidated(true);
        final Set<String> expected = OpenApiContractSupport.schemaProperties(openapiYaml, "CatalogApprovalDto");

        // 2.Act
        final Set<String> wire = OpenApiContractSupport.jsonFieldNames(
                objectMapper.writeValueAsString(dto), objectMapper);

        // 3.Assert
        assertEquals(expected, wire);
    }

    @Test
    void testValidationErrorResponseUsesValidationErrorMime() {
        // 1.Arrange — openapiYaml from @BeforeAll

        // 2.Act
        final boolean specDeclaresMime =
                openapiYaml.contains("application/vnd.paw.validation-error.v1+json");

        // 3.Assert
        assertTrue(specDeclaresMime, "openapi ValidationError response must use validation-error MIME");
        assertEquals(VndMediaType.VALIDATION_ERROR_V1_JSON, ValidationErrorDto.mediaType());
    }

    @Test
    void testApiIndexDtoJsonFieldsMatchOpenApi() throws Exception {
        // 1.Arrange
        final ApiIndexDto dto = sampleApiIndexDto();
        final Set<String> expected = OpenApiContractSupport.schemaProperties(openapiYaml, "ApiIndex");

        // 2.Act
        final Set<String> wire = OpenApiContractSupport.jsonFieldNames(
                objectMapper.writeValueAsString(dto), objectMapper);

        // 3.Assert
        assertEquals(expected, wire);
        assertTrue(dto.getResources().get("cars").getQueryParams().contains("page"));
    }

    @Test
    void testLinksDtoSerializationOmitsNullRelatedHrefs() throws Exception {
        // 1.Arrange
        final LinksDto links = LinksDto.ofSelf("/cars/1")
                .withRelated("model", null)
                .withRelated("brand", null)
                .withRelated("owner", "/users/2");

        // 2.Act
        final String json = objectMapper.writeValueAsString(java.util.Map.of("links", links));

        // 3.Assert
        assertTrue(!json.contains(":null"), () -> "links must not serialize null values: " + json);
        assertTrue(!json.contains("/null"), () -> "links must not contain /null segments: " + json);
        assertTrue(json.contains("\"owner\""));
    }

    @Test
    void testOpenApiServersDocumentApiMount() {
        // 1.Arrange — openapiYaml from @BeforeAll

        // 2.Act / 3.Assert
        assertTrue(openapiYaml.contains("url: /webapp/api"), "Tomcat WAR server entry");
        assertTrue(openapiYaml.contains("url: /api"), "Jetty dev server entry");
    }

    @Test
    void testFlatModelItemPathNotInOpenApi() {
        // 1.Arrange — openapiYaml from @BeforeAll

        // 2.Act / 3.Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> OpenApiContractSupport.operationResponseStatusCodes(openapiYaml, "/models/{id}", "get"));
    }

    @Test
    void testNestedModelEndpointsDocumented() {
        // 1.Arrange — openapiYaml from @BeforeAll

        // 2.Act
        final Set<String> getModel = OpenApiContractSupport.operationResponseStatusCodes(
                openapiYaml, "/brands/{id}/models/{modelId}", "get");
        final Set<String> getInsight = OpenApiContractSupport.operationResponseStatusCodes(
                openapiYaml, "/brands/{id}/models/{modelId}/price-insight", "get");
        final Set<String> patchModel = OpenApiContractSupport.operationResponseStatusCodes(
                openapiYaml, "/brands/{id}/models/{modelId}", "patch");

        // 3.Assert
        assertTrue(getModel.contains("200"));
        assertTrue(getInsight.contains("200"));
        assertTrue(patchModel.contains("200"));
    }

    @Test
    void testHateoasUtilityGetEndpointsDocumented() {
        // 1.Arrange — openapiYaml from @BeforeAll

        // 2.Act
        final Set<String> favoriteProbe = OpenApiContractSupport.operationResponseStatusCodes(
                openapiYaml, "/users/{id}/favorites/{carId}", "get");
        final Set<String> primaryPicture = OpenApiContractSupport.operationResponseStatusCodes(
                openapiYaml, "/cars/{id}/pictures/primary", "get");
        final Set<String> messageById = OpenApiContractSupport.operationResponseStatusCodes(
                openapiYaml, "/reservations/{id}/messages/{messageId}", "get");

        // 3.Assert
        assertTrue(favoriteProbe.contains("204"));
        assertTrue(primaryPicture.contains("200"));
        assertTrue(messageById.contains("200"));
    }

    private static ApiIndexDto sampleApiIndexDto() {
        final ApiIndexDto dto = new ApiIndexDto();
        dto.setLinks(java.util.Map.of(
                "self", "http://localhost/webapp/api/",
                "cars", "http://localhost/webapp/api/cars"));
        dto.setResources(java.util.Map.of(
                "cars",
                new ApiIndexDto.ResourceDescriptor(
                        "http://localhost/webapp/api/cars", java.util.List.of("page", "pageSize", "q"))));
        return dto;
    }

    private static CarSummaryDto sampleCarSummaryDto() {
        final CarSummaryDto dto = new CarSummaryDto();
        dto.setBrandName("Toyota");
        dto.setModelName("Corolla");
        dto.setStatus("active");
        dto.setMinimumRentalDays(2);
        dto.setRatingAvg(new BigDecimal("4.50"));
        dto.setDayPrice(new BigDecimal("12000.00"));
        dto.setModelValidated(true);
        dto.setPriceMarketPositionModifier("at_market");
        dto.setMarketAveragePrice(new BigDecimal("11800.00"));
        dto.setMarketSampleCount(12L);
        dto.setLinks(LinksDto.ofSelf("/cars/10")
                .withRelated("owner", "/users/3")
                .withRelated("cover", "/cars/10/pictures/primary"));
        return dto;
    }

    private static ReservationSummaryDto sampleReservationSummaryDto() {
        final ReservationSummaryDto dto = new ReservationSummaryDto();
        dto.setStartDate("2026-06-01T10:00:00+00:00");
        dto.setEndDate("2026-06-05T18:00:00+00:00");
        dto.setStatus("pending");
        dto.setTotalPrice(new BigDecimal("15000.00"));
        dto.setBrandName("Toyota");
        dto.setModelName("Corolla");
        dto.setLinks(LinksDto.ofSelf("/reservations/1")
                .withRelated("car", "/cars/10")
                .withRelated("messages", "/reservations/1/messages")
                .withRelated("reviews", "/reviews?reservationId=1"));
        return dto;
    }

    private static ReservationDto sampleReservationDto() {
        final ReservationDto dto = new ReservationDto();
        dto.setStartDate("2026-06-01T10:00:00+00:00");
        dto.setEndDate("2026-06-05T18:00:00+00:00");
        dto.setStatus("pending");
        dto.setTotalPrice(new BigDecimal("15000.00"));
        dto.setCarReturned(false);
        dto.setPaymentProofDeadlineAt(null);
        dto.setRefundProofDeadlineAt(null);
        dto.setPaymentRefundRequired(false);
        dto.setHasPaymentReceipt(false);
        dto.setHasRefundReceipt(false);
        dto.setOwnerCbu("0000003100010000000001");
        dto.setPickupStreet("Av. Siempre Viva");
        dto.setPickupNumber("742");
        dto.setPickupNeighborhood("Palermo");
        dto.setCheckInTime("10:00");
        dto.setCheckOutTime("18:00");
        dto.setCreatedAt("2026-06-01T09:00:00+00:00");
        dto.setLinks(LinksDto.ofSelf("/reservations/1")
                .withRelated("car", "/cars/10")
                .withRelated("rider", "/users/2")
                .withRelated("owner", "/users/3")
                .withRelated("messages", "/reservations/1/messages")
                .withRelated("reviews", "/reviews?reservationId=1")
                .withRelated("payment-receipt", "/reservations/1/payment-receipt")
                .withRelated("refund-receipt", "/reservations/1/refund-receipt"));
        return dto;
    }

    private static MessageDto sampleMessageDto() {
        final MessageDto dto = new MessageDto();
        dto.setBody("hello");
        dto.setCreatedAt("2026-06-02T15:30:00+00:00");
        dto.setSeen(false);
        dto.setHasAttachment(true);
        dto.setLinks(LinksDto.ofSelf("/reservations/1/messages/9")
                .withRelated("reservation", "/reservations/1")
                .withRelated("sender", "/users/2")
                .withRelated("attachment", "/reservations/1/messages/9/attachment"));
        return dto;
    }

    private static Set<String> beanPropertyNames(final Class<?> type) throws Exception {
        final BeanInfo info = Introspector.getBeanInfo(type, Object.class);
        return Arrays.stream(info.getPropertyDescriptors())
                .map(PropertyDescriptor::getName)
                .collect(Collectors.toSet());
    }

    private static boolean beanPropertyPresent(final Object bean, final String property) throws Exception {
        final BeanInfo info = Introspector.getBeanInfo(bean.getClass(), Object.class);
        for (final PropertyDescriptor pd : info.getPropertyDescriptors()) {
            if (pd.getName().equals(property)) {
                final Object value = pd.getReadMethod().invoke(bean);
                return value != null && (!(value instanceof String s) || !s.isBlank());
            }
        }
        return false;
    }

    private static Set<String> diff(final Set<String> expected, final Set<String> actual) {
        return expected.stream().filter(v -> !actual.contains(v)).collect(Collectors.toSet());
    }
}
