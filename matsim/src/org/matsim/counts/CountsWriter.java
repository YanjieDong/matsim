/* *********************************************************************** *
 * project: org.matsim.*
 * CountsWriter.java
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

package org.matsim.counts;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.matsim.core.api.internal.MatsimFileWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.MatsimXmlWriter;

public class CountsWriter extends MatsimXmlWriter implements MatsimFileWriter {

	private CountsWriterHandler handler = null;
	private final Counts counts;

	public CountsWriter(final Counts counts) {
		this.counts = counts;

		// use the newest writer-version by default
		this.handler = new CountsWriterHandlerImplV1();
	}

	public final void writeFile(final String filename) {
		try {
			openFile(filename);

			// write custom header
			writeXmlHead();

			this.handler.startCounts(this.counts, this.writer);
			this.handler.writeSeparator(this.writer);
			
			List<Count> countsTemp = new Vector<Count>();
			countsTemp.addAll(this.counts.getCounts().values());
			Collections.sort(countsTemp, new CountComparator());
			
			//counts iterator
			Iterator<Count> c_it = countsTemp.iterator();
			while (c_it.hasNext()) {
				Count c = c_it.next();
				
				List<Volume> volumesTemp = new Vector<Volume>();
				volumesTemp.addAll(c.getVolumes().values());
				Collections.sort(volumesTemp, new VolumeComparator());
				
				this.handler.startCount(c,this.writer);

				// volume iterator
				Iterator<Volume> vol_it = volumesTemp.iterator();
				while (vol_it.hasNext()) {
					Volume v = vol_it.next();
					this.handler.startVolume(v, this.writer);
					this.handler.endVolume(this.writer);
				}
				this.handler.endCount(this.writer);
				this.handler.writeSeparator(this.writer);
				this.writer.flush();
			}
			this.handler.endCounts(this.writer);
			close();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	@Override
	public final String toString() {
		return super.toString();
	}
	
	static class VolumeComparator implements Comparator<Volume>, Serializable {
		private static final long serialVersionUID = 1L;

		public int compare(final Volume v1, final Volume v2) {
			return Double.compare(v1.getHour(), v2.getHour());
		}
	}
	
	static class CountComparator implements Comparator<Count>, Serializable {
		private static final long serialVersionUID = 1L;

		public int compare(final Count c1, final Count c2) {
			
			int i1 = Integer.parseInt(c1.getCsId().substring(c1.getCsId().length()-3));
			int i2 = Integer.parseInt(c2.getCsId().substring(c2.getCsId().length()-3));
			if (i1 < i2) return -1;
			else if (i1 > i2) return 1;
			else return 0;
		}
	}
}
