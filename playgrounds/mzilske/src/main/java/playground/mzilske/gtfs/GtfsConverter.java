package playground.mzilske.gtfs;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;


public class GtfsConverter {

	private String filepath = "";

	private CoordinateTransformation transform;

	private ScenarioImpl scenario;
	private Map<String,Integer> vehicleIdsAndTypes = new HashMap<String,Integer>();
	private Map<Id,Integer> lineToVehicleType = new HashMap<Id,Integer>();
	private Map<Id,Id> matsimRouteIdToGtfsTripIdAssignments = new HashMap<Id,Id>();
	private boolean createShapedNetwork = false;

	private long date = 0;


	// Fields for shaped Network
	// (TripId,ShapeId)
	private Map<String,String> shapeIdToTripIdAssignments = new HashMap<String,String>();
	// (LinkId,(TripId,FromShapeDist,ToShapeDist))
	private Map<Id,String[]> shapedLinkIds = new HashMap<Id,String[]>();
	// If there is no shape_dist_traveled field, try to identify the stations by its coordinates
	// (LinkId,(TripId,FromCoordX, FromCoordY ,ToCoordX, ToCoordY)) - Both Coordinates as Strings and in Matsim-KS
	private Map<Id,String[]> shapedLinkIdsCoordinate = new HashMap<Id,String[]>();
	private boolean alternativeStationToShapeAssignment = false;
	private double toleranceInM = 0;
	// (ShapeId, (shapeDist, x, y))
	private Map<Id,List<String[]>> shapes = new HashMap<Id,List<String[]>>();



	private TransitSchedule ts;


	public GtfsConverter(String filepath, Scenario scenario, CoordinateTransformation transform) {
		this.filepath = filepath;
		this.transform = transform;
		this.scenario = (ScenarioImpl) scenario;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public void setCreateShapedNetwork(boolean createShapedNetwork) {
		if(((new File(filepath + "/shapes.txt")).exists()) && (createShapedNetwork)){
			this.createShapedNetwork = createShapedNetwork;
		}else if(createShapedNetwork){
			System.out.println("Couldn't find the 'shapes.txt'. No shaped network will be created!");
		}else if(!createShapedNetwork){
			this.createShapedNetwork = createShapedNetwork;
		}
	}

	@SuppressWarnings("deprecation")
	public void convert(){
		// Parse required Files
		System.out.println("Parse required Files...");
		GtfsSource stopsSource = GtfsSource.parseGtfsFile(filepath + "/stops.txt");
		GtfsSource routesSource = GtfsSource.parseGtfsFile(filepath + "/routes.txt");
		GtfsSource tripSource = GtfsSource.parseGtfsFile(filepath + "/trips.txt");
		GtfsSource stopTimesSource = GtfsSource.parseGtfsFile(filepath + "/stop_times.txt");

		// Parse optional Files
		System.out.println("Parse optional Files...");
		String calendarFilename = filepath + "/calendar.txt";
		GtfsSource calendarSource = null;
		if((new File(calendarFilename)).exists()){
			calendarSource = GtfsSource.parseGtfsFile(calendarFilename);
		}
		String calendarDatesFilename = filepath + "/calendar_dates.txt";
		GtfsSource calendarDatesSource = null;
		if((new File(calendarDatesFilename)).exists()){
			calendarDatesSource = GtfsSource.parseGtfsFile(calendarDatesFilename);
		}
		String frequenciesFilename = filepath + "/frequencies.txt";
		GtfsSource frequenciesSource = null;
		if((new File(frequenciesFilename)).exists()){
			frequenciesSource = GtfsSource.parseGtfsFile(frequenciesFilename);
		}
		String shapesFilename = filepath + "/shapes.txt";;
		GtfsSource shapesSource = null;
		if(this.createShapedNetwork){
			if((new File(shapesFilename)).exists()){
				shapesSource = GtfsSource.parseGtfsFile(shapesFilename);
			}else{
				System.out.println(shapesFilename + " doesn't exist - no shaped network will be created");
				this.createShapedNetwork = false;
			}
		}

		this.ts = scenario.getTransitSchedule();

		// Put all stops in the Schedule
		this.convertStops(stopsSource);

		// Get the Routenames and the assigned Trips
		Map<Id, Id> gtfsRouteToMatsimLineID = getMatsimLineIds(routesSource);

		Map<Id,Id> tripToRoute = getTripToRouteMap(tripSource,gtfsRouteToMatsimLineID);

		// Create Transitlines
		this.createTransitLines(gtfsRouteToMatsimLineID);



		// Get the used service Id for the choosen weekday and date
		List<String> usedServiceIds = new ArrayList<String>();
		if((new File(calendarFilename)).exists()){
			System.out.println("Reading calendar.txt");
			usedServiceIds.addAll(this.getUsedServiceIds(calendarSource));
		}
		if((new File(calendarDatesFilename)).exists()){
			System.out.println("Reading calendar_dates.txt");
			for(String serviceId: this.getUsedServiceIdsForSpecialDates(calendarDatesSource)){
				if(serviceId.charAt(0) == '+'){
					usedServiceIds.add(serviceId.substring(1));
				}else{
					if(usedServiceIds.contains(serviceId.substring(1))){
						usedServiceIds.remove(serviceId.substring(1));
					}
				}
			}
		}		
		System.out.println("Reading of ServiceIds succesfull: " + usedServiceIds);

		// Get the TripIds, which are available for the serviceIds
		List<Id> usedTripIds = this.getUsedTripIds(tripSource, usedServiceIds);
		System.out.println("Reading of TripIds succesfull: " + usedTripIds);

		// Create the Network
		System.out.println("Creating Network");
		this.createNetworkOfStopsAndTrips(stopTimesSource, ts);

		// Get the TripRoutes
		Map<Id,NetworkRoute> tripRoute;
		System.out.println("Create NetworkRoutes");
		tripRoute = createNetworkRoutes(stopTimesSource);
		if(this.createShapedNetwork){
			System.out.println("Creating shaped Network");
			this.convertShapes(shapesSource);
			Map<Id, List<Coord>> shapedLinks = this.assignShapesToLinks();
			NetworkEnricher networkEnricher = new NetworkEnricher(scenario.getNetwork());
			tripRoute = networkEnricher.replaceLinks(shapedLinks, tripRoute);
			scenario.setNetwork(networkEnricher.getEnrichedNetwork());
		}

		// Convert the schedules for the trips
		System.out.println("Convert the schedules");
		this.convertSchedules(stopTimesSource, tripToRoute, tripRoute);

		// If you use the optional frequencies.txt, it will be transformed here
		if((new File(frequenciesFilename)).exists()){
			this.convertFrequencies(frequenciesSource, tripToRoute, usedTripIds);
		}

		this.createTransitVehicleTypes();
		this.createTransitVehicles();

		if(usedTripIds.isEmpty()){
			System.out.println("There are no converted trips. You might need to change the date for better results.");
		}
		System.out.println("Conversion successfull");
	}

	private List<Id> getUsedTripIds(GtfsSource tripsSource, List<String> usedServiceIds) {
		List<Id> usedTripIds = new ArrayList<Id>();
		int serviceIdIndex = tripsSource.getContentIndex("service_id");
		int tripIdIndex = tripsSource.getContentIndex("trip_id");
		int shapeIdIndex = tripsSource.getContentIndex("shape_id");
		int headsignIndex = tripsSource.getContentIndex("trip_headsign");
		for (String[] entries : tripsSource.getContent()) {
			if (usedServiceIds.contains(entries[serviceIdIndex])) {
				usedTripIds.add(new IdImpl(entries[tripIdIndex]));
			}
			if (this.createShapedNetwork) {
				if (shapeIdIndex > 0) {
					String shapeId = entries[shapeIdIndex];
					this.shapeIdToTripIdAssignments.put(entries[tripIdIndex],
							shapeId);
				} else {
					System.out
					.println("WARNING: Couldn't find shape_id header in trips.txt. Deactivating creation of shaped network");
					this.createShapedNetwork = false;
				}
			}
			if(headsignIndex>0){
				String headsign = entries[headsignIndex];
				String oldId = entries[tripIdIndex];
				oldId = oldId.replace("_", "-");
				headsign = headsign.replace("_", "-");
				this.matsimRouteIdToGtfsTripIdAssignments.put(new IdImpl(entries[tripIdIndex]), new IdImpl(oldId + "_" + headsign));
			}else{
				this.matsimRouteIdToGtfsTripIdAssignments.put(new IdImpl(entries[tripIdIndex]), new IdImpl(entries[tripIdIndex]));
			} 							
		}
		return usedTripIds;
	}

	private Map<Id, Id> getMatsimLineIds(GtfsSource routesSource) {
		Map<Id, Id> routeNames = new HashMap<Id, Id>();
		int routeIdIndex = routesSource.getContentIndex("route_id");
		int routeShortNameIndex = routesSource.getContentIndex("route_short_name");
		int routeTypeIndex = routesSource.getContentIndex("route_type");
		for(String[] entries: routesSource.getContent()){
			String shortName = entries[routeShortNameIndex];
			String oldId = entries[routeIdIndex];
			oldId = oldId.replace("_", "-");
			shortName = shortName.replace("_", "-");
			if(shortName.equals("")){
				System.out.println("Route " + oldId + " has no short route name");
				shortName = "NoShortRouteName";
			}
			Id newId = new IdImpl(oldId + "_" + shortName);
			routeNames.put(new IdImpl(entries[routeIdIndex]),newId);
			this.lineToVehicleType.put(newId, Integer.parseInt(entries[routeTypeIndex].trim()));
		}
		return routeNames;
	}

	private Map<Id,Id> getTripToRouteMap(GtfsSource tripsSource, Map<Id, Id> gtfsToMatsimRouteIdAssingments){
		Map<Id,Id> routeTripAssignment = new HashMap<Id,Id>();
		int routeIdIndex = tripsSource.getContentIndex("route_id");
		int tripIdIndex = tripsSource.getContentIndex("trip_id");
		for(String[] entries: tripsSource.getContent()) {
			routeTripAssignment.put(new IdImpl(entries[tripIdIndex]), gtfsToMatsimRouteIdAssingments.get(new IdImpl(entries[routeIdIndex])));				
		}
		return routeTripAssignment;		
	}

	private List<String> getUsedServiceIds(GtfsSource calendarSource) {
		List<String> serviceIds = new ArrayList<String>();
		int serviceIdIndex = calendarSource.getContentIndex("service_id");
		int startDateIndex = calendarSource.getContentIndex("start_date");
		int endDateIndex = calendarSource.getContentIndex("end_date");
		int[] weekdayIndexes= new int[7];
		weekdayIndexes[0] = calendarSource.getContentIndex("monday");
		weekdayIndexes[1] = calendarSource.getContentIndex("tuesday");
		weekdayIndexes[2] = calendarSource.getContentIndex("wednesday");
		weekdayIndexes[3] = calendarSource.getContentIndex("thursday");
		weekdayIndexes[4] = calendarSource.getContentIndex("friday");
		weekdayIndexes[5] = calendarSource.getContentIndex("saturday");
		weekdayIndexes[6] = calendarSource.getContentIndex("sunday");
		for(String[] entries: calendarSource.getContent()){
			int weekday;
			if(this.date == 0){
				this.date = Long.parseLong(entries[endDateIndex].trim());
				weekday = this.getWeekday(date);
				System.out.println("Used Date for active schedules: " + this.date + " (weekday: " + weekday + "). If you want to choose another date, please specify it, before running the converter");
			}else{
				weekday = this.getWeekday(date);
			}
			if(entries[weekdayIndexes[weekday-1]].equals("1")){
				if((this.date >= Double.parseDouble(entries[startDateIndex].trim())) && (this.date <= Double.parseDouble(entries[endDateIndex].trim()))){
					serviceIds.add(entries[serviceIdIndex]);
				}
			}
		}
		return serviceIds;
	}

	private List<String> getUsedServiceIdsForSpecialDates(GtfsSource calendarDatesSource){
		List<String> serviceIds = new ArrayList<String>();
		int serviceIdIndex = calendarDatesSource.getContentIndex("service_id");
		int dateIndex = calendarDatesSource.getContentIndex("date");
		int exceptionTypeIndex = calendarDatesSource.getContentIndex("exception_type");
		for (String[] entries : calendarDatesSource.getContent()) {
			String serviceId = entries[serviceIdIndex];
			long exceptionDate = Long.parseLong(entries[dateIndex].trim());
			int exceptionType = Integer.parseInt(entries[exceptionTypeIndex].trim());
			if (this.date == 0) {
				this.date = exceptionDate;
				System.out.println("Used Date for active schedules: " + this.date + ". If you want to choose another date, please specify it, before running the converter");
			}
			if (exceptionDate == this.date) {
				if (exceptionType == 1) {
					serviceIds.add("+" + serviceId);
				} else {
					serviceIds.add("-" + serviceId);
				}
			}
		}
		return serviceIds;
	}

	private void convertFrequencies(GtfsSource frequenciesSource, Map<Id, Id> routeToTripAssignments, List<Id> usedTripIds) {
		int tripIdIndex = frequenciesSource.getContentIndex("trip_id");
		int startTimeIndex = frequenciesSource.getContentIndex("start_time");
		int endTimeIndex = frequenciesSource.getContentIndex("end_time");
		int stepIndex = frequenciesSource.getContentIndex("headway_secs");
		int departureCounter = 2;
		String oldTripId = "";
		for(String[] entries: frequenciesSource.getContent()){
			Id tripId = new IdImpl(entries[tripIdIndex]);
			double startTime = Time.parseTime(entries[startTimeIndex].trim());
			double endTime = Time.parseTime(entries[endTimeIndex].trim());
			double step = Double.parseDouble(entries[stepIndex]);
			if((!(entries[tripIdIndex].equals(oldTripId))) && (usedTripIds.contains(tripId))){
				departureCounter = ts.getTransitLines().get(routeToTripAssignments.get(tripId)).getRoutes().get(this.matsimRouteIdToGtfsTripIdAssignments.get(tripId)).getDepartures().size();
			}
			if(usedTripIds.contains(tripId)){
				Map<Id,Departure> depatures = ts.getTransitLines().get(routeToTripAssignments.get(tripId)).getRoutes().get(this.matsimRouteIdToGtfsTripIdAssignments.get(tripId)).getDepartures();
				double latestDeparture = 0;
				for(Departure d: depatures.values()){
					if(latestDeparture < d.getDepartureTime()){
						latestDeparture = d.getDepartureTime();
					}
				}
				double time = latestDeparture + step;				
				do{
					if(time>startTime){
						Departure d = ts.getFactory().createDeparture(new IdImpl(tripId.toString() + "." + departureCounter), time);
						d.setVehicleId(new IdImpl(tripId.toString() + "." + departureCounter));
						this.vehicleIdsAndTypes.put(tripId.toString() + "." + departureCounter,this.lineToVehicleType.get(routeToTripAssignments.get(tripId)));
						ts.getTransitLines().get(routeToTripAssignments.get(tripId)).getRoutes().get(this.matsimRouteIdToGtfsTripIdAssignments.get(tripId)).addDeparture(d);
						departureCounter++;
					}						
					time = time + step;
				}while(time <= endTime);		
			}
			oldTripId = entries[tripIdIndex];
		}
	}


	private void convertSchedules(GtfsSource stopTimesSource, Map<Id, Id> routeToTripAssignments, Map<Id, NetworkRoute> tripRoute){
		List<TransitRouteStop> stops = new LinkedList<TransitRouteStop>();
		int tripIdIndex = stopTimesSource.getContentIndex("trip_id");
		int arrivalTimeIndex = stopTimesSource.getContentIndex("arrival_time");
		int departureTimeIndex = stopTimesSource.getContentIndex("departure_time");
		int stopIdIndex = stopTimesSource.getContentIndex("stop_id");
		String[] firstEntry = stopTimesSource.getContent().get(0);
		String currentTrip = firstEntry[tripIdIndex];
		Double departureTime = Time.parseTime(firstEntry[arrivalTimeIndex].trim());
		Departure departure = ts.getFactory().createDeparture(new IdImpl(firstEntry[tripIdIndex]), departureTime);
		String vehicleId = firstEntry[tripIdIndex];
		departure.setVehicleId(new IdImpl(vehicleId));		
		this.vehicleIdsAndTypes.put(vehicleId,lineToVehicleType.get(routeToTripAssignments.get(new IdImpl(firstEntry[tripIdIndex]))));
		int nRows = stopTimesSource.getContent().size();
		int nRow = 1;
		for(String[] entries: stopTimesSource.getContent()) {		
			System.out.println(nRow++ + "/" + nRows);
			Id currentTripId = new IdImpl(currentTrip);
			Id tripId = new IdImpl(entries[tripIdIndex]);
			Id stopId = new IdImpl(entries[stopIdIndex]);
			if(entries[tripIdIndex].equals(currentTrip)){
				TransitStopFacility stop = ts.getFacilities().get(stopId);
				TransitRouteStop routeStop = ts.getFactory().createTransitRouteStop(stop, Time.parseTime(entries[arrivalTimeIndex].trim())-departureTime, Time.parseTime(entries[departureTimeIndex].trim())-departureTime);
				stops.add(routeStop);	
			}else{
				//finish old route
				stops = this.interpolateMissingDepartures(stops);
				TransitLine tl = ts.getTransitLines().get(routeToTripAssignments.get(currentTripId));
				Id routeId = this.matsimRouteIdToGtfsTripIdAssignments.get(currentTripId);
				NetworkRoute networkRoute = tripRoute.get(currentTripId);
				TransitRoute tr = findOrAddTransitRoute(tl, stops,  networkRoute, routeId);
				tr.addDeparture(departure);
				stops = new LinkedList<TransitRouteStop>();
				//begin new route
				departureTime = Time.parseTime(entries[arrivalTimeIndex].trim());
				departure = ts.getFactory().createDeparture(new IdImpl(entries[tripIdIndex]), departureTime);
				vehicleId = tripId.toString();
				departure.setVehicleId(new IdImpl(vehicleId));
				this.vehicleIdsAndTypes.put(vehicleId,lineToVehicleType.get(routeToTripAssignments.get(tripId)));												
				TransitStopFacility stop = ts.getFacilities().get(stopId);
				TransitRouteStop routeStop = ts.getFactory().createTransitRouteStop(stop, 0, Time.parseTime(entries[departureTimeIndex].trim())-departureTime);
				stops.add(routeStop);
				currentTrip = entries[tripIdIndex];						
			}						
		}
		// The last trip of the file was not added, so it needs to be added now
		Id currentTripId = new IdImpl(currentTrip);
		//finish old route
		stops = this.interpolateMissingDepartures(stops);
		TransitLine tl = ts.getTransitLines().get(routeToTripAssignments.get(currentTripId));
		Id routeId = this.matsimRouteIdToGtfsTripIdAssignments.get(currentTripId);
		NetworkRoute networkRoute = tripRoute.get(currentTripId);
		TransitRoute tr = findOrAddTransitRoute(tl, stops,  networkRoute, routeId);
		tr.addDeparture(departure);
	}

	private TransitRoute findOrAddTransitRoute(TransitLine tl, List<TransitRouteStop> stops, NetworkRoute networkRoute, Id routeId) {
		for (TransitRoute tr : tl.getRoutes().values()) {
			if (tr.getStops().equals(stops)) {
				System.out.println("Saved a route.");
				return tr;
			} 
		}
		TransitRoute tr = ts.getFactory().createTransitRoute(routeId, networkRoute, stops, "pt");
		tl.addRoute(tr);
		return tr;
	}

	/**
	 * Beats me how this works. michaz '13
	 * @param stops
	 * @return
	 */
	private List<TransitRouteStop> interpolateMissingDepartures(List<TransitRouteStop> stops) {
		List<TransitRouteStop> result = new ArrayList<TransitRouteStop>();
		List<TransitRouteStop> toBeInterpolated = new ArrayList<TransitRouteStop>();
		Map<Integer,TransitRouteStop> toBeReplaced = new HashMap<Integer,TransitRouteStop>();
		boolean properDeparture = true;
		int lastProperDepartureIndex = 0;
		for(Iterator<TransitRouteStop> it = stops.iterator(); it.hasNext(); ){
			TransitRouteStop s = it.next();
			if(Double.isInfinite(s.getDepartureOffset())){
				toBeInterpolated.add(s);
				properDeparture = false;
			}else if(!properDeparture){
				double totalLength = 0;
				TransitRouteStop lastProperStop = stops.get(lastProperDepartureIndex);
				for(TransitRouteStop sr: toBeInterpolated){
					totalLength = CoordUtils.calcDistance(sr.getStopFacility().getCoord(), lastProperStop.getStopFacility().getCoord()) + totalLength;
				}
				totalLength = totalLength + CoordUtils.calcDistance(toBeInterpolated.get(toBeInterpolated.size()-1).getStopFacility().getCoord(), s.getStopFacility().getCoord());
				double timeAvaible = s.getArrivalOffset() - lastProperStop.getDepartureOffset();
				double oldDepartureOffset = lastProperStop.getDepartureOffset();
				for(Iterator<TransitRouteStop> it2 = toBeInterpolated.iterator(); it2.hasNext();){
					TransitRouteStop sr = it2.next();
					double newDepartureOffset = (CoordUtils.calcDistance(sr.getStopFacility().getCoord(), lastProperStop.getStopFacility().getCoord()))/(totalLength) * timeAvaible + oldDepartureOffset;
					oldDepartureOffset = newDepartureOffset;
					TransitRouteStop newStop = ts.getFactory().createTransitRouteStop(sr.getStopFacility(), newDepartureOffset, newDepartureOffset);
					toBeReplaced.put(stops.indexOf(sr), newStop);
				}
				toBeInterpolated = new ArrayList<TransitRouteStop>();
				lastProperDepartureIndex = stops.indexOf(s);
				properDeparture = true;
			}else{
				lastProperDepartureIndex = stops.indexOf(s);
			}
		}
		for(TransitRouteStop s: stops){
			if(toBeReplaced.containsKey(stops.indexOf(s))){
				s.setAwaitDepartureTime(false);
				result.add(toBeReplaced.get(stops.indexOf(s)));
			}else{
				s.setAwaitDepartureTime(true);
				result.add(s);
			}
		}
		return result;
	}


	/**
	 * Problem: Will also generate stops, and links for stops, which are disconnected from the network because
	 * they are not used by any line. These will have an invalid link reference. :(
	 * @param stopsSource
	 */
	private void convertStops(GtfsSource stopsSource){
		int stopIdIndex = stopsSource.getContentIndex("stop_id");
		int stopNameIndex = stopsSource.getContentIndex("stop_name");
		int stopLatitudeIndex = stopsSource.getContentIndex("stop_lat");
		int stopLongitudeIndex = stopsSource.getContentIndex("stop_lon");
		for(String[] entries: stopsSource.getContent()){
			TransitStopFacility t = this.ts.getFactory().createTransitStopFacility(new IdImpl(entries[stopIdIndex]), transform.transform(new CoordImpl(Double.parseDouble(entries[stopLongitudeIndex]), Double.parseDouble(entries[stopLatitudeIndex]))), false);
			t.setName(entries[stopNameIndex]);
			ts.addStopFacility(t);
		}		
	}

	private Map<Id,NetworkRoute> createNetworkRoutes(GtfsSource stopTimesSource) {
		Map<Id,NetworkRoute> tripRoutes = new HashMap<Id,NetworkRoute>();
		int tripIdIndex = stopTimesSource.getContentIndex("trip_id");
		int stopIdIndex = stopTimesSource.getContentIndex("stop_id");
		String[] firstEntry = stopTimesSource.getContent().get(0);			
		LinkedList<Id> route = new LinkedList<Id>();
		String currentTrip = firstEntry[tripIdIndex];
		String startStation = firstEntry[stopIdIndex];
		for(String[] entries: stopTimesSource.getContent()) {
			String nextStation = entries[stopIdIndex];				
			if(currentTrip.equals(entries[tripIdIndex])){					
				if(!(startStation.equals(nextStation))){
					IdImpl startStationNodeId = new IdImpl(startStation);
					IdImpl nextStationNodeId = new IdImpl(nextStation);
					Id linkId = findLinkFromNodeToNode(startStationNodeId, nextStationNodeId);
					route.add(linkId);
					route.add(new IdImpl("dL2_" + nextStation));
					route.add(ts.getFacilities().get(new IdImpl(entries[stopIdIndex])).getLinkId());
				}else{
					route.add(ts.getFacilities().get(new IdImpl(entries[stopIdIndex])).getLinkId());
				}
				startStation = nextStation;
			}else{
				NetworkRoute netRoute = (NetworkRoute) (new LinkNetworkRouteFactory()).createRoute(route.getFirst(), route.getLast());
				if(route.size() > 2){
					netRoute.setLinkIds(route.getFirst(), route.subList(1, route.size()-1), route.getLast());
				}
				tripRoutes.put(new IdImpl(currentTrip), netRoute);
				// Start new Route
				currentTrip = entries[tripIdIndex];
				route = new LinkedList<Id>();
				route.add(ts.getFacilities().get(new IdImpl(entries[stopIdIndex])).getLinkId());
				startStation = entries[stopIdIndex];
			}
		}		
		NetworkRoute netRoute = (NetworkRoute) (new LinkNetworkRouteFactory()).createRoute(route.getFirst(), route.getLast());
		if(route.size() > 2){
			netRoute.setLinkIds(route.getFirst(), route.subList(1, route.size()-1), route.getLast());
		}
		tripRoutes.put(new IdImpl(currentTrip), netRoute);
		return tripRoutes;		
	}


	private Id findLinkFromNodeToNode(IdImpl fromNodeId, IdImpl toNodeId) {
		Id linkId = null;
		for(Id fromId: scenario.getNetwork().getNodes().get(fromNodeId).getOutLinks().keySet()){
			for(Id toId: scenario.getNetwork().getNodes().get(toNodeId).getInLinks().keySet()){
				if(fromId.equals(toId)){
					linkId = fromId;
				}
			}
		}
		return linkId;
	}

	private void createTransitLines(Map<Id, Id> gtfsToMatsimRouteId) {
		for(Id id: gtfsToMatsimRouteId.keySet()){
			TransitLine tl = ts.getFactory().createTransitLine(gtfsToMatsimRouteId.get(id));
			ts.addTransitLine(tl);
		}		
	}

	private void createNetworkOfStopsAndTrips(GtfsSource stopTimesSource, TransitSchedule ts){
		double freespeedKmPerHour=50;
		double capacity = 1500.0;
		int numLanes = 1;
		long i = 0;
		// To prevent the creation of similiar links in different directions there need to be a Map which contains all existing connections
		Map<Id,List<Id>> fromNodes = new HashMap<Id,List<Id>>();
		// Create a new Network
		Network network = scenario.getNetwork();
		// Add all stops as nodes
		Map<Id, TransitStopFacility> stops = ts.getFacilities();
		Map<Id, ? extends Node> nodes = network.getNodes();
		for(Id id: stops.keySet()){
			TransitStopFacility stop = stops.get(id);
			NodeImpl n = new NodeImpl(id);
			n.setCoord(stop.getCoord());
			network.addNode(n);
			createDummyNodeAndLinks(id, capacity, numLanes, network, nodes);
			stop.setLinkId(new IdImpl("dL1_"+ stop.getId().toString()));
		}
		// Get the Links from the trips in stopTimesSource
		int tripIdIndex = stopTimesSource.getContentIndex("trip_id");
		int stopIdIndex = stopTimesSource.getContentIndex("stop_id");
		int arrivalTimeIndex = stopTimesSource.getContentIndex("arrival_time");
		int departureTimeIndex = stopTimesSource.getContentIndex("departure_time");
		int shapeDistIndex = stopTimesSource.getContentIndex("shape_dist_traveled");
		if((shapeDistIndex < 0) && (this.createShapedNetwork)){
			System.out.println("Couldn't find the shape_dist_traveled field in stop_times.txt. Now it uses the alternative station to shape assingment.");
			this.alternativeStationToShapeAssignment = true;
		}
		String[] entries = stopTimesSource.getContent().get(0);
		for(Iterator<String[]> it = stopTimesSource.getContent().iterator(); it.hasNext();) {
			boolean addLink = false;	
			Id fromNodeId = new IdImpl(entries[stopIdIndex]);
			double departureTime = Time.parseTime(entries[departureTimeIndex].trim());
			String usedTripId = entries[tripIdIndex];
			String fromShapeDist = "";
			Coord fromShapeCoord = null;				
			// Prepare the replacing with shaped links
			if(createShapedNetwork){
				if(!this.alternativeStationToShapeAssignment){
					fromShapeDist = entries[shapeDistIndex];
					if(fromShapeDist.isEmpty()){
						fromShapeDist = "0.0";
					}
				}else{
					//WARNING: Couldn't find shape_dist_traveled header in stop_times.txt. The converter will try to identify the Stations by its coordinates.
					this.alternativeStationToShapeAssignment = true;
					fromShapeCoord = network.getNodes().get(fromNodeId).getCoord();
				}
			}
			if(it.hasNext()){
				entries = it.next();
				Id toNodeId = new IdImpl(entries[stopIdIndex]);
				double arrivalTime = Time.parseTime(entries[arrivalTimeIndex].trim());
				String toShapeDist = "";
				Coord toShapeCoord = null;
				// Prepare the replacing with shaped links
				if(createShapedNetwork){
					if(!this.alternativeStationToShapeAssignment){
						toShapeDist = entries[shapeDistIndex];
					}else{
						toShapeCoord = network.getNodes().get(toNodeId).getCoord();
					}						
				}
				if(fromNodes.containsKey(fromNodeId)){
					if(!(fromNodes.get(fromNodeId)).contains(toNodeId)){
						addLink = true;
					}
				}else{
					addLink = true;
					fromNodes.put(fromNodeId, new ArrayList<Id>());
				}
				if(!(fromNodes.containsKey(toNodeId))){
					fromNodes.put(toNodeId, new ArrayList<Id>());
				}
				// No 0-Length Links
				if(fromNodeId.equals(toNodeId)){
					addLink = false;
				}
				// If the toNode belongs to a different trip, there should not be a link!
				if(!usedTripId.equals(entries[tripIdIndex])){
					addLink = false;				
				}
				// for each stop should exist one dummy node with a link in to it (dL2_) and a link back to the road (dL1_).

				Link link = null;
				if(addLink){
					double length = CoordUtils.calcDistance(nodes.get(fromNodeId).getCoord(), nodes.get(toNodeId).getCoord());
					Double freespeed = freespeedKmPerHour/3.6;
					if((length > 0.0) && (!Double.isInfinite(departureTime)) && (!Double.isInfinite(arrivalTime))){
						freespeed = length/(arrivalTime - departureTime);
						if(freespeed.isInfinite()){
							freespeed = freespeedKmPerHour/3.6;
							System.out.println("The Difference between ArrivalTime at one Stop " + ts.getFacilities().get(toNodeId).getName() + "(" + toNodeId + ") and DepartureTime at the previous Stop " + ts.getFacilities().get(fromNodeId).getName() + "(" + fromNodeId + ") is 0. That leads to high freespeeds.");
						}
					}
					link = network.getFactory().createLink(new IdImpl(i++), nodes.get(fromNodeId), nodes.get(toNodeId));
					link.setLength(length);
					link.setFreespeed(freespeed);
					link.setCapacity(capacity);
					link.setNumberOfLanes(numLanes);
					network.addLink(link);
					// Change the linktype to pt
					Set<String> modes = new HashSet<String>();
					modes.add(TransportMode.pt);
					link.setAllowedModes(modes);
					fromNodes.get(fromNodeId).add(toNodeId);
					// Prepare the replacing with shaped links
					if(createShapedNetwork){
						if(!alternativeStationToShapeAssignment){
							String[] shapeInfos = new String[3];
							shapeInfos[0] = usedTripId;
							shapeInfos[1] = fromShapeDist;
							shapeInfos[2] = toShapeDist;
							this.shapedLinkIds.put(link.getId(), shapeInfos);
						}else{
							String[] shapeInfos = new String[5];
							shapeInfos[0] = usedTripId;
							shapeInfos[1] = String.valueOf(fromShapeCoord.getX());
							shapeInfos[2] = String.valueOf(fromShapeCoord.getY());
							shapeInfos[3] = String.valueOf(toShapeCoord.getX());
							shapeInfos[4] = String.valueOf(toShapeCoord.getY());
							this.shapedLinkIdsCoordinate.put(link.getId(), shapeInfos);
						}							
					}						
				}									
			}		
		}
	}

	private void createDummyNodeAndLinks(Id toNodeId, double capacity,
			int numLanes, Network network, Map<Id, ? extends Node> nodes) {
		Id dummyId = new IdImpl("dN_" + toNodeId);
		if(!(network.getNodes().containsKey(dummyId))){
			NodeImpl n = new NodeImpl(dummyId);
			n.setCoord(new CoordImpl(nodes.get(toNodeId).getCoord().getX()+1,nodes.get(toNodeId).getCoord().getY()+1));
			network.addNode(n);
			double length = 50;
			Link link = network.getFactory().createLink(new IdImpl("dL1_" + toNodeId), n, nodes.get(toNodeId));
			link.setLength(length);
			link.setFreespeed(1000);
			link.setCapacity(capacity);
			link.setNumberOfLanes(numLanes);
			network.addLink(link);
			// Change the linktype to pt
			Set<String> modes = new HashSet<String>();
			modes.add(TransportMode.pt);
			link.setAllowedModes(modes);
			// Backwards
			Link link2 = network.getFactory().createLink(new IdImpl("dL2_" + toNodeId), nodes.get(toNodeId), n);
			link2.setLength(length);
			link2.setFreespeed(1000);
			link2.setCapacity(capacity);
			link2.setNumberOfLanes(numLanes);
			network.addLink(link2);
			link2.setAllowedModes(modes);
		}
	}


	private void createTransitVehicles(){
		for(String s: vehicleIdsAndTypes.keySet()){
			// TYPE
			VehicleType vt;
			switch(vehicleIdsAndTypes.get(s)) {
			// These are route types as specified by gtfs
			case 0: vt = scenario.getVehicles().getVehicleTypes().get(new IdImpl("type-tram")); break;
			case 1: vt = scenario.getVehicles().getVehicleTypes().get(new IdImpl("type-subway")); break;
			case 2: vt = scenario.getVehicles().getVehicleTypes().get(new IdImpl("type-rail")); break;
			case 3: vt = scenario.getVehicles().getVehicleTypes().get(new IdImpl("type-bus")); break;
			case 4: vt = scenario.getVehicles().getVehicleTypes().get(new IdImpl("type-ferry")); break;
			case 5: vt = scenario.getVehicles().getVehicleTypes().get(new IdImpl("type-cablecar")); break;
			case 6: vt = scenario.getVehicles().getVehicleTypes().get(new IdImpl("type-gondola")); break;
			case 7: vt = scenario.getVehicles().getVehicleTypes().get(new IdImpl("type-funicular")); break;
			default: vt = scenario.getVehicles().getVehicleTypes().get(new IdImpl("type-unidentified"));
			}

			// Vehicle
			Vehicle v = scenario.getVehicles().getFactory().createVehicle(new IdImpl(s), vt);
			scenario.getVehicles().addVehicle(v);
		}
	}

	private void createTransitVehicleTypes() {
		{
			VehicleCapacity vc = scenario.getVehicles().getFactory().createVehicleCapacity();
			vc.setSeats(50);
			vc.setStandingRoom(50);
			VehicleType vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("type-tram"));
			vt.setCapacity(vc);
			vt.setLength(5);

			scenario.getVehicles().addVehicleType(vt);
		}
		{
			VehicleCapacity vc = scenario.getVehicles().getFactory().createVehicleCapacity();
			vc.setSeats(50);
			vc.setStandingRoom(50);
			VehicleType vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("type-subway"));
			vt.setCapacity(vc);
			vt.setLength(5);

			scenario.getVehicles().addVehicleType(vt);
		}
		{
			VehicleCapacity vc = scenario.getVehicles().getFactory().createVehicleCapacity();
			vc.setSeats(50);
			vc.setStandingRoom(50);
			VehicleType vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("type-rail"));
			vt.setCapacity(vc);
			vt.setLength(5);
		}
		{
			VehicleCapacity vc = scenario.getVehicles().getFactory().createVehicleCapacity();
			vc.setSeats(50);
			vc.setStandingRoom(50);
			VehicleType vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("type-bus"));
			vt.setCapacity(vc);
			vt.setLength(5);

			scenario.getVehicles().addVehicleType(vt);
		}
		{
			VehicleCapacity vc = scenario.getVehicles().getFactory().createVehicleCapacity();
			vc.setSeats(50);
			vc.setStandingRoom(50);
			VehicleType vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("type-ferry"));
			vt.setCapacity(vc);
			vt.setLength(5);

			scenario.getVehicles().addVehicleType(vt);
		}
		{
			VehicleCapacity vc = scenario.getVehicles().getFactory().createVehicleCapacity();
			vc.setSeats(50);
			vc.setStandingRoom(50);
			VehicleType vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("type-cablecar"));
			vt.setCapacity(vc);
			vt.setLength(5);

			scenario.getVehicles().addVehicleType(vt);
		}
		{
			VehicleCapacity vc = scenario.getVehicles().getFactory().createVehicleCapacity();
			vc.setSeats(50);
			vc.setStandingRoom(50);
			VehicleType vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("type-gondola"));
			vt.setCapacity(vc);
			vt.setLength(5);

			scenario.getVehicles().addVehicleType(vt);
		}
		{
			VehicleCapacity vc = scenario.getVehicles().getFactory().createVehicleCapacity();
			vc.setSeats(50);
			vc.setStandingRoom(50);
			VehicleType vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("type-funicular"));
			vt.setCapacity(vc);
			vt.setLength(5);

			scenario.getVehicles().addVehicleType(vt);
		}
		{
			VehicleCapacity vc = scenario.getVehicles().getFactory().createVehicleCapacity();
			vc.setSeats(50);
			vc.setStandingRoom(50);
			VehicleType vt = scenario.getVehicles().getFactory().createVehicleType(new IdImpl("type-unidentified"));
			vt.setCapacity(vc);
			vt.setLength(5);

			scenario.getVehicles().addVehicleType(vt);
		}
	}

	private int getWeekday(long date) {
		int year = (int)(date/10000);
		int month = (int)((date - year*10000)/100);
		int day = (int)((date-month*100-year*10000));
		Calendar cal = new GregorianCalendar(year,month-1,day);
		int weekday = cal.get(Calendar.DAY_OF_WEEK)-1;
		if(weekday < 1){
			return 7;
		}else{
			return weekday;
		}		
	}




	// OPTIONAL SHAPE STUFF

	private void convertShapes(GtfsSource shapesSource){
		int shapeIdIndex = shapesSource.getContentIndex("shape_id");
		int shapeLatIndex = shapesSource.getContentIndex("shape_pt_lat");
		int shapeLonIndex = shapesSource.getContentIndex("shape_pt_lon");
		int shapeDistIndex = shapesSource.getContentIndex("shape_dist_traveled");
		if(shapeDistIndex<0){
			this.alternativeStationToShapeAssignment = true;
			System.out.println("Couldn't find the shape_dist_traveled field in shapes.txt. Now it uses the alternative station to shape assingment.");
		}
		String[] firstEntry = shapesSource.getContent().get(0);
		String oldShapeId = firstEntry[shapeIdIndex];
		List<String[]> shapes = new ArrayList<String[]>();
		for(String[] entries: shapesSource.getContent()) {
			String shapeId = entries[shapeIdIndex];
			String shapeLat = entries[shapeLatIndex];
			String shapeLon = entries[shapeLonIndex];
			String shapeDist;
			if(alternativeStationToShapeAssignment){
				shapeDist = "Alternative Station To Shape Assignment Is Used";
			}else{
				shapeDist = entries[shapeDistIndex];
			}
			if(oldShapeId.equals(shapeId)){
				String[] params = new String[3];
				params[0] = shapeDist;
				params[1] = shapeLon;
				params[2] = shapeLat;
				shapes.add(params);
			}else{
				this.shapes.put(new IdImpl(oldShapeId), shapes);
				oldShapeId = shapeId;
				shapes = new ArrayList<String[]>();
				String[] params = new String[3];
				params[0] = shapeDist;
				params[1] = shapeLon;
				params[2] = shapeLat;
				shapes.add(params);
			}
		}
		this.shapes.put(new IdImpl(oldShapeId), shapes);
	}

	private Map<Id,List<Coord>> assignShapesToLinks() {
		Map<Id,List<Coord>> result = new HashMap<Id, List<Coord>>();
		Map<Id,String[]> linkIdInfos = null;
		if(this.alternativeStationToShapeAssignment){
			linkIdInfos = this.shapedLinkIdsCoordinate;			
		}else{
			linkIdInfos = this.shapedLinkIds;
		}
		for(Id linkId: linkIdInfos.keySet()){
			String[] params = linkIdInfos.get(linkId);
			if(params[2].isEmpty()){
				System.out.println("There might be a problem with shape_dist_traveled field of the trip " + params[0]);
			}
			String tripId = params[0];
			List<Coord> coord = new ArrayList<Coord>();
			if(!this.alternativeStationToShapeAssignment){
				Double shapeDistStart = Double.parseDouble(params[1].trim());
				Double shapeDistEnd = Double.parseDouble(params[2].trim());
				String shapeId = this.shapeIdToTripIdAssignments.get(tripId);
				List<String[]> shapes = this.shapes.get(new IdImpl(shapeId));		
				for(String[] shapeCoord: shapes){
					double dist = Double.parseDouble(shapeCoord[0].trim());
					if((shapeDistStart <= dist) && (shapeDistEnd >= dist)){
						coord.add(transform.transform(new CoordImpl(Double.parseDouble(shapeCoord[1]), Double.parseDouble(shapeCoord[2]))));
					}
				}
			}else{
				Coord fromCoord = new CoordImpl(params[1], params[2]);
				Coord toCoord = new CoordImpl(params[3], params[4]);
				String shapeId = this.shapeIdToTripIdAssignments.get(tripId);
				List<String[]> shapes = this.shapes.get(new IdImpl(shapeId));
				boolean add = false;
				for(String[] shapeCoord: shapes){
					Coord c = transform.transform(new CoordImpl(Double.parseDouble(shapeCoord[1]), Double.parseDouble(shapeCoord[2])));
					if(CoordUtils.calcDistance(c, fromCoord) <= this.toleranceInM){
						add = true;						 
					}else if(CoordUtils.calcDistance(c, toCoord) <= this.toleranceInM){
						add = false;
						coord.add(c);
						break;
					}
					if(add){
						coord.add(c);						
					}
				}
			}
			if(!coord.isEmpty()){
				result.put(linkId, coord);
			}else{
				result.put(linkId, coord);
				System.out.println("Couldn't find any shapes for Link " + linkId + ". This Link will not be shaped.");
			}			
		}
		return result;
	}

}