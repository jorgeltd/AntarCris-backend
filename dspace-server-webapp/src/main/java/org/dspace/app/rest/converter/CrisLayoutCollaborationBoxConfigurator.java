/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.CrisLayoutBoxConfigurationRest;
import org.dspace.app.rest.model.CrisLayoutBoxRelationConfigurationRest;
import org.dspace.core.Context;
import org.dspace.layout.*;
import org.springframework.stereotype.Component;

/**
 * This is the configurator for metadata layout box
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutCollaborationBoxConfigurator implements CrisLayoutBoxConfigurator {

    @Override
    public boolean support(CrisLayoutBox box) {
        // Only support collaboration box for person entityType
        if (StringUtils.equals(box.getEntitytype().getLabel(), "Person")){
            return StringUtils.equals(box.getType(), CrisLayoutBoxTypes.COLLABORATION.name());
        }
        return false;
    }

    @Override
    public CrisLayoutBoxConfigurationRest getConfiguration(CrisLayoutBox box) {
        CrisLayoutBoxRelationConfigurationRest rest = new CrisLayoutBoxRelationConfigurationRest();
        StringBuilder discoveryConfiguration = new StringBuilder(CrisLayoutBoxTypes.COLLABORATION.name());
        discoveryConfiguration.append(".");
        discoveryConfiguration.append(box.getEntitytype().getLabel());
        discoveryConfiguration.append(".");
        discoveryConfiguration.append(box.getShortname());
        rest.setDiscoveryConfiguration(discoveryConfiguration.toString());
        return rest;
    }

    @Override
    public void configure(Context context, CrisLayoutBox box, CrisLayoutBoxConfigurationRest rest) {
        // Nothing to do
    }
}
