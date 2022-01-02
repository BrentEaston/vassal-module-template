/*
 * $Id: TerrainEdge.java 8004 2011-11-08 17:46:36Z morvael $
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
 * Concrete implementation of Edge Terrain
 */
public class TerrainEdge  {

  protected static final String TYPE = "e";
  protected EdgeRef reference;
  protected EdgeTerrain terrain;
  protected TerrainHexGrid grid;

  public TerrainEdge (int c1, int r1, int c2, int r2, EdgeTerrain t, Point c, TerrainHexGrid g) {
    reference = new EdgeRef(c1, r1, c2, r2, grid);  
    reference.setCenter(c);  
    grid = g;
    terrain = t;
  }
  
 public TerrainEdge (EdgeRef ref, EdgeTerrain t) {
   reference = ref;
   terrain = t;
 }
 
 public TerrainEdge (Point centerPos, TerrainHexGrid g, EdgeTerrain t) { 
    reference = new EdgeRef(centerPos, g);
    terrain = t;  
    grid = g;
  }
  
  public TerrainEdge(String code, TerrainHexGrid grid) {
    decode(code, grid);
  }
  
  public boolean equals(Object o) {
    if (o instanceof TerrainEdge) {
      return ((TerrainEdge) o).getLocation().equals(getLocation());
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
  
  public EdgeRef getLocation() {
    return reference;
  }

  public EdgeRef getReverseLocation() {
    return reference.reversed();
  }
  
  public TerrainEdge reversed() {
    return new TerrainEdge(reference.reversed(), terrain);
  }
  
  public Point getCenter() {
    return reference.getCenter();
  }
  
  public void setTerrain(EdgeTerrain t) {
    terrain = t; 
  }
  
  public EdgeTerrain getTerrain() {
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
    final SequenceEncoder.Decoder sd = new SequenceEncoder.Decoder(code, ',');
    sd.nextToken();
    reference = new EdgeRef(sd.nextInt(0), sd.nextInt(0), sd.nextInt(0), sd.nextInt(0), grid);
    terrain = (EdgeTerrain) TerrainDefinitions.getInstance().getEdgeTerrainDefinitions().getTerrain(sd.nextToken(TerrainMap.NO_TERRAIN)); 
  }
}
