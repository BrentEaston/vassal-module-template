/*
 * $Id: HexRef.java 3639 2008-05-23 11:24:55Z swampwallaby $
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

/**
 * Wrapper around a Point to represent a Hex Grid reference.
 *
 */
public class HexRef implements Comparable<HexRef> {

  protected int column;
  protected int row;
  protected Point center;
  protected TerrainHexGrid myGrid;
  
  public HexRef() {
    
  }
  
  public HexRef (int c, int r, Point p, TerrainHexGrid g) {
    column = c;
    row = r;    
    center = p;
    myGrid = g;
  }
  
  public HexRef(int c, int r, TerrainHexGrid g) {
    this(c, r, null, g);
  }
  
  public HexRef(int c, int r) {
    this(c, r, null);
  }  
   
  public HexRef(HexRef ref) {
    this(ref.getColumn(), ref.getRow(), ref.getGrid());
  }
  
  public HexRef(HexRef ref, Point c) {
    this(ref);
    center = c;
  }
  
  public TerrainHexGrid getGrid() {
    return myGrid;
  }
  
  public void setGrid(TerrainHexGrid g) {
    myGrid = g;
  }
  
  public int getColumn() {
    return column;
  }
  
  public int getRow() {
    return row;
  }
  
  public void setColumn(int c) {
    column = c;
  }
  
  public void setRow(int r) {
    row = r;
  }
  
  public void invalidate() {
    center = null;
  }
  
  protected Point getCenter() {
    if (center == null && myGrid != null) {
      center = myGrid.getHexCenter(column, row);
    }
    return center;
  }
  
  public void rotate() {
    final int x = column;
    column = row;
    row = x;
    if (center != null) {
      center.setLocation(center.y, center.x);
    }
  }

  public boolean equals(Object o) {
    if (o instanceof HexRef) {
      final HexRef h = (HexRef) o;
      return (h.getColumn() == getColumn() && h.getRow() == getRow());
    }
    return false;
  }
  
  public int hashCode() {
    return 29 * column + row;
  }

  public int compareTo(HexRef o) {
    if (o== null) {
      throw new NullPointerException();
    }
    else if (getColumn() < o.getColumn()) {
      return -1;
    }
    else if (getColumn() > o.getColumn()) {
      return 1;
    }
    else if (getRow() < o.getRow()) {
      return -1;
    }
    else if (getRow() > o.getRow()) {
      return 1;
    }
    return 0;
  }
}
