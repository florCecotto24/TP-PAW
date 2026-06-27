package ar.edu.itba.paw.webapp.form.catalog;

import javax.validation.constraints.NotBlank;

import ar.edu.itba.paw.webapp.validation.constraint.car.CarValidationSize;
import ar.edu.itba.paw.webapp.validation.constraint.car.CarValidationSize.Kind;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarType;

/** Body for {@code POST /brands/{id}/models} ({@code openapi.yaml} {@code ModelCreateDto}). */
public final class ModelCreateForm {

    @NotBlank(message = "{validation.model.notBlank}")
    @CarValidationSize(kind = Kind.MODEL, messageKey = "validation.model.size")
    private String name = "";

    @NotBlank(message = "{validation.type.notNull}")
    @ValidCarType
    private String type = "";

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name != null ? name : "";
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type != null ? type : "";
    }
}
