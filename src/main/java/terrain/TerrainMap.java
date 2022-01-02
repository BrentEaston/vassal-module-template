/*
 * $Id: TerrainMap.java 5656 2009-05-24 12:22:35Z morvael $
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

import VASSAL.build.GameModule;
import VASSAL.build.module.map.boardPicker.Board;
import VASSAL.tools.ArchiveWriter;
import VASSAL.tools.DataArchive;

import java.awt.Point;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * The terrain map. Holds all defined terrain for a board, plus appropriate
 * indexes to ensure fast access
 */
public class TerrainMap {

  public static final String NULL_TERRAIN = "NULL_TERRAIN";
  public static final String NULL_EDGE = "NULL_EDGE";
  public static final String NULL_LINE = "NULL_LINE";
  public static final String NULL_ATTRIBUTE = "NULL_ATTRIBUTE";
  public static final String CHAR_SET = "UTF-8";
  public static final String MAP_DIR = "terrainMaps";
  public static final String FILE_SUFFIX = "txt";
  protected static final String NO_TERRAIN = "No Terrain";
  protected Board board;
  protected TerrainHexGrid grid;
  protected HashMap<HexRef, TerrainHex> hexMap; // Keyed by Point = Hex Grid
  // Position
  protected HashMap<String, Area> hexArea;
  protected HashMap<EdgeRef, TerrainEdge> edgeMap; // Keyed by Rect = 2 Hex
  // Grid Positions
  protected HashMap<String, GeneralPath> edgePoly;
  protected HashMap<LineRef, TerrainLine> lineMap; // Keyed by EdgeRef
  protected HashMap<String, GeneralPath> linePoly;
  protected HashMap<AttrRef, TerrainAttribute> attributeMap; // Keyed by
  // AttrRef =
  // Point
  // /Attribute
  // name
  protected SortedSet<AttrRef> attributeList; // Tags sorted by hex

  public TerrainMap() {
    hexMap = new HashMap<HexRef, TerrainHex>();
    hexArea = new HashMap<String, Area>();
    edgeMap = new HashMap<EdgeRef, TerrainEdge>();
    edgePoly = new HashMap<String, GeneralPath>();
    lineMap = new HashMap<LineRef, TerrainLine>();
    linePoly = new HashMap<String, GeneralPath>();
    attributeMap = new HashMap<AttrRef, TerrainAttribute>();
    attributeList = new TreeSet<AttrRef>();
  }

  public TerrainMap(TerrainHexGrid g) {
    this();
    grid = g;
    board = grid.getBoard();
    load();
  }

  public void setHexTerrainType(TerrainHex hex, boolean load) {
    if (hex.getTerrain() == null) {
      hexMap.remove(hex.getLocation());
    }
    else {
      hexMap.put(hex.getLocation(), hex);
    }
    if (load == false) {
      clearHexAreas();
    }
  }

  /*
   * Add edge to map twice, so that we can look it up from either hex
   */
  public void setEdgeTerrainType(TerrainEdge edge, boolean load) {
    if (edge.getTerrain() == null) {
      edgeMap.remove(edge.getLocation());
      edgeMap.remove(edge.getReverseLocation());
    }
    else {
      edgeMap.put(edge.getLocation(), edge);
      edgeMap.put(edge.getReverseLocation(), edge.reversed());
    }
    if (load == false) {
      rebuildEdgeAreas();
    }
  }

  public void setLineTerrainType(TerrainLine line, boolean load) {
    if (line.getTerrain() == null) {
      lineMap.remove(line.getLocation());
      lineMap.remove(line.getReverseLocation());
    }
    else {
      lineMap.put(line.getLocation(), line);
      lineMap.put(line.getReverseLocation(), line.reversed());
    }
    if (load == false) {
      rebuildLineAreas();
    }
  }

  public void setAttributeTerrainType(TerrainAttribute attr) {
    final String value = attr.getValue();
    if (attr.getTerrain() == null || value == null || value.length() == 0) {
      final AttrRef ref = new AttrRef(attr.getLocation(), attr.getName());
      attributeMap.remove(ref);
      attributeList.remove(ref);
    }
    else {
      final AttrRef ref = new AttrRef(attr.getLocation(), attr.getName());
      attributeMap.put(ref, attr);
      attributeList.add(ref);
    }
  }

  public void setHexTerrainType(ArrayList<HexRef> hexes, HexTerrain terrain) {
    for (HexRef hexRef : hexes) {
      setHexTerrainType(new TerrainHex(hexRef, terrain, grid), false);
    }
  }

  public void setEdgeTerrainType(ArrayList<EdgeRef> edges, TerrainHexGrid grid, EdgeTerrain terrain) {
    for (EdgeRef edgeRef : edges) {
      setEdgeTerrainType(new TerrainEdge(edgeRef, terrain), false);
    }
  }

  public void setLineTerrainType(ArrayList<LineRef> lines, TerrainHexGrid grid, LineTerrain terrain) {
    for (LineRef lineRef : lines) {
      setLineTerrainType(new TerrainLine(lineRef, terrain), false);
    }
  }

  public TerrainHex getHexTerrain(HexRef hexPos) {
    return hexMap.get(hexPos);
  }

  public Collection<TerrainHex> getAllHexTerrain() {
    return hexMap.values();
  }

  public TerrainEdge getEdgeTerrain(HexRef hexPos1, HexRef hexPos2) {
    final EdgeRef key = new EdgeRef(hexPos1, hexPos2, grid);
    return edgeMap.get(key);
  }

  public Iterator<TerrainEdge> getAllEdgeTerrain() {
    return edgeMap.values().iterator();
  }

  public TerrainLine getLineTerrain(HexRef hexPos1, HexRef hexPos2) {
    final LineRef key = new LineRef(hexPos1, hexPos2, grid);
    return lineMap.get(key);
  }

  public Iterator<TerrainLine> getAllLineTerrain() {
    return lineMap.values().iterator();
  }

  public Iterator<TerrainAttribute> getAllAttributeTerrain() {
    return attributeMap.values().iterator();
  }

  public Iterator<TerrainAttribute> getSortedAttributeTerrain() {
    return new Iterator<TerrainAttribute>() {

      Iterator<AttrRef> i = attributeList.iterator();

      public boolean hasNext() {
        return i.hasNext();
      }

      public TerrainAttribute next() {
        return attributeMap.get(i.next());
      }

      public void remove() {
        return;
      }
    };
  }

  public TerrainAttribute getAttributeTerrain(HexRef hexPos) {
    return attributeMap.get(hexPos);
  }

  public Area getHexArea(String terrainType) {
    if (terrainType.equals(NO_TERRAIN)) {
      return null;
    }
    Area area = hexArea.get(terrainType);
    if (area == null) {
      rebuildHexArea(terrainType);
      area = hexArea.get(terrainType);
    }
    return area;
  }

  public GeneralPath getEdgePoly(String terrainType) {
    if (terrainType.equals(NO_TERRAIN)) {
      return null;
    }
    GeneralPath poly = edgePoly.get(terrainType);
    if (poly == null) {
      rebuildEdgeAreas();
      poly = edgePoly.get(terrainType);
    }
    return poly;
  }

  public GeneralPath getLinePoly(String terrainType) {
    if (terrainType.equals(NO_TERRAIN)) {
      return null;
    }
    GeneralPath poly = linePoly.get(terrainType);
    if (poly == null) {
      rebuildLineAreas();
      poly = linePoly.get(terrainType);
    }
    return poly;
  }

  public Collection<String> getHexAreaTypes() {
    return hexArea.keySet();
  }

  public Collection<String> getEdgeAreaTypes() {
    return edgePoly.keySet();
  }

  public Collection<String> getLineAreaTypes() {
    return linePoly.keySet();
  }

  protected void clearHexAreas() {
    hexArea.clear();
  }

  protected void clearEdgeAreas() {
    edgePoly.clear();
  }

  protected void clearLineAreas() {
    linePoly.clear();
  }

  public void rebuild() {
    rebuildHexAreas();
    rebuildEdgeAreas();
    rebuildLineAreas();
  }

  protected void rebuildHexAreas() {
    clearHexAreas();
    for (TerrainHex hex : getAllHexTerrain()) {
      final String type = hex.getTerrain().getConfigureName();
      Area area = hexArea.get(type);
      if (area == null) {
        area = new Area();
      }
      area.add(grid.getSingleHex(hex));
      hexArea.put(type, area);
    }
  }

  protected void rebuildHexArea(String type) {
    hexArea.remove(type);
    for (TerrainHex hex : getAllHexTerrain()) {
      final String t = hex.getTerrain().getConfigureName();
      if (t.equals(type)) {
        Area area = hexArea.get(type);
        if (area == null) {
          area = new Area();
        }
        area.add(grid.getSingleHex(hex));
        hexArea.put(type, area);
      }
    }
  }

  protected void rebuildEdgeAreas() {
    clearEdgeAreas();
    final Iterator<TerrainEdge> i = getAllEdgeTerrain();
    while (i.hasNext()) {
      final TerrainEdge edge = i.next();
      edge.recalculate();
      final String type = edge.getTerrain().getConfigureName();
      GeneralPath poly = edgePoly.get(type);
      if (poly == null) {
        poly = new GeneralPath();
      }
      Point end = edge.getEnd1();
      poly.moveTo(end.x, end.y);
      end = edge.getEnd2();
      poly.lineTo(end.x, end.y);
      edgePoly.put(type, poly);
    }
  }

  protected void rebuildLineAreas() {
    clearLineAreas();
    final Iterator<TerrainLine> i = getAllLineTerrain();
    while (i.hasNext()) {
      final TerrainLine line = i.next();
      line.recalculate();
      final String type = line.getTerrain().getConfigureName();
      GeneralPath poly = linePoly.get(type);
      if (poly == null) {
        poly = new GeneralPath();
      }
      Point end = line.getEnd1();
      poly.moveTo(end.x, end.y);
      end = line.getEnd2();
      poly.lineTo(end.x, end.y);
      linePoly.put(type, poly);
    }
  }

  public void save() {
    final StringBuffer buffer = new StringBuffer(2000);

    for (TerrainHex hex : hexMap.values()) {
      buffer.append(hex.encode());
      buffer.append(System.getProperty("line.separator"));
    }

    /*
     * Edges appear in the TerrainMap twice, once for each hex they seperate.
     * Only write them out once.
     */
    for (TerrainEdge edge : edgeMap.values()) {
      final EdgeRef ref = edge.getLocation();
      if (ref.getColumn() < ref.getColumn2()
          || (ref.getColumn() == ref.getColumn2() && ref.getRow() <= ref.getRow2())) {
        buffer.append(edge.encode());
        buffer.append(System.getProperty("line.separator"));
      }
    }

    for (TerrainLine line : lineMap.values()) {
      final LineRef ref = line.getLocation();
      if (ref.getColumn() < ref.getColumn2()
          || (ref.getColumn() == ref.getColumn2() && ref.getRow() <= ref.getRow2())) {
        buffer.append(line.encode());
        buffer.append(System.getProperty("line.separator"));
      }
    }

    for (TerrainAttribute attr : attributeMap.values()) {
      if (attr.getValue() != null && attr.getValue().length() > 0) {
        buffer.append(attr.encode());
        buffer.append(System.getProperty("line.separator"));
      }
    }

    final ArchiveWriter writer = GameModule.getGameModule().getArchiveWriter();
    byte[] bytes = new byte[0];
    try {
      bytes = buffer.toString().getBytes(CHAR_SET);
    }
    catch (Exception e) {
    }
    writer.addFile(getMapFileName(board), bytes);

  }

  public void load() {
    try {
      final DataArchive archive = GameModule.getGameModule().getDataArchive();
      InputStream stream = null;

      try {
        stream = archive.getInputStream(getMapFileName(board));

        final InputStreamReader reader = new InputStreamReader(stream, CHAR_SET);
        final BufferedReader buffer = new BufferedReader(reader);
        for (String line = buffer.readLine(); line != null; line = buffer.readLine()) {
          if (line.startsWith(TerrainHex.TYPE)) {
            addHexTerrain(line);
          }
          else if (line.startsWith(TerrainEdge.TYPE)) {
            addEdgeTerrain(line);
          }
          else if (line.startsWith(TerrainLine.TYPE)) {
            addLineTerrain(line);
          }
          else if (line.startsWith(TerrainAttribute.TYPE)) {
            addAttributeTerrain(line);
          }
        }
      }
      finally {
        stream.close();
      }
      clearHexAreas();
      rebuildEdgeAreas();
      rebuildLineAreas();

    }
    catch (Exception e) {
    }
  }

  public String getMapFileName(Board board) {
    return MAP_DIR + "/" + board.getName() + "." + FILE_SUFFIX;
  }

  protected void addHexTerrain(String line) {
    final TerrainHex th = new TerrainHex(line, grid);
    setHexTerrainType(th, true);
  }

  protected void addEdgeTerrain(String line) {
    final TerrainEdge te = new TerrainEdge(line, grid);
    setEdgeTerrainType(te, true);
  }

  protected void addLineTerrain(String line) {
    final TerrainLine tl = new TerrainLine(line, grid);
    setLineTerrainType(tl, true);
  }

  protected void addAttributeTerrain(String line) {
    final TerrainAttribute ta = new TerrainAttribute(line, grid);
    setAttributeTerrainType(ta);
  }

  public String getHexTerrainName(HexRef hex) {
    TerrainHex h = getHexTerrain(hex);
    HexTerrain h2 = h == null ? null : h.getTerrain();
    return h2 == null ? NULL_TERRAIN : h2.getTerrainName();
  }

  public String getEdgeTerrainName(HexRef fromHex, HexRef toHex) {
    TerrainEdge e = getEdgeTerrain(fromHex, toHex);
    EdgeTerrain e2 = e == null ? null : e.getTerrain();
    return e2 == null ? NULL_EDGE : e2.getTerrainName();
  }

  public String getLineTerrainName(HexRef fromHex, HexRef toHex) {
    TerrainLine l = getLineTerrain(fromHex, toHex);
    LineTerrain l2 = l == null ? null : l.getTerrain();
    return l2 == null ? NULL_LINE : l2.getTerrainName();
  }

  public String getAttributeTerrainValue(HexRef hex, String name) {
    TerrainAttribute a = getAttributeTerrain(new AttrRef(hex, name));
    return a == null ? NULL_ATTRIBUTE : a.getValue();
  }

  public HashSet<HexRef> findHexesWithTag(String name, String value) {
    HashSet<HexRef> result = new HashSet<HexRef>();
    Iterator<TerrainAttribute> it = getAllAttributeTerrain();
    TerrainAttribute ta;
    while (it.hasNext()) {
      ta = it.next();
      if ((name.equals(ta.getName())) && (value.equals(ta.getValue()))) {
        result.add(new HexRef(ta.getColumn(), ta.getRow(), grid));
      }
    }
    return result;
  }
}