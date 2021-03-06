/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.routing;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;
import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrix;

public class TollAreaTravelDisutility implements TravelDisutility {

	
	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		Coord coordinatesLindenhofZH = ParkingHerbieControler.getCoordinatesLindenhofZH();
		
		if (GeneralLib.getDistance(link.getCoord(), coordinatesLindenhofZH) < ZHScenarioGlobal.loadDoubleParam("radiusTolledArea")){
			return ZHScenarioGlobal.loadDoubleParam("costForEnertingTolledArea");
		}
		
		return 0;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}


}

