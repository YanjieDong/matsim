package org.matsim.contrib.accessibility;

import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.ZoneDataExchangeInterface;
import org.matsim.facilities.ActivityFacility;

import java.util.HashMap;
import java.util.Map;

class SpatialGridAggregator implements ZoneDataExchangeInterface {

	private Map<Modes4Accessibility,SpatialGrid> accessibilityGrids = new HashMap<>() ;

	@Override
	public void setZoneAccessibilities(ActivityFacility origin, Node fromNode, Map<Modes4Accessibility, Double> accessibilities) {
		for (Map.Entry<Modes4Accessibility, Double> modes4AccessibilityDoubleEntry : accessibilities.entrySet()) {
			accessibilityGrids.get(modes4AccessibilityDoubleEntry.getKey()).setValue(modes4AccessibilityDoubleEntry.getValue(), origin.getCoord().getX(), origin.getCoord().getY());
		}
	}

	Map<Modes4Accessibility, SpatialGrid> getAccessibilityGrids() {
		return accessibilityGrids;
	}

}
