package playground.wrashid.bsc.vbmh.vmParking;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.SumScoringFunction.BasicScoring;

/**
 * Gets the parking related score (which is saved in money so far) out of the agents vm_scorekeeper and calculates 
 * a score out of if. 
 * 
 * 
 *
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */


public class ParkScoring implements BasicScoring  {
	double score =0;
	Plan plan = null;
	//double betaMoney=-0.1; //!! Kommt auch in ParkControl vor >> In config auslagern?
	VMScoreKeeper scorekeeper = null;
	public ParkScoring(Plan plan) {
		super();
		this.plan = plan;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		if(this.plan.getPerson().getCustomAttributes().get("VMScoreKeeper")!=null){
			scorekeeper = (VMScoreKeeper) this.plan.getPerson().getCustomAttributes().get("VMScoreKeeper");
			this.score=scorekeeper.getScore();
			//System.out.println("Score Keeper geladen");
			//System.out.println("Park Scoring "+Double.toString(this.score));
		}
		
	}

	@Override
	public double getScore() {
		// TODO Auto-generated method stub
		return this.score;
	}

	//@Override
	/*public void reset() {
		// TODO Auto-generated method stub
		
	}*/

}