package ar.edu.itba.paw.webapp.contract;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.webapp.api.common.VndMediaType;

/**
 * Contract tests: every {@link VndMediaType} constant is declared in {@code openapi.yaml}
 * and the API stays JSON-only (no {@code +xml}, no generic {@code application/json}).
 */
class VendorMediaTypeContractTest {

    private static String openapiYaml;
    private static Set<String> codeMediaTypes;

    @BeforeAll
    static void loadContract() {
        openapiYaml = OpenApiContractSupport.loadOpenApiYaml();
        codeMediaTypes = Arrays.stream(VndMediaType.class.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == String.class)
                .map(VendorMediaTypeContractTest::readConstant)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Test
    void testVendorMediaTypesInCodeAreDeclaredInOpenApi() {
        // 1.Arrange
        final Set<String> inSpec = OpenApiContractSupport.vendorJsonMediaTypes(openapiYaml);

        // 2.Act
        final Set<String> missing = codeMediaTypes.stream()
                .filter(type -> !inSpec.contains(type))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 3.Assert
        assertTrue(missing.isEmpty(), () -> "openapi.yaml missing MIME types from VndMediaType: " + missing);
    }

    @Test
    void testOpenApiHasNoXmlVendorMediaTypes() {
        // 1.Arrange — openapiYaml from @BeforeAll

        // 2.Act / 3.Assert
        assertFalse(openapiYaml.contains("+xml"), "openapi.yaml must not document +xml vendor types");
    }

    @Test
    void testVndMediaTypeHasNoXmlConstants() {
        // 1.Arrange — codeMediaTypes from @BeforeAll

        // 2.Act
        final Set<String> xmlTypes = codeMediaTypes.stream()
                .filter(type -> type.contains("+xml"))
                .collect(Collectors.toSet());

        // 3.Assert
        assertTrue(xmlTypes.isEmpty(), () -> "VndMediaType must not define +xml: " + xmlTypes);
    }

    @Test
    void testSummaryMediaTypesAreDeclaredInOpenApi() {
        // 1.Arrange
        final Set<String> inSpec = OpenApiContractSupport.vendorJsonMediaTypes(openapiYaml);

        // 2.Act / 3.Assert
        assertTrue(inSpec.contains(VndMediaType.CAR_SUMMARY_V1_JSON));
        assertTrue(inSpec.contains(VndMediaType.RESERVATION_SUMMARY_V1_JSON));
    }

    private static String readConstant(final Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Could not read VndMediaType." + field.getName(), ex);
        }
    }
}
