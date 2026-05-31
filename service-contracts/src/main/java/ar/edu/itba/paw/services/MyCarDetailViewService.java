package ar.edu.itba.paw.services;

import java.util.Locale;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.dto.car.MyCarDetailPageModel;

/**
 * Read-only assembly for the {@code car/myCarDetail.jsp} page used by both
 * {@code MyCarsController.myCarDetail} (initial render) and {@code MyCarsController.editCar}
 * (error re-render). Decides between the rich variant (with the
 * {@link ar.edu.itba.paw.models.dto.car.OwnerCarDetailPageModel} + preview reservations) and the
 * fallback variant (raw car + first picture id) based on whether the owner detail page model is
 * present.
 */
public interface MyCarDetailViewService {

    /**
     * @param ownerUserId   authenticated owner id; used for the preview-reservations guard
     * @param car           ownership-resolved car (controller has already vetted access)
     * @param locale        locale used for the rich page model + preview row formatting
     * @param previewLimit  maximum number of preview rows to include in the rich variant
     */
    MyCarDetailPageModel loadMyCarDetailPage(long ownerUserId, Car car, Locale locale, int previewLimit);
}
