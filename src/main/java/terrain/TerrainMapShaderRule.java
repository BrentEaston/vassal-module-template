package terrain;

import java.util.HashMap;

import VASSAL.build.module.Map;
import VASSAL.counters.GamePiece;
import java.util.HashSet;

/**
 * 
 * @author morvael
 */
public abstract class TerrainMapShaderRule {

  protected Map map;
  protected TerrainHexGrid grid;
  protected TerrainMap terrainMap;
  protected String params;
  protected GamePiece piece;
  //
  protected HexRef from;
  protected boolean mayContinue;
  protected double pointsLeft;
  protected boolean traceOnly;
  protected String flag;
  protected int value;

  public TerrainMapShaderRule() {
  }

  public void reset(Map map, TerrainHexGrid grid, TerrainMap terrainMap, String params, GamePiece piece) {
    this.map = map;
    this.grid = grid;
    this.terrainMap = terrainMap;
    this.params = params;
    this.piece = piece;
  }

  public abstract void start(HashMap<HexRef, HashMap<String, TerrainMapShaderCost>> selectedHexes);

  public abstract boolean getCrossCost(HexRef fromHex, HexRef toHex, TerrainMapShaderCost currentCost);

  public abstract void stop();

  public abstract HexRef getBestPathDestination();

  public abstract TerrainMapShaderCost getBestCost(HashMap<String, TerrainMapShaderCost> costs);

  public int compareWith(TerrainMapShaderCost o2) {
    int result = TerrainMapShaderCost.compareBoolean(mayContinue, o2.isMayContinue());
    if (result == 0) {
      result = TerrainMapShaderCost.compareBoolean(traceOnly, o2.isTraceOnly());
      if (result == 0) {
        result = TerrainMapShaderCost.compareInteger(value, o2.getValue());
        if (result == 0) {
          result = -Double.compare(pointsLeft, o2.getPointsLeft());
        }
      }
    }
    return result;
  }

  protected boolean result(TerrainMapShaderCost c) {
    this.from = c.getFrom();
    this.mayContinue = c.isMayContinue();
    this.pointsLeft = c.getPointsLeft();
    this.traceOnly = c.isTraceOnly();
    this.flag = c.getFlag();
    this.value = c.getValue();
    return mayContinue && pointsLeft >= 0.0;
  }

  protected boolean result(HexRef from, boolean mayContinue, double newPoolValue, boolean traceOnly, String flag, int value) {
    this.from = from;
    this.mayContinue = mayContinue;
    this.pointsLeft = newPoolValue;
    this.traceOnly = traceOnly;
    this.flag = flag;
    this.value = value;
    return mayContinue && pointsLeft >= 0.0;
  }

  protected TerrainMapShaderCost create(HexRef from, boolean mayContinue, double newPoolValue, boolean traceOnly, String flag, int value) {
    result(from, mayContinue, newPoolValue, traceOnly, flag, value);
    return new TerrainMapShaderCost(this);
  }

  public HexRef getFrom() {
    return from;
  }

  public boolean isMayContinue() {
    return mayContinue;
  }

  public double getPointsLeft() {
    return pointsLeft;
  }

  public boolean isTraceOnly() {
    return traceOnly;
  }

  public String getFlag() {
    return flag;
  }

  public int getValue() {
    return value;
  }

  public Map getMap() {
    return map;
  }

  protected static boolean addHex(HashMap<HexRef, HashMap<String, TerrainMapShaderCost>> selectedHexes, HexRef hex, TerrainMapShaderCost cost) {
    HashMap<String, TerrainMapShaderCost> hf;
    if (selectedHexes.containsKey(hex)) {
      hf = selectedHexes.get(hex);
    } else {
      hf = new HashMap<String, TerrainMapShaderCost>();
      selectedHexes.put(hex, hf);
    }
    TerrainMapShaderCost oldCost = hf.get(cost.getFlag());
    if (oldCost == null) {
      hf.put(cost.getFlag(), cost);
      return true;
    } else {
      int compare = cost.compareTo(oldCost);
      if (compare < 0) {
        hf.put(cost.getFlag(), cost);
        return true;
      } else if (compare == 0) {
        oldCost.addVariant(cost.getFrom());
        return false;
      } else {
        return false;
      }
    }
  }

  private static boolean addHex(HashMap<HexRef, HashMap<String, TerrainMapShaderCost>> selectedHexes, HexRef hex, TerrainMapShaderRule rule) {
    HashMap<String, TerrainMapShaderCost> hf;
    if (selectedHexes.containsKey(hex)) {
      hf = selectedHexes.get(hex);
    } else {
      hf = new HashMap<String, TerrainMapShaderCost>();
      selectedHexes.put(hex, hf);
    }
    TerrainMapShaderCost oldCost = hf.get(rule.getFlag());
    if (oldCost == null) {
      hf.put(rule.getFlag(), new TerrainMapShaderCost(rule));
      return true;
    } else {
      int compare = rule.compareWith(oldCost);
      if (compare < 0) {
        hf.put(rule.getFlag(), new TerrainMapShaderCost(rule));
        return true;
      } else if (compare == 0) {
        oldCost.addVariant(rule.getFrom());
        return false;
      } else {
        return false;
      }
    }
  }

  public static HashMap<HexRef, TerrainMapShaderCost> getHexes(Map map, TerrainHexGrid grid, TerrainMap terrainMap, TerrainMapShaderRule r, String rulesParams, GamePiece piece) {
    HashMap<HexRef, TerrainMapShaderCost> result = new HashMap<HexRef, TerrainMapShaderCost>();
    r.reset(map, grid, terrainMap, rulesParams, piece);
    HexRef hdest = r.getBestPathDestination();
    HashSet<HexRef> hexesToCheck = new HashSet<HexRef>();
    HashSet<HexRef> hexesToCheckNextTime = new HashSet<HexRef>();
    HashSet<HexRef> hexesToCheckSwitch;
    HashMap<HexRef, HashMap<String, TerrainMapShaderCost>> selectedHexes = new HashMap<HexRef, HashMap<String, TerrainMapShaderCost>>();
    r.start(selectedHexes);
    TerrainMapShaderCost cost;
    if ((hdest == null) || (selectedHexes.containsKey(hdest) == false)) {
      for (HexRef hr : selectedHexes.keySet()) {
        cost = r.getBestCost(selectedHexes.get(hr));
        if ((cost == null) || ((cost.isMayContinue() == true) && (cost.getPointsLeft() > 0.0d))) {
          hexesToCheck.add(hr);
        }
      }
      boolean breakSearch = false;
      while (hexesToCheck.size() > 0) {
        for (HexRef hex : hexesToCheck) {
          HexRef[] neighbours = grid.getAdjacentHexes(hex);
          for (TerrainMapShaderCost pc : selectedHexes.get(hex).values()) {
            for (int i = 0; i < 6; i++) {
              if (neighbours[i] == null) {
                break;
              }
              //if (pc.isVariant(neighbours[i])) {
              //  continue;
              //}
              if (r.getCrossCost(hex, neighbours[i], pc)) {
                if (addHex(selectedHexes, neighbours[i], r)) {
                  if ((hdest != null) && (neighbours[i].equals(hdest))) {
                    breakSearch = true;
                    break;
                  }
                  if (r.getPointsLeft() > 0.0d) {
                    hexesToCheckNextTime.add(neighbours[i]);
                  }
                }
              }

            }
            if (breakSearch) {
              break;
            }
          }
          if (breakSearch) {
            break;
          }
        }
        if (breakSearch) {
          break;
        }
        hexesToCheckSwitch = hexesToCheckNextTime;
        hexesToCheckNextTime = hexesToCheck;
        hexesToCheck = hexesToCheckSwitch;
        hexesToCheckNextTime.clear();
      }
    }
    r.stop();
    if (hdest == null) {
      for (HexRef hr : selectedHexes.keySet()) {
        cost = r.getBestCost(selectedHexes.get(hr));
        if ((cost != null) && (cost.isTraceOnly() == false)) {
          result.put(hr, cost);
        }
      }
    } else {
      if (selectedHexes.containsKey(hdest)) {
        TerrainMapShaderCost startCost = r.getBestCost(selectedHexes.get(hdest));
        if (startCost != null) {
          String startFlag = startCost.getFlag();
          HexRef hr = hdest;
          while (hr != null) {
            cost = selectedHexes.get(hr).get(startFlag);
            if ((cost != null) && (cost.isTraceOnly() == false)) {
              result.put(hr, cost);
            }
            if (cost != null) {
              hr = cost.getFrom();
            } else {
              hr = null;
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * Finds best (lowest) value amongst non-trace hexes and sets as trace-only
   * those with worse (higher) value.
   * @param area Area to check.
   */
  public static HashMap<HexRef, TerrainMapShaderCost> leaveBestHexes(HashMap<HexRef, TerrainMapShaderCost> area) {
      //find out best value amongst non-trace hexes
    TerrainMapShaderCost cost;
    int min = Integer.MAX_VALUE;
    for (HexRef hr : area.keySet()) {
      cost = area.get(hr);
      if (cost.isTraceOnly() == false) {
        min = Math.min(min, cost.getValue());
      }
    }
    //set as trace hexes those with worse value than minimum
    for (HexRef hr : area.keySet()) {
      cost = area.get(hr);
      if ((cost.isTraceOnly() == false) && (cost.getValue() > min)) {
        cost.setTraceOnly(true);
      }
    }
    return area;
  }

  /**
   * Checks if given area has non trace-only hexes.
   * @param area Area to check.
   * @return true if given area has non trace-only hexes, false otherwise.
   */
  public static boolean hasNonTraceHexes(HashMap<HexRef, TerrainMapShaderCost> area) {
    for (HexRef hr : area.keySet()) {
      if (area.get(hr).isTraceOnly() == false) {
        return true;
      }
    }
    return false;
  }

}
