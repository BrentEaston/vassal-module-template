/*
 * $Id: TerrainHexGrid.java 5654 2009-05-22 12:52:20Z morvael $
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
import java.awt.geom.Area;

import VASSAL.build.module.map.boardPicker.Board;
import VASSAL.build.module.map.boardPicker.board.HexGrid;
import VASSAL.configure.AutoConfigurer;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Subclass of HexGrid supporting Terrain
 */
//FIXME: Merge into HexGrid
public class TerrainHexGrid extends HexGrid {
  
  protected TerrainHexGridEditor gridEditor;
  protected TerrainMap terrainMap = null;
  protected HashMap<HexRef, HexRef[]> adjacentHexes = new HashMap<HexRef, HexRef[]>();
  
  public TerrainHexGrid() {
    super();
  }
  
  public Board getBoard() {  
    return container == null ? null : container.getBoard();
  }
  
  public Area getSingleHex(int x, int y) {
    return getSingleHexShape(x, y, false);
  }
  
  public Area getSingleHex(Point p) {
    final Point snap = snapToHex(p);
    return getSingleHex(snap.x, snap.y);
  }
  
  public Area getSingleHex(TerrainHex t) {
    final Point p = getHexCenter(t.getColumn(), t.getRow());
    return getSingleHex(p.x, p.y);
  }
  
  /*
   * Return the terrain at the given map x,y point (in Global Map coordinates)
   */
  public String getTerrainName(Point p) {
    getTerrainMap();
    final HexRef hexPos = getHexPos(normalize(p));
    final TerrainHex hex = terrainMap.getHexTerrain(hexPos);
    if (hex != null) {
      return hex.getTerrain().getTerrainName();
    }
    else
      return "";
  }
  
  protected void getTerrainMap() {
    if (terrainMap == null) {
      terrainMap =  TerrainDefinitions.getInstance().getTerrainMap(this);
    }
    return;
  }
  
  protected Point normalize(Point p) {
    final Rectangle bounds = container.getBoard().bounds();
    return new Point(p.x - bounds.x, p.y - bounds.y);
  }
  
  /**
   * Check if property can be satisfied by this grid and if so,
   * return the value
   * 
   * @param propName Property Name
   * @param p GamePiece position
   * @return property value
   */
  public String getProperty(String propName, Point p) {
    // Does the Property name match an Attribute Terrain definition?
    String value = null;
    final AttributeTerrain at = (AttributeTerrain) TerrainDefinitions.getInstance().getAttributeTerrainDefinitions().getTerrain(propName);
    final Point boardPos = normalize(p);
    if (at != null) {
      getTerrainMap();
      final HexRef hexPos = getHexPos(boardPos);
      final AttrRef ref = new AttrRef(hexPos, propName);
      final TerrainAttribute attr = terrainMap.getAttributeTerrain(ref);
      value = attr == null ? at.getDefaultValue() : attr.getValue();
    }
    return value;
  }
  
  /*
   * Return the column,row co-ords for a hex at the given point
   */
  
  public HexRef getHexPos(Point p) {
    final Point snap = snapToHex(p);
    final HexRef pos = new HexRef(getGridPosition(snap), snap);
    rotateIfSideways(pos);
    return pos;
  }
  

  public void rotateIfSideways(HexRef p) {
    if (sideways) {
       p.rotate();
    }
  }

  /* 
   * getRawRow & getRowColumn extracted from HexGridNumbering where they
   * do not belong! Should have been in HexGrid in the first place.
   */
  public int getRawColumn(Point p) {
    p = new Point(p);
    rotateIfSideways(p);
    int x = p.x - getOrigin().x;
    x = (int) Math.floor(x / getHexWidth() + 0.5);
    return x;
  }
  
  protected int getRawRow(Point p) {
    p = new Point(p);
    rotateIfSideways(p);
    final Point origin = getOrigin();
    final double dx = getHexWidth();
    final double dy = getHexSize();
    final int nx = (int) Math.round((p.x - origin.x) / dx);
    int ny;
    if (nx % 2 == 0) {
      ny = (int) Math.round((p.y - origin.y) / dy);
    }
    else {
      ny = (int) Math.round((p.y - origin.y - dy / 2) / dy);
    }
    return ny;
  }
  
  protected int getMaxRows() {
		if (sideways) {
			// this case is checked and working
			return (int) Math.floor(getContainer().getSize().height
					/ getHexWidth() + 0.5);
		} else {
			// this case is NOT checked
			return (int) Math.floor(getContainer().getSize().width
					/ getHexWidth() + 0.5);
			// or return (int) Math.floor(getContainer().getSize().height /
			// getHexSize() + 0.5);
		}
	}

  protected int getMaxColumns() {
		if (sideways) {
			// this case is checked and working
			return (int) Math.floor(getContainer().getSize().width
					/ getHexSize() + 0.5);
		} else {
			// this case is NOT checked
			return (int) Math.floor(getContainer().getSize().height
					/ getHexSize() + 0.5);
			// or return (int) Math.floor(getContainer().getSize().width /
			// getHexWidth() + 0.5);
		}
	}

  
  /*
   * Return the center of the specified hex
   */
  public Point getHexCenter(int column, int row) {
    int x, y;
    
    if (sideways) {
      x = origin.y + (int) (dy * column) + ((row % 2 == 0) ? 0 : (int) (dy/2));
      y = origin.x + (int) (dx * row);
    }
    else {
      x = origin.x + (int) (dx * column);
      y = origin.y - (int) (dy * 0.5) + (int) (dy * row) + ((column % 2 == 0) ? (int) (dy/2) : (int) dy);
    }
    return new Point(x, y);
  }
  
  /*
   * Return the raw Grid Reference of the hex contining the given point
   */
  protected HexRef getGridPosition(Point p) {
    return new HexRef(getRawColumn(p), getRawRow(p), this);
  }
  
  /*
   * Override editGrid() to use the new Terrain GridEditor
   */
  @Override
  public void editGrid() {
    gridEditor = new TerrainHexGridEditor(this);
    gridEditor.setVisible(true);
    // Local variables may have been updated by GridEditor so refresh
    // configurers. Setting the Dy configurer will auto-recalculate dx
    final double origDx = dx;
    final AutoConfigurer cfg = (AutoConfigurer) getConfigurer();
    cfg.getConfigurer(DY).setValue(String.valueOf(dy));
    dx = origDx;
    cfg.getConfigurer(DX).setValue(String.valueOf(dx));
    cfg.getConfigurer(X0).setValue(String.valueOf(origin.x));
    cfg.getConfigurer(Y0).setValue(String.valueOf(origin.y));
    cfg.getConfigurer(SIDEWAYS).setValue(String.valueOf(sideways));
  }
 
  public HexRef[] getAdjacentHexesWithNulls(HexRef pos) {
	  
	    final HexRef[] adjacent = new HexRef[6];
	    final int c = pos.getColumn();
	    final int r = pos.getRow();
	    int next = 0;
	    
	    if (sideways) {
	      if (r % 2 == 0) {
	        next = addHexWithNulls(adjacent, next, c-1, r-1);
	        next = addHexWithNulls(adjacent, next, c, r-1);
	        next = addHexWithNulls(adjacent, next, c-1, r);
	        next = addHexWithNulls(adjacent, next, c+1, r);
	        next = addHexWithNulls(adjacent, next, c-1, r+1);
	        next = addHexWithNulls(adjacent, next, c, r+1);
	      }
	      else {
	        next = addHexWithNulls(adjacent, next, c, r-1);
	        next = addHexWithNulls(adjacent, next, c+1, r-1);
	        next = addHexWithNulls(adjacent, next, c-1, r);
	        next = addHexWithNulls(adjacent, next, c+1, r);
	        next = addHexWithNulls(adjacent, next, c, r+1);
	        next = addHexWithNulls(adjacent, next, c+1, r+1);
	      }
	    }
	    else {
	      if (c % 2 == 0) {
	        next = addHexWithNulls(adjacent, next, c-1, r-1);
	        next = addHexWithNulls(adjacent, next, c, r-1);
	        next = addHexWithNulls(adjacent, next, c+1, r-1);
	        next = addHexWithNulls(adjacent, next, c-1, r);
	        next = addHexWithNulls(adjacent, next, c, r+1);
	        next = addHexWithNulls(adjacent, next, c+1, r);
	      }
	      else {
	        next = addHexWithNulls(adjacent, next, c-1, r);
	        next = addHexWithNulls(adjacent, next, c, r-1);
	        next = addHexWithNulls(adjacent, next, c+1, r);
	        next = addHexWithNulls(adjacent, next, c-1, r+1);
	        next = addHexWithNulls(adjacent, next, c, r+1);
	        next = addHexWithNulls(adjacent, next, c+1, r+1);
	      }
	    }
	    
	    return adjacent;
  }

  protected int addHexWithNulls(HexRef[] points, int next, int column, int row) {
	    if (column >= -1 && column <= getMaxColumns()+1 && row >= -1 && row <= getMaxRows()+1) {
	      points[next] = new HexRef(column, row, this);
	    }
	    return ++next;
	  }

  public String getLocationName(HexRef hr) {
    return locationName(getHexCenter(hr.getColumn(), hr.getRow()));
  }

  public void getHexesInRange(HashSet<HexRef> result, HexRef hex, int range) {
    if (hex == null) {
      return;
    }
    result.add(hex);
    HashSet<HexRef> helper1 = new HashSet<HexRef>();
    helper1.add(hex);
    HashSet<HexRef> helper2 = new HashSet<HexRef>();
    HashSet<HexRef> helper3;
    for (int r = 0; r < range; r++) {
      for (HexRef hr : helper1) {
        HexRef[] neighbours = getAdjacentHexes(hr);
        for (int i = 0; i < neighbours.length; i++) {
          if (neighbours[i] == null) {
            break;
          }
          helper2.add(neighbours[i]);
          result.add(neighbours[i]);
        }
      }
      helper3 = helper2;
      helper2 = helper1;
      helper1 = helper3;
      helper2.clear();
    }
  }

  public HashSet<HexRef> getHexesInRange(HexRef hex, int range) {
    HashSet<HexRef> result = new HashSet<HexRef>();
    getHexesInRange(result, hex, range);
    return result;
  }

  public HexRef[] getAdjacentHexes(HexRef hex) {
    if (adjacentHexes.containsKey(hex) == false) {
      HexRef[] result = findAdjacentHexes(hex);
      adjacentHexes.put(hex, result);
      return result;
    } else {
      return adjacentHexes.get(hex);
    }
  }

  /*
   * Return a list of the Grid positions of adjacent hexes. Do not include
   * hexes where no part of the hex appears on the board.
   * Argument is a GridPosition (c, r), not a map position(x, y)
   */
  
  protected HexRef[] findAdjacentHexes(HexRef pos) {
  
    final HexRef[] adjacent = new HexRef[6];
    final int c = pos.getColumn();
    final int r = pos.getRow();
    int next = 0;
    
    if (sideways) {
      if (r % 2 == 0) {
        next = addHex(adjacent, next, c-1, r-1);
        next = addHex(adjacent, next, c, r-1);
        next = addHex(adjacent, next, c-1, r);
        next = addHex(adjacent, next, c+1, r);
        next = addHex(adjacent, next, c-1, r+1);
        next = addHex(adjacent, next, c, r+1);
      }
      else {
        next = addHex(adjacent, next, c, r-1);
        next = addHex(adjacent, next, c+1, r-1);
        next = addHex(adjacent, next, c-1, r);
        next = addHex(adjacent, next, c+1, r);
        next = addHex(adjacent, next, c, r+1);
        next = addHex(adjacent, next, c+1, r+1);
      }
    }
    else {
      if (c % 2 == 0) {
        next = addHex(adjacent, next, c-1, r-1);
        next = addHex(adjacent, next, c, r-1);
        next = addHex(adjacent, next, c+1, r-1);
        next = addHex(adjacent, next, c-1, r);
        next = addHex(adjacent, next, c, r+1);
        next = addHex(adjacent, next, c+1, r);
      }
      else {
        next = addHex(adjacent, next, c-1, r);
        next = addHex(adjacent, next, c, r-1);
        next = addHex(adjacent, next, c+1, r);
        next = addHex(adjacent, next, c-1, r+1);
        next = addHex(adjacent, next, c, r+1);
        next = addHex(adjacent, next, c+1, r+1);
      }
    }
    
    return adjacent;
  }
  
  /*
   * Add Hex to array if it is the map
   */
  protected int addHex(HexRef[] points, int next, int column, int row) {
    if (column >= -1 && column <= getMaxColumns()+1 && row >= -1 && row <= getMaxRows()+1) {
      points[next++] = new HexRef(column, row, this);
    }
    return next;
  }
}