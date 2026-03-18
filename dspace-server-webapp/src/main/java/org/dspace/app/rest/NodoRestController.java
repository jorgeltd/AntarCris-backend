package org.dspace.app.rest;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.*;
import org.dspace.app.rest.model.hateoas.SearchResultsResource;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.repository.NodoRepository;
import org.dspace.app.rest.repository.DiscoveryRestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.dspace.app.rest.utils.Utils;
import java.time.Year;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

import java.util.*;

@RestController
@RequestMapping("/api/nodo")

public class NodoRestController {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private NodoRepository nodoRepository;

    @Autowired
    private DiscoveryRestRepository discoveryRestRepository;

    @Autowired
    protected Utils utils;

    @RequestMapping(method = RequestMethod.GET, path = "/production-yield")
    public Map<String, Integer> getYearlyProduction(@RequestParam(name = "startYear") Integer startYear,
                                                    @RequestParam(name = "endYear", required = false) Integer endYear) {

        if (endYear == null) {
            endYear = Year.now().getValue();
        }

        if (log.isTraceEnabled()) {
            log.trace("Searching products with query startYear:{} endYear:{}", startYear, endYear);
        }

        List<SearchFilter> searchFilters = new ArrayList<>();

        return nodoRepository.yearlyProduction(startYear, endYear, searchFilters, null);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/collaborations")
    public Dictionary<String, Integer> getCollaborations(
            @RequestParam(name = "dsoType", required = false)
            List<String> dsoTypes,
            @RequestParam(name = "scope", required = false) String dsoScope,
            @RequestParam(name = "configuration", required = false, defaultValue = "RELATION.OrgUnit.rppublications") String configuration,
            @RequestParam(name = "entitytype", required = false, defaultValue = "OrgUnit") String entityType
    ) throws Exception {

        // Search
        Pageable page = createPageRequestUsing(0, 10000);
        String query = "";
        List<SearchFilter> searchFilters = new ArrayList<SearchFilter>();
        SearchResultsRest searchResultsRest = discoveryRestRepository.getSearchObjects(query, dsoTypes, dsoScope,
                configuration, searchFilters, page, utils.obtainProjection());
        List<SearchResultEntryRest> values = searchResultsRest.getSearchResults();

        // Init countries
        Dictionary<String, Integer> countryStr = new Hashtable<>();

        // Loop through results
        for (SearchResultEntryRest entry : values) {
            RestAddressableModel indexableObject = entry.getIndexableObject();
            MetadataRest<MetadataValueRest> metadataMap = ((ItemRest) indexableObject).getMetadata();
            SortedMap<String, List<MetadataValueRest>> metadata = metadataMap.getMap();
            List<MetadataValueRest> metadataAuthors = metadata.get("dc.contributor.author");
            List<MetadataValueRest> metadataAffiliation = metadata.get("oairecerif.author.affiliation");
            List<MetadataValueRest> metadataCountries = metadata.get("antarc.affiliation.country");

            if (entityType.equals("OrgUnit")) {
                if (metadataAffiliation != null && metadataCountries != null) {
                    for (int i = 0; i < metadataAffiliation.size(); i++) {
                        String affiliation = metadataAffiliation.get(i).getValue();
                        String authority = metadataAffiliation.get(i).getAuthority();
                        if (!affiliation.equals(PLACEHOLDER_PARENT_METADATA_VALUE) && (authority == null || !authority.equals(dsoScope))) {
                            String country = metadataCountries.get(i).getValue();
                            if (!country.equals(PLACEHOLDER_PARENT_METADATA_VALUE) && countryStr.get(country) == null) {
                                countryStr.put(country, 1);
                            } else if (!country.equals(PLACEHOLDER_PARENT_METADATA_VALUE)) {
                                countryStr.put(country, countryStr.get(country) + 1);
                            }

                        }
                    }
                }
            }

            if (entityType.equals("Person")) {
                if (metadataAuthors != null) {
                    for (int i = 0; i < metadataAuthors.size(); i++) {
                        String authorAuthority = metadataAuthors.get(i).getAuthority();

                        if (authorAuthority == null || !authorAuthority.equals(dsoScope)) {
                            String country = metadataCountries.get(i).getValue();
                            if (!country.equals(PLACEHOLDER_PARENT_METADATA_VALUE) && countryStr.get(country) == null) {
                                countryStr.put(country, 1);
                            } else if (!country.equals(PLACEHOLDER_PARENT_METADATA_VALUE)) {
                                countryStr.put(country, countryStr.get(country) + 1);
                            }
                        }
                    }
                }
            }


        }

        return countryStr;
    }

    private Pageable createPageRequestUsing(int page, int size) {
        return PageRequest.of(page, size);
    }

}