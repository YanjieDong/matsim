/* *********************************************************************** *
 * project: org.matsim.*
 * CASimTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor.casim.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.gregor.casim.events.CASimAgentConstructEvent;
import playground.gregor.casim.monitoring.CALinkMultiLaneMonitor;
import playground.gregor.casim.monitoring.Monitor;
import playground.gregor.casim.simulation.physics.AbstractCANetwork;
import playground.gregor.casim.simulation.physics.CAEvent;
import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;
import playground.gregor.casim.simulation.physics.CAMoveableEntity;
import playground.gregor.casim.simulation.physics.CAMultiLaneDensityEstimatorSPH;
import playground.gregor.casim.simulation.physics.CAMultiLaneDensityEstimatorSPHFactory;
import playground.gregor.casim.simulation.physics.CAMultiLaneLink;
import playground.gregor.casim.simulation.physics.CAMultiLaneNetworkFactory;
import playground.gregor.casim.simulation.physics.CANetwork;
import playground.gregor.casim.simulation.physics.CANetworkFactory;
import playground.gregor.casim.simulation.physics.CASimpleDynamicAgent;
import playground.gregor.casim.simulation.physics.CASingleLaneDensityEstimatorSPA;
import playground.gregor.casim.simulation.physics.CASingleLaneDensityEstimatorSPAFactory;
import playground.gregor.casim.simulation.physics.CASingleLaneDensityEstimatorSPAII;
import playground.gregor.casim.simulation.physics.CASingleLaneDensityEstimatorSPH;
import playground.gregor.casim.simulation.physics.CASingleLaneDensityEstimatorSPHFactory;
import playground.gregor.casim.simulation.physics.CASingleLaneDensityEstimatorSPHII;
import playground.gregor.casim.simulation.physics.CASingleLaneNetworkFactory;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimDensityDrawer;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

public class Long1DChannelBiMultiLane {

	public final static boolean USE_MULTI_LANE_MODEL = true;
	private static BufferedWriter bw;
	private static boolean USE_SPH;

	public static final class Setting {
		private int pR;
		private int pL;
		private double bexR;
		private double bexL;

		public Setting(double bl, double bCor, double br, int pL, int pR,
				double bexR, double bexL) {
			this.bL = bl;
			this.bCor = bCor;
			this.bR = br;
			this.pL = pL;
			this.pR = pR;
			this.bexR = bexR;
			this.bexL = bexL;

		}

		public double bL;
		public double bCor;
		public double bR;

		@Override
		public String toString() {

			return "bl: " + bL + " cor:" + bCor + " br:" + bR + " bexL:" + bexL
					+ " bexR:" + bexR;
		}
	}

	public static void main(String[] args) throws IOException {
		List<Setting> settings = new ArrayList<>();
		// settings.add(new Setting(1.8, 3.6, 1.8));
		// settings.add(new Setting(2, 3.6, 2));
		// settings.add(new Setting(.5, 3.6, .5, 56, 74));
		// settings.add(new Setting(.75, 3.6, .75, 62, 65));
		// settings.add(new Setting(.9, 3.6, .9, 110, 102));
		// settings.add(new Setting(1.2, 3.6, 1.2, 115, 106));
		// settings.add(new Setting(1.6, 3.6, 1.6, 140, 166));
		// settings.add(new Setting(2., 3.6, 2., 143, 166));
		// settings.add(new Setting(2.5, 3.6, 2.5, 141, 163));
		// settings.add(new Setting(2., 3.6, 2., 500, 500));
		// for (int i = 0; i < 500; i++) {
		// double r = 0, l = 0, c = 0;
		// while (l < 1 || l > 5) {
		// l = 2.5 + MatsimRandom.getRandom().nextGaussian();
		// }
		// while (r < 1 || r > 5) {
		// r = 2.5 + MatsimRandom.getRandom().nextGaussian();
		// }
		// while (c < 1 || c > 5) {
		// c = 2.5 + MatsimRandom.getRandom().nextGaussian();
		// }
		// settings.add(new Setting(l, c, r, 200, 200));
		// }

		for (double l = .4; l <= 3.; l += .1) {
			for (double r = l; r <= l; r += .61) {
				for (double cor = 3.6; cor <= 3.6; cor += .6) {
					for (double bexL = l; bexL <= l; bexL += .61) {
						for (double bexR = r; bexR <= r; bexR += .1) {

							settings.add(new Setting(l, cor, r, 400, 400, l, r));
							// if (l < bexR) {
							// continue;
							// }
							// // settings.add(new Setting(l, cor, r, 1600, 0,
							// bexL,
							// bexR));
							// settings.add(new Setting(l, cor, r, 0, 400, bexL,
							// bexR));
						}
					}
				}
			}

		}

		// settings.add(new Setting(3.6, 3.6, 3.6, 400, 0, 1.8, 7.2));
		// settings.add(new Setting(3.6, 3.6, 3.6, 400, 400, 1.8, 7.2));

		// for (double w = 1; w < 3.61; w += 0.1) {
		// settings.add(new Setting(w, 3.6, w, 500, 500));
		// }

		AbstractCANetwork.EMIT_VIS_EVENTS = true;
		USE_SPH = true;
		for (int R = 12; R <= 12; R++) {
			CASingleLaneDensityEstimatorSPH.H = R;
			CAMultiLaneDensityEstimatorSPH.H = R;
			CASingleLaneDensityEstimatorSPA.RANGE = R;
			CASingleLaneDensityEstimatorSPHII.H = R;
			CASingleLaneDensityEstimatorSPAII.RANGE = R;
			try {
				bw = new BufferedWriter(new FileWriter(new File(
						"/Users/laemmel/devel/bipedca/ant/trash" + R)));
				// bw = new BufferedWriter(
				// new FileWriter(new File(
				// "/home/laemmel/scenarios/bipedca/rho_new_uni_spl_"
				// + R)));
			} catch (IOException e) {
				e.printStackTrace();
			}

			for (Setting s : settings) {
				System.out.println(s);
				Config c = ConfigUtils.createConfig();
				c.global().setCoordinateSystem("EPSG:3395");
				Scenario sc = ScenarioUtils.createScenario(c);

				// VIS only
				Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
				Sim2DScenario sc2d = Sim2DScenarioUtils
						.createSim2dScenario(conf2d);
				sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, sc2d);

				Network net = sc.getNetwork();
				((NetworkImpl) net).setCapacityPeriod(1);
				NetworkFactory fac = net.getFactory();

				int l = 20;
				int res = 100;
				Node n0 = fac.createNode(Id.createNodeId("0"), new CoordImpl(
						20 - res, 0));
				Node n1 = fac.createNode(Id.createNodeId("1"), new CoordImpl(
						20, 0));
				Node n2 = fac.createNode(Id.createNodeId("2"), new CoordImpl(
						24, 0));
				Node n2ex = fac.createNode(Id.createNodeId("2ex"),
						new CoordImpl(24, 20));
				Node n3 = fac.createNode(Id.createNodeId("3"), new CoordImpl(
						24 + l, 0));
				Node n3ex = fac.createNode(Id.createNodeId("3ex"),
						new CoordImpl(24 + l, -20));
				Node n4 = fac.createNode(Id.createNodeId("4"), new CoordImpl(
						24 + 4 + l, 0));
				Node n5 = fac.createNode(Id.createNodeId("5"), new CoordImpl(24
						+ 4 + res + l, 0));
				net.addNode(n5);
				net.addNode(n4);
				net.addNode(n3ex);
				net.addNode(n3);
				net.addNode(n2ex);
				net.addNode(n2);
				net.addNode(n1);
				net.addNode(n0);

				Link l0 = fac.createLink(Id.createLinkId("0"), n0, n1);
				Link l0rev = fac.createLink(Id.createLinkId("0rev"), n1, n0);
				Link l1 = fac.createLink(Id.createLinkId("1"), n1, n2);
				Link l1rev = fac.createLink(Id.createLinkId("1rev"), n2, n1);
				Link l2 = fac.createLink(Id.createLinkId("2"), n2, n3);
				Link l2rev = fac.createLink(Id.createLinkId("2rev"), n3, n2);
				Link l2ex = fac.createLink(Id.createLinkId("2ex"), n2, n2ex);
				Link l3 = fac.createLink(Id.createLinkId("3"), n3, n4);
				Link l3ex = fac.createLink(Id.createLinkId("3ex"), n3, n3ex);
				Link l3rev = fac.createLink(Id.createLinkId("3rev"), n4, n3);
				Link l4 = fac.createLink(Id.createLinkId("4"), n4, n5);
				Link l4rev = fac.createLink(Id.createLinkId("4rev"), n5, n4);

				l0.setLength(res);
				l1.setLength(4);

				l0rev.setLength(res);
				l1rev.setLength(4);

				l2rev.setLength(l);
				l2.setLength(l);

				l3rev.setLength(4);
				l3.setLength(4);
				l4rev.setLength(res);
				l4.setLength(res);
				l3ex.setLength(20);
				l2ex.setLength(20);

				net.addLink(l4);
				net.addLink(l4rev);
				net.addLink(l1);
				net.addLink(l0);
				net.addLink(l2);
				net.addLink(l2rev);
				net.addLink(l1rev);
				net.addLink(l0rev);
				net.addLink(l3rev);
				net.addLink(l3);
				net.addLink(l3ex);
				net.addLink(l2ex);

				double bl = s.bL;
				double bc = s.bCor;
				double br = s.bR;
				double bexR = s.bexR;
				double bexL = s.bexL;

				l0.setCapacity(2 * bc);
				l0rev.setCapacity(2 * bc);
				l1.setCapacity(bl);
				l1rev.setCapacity(bl);
				l2.setCapacity(bc);
				l2rev.setCapacity(bc);
				l3.setCapacity(br);
				l3rev.setCapacity(br);
				l4.setCapacity(2 * bc);
				l4rev.setCapacity(2 * bc);
				l3ex.setCapacity(bexR);
				l2ex.setCapacity(bexL);

				List<Link> linksLR = new ArrayList<Link>();
				linksLR.add(l0);
				linksLR.add(l1);
				linksLR.add(l2);
				linksLR.add(l3ex);
				List<Link> linksRL = new ArrayList<Link>();
				linksRL.add(l4rev);
				linksRL.add(l3rev);
				linksRL.add(l2rev);
				linksRL.add(l2ex);

				for (int i = 0; i < 1; i++) {
					for (double th = 1; th <= 1; th += 0.15) {
						runIt(net, linksLR, linksRL, sc, th, s);
					}
					// for (double th = .26; th <= .4; th += 0.01) {
					// runIt(net, linksLR, sc, th);
					// }
					// for (double th = .45; th < .5; th += 0.05) {
					// runIt(net, linksLR, sc, th);
					// }
					// for (double th = .5; th <= 1.; th += 0.1) {
					// runIt(net, linksLR, sc, th);
					// }
				}
			}
			bw.close();
		}
	}

	private static void runIt(Network net, List<Link> linksLR,
			List<Link> linksRL, Scenario sc, double th, Setting s) {
		// visualization stuff
		EventsManager em = new EventsManagerImpl();
		// // // if (iter == 9)

		if (AbstractCANetwork.EMIT_VIS_EVENTS) {
			EventBasedVisDebuggerEngine vis = new EventBasedVisDebuggerEngine(
					sc);
			em.addHandler(vis);
			QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
			em.addHandler(qDbg);
			vis.addAdditionalDrawer(new InfoBox(vis, sc));
			vis.addAdditionalDrawer(qDbg);
		}
		CANetworkFactory fac;
		if (USE_MULTI_LANE_MODEL) {
			fac = new CAMultiLaneNetworkFactory();
			fac.setDensityEstimatorFactory(new CAMultiLaneDensityEstimatorSPHFactory());

		} else {
			fac = new CASingleLaneNetworkFactory();
			if (USE_SPH) {
				fac.setDensityEstimatorFactory(new CASingleLaneDensityEstimatorSPHFactory());
			} else {
				fac.setDensityEstimatorFactory(new CASingleLaneDensityEstimatorSPAFactory());
			}

		}

		CANetwork caNet = fac.createCANetwork(net, em, null);

		int agents = 0;
		{
			CAMultiLaneLink caLink = (CAMultiLaneLink) caNet.getCALink(linksLR
					.get(0).getId());
			int lanes = caLink.getNrLanes();
			CAMoveableEntity[] particles = caLink.getParticles(0);
			System.out.println("part left:" + particles.length);
			double oldR = 1;
			int tenth = particles.length / 10;
			int cnt = 0;
			for (int i = particles.length - 1; i >= 0; i--) {
				for (int lane = 0; lane < lanes; lane++) {
					double r = MatsimRandom.getRandom().nextDouble();
					if (r > th) {
						continue;
					}
					cnt++;
					if (cnt > s.pL) {
						break;
					}
					// agents++;
					CAMoveableEntity a = new CASimpleDynamicAgent(linksLR, 1,
							Id.create("g" + agents++,
									CASimpleDynamicAgent.class), caLink);
					a.materialize(i, 1, lane);
					caLink.getParticles(lane)[i] = a;
					CASimAgentConstructEvent ee = new CASimAgentConstructEvent(
							0, a);
					em.processEvent(ee);

					CAEvent e = new CAEvent(
							1 / (AbstractCANetwork.V_HAT * AbstractCANetwork.RHO_HAT),
							a, caLink, CAEventType.TTA);
					caNet.pushEvent(e);
					caNet.registerAgent(a);
				}
			}
		}

		{
			CAMultiLaneLink caLink = (CAMultiLaneLink) caNet.getCALink(linksRL
					.get(0).getId());
			int lanes = caLink.getNrLanes();
			CAMoveableEntity[] particles = caLink.getParticles(0);
			System.out.println("part left:" + particles.length);
			double oldR = 1;
			int tenth = particles.length / 10;
			int cnt = 0;
			for (int i = 0; i < particles.length - 1; i++) {
				// for (int i = 0; i < 0; i++) {
				for (int lane = 0; lane < lanes; lane++) {
					double r = MatsimRandom.getRandom().nextDouble();
					if (r > th) {
						continue;
					}
					cnt++;
					if (cnt > s.pR) {
						break;
					}
					// agents++;
					CAMoveableEntity a = new CASimpleDynamicAgent(linksRL, 1,
							Id.create("b" + agents++,
									CASimpleDynamicAgent.class), caLink);
					a.materialize(i, -1, lane);
					caLink.getParticles(lane)[i] = a;
					CASimAgentConstructEvent ee = new CASimAgentConstructEvent(
							0, a);
					em.processEvent(ee);

					CAEvent e = new CAEvent(
							1 / (AbstractCANetwork.V_HAT * AbstractCANetwork.RHO_HAT),
							a, caLink, CAEventType.TTA);
					caNet.pushEvent(e);
					caNet.registerAgent(a);
				}
			}
		}

		Monitor monitor = new CALinkMultiLaneMonitor(
				(CAMultiLaneLink) caNet.getCALink(Id.createLinkId("2")), 2.);

		// CALinkMonitorExact monitor = new CALinkMonitorExactIIUni(
		// caNet.getCALink(Id.createLinkId("0")), 10.,
		// ((CAMultiLaneLink) caNet.getCALink(Id.createLinkId("0")))
		// .getParticles(0), caNet.getCALink(Id.createLinkId("0"))
		// .getLink().getCapacity());
		caNet.addMonitor(monitor);
		monitor.init();
		caNet.run(3600);
		try {
			monitor.report(bw);
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
