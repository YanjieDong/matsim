/**
 * 
 */
package scenarios.braess.createInput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.core.network.NetworkWriter;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LaneDefinitionsFactory20;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;

/**
 * Class to create network and lanes for the breass scenario.
 * 
 * You may choose between simulating inflow capacity or not. Whereby simulating
 * inflow capacity means that one additional short link is added in front of
 * link 2-3, 2-4 and 4-5. (Inflow capacity at 3-4 and 3-5 is not necessary (i.e.
 * flow capacity can not be exceeded at this links) because they only have one
 * incoming link with the same flow capacity.)
 * 
 * @author tthunig
 * 
 */
public final class TtCreateBraessNetworkAndLanes {
	
	private static final Logger log = Logger
			.getLogger(TtCreateBraessNetworkAndLanes.class);
	
	public enum LaneType{
		NONE, TRIVIAL, REALISTIC
	}
	
	private Scenario scenario;
	
	private boolean simulateInflowCap = false;
	private boolean middleLinkExists = true;
	private LaneType laneType = LaneType.NONE; 
	private boolean btuRun = false;
	
	// capacity at the links that all agents have to use
	private long capFirstLast = 3600; // [veh/h]
	// capacity at all other links
	private long capMain = 1800; // [veh/h]
	// link length for the inflow links
	private double inflowLinkLength = 7.5 * 1; // [m]
	// link length for all other links
	private long linkLength = 10000; // [m]
	// travel time for the middle link
	private double linkTTMid = 1;
	// travel time for the middle route links
	private double linkTTSmall = 1*60; // [s]
	// travel time for the two remaining outer route links (choose at least 3*LINK_TT_SMALL!)
	private double linkTTBig = 10*60; // [s]
	// travel time for inflow links and links that all agents have to use
	private double minimalLinkTT = 1; // [s]

	public TtCreateBraessNetworkAndLanes(Scenario scenario) {		
		this.scenario = scenario;
	}

	/**
	 * Creates the network for the Breass scenario.
	 */
	public void createNetworkAndLanes(){
		
		initNetworkParams();
		
		createNetwork();
		
		if (laneType.equals(LaneType.TRIVIAL))
			createTrivialLanes();
		if (laneType.equals(LaneType.REALISTIC))
			createRealisticLanes();
		if (laneType.equals(LaneType.NONE))
			log.info("No lanes are used");
	}

	private void initNetworkParams() {
		if (btuRun){
			capFirstLast = 9000; 
			capMain = 9000; 
			inflowLinkLength = 7.5 * 1;
			linkLength = 200;
			linkTTMid = 1;
			linkTTSmall = 10;
			linkTTBig = 20;
			minimalLinkTT = 1;
		}else{
			capFirstLast = 3600; 
			capMain = 1800; 
			inflowLinkLength = 7.5 * 1;
			linkLength = 10000;
			linkTTMid = 1*60;
			linkTTSmall = 1*60;
			linkTTBig = 10*60;
			minimalLinkTT = 1;
		}
	}

	private void createNetwork() {
		Network net = this.scenario.getNetwork();
		NetworkFactory fac = net.getFactory();

		// create nodes
		net.addNode(fac.createNode(Id.createNodeId(0),
				scenario.createCoord(-200, 200)));
		net.addNode(fac.createNode(Id.createNodeId(1),
				scenario.createCoord(0, 200)));
		net.addNode(fac.createNode(Id.createNodeId(2),
				scenario.createCoord(200, 200)));
		net.addNode(fac.createNode(Id.createNodeId(3),
				scenario.createCoord(400, 400)));
		net.addNode(fac.createNode(Id.createNodeId(4),
				scenario.createCoord(400, 0)));
		net.addNode(fac.createNode(Id.createNodeId(5),
				scenario.createCoord(600, 200)));
		net.addNode(fac.createNode(Id.createNodeId(6),
				scenario.createCoord(800, 200)));
		
		if (simulateInflowCap){
			net.addNode(fac.createNode(Id.createNodeId(23),
					scenario.createCoord(250, 250)));
			net.addNode(fac.createNode(Id.createNodeId(24),
					scenario.createCoord(250, 150)));
			net.addNode(fac.createNode(Id.createNodeId(45), 
					scenario.createCoord(450, 50)));
		}
		
		// create links
		Link l = fac.createLink(Id.createLinkId("0_1"),
				net.getNodes().get(Id.createNodeId(0)),
				net.getNodes().get(Id.createNodeId(1)));
		setLinkAttributes(l, capFirstLast, linkLength, minimalLinkTT);
		net.addLink(l);
		
		l = fac.createLink(Id.createLinkId("1_2"),
				net.getNodes().get(Id.createNodeId(1)),
				net.getNodes().get(Id.createNodeId(2)));
		setLinkAttributes(l, capFirstLast, linkLength, minimalLinkTT);
		net.addLink(l);
		
		if (simulateInflowCap){
			l = fac.createLink(Id.createLinkId("2_23"),
					net.getNodes().get(Id.createNodeId(2)),
					net.getNodes().get(Id.createNodeId(23)));
			setLinkAttributes(l, capMain, inflowLinkLength, minimalLinkTT);
			net.addLink(l);
			
			l = fac.createLink(Id.createLinkId("23_3"),
					net.getNodes().get(Id.createNodeId(23)),
					net.getNodes().get(Id.createNodeId(3)));
			setLinkAttributes(l, capMain, linkLength, linkTTSmall - minimalLinkTT);
			net.addLink(l);
		} else {
			l = fac.createLink(Id.createLinkId("2_3"),
					net.getNodes().get(Id.createNodeId(2)),
					net.getNodes().get(Id.createNodeId(3)));
			setLinkAttributes(l, capMain, linkLength, linkTTSmall);
			net.addLink(l);
		}
		
		if (simulateInflowCap){
			l = fac.createLink(Id.createLinkId("2_24"),
					net.getNodes().get(Id.createNodeId(2)),
					net.getNodes().get(Id.createNodeId(24)));
			setLinkAttributes(l, capMain, inflowLinkLength, minimalLinkTT);
			net.addLink(l);
			
			l = fac.createLink(Id.createLinkId("24_4"),
					net.getNodes().get(Id.createNodeId(24)),
					net.getNodes().get(Id.createNodeId(4)));
			setLinkAttributes(l, capMain, linkLength, linkTTBig - minimalLinkTT);
			net.addLink(l);
		} else {
			l = fac.createLink(Id.createLinkId("2_4"),
					net.getNodes().get(Id.createNodeId(2)),
					net.getNodes().get(Id.createNodeId(4)));
			setLinkAttributes(l, capMain, linkLength, linkTTBig);
			net.addLink(l);
		}
		
		if (this.middleLinkExists){
			l = fac.createLink(Id.createLinkId("3_4"),
				net.getNodes().get(Id.createNodeId(3)),
				net.getNodes().get(Id.createNodeId(4)));
			setLinkAttributes(l, capMain, linkLength, linkTTMid);
			net.addLink(l);
		}
	
		l = fac.createLink(Id.createLinkId("3_5"),
				net.getNodes().get(Id.createNodeId(3)),
				net.getNodes().get(Id.createNodeId(5)));
		setLinkAttributes(l, capMain, linkLength, linkTTBig);
		net.addLink(l);
		
		if (simulateInflowCap){
			l = fac.createLink(Id.createLinkId("4_45"),
					net.getNodes().get(Id.createNodeId(4)),
					net.getNodes().get(Id.createNodeId(45)));
			setLinkAttributes(l, capMain, inflowLinkLength, minimalLinkTT);
			net.addLink(l);
			
			l = fac.createLink(Id.createLinkId("45_5"),
					net.getNodes().get(Id.createNodeId(45)),
					net.getNodes().get(Id.createNodeId(5)));
			setLinkAttributes(l, capMain, linkLength, linkTTSmall - minimalLinkTT);
			net.addLink(l);
		}
		else{
			l = fac.createLink(Id.createLinkId("4_5"),
					net.getNodes().get(Id.createNodeId(4)),
					net.getNodes().get(Id.createNodeId(5)));
			setLinkAttributes(l, capMain, linkLength, linkTTSmall);
			net.addLink(l);
		}
		
		l = fac.createLink(Id.createLinkId("5_6"),
				net.getNodes().get(Id.createNodeId(5)),
				net.getNodes().get(Id.createNodeId(6)));
		setLinkAttributes(l, capFirstLast, linkLength, minimalLinkTT);
		net.addLink(l);
	}

	private static void setLinkAttributes(Link link, double capacity,
			double length, double travelTime) {
		
		link.setCapacity(capacity);
		link.setLength(length);
		// agents have to reach the end of the link before the time step ends to
		// be able to travel forward in the next time step (matsim time step logic)
		link.setFreespeed(link.getLength() / (travelTime - 0.1));
	}

	/**
	 * creates a trivial lane for every link
	 */
	private void createTrivialLanes() {
		
		LaneDefinitions20 laneDef20 = this.scenario.getLanes();
		LaneDefinitionsFactory20 fac = laneDef20.getFactory();
		
		for (Link link: scenario.getNetwork().getLinks().values()){
			// create a trivial lane for every link that has outgoing links
			if (link.getToNode().getOutLinks() != null && !link.getToNode().getOutLinks().isEmpty()) {
				LanesToLinkAssignment20 linkAssignment = fac.createLanesToLinkAssignment(link.getId());

				// create to link list
				List<Id<Link>> toLinkList = new ArrayList<>();
				for (Id<Link> toLink : link.getToNode().getOutLinks().keySet()) {
					toLinkList.add(toLink);
				}
				LanesUtils.createAndAddLane20(linkAssignment, fac,
						Id.create(link.getId() + ".ol", Lane.class), link.getCapacity(),
						link.getLength(), 0, 1, toLinkList, null);

				laneDef20.addLanesToLinkAssignment(linkAssignment);
			}
		}
	}

	/**
	 * creates a lane for every turning direction, i.e. for every signal at a link
	 */
	private void createRealisticLanes() {
		
		LaneDefinitions20 laneDef20 = this.scenario.getLanes();
		LaneDefinitionsFactory20 fac = laneDef20.getFactory();

		// create link assignment of link 1_2
		LanesToLinkAssignment20 linkAssignment = fac
				.createLanesToLinkAssignment(Id.createLinkId("1_2"));

		LanesUtils.createAndAddLane20(linkAssignment, fac,
				Id.create("1_2.ol", Lane.class), capFirstLast,
				linkLength, 0, 1, null, 
				Arrays.asList(Id.create("1_2.1", Lane.class),
				Id.create("1_2.2", Lane.class)));
		
		if (simulateInflowCap) {
			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("1_2.1", Lane.class), capFirstLast,
					linkLength / 2, -1, 1, 
					Arrays.asList(Id.createLinkId("2_23")),	null);
			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("1_2.2", Lane.class), capFirstLast,
					linkLength / 2, 1, 1,  
					Arrays.asList(Id.createLinkId("2_24")), null);
		} else {
			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("1_2.1", Lane.class), capFirstLast,
					linkLength / 2, -1, 1, 
					Arrays.asList(Id.createLinkId("2_3")), null);
			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("1_2.2", Lane.class), capFirstLast,
					linkLength / 2, 1, 1,  
					Arrays.asList(Id.createLinkId("2_4")), null);
		}	
		
		laneDef20.addLanesToLinkAssignment(linkAssignment);
		
		// no lanes on 2_3 (or 23_3) are needed if 3_4 doesn't exist
		if (this.middleLinkExists) {
			// create link assignment of link 2_3 (or 23_3 if inflow capacity is
			// simulated)
			if (simulateInflowCap) {
				linkAssignment = fac.createLanesToLinkAssignment(Id
						.createLinkId("23_3"));

				LanesUtils.createAndAddLane20(linkAssignment, fac,
						Id.create("23_3.ol", Lane.class), capMain,
						linkLength, 0,	1, null,
						Arrays.asList(Id.create("23_3.1", Lane.class),
								Id.create("23_3.2", Lane.class)));

				LanesUtils.createAndAddLane20(linkAssignment, fac,
						Id.create("23_3.1", Lane.class), capMain,
						linkLength / 2, 0, 1,
						Arrays.asList(Id.createLinkId("3_5")), null);

				LanesUtils.createAndAddLane20(linkAssignment, fac,
						Id.create("23_3.2", Lane.class), capMain,
						linkLength / 2, 1, 1,
						Arrays.asList(Id.createLinkId("3_4")), null);

				laneDef20.addLanesToLinkAssignment(linkAssignment);
			} else {
				linkAssignment = fac.createLanesToLinkAssignment(Id
						.createLinkId("2_3"));

				LanesUtils.createAndAddLane20(linkAssignment, fac,
						Id.create("2_3.ol", Lane.class), capMain,
						linkLength, 0,	1, null,
						Arrays.asList(Id.create("2_3.1", Lane.class),
								Id.create("2_3.2", Lane.class)));

				LanesUtils.createAndAddLane20(linkAssignment, fac,
						Id.create("2_3.1", Lane.class), capMain,
						linkLength / 2, 0, 1,
						Arrays.asList(Id.createLinkId("3_5")), null);

				LanesUtils.createAndAddLane20(linkAssignment, fac,
						Id.create("2_3.2", Lane.class), capMain,
						linkLength / 2, 1, 1,
						Arrays.asList(Id.createLinkId("3_4")), null);

				laneDef20.addLanesToLinkAssignment(linkAssignment);
			}
		}
	}

	/**
	 * Sets the flag for simulating inflow capacity.
	 * 
	 * If true, links 2_3, 2_4 and 4_5 are divided into 2 links: a small one at
	 * the beginning that simulates an inflow capacity at the link and a longer
	 * one that preserves the other properties of the link
	 * 
	 * @param simulateInflowCap
	 */
	public void setSimulateInflowCap(boolean simulateInflowCap) {
		this.simulateInflowCap = simulateInflowCap;
	}

	public void setMiddleLinkExists(boolean middleLinkExists) {
		this.middleLinkExists = middleLinkExists;
	}

	public void setLaneType(LaneType laneType) {
		this.laneType = laneType;
	}

	public void setUseBTUProperties(boolean btuRun) {
		this.btuRun = btuRun;
	}

	public void writeNetworkAndLanes(String directory) {
		new NetworkWriter(scenario.getNetwork()).write(directory + "network.xml");
		if (!laneType.equals(LaneType.NONE)) new LaneDefinitionsWriter20(scenario.getLanes()).write(directory + "lanes.xml");
	}

}
