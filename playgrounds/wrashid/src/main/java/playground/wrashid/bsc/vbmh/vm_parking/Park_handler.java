package playground.wrashid.bsc.vbmh.vm_parking;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;





public class Park_handler implements ActivityEndEventHandler, ActivityStartEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {

	public Park_Control park_control = new Park_Control();
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		// TODO Auto-generated method stub
	//	if(event.getLegMode()=="car"){
			if(!event.getActType().equals("ParkO")&&!event.getActType().equals("ParkP")){
				park_control.park(event);
			}
	//	}
		
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// TODO Auto-generated method stub
		
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		// TODO Auto-generated method stub
		park_control.unpark(event);
	}

}