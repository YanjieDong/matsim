package playground.wrashid.bsc.vbmh.vmParking;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Keeps a list of all parking in the network.
 * 
 * !! Quadtree und evntl umkreis Suche hier her?
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */



@XmlRootElement
public class ParkingMap {
	private static List<Parking> parkings = new LinkedList<Parking>();

	@XmlElement(name = "Parking")
	public List<Parking> getParkings() {
		return parkings;
	}

	public void setParking(List<Parking> parkings) {

		ParkingMap.parkings = parkings;
	}
	
	public void addParking(Parking parking){
		ParkingMap.parkings.add(parking);
	}
	
	public void createSpots(){
		for (Parking parking : parkings){
			parking.createSpots();
		}
	}
	public void clearSpots(){
		for (Parking parking : parkings){
			parking.clearSpots();
		}
	}
	
}