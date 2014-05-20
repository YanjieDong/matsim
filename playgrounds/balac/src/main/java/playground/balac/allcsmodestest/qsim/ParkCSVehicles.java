package playground.balac.allcsmodestest.qsim;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import playground.balac.freefloating.qsim.FreeFloatingStation;
import playground.balac.freefloating.qsim.FreeFloatingVehiclesLocation;
import playground.balac.onewaycarsharingredisgned.qsim.OneWayCarsharingRDStation;
import playground.balac.onewaycarsharingredisgned.qsim.OneWayCarsharingRDVehicleLocation;
import playground.balac.twowaycarsharingredisigned.qsim.TwoWayCSStation;
import playground.balac.twowaycarsharingredisigned.qsim.TwoWayCSVehicleLocation;

public class ParkCSVehicles implements AgentSource {
	private Population population;
	private AgentFactory agentFactory;
	private QSim qsim;
	private Map<String, VehicleType> modeVehicleTypes;
	private Collection<String> mainModes;
	private boolean insertVehicles = true;
	private FreeFloatingVehiclesLocation ffvehiclesLocationqt;
	private OneWayCarsharingRDVehicleLocation owvehiclesLocationqt;
	private TwoWayCSVehicleLocation twvehiclesLocationqt;
	public ParkCSVehicles(Population population, AgentFactory agentFactory, QSim qsim,
			FreeFloatingVehiclesLocation ffvehiclesLocationqt, OneWayCarsharingRDVehicleLocation owvehiclesLocationqt, TwoWayCSVehicleLocation twvehiclesLocationqt) {
		this.population = population;
		this.agentFactory = agentFactory;
		this.qsim = qsim;  
		this.modeVehicleTypes = new HashMap<String, VehicleType>();
		this.mainModes = qsim.getScenario().getConfig().qsim().getMainModes();
		this.ffvehiclesLocationqt = ffvehiclesLocationqt;
		this.owvehiclesLocationqt = owvehiclesLocationqt;
		this.twvehiclesLocationqt = twvehiclesLocationqt;
		for (String mode : mainModes) {
			modeVehicleTypes.put(mode, VehicleUtils.getDefaultVehicleType());
		}
	}
	
	@Override
	public void insertAgentsIntoMobsim() {
		// TODO Auto-generated method stub
		if (ffvehiclesLocationqt != null)
		for (FreeFloatingStation ffstation: ffvehiclesLocationqt.getQuadTree().values()) {
			
			for (String id:ffstation.getIDs()) {
				qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(new IdImpl("FF_"+(id)), modeVehicleTypes.get("freefloating")), ffstation.getLink().getId());

			}
			
		}
		if (owvehiclesLocationqt != null)
		for (OneWayCarsharingRDStation owstation: owvehiclesLocationqt.getQuadTree().values()) {
			
			for (String id : owstation.getIDs()) {
				qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(new IdImpl("OW_"+id), modeVehicleTypes.get("onewaycarsharing")), owstation.getLink().getId());

			}
			
		}
		
		if (twvehiclesLocationqt != null)
			for (TwoWayCSStation twstation: twvehiclesLocationqt.getQuadTree().values()) {
				
				for (String id : twstation.getIDs()) {
					qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(new IdImpl("TW_"+id), modeVehicleTypes.get("twowaycarsharing")), twstation.getLink().getId());

				}
				
			}
		
	}

}