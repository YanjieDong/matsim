package playground.mzilske.ulm;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

public class ReadGraph {

	public static void main(String[] args) {
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S);
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();
		try {
			Graph graph = Graph.load(new File("/Users/michaelzilske/gtfs-ulm/Graph.obj"), Graph.LoadLevel.DEBUG);
			for (Vertex v : graph.getVertices()) {
				if (v instanceof IntersectionVertex) {
					// Can be an OSM node, but can also be a split OSM way to insert a transit stop.
					Node n = network.getFactory().createNode(new IdImpl(v.getIndex()), ct.transform(new CoordImpl(v.getX(), v.getY())));
					network.addNode(n);
				}
			}
			int i = 0;
			for (Vertex v : graph.getVertices()) {
				if (v instanceof IntersectionVertex) {
					for (Edge e : v.getOutgoing()) {
						if (e instanceof PlainStreetEdge) {
							Node fromNode = network.getNodes().get(new IdImpl(e.getFromVertex().getIndex()));
							Node toNode = network.getNodes().get(new IdImpl(e.getToVertex().getIndex()));
							Link l = network.getFactory().createLink(new IdImpl(e.getFromVertex().getIndex() + "_" + e.getToVertex().getIndex()+ "_" + i++), fromNode, toNode);
							network.addLink(l);
						} else if (e instanceof StreetTransitLink) {
							// Found a street transit link
						}
					}
				}
			}
			new NetworkWriter(scenario.getNetwork()).write("/Users/michaelzilske/gtfs-ulm/road-network.xml");
		} catch (IOException e) {
			throw new RuntimeException();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException();
		}

	}

}
