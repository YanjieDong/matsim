/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.johannes.gsv.matrices.episodes2matrix;

import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.gsv.synPop.mid.run.ProxyTaskRunner;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.gsv.zones.io.Zone2GeoJSON;
import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.synpop.data.PlainPerson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

/**
 * @author johannes
 */
public class Facilities2Zones {

    private static final Logger logger = Logger.getLogger(Facilities2Zones.class);

    public static void main(String args[]) throws IOException, FactoryException {
        String popIn = args[0];
        String facIn = args[1];
        String zonesIn = args[2];
        String zoneIdKey = args[3];
        String popOut = args[4];

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);

        logger.info("Loading facilities...");
        MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
        facReader.readFile(facIn);

        logger.info("Loading zones...");
        ZoneCollection zones = new ZoneCollection();
        String data = new String(Files.readAllBytes(Paths.get(zonesIn)));
        zones.addAll(Zone2GeoJSON.parseFeatureCollection(data));
        zones.setPrimaryKey(zoneIdKey);

        logger.info("Loading persons...");
        XMLParser parser = new XMLParser();
        parser.setValidating(false);
        parser.parse(popIn);
        logger.info(String.format("Loaded %s persons...", parser.getPersons().size()));

        Set<PlainPerson> persons = parser.getPersons();

        MathTransform transform = CRS.findMathTransform(CRSUtils.getCRS(31467), DefaultGeographicCRS.WGS84);
        SetZones task = new SetZones(zones, scenario.getActivityFacilities(), zoneIdKey, transform);

        ProxyTaskRunner.run(task, persons, true);

        logger.info(String.format("%s activities could not be located in a zone.", task.getNotFound()));

        XMLWriter writer = new XMLWriter();
        writer.write(popOut, persons);
    }
}
