package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ListingSearchCriteria;

import java.util.List;

public interface ListingSearchCriteriaService {

    ListingSearchCriteria build(
            String query,
            List<String> category,
            List<String> transmission,
            List<String> powertrain,
            List<String> price,
            String from,
            String until);
}
