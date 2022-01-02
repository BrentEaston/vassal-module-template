/*
 * $Id: TerrainLine.java 945 2006-07-09 12:42:41 +0000 (Sun, 09 Jul 2006) swampwallaby $
 *
 * Copyright (c) 2000-2008 by Brent Easton
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License (LGPL) as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, copies are available
 * at http://www.opensource.org.
 */
package terrain;

import java.awt.Point;

import VASSAL.tools.SequenceEncoder;

/**
 * Concrete implementation of Line Terrain
 */
public class TerrainLine {

	protected static final String TYPE = "l";
	protected LineRef reference;
	protected LineTerrain terrain;
	protected TerrainHexGrid grid;

	public TerrainLine(int c1, int r1, int c2, int r2, LineTerrain t, Point c,
			TerrainHexGrid g) {
		reference = new LineRef(c1, r1, c2, r2, grid);
		reference.setCenter(c);
		grid = g;
		terrain = t;
	}

	public TerrainLine(LineRef ref, LineTerrain t) {
		reference = ref;
		terrain = t;
	}

	public TerrainLine(Point centerPos, TerrainHexGrid g, LineTerrain t) {
		reference = new LineRef(centerPos, g);
		terrain = t;
		grid = g;
	}

	public TerrainLine(String code, TerrainHexGrid grid) {
		decode(code, grid);
	}

	public boolean equals(Object o) {
		if (o instanceof TerrainLine) {
			return ((TerrainLine) o).getLocation().equals(getLocation());
		}
		return false;
	}

	public void recalculate() {
		reference.invalidate();
	}

	public Point getEnd1() {
		return reference.getEnd1();
	}

	public Point getEnd2() {
		return reference.getEnd2();
	}

	public LineRef getLocation() {
		return reference;
	}

	public LineRef getReverseLocation() {
		return reference.reversed();
	}

	public TerrainLine reversed() {
		return new TerrainLine(reference.reversed(), terrain);
	}

	public Point getCenter() {
		return reference.getCenter();
	}

	public void setTerrain(LineTerrain t) {
		terrain = t;
	}

	public LineTerrain getTerrain() {
		return terrain;
	}

	public String encode() {
		final SequenceEncoder se = new SequenceEncoder(TYPE, ',');
		se.append(reference.getColumn());
		se.append(reference.getRow());
		se.append(reference.getColumn2());
		se.append(reference.getRow2());
		se.append(terrain == null ? "" : terrain.getTerrainName());
		return se.getValue();
	}

	public void decode(String code, TerrainHexGrid grid) {
		final SequenceEncoder.Decoder sd = new SequenceEncoder.Decoder(code,
				',');
		sd.nextToken();
		reference = new LineRef(sd.nextInt(0), sd.nextInt(0), sd.nextInt(0), sd
				.nextInt(0), grid);
		terrain = (LineTerrain) TerrainDefinitions.getInstance()
				.getLineTerrainDefinitions().getTerrain(
						sd.nextToken(TerrainMap.NO_TERRAIN));
	}

}
