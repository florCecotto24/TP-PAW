package ar.edu.itba.paw.services.car.view;

import java.time.YearMonth;
import java.util.Locale;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.dto.car.ManageCarPeriodsPageModel;

/**
 * Read-only assembly for the {@code car/manageCarPeriods.jsp} page. Carries all effective
 * availability periods for the owner calendar and filters them to the active month for the
 * right-hand list panel.
 */
public interface ManageCarPeriodsViewService {

    /**
     * @param car         ownership-resolved car (controller has already vetted access)
     * @param activeMonth the month currently shown in the calendar
     * @param locale      locale for formatting display strings (e.g. month name)
     * @param page        0-based page index for the period list
     * @param pageSize    items per page for the period list
     */
    ManageCarPeriodsPageModel loadManageCarPeriodsPage(Car car, YearMonth activeMonth, Locale locale, int page, int pageSize);
}
