///* *********************************************************************** *
// * project: org.matsim.*
// * ParkAndRideModeStrategy.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2012 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//package playground.thibautd.parknride.herbiespecific;
//
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.replanning.PlanStrategyModule;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.replanning.PlanStrategy;
//import org.matsim.core.replanning.PlanStrategyImpl;
//import org.matsim.core.replanning.selectors.PlanSelector;
//import org.matsim.core.replanning.selectors.RandomPlanSelector;
//
//import playground.thibautd.router.controler.MultiLegRoutingControler;
//
///**
// * @author thibautd
// */
//public class ParkAndRideModeStrategy implements PlanStrategy {
//	private final PlanStrategy delegate;
//
//	public ParkAndRideModeStrategy(final Controler controler) {
//		delegate = new PlanStrategyImpl( new RandomPlanSelector() );
//		addStrategyModule( new ParkAndRideChooseModeForSubtourModule((MultiLegRoutingControler) controler) );
//	}
//
//	@Override
//	public void addStrategyModule(final PlanStrategyModule module) {
//		delegate.addStrategyModule(module);
//	}
//
//	@Override
//	public int getNumberOfStrategyModules() {
//		return delegate.getNumberOfStrategyModules();
//	}
//
//	@Override
//	public void run(final Person person) {
//		delegate.run(person);
//	}
//
//	@Override
//	public void init() {
//		delegate.init();
//	}
//
//	@Override
//	public void finish() {
//		delegate.finish();
//	}
//
//	@Override
//	public String toString() {
//		return delegate.toString();
//	}
//
//	@Override
//	public PlanSelector getPlanSelector() {
//		return delegate.getPlanSelector();
//	}
//}
//
