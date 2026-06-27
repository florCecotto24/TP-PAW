package ar.edu.itba.paw.webapp.form.catalog;

import javax.validation.constraints.NotBlank;

import ar.edu.itba.paw.webapp.validation.constraint.car.CarValidationSize;
import ar.edu.itba.paw.webapp.validation.constraint.car.CarValidationSize.Kind;

/** Body for {@code POST /brands} ({@code openapi.yaml} {@code BrandCreateDto}). */
public final class BrandCreateForm {

    @NotBlank(message = "{validation.brand.notBlank}")
    @CarValidationSize(kind = Kind.BRAND, messageKey = "validation.brand.size")
    private String name = "";

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name != null ? name : "";
    }
}
