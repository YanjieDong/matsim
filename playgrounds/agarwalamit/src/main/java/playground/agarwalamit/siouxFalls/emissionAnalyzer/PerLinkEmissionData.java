/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.agarwalamit.siouxFalls.emissionAnalyzer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.WarmPollutant;
import playground.vsp.emissions.utils.EmissionUtils;

/**
 * @author amit
 *
 */
public class PerLinkEmissionData {
	private Logger logger = Logger.getLogger(PerLinkEmissionData.class);
	private static String outputDir = "/Users/aagarwal/Desktop/ils/agarwal/siouxFalls/output/run1/";
	private static String networkFile =outputDir+ "/output_network.xml.gz";
	private static String configFile = outputDir+"/output_config.xml";
	private static String emissionEventFile = outputDir+"/ITERS/it.100/100.emission.events.xml";
	EmissionUtils emissionUtils;

	 Network network;

	public static void main(String[] args) throws IOException {
		PerLinkEmissionData data = new PerLinkEmissionData();
		data.run();
	}

	private void run() throws IOException {
		
		BufferedWriter writer1 = IOUtils.getBufferedWriter(outputDir+"/ITERS/it.100/100.timeLinkIdTotalEmissions.txt");
		BufferedWriter writer2 = IOUtils.getBufferedWriter(outputDir+"/ITERS/it.100/100.timeLinkIdWarmEmissions.txt");
		BufferedWriter writer3 = IOUtils.getBufferedWriter(outputDir+"/ITERS/it.100/100.timeLinkIdColdEmissions.txt");
		
		Scenario scenario = loadScenario(networkFile);
		this.network = scenario.getNetwork();
		EmissionLinkAnalyzer linkAnalyzer = new EmissionLinkAnalyzer(configFile, emissionEventFile);
		linkAnalyzer.init(null);
		linkAnalyzer.preProcessData();
		linkAnalyzer.postProcessData();
		
		emissionUtils = new EmissionUtils();
		EmissionUtilsExtended emissionUtilsExtended = new EmissionUtilsExtended();
		
		Map<Double, Map<Id, Map<WarmPollutant, Double>>> time2warmEmissionsTotal = linkAnalyzer.getLink2WarmEmissions();
		Map<Double, Map<Id, Map<ColdPollutant, Double>>> time2coldEmissionsTotal = linkAnalyzer.getLink2ColdEmissions();
		Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal = linkAnalyzer.getLink2TotalEmissions();
		Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotalFilled = setNonCalculatedEmissions(time2EmissionsTotal);
		
		Map<Double, Map<Id, SortedMap<String, Double>>> time2WarmEmissionsTotalFilled = emissionUtilsExtended.convertPerLinkWarmEmissions2String(network, time2warmEmissionsTotal);
		Map<Double, Map<Id, SortedMap<String, Double>>> time2ColdEmissionsTotalFilled = emissionUtilsExtended.convertPerLinkColdEmissions2String(network, time2coldEmissionsTotal);
		
		writer1.write("time"+"\t"+"linkId"+"\t"+"CO"+"\t"+"CO2_Total"+"\t"+"FC"+"\t"+"HC"+"\t"+"NMHC"+"\t"+"NO2"+"\t"+"NOX"+"\t"+"PM"+"\t"+"SO2"+"\n");
		writer2.write("time"+"\t"+"linkId"+"\t"+"CO"+"\t"+"CO2_Total"+"\t"+"FC"+"\t"+"HC"+"\t"+"NMHC"+"\t"+"NO2"+"\t"+"NOX"+"\t"+"PM"+"\t"+"SO2"+"\n");
		writer3.write("time"+"\t"+"linkId"+"\t"+"CO"+"\t"+"CO2_Total"+"\t"+"FC"+"\t"+"HC"+"\t"+"NMHC"+"\t"+"NO2"+"\t"+"NOX"+"\t"+"PM"+"\t"+"SO2"+"\n");
		for(double time : time2ColdEmissionsTotalFilled.keySet()){
			for(Link link : network.getLinks().values()){
				writer1.write(link.getId().toString()+"\t");
				writer2.write(/*time+"\t"+*/link.getId().toString()+"\t");
				writer3.write(/*time+"\t"+*/link.getId().toString()+"\t");

				for(String str : (time2ColdEmissionsTotalFilled.get(time)).get(link.getId()).keySet()){
					writer1.write(time2EmissionsTotalFilled.get(time).get(link.getId()).get(str)+"\t");
					writer2.write(time2WarmEmissionsTotalFilled.get(time).get(link.getId()).get(str)+"\t");
					writer3.write(time2ColdEmissionsTotalFilled.get(time).get(link.getId()).get(str)+"\t");
					
					double d = time2EmissionsTotalFilled.get(time).get(link.getId()).get(str) - time2ColdEmissionsTotalFilled.get(time).get(link.getId()).get(str)-time2WarmEmissionsTotalFilled.get(time).get(link.getId()).get(str);
					if(d<-1||d>1){
						System.out.println(d);
						logger.warn("Total Emissions per link per time interval is not sum of cold and warm emissions per link per time interval");
						logger.info("time = "+time+"link = "+link.getId()+"pollutant = "+str+"total emission = "+time2EmissionsTotalFilled.get(time).get(link.getId()).get(str)+"cold emission = "
								+ time2ColdEmissionsTotalFilled.get(time).get(link.getId()).get(str)+"warm emissions = "+time2WarmEmissionsTotalFilled.get(time).get(link.getId()).get(str));
					}
				} 
				writer1.write("\n");
				writer2.write("\n");
				writer3.write("\n");
			}
		} 
		writer1.close();
		writer2.close();
		writer3.close();
		logger.info("Finished Writing files.");
	}
	
	private Scenario loadScenario(String netFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}
	
	
	 private Map<Double, Map<Id, SortedMap<String, Double>>> setNonCalculatedEmissions(Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal) {
			Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotalFilled = new HashMap<Double, Map<Id, SortedMap<String, Double>>>();
			
			for(double endOfTimeInterval : time2EmissionsTotal.keySet()){
				Map<Id, SortedMap<String, Double>> emissionsTotalFilled = emissionUtils.setNonCalculatedEmissionsForNetwork(network, time2EmissionsTotal.get(endOfTimeInterval));
				time2EmissionsTotalFilled.put(endOfTimeInterval, emissionsTotalFilled);
			}
			return time2EmissionsTotalFilled;
		}
	public void writeLinkLocation2Emissions(
				Map<Id, SortedMap<String, Double>> map,
				Network network,
				String outFile){
			try{
				FileWriter fstream = new FileWriter(outFile);			
				BufferedWriter out = new BufferedWriter(fstream);
				out.append("linkId\txLink\tyLink\t");
				for (String pollutant : emissionUtils.getListOfPollutants()){
					out.append(pollutant + "[g]\t");
				}
				out.append("\n");

				for(Id linkId : map.keySet()){
					Link link = network.getLinks().get(linkId);
					Coord linkCoord = link.getCoord();
					Double xLink = linkCoord.getX();
					Double yLink = linkCoord.getY();

					out.append(linkId + "\t" + xLink + "\t" + yLink + "\t");

					Map<String, Double> emissionType2Value = map.get(linkId);
					for(String pollutant : emissionUtils.getListOfPollutants()){
						out.append(emissionType2Value.get(pollutant) + "\t");
					}
					out.append("\n");
				}
				//Close the output stream
				out.close();
				logger.info("Finished writing output to " + outFile);
			} catch (Exception e){
				throw new RuntimeException(e);
			}
		}
}