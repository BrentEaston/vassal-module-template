package terrain;

import VASSAL.build.AutoConfigurable;
import VASSAL.build.module.Map;
import VASSAL.build.module.map.MapShader;
import VASSAL.build.module.map.boardPicker.Board;
import VASSAL.build.module.map.boardPicker.board.ZonedGrid;
import VASSAL.configure.Configurer;
import VASSAL.configure.ConfigurerFactory;
import VASSAL.configure.IconConfigurer;
import VASSAL.counters.GamePiece;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.util.HashMap;
import java.util.List;

import javax.swing.KeyStroke;

/**
 * 
 * @author morvael
 */
public class TerrainMapShader extends MapShader {

  public static final String RULES_CLASS = "rules_class";
  public static final String RULES_PARAMS = "rules_params";
  public static final String BUTTON_VISIBLE = "button_visible";

  protected String rulesClass = "";
  protected String rulesParams = "";
  protected boolean buttonVisible = true;
  private Board gridBoard;
  private TerrainHexGrid grid;
  private TerrainMap terrainMap;
  private TerrainMapShaderRule rules = null;
  private Area lastArea = null;

  public TerrainMapShader() {
    super();
  }

  @Override
  public String[] getAttributeNames() {
    return new String[]{NAME, ALWAYS_ON, STARTS_ON, BUTTON_TEXT, TOOLTIP,
      ICON, HOT_KEY, BOARDS, BOARD_LIST, TYPE, DRAW_OVER, PATTERN,
      COLOR, IMAGE, OPACITY, BORDER, BORDER_COLOR, BORDER_WIDTH,
      BORDER_OPACITY, RULES_CLASS, RULES_PARAMS, BUTTON_VISIBLE
    };
  }

  @Override
  public Class<?>[] getAttributeTypes() {
    return new Class<?>[]{String.class, Boolean.class, Boolean.class,
      String.class, String.class, IconConfig.class, KeyStroke.class,
      BoardPrompt.class, String[].class, TypePrompt.class,
      Boolean.class, PatternPrompt.class, Color.class, Image.class,
      Integer.class, Boolean.class, Color.class, Integer.class,
      Integer.class, String.class, String.class, Boolean.class
    };
  }

  @Override
  public String[] getAttributeDescriptions() {
    return new String[]{"Name:  ", "Shading Always On?  ",
      "Shading Starts turned on?  ", "Button text:  ",
      "Tooltip Text:  ", "Button Icon:  ", "Hotkey:  ",
      "All boards in map get Shaded?  ", "Board List:  ", "Type:  ",
      "Draw Shade on top of Counters?  ", "Shade Pattern:  ",
      "Color:  ", "Image:  ", "Opacity(%)", "Border?  ",
      "Border Color:  ", "Border Width:  ", "Border opacity(%)",
      "Rules class:  ", "Rules params:  ", "Button visible?  "
    };
  }

  @Override
  public void setAttribute(String key, Object value) {
    if (RULES_CLASS.equals(key)) {
      rulesClass = (String) value;
    } else if (RULES_PARAMS.equals(key)) {
      rulesParams = (String) value;
    } else if (BUTTON_VISIBLE.equals(key)) {
      if (value instanceof String) {
        value = Boolean.valueOf((String) value);
      }
      buttonVisible = ((Boolean) value).booleanValue();
    } else {
      super.setAttribute(key, value);
    }
  }

  @Override
  public String getAttributeValueString(String key) {
    if (RULES_CLASS.equals(key)) {
      return rulesClass + "";
    } else if (RULES_PARAMS.equals(key)) {
      return rulesParams + "";
    } else if (BUTTON_VISIBLE.equals(key)) {
      return String.valueOf(buttonVisible);
    } else {
      return super.getAttributeValueString(key);
    }
  }

  public static String getConfigureTypeName() {
    return "Terrain Map Shading";
  }

  @Override
  protected void toggleShading() {
    super.toggleShading();
    lastArea = null;
  }

  @Override
  protected Area getShadeShape(Map map) {
    if (lastArea == null) {
      if (type.equals(FG_TYPE)) {
        lastArea = new Area();
      } else {
        lastArea = new Area(getBoardClip());
      }
      lastArea.add(getArea(null));
    }
    return lastArea;
  }

  @SuppressWarnings("unchecked")
  public TerrainMapShaderRule getRules() {
    if (rules != null) {
      return rules;
    } else {
      try {
        Class<TerrainMapShaderRule> rc = (Class<TerrainMapShaderRule>) Class.forName(rulesClass);
        rules = rc.newInstance();
        try {
          findGrid();
          rules.reset(map, grid, TerrainDefinitions.getInstance().getTerrainMap(grid), "", null);
        } catch (Exception e) {

        }
        return rules;
      } catch (Exception e) {
        return null;
      }
    }
  }

  public TerrainHexGrid getGrid() {
    if (grid == null) {
      findGrid();
    }
    return grid;
  }

  private void findGrid() {
    gridBoard = null;
    grid = null;
    terrainMap = null;
    for (Board b : map.getBoards()) {
      if (b.getGrid() instanceof TerrainHexGrid) {
        gridBoard = b;
        grid = (TerrainHexGrid) b.getGrid();
        terrainMap = TerrainDefinitions.getInstance().getTerrainMap(grid);
        break;
      } else if (b.getGrid() instanceof ZonedGrid) {
        ZonedGrid zg = (ZonedGrid) b.getGrid();
        List<TerrainHexGrid> l = zg.getComponentsOf(TerrainHexGrid.class);
        if (l.size() > 0) {
          gridBoard = b;
          grid = l.get(0);
          terrainMap = TerrainDefinitions.getInstance().getTerrainMap(grid);
          break;
        }

      }
    }
  }

  public HashMap<HexRef, TerrainMapShaderCost> getHexes(GamePiece piece) {
    findGrid();
    TerrainMapShaderRule r = getRules();
    if ((grid != null) && (r != null)) {
      return TerrainMapShaderRule.getHexes(map, grid, terrainMap, r, rulesParams, piece);
    } else {
      return new HashMap<HexRef, TerrainMapShaderCost>();
    }
  }

  public Area getArea(GamePiece piece) {
    Area result = new Area();
    for (HexRef hr : getHexes(piece).keySet()) {
      final Point c = grid.getHexCenter(hr.getColumn(), hr.getRow());
      result.add(grid.getSingleHex(c.x, c.y));
    }
    if (gridBoard != null) {
      Rectangle r = gridBoard.bounds();
      result = new Area(AffineTransform.getTranslateInstance(r.x, r.y).createTransformedShape(result));
    }
    return result;
  }

  @Override
  public void setLaunchButtonVisibility() {
    getLaunchButton().setVisible(buttonVisible && !isAlwaysOn());
  }

  static class IconConfig implements ConfigurerFactory {
    @Override
    public Configurer getConfigurer(final AutoConfigurable c, final String key, final String name) {
      return new IconConfigurer(key, name, "/images/die.gif"); //$NON-NLS-1$
    }
  }

}
