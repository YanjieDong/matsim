/* *********************************************************************** *
 * project: org.matsim.*
 * DoublyWeightedSocialNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.framework;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author thibautd
 */
public class DoublyWeightedSocialNetwork {
	private final DoublyWeightedFriends[] alters;
	private final double lowestAllowedFirstWeight;
	private final double lowestAllowedSecondWeight;
	private int initialSize;

	public DoublyWeightedSocialNetwork(
			final int initialSize,
			final double lowestWeight,
			final int populationSize ) {
		this.initialSize  = initialSize;
		this.lowestAllowedFirstWeight = lowestWeight;
		this.lowestAllowedSecondWeight = lowestWeight;
		this.alters = new DoublyWeightedFriends[ populationSize ];
		for ( int i = 0; i < populationSize; i++ ) {
			this.alters[ i ] = new DoublyWeightedFriends( initialSize );
		}
	}

	public DoublyWeightedSocialNetwork(
			final double lowestWeight,
			final int populationSize ) {
		this( 20 , lowestWeight , populationSize );
	}

	public void clear() {
		for ( int i = 0; i < alters.length; i++ ) {
			this.alters[ i ] = new DoublyWeightedFriends( initialSize );
		}
	}

	public void addBidirectionalTie(
			final int ego,
			final int alter,
			final double weight1,
			final double weight2 ) {
		if ( weight1 < lowestAllowedFirstWeight ) return;
		if ( weight2 < lowestAllowedSecondWeight ) return;
		alters[ ego ].add( alter , weight1 , weight2 );
		alters[ alter ].add( ego , weight1 , weight2 );
	}

	public void addMonodirectionalTie(
			final int ego,
			final int alter,
			final double weight1,
			final double weight2 ) {
		if ( weight1 < lowestAllowedFirstWeight ) return;
		if ( weight2 < lowestAllowedSecondWeight ) return;
		alters[ ego ].add( alter , weight1 , weight2 );
	}


	public Set<Integer> getAltersOverWeights(
			final int ego,
			final double weight1,
			final double weight2 ) {
		if ( weight1 < lowestAllowedFirstWeight ) throw new IllegalArgumentException( "first weight "+weight1+" is lower than lowest stored weight "+lowestAllowedFirstWeight );
		if ( weight2 < lowestAllowedSecondWeight ) throw new IllegalArgumentException( "second weight "+weight2+" is lower than lowest stored weight "+lowestAllowedSecondWeight );
		return alters[ ego ].getAltersOverWeights( weight1 , weight2 );
	}

	// for tests
	/*package*/ int getSize( final int ego ) {
		return alters[ ego ].size;
	}

	// point quad-tree
	// No check is done that is is balanced!
	// should be ok, as agents are got in random order
	private static final class DoublyWeightedFriends {
		private int[] friends;

		// use float and short for memory saving
		private float[] weights1;
		private float[] weights2;

		private short[] childSE;
		private short[] childSW;
		private short[] childNE;
		private short[] childNW;

		// cannot exceed the maximum value of the "children" arrays:
		// set to short, even if it might have only limited impact on
		// memory
		private short size = 0;

		public DoublyWeightedFriends(final int initialSize) {
			this.friends = new int[ initialSize ];

			this.weights1 = new float[ initialSize ];
			this.weights2 = new float[ initialSize ];

			Arrays.fill( weights1 , Float.POSITIVE_INFINITY );
			Arrays.fill( weights2 , Float.POSITIVE_INFINITY );

			this.childSE = new short[ initialSize ];
			this.childSW = new short[ initialSize ];
			this.childNE = new short[ initialSize ];
			this.childNW = new short[ initialSize ];

			Arrays.fill( childSE , (short) -1 );
			Arrays.fill( childSW , (short) -1 );
			Arrays.fill( childNE , (short) -1 );
			Arrays.fill( childNW , (short) -1 );
		}

		public synchronized void add(
				final int friend,
				final double firstWeight,
				final double secondWeight ) {
			add( friend , (float) firstWeight , (float) secondWeight );
		}

		public synchronized void add(
				final int friend,
				final float firstWeight,
				final float secondWeight ) {
			if ( size == 0 ) {
				// first element is the head: special case...
				friends[ 0 ] = friend;

				weights1[ 0 ] = firstWeight;
				weights2[ 0 ] = secondWeight;
			}
			else {
				final int parent = searchParentLeaf( 0, firstWeight, secondWeight );

				if ( size == friends.length ) expand();

				final short[] quadrant = getQuadrant( parent , firstWeight, secondWeight );
				friends[ size ] = friend;

				weights1[ size ] = firstWeight;
				weights2[ size ] = secondWeight;

				quadrant[ parent ] = size;
			}
			size++;
		}

		private void expand() {
			final int newLength = 2 * friends.length;
			friends = Arrays.copyOf( friends , newLength );

			weights1 = Arrays.copyOf( weights1 , newLength );
			weights2 = Arrays.copyOf( weights2 , newLength );

			Arrays.fill( weights1 , size , newLength , Float.POSITIVE_INFINITY );
			Arrays.fill( weights2 , size , newLength , Float.POSITIVE_INFINITY );

			childSE = Arrays.copyOf( childSE , newLength );
			childSW = Arrays.copyOf( childSW , newLength );
			childNE = Arrays.copyOf( childNE , newLength );
			childNW = Arrays.copyOf( childNW , newLength );

			Arrays.fill( childSE , size , newLength , (short) -1 );
			Arrays.fill( childSW , size , newLength , (short) -1 );
			Arrays.fill( childNE , size , newLength , (short) -1 );
			Arrays.fill( childNW , size , newLength , (short) -1 );
		}

		private int searchParentLeaf(
				final int head,
				final float firstWeight,
				final float secondWeight ) {
			short[] quadrant = getQuadrant( head, firstWeight, secondWeight );

			return quadrant[ head ] == -1 ? head : searchParentLeaf( quadrant[head], firstWeight, secondWeight );
		}

		private short[] getQuadrant(
				final int head,
				final float firstWeight,
				final float secondWeight ) {
			if ( firstWeight > weights1[ head ] ) {
				return secondWeight > weights2[ head ] ? childNE : childSE;
			}
			return secondWeight > weights2[ head ] ? childNW : childSW;
		}

		public Set<Integer> getAltersOverWeights(
				final double firstWeight,
				final double secondWeight) {
			final Set<Integer> alters = new LinkedHashSet< >();

			addGreaterPoints( 0, alters, firstWeight, secondWeight );

			return alters;
		}

		private void addGreaterPoints(
				final int head,
				final Set<Integer> alters,
				final double firstWeight,
				final double secondWeight ) {
			if ( head == -1 ) return; // we fell of the tree!

			if ( weights1[ head ] > firstWeight && weights2[ head ] > secondWeight ) {
				alters.add( friends[ head ] );
				addGreaterPoints( childSW[ head ] , alters , firstWeight , secondWeight );
			}
			if ( weights1[ head ] > firstWeight ) {
				addGreaterPoints( childNW[ head ] , alters , firstWeight , secondWeight );
			}
			if ( weights2[ head ] > secondWeight ) {
				addGreaterPoints( childSE[ head ] , alters , firstWeight , secondWeight );
			}
			// always look to the NW
			addGreaterPoints( childNE[ head ] , alters , firstWeight , secondWeight );
		}
	}
}

