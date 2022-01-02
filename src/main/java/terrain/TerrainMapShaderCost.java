package terrain;

import java.util.HashSet;

/**
 * 
 * @author morvael
 */
public final class TerrainMapShaderCost implements Comparable<TerrainMapShaderCost> {

	private HexRef from;
	private boolean mayContinue;
	private double pointsLeft;
	private boolean traceOnly;
	private String flag;
  private int value;
  private HashSet<HexRef> variants;
	
	public TerrainMapShaderCost(TerrainMapShaderRule r) {
		override(r);
	}
	
	public TerrainMapShaderCost(HexRef from, boolean mayContinue, double pointsLeft, boolean traceOnly, String flag, int value) {
		this.from = from;
		this.mayContinue = mayContinue;
		this.pointsLeft = pointsLeft;
		this.traceOnly = traceOnly;
		this.flag = flag;
    this.value = value;
	}

	public void override(TerrainMapShaderRule r) {
		this.from = r.getFrom();
		this.mayContinue = r.isMayContinue();
		this.pointsLeft = r.getPointsLeft();
		this.traceOnly = r.isTraceOnly();
		this.flag = r.getFlag();
    this.value = r.getValue();
	}

	public HexRef getFrom() {
		return from;
	}

	public void setFrom(HexRef from) {
		this.from = from;
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

  public void setTraceOnly(boolean traceOnly) {
    this.traceOnly = traceOnly;
  }
	
	public String getFlag() {
		return flag;
	}
	
	public int getValue() {
		return value;
	}

  public HashSet<HexRef> getVariants() {
    return variants;
  }
  
  public void addVariant(HexRef hr) {
    if (variants == null) {
      variants = new HashSet<HexRef>();
      variants.add(from);
    }
    variants.add(hr);
  }

  public boolean isVariant(HexRef hr) {
    if (variants != null) {
      return variants.contains(hr);      
    } else {
      return from == null ? hr == null : from.equals(hr);
    }
  }

  public boolean hasCommonFrom(TerrainMapShaderCost cost) {
    if (cost.variants != null) {
      for (HexRef hr : cost.variants) {
        if (isVariant(hr)) {
          return true;
        }
      }
      return false;
    } else {
      return isVariant(cost.from);
    }
  }

	public int compareTo(TerrainMapShaderCost o) {
		int result = compareBoolean(mayContinue, o.isMayContinue());
		if (result == 0) {
			result = compareBoolean(traceOnly, o.isTraceOnly());
			if (result == 0) {
        result = compareInteger(value, o.getValue());
        if (result == 0) {
          result = -Double.compare(pointsLeft, o.getPointsLeft());
        }
			}
		}
		return result;	
	}
	
	public static int compareBoolean(boolean b1, boolean b2) {
		if (b1 == true) {
			if (b2 == true) {
				return 0;
			} else {
				return -1;
			}
		} else {
			if (b2 == true) {
				return 1;
			} else {
				return 0;
			}
			
		}
	}

	public static int compareInteger(int b1, int b2) {
    if (b1 > b2) {
      return 1;
    } else
    if (b1 == b2) {
      return 0;
    } else {
      return -1;
    }
	}

}
