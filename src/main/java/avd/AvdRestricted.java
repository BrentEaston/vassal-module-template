/*
 * $Id: AvdRestricted.java 4507 2008-11-19 04:42:48Z rodneykinney $
 *
 * Copyright (c) 2000-2009 by Rodney Kinney, Brent Easton
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
package avd;

import VASSAL.tools.NamedKeyStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import VASSAL.build.GameModule;
import VASSAL.build.module.Map;
import VASSAL.build.module.PlayerRoster;
import VASSAL.build.module.documentation.HelpFile;
import VASSAL.command.ChangeTracker;
import VASSAL.command.Command;
import VASSAL.command.NullCommand;
import VASSAL.configure.BooleanConfigurer;
import VASSAL.configure.HotKeyConfigurer;
import VASSAL.configure.StringArrayConfigurer;
import VASSAL.configure.StringConfigurer;
import VASSAL.counters.Decorator;
import VASSAL.counters.GamePiece;
import VASSAL.counters.KeyCommand;
import VASSAL.counters.PieceEditor;
import VASSAL.counters.PieceVisitor;
import VASSAL.counters.PieceVisitorDispatcher;
import VASSAL.counters.Properties;
import VASSAL.counters.Stack;
import VASSAL.i18n.TranslatablePiece;
import VASSAL.tools.SequenceEncoder;

/**
 * A GamePiece with the Restricted trait can only be manipulated by the player
 * playing a specific side
 */
public final class AvdRestricted extends Decorator implements TranslatablePiece {
  public static final String ID = "restrict;";
  public static final String LOCKED = "locked";
  private String[] side;
  private boolean restrictByPlayer;
  private String owningPlayer = "";
  private static PlayerRoster.SideChangeListener handleRetirement;
  private boolean multiLocking;
  private boolean locked = false;
  private ArrayList<String> lockedBy = new ArrayList<String>(0);
  private KeyStroke lockKey;
  private String lockCommand;
  private KeyStroke unlockKey;
  private String unlockCommand;
  private boolean restrictMovement = true;

  public AvdRestricted() {
    this(ID, null);
  }

  public AvdRestricted(String type, GamePiece p) {
    setInner(p);
    mySetType(type);
    if (handleRetirement == null) {
      handleRetirement = new RetirementHandler();
      GameModule.getGameModule().addSideChangeListenerToPlayerRoster(handleRetirement);
    }
  }

  public String getDescription() {
    return "Lockable Restricted Access";
  }

  public HelpFile getHelpFile() {
    return HelpFile.getReferenceManualPage("RestrictedAccess.htm");
  }

  public void mySetType(String type) {
    type = type.substring(ID.length());
    SequenceEncoder.Decoder st = new SequenceEncoder.Decoder(type, ';');
    side = st.nextStringArray(0);
    restrictByPlayer = st.nextBoolean(false);
    multiLocking = st.nextBoolean(false);
    lockCommand = st.nextToken("Lock");
    lockKey = st.nextKeyStroke('L');
    unlockCommand = st.nextToken("Unlock");
    unlockKey = st.nextKeyStroke('U');
    restrictMovement = st.nextBoolean(true);
  }

  public Shape getShape() {
    return piece.getShape();
  }

  public Rectangle boundingBox() {
    return piece.boundingBox();
  }

  public void draw(Graphics g, int x, int y, Component obs, double zoom) {
    piece.draw(g, x, y, obs, zoom);
  }

  public String getName() {
    return piece.getName();
  }

  protected KeyCommand[] myGetKeyCommands() {
    if (multiLocking) {
      if (locked) {
        if (lockedBy.contains(PlayerRoster.getMySide())) {
          if (unlockCommand.length() > 0 && unlockKey != null) {
            return new KeyCommand[] {new KeyCommand(unlockCommand, unlockKey, this)};
          }
        }
        else {
          if (unlockCommand.length() > 0 && unlockKey != null) {
            final KeyCommand command = new KeyCommand("Unlock - waiting for "+lockedBy.toString(), NamedKeyStroke.NULL_KEYSTROKE, this);
            command.setEnabled(false);
            return new KeyCommand[] {command};
          }
        }
      }
      else {
        if (lockCommand.length() > 0 && lockKey != null) {
          return new KeyCommand[] {new KeyCommand(lockCommand, lockKey, this)};
        }
      }
    }

    return new KeyCommand[0];
  }

  public boolean isRestricted() {
    if (multiLocking) {
      return locked;
    }
    
    boolean restricted = false;
    if (restrictByPlayer) {
      restricted = owningPlayer.length() > 0
          && !GameModule.getUserId().equals(owningPlayer);
    }
    if ((restricted || !restrictByPlayer) && PlayerRoster.isActive()
        && GameModule.getGameModule().getGameState().isGameStarted()) {
      restricted = true;
      for (int i = 0; i < side.length; ++i) {
        if (side[i].equals(PlayerRoster.getMySide())) {
          restricted = false;
          break;
        }
      }
    }
    return restricted;
  }

  /*
   * @Override public void setMap(Map m) { if (m != null && restrictByPlayer &&
   * owningPlayer.length() == 0) { owningPlayer = GameModule.getUserId(); }
   * super.setMap(m); }
   */
  @Override
  public void setProperty(Object key, Object val) {
    if (Properties.SELECTED.equals(key) && Boolean.TRUE.equals(val)
        && restrictByPlayer && owningPlayer.length() == 0) {
      if (getMap() != null) {
        owningPlayer = GameModule.getUserId();
      }
      else {
        System.err.println("Selected, but map == null");
      }
    }
    super.setProperty(key, val);
  }

  protected KeyCommand[] getKeyCommands() {
    if (!isRestricted()) {
      return super.getKeyCommands();
    }
    else {
      return myGetKeyCommands();
    }
  }

  @Override
  public Object getLocalizedProperty(Object key) {
    if (Properties.RESTRICTED.equals(key)) {
      return Boolean.valueOf(isRestricted() && restrictMovement);
    }
    else if (LOCKED.equals(key)) {
      return multiLocking && locked ? Boolean.TRUE : Boolean.FALSE;
    }
    else {
      return super.getLocalizedProperty(key);
    }
  }

  public Object getProperty(Object key) {
    if (Properties.RESTRICTED.equals(key)) {
      return Boolean.valueOf(isRestricted() && restrictMovement);
    }
    else if (LOCKED.equals(key)) {
      return multiLocking && locked ? Boolean.TRUE : Boolean.FALSE;
    }
    else {
      return super.getProperty(key);
    }
  }

  public String myGetState() {
    return new SequenceEncoder(';').append(owningPlayer).append(locked).append(
        lockedBy.toArray(new String[0])).getValue();
  }

  public String myGetType() {
    return ID
        + new SequenceEncoder(';').append(side).append(restrictByPlayer)
            .append(multiLocking).append(lockCommand).append(lockKey).append(
                unlockCommand).append(unlockKey).append(restrictMovement).getValue();
  }

  public Command myKeyEvent(KeyStroke stroke) {
    if (multiLocking) {
      if (! locked && lockKey.equals(stroke)) {
        locked = true;
        lockedBy.clear();
        for (int i = 0; i < side.length; i++) {
          lockedBy.add(side[i]);
        }
      }
      else if (locked && unlockKey.equals(stroke)) {
        final String mySide = PlayerRoster.getMySide();
        if (lockedBy.contains(mySide)) {
          lockedBy.remove(mySide);
          if (lockedBy.size() == 0) {
            locked = false;
          }
        }
      }
    }
    return null;
  }

  public Command keyEvent(KeyStroke stroke) {
    if (!isRestricted()) {
      return super.keyEvent(stroke);
    }
    else {
      return myKeyEvent(stroke);
    }
  }

  public void mySetState(String newState) {
    final SequenceEncoder.Decoder sd = new SequenceEncoder.Decoder(newState,
        ';');
    owningPlayer = sd.nextToken("");
    locked = sd.nextBoolean(false);
    final String[] t = sd.nextStringArray(0);
    lockedBy = new ArrayList<String>(t.length);
    for (int i = 0; i < t.length; i++) {
      lockedBy.add(t[i]);
    }
  }

  public PieceEditor getEditor() {
    return new Ed(this);
  }

  public static class Ed implements PieceEditor {
    private BooleanConfigurer byPlayer;
    private StringArrayConfigurer config;
    private BooleanConfigurer lockConfig;
    private StringConfigurer lockCommandConfig;
    private HotKeyConfigurer lockKeyConfig;
    private StringConfigurer unlockCommandConfig;
    private HotKeyConfigurer unlockKeyConfig;
    private BooleanConfigurer movementConfig;
    private Box box;

    public Ed(AvdRestricted r) {
      byPlayer = new BooleanConfigurer(null,
          "Also belongs to initially-placing player", r.restrictByPlayer);
      config = new StringArrayConfigurer(null, "Belongs to side", r.side);
      lockConfig = new BooleanConfigurer(null, "Multi-player Locking?",
          r.multiLocking);
      lockConfig.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent arg0) {
          updateVisibility();          
        }});
      lockCommandConfig = new StringConfigurer(null, "Lock Command:",
          r.lockCommand);
      lockKeyConfig = new HotKeyConfigurer(null, "Lock Keystroke:", r.lockKey);
      unlockCommandConfig = new StringConfigurer(null, "Unlock Command:",
          r.unlockCommand);
      unlockKeyConfig = new HotKeyConfigurer(null, "Unlock Keystroke:",
          r.unlockKey);
      movementConfig = new BooleanConfigurer(null, "Prevent non-owning players from moving piece?", r.restrictMovement);
      box = Box.createVerticalBox();
      ((JComponent) byPlayer.getControls()).setAlignmentX(Box.RIGHT_ALIGNMENT);
      box.add(config.getControls());
      box.add(byPlayer.getControls());
      box.add(movementConfig.getControls());
      box.add(lockConfig.getControls());
      box.add(lockCommandConfig.getControls());
      box.add(lockKeyConfig.getControls());
      box.add(unlockCommandConfig.getControls());
      box.add(unlockKeyConfig.getControls());
      updateVisibility();
    }

    private void updateVisibility() {
      final boolean locking = lockConfig.booleanValue().booleanValue();
      lockCommandConfig.getControls().setVisible(locking);
      lockKeyConfig.getControls().setVisible(locking);
      unlockCommandConfig.getControls().setVisible(locking);
      unlockKeyConfig.getControls().setVisible(locking);
    }
    public Component getControls() {
      return box;
    }

    public String getState() {
      return "";
    }

    public String getType() {
      return ID
          + new SequenceEncoder(';').append(config.getValueString()).append(
              byPlayer.booleanValue()).append(lockConfig.getValueString())
              .append(lockCommandConfig.getValueString()).append(
                  lockKeyConfig.getValueString()).append(
                  unlockCommandConfig.getValueString()).append(
                  unlockKeyConfig.getValueString()).append(movementConfig.booleanValue()).getValue();
    }
  }

  /**
   * When a player changes sides to become an observer, relinquish ownership of
   * all pieces
   * 
   * @author rodneykinney
   * 
   */
  private static class RetirementHandler implements
      PlayerRoster.SideChangeListener, PieceVisitor {

    public void sideChanged(String oldSide, String newSide) {
      if (newSide == null) {
        PieceVisitorDispatcher d = new PieceVisitorDispatcher(this);
        Command c = new NullCommand();
        for (Map m : GameModule.getGameModule().getComponentsOf(Map.class)) {
          for (GamePiece piece : m.getPieces()) {
            c = c.append((Command) d.accept(piece));
          }
        }
        GameModule.getGameModule().sendAndLog(c);
      }
    }

    public Object visitDefault(GamePiece p) {
      AvdRestricted r = (AvdRestricted) Decorator.getDecorator(p,
          AvdRestricted.class);
      if (r != null && r.restrictByPlayer
          && GameModule.getUserId().equals(r.owningPlayer)) {

        ChangeTracker t = new ChangeTracker(p);
        r.owningPlayer = "";
        return t.getChangeCommand();
      }
      return null;
    }

    public Object visitStack(Stack s) {
      Command c = new NullCommand();
      for (Iterator<GamePiece> it = s.getPiecesIterator(); it.hasNext();) {
        c = c.append((Command) visitDefault(it.next()));
      }
      return c;
    }

  }
}
