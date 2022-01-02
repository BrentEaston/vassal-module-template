/*
 * $Id: MapTerrain.java 3639 2008-05-23 11:24:55Z swampwallaby $
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

import java.awt.Color;

import VASSAL.build.AbstractConfigurable;
import VASSAL.build.Buildable;
import VASSAL.build.module.documentation.HelpFile;
import VASSAL.configure.ColorConfigurer;

/**
 * Base class for all Terrain definitions
 *
 */
public class MapTerrain extends AbstractConfigurable {
  
  protected static final String NAME = "name";
  protected static final String COLOR = "color";

  protected Color color = null;
  
  public MapTerrain() {
    super();
  }  
  
  public String getTerrainName() {
    return getConfigureName();
  }
  
  public Color getColor() {
    return color;
  }

  public String[] getAttributeNames() {
    return new String[] {NAME, COLOR};
  }

  public String[] getAttributeDescriptions() {
    return new String[] {"Name:  ", "Display Color:  "};
  }

  public Class<?>[] getAttributeTypes() {
    return new Class[] {String.class, Color.class};
  }
  
  public void setAttribute(String key, Object value) {
    if(NAME.equals(key)) {
      setConfigureName((String) value);
    }
    else if (COLOR.equals(key)) {
      if (value instanceof String) {
        value = ColorConfigurer.stringToColor((String) value);
      }
      color = (Color) value;
    }
  }

  public String getAttributeValueString(String key) {
    if(NAME.equals(key)) {
      return getConfigureName();
    }
    else if (COLOR.equals(key)) {
      return ColorConfigurer.colorToString(color);
    }
    return null;
  }
  
  public void addTo(Buildable b) {

  }

  public void removeFrom(Buildable b) {

  }

  public HelpFile getHelpFile()  {
      return null;
  }

  public static String getConfigureTypeName() {
    return "Terrain";
  }

  public Class<?>[] getAllowableConfigureComponents() {
    return new Class[] {TerrainAttribute.class};
  }

}
