/*
 * $Id: EdgeRef.java 3639 2008-05-23 11:24:55Z swampwallaby $
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
import java.awt.Rectangle;

/**
 * A reference to an Edge made up of two hex grid references.
 *
 */
public class EdgeRef extends HexRef {
  
  protected int column2, row2;
  protected Point end1, end2;

  public EdgeRef() {
    this(0, 0, 0, 0);
  }
  
  public EdgeRef(HexRef h1, HexRef h2, TerrainHexGrid g) {
    this(h1.getColumn(), h1.getRow(), h2.getColumn(), h2.getRow());
    myGrid = g;
  }
  
  public EdgeRef(int c1, int r1, int c2, int r2) {
    this(c1, r1, c2, r2, null);
  }

  public EdgeRef(int c1, int r1, int c2, int r2, TerrainHexGrid g) {
    column = c1;
    row = r1;
    column2 = c2;
    row2 = r2;
    myGrid = g;
  }
  
  public EdgeRef(Point p, TerrainHexGrid g) {
    myGrid = g;
    center = new Point(p);
    
    final Point p1 = myGrid.snapToHex(new Point(center.x+4, center.y+4));
    final Point p2 = myGrid.snapToHex(new Point(center.x-4, center.y-4));
    
    HexRef hex1 = new HexRef(myGrid.getGridPosition(p1));
    myGrid.rotateIfSideways(hex1);
    column = hex1.getColumn();
    row = hex1.getRow();
    
    HexRef hex2 = new HexRef(myGrid.getGridPosition(p2));
    myGrid.rotateIfSideways(hex2);
    column2 = hex2.getColumn();
    row2 = hex2.getRow();
    
    recalculate();
  }
  
  public EdgeRef(EdgeRef e, TerrainHexGrid g) {
    this(e.column, e.row, e.column2, e.row2, g);
  }
  
  public void invalidate() {
    super.invalidate();
    end1 = null;
    end2 = null;
  }
  
  public boolean isAdjacent(HexRef hex) {
    final int hc = hex.getColumn();
    final int hr = hex.getRow();
    return (hc == getColumn() && hr == getRow()) || (hc == getColumn2() && hr == getRow2());
  }
  
  public int getColumn2() {
    return column2;
  }
  
  public int getRow2() {
    return row2;
  }
  
  public void setCenter(Point p) {
    if (center == null) {
      center = p;
    }
    else {
      center.setLocation(p);
    }
  }
  
  public boolean borders(HexRef h) {
    return (h.getColumn() == column && h.getRow() == row) ||
            (h.getColumn() == column2 && h.getRow() == row2);
  }
  
  public EdgeRef reversed() {
    return new EdgeRef(column2, row2, column, row, myGrid);
  }

  public Point getCenter() {
    if (center == null) {
      recalculate();
    }
    return center;
  }
  
  public Point getEnd1() {
    if (end1 == null) {
      recalculate();
    }
    return end1;
  }

  public Point getEnd2() {
    if (end2 == null) {
      recalculate();
    }
    return end2;
  }
  
  public Rectangle getBounds() {
    if (end1 == null) {
      recalculate();
    }
    int x = (end1.x < end2.x) ? end1.x : end2.x;
    int y = (end1.y < end2.y) ? end1.y : end2.y;
    x -= 5;
    y -= 5;
    final int width = Math.abs(end1.x - end2.x) + 10;
    final int height = Math.abs(end1.y - end2.y) + 10;
    
    return new Rectangle(x, y, width, height);
  }
  
  /**
   * Recalculate Center and endpoints of the Edge.
   */
  protected void recalculate() {

    if (center == null) {
      center = getEdgeCenter();
    }
    
    if (myGrid.isSideways()) {
      end1 = myGrid.snapToHexVertex(new Point(center.x+5, center.y+2));
      end2 = myGrid.snapToHexVertex(new Point(center.x-5, center.y-2));
    }
    else {
      end1 = myGrid.snapToHexVertex(new Point(center.x+2, center.y+5));
      end2 = myGrid.snapToHexVertex(new Point(center.x-2, center.y-5));
    }
    
    return;
  } 
  
  protected Point getEdgeCenter() {
    final Point c1 = myGrid.getHexCenter(column, row);
    final Point c2 = myGrid.getHexCenter(column2, row2);
    final Point p = new Point((c1.x+c2.x)/2, (c1.y+c2.y)/2);
    return myGrid.snapToHexSide(p);
  }
  
  public boolean equals(Object o) {
    if (o instanceof EdgeRef) {
      final EdgeRef target = (EdgeRef) o;
      return (target.column == column && target.row == row && target.column2 == column2 && target.row2 == row2);
    }
    return false;
  }
  
  public int hashCode() {
    return 29 * (column + column2) + row + row2;
  }
}
