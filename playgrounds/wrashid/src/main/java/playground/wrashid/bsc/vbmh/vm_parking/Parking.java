package playground.wrashid.bsc.vbmh.vm_parking;

import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

public class Parking {
	public @XmlAttribute int id;
	public long capacity_ev;
	public long capacity_nev;
	public int charging_rate, parking_pricem, charging_pricem;
	public String facility_id;
	public String type;
	public boolean ev_exklusive;
	public double coordinate_x, coordinate_y;
	public LinkedList <Parkingspot> spots;
///*
	@XmlTransient
	public Coord get_coordinate(){
		Coord coordinate = new CoordImpl(coordinate_x, coordinate_y);
		return coordinate;
	}//*/
	
	public void set_coordinate(Coord coordinate){
		this.coordinate_x=coordinate.getX();
		this.coordinate_y=coordinate.getY();
		coordinate = null;
	}
	
	public void create_spots(){
		spots= new LinkedList<Parkingspot>();
		for (int i=0; i<capacity_ev; i++){
			Parkingspot parkingspot = new Parkingspot();
			spots.add(parkingspot);
			spots.getLast().charge=true;
			spots.getLast().charging_pricem=this.charging_pricem;
			spots.getLast().charging_rate=this.charging_rate;
			spots.getLast().parking_pricem=this.parking_pricem;
			spots.getLast().occupied=false;
			spots.getLast().parking=this;
		}
		for (int i=0; i<capacity_nev; i++){
			Parkingspot parkingspot = new Parkingspot();
			spots.add(parkingspot);
			spots.getLast().charge=false;
			spots.getLast().parking_pricem=this.parking_pricem;
			spots.getLast().occupied=false;
			spots.getLast().parking=this;
		}
	}
	
	public void clear_spots(){
		spots=null;
	}
	
	public Parkingspot check_for_free_spot(){
		for(Parkingspot spot : spots){
			//System.out.println("checke spot");
			if (spot.occupied == false){
				return spot;
			}
		}
		return null;
	}

	
	
	
}