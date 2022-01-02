/*
 * $Id: Inventory.java 2269 2007-07-08 02:38:12Z swampwallaby $
 *
 * Copyright (c) 2000-2022 by Rodney Kinney, Brent Easton
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

package Inventory;

import VASSAL.build.AbstractConfigurable;
import VASSAL.build.AutoConfigurable;
import VASSAL.build.Buildable;
import VASSAL.build.GameModule;
import VASSAL.build.module.Chatter;
import VASSAL.build.module.GameComponent;
import VASSAL.build.module.documentation.HelpFile;
import VASSAL.command.Command;
import VASSAL.command.NullCommand;
import VASSAL.configure.Configurer;
import VASSAL.configure.ConfigurerFactory;
import VASSAL.configure.IconConfigurer;
import VASSAL.configure.StringArrayConfigurer;
import VASSAL.configure.StringEnum;
import VASSAL.configure.VisibilityCondition;
import VASSAL.counters.GamePiece;
import VASSAL.counters.PieceFilter;
import VASSAL.counters.PieceIterator;
import VASSAL.counters.Stack;
import VASSAL.tools.LaunchButton;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

public class Inventory extends AbstractConfigurable implements GameComponent {

  protected LaunchButton launch;
  protected ArrayList<Counter> counters;

  public static final String VERSION = "1.3";
  public static final String HOTKEY = "hotkey";
  public static final String BUTTON_TEXT = "text";
  public static final String NAME = "name";
  public static final String ICON = "icon";
  public static final String DEST = "destination";

  /*
   * Options Destination - Chat, Dialog, File.
   */
  public static final String DEST_CHAT = "Chat Window";
  public static final String DEST_DIALOG = "Dialog Window";
  protected String destination = DEST_CHAT;

  /*
   * Include pieces on named maps Include Offmap pieces? Include pieces with
   * marker Sort and break by Marker Total and report integer values in marker
   */

  public static final String INCLUDE_ALL_MAPS = "incAllMaps";
  public boolean incAllMaps = true;

  public static final String MAP_LIST = "mapList";
  public String[] mapList = new String[0];

  public static final String INCLUDE_OFFMAP = "incOff";

  public static final String INCLUDE = "include";
  protected String include = "";

  public static final String GROUP_BY = "groupBy";
  protected String groupBy = "";

  public static final String TOTAL = "total";
  protected String totalMarker = "";

  public Inventory() {
    ActionListener al = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ;
        generateInventory();
      }
    };
    launch = new LaunchButton(null, BUTTON_TEXT, HOTKEY, ICON, al);
    setAttribute(NAME, "Inventory");
    setAttribute(BUTTON_TEXT, "Inventory");
    launch.setToolTipText(getConfigureName());
    launch.setEnabled(false);
  }

  public static String getConfigureTypeName() {
    return "Inventory v" + VERSION;
  }

  public void addTo(Buildable b) {
    launch.setAlignmentY(0.0F);
    GameModule.getGameModule().getToolBar().add(getComponent());
    GameModule.getGameModule().getGameState().addGameComponent(this);
  }

  protected Component getComponent() {
    return launch;
  }

  public void removeFrom(Buildable b) {
    GameModule.getGameModule().getToolBar().remove(getComponent());
    GameModule.getGameModule().getGameState().removeGameComponent(this);
  }

  public void add(Buildable b) {
  }

  public void remove(Buildable b) {
  }

  protected void generateInventory() {

    counters = new ArrayList<>();
    PieceIterator pi = new PieceIterator(GameModule.getGameModule().getGameState().getAllPieces().iterator(), new Selector(
      incAllMaps, mapList, include));

    while (pi.hasMoreElements()) {

      GamePiece p = pi.nextPiece();

      String group = "";
      if (groupBy.length() > 0) {
        group = getGroupByValue(p);
      }

      int count = 1;
      if (totalMarker.length() > 0) {
        String s = getTotalValue(p);
        try {
          count = Integer.parseInt(s);
        }
        catch (Exception ignored) {
        }
      }

      Point pos = p.getPosition();

      String loc = p.getMap().locationName(pos);

      Counter c = new Counter(group, p.getName(), loc, count);

      counters.add(c);

    }

    Collections.sort(counters);

    String[] result = new String[] {getConfigureName() + ": "};
    String lastBreak = null;
    String breakVal;
    int count = 0;

    Iterator i = counters.iterator();
    while (i.hasNext()) {
      Counter c = (Counter) i.next();
      breakVal = c.getBreakKey() + "";
      if (breakVal.equals(lastBreak)) {
        count += c.getCount();
      }
      else {
        if (lastBreak != null) {
          result[0] += lastBreak + ": " + count + "  ";
        }
        lastBreak = breakVal;
        count = c.getCount();
      }
    }

    if (lastBreak != null) {
      result[0] += lastBreak + ": " + count + "  ";
    }

    Command c = new DisplayResults(result, destination);
    c.execute();
    GameModule.getGameModule().sendAndLog(c);

  }

  protected String getTotalValue(GamePiece p) {
    return (String) p.getProperty(totalMarker);
  }

  protected String getGroupByValue(GamePiece p) {
    return (String) p.getProperty(groupBy);
  }

  public class DisplayResults extends Command {

    String[] results;
    String destination;

    public DisplayResults(String[] results, String destination) {

      this.results = results;
      this.destination = destination;
    }

    protected void executeCommand() {

      if (destination.equals(DEST_CHAT)) {
        Command c = new NullCommand();
        for (int i = 0; i < results.length; i++) {
          c.append(new Chatter.DisplayText(GameModule.getGameModule().getChatter(), results[i]));
        }
        c.execute();
      }
      else if (destination.equals(DEST_DIALOG)) {

        String text = "";
        for (int i = 0; i < results.length; i++) {
          text += results[i] + "\n";
        }
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JOptionPane.showMessageDialog(GameModule.getGameModule().getPlayerWindow(), scrollPane, getConfigureName(),
          JOptionPane.PLAIN_MESSAGE);
      }

    }

    protected Command myUndoCommand() {
      return null;
    }

  }

  public HelpFile getHelpFile() {
    return null;
  }

  public Class<?>[] getAllowableConfigureComponents() {
    return new Class[0];
  }

  public String[] getAttributeDescriptions() {
    return new String[] {"Name", "Button text", "Button icon", "Hotkey", "Report Destination",
      "Include Counters from All Maps?", "Include Counters from these Maps only",
      "Only include counters with Marker", "Sort and Group By Marker", "Total and Report Integer values in Marker"};
  }

  public Class<?>[] getAttributeTypes() {
    return new Class[] {String.class, String.class, IconConfig.class, KeyStroke.class, Dest.class, Boolean.class,
      String[].class, String.class, String.class, String.class};
  }

  public String[] getAttributeNames() {
    return new String[] {NAME, BUTTON_TEXT, ICON, HOTKEY, DEST, INCLUDE_ALL_MAPS, MAP_LIST, INCLUDE, GROUP_BY, TOTAL};
  }

  public static class Dest extends StringEnum {
    public String[] getValidValues(AutoConfigurable target) {
      return new String[] {DEST_CHAT, DEST_DIALOG};
    }
  }

  public void setAttribute(String key, Object o) {

    if (NAME.equals(key)) {
      setConfigureName((String) o);
      launch.setToolTipText((String) o);
    }
    else if (DEST.equals(key)) {
      destination = (String) o;
    }
    else if (INCLUDE_ALL_MAPS.equals(key)) {
      if (o instanceof String) {
        o = Boolean.valueOf((String) o);
      }
      incAllMaps = (Boolean) o;
    }
    else if (MAP_LIST.equals(key)) {
      if (o instanceof String) {
        o = StringArrayConfigurer.stringToArray((String) o);
      }
      mapList = (String[]) o;
    }
    else if (INCLUDE.equals(key)) {
      include = (String) o;
    }
    else if (GROUP_BY.equals(key)) {
      groupBy = (String) o;
    }
    else if (TOTAL.equals(key)) {
      totalMarker = (String) o;
    }
    else {
      launch.setAttribute(key, o);
    }
  }

  public String getAttributeValueString(String key) {
    if (NAME.equals(key)) {
      return getConfigureName();
    }
    else if (DEST.equals(key)) {
      return destination;
    }
    else if (INCLUDE_ALL_MAPS.equals(key)) {
      return incAllMaps + "";
    }
    else if (MAP_LIST.equals(key)) {
      return StringArrayConfigurer.arrayToString(mapList);
    }
    else if (INCLUDE.equals(key)) {
      return include;
    }
    else if (GROUP_BY.equals(key)) {
      return groupBy;
    }
    else if (TOTAL.equals(key)) {
      return totalMarker;
    }
    else {
      return launch.getAttributeValueString(key);
    }
  }

  public VisibilityCondition getAttributeVisibility(String name) {
    if (MAP_LIST.equals(name)) {
      return new VisibilityCondition() {
        public boolean shouldBeVisible() {
          return !incAllMaps;
        }
      };
    }
    else {
      return new VisibilityCondition() {
        public boolean shouldBeVisible() {
          return true;
        }
      };
    }
  }

  public void setup(boolean gameStarting) {
    launch.setEnabled(gameStarting);
  }

  public Command getRestoreCommand() {
    return null;
  }

  public static class Counter implements Comparable {
    protected String sortKey;
    protected String breakKey;
    protected String name;
    protected String location;
    protected int count;

    public Counter(String breakKey, String name, String location, int count) {
      sortKey = breakKey + "*" + name;
      this.setBreakKey(breakKey);
      this.setName(name);
      this.location = location;
      this.count = count;
    }

    public void setBreakKey(String breakKey) {
      this.breakKey = breakKey;
    }

    public String getBreakKey() {
      return breakKey;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public int getCount() {
      return count;
    }

    public boolean equals(Object o) {
      if (!(o instanceof Counter))
        return false;
      Counter c = (Counter) o;
      return c.sortKey.equals(sortKey);
    }

    public int hashCode() {
      return getName().hashCode();
    }

    public String toString() {
      return getName();
    }

    public int compareTo(Object o) {
      Counter n = (Counter) o;
      return sortKey.compareTo(n.sortKey);
    }
  }

  /**
   * Filter to select pieces required
   *
   * @author Brent Easton
   */
  protected static class Selector implements PieceFilter {

    protected String includeMarker;
    protected String[] mapList;
    protected boolean allMaps;

    public Selector(boolean all, String[] maps, String include) {
      allMaps = all;
      mapList = maps;
      includeMarker = include;
    }

    public boolean accept(GamePiece piece) {

      // Ignore Stacks, pieces are reported individually from GameState
      if (piece instanceof Stack)
        return false;

      // Don't report pieces with no map
      if (piece.getMap() == null)
        return false;

      // If a list of Maps supplied, check map names match
      if (!allMaps && mapList != null && mapList.length > 0) {
        String mapName = piece.getMap().getMapName();
        boolean found = false;
        for (int i = 0; i < mapList.length && !found; i++) {
          found = mapName.equals(mapList[i]);
        }
        if (!found)
          return false;
      }

      // Check for marker
      if (includeMarker != null && includeMarker.length() > 0) {
        return piece.getProperty(includeMarker) != null;
      }

      // Default Accept piece
      return true;
    }

  }
  static class IconConfig implements ConfigurerFactory {
    @Override
    public Configurer getConfigurer(final AutoConfigurable c, final String key, final String name) {
      return new IconConfigurer(key, name, "/images/die.gif"); //$NON-NLS-1$
    }
  }

}