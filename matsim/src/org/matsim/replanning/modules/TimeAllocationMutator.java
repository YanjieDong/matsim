/* *********************************************************************** *
 * project: org.matsim.*
 * TimeAllocationMutator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.replanning.modules;

import org.matsim.gbl.Gbl;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.plans.algorithms.PlanMutateTimeAllocation;

public class TimeAllocationMutator extends MultithreadedModuleA {

	private int mutationRange = 1800;// TODO [MR] should be a config-option

	public TimeAllocationMutator() {
		String range = null;
		try {
			range = Gbl.getConfig().getParam("TimeAllocationMutator","mutationRange");
		}
		catch (IllegalArgumentException e) {
			Gbl.noteMsg(this.getClass(),"TimeAllocationMutator()","No mutation range defined in the config file. Using 1800 sec.");
		}
		if (range != null) {
			this.mutationRange = Integer.parseInt(range);
			Gbl.noteMsg(this.getClass(),"TimeAllocationMutator()","mutation range = " + this.mutationRange + ".");
		}
	}

	public TimeAllocationMutator(final int muntation_range) {
		this.mutationRange = muntation_range;
	}

	@Override
	public PlanAlgorithmI getPlanAlgoInstance() {
		return new PlanMutateTimeAllocation(mutationRange);
	}

}
