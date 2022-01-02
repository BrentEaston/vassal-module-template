package avl;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import terrain.EdgeTerrain;
import terrain.HexRef;
import terrain.HexTerrain;
import terrain.TerrainDefinitions;
import terrain.TerrainEdge;
import terrain.TerrainHex;
import terrain.TerrainHexGrid;
import terrain.TerrainMap;

public class AvlTerrainHexGrid extends TerrainHexGrid {

  public static final int SHAPE_CACHE_SIZE = 50;
  protected ShapeCache shapeCache = new ShapeCache(SHAPE_CACHE_SIZE);
  protected TerrainMap terrainMap;
  
  public Area getGridShape(Point center, int range) {
    return getTerrainGridShape(center, range);
  }
  
  /*
   * Take terrain into account when calculating the range shape
   */
  public Area getTerrainGridShape(Point center, int range) {
    
    // Generate the key to lookup the shape cache
    Rectangle cacheKey = new Rectangle(center.x, center.y, range, 0);
    
    // Hopefully we have generated this one already
    Area shape = (Area) shapeCache.get(cacheKey);
 
    // If not, generate it
    if (shape == null) {
        shape = getNewShape(center, range);
       
        // Add it to the cache for next time
        shapeCache.put(cacheKey, shape);
    }
 
    return shape;
  }
  
  /*
   * Generate a hex shape showing the command range from a given point on the
   * map taking terrain into account. 
   *   Hex Terrain "Water" - Blocks command Range
   *   Edge Terrain "Impassable" - Block command Range
   *   Edge Terrain "River" - Stops command range after crossing river
   */
  
  public static final String WATER = "Water";
  public static final String IMPASSABLE = "Impassable";
  public static final String RIVER = "River";
  
  protected Area getNewShape(Point center, int range) {
    
    if (terrainMap == null) {
      terrainMap =  TerrainDefinitions.getInstance().getTerrainMap(this);
    }
    
    // Create a directed graph of accessible hexes
    HashMap hexes = new HashMap();
    
    // Start with single hex
    HexRef start = new HexRef(getGridPosition(center));
    rotateIfSideways(start);
    hexes.put(start, new HexEntry(start, 0));
    
    // And expand outwards to range
    for (int r = 1; r <= range; r++) {
          
      // Copy old map to new, then process each old hex and check neighbors
      HashMap newHexes = new HashMap(hexes);
      Iterator it = hexes.values().iterator();     
      
      while (it.hasNext()) {
        HexEntry hex = (HexEntry) it.next();
        
        // Only check hexes at outer limits that have not been blocked
        if (!hex.isBlocked() && hex.getRange() >= (r-1)) {
          HexRef pos = hex.getGridPosition();
        
          // Get list of adjacent hexes
          HexRef[] adjacentHexes = getAdjacentHexes(pos);
        
          // And add if necessary
          for (int i = 0; i < adjacentHexes.length; i++) {
            HexRef nextHexPos = adjacentHexes[i];
            if (nextHexPos != null) {
              testHex(newHexes, pos, nextHexPos, r);
            }
          }
        }
        // Replace our list with new list for next iteration
      }
      hexes = newHexes;
    }
    
    // Build and return the final shape
    Area shape = new Area();
    Iterator it = hexes.keySet().iterator();
    while (it.hasNext()) {
      HexRef hexPos = (HexRef) it.next();
      Point c = getHexCenter(hexPos.getColumn(), hexPos.getRow());
      shape.add(getSingleHex(c.x, c.y));   
    }
    // Offset shape by the board position 
    Rectangle r = getBoard().bounds();
    shape = new Area(AffineTransform.getTranslateInstance(r.x, r.y).createTransformedShape(shape));
    return shape;
  }

  /* Process each hex at range (r-1):
   *    if hex is not blocked then
   *       Process each neighboring hex:
   *           Ignore if new hex is off the map
   *           Ignore if new hex is Impassible hex type
   *           Ignore if new hex is across Impassible edge
   *           Ignore if new hex has been visited before and is not blocked
   *           Other wise add to list with new range, set blocked if crossed river edge
   */ 
  
  protected void testHex(HashMap hexMap, HexRef fromHex, HexRef toHex, int range) {
    
    HexEntry targetHex = (HexEntry) hexMap.get(toHex);        
    TerrainHex h = terrainMap.getHexTerrain(toHex);
    HexTerrain targetTerrain = h == null ? null : h.getTerrain();
    TerrainEdge e = terrainMap.getEdgeTerrain(fromHex, toHex);
    EdgeTerrain targetEdge = e == null ? null : e.getTerrain();
    
    // Cannot add Water hexes under any circumstances
    if(targetTerrain != null && targetTerrain.getTerrainName().equals(WATER)) {
      return;
    }
    
    // Cannot cross an Impassible hex side under any circumstances
    if (targetEdge != null && targetEdge.getTerrainName().equals(IMPASSABLE)) {
      return;
    }
    
        
    if (targetHex == null) {
      // Target hex does not exist, so just add it and marked if blocked by river
      hexMap.put(toHex, new HexEntry(toHex, range,isBlocking(targetEdge)));
    }
    else {
      /* 
       * Target hex exists, so reentering a previously entered hex
       * We are only interested if entering a previously blocked hex
       * and we are unblocked
       */
      if (targetHex.isBlocked() && !isBlocking(targetEdge)) {
       hexMap.put(toHex, new HexEntry(toHex, range, false));
      }      
    }    
  }
    
  protected boolean isBlocking(EdgeTerrain edge) {
    return edge != null && edge.getTerrainName().equals(RIVER);
  }
  
  protected class HexEntry {
    protected HexRef ref;
    protected int range;
    protected boolean blocked;
    
    public HexEntry(HexRef p, int r) {
      this(p, r, false);
    }
    
    public HexEntry(HexRef p, int r, boolean b) {
      ref = new HexRef(p);
      range = r;
      blocked = b;
    }
    
    public HexRef getGridPosition() {
      return ref;
    }
    
    public int getRange() {
      return range;
    }
    
    public boolean isBlocked() {
      return blocked;
    }
    
    public void setRange(int r) {
      range = r;
    }
    
    public void setBlocked(boolean b) {
      blocked = b;
    }
  }
  
  /*
   * Initial version - hex terrain only
   */
  protected Area getOldShape(Point center, int range) {

    Point start = new Point(center);
    Area shape = getSingleHexShape(origin.x, origin.y, false);
    rotateIfSideways(origin);

    for (int i = -range; i <= range; i++) {
      int x = start.x + (int) (i * dx);

      int length = range * 2 + 1 - Math.abs(i);

      int startY = 0;
      if (length % 2 == 1) {
        startY = start.y - (int) (dy * (length - 1) / 2);
      }
      else {
        startY = start.y - (int) (dy * (0.5 + (length - 2) / 2));
      }

      int y = startY;
      for (int j = 0; j < length; j++) {
        Point p = new Point(x, y);
        rotateIfSideways(p);
        HexRef hexPos = this.getGridPosition(p);
        rotateIfSideways(hexPos);
        
        TerrainHex hex = TerrainDefinitions.getInstance().getTerrainMap(this).getHexTerrain(hexPos);
        if (hex == null || ! "Water".equals(hex.getTerrain().getTerrainName())) {
          shape.add(getSingleHexShape(p.x, p.y, false));
        }
        y = startY + (int) (j * dy);
      }
    }

    rotateIfSideways(start);
    
    return shape;
  }
  
/* -------------------------------------------------------
 * Least Recently used cache of Terrain Shapes generated.
 * Key is a Rectangle where 
 *   rect.x = x co-ord of center point
 *   rect.y = y co-ord of center point
 *   rect.width = range
 */
  
  public class ShapeCache extends LinkedHashMap {
    
    private static final long serialVersionUID = 1L;
    protected int max_size = 16;
    
    // create as a LRU orderered map
    public ShapeCache(int size) {
      super(16, 0.75f, true);
      max_size = size;
    }
    
    protected boolean removeEldestEntry(Map.Entry entry) {
      return this.size() > max_size;
    }
  }
}
